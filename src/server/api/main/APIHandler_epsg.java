package server.api.main;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.tinylog.Logger;

import broker.Broker;
import jakarta.servlet.http.HttpServletResponse;
import pointcloud.Importer;
import server.api.APIHandler;
import util.Web;

public class APIHandler_epsg extends APIHandler {

	public APIHandler_epsg(Broker broker) {
		super(broker, "epsg");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		UserIdentity userIdentity = Web.getUserIdentity(request);
		try {
			if(target.isEmpty() || target.equals("/")) {
				handleRoot(request, response, userIdentity);
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name in: "+target);
				}			
				//String name = i < 0 ? target.substring(1) : target.substring(1, i);
				//String next = i < 0 ? "/" : target.substring(i);
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.setContentType(Web.MIME_TEXT);
			}
		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			response.setContentType(Web.MIME_TEXT);
			response.getWriter().println(e);
		}
	}

	private void handleRoot(Request request, HttpServletResponse response, UserIdentity userIdentity) throws IOException {		
		String crsText = Web.getString(request, "crs");		
		CoordinateReferenceSystem reqCRS = Importer.CRS_FACTORY.createFromName(crsText);
		if(reqCRS != null) {
			String reqProj4 = reqCRS.getParameterString();
			if(reqProj4 != null && !reqProj4.isEmpty()) {
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType(Web.MIME_JSON);
				JSONWriter json = new JSONWriter(response.getWriter());
				json.object();
				json.key("crs");
				json.value(crsText);
				json.key("proj4");
				json.value(reqProj4);
				json.endObject();
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.setContentType(Web.MIME_TEXT);
			}
		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			response.setContentType(Web.MIME_TEXT);
		}
	}
}
