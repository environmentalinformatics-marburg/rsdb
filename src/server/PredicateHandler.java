package server;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

public class PredicateHandler extends HandlerWrapper {

	public static interface RequestPredicate {
		boolean test(Request request);
	}

	private final RequestPredicate requestPredicate;

	public PredicateHandler(RequestPredicate requestPredicate) {
		this.requestPredicate = requestPredicate;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		if(requestPredicate.test(baseRequest)) {
			getHandler().handle(target, baseRequest, request, response);
		}
	}

}
