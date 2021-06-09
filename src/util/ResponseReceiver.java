package util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletResponse;

public class ResponseReceiver extends Receiver {
	private final HttpServletResponse response;
	
	public ResponseReceiver(HttpServletResponse response) {
		this.response = response;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return response.getOutputStream();
	}

	@Override
	public void setStatus(int sc) {
		response.setStatus(sc);
		
	}

	@Override
	public void setContentType(String contentType) {
		response.setContentType(contentType);			
	}
	
	@Override
	public void setContentLength(long len) {
		response.setContentLengthLong(len);		
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return response.getWriter();
	}
}