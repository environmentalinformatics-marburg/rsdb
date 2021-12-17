package server.api.vectordbs;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.Broker;
import util.Web;
import vectordb.VectorDB;

public class VectordbHandler_geometry extends VectordbHandler {
	
	
	public VectordbHandler_geometry(Broker broker) {
		super(broker, "geometry.json");
	}

	@Override
	public void handleGET(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {		
		try {
			boolean just_name_attribute = Web.getFlagBoolean(request, "just_name_attribute");
			int epsg = Web.getInt(request, "epsg", -1);
			if(request.getParameter("epsg") != null && epsg == -1) {
				throw new RuntimeException("epsg paremeter expects a numeric epsg code. example: '3857'");
			}
			//String geometry = vectordb.getGeoJSONAsCollection(epsg);
			String geometry = vectordb.getGeoJSON(epsg, just_name_attribute);
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/geo+json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().print(geometry);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}		
	}
}
