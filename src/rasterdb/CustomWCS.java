package rasterdb;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONWriter;

import util.JsonUtil;
import util.collections.array.ReadonlyArray;
import util.collections.vec.Vec;
import util.yaml.YamlMap;

public class CustomWCS {

	public static class Builder {
		public int epsg = 0;
		public Vec<Integer> bands = new Vec<Integer>();
		
		public Builder() {}
		
		public static Builder ofYaml(YamlMap yamlMap) {
			Builder b = new Builder();
			yamlMap.optFunInt("epsg", epsg -> b.epsg = epsg);
			yamlMap.optList("bands").forEachInt(b.bands::add);
			return b;
		}
		
		public static Builder ofJSON(JSONObject jsonObject) {
			Builder b = new Builder();
			JsonUtil.optFunInt(jsonObject, "epsg", epsg -> b.epsg = epsg);
			JsonUtil.optFunInts(jsonObject, "bands", b.bands::add);
			return b;
		}

		public CustomWCS build() {
			return new CustomWCS(this);
		}
	}

	public final int epsg;
	public final ReadonlyArray<Integer> bands;
	
	public static CustomWCS ofYaml(YamlMap yamlMap) {
		return Builder.ofYaml(yamlMap).build();
	}
	
	public static CustomWCS ofJSON(JSONObject jsonObject) {
		return Builder.ofJSON(jsonObject).build();		
	}

	private CustomWCS(Builder b) {
		epsg = b.epsg;
		bands = b.bands.copyReadonly();
	}	
	
	public boolean hasEPSG() {
		return epsg > 0;
	}
	
	public boolean hasBands() {
		return !bands.isEmpty();
	}

	public Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		if(hasEPSG()) {
			yamlMap.put("epsg", epsg);
		}
		if(hasBands()) {
			yamlMap.put("bands", bands.copyVec());
		}
		return yamlMap;
	}

	public void writeJson(JSONWriter json) {
		json.object();
		if(hasEPSG()) {
			json.key("epsg");
			json.value(epsg);
		}
		if(hasBands()) {
			json.key("bands");
			json.array();
			for(Integer b:bands) {
				json.value(b);
			}
			json.endArray();
		}
		json.endObject();
	}
	
	public boolean includesBand(int id) {
		if(bands.isEmpty()) {
			return true;
		}
		return bands.contains(id);
	}
}