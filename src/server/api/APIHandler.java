package server.api;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.AbstractHandler;

import broker.Broker;

public abstract class APIHandler extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();
	
	protected static final String MIME_JSON = "application/json";

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
			log.error(e.getClass().getSimpleName()+" "+e.getMessage());
			try {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.setContentType("text/plain;charset=utf-8");
				if(e instanceof RuntimeException) {
					response.getWriter().println(e.getMessage());
				} else {
					response.getWriter().println(e.getClass().getSimpleName()+":    "+e.getMessage());
				}
			} catch(Exception e1) {
				log.info(e1);
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
