package server.api.main;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;
import org.tinylog.Logger;

import broker.Broker;
import broker.catalog.CatalogKey;
import jakarta.servlet.http.HttpServletResponse;
import postgis.PostgisLayerManager;
import rasterdb.RasterDB;
import server.api.APIHandler;
import util.JsonUtil;
import util.Web;


public class APIHandler_layers extends APIHandler {

	private PostgisLayerManager postgisLayerManager;

	public APIHandler_layers(Broker broker) {
		super(broker, "layers");
		this.postgisLayerManager = broker.postgisLayerManager();
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		UserIdentity userIdentity = Web.getUserIdentity(request);
		try {
			if(target.isEmpty() || target.equals("/")) {
				handleList(request, response, userIdentity);
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

	private void handleList(Request request, HttpServletResponse response, UserIdentity userIdentity) throws IOException {
		boolean refresh = Web.getFlagBoolean(request, "refresh");
		if(refresh) {
			postgisLayerManager.refresh();
			broker.refreshRasterdbConfigs();
		}

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("layers");
		json.array();

		for (String name : broker.getRasterdbNames()) {	
			RasterDB rasterdb = broker.getRasterdb(name);
			if (rasterdb.isAllowed(userIdentity)) {
				json.object();
				json.key("name");
				json.value(name);
				json.key("type");
				json.value("rasterdb");
				json.endObject();
			}
		}

		for (String name : broker.getPointCloudNames()) {
			if(broker.isAllowedPointCloud(name, userIdentity)) {
				json.object();
				json.key("name");
				json.value(name);
				json.key("type");
				json.value("pointcloud");
				json.endObject();
			}
		}		

		broker.catalog.getSorted(CatalogKey.TYPE_VECTORDB, userIdentity).forEach(entry -> {
			json.object();
			json.key("name");
			json.value(entry.name);
			json.key("type");
			json.value("vectordb");
			json.endObject();
		});
		
		/*broker.catalog.getSorted(userIdentity).forEach(entry -> {
			json.object();
			json.key("name");
			json.value(entry.name);
			json.key("type");
			json.value(entry.type.toLowerCase());
			json.endObject();
		});*/

		postgisLayerManager.forEach(userIdentity, postgisLayerBase -> {
			json.object();
			json.key("name");
			json.value(postgisLayerBase.name);
			json.key("type");
			json.value("postgis");
			json.endObject();
		});

		json.endArray();
		json.endObject();
	}
}
