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
import vectordb.Renderer.Drawer.PolygonDrawer;

public abstract class PolygonStyle implements PolygonDrawer {

	protected String polygon_type;

	protected abstract void toYaml(Map<String, Object> yamlMap);
	protected abstract void writeJsonProperties(JSONWriter json);
	public abstract void parseYaml(YamlMap yamlMap);
	protected abstract void parseJsonProperties(JSONObject json);
	
	public final void draw(Graphics2D gc, Drawer drawer, Vec<Object[]> polygons) {
		for(Object[] polygon:polygons) {
			drawer.drawPolygon(polygon, this);
		}		
	}

	public final Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		yamlMap.put("polygon_type", polygon_type);
		toYaml(yamlMap);
		return yamlMap;
	}
	
	public final void writeJson(JSONWriter json) {
		json.object();
		JsonUtil.put(json, "polygon_type", polygon_type);
		writeJsonProperties(json);
		json.endObject();
	}

	public static PolygonStyle ofYaml(YamlMap yamlMap) {
		String polygon_type = yamlMap.getString("polygon_type");
		PolygonStyle s = ofType(polygon_type);
		s.polygon_type = polygon_type;
		s.parseYaml(yamlMap);
		return s;
	}
	
	public static PolygonStyle ofJSON(JSONObject json) {
		String polygon_type = json.getString("polygon_type");
		PolygonStyle s = ofType(polygon_type);
		s.polygon_type = polygon_type;
		s.parseJsonProperties(json);
		return s;
	}

	public static PolygonStyle ofType(String polygon_type) {
		switch(polygon_type) {
		case "box":
			return new PolygonStyleBox();
		default: 
			throw new RuntimeException("unknown polygon_type: " + polygon_type);
		}			
	}
	
	public static abstract class PolygonStyleColor extends PolygonStyle {
		protected Color color = Renderer.COLOR_POLYGON;
		protected Color outline_color = Renderer.COLOR_POLYGON_OUTLINE;

		@Override
		public void parseYaml(YamlMap yamlMap) {
			yamlMap.optFunString("color", text -> color = Renderer.stringToColor(text));
			yamlMap.optFunString("outline_color", text -> outline_color = Renderer.stringToColor(text));
		}
		
		@Override
		protected void parseJsonProperties(JSONObject json) {
			JsonUtil.optFunString(json, "color", text -> color = Renderer.stringToColor(text));
			JsonUtil.optFunString(json, "outline_color", text -> outline_color = Renderer.stringToColor(text));	
		}

		@Override
		protected void toYaml(Map<String, Object> yamlMap) {
			yamlMap.put("color", Renderer.colorToString(color));
			yamlMap.put("outline_color", Renderer.colorToString(outline_color));
		}
		
		@Override
		protected void writeJsonProperties(JSONWriter json) {
			JsonUtil.put(json, "color", Renderer.colorToString(color));
			JsonUtil.put(json, "outline_color", Renderer.colorToString(outline_color));	
		}	
	}

	public static class PolygonStyleBox extends PolygonStyleColor {
		Stroke s = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		
		@Override
		public void drawPolygon(Graphics2D gc, int[] xs, int[] ys, int len) {
			gc.setColor(color);
			gc.fillPolygon(xs, ys, len);
			gc.setColor(outline_color);
			gc.setStroke(s);
			gc.drawPolygon(xs, ys, len);
		}
	}
}