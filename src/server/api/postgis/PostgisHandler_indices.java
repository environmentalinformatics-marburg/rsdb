package server.api.postgis;

import java.io.IOException;
import java.util.HashMap;
import java.util.function.Supplier;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.locationtech.jts.geom.Polygon;
import org.tinylog.Logger;

import jakarta.servlet.http.HttpServletResponse;
import pointcloud.Rect2d;
import postgis.PostgisLayer;
import postgis.PostgisLayer.JTSGeometryConsumer;
import util.JsonUtil;
import util.Web;
import util.collections.vec.DoubleVec;

public class PostgisHandler_indices {

	public static abstract class ValuCollector implements JTSGeometryConsumer {		
		public DoubleVec collector = new DoubleVec();
	}

	public static class CalcArea extends ValuCollector {

		@Override
		public void acceptPolygon(Polygon polygon) {
			double area = polygon.getArea();
			collector.add(area);
		}		
	}

	public static class CalcPerimeter extends ValuCollector {

		@Override
		public void acceptPolygon(Polygon polygon) {
			double area = polygon.getLength();
			collector.add(area);
		}		
	}

	public static class CalcProcessor implements JTSGeometryConsumer {	

		private CalcArea calcArea = new CalcArea();
		private CalcPerimeter calcPerimeter = new CalcPerimeter();

		public CalcProcessor() {
		}

		@Override
		public void acceptPolygon(Polygon polygon) {
			calcArea.acceptPolygon(polygon);
			calcPerimeter.acceptPolygon(polygon);
		}

		public void writeJSON(JSONWriter json) {
			DoubleVec area = calcArea.collector;
			DoubleVec perimeter = calcPerimeter.collector;
			DoubleVec para = perimeter.div(area);

			json.key("count");
			json.value(area.size());			
			
			JsonUtil.putDoubleIfFinite(json, "area", area.sum());
			JsonUtil.putDoubleIfFinite(json, "area_mn", area.mean());
			JsonUtil.putDoubleIfFinite(json, "area_sd", area.sd());
			JsonUtil.putDoubleIfFinite(json, "area_cv", area.cv());			
			JsonUtil.putDoubleIfFinite(json, "area_skewness", area.skewness());
			JsonUtil.putDoubleIfFinite(json, "area_kurtosis", area.kurtosis());
			
			JsonUtil.putDoubleIfFinite(json, "perimeter", perimeter.sum());
			JsonUtil.putDoubleIfFinite(json, "perimeter_mn", perimeter.mean());
			JsonUtil.putDoubleIfFinite(json, "perimeter_sd", perimeter.sd());
			JsonUtil.putDoubleIfFinite(json, "perimeter_cv", perimeter.cv());			
			JsonUtil.putDoubleIfFinite(json, "perimeter_skewness", perimeter.skewness());
			JsonUtil.putDoubleIfFinite(json, "perimeter_kurtosis", perimeter.kurtosis());
			
			JsonUtil.putDoubleIfFinite(json, "para", para.sum());
			JsonUtil.putDoubleIfFinite(json, "para_mn", para.mean());
			JsonUtil.putDoubleIfFinite(json, "para_sd", para.sd());
			JsonUtil.putDoubleIfFinite(json, "para_cv", para.cv());			
			JsonUtil.putDoubleIfFinite(json, "para_skewness", para.skewness());
			JsonUtil.putDoubleIfFinite(json, "para_kurtosis", para.kurtosis());
		}
	}


	public void handle(PostgisLayer postgisLayer, String target, Request request, Response response, UserIdentity userIdentity) throws JSONException, IOException {
		JSONObject jsonR = new JSONObject(Web.requestContentToString(request));
		JSONObject jsonRect = jsonR.getJSONObject("bbox");
		Rect2d rect2d = Rect2d.ofJSON(jsonRect);

		HashMap<Object, CalcProcessor> groupMap = new HashMap<Object, CalcProcessor>();
		postgisLayer.forEachJTSGeometryByGroup(rect2d, "class_0", groupMap, CalcProcessor::new);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		json.object();
		json.key("groups");
		json.array();
		groupMap.forEach((group, calcProcessor) -> {
			json.object();
			json.key("group");
			json.value(group);
			calcProcessor.writeJSON(json);
			json.endObject();
		});
		json.endArray();
		json.endObject();	

		/*CalcArea calcArea = new CalcArea();
		CalcPerimeter calcPerimeter = new CalcPerimeter();

		DoubleVec area = calcArea.collector;
		DoubleVec perimeter = calcPerimeter.collector;

		postgisLayer.forEachJTSGeometry(rect2d, calcArea);
		postgisLayer.forEachJTSGeometry(rect2d, calcPerimeter);

		HashMap<Object, CalcArea> groupMap = new HashMap<Object, CalcArea>();
		postgisLayer.forEachJTSGeometryByGroup(rect2d, "class_0", groupMap, () -> {
			return new CalcArea();
		});

		Logger.info("keys " + groupMap.keySet());

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("count");
		json.value(area.size());
		json.key("area_sum");
		json.value(area.sum());
		json.key("area_mean");
		json.value(area.mean());
		json.key("perimeter_sum");
		json.value(perimeter.sum());
		json.key("perimeter_mean");
		json.value(perimeter.mean());
		json.key("groups");
		json.array();
		groupMap.forEach((group, value) -> {
			json.object();
			json.key("group");
			json.value(group);
			json.key("value");
			json.value(value.collector.sum());
			json.endObject();
		});

		json.endArray();
		json.endObject();	*/
	}	
}
