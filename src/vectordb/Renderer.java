package vectordb;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Feature;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.Layer;

import pointdb.base.Point2d;
import util.Extent2d;
import util.collections.vec.Vec;
import util.image.ImageBufferARGB;

public class Renderer {
	private static final Logger log = LogManager.getLogger();

	private static Color COLOR_POLYGON = new Color(0, 255, 0, 100);
	private static Color COLOR_LINE = new Color(0, 0, 255, 100);
	private static Color COLOR_POINT = new Color(255, 0, 0, 100);
	//private static Color COLOR_POINT_TOP = new Color(0, 0, 0, 100);

	public static ImageBufferARGB renderProportionalFullMaxSize(DataSource datasource, int maxWidth, int maxHeight) {		
		Extent2d extent = VectorDB.getExtent(VectorDB.getPoints(datasource));		
		return renderProportionalMaxSize(datasource, extent, maxWidth, maxHeight);
	}
	
	public static ImageBufferARGB renderProportionalMaxSize(DataSource datasource, Extent2d extent, int maxWidth, int maxHeight) {		
		double xlen = extent.getWidth();
		double ylen = extent.getHeight();

		double xTargetScale = maxWidth / xlen;
		double yTargetScale = maxHeight / ylen;

		double targetScale = Math.min(xTargetScale, yTargetScale);
		
		int width = (int) Math.ceil(targetScale * xlen);
		int height = (int) Math.ceil(targetScale * ylen);
		
		log.info(maxWidth + " x " + maxHeight + " -> " + width + " x " + height);
		
		return render(datasource, extent, width, height);
	}
	
	public static ImageBufferARGB render(DataSource datasource, Extent2d extent, int width, int height) {
		ImageBufferARGB image = new ImageBufferARGB(width, height);
		Graphics2D gc = image.bufferedImage.createGraphics();
		gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gc.setColor(Color.DARK_GRAY);
		//gc.drawLine(0, 0, 99, 99);		

		double xlen = extent.getWidth();
		double ylen = extent.getHeight();

		double xscale = (width - 4) / xlen;
		double yscale = (height - 4) / ylen;
		
		double xoff = - extent.xmin + 2 * (1 / xscale);
		double yoff = extent.ymax + 2 * (1 / yscale);
		log.info("xscale " + xscale);
		log.info("yscale " + yscale);
		log.info("1/xscale " + (1 / xscale));
		log.info("1/yscale " + (1 / yscale));
		Drawer drawer = new Drawer(gc, xoff, yoff, xscale, yscale);

		/*traversePolygons(datasource, drawer);
		traverseLines(datasource, drawer);
		traversePointsCross(datasource, drawer);
		traversePointsTop(datasource, drawer);*/

		Vec<Point2d> points = new Vec<Point2d>();
		Vec<Object[]> lines = new Vec<Object[]>();
		Vec<Object[]> polygons = new Vec<Object[]>();
		collectDataSource(datasource, points, lines, polygons);
		gc.setColor(COLOR_POLYGON);
		for(Object[] polygon:polygons) {
			drawer.drawPolygon(polygon);
		}
		gc.setColor(COLOR_LINE);
		for(Object[] line:lines) {
			drawer.drawPolyline(line);
		}
		gc.setColor(COLOR_POINT);
		for(Point2d p:points) {
			drawer.drawPointCross(p.x, p.y);
		}
		/*gc.setColor(COLOR_POINT_TOP);
		for(Point2d p:points) {
			drawer.drawPointTop(p.x, p.y);
		}*/

		gc.dispose();

		return image;
	}

	static class Drawer {
		private final Graphics2D gc;
		private final double xoff;
		private final double yoff;
		private final double xscale;
		private final double yscale;

		public Drawer(Graphics2D gc, double xoff, double yoff, double xscale, double yscale) {
			this.gc = gc;
			this.xoff = xoff;
			this.yoff = yoff;
			this.xscale = xscale;
			this.yscale = yscale;
		}

		public void drawPolyline(Object[] line) {
			int len = line.length;
			int[] xs = new int[len];
			int[] ys = new int[len];
			for (int i = 0; i < len; i++) {
				double[] p = (double[]) line[i];
				xs[i] = (int) ((p[0] + xoff) * xscale);
				ys[i] = (int) ((yoff - p[1]) * yscale);
			}
			gc.drawPolyline(xs, ys, len);
		}

