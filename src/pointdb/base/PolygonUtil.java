package pointdb.base;

import java.util.Arrays;

import pointdb.base.PolygonUtil.PolygonWithHoles;

/**
 * Point in polygon test
 * <p>
 * Modified source of "isLeft" and "wn_PnPoly" based on c++ source from http://geomalgorithms.com/a03-_inclusion.html
 * <p>
 * Copyright 2000 softSurfer, 2012 Dan Sunday
 * 
 * This code may be freely used and modified for any purpose
 * providing that this copyright notice is included with it.
 * 
 * SoftSurfer makes no warranty for this code, and cannot be held
 * liable for any real or imagined damage resulting from its use.
 * Users of this code must verify correctness for their application.
 *
 */
public class PolygonUtil {

	public static void validatePolygon(Point2d[] polygon) {
		if(polygon == null) {
			throw new RuntimeException("null polygon");
		}
		if(polygon.length < 4) {
			throw new RuntimeException("polygon needs at leat four points (first and last same point)  of " + polygon.length + " points");
		}
		/*if(!polygon[0].equals(polygon[polygon.length-1])) {
			throw new RuntimeException("fist and last point need to be same of " + polygon.length + " points");
		}
		if(polygon[0].equals(polygon[1])) {
			throw new RuntimeException("same fist and second of " + polygon.length + " points");
		}
		if(polygon[0].equals(polygon[2])) {
			throw new RuntimeException("same fist and third of " + polygon.length + " points");
		}
		if(polygon[1].equals(polygon[2])) {
			throw new RuntimeException("same second and third of " + polygon.length + " points");
		}*/
		for (int i = 0; i < polygon.length; i++) {
			if(!polygon[i].isFinite()) {
				throw new RuntimeException("poylgon point not finite: " + i + "   " + polygon[i].toString() + " of " + polygon.length + " points");
			}
		}
	}


	/**
	 * Tests if a point is Left|On|Right of an infinite line.
	 * Input:  three points p, a, and b
	 * @param a start of line
	 * @param b end of line
	 * @param p point to be tested
	 * @return >0 for p left of the line through a and b
	 *	       =0 for p  on the line
	 *	       <0 for p  right of the line
	 */
	public static double isLeft(Point2d a, Point2d b, GeoPoint p) {	
		return ( (b.x - a.x) * (p.y - a.y) - (p.x -  a.x) * (b.y - a.y) );
	}

	/**
	 * Tests if a point is Left|On|Right of an infinite line.
	 * Input:  three points p, a, and b
	 * @param a start of line
	 * @param b end of line
	 * @param p point to be tested
	 * @return >0 for p left of the line through a and b
	 *	       =0 for p  on the line
	 *	       <0 for p  right of the line
	 */
	public static double isLeft(double ax, double ay, double bx, double by, double px, double py) {	
		return ( (bx - ax) * (py - ay) - (px -  ax) * (by - ay) );
	}

	/**
	 * Winding number test for a point in a polygon.
	 * @param p a point
	 * @param v vertex points of a polygon V[n+1] with V[n]=V[0]
	 * @return the winding number (=0 only when p is outside)
	 */
	public static int wn_PnPoly(GeoPoint p, Point2d[] v) {
		int    wn = 0;    // the  winding number counter

		// loop through all edges of the polygon
		for (int i=0; i<v.length-1; i++) {   // edge from V[i] to  V[i+1]
			if (v[i].y <= p.y) {          // start y <= P.y
				if (v[i+1].y  > p.y)      // an upward crossing
					if (isLeft( v[i], v[i+1], p) > 0)  // P left of  edge
						++wn;            // have  a valid up intersect
			}
			else {                        // start y > P.y (no test needed)
				if (v[i+1].y  <= p.y)     // a downward crossing
					if (isLeft( v[i], v[i+1], p) < 0)  // P right of  edge
						--wn;            // have  a valid down intersect
			}
		}
		return wn;
	}

