package chat.server;

import java.io.IOException;
import java.net.Socket;

/**
 * Abstract class that each chat services MUST extend.
 * @author Antonio Terpin
 * @year 2016
 */
public abstract class Server implements Runnable {
	protected boolean running = true;
	/**
	 * This method allows to stop running
	 */
	public void stopRunning() {
		running = false;
		if (clientSocket != null) {
			try {
				clientSocket.close();
			} catch (IOException e) {}
		}
	}
	
	/**
	 * This method is called internally the run one.
	 */
	protected abstract void doJob();
	
	// client
	protected Socket clientSocket = null;
	// server
	protected ChatServer chatServer = null;
	
	/**
	 * Constructor that allows to set the client socket and the chat server for each chat service.
	 * @param clientSocket Client socket to provide socket connection.
	 * @param chatServer Chat server, useful in some cases.
	 */
	public Server(Socket clientSocket, ChatServer chatServer) {
		this.clientSocket = clientSocket;
		this.chatServer = chatServer;
	}
		
	/**
	 * Method that allows to set the client socket and the chat server for each chat service
	 * @param clientSocket Client socket to provide socket connection.
	 * @param chatServer Chat server, useful in some cases.
	 */
	public void setClientServer(Socket clientSocket, ChatServer chatServer){
		this.clientSocket = clientSocket;
		this.chatServer = chatServer;
	}
	
	/**
	 * When a thread starts this runnable, the doJob implementation of the service is called.
	 */
	@Override
	public void run() {
		doJob();
	}
	
}
