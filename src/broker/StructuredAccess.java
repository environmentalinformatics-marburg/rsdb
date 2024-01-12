package broker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.json.JSONObject;
import org.json.JSONWriter;

import util.yaml.YamlMap;

public class StructuredAccess {	
	
	public static final StructuredAccess DEFAULT = new StructuredAccess(false, false);
	
	public final boolean poi;
	public final boolean roi;

	public StructuredAccess(boolean poi, boolean roi) {
		this.poi = poi;
		this.roi = roi;
	}

	public static StructuredAccess parseJSON(JSONObject json) {
		boolean poi = json.has("poi") ? json.getBoolean("poi") : false;
		boolean roi = json.has("roi") ? json.getBoolean("roi") : false;
		return new StructuredAccess(poi, roi);
	}

	public void writeJSON(JSONWriter json) {
		json.object();
		json.key("poi");
		json.value(poi);
		json.key("roi");
		json.value(roi);
		json.endObject();
	}
	
	public static StructuredAccess ofYaml(YamlMap yamlMap) {
		boolean structured_access_poi = yamlMap.optBoolean("poi", false);
		boolean structured_access_roi = yamlMap.optBoolean("roi", false);
		return new StructuredAccess(structured_access_poi, structured_access_roi);
	}
	
	public Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		yamlMap.put("poi", poi);
		yamlMap.put("roi", roi);		
		return yamlMap;		
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StructuredAccess other = (StructuredAccess) obj;
		return poi == other.poi && roi == other.roi;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(poi, roi);
	}
}
