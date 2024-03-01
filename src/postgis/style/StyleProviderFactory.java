package postgis.style;

import java.awt.Color;
import java.util.HashMap;

import org.json.JSONObject;

import util.yaml.YamlMap;
import vectordb.style.BasicStyle;
import vectordb.style.Style;

public class StyleProviderFactory {
	
	public static final StyleProvider DEFAULT_STYLE_PROVIDER;
	
	static {
		Style[] styles = new Style[] {
				new BasicStyle(BasicStyle.createStroke(1), new Color(0, 50, 0, 100), new Color(0, 150, 0, 100)),
				new BasicStyle(BasicStyle.createStroke(1), new Color(50, 0, 0, 100), new Color(150, 0, 0, 100)),
				new BasicStyle(BasicStyle.createStroke(1), new Color(0, 0, 50, 100), new Color(0, 0, 150, 100)),
				new BasicStyle(BasicStyle.createStroke(1), new Color(50, 50, 0, 100), new Color(150, 150, 0, 100)),
				new BasicStyle(BasicStyle.createStroke(1), new Color(0, 50, 50, 100), new Color(0, 150, 150, 100)),
				new BasicStyle(BasicStyle.createStroke(1), new Color(50, 0, 150, 100), new Color(150, 0, 150, 100)),
				
				new BasicStyle(BasicStyle.createStroke(1), new Color(50, 50, 25, 100), new Color(150, 150, 25, 100)),
				new BasicStyle(BasicStyle.createStroke(1), new Color(25, 50, 50, 100), new Color(25, 150, 150, 100)),
				new BasicStyle(BasicStyle.createStroke(1), new Color(50, 25, 150, 100), new Color(150, 25, 150, 100)),
				
				new BasicStyle(BasicStyle.createStroke(1), new Color(25, 50, 0, 100), new Color(25, 150, 0, 100)),
				new BasicStyle(BasicStyle.createStroke(1), new Color(50, 25, 0, 100), new Color(150, 25, 0, 100)),
				new BasicStyle(BasicStyle.createStroke(1), new Color(25, 0, 50, 100), new Color(25, 0, 150, 100)),
				
				new BasicStyle(BasicStyle.createStroke(1), new Color(0, 50, 25, 100), new Color(0, 150, 25, 100)),
				new BasicStyle(BasicStyle.createStroke(1), new Color(50, 0, 25, 100), new Color(150, 0, 25, 100)),
				new BasicStyle(BasicStyle.createStroke(1), new Color(0, 25, 50, 100), new Color(0, 25, 150, 100)),
				
				new BasicStyle(BasicStyle.createStroke(1), new Color(25, 50, 25, 100), new Color(25, 150, 25, 100)),
				new BasicStyle(BasicStyle.createStroke(1), new Color(50, 25, 25, 100), new Color(150, 25, 25, 100)),
				new BasicStyle(BasicStyle.createStroke(1), new Color(25, 25, 50, 100), new Color(25, 25, 150, 100)),
				
				new BasicStyle(BasicStyle.createStroke(1), new Color(50, 50, 50, 100), new Color(150, 150, 150, 100)),					
		};
		DEFAULT_STYLE_PROVIDER = new RotatingArrayStyleProvider(styles);
	}
	
	public static StyleProvider ofYaml(YamlMap yamlMap) {
		StyleProvider fallbackStyleProvider = DEFAULT_STYLE_PROVIDER;
		String fallbackType = yamlMap.optString("type");
		String labelField = yamlMap.optString("label_field");
		if(fallbackType != null) {
			Style fallbackStyle = Style.ofYaml(yamlMap);
			fallbackStyleProvider = new SimpleStyleProvider(fallbackStyle, labelField);
		}
		String valueField = yamlMap.optString("value_field");
		if(valueField == null) {
			return fallbackStyleProvider;
		} else {
			HashMap<Integer, Style> map = new HashMap<Integer, Style>();
			YamlMap valueMap = yamlMap.getMap("values");
			valueMap.forEachKey((m, key) -> {
				int i = Integer.parseInt(key);
				Style style = Style.ofYaml(valueMap.getMap(key));
				map.put(i, style);
			});			
			return new MappedStyleProvider(valueField, labelField, map, fallbackStyleProvider);
		}		
	}
	
	public static StyleProvider ofJSON(JSONObject json) {
		StyleProvider fallbackStyleProvider = DEFAULT_STYLE_PROVIDER;
		String fallbackType = json.optString("type");
		String labelField = json.optString("label_field");
		if(fallbackType != null) {
			Style fallbackStyle = Style.ofJSON(json);
			fallbackStyleProvider = new SimpleStyleProvider(fallbackStyle, labelField);
		}
		String valueField = json.optString("value_field");
		if(valueField == null) {
			return fallbackStyleProvider;
		} else {
			HashMap<Integer, Style> map = new HashMap<Integer, Style>();
			JSONObject valueJson = json.getJSONObject("values");
			for(String key : valueJson.keySet()) {
				int i = Integer.parseInt(key);
				Style style = Style.ofJSON(valueJson.getJSONObject(key));
				map.put(i, style);
			}		
			return new MappedStyleProvider(valueField, labelField, map, fallbackStyleProvider);
		}		
	}
}
