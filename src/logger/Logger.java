package logger;

import java.io.*;

/**
 * Logger class which provides a monitored access to a text file.<br>
 * It allows to do reading and writing customized operation without synchronization errors.
 * @author Antonio Terpin
 */
public class Logger {
	private String logFilePath = "log.txt";
	private File logFile;
	
	public Logger() {
		openOrCreateFile();
	}
	
	/**
	 * Constructor to define which file the logger acts on.<br>
	 * Default logFilePath is "./log.txt".
	 * @param logFilePath File path.
	 */
	public Logger(String logFilePath) {
		if(!logFilePath.endsWith("/")) {
			this.logFilePath = logFilePath;
		}
		openOrCreateFile();
	}
	
	// open or, if the file doesn't exist, creates it.
	// This way prevent to open the file thousands of times in a very inefficient way!
	private void openOrCreateFile() {
		logFile = new File(logFilePath);
		// creates the file if it doesn't exist
		if(!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {}
		}
	}
	
	// synchronized access to log file (manages multiple accesses)
	
	/**
	 * This method allows to perform the reading operations defined in the LoggerCallback implementation of read.<br>
	 * Provides to the object interfaced to the LoggerCallback a buffered reader and an undefined numbers of Object parameters.
	 * @param loggerCallback Object which implements the interface LoggerCallback, the read method is called.
	 * @param args Undefined number of Object parameters to pass to loggerCallback read method.
	 * @throws IOException when some errors occurs during reading operation. Shouldn't happen :)
	 */
	synchronized public void read(LoggerCallback lc, Object... args) throws IOException {
		// creates the buffered reader
		BufferedReader br = new BufferedReader(new FileReader(logFile));
		// call logger callback read with the buffered reader
		lc.read(br, args);
		// then close the buffered reader
		br.close();
	}
	
	/**
	 * This method allows to perform the writing operations defined in the LoggerCallback implementation of write.<br>
	 * Provides to the object interfaced to the LoggerCallback a buffered writer and an undefined numbers of Object parameters.
	 * @param loggerCallback Object which implements the interface LoggerCallback, the write method is called.
	 * @param args Undefined number of Object parameters to pass to loggerCallback read method.
	 * @throws IOException when some errors occurs during writing operation. Shouldn't happen :)
	 */
	synchronized public void write(LoggerCallback lc, Object... args) throws IOException {
		// creates the buffered writer
		BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true));
		// call logger callback write with the buffered writer
		lc.write(bw, args);
		// then close the buffered reader
		bw.close();
	}
}
