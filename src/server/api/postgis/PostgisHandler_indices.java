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
		public void acceptGeometry(Geometry geometry) {
			double area = geometry.getArea();
			collector.add(area);
		}		
	}

	public static class CalcPerimeter extends ValuCollector {

		@Override
		public void acceptGeometry(Geometry geometry) {
			double area = geometry.getLength();
			collector.add(area);
		}	
	}

	public static class CalcMinDiameter extends ValuCollector {

		@Override
		public void acceptGeometry(Geometry geometry) {
			double minDiameter = new MinimumDiameter(geometry).getLength();
			collector.add(minDiameter);
		}		
	}

	public static class CalcMaxDiameter extends ValuCollector {

		@Override
		public void acceptGeometry(Geometry geometry) {
			Geometry geometryConvex = geometry.convexHull();
			Coordinate[] cs = geometryConvex.getCoordinates();
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
		public void acceptGeometry(Geometry geometry) {
			double boundingCircle = new MinimumBoundingCircle(geometry).getRadius() * 2d;
			collector.add(boundingCircle);
		}		
	}

	public static class CalcInscribedCircle extends ValuCollector {

		@Override
		public void acceptGeometry(Geometry geometry) {
			double inscribedCircle = new MaximumInscribedCircle(geometry, 0.001d).getRadiusLine().getLength() * 2d;
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
		public void acceptGeometry(Geometry geometry) {
			calcArea.acceptGeometry(geometry);
			calcPerimeter.acceptGeometry(geometry);
			calcMinDiameter.acceptGeometry(geometry);
			calcMaxDiameter.acceptGeometry(geometry);
			calcBoundingCircle.acceptGeometry(geometry);
			calcInscribedCircle.acceptGeometry(geometry);
		}

		public void writeJSON(JSONWriter json, Object group, boolean writeValues) {
			DoubleVec area = calcArea.collector;
			DoubleVec perimeter = calcPerimeter.collector;
			DoubleVec para = perimeter.div(area);
			DoubleVec minDiameter = calcMinDiameter.collector;
			DoubleVec maxDiameter = calcMaxDiameter.collector;
			DoubleVec diameter_ratio = maxDiameter.div(minDiameter);
			DoubleVec boundig_circle = calcBoundingCircle.collector;
			DoubleVec inscribed_circle = calcInscribedCircle.collector;
			DoubleVec circle_ratio = boundig_circle.div(inscribed_circle);

			if(writeValues) {	
				int len = area.size();
				for (int i = 0; i < len; i++) {
					json.object();
					json.key("class");
					json.value(group);
					JsonUtil.putDoubleIfFinite(json, "area", area.get(i));
					JsonUtil.putDoubleIfFinite(json, "perimeter", perimeter.get(i));
					JsonUtil.putDoubleIfFinite(json, "para", para.get(i));
					JsonUtil.putDoubleIfFinite(json, "min_diameter", minDiameter.get(i));
					JsonUtil.putDoubleIfFinite(json, "max_diameter", maxDiameter.get(i));
					JsonUtil.putDoubleIfFinite(json, "diameter_ratio", diameter_ratio.get(i));
					JsonUtil.putDoubleIfFinite(json, "boundig_circle", boundig_circle.get(i));
					JsonUtil.putDoubleIfFinite(json, "inscribed_circle", inscribed_circle.get(i));
					JsonUtil.putDoubleIfFinite(json, "circle_ratio", circle_ratio.get(i));
					json.endObject();
				}				
			} else {
				json.object();

				json.key("class");
				json.value(group);

				json.key("count");
				json.value(area.size());			

				JsonUtil.putDoubleIfFinite(json, "area", area.sum());
				JsonUtil.putDoubleIfFinite(json, "area_mn", area.mean());
				JsonUtil.putDoubleIfFinite(json, "area_md", area.median());
				JsonUtil.putDoubleIfFinite(json, "area_min", area.min());
				JsonUtil.putDoubleIfFinite(json, "area_max", area.max());
				JsonUtil.putDoubleIfFinite(json, "area_q1", area.quartile1());
				JsonUtil.putDoubleIfFinite(json, "area_q3", area.quartile3());
				//JsonUtil.putDoubleIfFinite(json, "area_sd", area.sd());
				JsonUtil.putDoubleIfFinite(json, "area_cv", area.cv());			
				JsonUtil.putDoubleIfFinite(json, "area_skewness", area.skewness());
				JsonUtil.putDoubleIfFinite(json, "area_excess", area.excess_kurtosis());

				JsonUtil.putDoubleIfFinite(json, "perimeter", perimeter.sum());
				JsonUtil.putDoubleIfFinite(json, "perimeter_mn", perimeter.mean());
				JsonUtil.putDoubleIfFinite(json, "perimeter_md", perimeter.median());
				JsonUtil.putDoubleIfFinite(json, "perimeter_min", perimeter.min());
				JsonUtil.putDoubleIfFinite(json, "perimeter_max", perimeter.max());
				//JsonUtil.putDoubleIfFinite(json, "perimeter_sd", perimeter.sd());
				JsonUtil.putDoubleIfFinite(json, "perimeter_cv", perimeter.cv());			
				JsonUtil.putDoubleIfFinite(json, "perimeter_skewness", perimeter.skewness());
				JsonUtil.putDoubleIfFinite(json, "perimeter_excess", perimeter.excess_kurtosis());

				//JsonUtil.putDoubleIfFinite(json, "para", para.sum());
				JsonUtil.putDoubleIfFinite(json, "para_mn", para.mean());
				JsonUtil.putDoubleIfFinite(json, "para_md", para.median());
				JsonUtil.putDoubleIfFinite(json, "para_min", para.min());
				JsonUtil.putDoubleIfFinite(json, "para_max", para.max());
				//JsonUtil.putDoubleIfFinite(json, "para_sd", para.sd());
				JsonUtil.putDoubleIfFinite(json, "para_cv", para.cv());			
				JsonUtil.putDoubleIfFinite(json, "para_skewness", para.skewness());
				JsonUtil.putDoubleIfFinite(json, "para_excess", para.excess_kurtosis());

				//JsonUtil.putDoubleIfFinite(json, "min_diameter", minDiameter.sum());
				JsonUtil.putDoubleIfFinite(json, "min_diameter_mn", minDiameter.mean());
				JsonUtil.putDoubleIfFinite(json, "min_diameter_md", minDiameter.median());
				JsonUtil.putDoubleIfFinite(json, "min_diameter_min", minDiameter.min());
				JsonUtil.putDoubleIfFinite(json, "min_diameter_max", minDiameter.max());
				//JsonUtil.putDoubleIfFinite(json, "min_diameter_sd", minDiameter.sd());
				JsonUtil.putDoubleIfFinite(json, "min_diameter_cv", minDiameter.cv());			
				JsonUtil.putDoubleIfFinite(json, "min_diameter_skewness", minDiameter.skewness());
				JsonUtil.putDoubleIfFinite(json, "min_diameter_excess", minDiameter.excess_kurtosis());

				//JsonUtil.putDoubleIfFinite(json, "max_diameter", maxDiameter.sum());
				JsonUtil.putDoubleIfFinite(json, "max_diameter_mn", maxDiameter.mean());
				JsonUtil.putDoubleIfFinite(json, "max_diameter_median", maxDiameter.median());
				JsonUtil.putDoubleIfFinite(json, "max_diameter_min", maxDiameter.min());
				JsonUtil.putDoubleIfFinite(json, "max_diameter_max", maxDiameter.max());
				//JsonUtil.putDoubleIfFinite(json, "max_diameter_sd", maxDiameter.sd());
				JsonUtil.putDoubleIfFinite(json, "max_diameter_cv", maxDiameter.cv());			
				JsonUtil.putDoubleIfFinite(json, "max_diameter_skewness", maxDiameter.skewness());
				JsonUtil.putDoubleIfFinite(json, "max_diameter_excess", maxDiameter.excess_kurtosis());

				//JsonUtil.putDoubleIfFinite(json, "diameter_ratio", diameter_ratio.sum());
				JsonUtil.putDoubleIfFinite(json, "diameter_ratio_mn", diameter_ratio.mean());
				JsonUtil.putDoubleIfFinite(json, "diameter_ratio_md", diameter_ratio.median());
				JsonUtil.putDoubleIfFinite(json, "diameter_ratio_min", diameter_ratio.min());
				JsonUtil.putDoubleIfFinite(json, "diameter_ratio_max", diameter_ratio.max());
				//JsonUtil.putDoubleIfFinite(json, "diameter_ratio_sd", diameter_ratio.sd());
				JsonUtil.putDoubleIfFinite(json, "diameter_ratio_cv", diameter_ratio.cv());			
				JsonUtil.putDoubleIfFinite(json, "diameter_ratio_skewness", diameter_ratio.skewness());
				JsonUtil.putDoubleIfFinite(json, "diameter_ratio_excess", diameter_ratio.excess_kurtosis());

				//JsonUtil.putDoubleIfFinite(json, "boundig_circle", boundig_circle.sum());
				JsonUtil.putDoubleIfFinite(json, "boundig_circle_mn", boundig_circle.mean());
				JsonUtil.putDoubleIfFinite(json, "boundig_circle_md", boundig_circle.median());
				JsonUtil.putDoubleIfFinite(json, "boundig_circle_min", boundig_circle.min());
				JsonUtil.putDoubleIfFinite(json, "boundig_circle_max", boundig_circle.max());
				//JsonUtil.putDoubleIfFinite(json, "boundig_circle_sd", boundig_circle.sd());
				JsonUtil.putDoubleIfFinite(json, "boundig_circle_cv", boundig_circle.cv());			
				JsonUtil.putDoubleIfFinite(json, "boundig_circle_skewness", boundig_circle.skewness());
				JsonUtil.putDoubleIfFinite(json, "boundig_circle_excess", boundig_circle.excess_kurtosis());

				//JsonUtil.putDoubleIfFinite(json, "inscribed_circle", inscribed_circle.sum());
				JsonUtil.putDoubleIfFinite(json, "inscribed_circle_mn", inscribed_circle.mean());
				JsonUtil.putDoubleIfFinite(json, "inscribed_circle_md", inscribed_circle.median());
				JsonUtil.putDoubleIfFinite(json, "inscribed_circle_min", inscribed_circle.min());
				JsonUtil.putDoubleIfFinite(json, "inscribed_circle_max", inscribed_circle.max());
				//JsonUtil.putDoubleIfFinite(json, "inscribed_circle_sd", inscribed_circle.sd());
				JsonUtil.putDoubleIfFinite(json, "inscribed_circle_cv", inscribed_circle.cv());			
				JsonUtil.putDoubleIfFinite(json, "inscribed_circle_skewness", inscribed_circle.skewness());
				JsonUtil.putDoubleIfFinite(json, "inscribed_circle_excess", inscribed_circle.excess_kurtosis());

				//JsonUtil.putDoubleIfFinite(json, "circle_ratio", circle_ratio.sum());
				JsonUtil.putDoubleIfFinite(json, "circle_ratio_mn", circle_ratio.mean());
				JsonUtil.putDoubleIfFinite(json, "circle_ratio_md", circle_ratio.median());
				JsonUtil.putDoubleIfFinite(json, "circle_ratio_min", circle_ratio.min());
				JsonUtil.putDoubleIfFinite(json, "circle_ratio_max", circle_ratio.max());
				//JsonUtil.putDoubleIfFinite(json, "circle_ratio_sd", circle_ratio.sd());
				JsonUtil.putDoubleIfFinite(json, "circle_ratio_cv", circle_ratio.cv());			
				JsonUtil.putDoubleIfFinite(json, "circle_ratio_skewness", circle_ratio.skewness());
				JsonUtil.putDoubleIfFinite(json, "circle_ratio_excess", circle_ratio.excess_kurtosis());

				json.endObject();
			}
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
		
		boolean writeValues = jsonR.optBoolean("values", false);

		HashMap<Object, CalcProcessor> groupMap = new HashMap<Object, CalcProcessor>();
		postgisLayer.forEachJTSGeometryByGroup(rect2d, crop, classField, groupMap, CalcProcessor::new);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		json.array();
		groupMap.forEach((group, calcProcessor) -> {				
			calcProcessor.writeJSON(json, group, writeValues);
		});
		json.endArray();

	}	
}