		public void drawPointCross(double x, double y) {
			int x1 = (int) ((x + xoff) * xscale);
			int y1 = (int) ((yoff - y) * yscale);
			//log.info(x1 + " " + y1);
			/*gc.setColor(Color.gray);
			gc.drawLine(x1, y1-1, x1, y1+1);
			gc.drawLine(x1-1, y1, x1+1, y1);*/
			/*gc.setColor(Color.DARK_GRAY);
			gc.drawLine(x1-1, y1-1, x1+1, y1+1);
			gc.drawLine(x1-1, y1+1, x1+1, y1-1);*/
			gc.drawLine(x1-1, y1-1, x1+1, y1-1);
			gc.drawLine(x1-1, y1, x1+1, y1);
			gc.drawLine(x1-1, y1+1, x1+1, y1+1);
		}

		public void drawPointTop(double x, double y) {
			int x1 = (int) ((x + xoff) * xscale);
			int y1 = (int) ((yoff - y) * yscale);
			gc.drawLine(x1, y1, x1, y1);			
		}

		public void drawLine(double x, double y, double xa, double ya) {
			int x1 = (int) ((x + xoff) * xscale);
			int y1 = (int) ((yoff - y) * yscale);
			int x2 = (int) ((xa + xoff) * xscale);
			int y2 = (int) ((yoff - ya) * yscale);
			log.info("draw line "+x1+" "+y1+"  "+x2+" "+y2+"   " + x +" " + y + "  " + xa + " " + ya);
			gc.setColor(Color.DARK_GRAY);
			gc.drawLine(x1, y1, x2, y2);			
		}

		public void drawPolygon(double[][] points) {
			int len = points.length;
			int[] xs = new int[len];
			int[] ys = new int[len];
			for (int i = 0; i < len; i++) {
				double[] p = points[i];
				xs[i] = (int) ((p[0] + xoff) * xscale);
				ys[i] = (int) ((yoff - p[1]) * yscale);
			}
			gc.setColor(Color.GREEN);
			gc.fillPolygon(xs, ys, len);
		}

		public void drawPolygon(Object[] points) {
			int len = points.length;
			int[] xs = new int[len];
			int[] ys = new int[len];
			for (int i = 0; i < len; i++) {
				double[] p = (double[]) points[i];
				xs[i] = (int) ((p[0] + xoff) * xscale);
				ys[i] = (int) ((yoff - p[1]) * yscale);
				//log.info("polygon point " + xs[i] + " " + ys[i]);
			}
			gc.fillPolygon(xs, ys, len);
		}
	}

	public static void traversePointsCross(DataSource datasource, Drawer drawer) {
		int layerCount = datasource.GetLayerCount();
		for(int layerIndex=0; layerIndex<layerCount; layerIndex++) {
			Layer layer = datasource.GetLayerByIndex(layerIndex);
			layer.ResetReading();
			Feature feature = layer.GetNextFeature();
			while(feature != null) {
				Geometry geometry = feature.GetGeometryRef();
				drawGeometryPointsCross(geometry, drawer);
				feature = layer.GetNextFeature();
			}
		}
	}

	public static void traversePointsTop(DataSource datasource, Drawer drawer) {
		int layerCount = datasource.GetLayerCount();
		for(int layerIndex=0; layerIndex<layerCount; layerIndex++) {
			Layer layer = datasource.GetLayerByIndex(layerIndex);
			layer.ResetReading();
			Feature feature = layer.GetNextFeature();
			while(feature != null) {
				Geometry geometry = feature.GetGeometryRef();
				drawGeometryPointsTop(geometry, drawer);
				feature = layer.GetNextFeature();
			}
		}
	}

	public static void traverseLines(DataSource datasource, Drawer drawer) {
		int layerCount = datasource.GetLayerCount();
		for(int layerIndex=0; layerIndex<layerCount; layerIndex++) {
			Layer layer = datasource.GetLayerByIndex(layerIndex);
			layer.ResetReading();
			Feature feature = layer.GetNextFeature();
			while(feature != null) {
				Geometry geometry = feature.GetGeometryRef();
				drawGeometryLines(geometry, drawer);
				feature = layer.GetNextFeature();
			}
		}
	}

