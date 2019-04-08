package server.api.pointdb;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONWriter;

import broker.Broker;
import server.api.pointdb.feature.Feature;
import server.api.pointdb.feature.FeatureCollection;
import util.Web;

public class APIHandler_features extends PointdbAPIHandler {
	
	private final FeatureCollection featureCollection = new FeatureCollection();

	public APIHandler_features(Broker broker) {
		super(broker, "features");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("features");
		json.array();
		
		for(Feature feature:featureCollection.getFeatures()) {
			json.object();
			json.key("name");
			json.value(feature.name);
			json.endObject();
		}
		
		
		json.endArray();
		json.endObject();
	}

}
