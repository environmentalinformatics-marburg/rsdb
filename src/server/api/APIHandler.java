package server.api;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.CarpException;
import util.Web;

import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.AbstractHandler;

import broker.Broker;

public abstract class APIHandler extends AbstractHandler {

	protected final Broker broker;

	private String apiMethod;

	protected APIHandler(Broker broker, String apiMethod) {
		this.broker = broker;
		this.apiMethod = apiMethod;
	}

	@Override
	public final void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			handle(target, baseRequest,(Response) response);
		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e.getClass().getSimpleName()+" "+e.getMessage());
			try {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.setContentType(Web.MIME_TEXT);
				if(e instanceof CarpException) {
					response.getWriter().println(e.getMessage());
				} else if(e instanceof RuntimeException) {
					Throwable cause = e.getCause();
					if(cause != null) {
						if(cause instanceof CarpException || cause instanceof RuntimeException) {
							response.getWriter().println(cause.getMessage());
						} else{
							response.getWriter().println(e.getClass().getSimpleName()+":    "+e.getMessage());
						}
					} else {
						response.getWriter().println(e.getMessage());
					}
				} else{
					response.getWriter().println(e.getClass().getSimpleName()+":    "+e.getMessage());
				}
			} catch(Exception e1) {
				Logger.info(e1);
			}
		}
	}

	protected abstract void handle(String target, Request request, Response response) throws IOException;

	public String getAPIMethod() {
		return apiMethod;
	}

	public Broker getBroker() {
		return broker;
	}
}
