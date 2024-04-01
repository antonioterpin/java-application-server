package chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Vector;

/**
 * Chat service which allows the client to join a global room where he can communicates with all the others clients.
 * @author Antonio Terpin
 * @year 2016
 */
public class BroadcastServer extends Server {
	/**
	 * Constructor matching Server constructor which allows to set clientSocket and chatServer
	 * @param clientSocket Client socket to provide socket connection.
	 * @param chatServer Chat server, useful in some cases.
	 */
	public BroadcastServer(Socket clientSocket, ChatServer chatServer) {
		super(clientSocket, chatServer);
	}

	private PrintWriter sender = null;
	private String username = null;
	
	/**
	 * Synchronized method which can be used by other clients to communicate with this.
	 * @param message Message to sent to this client.
	 */
	synchronized public void sendMessage(String message) {
		sender.println(message);
	}
	
	/**
	 * Method that can be used by other clients to retrieve this username. <br>
	 * This method owns the monitor on username, so it checks the effective username.
	 * @return username This client's username.
	 */
	public String getUsername() {
		synchronized (username) { return username; }
	}

	/**
	 * <p>Custom implementation of doJob which allows the client to join a global room where he can send message to other clients.</p>
	 * <p>Particular strings used are:<br>
	 * 1) ChatServer.getConn --> get number of active connections<br>
	 * 2) ChatServer.quit --> close connection<br>
	 * 3) ChatServer.changeUsername --> change username (messages arrives to other clients as from the selected username, no more from the IP)</p>
	 * <p>Example:<br> 
	 * type: USERNAME<br>
	 * Press enter.<br>
	 * type: Antonio Terpin<br>
	 * Press enter.<br>
	 * Now send a message: You will be seen as Antonio Terpin. :)</p>
	 */
	@Override
	protected void doJob() {
		try {
			sender = new PrintWriter(clientSocket.getOutputStream(), true);
			BufferedReader receiver = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String receivedString = "";
			// bulk operation to send broadcast messages
			BroadcastSender sendToAll = new BroadcastSender();
			// bulk operation to change username
			UsernameChanger changeUsername = new UsernameChanger();
			// while not quit or stopped by father
			while(!(receivedString = receiver.readLine()).equals(chatServer.quit) && running) {
				// write log
				chatServer.logger.write(chatServer.cb, 
						clientSocket.getInetAddress(), 
						receivedString,
						ChatServer.server_type.BROADCAST_SERVER.getName());
				// check if it is a special string
				if(receivedString.equals(chatServer.getConn)) {
					// tell the client the number of active connections (just for try, not really a good idea in a real implementation).
					sender.println(chatServer.getNumberOfConnections());
				}
				else if(receivedString.equals(chatServer.changeUsername)) {
					// change username
					chatServer.doBulkOperationOnSockets(changeUsername, receiver.readLine());
				}
				else {
					// if it isn't a special string, it is a message to all the others connected clients.
					chatServer.doBulkOperationOnSockets(sendToAll, receivedString);
				}
			}
			clientSocket.close(); // close connection
		} catch(IOException | NullPointerException ex) {}
		chatServer.disconnectClient(this); // tell the server this client is no more connected.
	}
	
	/**
	 * Implementation of BulkOperation which sends to all the connected clients the message string in the first argument.
	 * @author Antonio Terpin
	 */
	protected class BroadcastSender implements BulkOperation {
		@Override
		public void doOperation(Vector<Server> threads, Object... args) {
			for (Server s : threads) {
				// send to all the server of the type Broadcast exception of the sender 
				if(!s.equals(BroadcastServer.this) && s instanceof BroadcastServer) {
					String sender = (BroadcastServer.this.username == null)? 
							// the sender field is optionally the IP or the username of the sender socket
							BroadcastServer.this.clientSocket.getInetAddress().toString(): BroadcastServer.this.username;
					((BroadcastServer) s).sendMessage(sender + ": " + (String)args[0]);
				}
			}
		}
	}
	
	/**
	 * Implementation of BulkOperation which checks if the desired username is unique in this session and eventually updates it.
	 * @author Antonio Terpin
	 */
	protected class UsernameChanger implements BulkOperation {
		@Override
		public void doOperation(Vector<Server> threads, Object... args) {
			String newUsername = (String) args[0];
			for (Server s : threads) {
				// check all the server of the type Broadcast exception of the one who wants to change the username
				try {
					if(!s.equals(BroadcastServer.this) && s instanceof BroadcastServer && ((BroadcastServer) s).getUsername().equals(newUsername)) {
						BroadcastServer.this.sender.println("Username not available"); // tells the client that the selected username is no available.
						return; // if the username have been already assigned no update is performed.
					}
				} catch (Exception e) {}
			}
			// no other client has the same username
			BroadcastServer.this.username = newUsername; // update the username of the broadcast server client who performed this bulk operation. 
		}
	}
}
