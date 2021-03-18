package vectordb;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.LinkedHashMap;
import java.util.Map;

import util.collections.vec.Vec;
import util.yaml.YamlMap;
import vectordb.Renderer.Drawer;
import vectordb.Renderer.Drawer.PolygonDrawer;

public abstract class PolygonStyle implements PolygonDrawer {

	protected String polygon_type;

	public abstract void parseYaml(YamlMap yamlMap);		 
	protected abstract void toYaml(Map<String, Object> yamlMap);
	
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

	public static PolygonStyle ofYaml(YamlMap yamlMap) {
		String polygon_type = yamlMap.getString("polygon_type");
		PolygonStyle s = ofType(polygon_type);
		s.polygon_type = polygon_type;
		s.parseYaml(yamlMap);
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
		}

		@Override
		protected void toYaml(Map<String, Object> yamlMap) {
			yamlMap.put("color", Renderer.colorToString(color));
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