	/**
	 * Winding number test for a point in a polygon.
	 * @param p a point
	 * @param v vertex points of a polygon V[n+1] with V[n]=V[0]
	 * @return the winding number (=0 only when p is outside)
	 */
	public static int wn_PnPolyDirect(GeoPoint p, Point2d[] v) {
		int    wn = 0;    // the  winding number counter

		// loop through all edges of the polygon
		int N_1 = v.length-1;
		double px = p.x;
		double py = p.y;
		for (int i=0; i<N_1; i++) {   // edge from V[i] to  V[i+1]
			Point2d vi = v[i];
			double vix = vi.x;
			double viy = vi.y;
			Point2d vi1 = v[i+1];
			double vi1x = vi1.x;
			double vi1y = vi1.y;
			if (viy <= py) {          // start y <= P.y
				if (vi1y  > py)      // an upward crossing
					if (( (vi1x - vix) * (py - viy) - (px -  vix) * (vi1y - viy) ) > 0)  // P left of  edge
						++wn;            // have  a valid up intersect
			}
			else {                        // start y > P.y (no test needed)
				if (vi1y  <= py)     // a downward crossing
					if (( (vi1x - vix) * (py - viy) - (px -  vix) * (vi1y - viy) ) < 0)  // P right of  edge
						--wn;            // have  a valid down intersect
			}
		}
		return wn;
	}

	/**
	 * Winding number test for a point in a polygon.
	 * @param p a point
	 * @param v vertex points of a polygon V[n+1] with V[n]=V[0]
	 * @return the winding number (=0 only when p is outside)
	 */
	public static int wn_PnPolyDirectV(double px, double py, double[] vx, double[] vy) {
		int wn = 0;    // the  winding number counter

		// loop through all edges of the polygon
		int N_1 = vx.length - 1;
		for (int i = 0; i < N_1; i++) {   // edge from V[i] to  V[i+1]
			double vix = vx[i];
			double viy = vy[i];
			double vi1x = vx[i+1];
			double vi1y = vy[i+1];
			if (viy <= py) {          // start y <= P.y
				if (vi1y  > py) {      // an upward crossing
					if (( (vi1x - vix) * (py - viy) - (px -  vix) * (vi1y - viy) ) > 0) {  // P left of  edge
						wn++;            // have  a valid up intersect
					}
				}
			}
			else {                        // start y > P.y (no test needed)
				if (vi1y  <= py) {     // a downward crossing
					if (( (vi1x - vix) * (py - viy) - (px -  vix) * (vi1y - viy) ) < 0) {  // P right of  edge
						wn--;            // have  a valid down intersect
					}
				}
			}
		}
		return wn;
	}

	/**
	 * Winding number test for a point in a polygon.
	 * @param p a point
	 * @param v vertex points of a polygon V[n+1] with V[n]=V[0]
	 * @return the winding number (=0 only when p is outside)
	 */
	public static int wn_PnPolyDirectV2(double px, double py, double[] vx, double[] vy) { //fastest
		int wn = 0;    // the  winding number counter

		// loop through all edges of the polygon
		int N_1 = vx.length - 1;
		for (int i = 0; i < N_1; i++) {   // edge from V[i] to  V[i+1]
			double vix = vx[i];
			double viy = vy[i];
			double vi1x = vx[i+1];
			double vi1y = vy[i+1];
			double left = (vi1x - vix) * (py - viy) - (px -  vix) * (vi1y - viy);
			if (viy <= py) {          // start y <= P.y
				if (vi1y > py) {      // an upward crossing
					if (left > 0) {  // P left of  edge
						wn++;            // have  a valid up intersect
					}
				}
			}
			else {                        // start y > P.y (no test needed)
				if (vi1y <= py) {     // a downward crossing
					if (left < 0) {  // P right of  edge
						wn--;            // have  a valid down intersect
					}
				}
			}
		}
		return wn;
	}


	//derived from java.awt.Polygon
	public static boolean awtContains(double x, double y, double[] xpoints, double[] ypoints) {
		int npoints = xpoints.length;

		int hits = 0;

		double lastx = xpoints[npoints - 1];
		double lasty = ypoints[npoints - 1];
		double curx;
		double cury;

		// Walk the edges of the polygon
		for (int i = 0; i < npoints; lastx = curx, lasty = cury, i++) {
			curx = xpoints[i];
			cury = ypoints[i];

			if (cury == lasty) {
				continue;
			}

			double leftx;
			if (curx < lastx) {
				if (x >= lastx) {
					continue;
				}
				leftx = curx;
			} else {
				if (x >= curx) {
					continue;
				}
				leftx = lastx;
			}

			double test1, test2;
			if (cury < lasty) {
				if (y < cury || y >= lasty) {
					continue;
				}
				if (x < leftx) {
					hits++;
					continue;
				}
				test1 = x - curx;
				test2 = y - cury;
			} else {
				if (y < lasty || y >= cury) {
					continue;
				}
				if (x < leftx) {
					hits++;
					continue;
				}
				test1 = x - lastx;
				test2 = y - lasty;
			}

			if (test1 < (test2 / (lasty - cury) * (lastx - curx))) {
				hits++;
			}
		}

		return ((hits & 1) != 0);
	}

