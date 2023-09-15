package postgis.style;

import java.util.Map;

import org.json.JSONWriter;

import vectordb.style.Style;

public abstract class StyleProvider {

	public abstract Style getStyleByValue(int value);
	public abstract Style getStyle();
	
	public abstract Map<String, Object> toYaml();	
	public abstract void writeJson(JSONWriter json);
	public abstract String getValueField();
}