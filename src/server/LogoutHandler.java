package server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import broker.Broker;

public class LogoutHandler extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;

	public LogoutHandler(Broker broker) {		
		this.broker = broker;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {		
		baseRequest.setHandled(true);
		HttpSession session = request.getSession(false);
		if(session != null) {
			session.invalidate();
		}
		response.setHeader("Set-Cookie", "session=logout; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT");
		
		String loc = "/entrypoint";
		String ref = request.getParameter("ref");
		log.info("ref " + ref);
		if(ref != null) {
			loc = ref;
		}
		
		response.setHeader(HttpHeader.LOCATION.asString(), loc);
		response.setStatus(HttpServletResponse.SC_FOUND);
		response.setContentLength(0);
	}

}
