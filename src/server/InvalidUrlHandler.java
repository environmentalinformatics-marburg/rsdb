package server;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.Web;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class InvalidUrlHandler extends AbstractHandler {
	
	private final String message;
	
	public InvalidUrlHandler(String message) {
		this.message = message;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		baseRequest.setHandled(true);
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		response.setContentType(Web.MIME_HTML);
		//response.setHeader("Server", "");
		//response.setHeader("Date", null);
		PrintWriter writer = response.getWriter();
		writer.print("<!DOCTYPE html>");
		writer.print("<html lang=\"en\">");
		writer.print("<head>");
		writer.print("<meta charset=\"utf-8\">");
		writer.print("<meta name=\"robots\" content=\"noindex\" />");
		writer.print("<title>");
		writer.print(message);
		writer.print("</title>");
		writer.print("</head>");
		writer.print("<body>");
		writer.print(message);
		writer.print("</body>");
		writer.print("</html>");		
	}

}
