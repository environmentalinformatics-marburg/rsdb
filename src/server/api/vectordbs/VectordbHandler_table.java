package server.api.vectordbs;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;
import util.Web;

import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

import broker.Broker;
import vectordb.VectorDB;

public class VectordbHandler_table extends VectordbHandler {
	
	
	public VectordbHandler_table(Broker broker) {
		super(broker, "table.json");
	}

	@Override
	public void handleGET(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {		
		try {
			response.setStatus(HttpServletResponse.SC_OK);			
			response.setContentType(Web.MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			vectordb.writeTableJSON(json);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}		
	}
}
