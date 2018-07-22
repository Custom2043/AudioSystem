package util;

public class Logger
{
	private static boolean debug = false;
	private static boolean warning = true;
	private static boolean error = true;
	private static boolean exceptionPrintStack = true;

	private Logger(){}

	public static synchronized void setLoggerProperties(boolean error, boolean warning, boolean debug, boolean exceptionPrintStack)
	{
		Logger.error = error;
		Logger.warning = warning;
		Logger.debug = debug;
		Logger.exceptionPrintStack = exceptionPrintStack;
	}

	public static synchronized void error(Exception exception)
	{
		privateError(exception.getMessage(), exception);
	}
	public static synchronized void error(String message)
	{
		privateError(message, null);
	}
	public static synchronized void error(String message, Exception exception)
	{
		privateError(message, exception);
	}

	private static synchronized void privateError(String message, Exception exception)
	{
		if (error)
		{
			System.err.flush();
			System.err.println("Error from class "+getCallingClass());
			System.err.println("    "+message);
			if (exception != null && Logger.exceptionPrintStack)
				exception.printStackTrace();
			System.err.flush();
		}
	}

	public static synchronized void warning(Exception exception)
	{
		privateWarning(exception.getMessage(), exception);
	}
	public static synchronized void warning(String message)
	{
		privateWarning(message, null);
	}
	public static synchronized void warning(String message, Exception exception)
	{
		privateWarning(message, exception);
	}

	private static synchronized void privateWarning(String message, Exception exception)
	{
		if (warning)
		{
			System.out.flush();
			System.out.println("/!\\ Warning from class "+getCallingClass());
			System.out.println("    "+message);
			System.out.flush();
			System.err.flush();
			if (exception != null && Logger.exceptionPrintStack)
				exception.printStackTrace();
			System.err.flush();
		}
	}

	public static synchronized void debug(Exception exception)
	{
		privateDebug(exception.getMessage(), exception);
	}
	public static synchronized void debug(String message)
	{
		privateDebug(message, null);
	}
	public static synchronized void debug(String message, Exception exception)
	{
		privateDebug(message, exception);
	}

	private static synchronized void privateDebug(String message, Exception exception)
	{
		if (debug)
		{
			System.out.flush();
			System.out.println("Debug Message from "+getCallingClass());
			System.out.println("    "+message);
			System.out.flush();
			System.err.flush();
			if (exception != null && Logger.exceptionPrintStack)
				exception.printStackTrace();
			System.err.flush();
		}
	}

	private static String getCallingClass()
	{
		return new Exception().getStackTrace()[3].toString();
	}
}
