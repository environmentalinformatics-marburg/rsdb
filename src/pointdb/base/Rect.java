package pointdb.base;

import java.util.Arrays;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;

import util.Web;

/**
 * value class
 * @author woellauer
 *
 */
public class Rect {
	private static final Logger log = LogManager.getLogger();
	
	public final long utmm_min_x;
	public final long utmm_min_y;
	public final long utmm_max_x;
	public final long utmm_max_y;

	public static Rect of_UTM(double utm_min_x, double utm_min_y, double utm_max_x, double utm_max_y) {
		long utmm_min_x = PdbConst.to_utmm(utm_min_x);
		long utmm_min_y = PdbConst.to_utmm(utm_min_y);
		long utmm_max_x = PdbConst.to_utmm(utm_max_x);
		long utmm_max_y = PdbConst.to_utmm(utm_max_y);
		return new Rect(utmm_min_x, utmm_min_y, utmm_max_x, utmm_max_y);
	}
	
	public static Rect of_UTM_request(Request request) {
		double x1 = Web.getDouble(request, "x1");
		double y1 = Web.getDouble(request, "y1");
		double x2 = Web.getDouble(request, "x2");
		double y2 = Web.getDouble(request, "y2");
		double utm_min_x = x1<x2?x1:x2; 
		double utm_min_y = y1<y2?y1:y2;
		double utm_max_x = x1<x2?x2:x1; 
		double utm_max_y = y1<y2?y2:y1;
		return of_UTM(utm_min_x, utm_min_y, utm_max_x, utm_max_y);
	}
	
	public static Rect of_extent_request(Request request) {
		double[] ext = Web.getDoubles(request, "ext");
		if(ext.length!=4) {
			throw new RuntimeException("parameter ext needs four values: "+Arrays.toString(ext));
		}
		double x1 = ext[0];
		double x2 = ext[1];
		double y1 = ext[2];		
		double y2 = ext[3];
		double x_min = x1<x2?x1:x2;
		double y_min = y1<y2?y1:y2;
		double x_max = x1<x2?x2:x1;
		double y_max = y1<y2?y2:y1;
		return of_UTM(x_min, y_min, x_max, y_max);
	}

	public static Rect of_UTMM(long utmm_min_x, long utmm_min_y, long utmm_max_x, long utmm_max_y) {
		return new Rect(utmm_min_x, utmm_min_y, utmm_max_x, utmm_max_y);
	}

	public static Rect of_utm_center(int utm_center_x, int utm_center_y, int screen_width, int screen_height, int TILE_LOCAL_TO_SCREEN_DIV) {
		if(screen_width<1 || screen_height<1) {
			return null;
		}		

		long utmm_width = screen_width*TILE_LOCAL_TO_SCREEN_DIV;
		long utmm_height = screen_height*TILE_LOCAL_TO_SCREEN_DIV;

		long utmm_min_x = ((long)utm_center_x)*PdbConst.LOCAL_SCALE_FACTOR - utmm_width/2;
		long utmm_min_y = ((long)utm_center_y)*PdbConst.LOCAL_SCALE_FACTOR  - utmm_height/2;
		long utmm_max_x = utmm_min_x + utmm_width - TILE_LOCAL_TO_SCREEN_DIV;
		long utmm_max_y = utmm_min_y + utmm_height - TILE_LOCAL_TO_SCREEN_DIV;

		return new Rect(utmm_min_x, utmm_min_y, utmm_max_x, utmm_max_y);
	}

	/**
	 * Create bounding box of polygon.
	 * @param polygon
	 * @return
	 */
	public static Rect of_polygon(Point2d[] polygon) {
		PolygonUtil.validatePolygon(polygon);
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		for(Point2d p:polygon) {
			if(p.x<minX) minX = p.x;
			if(p.y<minY) minY = p.y;
			if(p.x>maxX) maxX = p.x;
			if(p.y>maxY) maxY = p.y;
		}
		return of_UTM(minX,minY,maxX,maxY);
	}
	
	public static Rect of_points(Point[] points) {
		long xmin = Long.MAX_VALUE;
		long ymin = Long.MAX_VALUE;
		long xmax = Long.MIN_VALUE;
		long ymax = Long.MIN_VALUE;
		for(Point p:points) {
			if(p.x<xmin) xmin = p.x;
			if(p.y<ymin) ymin = p.y;
			if(p.x>xmax) xmax = p.x;
			if(p.y>ymax) ymax = p.y;
		}
		return new Rect(xmin, ymin, xmax, ymax);
	}
	
	public Rect(long utmm_min_x, long utmm_min_y, long utmm_max_x, long utmm_max_y) {
		this.utmm_min_x = utmm_min_x;
		this.utmm_min_y = utmm_min_y;
		this.utmm_max_x = utmm_max_x;
		this.utmm_max_y = utmm_max_y;
	}
	
	public double getUTMd_min_x() {
		return PdbConst.utmmToDouble(utmm_min_x);
	}
	
