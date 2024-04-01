package logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;

/**
 * Interface that has to be implemented by the loggerCallback needed to use the logger monitor.
 * @author Antonio Terpin
 */
public interface LoggerCallback {
	public void write(BufferedWriter bw, Object... args);
	public void read(BufferedReader br, Object... args);
}
