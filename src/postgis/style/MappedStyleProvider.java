package postgis.style;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONWriter;

import server.api.postgis.PostgisHandler_image_png;
import vectordb.style.Style;

public class MappedStyleProvider extends StyleProvider {

	private final String valueField;
	private final Map<Integer, Style> map;
	private final StyleProvider fallbackStyleProvider;

	public MappedStyleProvider(String valueField, Map<Integer, Style> map, StyleProvider fallbackStyleProvider) {
		this.valueField = valueField;
		this.map = map;
		this.fallbackStyleProvider = fallbackStyleProvider;
	}

	@Override
	public Style getStyleByValue(int value) {
		Style style = map.get(value);
		return style != null ? style : fallbackStyleProvider.getStyleByValue(value);
	}

	@Override
	public Style getStyle() {
		return fallbackStyleProvider.getStyle();
	}

	@Override
	public Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		if(fallbackStyleProvider != StyleProviderFactory.DEFAULT_STYLE_PROVIDER) {
			fallbackStyleProvider.getStyle().toYaml(yamlMap);
		}
		yamlMap.put("value_field", valueField);
		LinkedHashMap<String, Object> valueMap = new LinkedHashMap<String, Object>();
		map.forEach((value, style) -> valueMap.put(Integer.toString(value), style.toYaml()));
		yamlMap.put("values", valueMap);
		return yamlMap;
	}

	@Override
	public void writeJson(JSONWriter json) {
		json.object();
		if(fallbackStyleProvider != StyleProviderFactory.DEFAULT_STYLE_PROVIDER) {
			fallbackStyleProvider.getStyle().writeJsonInside(json);
		}
		json.key("value_field");
		json.value(valueField);
		json.key("values");
		json.object();
		map.forEach((value, style) -> {
			json.key(Integer.toString(value));
			style.writeJson(json);
		});
		json.endObject();
		json.endObject();
	}

	@Override
	public String getValueField() {
		return valueField;
	}
}
