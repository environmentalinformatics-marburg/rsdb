package util;

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
}
