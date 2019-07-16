package server.api.pointdb;

import java.io.IOException;
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

import broker.Broker;
import util.Web;

public class APICollectionHandler extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();
	public static final Marker MARKER_API = MarkerManager.getMarker("API");

	HashMap<String, PointdbAPIHandler> methodMap = new HashMap<String, PointdbAPIHandler>();

	public APICollectionHandler(Broker broker) {
		addMethodAndAliases(new APIHandler_query(broker), "points.rdat", "points.xyz", "points.las", "points.zip");
		addMethodAndAliases(new APIHandler_query_raster(broker), "raster.rdat", "raster.tiff", "raster.png");
		addMethodAndAliases(new APIHandler_info(broker), "info.json");
		addMethod(new APIHandler_image(broker));
		addMethod(new APIHandler_map(broker));
		addMethod(new APIHandler_tile_meta(broker));
		addMethod(new APIHandler_polygon(broker));
		addMethod(new APIHandler_dbs_json(broker));
		addMethod(new APIHandler_feature(broker));
		addMethod(new APIHandler_features(broker));
		addMethod(new APIHandler_dtm(broker));
		addMethod(new APIHandler_dsm(broker));
		addMethod(new APIHandler_chm(broker));
		addMethod(new APIHandler_process(broker));
		addMethod(new APIHandler_process_functions(broker));
	}
	
	private void addMethodAndAliases(PointdbAPIHandler handler, String ...aliases) {
		addMethod(handler, handler.getAPIMethod());
		for(String alias:aliases) {
			addMethod(handler, alias);
		}
	}
	
	
	private void addMethod(PointdbAPIHandler handler) {
		addMethod(handler, handler.getAPIMethod());
	}
	
	private void addMethod(PointdbAPIHandler handler, String apiMethod) {
		String name = apiMethod;
		if(methodMap.containsKey(name)) {
			log.warn("method name already exists overwrite '"+name+"'  "+methodMap.get(name)+"  "+handler);
		}
		methodMap.put(name, handler);
	}


	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		baseRequest.setHandled(true);
		//log.info("pointDB request "+target);
		String method = target;
		if(method.charAt(0)=='/') {
			method = target.substring(1);
		}
		log.info(MARKER_API, Web.getRequestLogString("API",method,baseRequest));
		if(method.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("PointDB");			
			return;
		}
		if(method.charAt(method.length()-1)=='/') {
			method = method.substring(0, method.length()-1);
		}
		//log.info("method: "+method);
		PointdbAPIHandler handler = methodMap.get(method);
		if(handler==null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("unknown method "+method);
			return;
		}
		handler.handle(target, baseRequest, request, response);
	}
}
