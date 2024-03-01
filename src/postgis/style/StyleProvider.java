package postgis.style;

import java.util.Map;

import org.json.JSONWriter;

import vectordb.style.Style;

public abstract class StyleProvider {

	public abstract Style getStyleByValue(int value);
	public abstract Style getStyle();
	
	public abstract Map<String, Object> toYaml();	
	public abstract void writeJson(JSONWriter json);
	
	/**
	 * 
	 * @return nullable
	 */
	public abstract String getValueField();
	
	public boolean hasValueField() {
		return getValueField() != null;
	}
	
	/**
	 * 
	 * @return nullable
	 */
	public abstract String getLabelField();
	
	public boolean hasLabelField() {
		return getLabelField() != null;
	}
}