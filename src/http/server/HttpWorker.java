package http.server;

import java.io.*;
import java.net.*;
import java.util.*;

import logger.Logger;
import logger.LoggerCallback;
import logger.utils.ChangeablePrimitive;

/**
 * Http worker which provides response to GET and HEAD requests.
 * @author Antonio Terpin
 */
public class HttpWorker implements Runnable {
	private Socket clientSocket;  // connection socket
	private String name = "Antonio", METHOD = "method", PROTOCOL_VERSION = "protocol-version", RESOURCE = "resource", HEADER = "header",
			/*BODY = "body",*/ relativePath = "www", logFileRequest = "/log.html", logStylePath = "style/logStyle.css", errorsFolder = "errors", HTTPv = "HTTP/1.1";
	private Map<String, Object> request = null;
	// ways to communicate to client
	private PrintWriter sender = null;
	private OutputStream out = null; // to send bytes
	private BufferedReader receiver = null;
	private Logger logger; // logger
	private HTTPLoggerCallback httpL = new HTTPLoggerCallback(); // create callback class to user the logger

	/**
	 * Constructor that allows to provide the client socket and the logger to work on. 
	 * @param clientSocket The client socket which will send the GET request.
	 * @param logger Logger to save logs created from each connection.
	 */
	public HttpWorker(Socket clientSocket, Logger logger) {
		this.clientSocket = clientSocket;
		this.logger = logger;
	}

