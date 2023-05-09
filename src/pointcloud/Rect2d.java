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

	public static Rect2d parseBbox(String[] bbox) {
		return new Rect2d(
				Double.parseDouble(bbox[0]), 
				Double.parseDouble(bbox[1]), 
				Double.parseDouble(bbox[2]),
				Double.parseDouble(bbox[3])
				);
	}

	public static Rect2d ofPoints(double[][] points) {
		double wmsXmin = Double.POSITIVE_INFINITY;
		double wmsYmin = Double.POSITIVE_INFINITY;
		double wmsXmax = Double.NEGATIVE_INFINITY;
		double wmsYmax = Double.NEGATIVE_INFINITY;
		for(double[] point : points) {
			double x = point[0];
			double y = point[1];
			if(x < wmsXmin) {
				wmsXmin = x;
			}
			if(y < wmsYmin) {
				wmsYmin = y;
			}
			if(x > wmsXmax) {
				wmsXmax = x;
			}
			if(y > wmsYmax) {
				wmsYmax = y;
			}
		}
		return new Rect2d(wmsXmin, wmsYmin, wmsXmax, wmsYmax);
	}

	@FunctionalInterface
	public static interface TileRect2dConsumer {
		void accept(long xtile, long ytile, Rect2d rect);
	}

	public void tiled(double xsize, double ysize, TileRect2dConsumer consumer) {
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

	@Override
	public String toString() {
		return "Rect2d [xmin=" + xmin + ", ymin=" + ymin + ", xmax=" + xmax + ", ymax=" + ymax + "]";
	}

	public double[][] createPoints4() {
		double[][] points = new double[][] {
			{xmin, ymin},
			{xmin, ymax},
			{xmax, ymin},
			{xmax, ymax},		
		};
		return points;
	}

	public double[][] createPoints9() {
		double xmid = (xmin + xmax) / 2d;
		double ymid = (ymin + ymax) / 2d;
		double[][] points = new double[][] {
			{xmin, ymin},
			{xmin, ymid},
			{xmin, ymax},			
			{xmid, ymin},
			{xmid, ymid},
			{xmid, ymax},			
			{xmax, ymin},
			{xmax, ymid},
			{xmax, ymax},		
		};
		return points;
	}
	
	public double[][] createPointsMidBorder() {
		double xmid = (xmin + xmax) / 2d;
		double ymid = (ymin + ymax) / 2d;
		double[][] points = new double[][] {
			{xmin, ymid},
			{xmid, ymin},
			{xmid, ymax},			
			{xmax, ymid},	
		};
		return points;
	}

	public double width() {
		return xmax - xmin;
	}

	public double height() {
		return ymax - ymin;
	}
}
