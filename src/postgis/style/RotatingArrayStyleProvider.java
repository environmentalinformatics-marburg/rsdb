package postgis.style;

import java.util.Map;

import org.json.JSONWriter;

import vectordb.style.Style;

public class RotatingArrayStyleProvider extends StyleProvider {
	
	private final Style[] styles;	
	
	public RotatingArrayStyleProvider(Style[] styles) {
		this.styles = styles;
	}
	
	@Override
	public Style getStyleByValue(int value) {
		int styleIndex = value % styles.length;
		Style style = styles[styleIndex];
		return style;
	}

	@Override
	public Style getStyle() {
		return styles[0];
	}

	@Override
	public Map<String, Object> toYaml() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void writeJson(JSONWriter json) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public String getValueField() {
		return null;
	}

	@Override
	public String getLabelField() {
		return null;
	}
}