	public static void traversePolygons(DataSource datasource, Drawer drawer) {
		int layerCount = datasource.GetLayerCount();
		for(int layerIndex=0; layerIndex<layerCount; layerIndex++) {
			Layer layer = datasource.GetLayerByIndex(layerIndex);
			log.info("layer.GetGeomType() " + layer.GetGeomType());
			layer.ResetReading();
			Feature feature = layer.GetNextFeature();
			while(feature != null) {
				Geometry geometry = feature.GetGeometryRef();
				drawGeometryPolygons(geometry, drawer);
				feature = layer.GetNextFeature();
			}
		}
	}

	public static void drawGeometryPointsCross(Geometry geometry, Drawer drawer) {
		int type = geometry.GetGeometryType();
		switch (type) {
		case 1: {
			double x = geometry.GetX();
			double y = geometry.GetY();
			log.info("point " + x + " " + y);
			drawer.drawPointCross(x, y);
			break;
		}
		default:
			//log.warn("unknown geometry " + type);
		}
	}

	public static void drawGeometryPointsTop(Geometry geometry, Drawer drawer) {
		int type = geometry.GetGeometryType();
		switch (type) {
		case 1: { // POINT
			double x = geometry.GetX();
			double y = geometry.GetY();
			log.info("point " + x + " " + y);
			drawer.drawPointTop(x, y);
			break;
		}
		default: 
			//log.warn("unknown geometry " + type + "  " + geometry.GetGeometryName());
		}
	}

	public static void drawGeometryLines(Geometry geometry, Drawer drawer) {
		int type = geometry.GetGeometryType();
		switch (type) {
		case -2147483643: { // MULTILINESTRING 
			/*double x = geometry.GetX();
			double y = geometry.GetY();
			log.info("point " + x + " " + y);
			drawer.drawPointTop(x, y);*/
			int geoCount = geometry.GetGeometryCount();
			for(int i=0; i<geoCount; i++) {
				Geometry subGeo = geometry.GetGeometryRef(i);
				drawGeometryLines(subGeo, drawer);
			}
			break;
		}
		case 2:
		case -2147483646: { // LINESTRING 
			/*double x = geometry.GetX();
			double y = geometry.GetY();
			log.info("point " + x + " " + y);
			drawer.drawPointTop(x, y);*/

			double[][] points = geometry.GetPoints();
			log.info(points.length);
			double[] st = null;
			for(double[] p:points) {
				if(st != null) {
					drawer.drawLine(st[0], st[1], p[0], p[1]);
				}
				st = p;
			}
			break;
		}
		default: 
			//log.warn("unknown geometry " + type + "  " + geometry.GetGeometryName());
		}
	}

	public static void drawGeometryPolygons(Geometry geometry, Drawer drawer) {
		int type = geometry.GetGeometryType();
		switch (type) {
		case 6: { // MULTIPOLYGON
			/*double x = geometry.GetX();
			double y = geometry.GetY();
			log.info("point " + x + " " + y);
			drawer.drawPointTop(x, y);*/
			int geoCount = geometry.GetGeometryCount();
			for(int i=0; i<geoCount; i++) {
				Geometry subGeo = geometry.GetGeometryRef(i);
				drawGeometryPolygons(subGeo, drawer);
			}
			break;
		}

		case 3:
		case -2147483645: { // POLYGON
			/*double x = geometry.GetX();
			double y = geometry.GetY();
			log.info("point " + x + " " + y);
			drawer.drawPointTop(x, y);*/
			int geoCount = geometry.GetGeometryCount();
			for(int i=0; i<geoCount; i++) {
				Geometry subGeo = geometry.GetGeometryRef(i);
				drawGeometryPolygons(subGeo, drawer);
			}
			break;
		}

		//case 2:
		case -2147483646: { // LINEARRING
			/*double x = geometry.GetX();
			double y = geometry.GetY();
			log.info("point " + x + " " + y);
			drawer.drawPointTop(x, y);*/

			double[][] points = geometry.GetPoints();
			drawer.drawPolygon(points);
			/*log.info(points.length);
			double[] st = null;
			for(double[] p:points) {
				if(st != null) {
					drawer.drawLine(st[0], st[1], p[0], p[1]);
				}
				st = p;
			}*/
			break;
		}

		default: 
			log.warn("unknown geometry " + type + "  " + geometry.GetGeometryName());
		}
	}

