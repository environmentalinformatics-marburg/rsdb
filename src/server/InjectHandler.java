package server;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import util.Web;

public class InjectHandler extends AbstractHandler {
	

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
			Logger.tag("REQ").info(Web.getRequestLogString("REQ", baseRequest.getMethod() + "   " + baseRequest.getRequestURL().toString(), baseRequest));
		}
	}
}
