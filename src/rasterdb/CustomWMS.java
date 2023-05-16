package rasterdb;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONWriter;

import util.JsonUtil;
import util.yaml.YamlMap;

public class CustomWMS {

	public static class Builder {
		public String value_range = null;
		public double value_range_static_min = Double.NaN;
		public double value_range_static_max = Double.NaN;;
		public String gamma = null;
		public boolean gamma_auto_sync = false;
		public String palette = null;
		public String format = null;
		public int epsg = 0;
		
		public Builder() {}
		
		public static Builder ofYaml(YamlMap yamlMap) {
			Builder b = new Builder();
			yamlMap.optFunString("value_range", value_range -> b.value_range = value_range);
			yamlMap.optFunDouble("value_range_static_min", value_range_static_min -> b.value_range_static_min = value_range_static_min);
			yamlMap.optFunDouble("value_range_static_max", value_range_static_max -> b.value_range_static_max = value_range_static_max);
			yamlMap.optFunString("gamma", gamma -> b.gamma = gamma);
			yamlMap.optFunBoolean("gamma_auto_sync", gamma_auto_sync -> b.gamma_auto_sync = gamma_auto_sync);
			yamlMap.optFunString("palette", palette -> b.palette = palette);
			yamlMap.optFunString("format", format -> b.format = format);
			yamlMap.optFunInt("epsg", epsg -> b.epsg = epsg);
			return b;
		}
		
		public static Builder ofJSON(JSONObject jsonObject) {
			Builder b = new Builder();
			JsonUtil.optFunString(jsonObject, "value_range", value_range -> b.value_range = value_range);
			JsonUtil.optFunDouble(jsonObject, "value_range_static_min", value_range_static_min -> b.value_range_static_min = value_range_static_min);
			JsonUtil.optFunDouble(jsonObject, "value_range_static_max", value_range_static_max -> b.value_range_static_max = value_range_static_max);
			JsonUtil.optFunString(jsonObject, "gamma", gamma -> b.gamma = gamma);
			JsonUtil.optFunBoolean(jsonObject, "gamma_auto_sync", gamma_auto_sync -> b.gamma_auto_sync = gamma_auto_sync);
			JsonUtil.optFunString(jsonObject, "palette", palette -> b.palette = palette);
			JsonUtil.optFunString(jsonObject, "format", format -> b.format = format);
			JsonUtil.optFunInt(jsonObject, "epsg", epsg -> b.epsg = epsg);
			return b;
		}

		public CustomWMS build() {
			return new CustomWMS(this);
		}
	}

	public final String value_range;
	public final double value_range_static_min;
	public final double value_range_static_max;
	public final String gamma;
	public final boolean gamma_auto_sync;
	public final String palette;
	public final String format;
	public final int epsg;
	
	public static CustomWMS ofYaml(YamlMap yamlMap) {
		return Builder.ofYaml(yamlMap).build();
	}
	
	public static CustomWMS ofJSON(JSONObject jsonObject) {
		return Builder.ofJSON(jsonObject).build();		
	}

	private CustomWMS(Builder b) {
		value_range = b.value_range;
		value_range_static_min = b.value_range_static_min;
		value_range_static_max = b.value_range_static_max;
		gamma = b.gamma;
		gamma_auto_sync = b.gamma_auto_sync;
		palette = b.palette;
		format = b.format;
		epsg = b.epsg;
	}
	
	public boolean hasValue_range() {
		return value_range != null;
	}
	
	public boolean hasValue_range_static_min() {
		return Double.isFinite(value_range_static_min);
	}
	
	public boolean hasValue_range_static_max() {
		return Double.isFinite(value_range_static_max);
	}
	
	public boolean hasGamma() {
		return gamma != null;
	}

	public boolean hasPalette() {
		return palette != null;
	}
	
	public boolean hasFormat() {
		return format != null;
	}
	
	public boolean hasEPSG() {
		return epsg > 0;
	}

	public Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		if(hasValue_range()) {
			yamlMap.put("value_range", value_range);
			if(hasValue_range_static_min()) {
				yamlMap.put("value_range_static_min", value_range_static_min);
			}
			if(hasValue_range_static_max()) {
				yamlMap.put("value_range_static_max", value_range_static_max);
			}
		}
		if(hasGamma()) {
			yamlMap.put("gamma", gamma);
			yamlMap.put("gamma_auto_sync", gamma_auto_sync);
		}
		if(hasPalette()) {
			yamlMap.put("palette", palette);
		}
		if(hasFormat()) {
			yamlMap.put("format", format);
		}
		if(hasEPSG()) {
			yamlMap.put("epsg", epsg);
		}
		return yamlMap;
	}

	public void writeJson(JSONWriter json) {
		json.object();
		if(hasValue_range()) {
			json.key("value_range");
			json.value(value_range);
			if(hasValue_range_static_min()) {
				json.key("value_range_static_min");
				json.value(value_range_static_min);
			}
			if(hasValue_range_static_max()) {
				json.key("value_range_static_max");
				json.value(value_range_static_max);
			}
		}
		if(hasGamma()) {
			json.key("gamma");
			json.value(gamma);
			json.key("gamma_auto_sync");
			json.value(gamma_auto_sync);
		}
		if(hasPalette()) {
			json.key("palette");
			json.value(palette);
		}
		if(hasFormat()) {
			json.key("format");
			json.value(format);
		}
		if(hasEPSG()) {
			json.key("epsg");
			json.value(epsg);
		}
		json.endObject();
	}	
}