	/*
		  wkbUnknown = 0, wkbPoint = 1, wkbLineString = 2, wkbPolygon = 3,
		  wkbMultiPoint = 4, wkbMultiLineString = 5, wkbMultiPolygon = 6, wkbGeometryCollection = 7,
		  wkbCircularString = 8, wkbCompoundCurve = 9, wkbCurvePolygon = 10, wkbMultiCurve = 11,
		  wkbMultiSurface = 12, wkbCurve = 13, wkbSurface = 14, wkbPolyhedralSurface = 15,
		  wkbTIN = 16, wkbTriangle = 17, wkbNone = 100, wkbLinearRing = 101,
		  wkbCircularStringZ = 1008, wkbCompoundCurveZ = 1009, wkbCurvePolygonZ = 1010, wkbMultiCurveZ = 1011,
		  wkbMultiSurfaceZ = 1012, wkbCurveZ = 1013, wkbSurfaceZ = 1014, wkbPolyhedralSurfaceZ = 1015,
		  wkbTINZ = 1016, wkbTriangleZ = 1017, wkbPointM = 2001, wkbLineStringM = 2002,
		  wkbPolygonM = 2003, wkbMultiPointM = 2004, wkbMultiLineStringM = 2005, wkbMultiPolygonM = 2006,
		  wkbGeometryCollectionM = 2007, wkbCircularStringM = 2008, wkbCompoundCurveM = 2009, wkbCurvePolygonM = 2010,
		  wkbMultiCurveM = 2011, wkbMultiSurfaceM = 2012, wkbCurveM = 2013, wkbSurfaceM = 2014,
		  wkbPolyhedralSurfaceM = 2015, wkbTINM = 2016, wkbTriangleM = 2017, wkbPointZM = 3001,
		  wkbLineStringZM = 3002, wkbPolygonZM = 3003, wkbMultiPointZM = 3004, wkbMultiLineStringZM = 3005,
		  wkbMultiPolygonZM = 3006, wkbGeometryCollectionZM = 3007, wkbCircularStringZM = 3008, wkbCompoundCurveZM = 3009,
		  wkbCurvePolygonZM = 3010, wkbMultiCurveZM = 3011, wkbMultiSurfaceZM = 3012, wkbCurveZM = 3013,
		  wkbSurfaceZM = 3014, wkbPolyhedralSurfaceZM = 3015, wkbTINZM = 3016, wkbTriangleZM = 3017,
		  wkbPoint25D = 0x80000001, wkbLineString25D = 0x80000002, wkbPolygon25D = 0x80000003, wkbMultiPoint25D = 0x80000004,
		  wkbMultiLineString25D = 0x80000005, wkbMultiPolygon25D = 0x80000006, wkbGeometryCollection25D = 0x80000007
	 */

	/*
	 * -2147483647 Point25D
-2147483646 LineString25D
-2147483645 Polygon25D
-2147483644 MultiPoint25D
-2147483643 MultiLineString25D
-2147483642 MultiPolygon25D
          0 Geometry
          1 Point
          2 Line
          3 Polygon
          4 MultiPoint
          5 MultiLineString
          6 MultiPolygon
        100 No Geometry
	 */

	public static void collectDataSource(DataSource datasource, Vec<Point2d> points, Vec<Object[]> lines, Vec<Object[]> polygons) {
		int layerCount = datasource.GetLayerCount();
		for(int layerIndex=0; layerIndex<layerCount; layerIndex++) {
			Layer layer = datasource.GetLayerByIndex(layerIndex);
			collectLayer(layer, points, lines, polygons);
		}
	}

	public static void collectLayer(Layer layer, Vec<Point2d> points, Vec<Object[]> lines, Vec<Object[]> polygons) {
		layer.ResetReading();
		Feature feature = layer.GetNextFeature();
		while(feature != null) {
			Geometry geometry = feature.GetGeometryRef();
			if(geometry != null) {
				collectGeometry(geometry, points, lines, polygons);
			} else {
				log.warn("missing geometry");
			}
			feature = layer.GetNextFeature();
		}		
	}

