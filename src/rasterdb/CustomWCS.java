package rasterdb;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONWriter;

import util.JsonUtil;
import util.yaml.YamlMap;

public class CustomWCS {

	public static class Builder {
		public int epsg = 0;
		
		public Builder() {}
		
		public static Builder ofYaml(YamlMap yamlMap) {
			Builder b = new Builder();
			yamlMap.optFunInt("epsg", epsg -> b.epsg = epsg);
			return b;
		}
		
		public static Builder ofJSON(JSONObject jsonObject) {
			Builder b = new Builder();
			JsonUtil.optFunInt(jsonObject, "epsg", epsg -> b.epsg = epsg);
			return b;
		}

		public CustomWCS build() {
			return new CustomWCS(this);
		}
	}

	public final int epsg;
	
	public static CustomWCS ofYaml(YamlMap yamlMap) {
		return Builder.ofYaml(yamlMap).build();
	}
	
	public static CustomWCS ofJSON(JSONObject jsonObject) {
		return Builder.ofJSON(jsonObject).build();		
	}

	private CustomWCS(Builder b) {
		epsg = b.epsg;
	}	
	
	public boolean hasEPSG() {
		return epsg > 0;
	}

	public Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		if(hasEPSG()) {
			yamlMap.put("epsg", epsg);
		}
		return yamlMap;
	}

	public void writeJson(JSONWriter json) {
		json.object();
		if(hasEPSG()) {
			json.key("epsg");
			json.value(epsg);
		}
		json.endObject();
	}	
}