	public static double area_of_polygon(Point2d[] v) {
		return Math.abs(signed_area_of_polygon(v)); 
	}


	/**
	 * area of polygon
	 * not correct for self overlapping or twisted polygons
	 * 0 < area for counterclockwise points, area < 0 for clockwise points
	 * @param v
	 * @return
	 */
	public static double signed_area_of_polygon(Point2d[] v) { // reference: http://mathworld.wolfram.com/PolygonArea.html
		double sum = 0d;
		int len_1 = v.length-1;
		for (int i = 0; i < len_1; i++) {
			sum += Point2d.determinant(v[i], v[i+1]);
		}
		return sum / 2d;
	}

	public static Point2d centroid_of_polygon(Point2d[] v) { // reference: https://en.wikipedia.org/wiki/Centroid#Centroid_of_a_polygon
		double a6 = signed_area_of_polygon(v) * 6;		
		double xsum = 0;
		double ysum = 0;
		int len_1 = v.length-1;
		for (int i = 0; i < len_1; i++) {
			double d = Point2d.determinant(v[i], v[i+1]);
			xsum += (v[i].x + v[i+1].x) * d;
			ysum += (v[i].y + v[i+1].y) * d;
		}
		double x = xsum / a6;
		double y = ysum / a6;
		return new Point2d(x, y);
	}	

	public static Point2d meanOfPolygonPoints(Point2d[] points) {
		int cnt = points.length - 1;
		double xsum = 0;
		double ysum = 0;
		for (int i = 0; i < cnt; i++) {
			Point2d p = points[i];
			xsum += p.x;
			ysum += p.y;
		}
		return new Point2d(xsum / cnt, ysum / cnt);
	}	

	public static class PolygonWithHoles {
		public Point2d[] polygon; // not null
		public Point2d[][] holes; // nullable,  not Point2d[0][]

		public static PolygonWithHoles ofPolygon(Point2d[] polygon) {
			return new PolygonWithHoles(polygon);
		}
		
		public static PolygonWithHoles ofRings(Point2d[][] rings) {
			if(rings.length == 1) {
				return ofPolygon(rings[0]);
			} else {
				Point2d[][] holes = new Point2d[rings.length - 1][];
				for (int i = 1; i < rings.length; i++) {
					holes[i - 1] = rings[i];
				}
				return new PolygonWithHoles(rings[0], holes);
			}
		}

		public void validate() {
			PolygonUtil.validatePolygon(polygon);
			if(holes != null) {
				for(Point2d[] hole : holes) {
					PolygonUtil.validatePolygon(hole);
				}
			}			
		}

		public PolygonWithHoles(Point2d[] polygon, Point2d[] ...holes) {
			this.polygon = polygon;
			this.holes = holes;
		}

		public boolean hasHoles() {
			return holes != null;
		}

		public boolean hasNoHoles() {
			return holes == null;
		}

		public Point2d centroid() {
			Point2d center = PolygonUtil.centroid_of_polygon(polygon);
			if(!center.isFinite()) {
				return Point2d.NaN;
			} else if(hasNoHoles()) {
				return center;
			} else {
				double cnt = PolygonUtil.area_of_polygon(polygon);
				double xsum = center.x * cnt;
				double ysum = center.y * cnt;				
				int holesPointsLen = holes.length;
				for (int ringIndex = 0; ringIndex < holesPointsLen; ringIndex++) {
					Point2d holeCenter = PolygonUtil.centroid_of_polygon(holes[ringIndex]);
					if(!holeCenter.isFinite()) {
						return Point2d.NaN;
					}
					double hole_area = PolygonUtil.area_of_polygon(polygon);
					cnt -= hole_area;
					xsum -= holeCenter.x * hole_area;
					ysum -= holeCenter.y * hole_area;				
				}
				return cnt > 0 ? new Point2d(xsum / cnt, ysum / cnt) : Point2d.NaN;
			}
		}

		public double area() {
			double a = PolygonUtil.area_of_polygon(polygon);
			if(hasHoles()) {
				for(Point2d[] hole : holes) {
					a -= PolygonUtil.area_of_polygon(hole);
				}
			}
			return a;
		}
		
