package pointdb.subsetdsl;

import pointdb.base.Point2d;
import pointdb.base.PolygonUtil;
import pointdb.base.PolygonUtil.PolygonWithHoles;
import pointdb.base.Rect;

public class Region {	
	public final Rect bbox;
	//public final Point2d[] polygonPoints; //nullable if no polygon
	public final PolygonWithHoles[] polygons;  //nullable if no polygon
	
	/**
	 * 
	 * @param bbox Bounding box
	 * @param polygonPoints normally polygon is inside of bbox. In special cases bbox may be smaller than polygon to filter points in bbox with polygon borders.
	 */
	private Region(Rect bbox, PolygonWithHoles[] polygons) {
		this.bbox = bbox;
		this.polygons = polygons;
	}
	
	public static Region ofRect(Rect rect) {
		return new Region(rect, null);
	}
	
	public static Region ofPlainPolygon(Point2d[] polygon) {
		return new Region(Rect.of_polygon(polygon), new PolygonWithHoles[] {PolygonWithHoles.ofPolygon(polygon)});
	}
	
	public static Region ofPolygonsWithHoles(PolygonWithHoles[] polygons) {
		return new Region(PolygonUtil.PolygonsWithHoles.toRectBBOX(polygons), polygons);
	}
	
	public static Region ofPlainPolygon(double[] vx, double[] vy) {
		int len = vx.length;
		if(vy.length != len) {
			throw new RuntimeException();
		}
		Point2d[] polygonPoints = new Point2d[len];
		for (int i = 0; i < len; i++) {
			polygonPoints[i] = new Point2d(vx[i], vy[i]);
		}
		return ofPlainPolygon(polygonPoints);
	}
	
	public static Region ofFilteredBbox(Rect bbox, PolygonWithHoles[] polygons) {
		return new Region(bbox, polygons);
	}
	
	public boolean isBbox() {
		return polygons == null;
	}
	
	public double getArea() {
		return polygons == null ? bbox.getArea() : PolygonUtil.PolygonsWithHoles.area(polygons);
	}

	@Override
	public String toString() {
		return polygons == null ? "Region [bbox=" + bbox + "]" : "Region [bbox=" + bbox + ", polygons=" + polygons.length + "]";
	}
	
	public Region toBboxRegion() {
		return polygons == null ? this : Region.ofRect(bbox);
	}
}
