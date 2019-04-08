package server.api.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import server.api.APIHandler;
import util.Web;

public abstract class APICollectionHandler extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();
	public static final Marker MARKER_API = MarkerManager.getMarker("API");

	HashMap<String, APIHandler> methodMap = new HashMap<String, APIHandler>();

	protected void addMethod(APIHandler handler) {
		String name = handler.getAPIMethod();
		if(methodMap.containsKey(name)) {
			log.warn("method name already exists overwrite '"+name+"'  "+methodMap.get(name)+"  "+handler);
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
		log.info(MARKER_API, Web.getRequestLogString("API",method,baseRequest));
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
		//log.info("method: "+method);
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