	/**
	 * The HttpWorker receive the request, parse the header and compute the request to provide, if possible, a response.<br>
	 * The request needed is a common HTTP request.
	 */
	@SuppressWarnings("unchecked")
	public void run() {
		System.out.println ("WS: Connection successful! Waiting for input..");
		try {
			out = clientSocket.getOutputStream();
			sender = new PrintWriter(out,true);
			receiver = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); 
			request = getRequest(); // get client request
			String log = "";
			if (request != null) {
				// there is a request, provide response if possible
				log = provideResponse();
			} else {
				// if get request returns null it is because of a bad request error
				log = sendBadRequest();
			}
			System.out.println("WS: Response: " + log);
			String resource = "";
			try {
				// try to get the requested resource
				resource = (String) ((Map<String, Object>) request.get(HEADER)).get(RESOURCE);
			} catch(NullPointerException ex) {}
			//if(r == null) { r = ""; } // not necessary
			logger.write(httpL, clientSocket.getLocalAddress(), log, resource); // writing the log
			System.out.println("WS: Closing connection..");
			// closing connection (no keep alive, single tcp connection for each request).
			sender.close();
			receiver.close();
		} catch (IOException ioe) {
			System.out.println("WS: IOException on socket: " + ioe.getMessage());
		} 
		try {
			clientSocket.close(); 
		} catch (IOException ioe) {
			System.out.println("WS: IOException on socket: " + ioe.getMessage());
		}
	}

	/**
	 * Compute the header and try to satisfy the request.
	 * @return Log Response log (such as "HTTP/1.1 200 OK")
	 */
	protected String provideResponse() {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> header = (Map<String, Object>) request.get(HEADER);
			String method = (String) header.get(METHOD);
			switch (method) {
			case "GET": return provideGetResponse((String) header.get(RESOURCE));
			case "HEAD": return provideHeadResponse((String) header.get(RESOURCE));
			case "POST": // TODO create page form and update file adding an input line
			case "PUT": // TODO add a file updated from a form
			case "DELETE": // TODO delete a file not in the blacklist 
				return sendNotImplemented(); // no response for the requested method
			default: return sendBadRequest(); // not a valid method
			}
		} catch (NullPointerException ex ) {
			return sendBadRequest(); // request was bad formatted
		}
	}

	
	// METHODS RESPONSES
	
	/**
	 * Method to provide response to GET request.
	 * @param resourcePath The path of the requested resource.
	 * @return Log The log which represents the result of the response.
	 */
	protected String provideGetResponse(String resourcePath) {
		if(resourcePath.equals(logFileRequest)) {
			// returns log file built at runtime (just for try, not really a good idea in a real implementation..)
			return provideLogFileAsHtml();
		}
		String log = HTTPv + " 200 OK";
		File f = new File(relativePath + resourcePath);
		if (f.exists()) {
			// it can be a folder
			if (f.isDirectory()) {
				f = new File(f.getAbsolutePath() + "/index.html");
				if (!f.exists()) {
					return sendFileNotFound();
				}
			}
			// get response header for the file
			String[] responseHeader = getResponseHeader(f, log);
			if (responseHeader == null) {
				return sendBadRequest(); // something went wrong because of bad format
			}
			// if was possible to obtain the header, provide the requested resource
			FileInputStream reader;
			try {
				reader = new FileInputStream(f.getAbsolutePath());
			} catch (FileNotFoundException e) {
				return sendBadRequest();
			}
			String result = sendResponse(responseHeader, reader); // send response
			if(result != null) {
				return result; // if some other log was received, returns that
			}
			return log; // all was fine :)
		}
		return sendFileNotFound(); // file not found error
	}
	
	/**
	 * Method to provide response to HEAD request.
	 * @param resourcePath The path of the requested resource.
	 * @return Log The log which represents the result of the response.
	 */
	protected String provideHeadResponse(String resourcePath) {
		String log = HTTPv + " 200 OK";
		File f = new File(relativePath + resourcePath);
		if (f.exists()) {
			// it can be a folder
			if (f.isDirectory()) {
				f = new File(f.getAbsolutePath() + "/index.html");
				if (!f.exists()) {
					return sendFileNotFound();
				}
			}
			// get response header for the file
			String[] responseHeader = getResponseHeader(f, log);
			if (responseHeader == null) {
				return sendBadRequest(); // something went wrong because of bad format
			}
			// send only the header (the standard says that optionally is possible to omit the content-length, but in this implementation is sent) 
			for (String field : responseHeader) {
				sender.println(field);
			}
			sender.println(""); // end of header
			return log; // all was fine :)
		}
		return sendFileNotFound(); // file not found error
	}

	
	/**
	 * Centralized method which returns a header for the response file, the first line is the provided log.
	 * @param f Response file.
	 * @param log Response log.
	 * @return Header The header for the response.
	 */
	protected String[] getResponseHeader(File f, String log) {
		String header[] = {
				log,
				"Server: " + name,
				"Date: " + (new Date().toString()),
				"Content-type: " + URLConnection.guessContentTypeFromName(f.getName()),
				"Content-length: " + f.length(),
				"Last-modified: " + (new Date(f.lastModified()))
		};
		return header;
	}

	/**
	 * Centralized method to send responses.
	 * @param header The header of the response.
	 * @param reader The FileInputStream reader to read and send each byte.
	 * @return Log If all was fine returns the log in the first header line, otherwise return the 500 error log. 
	 */
	protected String sendResponse(String[] header, FileInputStream reader) {
		// send header
		for (String line: header) {
			sender.println(line);
		}
		sender.println(""); // blank line before body
		try {
			// send each byte of the file
			while((reader.available()) != 0) {
				out.write(reader.read());
			}
		} catch (IOException e) {
			// return sendInternalServer();
			// if an error occurs it may go into a loop...
			return HTTPv + " 500 Internal Server Error";
		}
		// returns the expected log if all went correctly.
		return header[0];
	}

	/**
	 * Method to read the request from the client and save it on an associative array.
	 * @return request The request as an associative array.
	 * @throws IOException If something happens during the listening process.
	 */
	protected Map<String, Object> getRequest() throws IOException {
		String inputLine = ""/*, body = ""*/;
		Map<String, Object> header = new HashMap<>(); // associative array
		// get header
		// get first line
		try {
			// the first line is composed by three elements separated from a white space
			String[] firstLine = receiver.readLine().split(" ");
			System.out.println("WS: REQUEST: " + firstLine[0] + " " + firstLine[1] + " " + firstLine[2]);
			header.put(METHOD, firstLine[0]);
			header.put(RESOURCE, firstLine[1]);
			header.put(PROTOCOL_VERSION, firstLine[2]);
		} catch(IndexOutOfBoundsException|NullPointerException ex) {
			return null; // couldn't read a valid request, bad format.
		}

		// read header fields (until blank line, which tells the end of the header)
		while(!(inputLine = receiver.readLine()).equals("")) {
			String[] line = inputLine.split(": ");
			header.put(line[0], line[1]);
		}
		// check for body (there is the field content-length in header)
		/*
		try {
			Integer bodySize = (Integer) header.get("Content-length");
			// TODO get body
		} catch (NullPointerException e) {} // there is no body
		catch (ClassCastException ex) {
			// way to tell that there is a bad request
			return null;
		}
		*/
		request = new HashMap<>();
		request.put(HEADER, header);
		//request.put("body", "");
		return request;
	}

	// ERROR RESPONSES

	/**
	 * 404: File Not Found<br>
	 * The server has not found anything matching the Request-URI.
	 * @return Log Response log
	 */
	protected String sendFileNotFound()
	{
		return sendError(HTTPv + " 404 FILE NOT FOUND", 404);
	}

	/**
	 * 400: Bad Request<br>
	 * The request could not be understood by the server due to malformed syntax.
	 * @return Log Response log
	 */
	protected String sendBadRequest() {
		return sendError(HTTPv + " 400 BAD REQUEST", 400);
	}

	/**
	 * 500: Internal Server Error<br>
	 * The server encountered an unexpected condition which prevented it from fulfilling the request.
	 * @return Log Response log
	 */
	protected String sendInternalServer() {
		return sendError(HTTPv + " 500 INTERNAL SERVER ERROR", 500);
	}
	
	/**
	 * 501: Not implemented error<br>
	 * The server does not support the functionality required to fulfill the request.
	 * @return Log Response log
	 */
	protected String sendNotImplemented() {
		return sendError(HTTPv + " 501 NOT IMPLEMENTED ERROR", 501);
	}

	/**
	 * Centralized method to send errors.
	 * @param log Response log
	 * @param statusCode Error code, used to retrieve the error html file inside the ERRORS folder.
	 * @return log Response log (should be the selected log, but if the error files was not found returns 500 I.S.E. log)
	 */
	protected String sendError(String log, int statusCode) {
		File f = new File(relativePath + "/" + errorsFolder + "/" + statusCode + ".html");
		String[] responseHeader = getResponseHeader(f, log);
		try {
			sendResponse(responseHeader, new FileInputStream(f));
		} catch (FileNotFoundException e) {
			// files has to be found
			System.out.println("WS: Somebody changed errors files position..");
			return HTTPv + "500 INTERNAL SERVER ERROR";
		}
		// error page correctly sent
		return log;
	}
	
	/**
	 * Method which allows to build at runtime an html file from the log file and send it to the client.
	 * @return Log Response log.
	 */
	protected String provideLogFileAsHtml() {
		// Using the changeable primitive to allow changes through parameters to log string
		ChangeablePrimitive<String> log = new ChangeablePrimitive<String>(""); 
		try {
			// send log as an html file
			logger.read(httpL, log);
		} catch (IOException e) {
			return sendInternalServer();
		}
		// return the response log
		return log.getValue();
	}

	/**
	 * This class implements the LoggerCallback interface to use the synchronized api of the logger to safely access to the log file. 
	 * @author Antonio Tepin
	 */
	protected class HTTPLoggerCallback implements LoggerCallback {
		/**
		 * The write method allows to add a log line to the log file.
		 */
		@Override
		public void write(BufferedWriter bw, Object... args) {
			try {
				String log = "DATE: " + new Date().toString() + "; IP: " + args[0] + "; RESPONSE: " + args[1];
				if (!args[2].equals("")) { log += "; RESOURCE: " + args[2]; }
				bw.write(log + "\n");
			} catch (IOException e) {}
		}
		/**
		 * The read method allows to build at runtime an html file from the log file and send it to the client.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void read(BufferedReader br, Object... args) {
			String logLine = "";
			ArrayList<String> response = new ArrayList<String>();
			int contentLengthPos = 1;
			// header
			response.add(HTTPv + " 200 OK"); // log
			// data
			response.add("0"); // represent content length.
			response.add("Server: " + HttpWorker.this.name);
			response.add("Date: " + new Date().toString());
			response.add("Content-type: text/html");
			response.add("Server: " + HttpWorker.this.name);
			response.add(""); // blank line
			// add response body.
			addLineInResponse(contentLengthPos, response, "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=" + logStylePath + "><title>Log File</title></head><body><table>");
			addLineInResponse(contentLengthPos, response, "<tr><th>DATE</th><th>IP</th><th>RESPONSE</th><th>RESOURCE</th></tr>");
			try {
				while((logLine = br.readLine()) != null) {
					// populate a template for each log.
					// a row represent a line in the log file
					String[] logRow = logLine.split("; ");
					addLineInResponse(contentLengthPos, response, "<tr>");
					for (String column : logRow) {
						addLineInResponse(contentLengthPos, response, "<td>" + column.split(": ")[1] + "</td>");
					}
					// to fill the table so all the table row has the same number of columns
					for (int i = 0; i < 4 - logRow.length; i++) {
						addLineInResponse(contentLengthPos, response, "<td> </td>");
					}
					addLineInResponse(contentLengthPos, response, "</tr>");
				}
				addLineInResponse(contentLengthPos, response, "</table></body></html>");
				// set content length
				response.set(contentLengthPos, "Content-length: " + response.get(contentLengthPos));
				boolean body = false;
				for (Iterator<String> iterator = response.iterator(); iterator.hasNext();) {
					String str = iterator.next();
					if(body) {
						// the body needs to be sent one byte at the time.
						for(byte b : str.getBytes()) {
							HttpWorker.this.out.write(b);
						}
					} else {
						HttpWorker.this.sender.println(str);
						if(str.equals("")) {
							// if a blank line was read, then the body starts.
							body = true;
						}
					}
				}
				
			} catch (IOException e) {
				((ChangeablePrimitive<String>) args[0]).setValue(sendInternalServer()); // if something went wrong sent I.S.E.
			}
			((ChangeablePrimitive<String>) args[0]).setValue(response.get(0)); // "returns" the expected log.
		}
		
		// method to centralized the line add
		private void addLineInResponse(int contentLengthPos, ArrayList<String> response, String newLine) {
			response.add(newLine); // add new line in response
			response.set(contentLengthPos, String.valueOf(Integer.parseInt(response.get(contentLengthPos)) + newLine.length())); // increment body length.
		}
	}
}