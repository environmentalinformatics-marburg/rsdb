package postgis.style;

import java.awt.Graphics2D;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import util.collections.vec.Vec;
import vectordb.style.ImgLabel;
import vectordb.style.Style;

public class StyleJtsGeometryRasterizer implements StyleJtsGeometryConsumer {

	public final double xoff;
	public final double yoff;
	public final double xscale;
	public final double yscale;

	public final Graphics2D gc;

	private final Vec<ImgLabel> labelBuffer = new Vec<ImgLabel>();

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
			style.drawImgPolygon(gc, xs, ys, len);			
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
			style.drawImgPolygonWithHoles(gc, rings);
		}

		if(text != null) {
			double lenScale = Math.max(xscale, xscale);
			double polyLen = polygon.getLength() * lenScale;
			if(polyLen >= 50d) {
				Point ptext = polygon.getInteriorPoint();
				if(!ptext.isEmpty()) {
					int xtext = (int) ((ptext.getX() + xoff) * xscale);
					int ytext = (int) ((yoff - ptext.getY()) * yscale);
					ImgLabel label = new ImgLabel(text, xtext, ytext);
					labelBuffer.add(label);
				}
			}
		}
	}

	@Override
	public void acceptLineString(Style style, LineString lineString, String text) {
		Coordinate[] cs = lineString.getCoordinateSequence().toCoordinateArray();
		int len = cs.length;
		int[] xs = new int[len];
		int[] ys = new int[len];
		for (int i = 0; i < len; i++) {
			Coordinate c = cs[i];
			xs[i] = (int) ((c.x + xoff) * xscale);
			ys[i] = (int) ((yoff - c.y) * yscale);
		}
		style.drawImgPolyline(gc, xs, ys, len);	

		if(text != null) {
			double lenScale = Math.max(xscale, xscale);
			double polyLen = lineString.getLength() * lenScale;
			if(polyLen >= 50d) {
				Point ptext = lineString.getInteriorPoint();
				if(!ptext.isEmpty()) {
					int xtext = (int) ((ptext.getX() + xoff) * xscale);
					int ytext = (int) ((yoff - ptext.getY()) * yscale);
					ImgLabel label = new ImgLabel(text, xtext, ytext);
					labelBuffer.add(label);
				}
			}
		}
	}

	@Override
	public void acceptPoint(Style style, Point point, String text) {
		double x = point.getX();
		double y = point.getY();
		int xi = (int) ((x + xoff) * xscale);
		int yi = (int) ((yoff - y) * yscale);

		style.drawImgPolyline(gc, new int[] {xi-5, xi+5}, new int[] {yi-7, yi+7}, 2);
		style.drawImgPolyline(gc, new int[] {xi+5, xi-5}, new int[] {yi-7, yi+7}, 2);	

		if(text != null) {
			ImgLabel label = new ImgLabel(text, xi, yi);
			labelBuffer.add(label);			
		}
	}

	public void drawLabels(Style style) {
		for (ImgLabel label : labelBuffer) {
			style.drawImgText(gc, label.x, label.y, label.text);
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