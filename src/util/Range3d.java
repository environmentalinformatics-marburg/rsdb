package util;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONWriter;

import util.yaml.YamlMap;

public class Range3d {
	public static final Range3d ZERO = new Range3d(0, 0, 0, 0, 0, 0);
	
	public final int xmin;
	public final int ymin;
	public final int zmin;	
	public final int xmax;
	public final int ymax;
	public final int zmax;
	
	public Range3d(int xmin, int ymin, int zmin, int xmax, int ymax, int zmax) {
		this.xmin = xmin;
		this.ymin = ymin;
		this.zmin = zmin;
		this.xmax = xmax;
		this.ymax = ymax;
		this.zmax = zmax;
	}

	public boolean isEmpty() {
		return xmax == Integer.MIN_VALUE;
	}
	
	public int xlen() {
		return xmax - xmin + 1;
	}
	
	public int ylen() {
		return ymax - ymin + 1;
	}
	
	public int zlen() {
		return zmax - zmin + 1;
	}
	
	public int xyzlen() {
		return xlen() * ylen() * zlen();
	}

	public Range3d add(int x, int y, int z) {
		return new Range3d(xmin + x, ymin + y, zmin + z, xmax + x, ymax + y, zmax + z);
	}
	
	public Range3d add(int xminAdd, int yminAdd, int zminAdd, int xmaxAdd, int ymaxAdd, int zmaxAdd) {
		return new Range3d(xmin + xminAdd, ymin + yminAdd, zmin + zminAdd, xmax + xmaxAdd, ymax + ymaxAdd, zmax + zmaxAdd);
	}
	
	public Range3d mul(int f) {
		return new Range3d(xmin * f, ymin * f, zmin * f, xmax * f, ymax * f, zmax * f);
	}
	
	public Range3d div(int x, int y, int z) {
		return new Range3d(xmin / x, ymin / y, zmin / z, xmax / x, ymax / y, zmax / z);
	}
	
	public Range3d overlapping(Range3d range) {
		int oxmin = Math.max(xmin, range.xmin);
		int oymin = Math.max(ymin, range.ymin);
		int ozmin = Math.max(zmin, range.zmin);
		int oxmax = Math.min(xmax, range.xmax);
		int oymax = Math.min(ymax, range.ymax);
		int ozmax = Math.min(zmax, range.zmax);
		return new Range3d(oxmin, oymin, ozmin, oxmax, oymax, ozmax);
	}
	
	@Override
	public String toString() {
		return "Range3d [x:" + xmin + " y:" + ymin + " z:" + zmin + " - x:" + xmax + " y:" + ymax
				+ " z:" + zmax + "]";
	}

	public static Range3d ofYaml(YamlMap yamlMap) {
		int xmin = yamlMap.getInt("xmin");
		int ymin = yamlMap.getInt("ymin");
		int zmin = yamlMap.getInt("zmin");
		int xmax = yamlMap.getInt("xmax");
		int ymax = yamlMap.getInt("ymax");
		int zmax = yamlMap.getInt("zmax");
		return new Range3d(xmin, ymin, zmin, xmax, ymax, zmax);
	}

	public Map<String, Integer> toYaml() {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
		map.put("xmin", xmin);
		map.put("ymin", ymin);
		map.put("zmin", zmin);
		map.put("xmax", xmax);
		map.put("ymax", ymax);
		map.put("zmax", zmax);
		return map;
	}

	public void toJSON(JSONWriter json) {
		json.object();
		json.key("xmin");
		json.value(xmin);	
		json.key("xmax");
		json.value(xmax);	
		json.key("ymin");
		json.value(ymin);	
		json.key("ymax");
		json.value(ymax);
		json.key("zmin");
		json.value(zmin);	
		json.key("zmax");
		json.value(zmax);
		json.endObject();	
	}
}
