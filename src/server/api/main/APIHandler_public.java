package server.api.main;

import java.io.IOException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import broker.Broker;
import broker.PublicAccess;
import broker.PublicAccess.RasterDbWMS;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import rasterdb.RasterDB;
import server.api.rasterdb.RasterdbMethod_wms;

public class APIHandler_public extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();
	
	protected static final String MIME_JSON = "application/json";
	
	protected final Broker broker;
	
	private HashMap<String, PublicAccessHandler> handlerMap = new HashMap<String, PublicAccessHandler>();
	
	private abstract static class PublicAccessHandler {
		public void handle(String id, String target, Request baseRequest, Response response) throws IOException {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("id");
			json.value(id);
			json.endObject();	
		}
	}
	
	private static class WmsProxyHandler extends PublicAccessHandler {
		
		private final RasterDB rasterdb;
		private final Broker broker;
		private final RasterdbMethod_wms handler;
		
		public WmsProxyHandler(Broker broker, RasterDB rasterdb) {
			this.broker = broker;
			this.rasterdb = rasterdb;
			this.handler = new RasterdbMethod_wms(broker);
		}

		@Override
		public void handle(String id, String target, Request baseRequest, Response response) throws IOException {
			handler.handle(rasterdb, target, baseRequest, response, null);
		}		
	}
	
	public APIHandler_public(Broker broker) {
		this.broker = broker;
		broker.publicAccessManager().changeListenerAdd(this::refresh);
		refresh();
	}
	
	public void refresh() {
		log.info("refresh");
		HashMap<String, PublicAccessHandler> idMap = new HashMap<String, PublicAccessHandler>();
		broker.publicAccessManager().forEach((id, publicAccess) -> {
			PublicAccessHandler idHandler = toIdHandler(id, publicAccess);
			if(idHandler != null) {
				idMap.put(id, idHandler);
			}
		});
		this.handlerMap = idMap;
	}
	
	private PublicAccessHandler toIdHandler(String id, PublicAccess publicAccess) {
		PublicAccessHandler handler = null;
		switch(publicAccess.type) {
		case RasterDbWMS.TYPE: {
			RasterDbWMS p = (RasterDbWMS) publicAccess;
			RasterDB rasterdb = broker.getRasterdb(p.rasterdb);
			handler = new WmsProxyHandler(broker, rasterdb);
			break;
		}
		default:
			log.warn("unknown type: ", publicAccess.type);
		}
		return handler;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse httpResponse) throws IOException, ServletException {
		Response response = (Response) httpResponse;
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
				String sub = i < 0 ? "" : (i + 1 >= target.length() ? "" : target.substring(i + 1));
				handleId(name, sub, baseRequest, response);
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
    
	private void handleId(String id, String next, Request baseRequest, Response response) throws IOException {		
		PublicAccessHandler idHandler = handlerMap.get(id);
		if(idHandler == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			response.setContentType("text/plain;charset=utf-8");
			response.getWriter().println("ERROR: id not found");
			return;
		}
		idHandler.handle(id, next, baseRequest, response);			
	}
}
