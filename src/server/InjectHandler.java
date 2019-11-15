package server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class InjectHandler extends AbstractHandler {

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		response.setHeader("X-Robots-Tag", "noindex, nofollow");
		//response.setHeader("X-Frame-Options", "deny"); // deny all iframes
		response.setHeader("X-Frame-Options", "sameorigin"); // allow iframes on same site (needed for webfiles iframe)
		response.setHeader("Referrer-Policy", "no-referrer");
		response.setHeader("X-Content-Type-Options", "nosniff");
		if("127.0.0.1".equals(baseRequest.getRemoteAddr())) {
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setHeader("Access-Control-Allow-Headers", "content-type");
			if(baseRequest.getMethod().equals("OPTIONS")) {
				baseRequest.setHandled(true);
			}
		}
	}
}
