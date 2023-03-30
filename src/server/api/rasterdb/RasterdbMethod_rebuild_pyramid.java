package server.api.rasterdb;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

import broker.Broker;
import rasterdb.RasterDB;
import util.Web;

public class RasterdbMethod_rebuild_pyramid extends RasterdbMethod {
	

	public RasterdbMethod_rebuild_pyramid(Broker broker) {
		super(broker, "rebuild_pyramid");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		request.setHandled(true);
		try {
			rasterdb.rebuildPyramid(true);
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(Web.MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("result");
			json.value("rebuilt pyramid");
			json.endObject();
		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}		
	}

}