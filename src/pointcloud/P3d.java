package pointcloud;

import java.util.Comparator;

public class P3d {	
	public final double x;
	public final double y;
	public final double z;
	
	public P3d(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public static final Comparator<P3d> Z_COMPARATOR = new Comparator<P3d>() {
		@Override
		public int compare(P3d o1, P3d o2) {
			return Double.compare(o1.z, o2.z);
		}
		
	};
}
