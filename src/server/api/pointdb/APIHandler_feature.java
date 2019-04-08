package server.api.pointdb;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONWriter;

import broker.Broker;
import pointdb.PointDB;
import pointdb.base.Rect;
import server.api.pointdb.feature.Feature;
import server.api.pointdb.feature.FeatureCollection;
import util.Web;

public class APIHandler_feature extends PointdbAPIHandler {
	
	private FeatureCollection featureCollection = new FeatureCollection();

	public APIHandler_feature(Broker broker) {
		super(broker, "feature");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {		

		String featureName = request.getParameter("name");
		if(featureName==null) {
			throw new RuntimeException("missing name parameter");
		}
		
		
		Feature feature = featureCollection.getFeature(featureName);
		if(feature==null) {
			throw new RuntimeException("unknown feature: "+featureName);
		}
		
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		Rect rect = Rect.of_UTM_request(request);
		PointDB db = getPointdb(request);
		
		if(rect.getArea()<=0) {
			throw new RuntimeException("area too small");
		}
		
		feature.calc(json, db, rect);   // example: http://localhost:8081/pointdb/feature?db=hai&name=pulse_density&x1=598780&y1=5668140&x2=598880&y2=5668240
	}

}
