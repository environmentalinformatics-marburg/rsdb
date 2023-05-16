package pointcloud;

import java.util.LinkedHashMap;

import org.json.JSONWriter;

import util.yaml.YamlMap;

public class P2d {
	public final double x;
	public final double y;
	public P2d(double x, double y) {
		this.x = x;
		this.y = y;
	}
	public Object toYaml() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("x", x);
		map.put("y", y);
		return map;
	}

	public static P2d ofYaml(YamlMap yamlmap) {
		double x = yamlmap.getDouble("x");
		double y = yamlmap.getDouble("y");
		return new P2d(x, y);	
	}
	@Override
	public String toString() {
		return "["+ x + " " + y + "]";
	}
	public P2d mul(double a) {
		return new P2d(x * a, y * a);
	}
	public void toJSON(JSONWriter json) {
		json.object();
		json.key("x");
		json.value(x);
		json.key("y");
		json.value(y);
		json.endObject();		
	}
}