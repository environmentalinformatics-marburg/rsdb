package server.api.main;

import java.io.IOException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import broker.Broker;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class APIHandler_public_wms extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();
	
	protected static final String MIME_JSON = "application/json";
	
	protected final Broker broker;
	
	private abstract static class IdHandler {
		public void handle(String id, String target, Request baseRequest, HttpServletResponse response) throws IOException {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("id");
			json.value(id);
			json.endObject();	
		}
	}
	
	private HashMap<String, IdHandler> idMap = new HashMap<String, IdHandler>();

	public APIHandler_public_wms(Broker broker) {
		this.broker = broker;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		baseRequest.setHandled(true);
		try {
			baseRequest.setHandled(true);
			if(target.equals("/")) {
				handleRoot(baseRequest, response);
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name: "+target);
				}			
				String name = i < 0 ? target.substring(1) : target.substring(1, i);
				String next = i < 0 ? "/" : target.substring(i);
				handleId(name, next, baseRequest, response);
			}
		} catch(Exception e) {
			e.printStackTrace();
			log.error(e);
			try {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("ERROR: " + e.getMessage());
			} catch(Exception e1) {
				log.warn(e1);
			}
		}		
	}

    private void handleRoot(Request baseRequest, HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		response.setContentType("text/plain;charset=utf-8");
		response.getWriter().println("missing id");		
	}
    
	private void handleId(String id, String next, Request baseRequest, HttpServletResponse response) throws IOException {		
		IdHandler idHandler = idMap.get(id);
		if(idHandler == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("ERROR: id not found");
			return;
		}
		idHandler.handle(id, next, baseRequest, response);			
	}
}
