package server.api.pointclouds;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mapdb.Fun.Pair;

import broker.Broker;
import broker.TimeSlice;
import pointcloud.PointCloud;
import pointdb.base.Point2d;
import pointdb.base.Rect;
import pointdb.subsetdsl.Region;
import server.api.pointdb.ProcessIndices;
import util.Web;
import util.collections.vec.Vec;

public class APIHandler_indices {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;

	public APIHandler_indices(Broker broker) {
		this.broker = broker;
	}

	public void handle(PointCloud pointcloud, String format, Request request, Response response) throws IOException {
		JSONObject json = Web.getRequestContentJSON(request);
		boolean omit_empty_areas = json.getBoolean("omit_empty_areas");
		JSONArray jsonAreas = json.optJSONArray("areas");
		if(jsonAreas == null) {
			JSONObject sub = json.optJSONObject("areas");
			if(sub == null) {
				throw new RuntimeException("invalid content in 'functions'");
			}
			jsonAreas = new JSONArray(java.util.Collections.singleton(sub));
		}
		Vec<Pair<Region, String>> areas = new Vec<Pair<Region, String>>();
		int jsonAreasLen = jsonAreas.length();
		for (int i = 0; i < jsonAreasLen; i++) {
			JSONObject e = jsonAreas.getJSONObject(i);
			String name = e.has("name") ? e.get("name").toString() : ""+(i+1);
			//log.info("name of area: " + name);
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
				throw new RuntimeException("unknown area "+e);
			}
		}

		if(!json.has("functions")) {
			throw new RuntimeException("parameter functions missing");
		}
		JSONArray jsonFunctions = json.optJSONArray("functions");
		if(jsonFunctions == null) {
			String script = json.optString("functions");
			if(script == null) {
				throw new RuntimeException("invalid content in 'functions'");
			}
			jsonFunctions = new JSONArray(java.util.Collections.singleton(script));
		}
		Vec<String> functions = new Vec<String>();
		int jsonFunctionsLen = jsonFunctions.length();
		for (int i = 0; i < jsonFunctionsLen; i++) {
			String script = jsonFunctions.getString(i);
			functions.add(script);
		}
		
		TimeSlice timeSlice = null;
		if(json.has("time_slice_id")) {
			int time_slice_id = json.getInt("time_slice_id");
			timeSlice = pointcloud.timeMapReadonly.get(time_slice_id);
			if(timeSlice == null) {
				if(time_slice_id == 0) {
					timeSlice = new TimeSlice(0, "default");
				} else {
					throw new RuntimeException("uknown time_slice_id: " + time_slice_id);
				}
			}
			if(Web.has(request, "time_slice_name") && !Web.getString(request, "time_slice_name").equals(timeSlice.name)) {
				throw new RuntimeException("time_slice_name does not match to time slice of time_slice_id: '" + Web.getString(request, "time_slice_name") + "'  '" + timeSlice.name + "'");
			}
		} else if(json.has("time_slice_name")) {
			String time_slice_name = json.getString("time_slice_name");
			timeSlice = pointcloud.getTimeSliceByName(time_slice_name);
			if(timeSlice == null) {
				throw new RuntimeException("unknown time_slice_name: " + time_slice_name);
			}
		} else if(!pointcloud.timeMapReadonly.isEmpty()) {
			timeSlice = pointcloud.timeMapReadonly.lastEntry().getValue();
		}
		int req_t = timeSlice == null ? 0 : timeSlice.id;

		ProcessIndices.process(req_t, areas, functions, format, response, null, pointcloud, omit_empty_areas);
	}
}
