package vectordb;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONWriter;

import util.JsonUtil;
import util.yaml.YamlMap;

public class VectorStyle {
	
	private PointStyle pointStyle = null;
	private LineStyle lineStyle = null;
	private PolygonStyle polygonStyle = null;
	
	public static VectorStyle ofYaml(YamlMap yamlMap) {
		VectorStyle v = new VectorStyle();
		yamlMap.optMapFun("point_style", m -> v.pointStyle = PointStyle.ofYaml(m));
		yamlMap.optMapFun("line_style", m -> v.lineStyle = LineStyle.ofYaml(m));
		yamlMap.optMapFun("polygon_style", m -> v.polygonStyle = PolygonStyle.ofYaml(m));
		return v;
	}
	
	public static VectorStyle ofJSON(JSONObject json) {
		VectorStyle v = new VectorStyle();
		JsonUtil.optFunJSONObject(json, "point_style", j -> v.pointStyle = PointStyle.ofJSON(j));
		JsonUtil.optFunJSONObject(json, "line_style", j -> v.lineStyle = LineStyle.ofJSON(j));
		JsonUtil.optFunJSONObject(json, "polygon_style", j -> v.polygonStyle = PolygonStyle.ofJSON(j));
		return v;
	}
	
	public Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();		
		if(pointStyle != null) {
			yamlMap.put("point_style", pointStyle.toYaml());
		}
		if(lineStyle != null) {
			yamlMap.put("line_style", lineStyle.toYaml());
		}
		if(polygonStyle != null) {
			yamlMap.put("polygon_style", polygonStyle.toYaml());
		}
		return yamlMap;
	}
	
	public void writeJson(JSONWriter json) {
		json.object();
		if(pointStyle != null) {
			json.key("point_style");
			pointStyle.writeJson(json);
		}
		if(lineStyle != null) {
			json.key("line_style");
			lineStyle.writeJson(json);
		}
		if(polygonStyle != null) {
			json.key("polygon_style");
			polygonStyle.writeJson(json);
		}
		json.endObject();
	}

	public PointStyle getPointStyle() {
		return pointStyle;
	}
	
	public LineStyle getLineStyle() {
		return lineStyle;
	}
	
	public PolygonStyle getPolygonStyle() {
		return polygonStyle;
	}
}
