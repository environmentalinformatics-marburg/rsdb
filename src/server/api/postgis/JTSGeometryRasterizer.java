package server.api.postgis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import postgis.JTSGeometryConsumer;
import vectordb.style.BasicStyle;
import vectordb.style.Style;

public class JTSGeometryRasterizer implements JTSGeometryConsumer {

	public final double xoff;
	public final double yoff;
	public final double xscale;
	public final double yscale;

	public final Style style;
	public final Graphics2D gc;	

	BasicStroke stroke = BasicStyle.createStroke(1);
	Color stroke_color = new Color(0, 50, 0, 100); 
	Color fill_color = new Color(0, 150, 0, 100);

	public JTSGeometryRasterizer(double xoff, double yoff, double xscale, double yscale, Style style, Graphics2D gc) {
		this.xoff = xoff;
		this.yoff = yoff;
		this.xscale = xscale;
		this.yscale = yscale;
		this.style = style;
		this.gc = gc;
	}

	@Override
	public void acceptPolygon(Polygon polygon) {
		final int interiorRings = polygon.getNumInteriorRing();
		if(interiorRings == 0) {
			LinearRing ring = polygon.getExteriorRing();
			Coordinate[] cs = ring.getCoordinateSequence().toCoordinateArray();
			int len = cs.length;
			int[] xs = new int[len];
			int[] ys = new int[len];
			for (int i = 0; i < len; i++) {
				Coordinate c = cs[i];
				xs[i] = (int) ((c.x + xoff) * xscale);
				ys[i] = (int) ((yoff - c.y) * yscale);
			}
			style.drawPolygon(gc, xs, ys, len);
		} else {
			int[][][] rings = new int[interiorRings + 1][][];
			{
				LinearRing ring = polygon.getExteriorRing();
				Coordinate[] cs = ring.getCoordinateSequence().toCoordinateArray();
				rings[0] = convert(cs);
			}			
			
			for (int r = 0; r < interiorRings; r++) {
				LinearRing ring = polygon.getInteriorRingN(r);
				Coordinate[] cs = ring.getCoordinateSequence().toCoordinateArray();
				rings[r + 1] = convert(cs);
			}
			style.drawPolygonWithHoles(gc, rings);
		}
	}

	private int[][] convert(Coordinate[] cs) {
		int len = cs.length;
		int[] xs = new int[len];
		int[] ys = new int[len];
		for (int i = 0; i < len; i++) {
			Coordinate c = cs[i];
			xs[i] = (int) ((c.x + xoff) * xscale);
			ys[i] = (int) ((yoff - c.y) * yscale);
		}
		return new int[][] {xs, ys};
	}
}