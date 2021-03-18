package vectordb;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONWriter;

import util.JsonUtil;
import util.collections.vec.Vec;
import util.yaml.YamlMap;
import vectordb.Renderer.Drawer;

public abstract class LineStyle {

	protected String line_type;

	public abstract void draw(Graphics2D gc, Drawer drawer, Vec<Object[]> lines);
	protected abstract void toYamlProperties(Map<String, Object> yamlMap);
	protected abstract void writeJsonProperties(JSONWriter json);
	public abstract void parseYamlProperties(YamlMap yamlMap);		 
	protected abstract void parseJsonProperties(JSONObject json);

	public final Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		yamlMap.put("line_type", line_type);
		toYamlProperties(yamlMap);
		return yamlMap;
	}
	
	public final void writeJson(JSONWriter json) {
		json.object();
		JsonUtil.put(json, "line_type", line_type);
		writeJsonProperties(json);
		json.endObject();
	}

	public static LineStyle ofYaml(YamlMap yamlMap) {
		String line_type = yamlMap.getString("line_type");
		LineStyle s = ofType(line_type);
		s.line_type = line_type;
		s.parseYamlProperties(yamlMap);
		return s;
	}
	
	public static LineStyle ofJSON(JSONObject json) {
		String line_type = json.getString("line_type");
		LineStyle s = ofType(line_type);
		s.line_type = line_type;
		s.parseJsonProperties(json);
		return s;
	}
	
	public static LineStyle ofType(String line_type) {
		switch(line_type) {
		case "box":
			return new LineStyleBox();
		default: 
			throw new RuntimeException("unknown line_type: " + line_type);
		}			
	}
	
	public static abstract class LineStyleColor extends LineStyle {
		protected Color color = Renderer.COLOR_LINE;

		@Override
		public void parseYamlProperties(YamlMap yamlMap) {
			yamlMap.optFunString("color", text -> color = Renderer.stringToColor(text));			
		}
		
		@Override
		protected void parseJsonProperties(JSONObject json) {
			JsonUtil.optFunString(json, "color", text -> color = Renderer.stringToColor(text));			
		}	

		@Override
		protected void toYamlProperties(Map<String, Object> yamlMap) {
			yamlMap.put("color", Renderer.colorToString(color));
		}

		@Override
		protected void writeJsonProperties(JSONWriter json) {
			JsonUtil.put(json, "color", Renderer.colorToString(color));			
		}	
	}

	public static class LineStyleBox extends LineStyleColor {

		@Override
		public void draw(Graphics2D gc, Drawer drawer, Vec<Object[]> lines) {
			gc.setColor(color);
			Stroke s = new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			gc.setStroke(s);
			for(Object[] line:lines) {
				drawer.drawPolyline(line);
			}			
		}		
	}
}