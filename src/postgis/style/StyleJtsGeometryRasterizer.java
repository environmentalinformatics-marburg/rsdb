package postgis.style;

import java.awt.Graphics2D;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import util.collections.vec.Vec;
import vectordb.style.Style;

public class StyleJtsGeometryRasterizer implements StyleJtsGeometryConsumer {

	public final double xoff;
	public final double yoff;
	public final double xscale;
	public final double yscale;

	public final Graphics2D gc;

	private static class Label {
		public final String text;
		public final int x;
		public final int y;

		public Label(String text, int x, int y) {
			this.text = text;
			this.x = x;
			this.y = y;
		}
	}

	private final Vec<Label> labelBuffer = new Vec<Label>();

	public StyleJtsGeometryRasterizer(double xoff, double yoff, double xscale, double yscale, Graphics2D gc) {
		this.xoff = xoff;
		this.yoff = yoff;
		this.xscale = xscale;
		this.yscale = yscale;
		this.gc = gc;
	}

	@Override
	public void acceptPolygon(Style style, Polygon polygon, String text) {
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
			float[][] rings = new float[interiorRings + 1][];
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
		
		if(text != null) {
			double lenScale = Math.max(xscale, xscale);
			double polyLen = polygon.getLength() * lenScale;
			if(polyLen >= 50d) {
				Point ptext = polygon.getInteriorPoint();
				if(!ptext.isEmpty()) {
					int xtext = (int) ((ptext.getX() + xoff) * xscale);
					int ytext = (int) ((yoff - ptext.getY()) * yscale);
					Label label = new Label(text, xtext, ytext);
					labelBuffer.add(label);
				}
			}
		}
	}

	public void drawLabels(Style style) {
		for (Label label : labelBuffer) {
			style.drawText(gc, label.x, label.y, label.text);
		}
		labelBuffer.clear();
	}

	private float[] convert(Coordinate[] cs) {
		int len = cs.length;
		float[] vs = new float[len << 1];
		int pos = 0;
		for(Coordinate c : cs) {
			vs[pos++] = (int) ((c.x + xoff) * xscale);
			vs[pos++] = (int) ((yoff - c.y) * yscale);
		}
		return vs;
	}
}