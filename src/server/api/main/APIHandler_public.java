package server.api.main;

import java.io.IOException;
import java.util.HashMap;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import broker.Broker;
import broker.PublicAccess;
import broker.PublicAccess.RasterDbWCS;
import broker.PublicAccess.RasterDbWMS;
import broker.PublicAccess.VectorDbWFS;
import broker.PublicAccess.VectorDbWMS;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import rasterdb.RasterDB;
import server.api.rasterdb.RasterdbMethod_wcs;
import server.api.rasterdb.RasterdbMethod_wms;
import server.api.vectordbs.VectordbHandler_wfs;
import server.api.vectordbs.VectordbHandler_wms;
import vectordb.VectorDB;

public class APIHandler_public extends AbstractHandler {


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

	private static class RasterDBWmsProxyHandler extends PublicAccessHandler {

		private final RasterDB rasterdb;
		//private final Broker broker;
		private final RasterdbMethod_wms handler;

		public RasterDBWmsProxyHandler(Broker broker, RasterDB rasterdb) {
			//this.broker = broker;
			this.rasterdb = rasterdb;
			this.handler = new RasterdbMethod_wms(broker);
		}

		@Override
		public void handle(String id, String target, Request baseRequest, Response response) throws IOException {
			handler.handle(rasterdb, target, baseRequest, response, null);
		}		
	}

	private static class RasterDBWcsProxyHandler extends PublicAccessHandler {

		private final RasterDB rasterdb;
		//private final Broker broker;
		private final RasterdbMethod_wcs handler;

		public RasterDBWcsProxyHandler(Broker broker, RasterDB rasterdb) {
			//this.broker = broker;
			this.rasterdb = rasterdb;
			this.handler = new RasterdbMethod_wcs(broker);
		}

		@Override
		public void handle(String id, String target, Request baseRequest, Response response) throws IOException {
			handler.handle(rasterdb, target, baseRequest, response, null);
		}		
	}
	
	private static class VectorDBWfsProxyHandler extends PublicAccessHandler {

		private final VectorDB vecotordb;
		//private final Broker broker;
		private final VectordbHandler_wfs handler;

		public VectorDBWfsProxyHandler(Broker broker, VectorDB rasterdb) {
			//this.broker = broker;
			this.vecotordb = rasterdb;
			this.handler = new VectordbHandler_wfs(broker);
		}

		@Override
		public void handle(String id, String target, Request baseRequest, Response response) throws IOException {
			handler.handle(vecotordb, target, baseRequest, response, null);
		}		
	}
	
	private static class VectorDBWmsProxyHandler extends PublicAccessHandler {

		private final VectorDB vecotordb;
		//private final Broker broker;
		private final VectordbHandler_wms handler;

		public VectorDBWmsProxyHandler(Broker broker, VectorDB rasterdb) {
			//this.broker = broker;
			this.vecotordb = rasterdb;
			this.handler = new VectordbHandler_wms(broker);
		}

		@Override
		public void handle(String id, String target, Request baseRequest, Response response) throws IOException {
			handler.handle(vecotordb, target, baseRequest, response, null);
		}		
	}

	public APIHandler_public(Broker broker) {
		this.broker = broker;
		broker.publicAccessManager().changeListenerAdd(this::refresh);
		refresh();
	}

	public void refresh() {
		Logger.info("refresh");
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
			handler = new RasterDBWmsProxyHandler(broker, rasterdb);
			break;
		}
		case RasterDbWCS.TYPE: {
			RasterDbWCS p = (RasterDbWCS) publicAccess;
			RasterDB rasterdb = broker.getRasterdb(p.rasterdb);
			handler = new RasterDBWcsProxyHandler(broker, rasterdb);
			break;
		}
		case VectorDbWFS.TYPE: {
			VectorDbWFS p = (VectorDbWFS) publicAccess;
			VectorDB vectordb = broker.getVectorDB(p.vectordb);
			handler = new VectorDBWfsProxyHandler(broker, vectordb);
			break;
		}
		case VectorDbWMS.TYPE: {
			VectorDbWMS p = (VectorDbWMS) publicAccess;
			VectorDB vectordb = broker.getVectorDB(p.vectordb);
			handler = new VectorDBWmsProxyHandler(broker, vectordb);
			break;
		}
		default:
			Logger.warn("unknown type: ", publicAccess.type);
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
			Logger.error(e);
			try {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().println("ERROR: " + e.getMessage());
			} catch(Exception e1) {
				Logger.warn(e1);
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
