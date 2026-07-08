package pointcloud;

public class Rect3d {
	public final double xmin;
	public final double ymin;
	public final double zmin;
	public final double xmax;
	public final double ymax;	
	public final double zmax;

	public Rect3d(double xmin, double ymin, double zmin, double xmax, double ymax, double zmax) {
		this.xmin = xmin;
		this.ymin = ymin;
		this.zmin = zmin;
		this.xmax = xmax;
		this.ymax = ymax;
		this.zmax = zmax;
	}

	public double maxDimRange() {
		double dx = xmax - xmin;
		double dy = ymax - ymin;
		double dz = zmax - zmin;
		return Math.max(dx, Math.max(dy, dz));
	}
}
