package pointcloud;

import java.util.LinkedHashMap;

import util.yaml.YamlMap;

public class DoublePoint {
	public final double x;
	public final double y;
	public DoublePoint(double x, double y) {
		this.x = x;
		this.y = y;
	}
	public Object toYaml() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("x", x);
		map.put("y", y);
		return map;
	}

	public static DoublePoint ofYaml(YamlMap yamlmap) {
		double x = yamlmap.getDouble("x");
		double y = yamlmap.getDouble("y");
		return new DoublePoint(x, y);	
	}
	@Override
	public String toString() {
		return "["+ x + " " + y + "]";
	}
	public DoublePoint mul(double a) {
		return new DoublePoint(x * a, y * a);
	}
}