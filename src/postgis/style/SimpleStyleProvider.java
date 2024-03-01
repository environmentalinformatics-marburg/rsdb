package postgis.style;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONWriter;

import vectordb.style.Style;

public class SimpleStyleProvider extends StyleProvider {

	private final Style style;
	private final String labelField;

	public SimpleStyleProvider(Style style, String labelField) {
		this.style = style;
		this.labelField = labelField;
	}

	@Override
	public Style getStyleByValue(int value) {
		return style;
	}

	@Override
	public Style getStyle() {
		return style;
	}

	@Override
	public Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		yamlMap.putAll(style.toYaml());
		yamlMap.put("label_field", labelField);
		return yamlMap;
	}

	@Override
	public void writeJson(JSONWriter json) {		
		json.object();
		style.writeJsonInside(json);
		json.key("label_field");
		json.value(labelField);		
		json.endObject();
	}

	@Override
	public String getValueField() {
		return null;
	}

	@Override
	public String getLabelField() {
		return labelField;
	}
}
