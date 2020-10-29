package util;

import java.util.LinkedHashMap;
import java.util.Map;

import util.yaml.YamlMap;

public class Range2d {
	public final int xmin;
	public final int ymin;
	public final int xmax;
	public final int ymax;

	public static Range2d ofCenter(int x, int y, int width, int height) {
		int xmin = x - width / 2;
		int ymin = y - height / 2;
		int xmax = xmin + width - 1;
		int ymax = ymin + height - 1;
		return new Range2d(xmin, ymin, xmax, ymax);		
	}
	
	public static Range2d ofCorner(int xmin, int ymin, int width, int height) {
		int xmax = xmin + width - 1;
		int ymax = ymin + height - 1;
		return new Range2d(xmin, ymin, xmax, ymax);		
	}

	public Range2d(int xmin, int ymin, int xmax, int ymax) {
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
	}

	public int getWidth() {
		return xmax - xmin + 1;
	}

	public int getHeight() {
		return ymax - ymin + 1;
	}
	
	public long getPixelCount() {
		return ((long)getWidth()) * ((long)getHeight());
	}

	public Range2d truncDiv(int d) {
		return new Range2d(xmin / d, ymin / d, xmax / d, ymax / d);
	}

	public Range2d floorDiv(int d) {
		return new Range2d(Math.floorDiv(xmin, d), Math.floorDiv(ymin, d), Math.floorDiv(xmax, d), Math.floorDiv(ymax, d));
	}

	public Range2d mul(int f) {
		return new Range2d(xmin * f, ymin * f, xmax * f, ymax * f);
	}
	
	public Range2d mulExpand(int f) {
		int expand = f - 1;
		return new Range2d(xmin * f, ymin * f, xmax * f + expand, ymax * f + expand);
	}

	public Range2d transposed() {
		return new Range2d(ymin, xmin, ymax, xmax);
	}

	public Range2d transposed(boolean transposed) {
		if(transposed) {
			return new Range2d(ymin, xmin, ymax, xmax);
		} else {
			return this;
		}
	}

	@Override
	public String toString() {
		return "Range2d [xmin=" + xmin + ", ymin=" + ymin + ", xmax=" + xmax + ", ymax=" + ymax + "] w: " + getWidth() + " h: " + getHeight();
	}

	public Range2d add(int xminAdd, int yminAdd, int xmaxAdd, int ymaxAdd) {
		return new Range2d(xmin + xminAdd, ymin + yminAdd, xmax + xmaxAdd, ymax + ymaxAdd);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + xmax;
		result = prime * result + xmin;
		result = prime * result + ymax;
		result = prime * result + ymin;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Range2d other = (Range2d) obj;
		if (xmax != other.xmax)
			return false;
		if (xmin != other.xmin)
			return false;
		if (ymax != other.ymax)
			return false;
		if (ymin != other.ymin)
			return false;
		return true;
	}

	public Map<String, Integer> toYaml() {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
		map.put("xmin", xmin);
		map.put("ymin", ymin);
		map.put("xmax", xmax);
		map.put("ymax", ymax);
		return map;
	}

	public static Range2d ofYaml(YamlMap yamlMap) {
		int xmin = yamlMap.getInt("xmin");
		int ymin = yamlMap.getInt("ymin");
		int xmax = yamlMap.getInt("xmax");
		int ymax = yamlMap.getInt("ymax");
		return new Range2d(xmin, ymin, xmax, ymax);
	}

	/**
	 * Clip this range by range of r.
	 * @param r
	 * @return clipped range or if not overlapping one pixel of this Range2d(xmin, ymin, xmin, ymin)
	 */
	public Range2d clip(Range2d r) {
		if(xmax < r.xmin || ymax < r.ymin || r.xmax < xmin || r.ymax < ymin) {
			return new Range2d(xmin, ymin, xmin, ymin);
		} else {
			int cxmin = xmin < r.xmin ? r.xmin : xmin;
			int cymin = ymin < r.ymin ? r.ymin : ymin;
			int cxmax = r.xmax < xmax ? r.xmax : xmax;
			int cymax = r.ymax < ymax ? r.ymax : ymax;
			return new Range2d(cxmin, cymin, cxmax, cymax);
		}
	}
	
	@FunctionalInterface
	public static interface TileRange2dConsumer {
		void accept(int xtile, int ytile, Range2d range2d);
	}
	
	public void tiled(int xsize, int ysize, TileRange2dConsumer consumer) {
		int fxmax = xmax - xmin;
		int fymax = ymax - ymin;
		int xtilemax = (int) Math.floor(fxmax / xsize);
		int ytilemax = (int) Math.floor(fymax / ysize);
		for(int ytile = 0; ytile <= ytilemax;  ytile++) {
			for(int xtile = 0; xtile <= xtilemax;  xtile++) {
				int xtmin = xmin + xtile * xsize;
				int ytmin = ymin + ytile * ysize;
				int xtmax = xtmin + xsize - 1;
				int ytmax = ytmin + ysize - 1;
				xtmax = xmax < xtmax ? xmax : xtmax;
				ytmax = ymax < ytmax ? ymax : ytmax;
				Range2d tile_range2d = new Range2d(xtmin, ytmin, xtmax, ytmax);
				consumer.accept(xtile, ytile, tile_range2d);
			}
		}
	}
	
	public boolean isEmptyMarker() {
		return xmin == Integer.MAX_VALUE && ymin == Integer.MAX_VALUE && xmax == Integer.MIN_VALUE && ymax == Integer.MIN_VALUE;
	}
}
