import java.util.Scanner;

import chat.server.ChatServer;
import http.server.HttpServer;

/**
 * Sockets application that provides an http server (reachable from a common browser) and a simple chat 
 * @author Antonio Terpin
 * @year 2016
 */

public class Application {
	private static int chatApplicationPort = 12345, httpServerPort = 9000;
	private static boolean launchChat = true, launchHTTP = true;
	private static String quitChat = "QUIT CHAT", quitHttp = "QUIT HTTP";
	
	/**
	 * <p>Application entry point, allows to launch chat and http server.<br>
	 * To quit the services, type QUIT CHAT or QUIT HTTP.</p>
	 * 
	 * <p>Enabled settings:<br>
	 * 1) [-http] : Launch only the chat application<br>
	 * 2) [-chat] : Launch only the http server<br>
	 * 3) [chat [port]] : Launch both the services, chat on selected port.<br>
	 * 4) [http [port]] : Launch both the services, http on selected port.<br>
	 * 5) [-http chat [port]] : Launch only the chat application on selected port.<br>
	 * 6) [-chat http [port]] : Launch only the http server on selected port.<br>
	 * 7) [chat [port] http [port]] : Launch both the services on selected port.<br>
	 * 8) [http [port] chat [port]] : Launch both the services on selected port.</p>
	 * 
	 * @param args Application settings.
	 */
	public static void main(String[] args) {
		ChatServer chat = null; 
		HttpServer http = null;
		Thread chatThread = null, httpThread = null;
		parseArgs(args);
		if(launchChat) {
			// run chat server on port [chatApplicationPort]
			chat = new ChatServer(chatApplicationPort);
			chatThread = new Thread(chat);
			// launch another thread, so server can do other things
			chatThread.start();
		}
		if(launchHTTP) {
			// run http server on port [httpServerPort]
			http = new HttpServer(httpServerPort);
			httpThread = new Thread(http);
			// launch another thread, so server can do other things
			httpThread.start();
		}
		
		// listening for commands: QUIT CHAT and QUIT HTTP to stop services
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(System.in);
		while(launchHTTP || launchChat) {
			String input = sc.nextLine();
			if(input.equals(quitChat) && chat != null) {
				launchChat = false;
				chat.stopRunning();
				chat = null;
				System.out.println("CHAT SERVICE CLOSED");
			} else if(input.equals(quitHttp) && http != null) {
				launchHTTP = false;
				http.running = false;
				http = null;
				System.out.println("HTTP SERVICE CLOSED");
			}
		}
		// application finished, no clients connected
		System.exit(0);
	}

	// try to get an integer from a string. If not possible returns null.
	private static Integer tryParse(String integer) {
		try {
			return Integer.parseInt(integer);
		} catch(NumberFormatException e) {
			return null;
		}
	}

	// This method reads from the command line arguments the application settings.
	private static void parseArgs(String[] args) {
		// parse parameters
		if (args.length > 0) {
			if (args.length > 1) {
				if (args.length < 3) {
					// is only one port changing
					Integer port = tryParse(args[1]); 
					if(port != null) {
						if (args[0].equals("chat")) { chatApplicationPort = port; }
						else if(args[0].equals("http")) { httpServerPort = port; }
					}
				}
				else if (args.length == 3) {
					// is one launch deleting and one port change
					Integer port = tryParse(args[2]);
					if(port != null) {
						if (args[1].equals("chat")) { chatApplicationPort = port; }
						else if(args[1].equals("http")) { httpServerPort = port; }
					}
				}
				else if (args.length == 4) {
					// is two port change
					Integer port1 = tryParse(args[1]), port2 = tryParse(args[3]);
					if(port1 != null) {
						if (args[0].equals("chat")) { chatApplicationPort = port1; }
						else if(args[0].equals("http")) { httpServerPort = port1; }
					}
					if(port2 != null) {
						if (args[2].equals("chat")) { chatApplicationPort = port2; }
						else if(args[2].equals("http")) { httpServerPort = port2; }
					}
				}
			}
			if (args[0].equals("-chat")) { launchChat = false; }
			if (args[0].equals("-http")) { launchHTTP = false; }
		}
	}
}
