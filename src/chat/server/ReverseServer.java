package chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Chat service which allows the client to receive a reversed string from the server.
 * @author Antonio Terpin
 * @year 2016
 */
public class ReverseServer extends Server {
	/**
	 * Constructor matching Server constructor which allows to set clientSocket and chatServer
	 * @param clientSocket Client socket to provide socket connection.
	 * @param chatServer Chat server, useful in some cases.
	 */
	public ReverseServer(Socket clientSocket, ChatServer chatServer) {
		super(clientSocket, chatServer);
	}

	// get an input string and returns the reversed string
	private String reverseString(String message) {
		return new StringBuilder(message).reverse().toString();
	}
	
	/**
	 * <p>Custom implementation of doJob which allows the client to send every string he wants and get the reversed string as response.</p>
	 * <p>Particular strings used are:<br>
	 * 1) ChatServer.getConn --> get number of active connections<br>
	 * 2) ChatServer.quit --> close connection</p>
	 */
	@Override
	protected void doJob() {
		try {
			// get communication path
			PrintWriter sender = new PrintWriter(clientSocket.getOutputStream(), true);
			BufferedReader receiver = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String receivedString = "";
			// while not quit or stopped by father
			while(!(receivedString = receiver.readLine()).equals(chatServer.quit) && running) {
				// write log
				chatServer.logger.write(chatServer.cb, 
						clientSocket.getInetAddress(), // client's IP
						receivedString, // message sent from the client
						ChatServer.server_type.REVERSE_SERVER.getName()); // type of service the client sent the message
				if(receivedString.equals(chatServer.getConn)) {
					// tell the client the number of active connections (just for try, not really a good idea in a real implementation).
					sender.println(chatServer.getNumberOfConnections());
				}
				else {
					sender.println(reverseString(receivedString)); // send the string received, reversed.
				}
			}
			clientSocket.close(); // close connection
		} catch(IOException | NullPointerException ex) {}
		chatServer.disconnectClient(this); // tell the server this client is no more connected.
	}

}
