package vectordb;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONWriter;

import util.JsonUtil;
import util.yaml.YamlMap;

public class VectorStyle {
	
	private PointStyle pointStyle = null;
	
	public static VectorStyle ofYaml(YamlMap yamlMap) {
		VectorStyle v = new VectorStyle();
		yamlMap.optMapFun("point_style", m -> v.pointStyle = PointStyle.ofYaml(m));
		return v;
	}
	
	public static VectorStyle ofJSON(JSONObject json) {
		VectorStyle v = new VectorStyle();
		JsonUtil.optFunJSONObject(json, "point_style", j -> v.pointStyle = PointStyle.ofJSON(j));
		return v;
	}
	
	public Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();		
		if(pointStyle != null) {
			yamlMap.put("point_style", pointStyle.toYaml());
		}
		return yamlMap;
	}
	
	public void writeJson(JSONWriter json) {
		json.object();
		if(pointStyle != null) {
			json.key("point_style");
			pointStyle.writeJson(json);
		}
		json.endObject();
	}

	public PointStyle getPointStyle() {
		return pointStyle;
	}
}
