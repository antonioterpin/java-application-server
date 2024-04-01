package chat.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Vector;

import logger.Logger;
import logger.LoggerCallback;

/**
 * Chat server which listens for connections and forks a new different service, depends on what type of Server the client choose.
 * @author Antonio Terpin
 * @year 2016
 */
public class ChatServer implements Runnable {
	private int serverPort = 12345, // default server port
				numberOfMaximumConnections = 5, // maximum number of connections
				numberOfClientsSelectingService = 0; // number of clients which are selecting the service
	/**
	 * Set this to false to safely stop the thread.
	 */
	private boolean running = true;
	// vector of services clients are currently connected to
	private Vector<Server> connectedClients = new Vector<Server>();
	/**
	 * Instance of logger which provides synchronized access to the file "log/chatLog.txt"
	 */
	public Logger logger = new Logger("log/chatLog.txt");
	/**
	 * Logger callback used to perform synchronized operation on the log file.
	 */
	public ChatLoggerCallback cb = new ChatLoggerCallback();
	/**
	 * <p>Particular string which allows some functionality in the services that decide to provide it.<br>
	 */
	public final String quit = "QUIT", getConn = "CONN", changeUsername = "USERNAME", getLogs = "LOG";
	
	/**
	 * Constructor which allows to customize the port the server is running on.
	 * @param serverPort Port the server has to run on.
	 */
	public ChatServer(int serverPort) {
		this.serverPort = serverPort;
	}
	
	/**
	 * Method to retrieve the port the server is running on.
	 * @return serverPort Port the server is running on.
	 */
	public int getServerPort() {
		return serverPort;
	}
	
	
	// SYNCHRONIZED METHODS
	
	/**
	 * This method remove a server from connectedClients, in fact removing a server which has finished satisfying a client tells the chat server that the client is no more connected.
	 * @param server Server which has finished to satisfying the client.
	 */
	synchronized void disconnectClient(Server server) {
		connectedClients.remove(server);
		this.notify(); // in case this thread was sleeping, wake up it
	}
	
	/**
	 * This method add a server to connectedClients, in fact adding a server which has to satisfy a client tells the chat server that a client has been connected.
	 * @param server Server which has to satisfy the client.
	 */
	synchronized void connectClient(Server server) {
		connectedClients.addElement(server);
	}
	
	/**
	 * This method allows to retrieve the number of connected clients. 
	 * @return numberOfConnectedClients Number of connected clients.
	 */
	synchronized int getNumberOfConnections() {
		return connectedClients.size();
	}
	
	/**
	 * This method allows to understand if there are or not too much connections.
	 * @return evaluation Result of comparisons between the current number of connections (and the possible new connections, represented by the clients selecting a service) and the maximum number.
	 */
	synchronized boolean tooMuchConnections() {
		return connectedClients.size() + getNumberOfClientsSelectingService() >= numberOfMaximumConnections;
	}
	
	/**
	 * This method allows to perform an operation on each connected client.
	 * @param op A custom implementation of BulkOperation, the doOperation method will be call. 
	 * @param args Variable number of arguments that will be pass to the BulkOperation.doOperation method.
	 */
	synchronized void doBulkOperationOnSockets(BulkOperation op, Object... args) {
		op.doOperation(connectedClients, args); // using this approach the bulk operation is done inside a synchronized block
	}
	
	/**
	 * This method allows to increment correctly the number of clients which are selecting the service (they are not properly connected but they could).
	 */
	synchronized void doingRedirection() {
		numberOfClientsSelectingService++;
	}
	
	/**
	 * This method allows to decrement correctly the number of clients which are selecting the service (a client has selected a service or renounce to connect).
	 */
	synchronized void finishedRedirection() {
		numberOfClientsSelectingService--;
	}
	
	/**
	 * This method allows to get the correct number of clients which are selecting the service.
	 * @return numberOfClientsSelectingService The number of clients which are selecting the service.
	 */
	synchronized int getNumberOfClientsSelectingService() {
		return numberOfClientsSelectingService;
	}
	
	/** 
	 * This method close all connections and the service
	 */
	synchronized public void stopRunning() {
		running = false;
		doBulkOperationOnSockets(new BulkOperation() {
			
			@Override
			public void doOperation(Vector<Server> threads, Object... args) {
				for(Server t : threads) {
					t.stopRunning(); // stop thread safely
				}
				
			}
		}, (Object) null);
	}
	
	
	/**
	 * This method provide a welcoming message built at runtime to new connected clients.
	 * @return msg Welcoming message built at runtime.
	 */
	// welcoming message
	public String welcomingMessage() {
		String msg = "Welcome to my chat server\n\r"
				+ "Choose the type of chat you prefere, default is ECHO_SERVER.\n\r"
				+ "Options are:\n\r";
		// add every possible server type
		for (server_type s : server_type.values()) {
			msg += s.getName() + "\n\r";
		}
		return msg;
	}
	
