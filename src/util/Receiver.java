package util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public abstract class Receiver {
	public abstract OutputStream getOutputStream() throws IOException;
	public abstract void setStatus(int sc);
	public abstract void setContentType(String contentType);
	public abstract PrintWriter getWriter() throws IOException;
}