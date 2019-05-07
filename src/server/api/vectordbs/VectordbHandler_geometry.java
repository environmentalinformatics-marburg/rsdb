package server.api.vectordbs;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.Broker;
import util.Web;
import vectordb.VectorDB;

public class VectordbHandler_geometry extends VectordbHandler {
	private static final Logger log = LogManager.getLogger();
	
	public VectordbHandler_geometry(Broker broker) {
		super(broker, "geometry.json");
	}

	@Override
	public void handleGET(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {		
		try {
			int epsg = Web.getInt(request, "epsg", -1);
			//String geometry = vectordb.getGeoJSONAsCollection(epsg);
			String geometry = vectordb.getGeoJSON(epsg);
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/geo+json");
			response.getWriter().print(geometry);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}		
	}
}
