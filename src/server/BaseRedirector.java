package server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class BaseRedirector extends AbstractHandler
{
	private final String redirect_target;
	public BaseRedirector(String redirect_target) {
		this.redirect_target = redirect_target;
	}
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		if(target.equals("/")) {
			response.setHeader(HttpHeader.LOCATION.asString(), redirect_target);
			response.setStatus(HttpServletResponse.SC_FOUND);
			response.setContentLength(0);
			baseRequest.setHandled(true);
		}
	}
}
