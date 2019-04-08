package util;

public class MinMax2d {
	
	public double xmin = Double.MAX_VALUE;
	public double xmax = -Double.MAX_VALUE;
	public double ymin = Double.MAX_VALUE;
	public double ymax = -Double.MAX_VALUE;
	
	public void apply(double x, double y) {
		if(x<xmin) {
			xmin = x; 
		}
		if(x>xmax) {
			xmax = x; 
		}
		if(y<ymin) {
			ymin = y; 
		}
		if(y>ymax) {
			ymax = y; 
		}
	}
	
	public void apply(double[] v) {
		apply(v[0], v[1]);
	}

	@Override
	public String toString() {
		return xmin+" - "+xmax+" , "+ymin+" - "+ymax;
	}
	
	public double xrange() {
		return xmax-xmin;
	}
	
	public double yrange() {
		return ymax-ymin;
	}
	

}
