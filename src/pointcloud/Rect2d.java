package pointcloud;

public class Rect2d {
	public final double xmin;
	public final double ymin;
	public final double xmax;
	public final double ymax;	

	public Rect2d(double xmin, double ymin, double xmax, double ymax) {
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
	}
	
	@FunctionalInterface
	public static interface TileRectConsumer {
		void accept(long xtile, long ytile, Rect2d rect);
	}
	
	public void tiles_utmm(double xsize, double ysize, TileRectConsumer consumer) {
		long xtilemin = (long) Math.floor(xmin / xsize);
		long ytilemin = (long) Math.floor(ymin / ysize);
		long xtilemax = (long) Math.floor(xmax / xsize);
		long ytilemax = (long) Math.floor(ymax / ysize);
		for(long ytile = ytilemin; ytile <= ytilemax;  ytile++) {
			for(long xtile = xtilemin; xtile <= xtilemax;  xtile++) {
				
			}
		}
	}
	
	public void tiled(double xsize, double ysize, TileRectConsumer consumer) {
		double fxmax = xmax - xmin;
		double fymax = ymax - ymin;
		int xtilemax = (int) Math.floor(fxmax / xsize);
		int ytilemax = (int) Math.floor(fymax / ysize);
		for(int ytile = 0; ytile <= ytilemax;  ytile++) {
			for(int xtile = 0; xtile <= xtilemax;  xtile++) {
				double xtmin = xmin + xtile * xsize;
				double ytmin = ymin + ytile * ysize;
				double xtmax = Math.nextDown(xtmin + xsize); 
				double ytmax = Math.nextDown(ytmin + ysize);
				xtmax = xmax < xtmax ? xmax : xtmax;
				ytmax = ymax < ytmax ? ymax : ytmax;
				Rect2d tileRect = new Rect2d(xtmin, ytmin, xtmax, ytmax);
				consumer.accept(xtile, ytile, tileRect);
			}
		}
	}
}