	public static void collectGeometry(Geometry geometry, Vec<Point2d> points, Vec<Object[]> lines, Vec<Object[]> polygons) {
		int type = geometry.GetGeometryType();
		switch (type) {
		case 1:  // 1  POINT
		case -2147483647: { //  0x80000001 Point25D
			double x = geometry.GetX();
			double y = geometry.GetY();
			points.add(new Point2d(x, y));
			break;
		}
		case 2: // 2  LINESTRING
		case -2147483646: { // 0x80000002  LineString25D
			Object[] linePoints = geometry.GetPoints();
			lines.add(linePoints);
			break;
		}
		case 3: // 3  POLYGON 
		case -2147483645: { // 0x80000003  Polygon25D
			//log.info("collect polygon");
			int geoCount = geometry.GetGeometryCount();
			for(int i=0; i<geoCount; i++) {
				Geometry subGeo = geometry.GetGeometryRef(i);
				int subType = subGeo.GetGeometryType();
				switch (subType) {
				case 2: // 2  LINESTRING
				case -2147483646: { // 0x80000002  LineString25D
					Object[] polygonPoints = subGeo.GetPoints();
					polygons.add(polygonPoints);
					break;
				}
				default: 
					log.warn("unknown POLYGON sub geometry " + subType + "  "+ Long.toHexString(Integer.toUnsignedLong(subType)) + "  " + subGeo.GetGeometryName());
				}
			}
			break;
		}
		case 4: // 4  MultiPoint
		case -2147483644: { // 0x80000004  MultiPoint25D
			Object[] ps = geometry.GetPoints();
			for(Object p:ps) {
				double[] pp = (double[]) p;
				points.add(new Point2d(pp[0], pp[1]));
			}
			break;
		}
		case 5: // 5  MultiLineString
		case -2147483643: { //  0x80000005  MultiLineString25D
			int geoCount = geometry.GetGeometryCount();
			for(int i=0; i<geoCount; i++) {
				Geometry subGeo = geometry.GetGeometryRef(i);
				int subType = subGeo.GetGeometryType();
				switch (subType) {
				case 2: // 2  LINESTRING
				case -2147483646: { // 0x80000002  LineString25D
					Object[] linePoints = subGeo.GetPoints();
					lines.add(linePoints);
					break;
				}
				default: 
					log.warn("unknown MULTILINESTRING sub geometry " + subType + "  "+ Long.toHexString(Integer.toUnsignedLong(subType)) + "  " + subGeo.GetGeometryName());
				}
			}
			break;
		}
		case 6: // 6  MULTIPOLYGON 
		case -2147483642: { // 0x80000006  MultiPolygon25D
			int geoCount = geometry.GetGeometryCount();
			for(int i=0; i<geoCount; i++) {
				Geometry subGeo = geometry.GetGeometryRef(i);
				int subType = subGeo.GetGeometryType();
				switch (subType) {
				case 3: // 3  POLYGON 
				case -2147483645: { // 0x80000003  Polygon25D
					int subGeoCount = subGeo.GetGeometryCount();
					for(int j=0; j<subGeoCount; j++) {
						Geometry subsubGeo = subGeo.GetGeometryRef(j);
						int subsubType = subsubGeo.GetGeometryType();
						switch (subsubType) {
						case 2: // 2  LINESTRING
						case -2147483646: { // 0x80000002  LineString25D
							Object[] polygonPoints = subsubGeo.GetPoints();
							polygons.add(polygonPoints);
							break;
						}
						default: 
							log.warn("unknown MULTIPOLYGON POLYGON sub geometry " + subsubType + "  "+ Long.toHexString(Integer.toUnsignedLong(subsubType)) + "  " + subsubGeo.GetGeometryName());
						}
					}
					break;
				}
				default: 
					log.warn("unknown MULTIPOLYGON sub geometry " + subType + "  "+ Long.toHexString(Integer.toUnsignedLong(subType)) + "  " + subGeo.GetGeometryName());
				}
			}
			break;
		}
		default: 
			log.warn("unknown geometry " + type + "  "+ Long.toHexString(Integer.toUnsignedLong(type)) + "  " + geometry.GetGeometryName());
		}		
	}
}
