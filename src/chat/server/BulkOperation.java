package chat.server;

import java.util.Vector;

/**
 * Interface used to perform operation on multiple chat services in a safe (synchronized) way.
 * @author Antonio Terpin
 * @year 2016
 */
public interface BulkOperation {
	/**
	 * Implements this method to perform an operation on multiple threads.
	 * @param threads The chat services for the connected clients.
	 * @param args Other parameters the method needs.
	 */
	public void doOperation(Vector<Server> threads, Object... args);
}
