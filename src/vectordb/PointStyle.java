package vectordb;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONWriter;

import pointdb.base.Point2d;
import util.JsonUtil;
import util.collections.vec.Vec;
import util.yaml.YamlMap;
import vectordb.Renderer.Drawer;

public abstract class PointStyle {

	protected String point_type;

	public abstract void draw(Graphics2D gc, Drawer drawer, Vec<Point2d> points);
	public abstract void parseYamlProperties(YamlMap yamlMap);		 
	protected abstract void parseJsonProperties(JSONObject json);
	protected abstract void toYamlProperties(Map<String, Object> yamlMap);
	protected abstract void writeJsonProperties(JSONWriter json);

	public final Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		yamlMap.put("point_type", point_type);
		toYamlProperties(yamlMap);
		return yamlMap;
	}
	
	public final void writeJson(JSONWriter json) {
		json.object();
		JsonUtil.put(json, "point_type", point_type);
		writeJsonProperties(json);
		json.endObject();
	}

	public static PointStyle ofYaml(YamlMap yamlMap) {
		String point_type = yamlMap.getString("point_type");
		PointStyle s = ofType(point_type);
		s.point_type = point_type;
		s.parseYamlProperties(yamlMap);
		return s;
	}
	
	public static PointStyle ofJSON(JSONObject json) {
		String point_type = json.getString("point_type");
		PointStyle s = ofType(point_type);
		s.point_type = point_type;
		s.parseJsonProperties(json);
		return s;
	}
	
	public static PointStyle ofType(String point_type) {
		switch(point_type) {
		case "box":
			return new PointStyleBox();
		default: 
			throw new RuntimeException("unknown point_type: " + point_type);
		}			
	}
	
	public static abstract class PointStyleColor extends PointStyle {
		protected Color color = Renderer.COLOR_POINT;

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

	public static class PointStyleBox extends PointStyleColor {
		@Override
		public void draw(Graphics2D gc, Drawer drawer, Vec<Point2d> points) {
			gc.setColor(color);
			Stroke s = new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			gc.setStroke(s);
			for(Point2d p:points) {
				drawer.drawPointCross(p.x, p.y);
			}			
		}		
	}
}