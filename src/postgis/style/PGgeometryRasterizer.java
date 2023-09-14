package postgis.style;

import java.awt.Graphics2D;

import net.postgis.jdbc.geometry.LinearRing;
import net.postgis.jdbc.geometry.Point;
import net.postgis.jdbc.geometry.Polygon;
import postgis.PGgeometryConsumer;
import vectordb.style.Style;

public class PGgeometryRasterizer implements PGgeometryConsumer {
	
	public final double xoff;
	public final double yoff;
	public final double xscale;
	public final double yscale;
	
	public final Style style;
	public final Graphics2D gc;

	public PGgeometryRasterizer(double xoff, double yoff, double xscale, double yscale, Style style, Graphics2D gc) {
		this.xoff = xoff;
		this.yoff = yoff;
		this.xscale = xscale;
		this.yscale = yscale;
		this.style = style;
		this.gc = gc;
	}
	
	@Override
	public void acceptPolygon(Polygon polygon) {
		int numRings = polygon.numRings();
		if(numRings > 0) {
			if(numRings > 1) {
				//Logger.warn("more than one ring: " + numRings);
			}
			LinearRing linearRing = polygon.getRing(0);
			Point[] points = linearRing.getPoints();
			
			int len = points.length;
			int[] xs = new int[len];
			int[] ys = new int[len];
			for (int i = 0; i < len; i++) {
				Point p = points[i];
				xs[i] = (int) ((p.x + xoff) * xscale);
				ys[i] = (int) ((yoff - p.y) * yscale);
				//Logger.info("polygon point " + xs[i] + " " + ys[i]);
			}
			
			style.drawPolygon(gc, xs, ys, len);
		}
	}	
}