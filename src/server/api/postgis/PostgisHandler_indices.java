package server.api.postgis;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.locationtech.jts.algorithm.MinimumBoundingCircle;
import org.locationtech.jts.algorithm.MinimumDiameter;
import org.locationtech.jts.algorithm.construct.MaximumInscribedCircle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.tinylog.Logger;

import jakarta.servlet.http.HttpServletResponse;
import pointcloud.Rect2d;
import postgis.JTSGeometryConsumer;
import postgis.PostgisLayer;
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

	public static class CalcMinDiameter extends ValuCollector {

		@Override
		public void acceptPolygon(Polygon polygon) {
			double minDiameter = new MinimumDiameter(polygon).getLength();
			collector.add(minDiameter);
		}		
	}

	public static class CalcMaxDiameter extends ValuCollector {

		@Override
		public void acceptPolygon(Polygon polygon) {
			Geometry geometry = polygon.convexHull();
			Coordinate[] cs = geometry.getCoordinates();
			double maxDiameter = 0d;
			for(int i = 0; i < cs.length; i++) {
				for(int j = i + 1; j < cs.length; j++) {
					double diameter = cs[i].distance(cs[j]);
					if(diameter > maxDiameter) {
						maxDiameter = diameter;
					}
				}
			}
			collector.add(maxDiameter);
		}		
	}
	
	public static class CalcBoundingCircle extends ValuCollector {

		@Override
		public void acceptPolygon(Polygon polygon) {
			double boundingCircle = new MinimumBoundingCircle(polygon).getRadius() * 2d;
			collector.add(boundingCircle);
		}		
	}
	
	public static class CalcInscribedCircle extends ValuCollector {

		@Override
		public void acceptPolygon(Polygon polygon) {
			double inscribedCircle = new MaximumInscribedCircle(polygon, 0.001d).getRadiusLine().getLength() * 2d;
			collector.add(inscribedCircle);
		}		
	}

	public static class CalcProcessor implements JTSGeometryConsumer {	

		private CalcArea calcArea = new CalcArea();
		private CalcPerimeter calcPerimeter = new CalcPerimeter();
		private CalcMinDiameter calcMinDiameter = new CalcMinDiameter();
		private CalcMaxDiameter calcMaxDiameter = new CalcMaxDiameter();
		private CalcBoundingCircle calcBoundingCircle = new CalcBoundingCircle();
		private CalcInscribedCircle calcInscribedCircle = new CalcInscribedCircle();

		public CalcProcessor() {
		}

		@Override
		public void acceptPolygon(Polygon polygon) {
			calcArea.acceptPolygon(polygon);
			calcPerimeter.acceptPolygon(polygon);
			calcMinDiameter.acceptPolygon(polygon);
			calcMaxDiameter.acceptPolygon(polygon);
			calcBoundingCircle.acceptPolygon(polygon);
			calcInscribedCircle.acceptPolygon(polygon);
		}

		public void writeJSON(JSONWriter json) {
			DoubleVec area = calcArea.collector;
			DoubleVec perimeter = calcPerimeter.collector;
			DoubleVec para = perimeter.div(area);
			DoubleVec minDiameter = calcMinDiameter.collector;
			DoubleVec maxDiameter = calcMaxDiameter.collector;
			DoubleVec diameter_ratio = maxDiameter.div(minDiameter);
			DoubleVec boundig_circle = calcBoundingCircle.collector;
			DoubleVec inscribed_circle = calcInscribedCircle.collector;
			DoubleVec circle_ratio = boundig_circle.div(inscribed_circle);

			json.key("count");
			json.value(area.size());			

			JsonUtil.putDoubleIfFinite(json, "area", area.sum());
			JsonUtil.putDoubleIfFinite(json, "area_mn", area.mean());
			//JsonUtil.putDoubleIfFinite(json, "area_sd", area.sd());
			JsonUtil.putDoubleIfFinite(json, "area_cv", area.cv());			
			//JsonUtil.putDoubleIfFinite(json, "area_skewness", area.skewness());
			//JsonUtil.putDoubleIfFinite(json, "area_kurtosis", area.kurtosis());

			JsonUtil.putDoubleIfFinite(json, "perimeter", perimeter.sum());
			JsonUtil.putDoubleIfFinite(json, "perimeter_mn", perimeter.mean());
			//JsonUtil.putDoubleIfFinite(json, "perimeter_sd", perimeter.sd());
			JsonUtil.putDoubleIfFinite(json, "perimeter_cv", perimeter.cv());			
			//JsonUtil.putDoubleIfFinite(json, "perimeter_skewness", perimeter.skewness());
			//JsonUtil.putDoubleIfFinite(json, "perimeter_kurtosis", perimeter.kurtosis());

			//JsonUtil.putDoubleIfFinite(json, "para", para.sum());
			JsonUtil.putDoubleIfFinite(json, "para_mn", para.mean());
			//JsonUtil.putDoubleIfFinite(json, "para_sd", para.sd());
			JsonUtil.putDoubleIfFinite(json, "para_cv", para.cv());			
			//JsonUtil.putDoubleIfFinite(json, "para_skewness", para.skewness());
			//JsonUtil.putDoubleIfFinite(json, "para_kurtosis", para.kurtosis());

			//JsonUtil.putDoubleIfFinite(json, "min_diameter", minDiameter.sum());
			JsonUtil.putDoubleIfFinite(json, "min_diameter_mn", minDiameter.mean());
			//JsonUtil.putDoubleIfFinite(json, "min_diameter_sd", minDiameter.sd());
			JsonUtil.putDoubleIfFinite(json, "min_diameter_cv", minDiameter.cv());			
			//JsonUtil.putDoubleIfFinite(json, "min_diameter_skewness", minDiameter.skewness());
			//JsonUtil.putDoubleIfFinite(json, "min_diameter_kurtosis", minDiameter.kurtosis());

			//JsonUtil.putDoubleIfFinite(json, "max_diameter", maxDiameter.sum());
			JsonUtil.putDoubleIfFinite(json, "max_diameter_mn", maxDiameter.mean());
			//JsonUtil.putDoubleIfFinite(json, "max_diameter_sd", maxDiameter.sd());
			JsonUtil.putDoubleIfFinite(json, "max_diameter_cv", maxDiameter.cv());			
			//JsonUtil.putDoubleIfFinite(json, "max_diameter_skewness", maxDiameter.skewness());
			//JsonUtil.putDoubleIfFinite(json, "max_diameter_kurtosis", maxDiameter.kurtosis());

			//JsonUtil.putDoubleIfFinite(json, "diameter_ratio", diameter_ratio.sum());
			JsonUtil.putDoubleIfFinite(json, "diameter_ratio_mn", diameter_ratio.mean());
			//JsonUtil.putDoubleIfFinite(json, "diameter_ratio_sd", diameter_ratio.sd());
			JsonUtil.putDoubleIfFinite(json, "diameter_ratio_cv", diameter_ratio.cv());			
			//JsonUtil.putDoubleIfFinite(json, "diameter_ratio_skewness", diameter_ratio.skewness());
			//JsonUtil.putDoubleIfFinite(json, "diameter_ratio_kurtosis", diameter_ratio.kurtosis());
			
			//JsonUtil.putDoubleIfFinite(json, "boundig_circle", boundig_circle.sum());
			JsonUtil.putDoubleIfFinite(json, "boundig_circle_mn", boundig_circle.mean());
			//JsonUtil.putDoubleIfFinite(json, "boundig_circle_sd", boundig_circle.sd());
			JsonUtil.putDoubleIfFinite(json, "boundig_circle_cv", boundig_circle.cv());			
			//JsonUtil.putDoubleIfFinite(json, "boundig_circle_skewness", boundig_circle.skewness());
			//JsonUtil.putDoubleIfFinite(json, "boundig_circle_kurtosis", boundig_circle.kurtosis());
			
			//JsonUtil.putDoubleIfFinite(json, "inscribed_circle", inscribed_circle.sum());
			JsonUtil.putDoubleIfFinite(json, "inscribed_circle_mn", inscribed_circle.mean());
			//JsonUtil.putDoubleIfFinite(json, "inscribed_circle_sd", inscribed_circle.sd());
			JsonUtil.putDoubleIfFinite(json, "inscribed_circle_cv", inscribed_circle.cv());			
			//JsonUtil.putDoubleIfFinite(json, "inscribed_circle_skewness", inscribed_circle.skewness());
			//JsonUtil.putDoubleIfFinite(json, "inscribed_circle_kurtosis", inscribed_circle.kurtosis());
			
			//JsonUtil.putDoubleIfFinite(json, "circle_ratio", circle_ratio.sum());
			JsonUtil.putDoubleIfFinite(json, "circle_ratio_mn", circle_ratio.mean());
			//JsonUtil.putDoubleIfFinite(json, "circle_ratio_sd", circle_ratio.sd());
			JsonUtil.putDoubleIfFinite(json, "circle_ratio_cv", circle_ratio.cv());			
			//JsonUtil.putDoubleIfFinite(json, "circle_ratio_skewness", circle_ratio.skewness());
			//JsonUtil.putDoubleIfFinite(json, "circle_ratio_kurtosis", circle_ratio.kurtosis());
		}
	}


	public void handle(PostgisLayer postgisLayer, String target, Request request, Response response, UserIdentity userIdentity) throws JSONException, IOException {
		JSONObject jsonR = new JSONObject(Web.requestContentToString(request));
		JSONObject jsonRect = jsonR.getJSONObject("bbox");
		Rect2d rect2d = Rect2d.ofJSON(jsonRect);
		boolean crop = jsonR.optBoolean("crop", true);
		String classField = jsonR.optString("field", null);
		if(classField == null || classField.isBlank()) {
			if(!postgisLayer.getClass_fields().isEmpty()) {
				classField = postgisLayer.getClass_fields().first();
			} else if(!postgisLayer.fields.isEmpty()){
				classField = postgisLayer.fields.first().name;
			} else {
				classField = postgisLayer.primaryGeometryColumn;
			}
		}

		HashMap<Object, CalcProcessor> groupMap = new HashMap<Object, CalcProcessor>();
		postgisLayer.forEachJTSGeometryByGroup(rect2d, crop, classField, groupMap, CalcProcessor::new);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		//json.object();
		//json.key("groups");
		json.array();
		groupMap.forEach((group, calcProcessor) -> {
			json.object();
			json.key("group");
			json.value(group);
			calcProcessor.writeJSON(json);
			json.endObject();
		});
		json.endArray();
		//json.endObject();	

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
