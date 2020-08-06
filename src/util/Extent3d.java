package util;

public class Extent3d {	
	public final double xmin;
	public final double ymin;
	public final double zmin;	
	public final double xmax;
	public final double ymax;
	public final double zmax;
	
	public Extent3d(double xmin, double ymin, double zmin, double xmax, double ymax, double zmax) {
		this.xmin = xmin;
		this.ymin = ymin;
		this.zmin = zmin;
		this.xmax = xmax;
		this.ymax = ymax;
		this.zmax = zmax;
	}
}
