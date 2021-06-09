package server;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class Redirector extends AbstractHandler
{
	private final String redirect_target;
	public Redirector(String redirect_target) {
		this.redirect_target = redirect_target;
	}
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		response.setHeader(HttpHeader.LOCATION.asString(), redirect_target);
		response.setStatus(HttpServletResponse.SC_FOUND);
		response.setContentLength(0);
		baseRequest.setHandled(true);
	}
}