		public int pointCount() {
			int cnt = polygon.length;
			if(hasHoles()) {
				for(Point2d[] hole : holes) {
					cnt += hole.length;
				}
			}
			return cnt;
		}

		@Override
		public String toString() {
			return "PolygonWithHoles [polygon=" + Arrays.toString(polygon) + ", holes=" + Arrays.toString(holes) + "]";
		}		
	}

	public static class PolygonsWithHoles {
		public static Point2d[] polygonsWithHolesToPolygonThrow(PolygonWithHoles[] polygonsWithHoles) {
			if(polygonsWithHoles.length != 1 || polygonsWithHoles[0].hasHoles()) {
				throw new RuntimeException("multiple polygons or with holes");
			}
			return polygonsWithHoles[0].polygon;		
		}

		public static Point2d[] polygonswithHolesToPolygonNoCheck(PolygonWithHoles[] polygonsWithHoles) {
			return polygonsWithHoles[0].polygon;	
		}

		public static boolean isPlainPolygon(PolygonWithHoles[] polygonsWithHoles) {
			return polygonsWithHoles.length == 1 && polygonsWithHoles[0].hasNoHoles();
		}

		public static Point2d meanOfPolygonsWithHolesPoints(PolygonWithHoles[] polygonsWithHoles) {
			double xsum = 0;
			double ysum = 0;
			int cnt = 0;
			for(PolygonWithHoles polygonWithHoles: polygonsWithHoles) {
				cnt += polygonWithHoles.polygon.length;
				for(Point2d p : polygonWithHoles.polygon) {
					xsum += p.x;
					ysum += p.y;
				}
			}	
			return new Point2d(xsum / cnt, ysum / cnt);
		}

		public static Point2d centroidOfPolygonsWithHolesPoints(PolygonWithHoles[] polygonsWithHoles) {
			double xsum = 0;
			double ysum = 0;
			double cnt = 0;
			for(PolygonWithHoles polygonWithHoles: polygonsWithHoles) {				
				Point2d center = polygonWithHoles.centroid();				
				if(!center.isFinite()) {
					Point2d[] ps = Point2d.toUnique(polygonWithHoles.polygon);
					if(ps.length != polygonWithHoles.polygon.length) {
						center = PolygonUtil.centroid_of_polygon(ps);
					}
				}
				if(!center.isFinite()) {
					center = PolygonUtil.meanOfPolygonPoints(polygonWithHoles.polygon);
				}
				double area = PolygonUtil.area_of_polygon(polygonWithHoles.polygon);
				xsum += center.x * area;
				ysum += center.y * area;
				cnt += area;
			}
			return cnt > 0 ? new Point2d(xsum / cnt, ysum / cnt) : meanOfPolygonsWithHolesPoints(polygonsWithHoles);
		}

		public static void validatePolygonsWithHoles(PolygonWithHoles[] polygons) {
			for(PolygonWithHoles polygon : polygons) {
				polygon.validate();
			}
		}

		public static Rect toRectBBOX(PolygonWithHoles[] polygonsWithHoles) {
			double minX = Double.MAX_VALUE;
			double minY = Double.MAX_VALUE;
			double maxX = -Double.MAX_VALUE;
			double maxY = -Double.MAX_VALUE;

			for(PolygonWithHoles pwh : polygonsWithHoles) {
				for(Point2d p : pwh.polygon) {
					if(p.x<minX) minX = p.x;
					if(p.y<minY) minY = p.y;
					if(p.x>maxX) maxX = p.x;
					if(p.y>maxY) maxY = p.y;
				}
				if(pwh.hasHoles()) {
					for(Point2d[] hole : pwh.holes) {
						for(Point2d p : hole) {
							if(p.x<minX) minX = p.x;
							if(p.y<minY) minY = p.y;
							if(p.x>maxX) maxX = p.x;
							if(p.y>maxY) maxY = p.y;
						}
					}
				}
			}			

			return Rect.of_UTM(minX,minY,maxX,maxY);
		}

		public static double area(PolygonWithHoles[] polygonsWithHoles) {
			double a = 0d;
			for(PolygonWithHoles pwh : polygonsWithHoles) {
				a += pwh.area();
			}	
			return a;
		}

		public static int pointCount(PolygonWithHoles[] polygonsWithHoles) {
		int cnt = 0;
			for(PolygonWithHoles pwh : polygonsWithHoles) {
				cnt += pwh.pointCount();
			}	
			return cnt;
		}
	}
}
