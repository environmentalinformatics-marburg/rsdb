package server.api.vectordbs;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.Broker;
import vectordb.VectorDB;

public abstract class VectordbHandler {
private static final Logger log = LogManager.getLogger();
	
	protected static final String MIME_JSON = "application/json";

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
			log.error("HTTP method not allowed");
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("HTTP method not allowed");
			return;
		}	
	}
	
	public void handleGET(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		log.error("HTTP method not allowed");
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		response.setContentType("text/plain;charset=utf-8");
		response.getWriter().println("HTTP method not allowed");
		return;		
	}
	
	public void handlePOST(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		log.error("HTTP method not allowed");
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		response.setContentType("text/plain;charset=utf-8");
		response.getWriter().println("HTTP method not allowed");
		return;		
	}
	
	public void handleDELETE(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		log.error("HTTP method not allowed");
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		response.setContentType("text/plain;charset=utf-8");
		response.getWriter().println("HTTP method not allowed");
		return;		
	}


}
