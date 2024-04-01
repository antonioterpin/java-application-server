package http.server;

import java.io.*;
import java.net.*;

import logger.Logger;

/**
 * HTTP server which listens for connections and forks a new HttpWorker at each connection.
 * @author Antonio Terpin
 */
public class HttpServer implements Runnable {
	private int serverPort = 9000;
	private Logger logger = new Logger("log/httpLog.txt"); // instance of logger
	public boolean running = true;
	
	/**
	 * Constructor which allows to choose the port the server has to run on. Default is 8080.
	 * @param serverPort Port the server has to run on.
	 */
	public HttpServer(int serverPort) {
		this.serverPort = serverPort;
	}
	
	/**
	 * Method to get the port the service is running on. 
	 * @return serverPort Port the service is running on.
	 */
	public int getServerPort() {
		return serverPort;
	}
	
	/**
	 * Implementation of Runnable interface.<br>
	 * A welcoming socket which listens for incoming connections and demands them to an HttpWorker.
	 */
	@Override
	public void run() {
		ServerSocket serverSocket = null;
		try {
			// create welcoming socket
			serverSocket = new ServerSocket(getServerPort());
			System.out.println("HTTP server running on port: " + getServerPort());
			
			// listen for connections
			Socket clientSocket = null; 
			System.out.println ("WS: Waiting for connection.....");
			try { 
				while(running)
				{
					// connect to client
					clientSocket = serverSocket.accept();
					// if still running
					if(running) {
						// satisfy client
						HttpWorker w = new HttpWorker(clientSocket, logger);
						Thread t = new Thread(w);
						t.start();
					} else {
						clientSocket.close();
					}
				}
			} 
			catch (IOException e) 
			{ 
				System.err.println("WS: Accept failed."); 
			} 
			try {
				serverSocket.close();
			} catch (IOException e) {
				System.out.println("WS: Error turning off the server");
			} 
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
}
