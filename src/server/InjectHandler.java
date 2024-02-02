package server;

import java.io.IOException;
import java.time.LocalDateTime;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.AbstractHandler;

import util.Web;

public class InjectHandler extends AbstractHandler {

	private static final TaggedLogger log = Logger.tag("REQ");

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		if(request.getAttribute("injected") == null) {
			request.setAttribute("injected", "injected");

			response.setHeader("X-Robots-Tag", "noindex, nofollow");

			response.setHeader("Referrer-Policy", "no-referrer");
			response.setHeader("X-Content-Type-Options", "nosniff");
			if("127.0.0.1".equals(baseRequest.getRemoteAddr())  || "[0:0:0:0:0:0:0:1]".equals(baseRequest.getRemoteAddr())) {
				response.setHeader("Access-Control-Allow-Origin", "*");
				response.setHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST, DELETE");
				response.setHeader("Access-Control-Allow-Headers", "content-type");
				if(baseRequest.getMethod().equals("OPTIONS")) {
					baseRequest.setHandled(true);
				}
			} else {
				//response.setHeader("X-Frame-Options", "deny"); // deny all iframes
				//response.setHeader("X-Frame-Options", "sameorigin"); // allow iframes on same site (needed for webfiles iframe)
			}
			/*Enumeration<String> hs = request.getHeaderNames();
		while(hs.hasMoreElements()) {
			String h = hs.nextElement();
			Logger.info("header   " + h + ": " + request.getHeader(h));
		}*/
			Logger.info(Web.getRequestLogString("REQ", baseRequest.getMethod() + "   " + baseRequest.getRequestURL().toString(), baseRequest));
			log.trace(getRequestLogString("REQ", baseRequest.getMethod() + "   " + baseRequest.getRequestURL().toString(), baseRequest));
		}
	}
	
	private static StringBuilder getRequestLogString(String handlerText, String target, Request request) {
		String user = "?";
		UserIdentity userIdentity = Web.getUserIdentity(request);
		if(userIdentity!=null) {
			//Logger.info("userIdentity "+userIdentity);
			user = userIdentity.getUserPrincipal().getName();
		}
		StringBuilder s = new StringBuilder();
		s.append(Web.timestampText(LocalDateTime.now()));
		s.append(" ");
		s.append(user);
		s.append(" ");
		s.append(request.getRemoteAddr());
		s.append(" ");
		s.append(target);
		String qs = request.getQueryString();
		if(qs != null) {
			s.append("?");
			s.append(request.getQueryString());
		}
		String referer = request.getHeader("Referer");
		if(referer!=null) {
			s.append("\t\tref ");
			s.append(referer);
		}
		return s;		
	}
}
