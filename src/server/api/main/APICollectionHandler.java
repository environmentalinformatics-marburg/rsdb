package server.api.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import server.api.APIHandler;
import util.Web;

public abstract class APICollectionHandler extends AbstractHandler {
	
	HashMap<String, APIHandler> methodMap = new HashMap<String, APIHandler>();

	protected void addMethod(APIHandler handler) {
		String name = handler.getAPIMethod();
		if(methodMap.containsKey(name)) {
			Logger.warn("method name already exists overwrite '"+name+"'  "+methodMap.get(name)+"  "+handler);
		}
		methodMap.put(name, handler);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		baseRequest.setHandled(true);

		String method = target;
		if(method.charAt(0)=='/') {
			method = target.substring(1);
		}
		int subIndex = method.indexOf('/');
		String subTarget = "";
		if(subIndex>=0) {
			subTarget = method.substring(subIndex + 1);	
			method = method.substring(0, subIndex);			
		}
		Logger.tag("API").info(Web.getRequestLogString("API",method,baseRequest));
		if(method.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("text/plain;charset=utf-8");
			PrintWriter writer = response.getWriter();
			writer.println("API: no method");
			writer.println("\navailable methods:\n");
			methodMap.keySet().stream().sorted().forEach(m->writer.println(m));
			return;
		}
		if(method.charAt(method.length()-1)=='/') {
			method = method.substring(0, method.length()-1);
		}
		//Logger.info("method: "+method);
		APIHandler handler = methodMap.get(method);
		if(handler==null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("unknown method "+method);
			return;
		}
		handler.handle(subTarget, baseRequest, request, response);
	}
}
