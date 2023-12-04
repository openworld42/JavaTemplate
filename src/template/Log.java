
/**
 * Copyright 2020 Heinz Silberbauer
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package template;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

/**
 * Logger, a simple logging utility, could be a wrapper for java.util.Logger or log4j, 
 * you may be change to it in the future, of course.
 * 
 * An application may call Util.renameToBackupFile() to save older log files.
 * Any severe() call will flush the log to the log file, to ensure exceptions or severe errors
 * are persisted.
 * 
 * Note: each application has to call init() before writing anything to the log and to
 * call close() before the exit to save pending log entries.
 */
public class Log {

	/** abbreviation for the line separator */
	public static final String lineSep = System.lineSeparator();
	/** the data format of logging messages */
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");

	/** the Log singleton instance */
	private static final Log instance = new Log();
	/** the underlying logger, default: the system logger */
	private static Logger logger;
	/** the handler for the logger, default: a file handler */
	private static FileHandler fileHandler;
	/** the current number of logging messages */
	private static int logCount;
	/** if true, error a displayed to the console too */
	private static boolean logExeptionsToConsole;

	/** the logging file name */
	private static String logFilename;

	/**
	 * Deny external construction, singleton.
	 */
	private Log() {
		
		logger = Logger.getLogger("");			// take the default logger
	}

	/**
	 * Closes the log file.
	 * 
	 * Note: each application HAS to close the logger before exit to save pending log entries.
	 */
	public static void close() {

		if (fileHandler != null) {
			fileHandler.close();
		}
	}

	/**
	 * Overwrite this method to change the logging prefix for log entries.
	 * 
	 * @param record 		the <code>LogRecord</code> to log
	 * 
	 * @return the prefix of a log entry
	 */
	public static String createLogEntry(LogRecord record) {

		StringBuffer sb = new StringBuffer(dateFormat.format(new Date()));
		String level = record.getLevel().getName();
		sb.append(level);
		for (int i = level.length(); i < 8; i++) {
			sb.append(' ');
		}
		sb.append(record.getMessage());
		return sb.toString();
	}

	/**
	 * Logs an error with the Exception and the stack trace.
	 *
	 * @param e			the Exception to log
	 */
	public static void exception(Exception e) {

		severe("", e);
	}

	/**
	 * Flush the log file if dirty, so all log content is persistent.
	 */
	public static void flush() {

		if (fileHandler != null) {
			fileHandler.flush();
		}
	}

	/**
	 * @return the log file name
	 */
	public static String getFileName() {

		return logFilename;
	}

	/**
	 * @return the number of log entries
	 */
	public static int getLogCount() {

		return logCount;
	}

	/**
	 * Log an information message.
	 * 
	 * @param message		the information message
	 */
	public static void info(String message) {

		logger.info(message);
		logCount++;
	}

	/**
	 * Log an information message looking like "MyClass: _message_".
	 * 
	 * @param clazz			a calling class 
	 * @param message		the information message
	 */
	public static void info(Class<?> clazz, String message) {

		info(clazz.getSimpleName() + ": " + message);
	}

	/**
	 * Initializes (opens) a log file, using an internal <code>FileHandler</code> for logging.
	 * An application has to call <code>init()</code> before writing anything to the log.
	 * 
	 * @param logFilename		the name of the file to log
	 */
	public static void init(String logFilename) {

		try {
			for (Handler h : logger.getHandlers()) {
				logger.removeHandler(h);
			}
			fileHandler = new FileHandler(logFilename);
			logger.addHandler(fileHandler);
			fileHandler.setFormatter(new Formatter() {
				@Override
				public String format(LogRecord record) {
					return createLogEntry(record);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error initilizing Logger: " + e.getMessage());
		}
	}

	/**(
	 * Initializes logging by using a customized <code>Logger</code>.
	 * An application has to call <code>init()</code> before writing anything to the log.
	 * 
	 * @param customLogger		the <code>Logger</code> to log
	 */
	public static void init(Logger customLogger) {

		logger = customLogger;
	}

	/**
	 * @param logExeptionsToConsole if true, any errors will also be written to System.err
	 */
	public static void logExeptionsToConsole(boolean logExeptionsToConsole) {

		Log.logExeptionsToConsole = logExeptionsToConsole;
	}

	/**
	 * Log a severe error.
	 * 
	 * @param message		the error message
	 */
	public static void severe(String message) {

		logger.severe(message);
		if (fileHandler != null) {
			fileHandler.flush();
		}
	}

	/**
	 * Log a severe error looking like "MyClass: _message_".
	 * 
	 * @param clazz			the calling class 
	 * @param message		the error message
	 */
	public static void severe(Class<?> clazz, String message) {

		severe(clazz.getSimpleName() + ": " + message);
	}

	/**
	 * Log a severe error, usually caused by an exception.
	 * 
	 * @param message		the error message
	 * @param e				the Throwable to display for the stacktrace
	 */
	public static void severe(String message, Throwable e) {

		String stackTrace = Util.stackTraceToString(e);
		logger.severe(message);
		logger.severe(stackTrace);
		if (fileHandler != null) {
			fileHandler.flush();
		}
		logCount++;
		if (logExeptionsToConsole) {
			System.err.println(lineSep + message + " " + e.getMessage() + lineSep);
			e.printStackTrace();
		}
	}

	/**
	 * Logs the end of a time measurement.
	 *
	 * @param obj		either a message string or the calling object takes the simple class name
	 * @param start		the time when the measurement started (System.currentTimeMillis())
	 */
	public static void timestampEnd(Object obj, long start) {

		long duration = System.currentTimeMillis() - start;
		if (obj instanceof String) {
			info((obj.toString() + " duration " + duration + "ms"));
		} else {
			info((obj.getClass().getSimpleName() + " duration " + duration + "ms"));
		}
	}

	/**
	 * Log a warning message.
	 *
	 * @param message		the warning message
	 */
	public static void warning(String message) {

		logger.warning(message);
		logCount++;
	}

	/**
	 * Log an warning message looking like "MyClass: _message_".
	 * 
	 * @param clazz			a calling class 
	 * @param message		the warning message
	 */
	public static void warning(Class<?> clazz, String message) {

		warning(clazz.getSimpleName() + ": " + message);
	}
}
