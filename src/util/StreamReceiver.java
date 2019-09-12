package util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class StreamReceiver extends Receiver {
	
	private final OutputStream out;

	public StreamReceiver(OutputStream out) {
		this.out = out;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return out;
	}

	@Override
	public void setStatus(int sc) {
	}

	@Override
	public void setContentType(String contentType) {
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return new PrintWriter(out);
	}
	
}