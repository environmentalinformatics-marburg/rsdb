package vectordb.style;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Iterator;
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
	private Color text_color = new Color(0, 0, 0, 255);

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
	public void drawGeoPoints(Graphics2D gc, Drawer drawer, Vec<Point2d> points) {
		gc.setStroke(stroke);
		gc.setColor(stroke_color);
		for(Point2d p:points) {
			drawer.drawPointCross(p.x, p.y);
		}	
	}

	@Override
	public void drawGeoLines(Graphics2D gc, Drawer drawer, Vec<Object[]> lines) {
		gc.setStroke(stroke);
		gc.setColor(stroke_color);
		for(Object[] line:lines) {
			drawer.drawPolyline(line);
		}		
	}

	@Override
	public void drawGeoPolygons(Graphics2D gc, Drawer drawer, Vec<Object[]> polygons) {
		for(Object[] polygon:polygons) {
			drawer.drawPolygon(polygon, this);
		}		
	}
	

	@Override
	public void drawGeoLabels(Graphics2D gc, Drawer drawer, Vec<GeoLabel> labels) {
		for (GeoLabel geoLabel : labels) {
			int x = (int) ((geoLabel.x + drawer.xoff) * drawer.xscale);
			int y = (int) ((drawer.yoff - geoLabel.y) * drawer.yscale);
			drawImgText(gc, x, y, geoLabel.text);
		}
	}


	@Override
	public void drawImgPolygon(Graphics2D gc, int[] xs, int[] ys, int len) {
		gc.setColor(fill_color);
		gc.fillPolygon(xs, ys, len);
		gc.setStroke(stroke);
		gc.setColor(stroke_color);
		gc.drawPolygon(xs, ys, len);
	}
	
	@Override
	public void drawImgPolyline(Graphics2D gc, int[] xs, int[] ys, int len) {
		gc.setStroke(stroke);
		gc.setColor(stroke_color);
		gc.drawPolyline(xs, ys, len);
	}

	@Override
	public void drawImgText(Graphics2D gc, int x, int y, String text) {
		gc.setStroke(stroke);
		gc.setColor(text_color);
		Rectangle2D bounds = gc.getFontMetrics().getStringBounds(text, gc);
		int xoff = (int) bounds.getCenterX();
		int yoff = (int) bounds.getCenterY();
		gc.drawString(text, x - xoff, y - yoff);
	}

	@Override
	public void drawImgPolygonWithHoles(Graphics2D gc, float[][] rings) {
		Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO);
		for(float[] ring : rings) {
			path.moveTo(ring[0], ring[1]);
			int len = ring.length;
			for (int pos = 2; pos < len;) {
				float x = ring[pos++];
				float y = ring[pos++];
				path.lineTo(x, y);
			}
		}
		gc.setColor(fill_color);
		gc.fill(path);
		gc.setStroke(stroke);
		gc.setColor(stroke_color);
		gc.draw(path);
	}
}
