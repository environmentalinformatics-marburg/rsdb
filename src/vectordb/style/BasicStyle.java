package vectordb.style;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONWriter;

import pointdb.base.Point2d;
import util.JsonUtil;
import util.collections.vec.Vec;
import util.yaml.YamlMap;
import vectordb.Renderer;
import vectordb.Renderer.Drawer;
import vectordb.Renderer.Drawer.PolygonDrawer;


public class BasicStyle extends Style implements PolygonDrawer {
	
	private Stroke stroke = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);	
	private Color stroke_color = Renderer.COLOR_POINT;	
	private Color fill_color = Renderer.COLOR_POLYGON;
	
	public BasicStyle() {}
	
	@Override
	public void parseYamlProperties(YamlMap yamlMap) {
		yamlMap.optFunString("stroke_color", text -> stroke_color = Renderer.stringToColor(text));
		yamlMap.optFunString("fill_color", text -> fill_color = Renderer.stringToColor(text));
	}
	
	@Override
	protected void parseJsonProperties(JSONObject json) {
		JsonUtil.optFunString(json, "stroke_color", text -> stroke_color = Renderer.stringToColor(text));
		JsonUtil.optFunString(json, "fill_color", text -> fill_color = Renderer.stringToColor(text));
	}	

	@Override
	protected void toYamlProperties(Map<String, Object> yamlMap) {
		yamlMap.put("stroke_color", Renderer.colorToString(stroke_color));
		yamlMap.put("fill_color", Renderer.colorToString(fill_color));
	}

	@Override
	protected void writeJsonProperties(JSONWriter json) {
		JsonUtil.put(json, "stroke_color", Renderer.colorToString(stroke_color));
		JsonUtil.put(json, "fill_color", Renderer.colorToString(fill_color));
	}	
	
	@Override
	public void drawPoints(Graphics2D gc, Drawer drawer, Vec<Point2d> points) {
		gc.setStroke(stroke);
		gc.setColor(stroke_color);
		for(Point2d p:points) {
			drawer.drawPointCross(p.x, p.y);
		}	
	}

	@Override
	public void drawLines(Graphics2D gc, Drawer drawer, Vec<Object[]> lines) {
		gc.setStroke(stroke);
		gc.setColor(stroke_color);
		for(Object[] line:lines) {
			drawer.drawPolyline(line);
		}		
	}

	@Override
	public void drawPolygons(Graphics2D gc, Drawer drawer, Vec<Object[]> polygons) {
		for(Object[] polygon:polygons) {
			drawer.drawPolygon(polygon, this);
		}		
	}

	@Override
	public void drawPolygons(Graphics2D gc, int[] xs, int[] ys, int len) {
		gc.setColor(fill_color);
		gc.fillPolygon(xs, ys, len);
		gc.setStroke(stroke);
		gc.setColor(stroke_color);
		gc.drawPolygon(xs, ys, len);		
	}	
}
