package server.api.pointdb;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mapdb.Fun.Pair;

import broker.Broker;
import pointdb.PointDB;
import pointdb.base.Point2d;
import pointdb.base.Rect;
import pointdb.subsetdsl.Region;
import util.collections.vec.Vec;

public class APIHandler_process extends PointdbAPIHandler {
	private static final Logger log = LogManager.getLogger();	

	public APIHandler_process(Broker broker) {
		super(broker, "process");		
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		request.setHandled(true);

		Vec<Pair<Region, String>> areas = null;
		Vec<String> functions = null;

		long reqest_size = request.getContentLength();
		if(reqest_size>0) {			
			log.info("reqest_size "+reqest_size);
			byte[] raw = new byte[(int) reqest_size];
			ServletInputStream in = request.getInputStream();
			int pos = 0;
			while(pos < reqest_size) {
				log.info("read at "+pos+" of "+reqest_size);
				int read_size = in.read(raw, pos, (int) (reqest_size - pos));
				if(read_size < 1) {
					throw new RuntimeException("not all bytes read "+pos+"  of  " + reqest_size);
				}
				pos += read_size;
			}
			if(pos != reqest_size) {
				throw new RuntimeException("not all bytes read "+pos+"  of  " + reqest_size);
			}
			JSONObject jsonObject = new JSONObject(new String(raw, StandardCharsets.UTF_8));
			log.info("json "+jsonObject);
			if(jsonObject.has("areas")) {
				areas = new Vec<Pair<Region, String>>();
				JSONArray jsonSubset = jsonObject.optJSONArray("areas");
				if(jsonSubset == null) {
					JSONObject sub = jsonObject.optJSONObject("areas");
					if(sub == null) {
						throw new RuntimeException("invalid content in 'functions'");
					}
					jsonSubset = new JSONArray(java.util.Collections.singleton(sub));
				}
				int len = jsonSubset.length();
				for (int i = 0; i < len; i++) {
					JSONObject e = jsonSubset.getJSONObject(i);
					String name = e.has("name") ? e.get("name").toString() : ""+(i+1);
					if(e.has("script")) {
						String script = e.get("script").toString();
						Vec<Pair<Region, String>> script_areas = new pointdb.subsetdsl.Compiler().parse(script).getRegions(broker);
						areas.addAll(script_areas);
					} else if(e.has("polygon")) {
						JSONArray polygon = e.getJSONArray("polygon");
						int polygon_len = polygon.length();
						Point2d[] points = new Point2d[polygon_len];
						for (int polygon_index = 0; polygon_index < polygon_len; polygon_index++) {
							JSONArray coord = polygon.getJSONArray(polygon_index);
							if(coord.length() != 2) {
								throw new RuntimeException("no coordinate " + coord);
							}
							Point2d p = Point2d.of(coord.getDouble(0), coord.getDouble(1));
							points[polygon_index] = p;
						}
						Region region = Region.ofPolygon(points);
						areas.add(new Pair<Region, String>(region, name));
					} else if(e.has("bbox")) {
						JSONArray bbox = e.getJSONArray("bbox");
						double xmin = bbox.getDouble(0);
						double ymin = bbox.getDouble(1);
						double xmax = bbox.getDouble(2);
						double ymax = bbox.getDouble(3);
						Region region = Region.ofRect(Rect.of_UTM(xmin, ymin, xmax, ymax));
						areas.add(new Pair<Region, String>(region, name));
					} else {
						throw new RuntimeException("unknown subset "+e);
					}
				}
			}
			if(jsonObject.has("functions")) {
				functions = new Vec<String>();
				JSONArray jsonScripts = jsonObject.optJSONArray("functions");
				if(jsonScripts == null) {
					String script = jsonObject.optString("functions");
					if(script == null) {
						throw new RuntimeException("invalid content in 'functions'");
					}
					jsonScripts = new JSONArray(java.util.Collections.singleton(script));
				}
				int len = jsonScripts.length();
				for (int i = 0; i < len; i++) {
					String script = jsonScripts.getString(i);
					functions.add(script);
				}
			}
		}

		PointDB db = getPointdb(request);

		String subset = request.getParameter("subset");
		if(subset == null) {
			if(areas == null) {
				throw new RuntimeException("Parameter 'subset' is missing.");
			}
		} else {
			if(areas != null) {
				throw new RuntimeException("doubled parameter 'subset'");
			}
			log.info("subset "+subset);
			//String[] subsets = subset.split("&");
			areas = new pointdb.subsetdsl.Compiler().parse(subset).getRegions(broker);
		}

		String script = request.getParameter("script");
		if(script == null) {
			if(functions == null) {
				throw new RuntimeException("Parameter 'script' is missing.");
			}
		} else {
			if(functions != null) {
				throw new RuntimeException("doubled parameter 'script'");
			}
			//String[] functions = script.split("&");
			functions = new pointdb.lidarindicesdsl.Compiler().parse(script);
		}		

		String format = request.getParameter("format");
		if(format==null) {
			throw new RuntimeException("Parameter 'format' is missing.");
		}
		
		//log.info("areas " + areas);

		ProcessIndices.process(areas, functions, format, response, db, null, true);



	}
}
