package vectordb.style;

import java.awt.Graphics2D;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONWriter;

import pointdb.base.Point2d;
import util.JsonUtil;
import util.collections.vec.Vec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;
import vectordb.Renderer.Drawer;

public abstract class Style {
	
	private String type;
	private String title;
	
	protected abstract void toYamlProperties(Map<String, Object> yamlMap);
	protected abstract void writeJsonProperties(JSONWriter json);
	public abstract void parseYamlProperties(YamlMap yamlMap);		 
	protected abstract void parseJsonProperties(JSONObject json);
	
	public abstract void drawPoints(Graphics2D gc, Drawer drawer, Vec<Point2d> points);
	public abstract void drawLines(Graphics2D gc, Drawer drawer, Vec<Object[]> lines);
	public abstract void drawPolygons(Graphics2D gc, Drawer drawer, Vec<Object[]> polygons);
	public abstract void drawPolygon(Graphics2D gc, int[] xs, int[] ys, int len);
	public abstract void drawPolygonWithHoles(Graphics2D gc, float[][] rings);
	public abstract void drawText(Graphics2D gc, int x, int y, String text);
	
	public final Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		return toYaml(yamlMap);
	}
	
	public final Map<String, Object> toYaml(LinkedHashMap<String, Object> yamlMap) {
		yamlMap.put("type", type);
		YamlUtil.optPut(yamlMap, "title", title);
		toYamlProperties(yamlMap);
		return yamlMap;
	}
	
	public final void writeJson(JSONWriter json) {
		json.object();
		writeJsonInside(json);
		json.endObject();
	}
	
	public final void writeJsonInside(JSONWriter json) {
		JsonUtil.put(json, "type", type);
		JsonUtil.optPut(json, "title", title);
		writeJsonProperties(json);
	}

	public static Style ofYaml(YamlMap yamlMap) {
		String type = yamlMap.getString("type");
		String title = yamlMap.optString("title");
		Style style = ofType(type);
		style.type = type;
		style.title = title;
		style.parseYamlProperties(yamlMap);
		return style;
	}
	
	public static Style ofJSON(JSONObject json) {
		String type = json.getString("type");
		String title = json.optString("title");
		Style style = ofType(type);
		style.type = type;
		style.title = title;
		style.parseJsonProperties(json);
		return style;
	}
	
	public static BasicStyle ofType(String type) {
		switch(type) {
		case "basic":
			return new BasicStyle();
		default: 
			throw new RuntimeException("unknown style type: " + type);
		}			
	}
	public boolean hasTitle() {
		return title != null && !title.isBlank();
	}
	public String getTitle() {
		return title;
	}
}
