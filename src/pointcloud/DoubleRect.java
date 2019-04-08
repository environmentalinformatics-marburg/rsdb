package pointcloud;

public class DoubleRect {
	public final double xmin;
	public final double ymin;
	public final double xmax;
	public final double ymax;
	
	public DoubleRect(double xmin, double ymin, double xmax, double ymax) {
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
	}

	@Override
	public String toString() {
		return "rect [xmin=" + xmin + ", ymin=" + ymin + ", xmax=" + xmax + ", ymax=" + ymax + "]";
	}
}
