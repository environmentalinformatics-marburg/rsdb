package server.api.postgis;

import java.awt.Graphics2D;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import postgis.PostgisLayer.JTSGeometryConsumer;
import vectordb.style.Style;

public class JTSGeometryConverter implements JTSGeometryConsumer {

	public final double xoff;
	public final double yoff;
	public final double xscale;
	public final double yscale;

	public final Style style;
	public final Graphics2D gc;

	public JTSGeometryConverter(double xoff, double yoff, double xscale, double yscale, Style style, Graphics2D gc) {
		this.xoff = xoff;
		this.yoff = yoff;
		this.xscale = xscale;
		this.yscale = yscale;
		this.style = style;
		this.gc = gc;
	}

	@Override
	public void acceptPolygon(Polygon polygon) {
		LinearRing ring = polygon.getExteriorRing();
		if(polygon.getNumInteriorRing() > 0) {
			//Logger.warn("inner rings ignored: " + numRings);
		}
		Coordinate[] cs = ring.getCoordinateSequence().toCoordinateArray();
		int len = cs.length;
		int[] xs = new int[len];
		int[] ys = new int[len];
		for (int i = 0; i < len; i++) {
			Coordinate c = cs[i];
			xs[i] = (int) ((c.x + xoff) * xscale);
			ys[i] = (int) ((yoff - c.y) * yscale);
			//Logger.info("polygon point " + xs[i] + " " + ys[i]);
		}
		style.drawPolygons(gc, xs, ys, len);
	}	
}