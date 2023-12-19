package pointdb.base;

import java.util.LinkedHashSet;

public class Point2d {
	
	public static final Point2d NaN = new Point2d(Double.NaN, Double.NaN);
	
	public final double x;
	public final double y;

	public static Point2d of(double x, double y) {
		return new Point2d(x, y);
	}

	public Point2d(double x, double y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return equals((Point2d) obj);
	}

	public boolean equals(Point2d other) {
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Point2d [" + x + ", " + y + "]";
	}
	
	public static double determinant(Point2d a, Point2d b) {
		return a.x * b.y - b.x * a.y;
	}
	
	public static double determinant(double ax, double ay, double bx, double by) {
		return ax * by - bx * ay;
	}
	
	public boolean isFinite() {
		return Double.isFinite(x) && Double.isFinite(y);
	}
	
	public static Point2d[] toUnique(Point2d[] points) {
		LinkedHashSet<Point2d> set = new LinkedHashSet<Point2d>();
		for(Point2d p : points) {
			set.add(p);
		}
		if(set.size() == points.length) {
			return points;
		} else {
			Point2d[] result = new Point2d[set.size()];
			result = set.toArray(result);
			return result;
		}
	}
}