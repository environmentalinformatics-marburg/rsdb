package rasterdb;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONWriter;

import util.JsonUtil;
import util.yaml.YamlMap;

public class CustomWMS {

	public static class Builder {
		public String gamma = null;
		public String palette = null;		
		
		public Builder() {}
		
		public static Builder ofYaml(YamlMap yamlMap) {
			Builder b = new Builder();
			yamlMap.optFunString("gamma", gamma -> b.gamma = gamma);
			yamlMap.optFunString("palette", palette -> b.palette = palette);
			return b;
		}
		
		public static Builder ofJSON(JSONObject jsonObject) {
			Builder b = new Builder();
			JsonUtil.optFunString(jsonObject, "gamma", gamma -> b.gamma = gamma);
			JsonUtil.optFunString(jsonObject, "palette", palette -> b.palette = palette);
			return b;
		}

		public CustomWMS build() {
			return new CustomWMS(this);
		}
	}

	public final String gamma;
	public final String palette;
	
	public static CustomWMS ofYaml(YamlMap yamlMap) {
		return Builder.ofYaml(yamlMap).build();
	}
	
	public static CustomWMS ofJSON(JSONObject jsonObject) {
		return Builder.ofJSON(jsonObject).build();		
	}

	private CustomWMS(Builder b) {
		gamma = b.gamma;
		palette = b.palette;		
	}
	
	public boolean hasGamma() {
		return gamma != null;
	}

	public boolean hasPalette() {
		return palette != null;
	}

	public Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		if(hasGamma()) {
			yamlMap.put("gamma", gamma);
		}
		if(hasPalette()) {
			yamlMap.put("palette", palette);
		}
		return yamlMap;
	}

	public void writeJson(JSONWriter json) {
		json.object();
		if(hasGamma()) {
			json.key("gamma");
			json.value(gamma);
		}
		if(hasPalette()) {
			json.key("palette");
			json.value(palette);
		}
		json.endObject();
	}	
}