	/**
	 * Implementation of Runnable interface.
	 * A welcoming socket which listens to connections and demands them to a redirector.
	 */
	@Override
	public void run() {
		ServerSocket welcomingSocket = null;
		try {
			welcomingSocket = new ServerSocket(getServerPort());
			System.out.println("Chat server running on port: " + getServerPort());
			// listen forever for connections
			while (running) {
				// get new connection
				Socket clientSocket = welcomingSocket.accept();
				// if still running
				if(running) {
					// check if the maximum number of connections has been reached
					if (tooMuchConnections()) {
						try {
							System.out.println("CS: Raggiunto il limite massimo di connessioni");
							// refuse connection
							clientSocket.close();
							// the thread must own his monitor
							synchronized(this) {
								this.wait(); // pause, no more connections until a client will be disconnected.
							}
						} catch (InterruptedException e) { e.printStackTrace(); } // it shouldn't happen
					}
					else {
						// redirecting the new connection to the requested server
						(new Thread(new Redirector(this, clientSocket))).start();
					}
				} else {
					clientSocket.close();
				}
			}
			if(welcomingSocket != null) {
				welcomingSocket.close(); // stop listening on port
			}
		} catch (IOException e) {
			// unable to create new server socket on [serverPort]
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * Set of all the type of service provided by the Chat Server
	 * @author Antonio Terpin
	 */
	public enum server_type {
		// all the chat type proposed to the clients
		ECHO_SERVER("ECHO_SERVER", EchoServer.class),
		REVERSE_SERVER("REVERSE_SERVER", ReverseServer.class),
		BROADCAST_SERVER("BROADCAST_SERVER", BroadcastServer.class);
		
		private String name;
		// use class to instance a new object every time (otherwise always the same object is used...)
		private Class<? extends Server> serverClass; // the class MUST extends Server
		
		/**
		 * Constructor which allows to choose a name and a class for the server_type.
		 * @param name The name of the chat service.
		 * @param serverClass The class that provides a chat service and extends Server.
		 */
		server_type(String name, Class<? extends Server> serverClass) {
			this.name = name;
			this.serverClass = serverClass;
		}
		
		/**
		 * Only-read access to server_type name.
		 * @return name Server name
		 */
		public String getName() {
			return name;
		}
		
		/**
		 * Only-read access to server_type class.
		 * @return serverClass Server class
		 */
		public Class<? extends Server> getServer() {
			return serverClass;
		}
		
		/**
		 * This method allows to retrieve, if possible, a server_type with the searched name.
		 * @param name The name of the server_type to search
		 * @return serverType The enumeration entry if exists, otherwise returns null.
		 */
		public static server_type fromString(String name) {
			if (name != null) {
				for (server_type s : server_type.values()) {
					if(name.equalsIgnoreCase(s.name)) {
						return s;
					}
				}
			}
			return null;
		}
	}
	
	/**
	 * This class implements the LoggerCallback interface to use the synchronized api of the logger to safely access to the log file. 
	 * @author Antonio Tepin
	 */
	public class ChatLoggerCallback implements LoggerCallback {
		/**
		 * The write method allows to add a log line to the log file.
		 */
		@Override
		public void write(BufferedWriter bw, Object... args) {
			try {
				bw.write("DATE: " + new Date().toString() + "; FROM: " + args[0] + "; TO: " + args[2] + "; MESSAGE: " + args[1] + "\n");
			} catch (IOException e) {}
		}
		/**
		 * The read method sends the socket, retrieved from the first argument, each line of the log file.
		 */
		@Override
		public void read(BufferedReader br, Object... args) {
			String logLine = null;
			PrintWriter sender = (PrintWriter) args[0];
			try {
				while ((logLine = br.readLine()) != null) {
					sender.println(logLine);
				}
			} catch (IOException e) {}
		}
	}
	
	/**
	 * The redirector proposes the clientSocket the various choices through the welcoming message and instantiates the right chat service to satisfy the client.  
	 * @author Antonio Terpin
	 */
	private class Redirector implements Runnable {
		Socket clientSocket;
		ChatServer server;
		
		/**
		 * Constructor which allows to set the chatServer and the clientSocket
		 * @param server The chat server the service will reference to.
		 * @param clientSocket The connected client.
		 */
		public Redirector(ChatServer server, Socket clientSocket) {
			this.clientSocket = clientSocket;
			this.server = server;
		}
		
		/**
		 * Implementation of interface Runnable which proposes to clientSocket various choices through the welcoming message and listens to a client response.
		 * Then understands what is the client response and instantiates the selected service connected to the client.
		 * If the client sends a request for a service not available, echo server is selected as default.
		 */
		@Override
		public void run() {
			ChatServer.this.doingRedirection(); // start the redirection operation
			Server new_service = null;
			try {
				// to communicate with the client
				PrintWriter sender = new PrintWriter(clientSocket.getOutputStream(), true);
				BufferedReader receiver = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				
				// tell the user between which server he can choose
				sender.println(welcomingMessage());
				// get choosed server
				server_type choosedServer = server_type.fromString(receiver.readLine());
				if (choosedServer == null) {
					choosedServer = server_type.ECHO_SERVER; // default server
				}
				System.out.println("CS: Selected server: " + choosedServer.getName());
				// send feedback
				sender.println("Selected server: " + choosedServer.getName());
				// satisfy the user (start the service)
				new_service = (Server) choosedServer.getServer().getConstructor(Socket.class, ChatServer.class).newInstance(clientSocket, server);
				Thread newServer = new Thread(new_service);
				newServer.start();
				// add client to connected clients
				connectClient(new_service);
			} catch (Exception e) {} // if something occurs, do nothing
			ChatServer.this.finishedRedirection(); // the redirection operation has been finished
		}
	}
}