	public double getUTMd_min_y() {
		return PdbConst.utmmToDouble(utmm_min_y);
	}
	
	public double getUTMd_max_x() {
		return PdbConst.utmmToDouble(utmm_max_x);
	}
	
	public double getUTMd_max_y() {
		return PdbConst.utmmToDouble(utmm_max_y);
	}
	
	/**
	 * upper bound excluding returned value
	 */
	public double getUTMd_max_x_exclusive() {
		return PdbConst.utmmToDouble(utmm_max_x + 1);
	}
	
	/**
	 * upper bound excluding returned value
	 */
	public double getUTMd_max_y_exclusive() {
		return PdbConst.utmmToDouble(utmm_max_y + 1);
	}
	
	public int getInteger_UTM_min_x() {
		return (int) (this.utmm_min_x/1000);
	}
	
	public int getInteger_UTM_min_y() {
		return (int) (this.utmm_min_y/1000);
	}
	
	public int getInteger_UTM_max_x() {
		return (int) (this.utmm_max_x/1000);
	}
	
	public int getInteger_UTM_max_y() {
		return (int) (this.utmm_max_y/1000);
	}

	/**
	 * rect in UTM (meter)
	 */
	@Override
	public String toString() {
		return "rect(" + utmm_min_x/1000d + ", " + utmm_min_y/1000d + " - " + utmm_max_x/1000d
				+ ", " + utmm_max_y/1000d + ")";
	}
	
	public double getArea() {
		return ((utmm_max_x-utmm_min_x+1)*(utmm_max_y-utmm_min_y+1))/1000000d;
	}
	
	private static long alignToSmallerMeter(long v) {
		return v-(v%1000);
	}
	
	private static long alignToLargerMeter(long v) {
		/*if(v%1000==0) {
			return v;
		}*/
		return v+(1000-(v%1000));
	}
	
	public Rect innerMeterRect() {
		long min_x = alignToLargerMeter(this.utmm_min_x);
		long min_y = alignToLargerMeter(this.utmm_min_y);
		long max_x = alignToSmallerMeter(this.utmm_max_x)-1;
		long max_y= alignToSmallerMeter(this.utmm_max_y)-1;
		return new Rect(min_x, min_y, max_x, max_y);
	}
	
	public Rect outerMeterRect() {
		long min_x = alignToSmallerMeter(this.utmm_min_x);
		long min_y = alignToSmallerMeter(this.utmm_min_y);
		long max_x = alignToLargerMeter(this.utmm_max_x)-1;
		long max_y= alignToLargerMeter(this.utmm_max_y)-1;
		return new Rect(min_x, min_y, max_x, max_y);
	}
	
	public Rect withBorderUTM(double d) {
		long c = (long) (d*1000d);
		return new Rect(utmm_min_x-c, utmm_min_y-c, utmm_max_x+c, utmm_max_y+c);
	}
	
	public Rect withBorderUTM(int d) {
		long c = ((long) d)*1000;
		return new Rect(utmm_min_x-c, utmm_min_y-c, utmm_max_x+c, utmm_max_y+c);
	}

	public Rect transform(double x, double y) {
		long tx = (long) (x*1000d);
		long ty = (long) (y*1000d);
		return new Rect(this.utmm_min_x + tx, this.utmm_min_y + ty, this.utmm_max_x + tx, this.utmm_max_y + ty);
	}
	
	@FunctionalInterface
	public static interface TileRectConsumer {
		void accept(long xtile, long ytile, Rect rect);
	}

	public void tiles_utmm(long xsize_utmm, long ysize_utmm, TileRectConsumer consumer) {
		long xtilemin = (long) Math.floor(utmm_min_x / xsize_utmm);
		long ytilemin = (long) Math.floor(utmm_min_y / ysize_utmm);
		long xtilemax = (long) Math.floor(utmm_max_x / xsize_utmm);
		long ytilemax = (long) Math.floor(utmm_max_y / ysize_utmm);
		for(long ytile = ytilemin; ytile <= ytilemax;  ytile++) {
			for(long xtile = xtilemin; xtile <= xtilemax;  xtile++) {
				long x = xtile * xsize_utmm;
				long y = ytile * ysize_utmm;
				long xtmin = x;
				long ytmin = y;
				long xtmax = x + xsize_utmm - 1;
				long ytmax = y + ysize_utmm - 1;
				xtmin = xtmin < utmm_min_x ? utmm_min_x : xtmin;
				ytmin = ytmin < utmm_min_y ? utmm_min_y : ytmin;
				xtmax = utmm_max_x < xtmax ? utmm_max_x : xtmax;
				ytmax = utmm_max_y < ytmax ? utmm_max_y : ytmax;
				Rect rect = Rect.of_UTMM(xtmin, ytmin, xtmax, ytmax);
				consumer.accept(xtile, ytile, rect);
			}
		}
	}
}
