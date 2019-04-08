package pointdb.base;

public class Point2d {
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
		return a.x*b.y - b.x*a.y;
	}
	
	public static double determinant(double ax, double ay, double bx, double by) {
		return ax*by - bx*ay;
	}
}