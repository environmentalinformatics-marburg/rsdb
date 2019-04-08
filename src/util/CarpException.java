package util;

// http://stackoverflow.com/questions/727628/how-do-i-throw-an-exception-from-the-callers-scope
public class CarpException extends RuntimeException {
	public CarpException(String message) {
		super(message);
	}

	@Override
	public Throwable fillInStackTrace() {
		super.fillInStackTrace();
		StackTraceElement[] origStackTrace = getStackTrace();
		StackTraceElement[] newStackTrace = new StackTraceElement[origStackTrace.length - 1];
		System.arraycopy(origStackTrace, 1, newStackTrace, 0, origStackTrace.length - 1);
		setStackTrace(newStackTrace);
		return this;
	}
}