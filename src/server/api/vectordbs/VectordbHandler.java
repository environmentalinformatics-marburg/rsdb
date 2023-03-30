package server.api.vectordbs;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;
import util.Web;

import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.Broker;
import vectordb.VectorDB;

public abstract class VectordbHandler {

	protected final Broker broker;

	private String method;

	protected VectordbHandler(Broker broker, String method) {
		this.broker = broker;
		this.method = method;
	}
	
	public String getMethod() {
		return method;
	}
	
	public void handle(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		String method = request.getMethod();
		switch(method) {
		case "GET":
			handleGET(vectordb, target, request, response, userIdentity);
			return;
		case "POST":
			handlePOST(vectordb, target, request, response, userIdentity);
			return;
		case "DELETE":
			handleDELETE(vectordb, target, request, response, userIdentity);
			return;			
		default:
			Logger.error("HTTP method not allowed");
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			response.setContentType(Web.MIME_TEXT);
			response.getWriter().println("HTTP method not allowed");
			return;
		}	
	}
	
	public void handleGET(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		Logger.error("HTTP method not allowed");
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		response.setContentType(Web.MIME_TEXT);
		response.getWriter().println("HTTP method not allowed");
		return;		
	}
	
	public void handlePOST(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		Logger.error("HTTP method not allowed");
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		response.setContentType(Web.MIME_TEXT);
		response.getWriter().println("HTTP method not allowed");
		return;		
	}
	
	public void handleDELETE(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		Logger.error("HTTP method not allowed");
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		response.setContentType(Web.MIME_TEXT);
		response.getWriter().println("HTTP method not allowed");
		return;		
	}
}
