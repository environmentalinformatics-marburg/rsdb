package vectordb.style;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONWriter;
import org.tinylog.Logger;

import pointdb.base.Point2d;
import util.JsonUtil;
import util.collections.vec.Vec;
import util.yaml.YamlMap;
import vectordb.Renderer;
import vectordb.Renderer.Drawer;
import vectordb.Renderer.Drawer.PolygonDrawer;


public class BasicStyle extends Style implements PolygonDrawer {
	

	private static final float DEFAULT_STROKE_WIDTH = 2f;

	private BasicStroke stroke = createStroke(DEFAULT_STROKE_WIDTH);	
	private Color stroke_color = Renderer.COLOR_POINT;
	private Color fill_color = Renderer.COLOR_POLYGON;
	
	public static BasicStroke createStroke(float stroke_width) {
		return createStroke(stroke_width, null);
	}

	public static BasicStroke createStroke(float stroke_width, float[] stroke_dash) {
		float miterlimit = 10.0f;
		float dash_phase = 0.0f;
		return new BasicStroke(stroke_width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, miterlimit, stroke_dash, dash_phase);
	}

	public BasicStyle() {}
	
	public BasicStyle(BasicStroke stroke, Color stroke_color, Color fill_color) {
		this.stroke = stroke;
		this.stroke_color = stroke_color;
		this.fill_color = fill_color;
	}

	@Override
	public void parseYamlProperties(YamlMap yamlMap) {
		yamlMap.optFunString("stroke_color", text -> stroke_color = Renderer.stringToColor(text));
		float stroke_width = yamlMap.optFloat("stroke_width", DEFAULT_STROKE_WIDTH);
		float[] stroke_dash = yamlMap.optFloatArray("stroke_dash", null);
		//Logger.info(Arrays.toString(stroke_dash));
		stroke = createStroke(stroke_width, stroke_dash);
		yamlMap.optFunString("fill_color", text -> fill_color = Renderer.stringToColor(text));
	}

	@Override
	protected void parseJsonProperties(JSONObject json) {
		JsonUtil.optFunString(json, "stroke_color", text -> stroke_color = Renderer.stringToColor(text));
		float stroke_width = json.optFloat("stroke_width", DEFAULT_STROKE_WIDTH);
		float[] stroke_dash = JsonUtil.optFloatArray(json, "stroke_dash", null);
		Logger.info(Arrays.toString(stroke_dash));
		stroke = createStroke(stroke_width, stroke_dash);
		JsonUtil.optFunString(json, "fill_color", text -> fill_color = Renderer.stringToColor(text));
	}	

	@Override
	protected void toYamlProperties(Map<String, Object> yamlMap) {
		yamlMap.put("stroke_color", Renderer.colorToString(stroke_color));
		yamlMap.put("stroke_width", stroke.getLineWidth());
		float[] dash = stroke.getDashArray();
		if(dash != null && dash.length > 0) {
			if(dash.length == 1) {
				yamlMap.put("stroke_dash", dash[0]);
			} else {
				yamlMap.put("stroke_dash", dash);
			}
		}
		yamlMap.put("fill_color", Renderer.colorToString(fill_color));
	}

	@Override
	protected void writeJsonProperties(JSONWriter json) {
		JsonUtil.put(json, "stroke_color", Renderer.colorToString(stroke_color));
		JsonUtil.putFloat(json, "stroke_width", stroke.getLineWidth());
		float[] dash = stroke.getDashArray();
		if(dash != null && dash.length > 0) {
			JsonUtil.putFloatArray(json, "stroke_dash", dash);
		}
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
