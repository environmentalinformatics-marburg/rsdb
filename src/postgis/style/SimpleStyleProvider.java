package postgis.style;

import java.util.Map;

import org.json.JSONWriter;

import vectordb.style.Style;

public class SimpleStyleProvider extends StyleProvider {
	
	private final Style style;	
	
	public SimpleStyleProvider(Style style) {
		this.style = style;
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
		return style.toYaml();
	}
	
	@Override
	public void writeJson(JSONWriter json) {
		style.writeJson(json);
	}

	@Override
	public String getValueField() {
		return null;
	}
}
