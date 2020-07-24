package rasterunit;

public class KeyRange {
	public final int xmin;
	public final int ymin;
	public final int bmin;
	public final int tmin;
	public final int xmax;
	public final int ymax;
	public final int bmax;
	public final int tmax;

	public KeyRange(int xmin, int ymin, int bmin, int tmin, int xmax, int ymax, int bmax, int tmax) {
		this.xmin = xmin;
		this.ymin = ymin;
		this.bmin = bmin;
		this.tmin = tmin;
		this.xmax = xmax;
		this.ymax = ymax;
		this.bmax = bmax;
		this.tmax = tmax;
	}

	public boolean isEmptyMarker() {
		return xmin == Integer.MAX_VALUE 
				&& ymin == Integer.MAX_VALUE 
				&& bmin == Integer.MAX_VALUE 
				&& tmin == Integer.MAX_VALUE 
				&& xmax == Integer.MIN_VALUE 
				&& ymax == Integer.MIN_VALUE
				&& bmax == Integer.MIN_VALUE
				&& tmax == Integer.MIN_VALUE;
	}

	@Override
	public String toString() {
		return "KeyRange [xmin=" + xmin + ", ymin=" + ymin + ", bmin=" + bmin + ", tmin=" + tmin + ", xmax=" + xmax
				+ ", ymax=" + ymax + ", bmax=" + bmax + ", tmax=" + tmax + "]";
	}
}
