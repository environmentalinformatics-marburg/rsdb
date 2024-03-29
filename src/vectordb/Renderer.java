package vectordb;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.gdal.ogr.DataSource;
import org.gdal.ogr.Feature;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.Layer;
import org.tinylog.Logger;

import pointcloud.Rect2d;
import pointdb.base.Point2d;
import util.collections.vec.Vec;
import util.image.ImageBufferARGB;
import vectordb.style.BasicStyle;
import vectordb.style.GeoLabel;
import vectordb.style.Style;

public class Renderer {

	public static Color COLOR_POLYGON = new Color(0, 255, 0, 100);
	public static Color COLOR_POLYGON_OUTLINE = new Color(128, 128, 128, 100);
	static Color COLOR_LINE = new Color(0, 0, 255, 100);
	public static Color COLOR_POINT = new Color(255, 0, 0, 255);
	//private static Color COLOR_POINT_TOP = new Color(0, 0, 0, 100);

	public static final BasicStyle STYLE_DEFAULT = new BasicStyle();

	public static String colorToString(Color c) {
		int v = (c.getRed() << 24) | (c.getGreen() << 16) | (c.getBlue() << 8) | (c.getAlpha() << 0);		
		String hex = '#' + Integer.toHexString(v).toUpperCase();
		//Logger.info(Integer.toHexString(c.getRed()).toUpperCase() + " " + Integer.toHexString(c.getGreen()).toUpperCase() + " " + Integer.toHexString(c.getBlue()).toUpperCase() + " " + Integer.toHexString(c.getAlpha()).toUpperCase() + " -> " + hex);
		return hex;
	}

	private static final Color COLOR_ERROR = new Color(255, 255, 0, 255);

	public static Color stringToColor(String hex) {
		if(hex.startsWith("#")) {
			try {
				int v = Integer.parseUnsignedInt(hex.substring(1), 16);
				int r = (v >> 24) & 0xFF;
				int g = (v >> 16) & 0xFF;
				int b = (v >> 8) & 0xFF;
				int a = (v >> 0) & 0xFF;
				Color c = new Color(r, g, b, a);
				//Logger.info(hex + " -> " + Integer.toHexString(c.getRed()).toUpperCase() + " " + Integer.toHexString(c.getGreen()).toUpperCase() + " " + Integer.toHexString(c.getBlue()).toUpperCase() + " " + Integer.toHexString(c.getAlpha()).toUpperCase());
				return c;
			} catch(Exception e) {
				return COLOR_ERROR;
			}
		} 
		return COLOR_ERROR;

	}	

	public static ImageBufferARGB renderProportionalFullMaxSize(DataSource datasource, Object sync, int maxWidth, int maxHeight, util.GeoUtil.Transformer transformer, Style style) {		
		Rect2d extent = VectorDB.getExtent(VectorDB.getPoints(datasource, sync));		
		return renderProportionalMaxSize(datasource, sync, extent, maxWidth, maxHeight, transformer, style);
	}

	public static ImageBufferARGB renderProportionalMaxSize(DataSource datasource, Object sync, Rect2d rect, int maxWidth, int maxHeight, util.GeoUtil.Transformer transformer, Style style) {		
		double xlen = rect.width();
		double ylen = rect.height();

		double xTargetScale = maxWidth / xlen;
		double yTargetScale = maxHeight / ylen;

		double targetScale = Math.min(xTargetScale, yTargetScale);

		int width = (int) Math.ceil(targetScale * xlen);
		int height = (int) Math.ceil(targetScale * ylen);

		Logger.info(maxWidth + " x " + maxHeight + " -> " + width + " x " + height);

		return render(datasource, sync, rect, width, height, transformer, style, null);
	}

	/**
	 * 
	 * @param datasource
	 * @param sync
	 * @param renderRect
	 * @param width
	 * @param height
	 * @param transformer
	 * @param style
	 * @param labelField nullable
	 * @return
	 */
	public static ImageBufferARGB render(DataSource datasource, Object sync, Rect2d renderRect, int width, int height, util.GeoUtil.Transformer transformer, Style style, String labelField) {
		if(style == null) {
			style = STYLE_DEFAULT;
		}

		ImageBufferARGB image = new ImageBufferARGB(width, height);
		Graphics2D gc = image.bufferedImage.createGraphics();
		gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gc.setColor(Color.DARK_GRAY);
		//gc.drawLine(0, 0, 99, 99);		

		double xlen = renderRect.width();
		double ylen = renderRect.height();

		double xscale = (width - 4) / xlen;
		double yscale = (height - 4) / ylen;

		double xoff = - renderRect.xmin + 2 * (1 / xscale);
		double yoff = renderRect.ymax + 2 * (1 / yscale);
		//Logger.info("xscale " + xscale);
		//Logger.info("yscale " + yscale);
		//Logger.info("1/xscale " + (1 / xscale));
		//Logger.info("1/yscale " + (1 / yscale));
		Drawer drawer = new Drawer(gc, xoff, yoff, xscale, yscale);

		/*traversePolygons(datasource, drawer);
		traverseLines(datasource, drawer);
		traversePointsCross(datasource, drawer);
		traversePointsTop(datasource, drawer);*/

		Vec<Point2d> points = new Vec<Point2d>();
		Vec<Object[]> lines = new Vec<Object[]>();
		Vec<Object[]> polygons = new Vec<Object[]>();
		Vec<GeoLabel> labels  = new Vec<GeoLabel>();
		collectDataSource(datasource, sync, points, lines, polygons, labels, transformer, labelField);
		style.drawGeoPolygons(gc, drawer, polygons);
		style.drawGeoLines(gc, drawer, lines);				
		style.drawGeoPoints(gc, drawer, points);
		style.drawGeoLabels(gc, drawer, labels);

		/*gc.setColor(COLOR_POINT_TOP);
		for(Point2d p:points) {
			drawer.drawPointTop(p.x, p.y);
		}*/

		gc.dispose();

		return image;
	}

	public static class Drawer {
		public final Graphics2D gc;
		public final double xoff;
		public final double yoff;
		public final double xscale;
		public final double yscale;

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

		public void drawPointBox(double x, double y) {
			int x1 = (int) ((x + xoff) * xscale);
			int y1 = (int) ((yoff - y) * yscale);
			//Logger.info(x1 + " " + y1);
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

		public void drawPointCross(double x, double y) {
			int x1 = (int) ((x + xoff) * xscale);
			int y1 = (int) ((yoff - y) * yscale);
			gc.drawLine(x1-7, y1-7, x1+7, y1+7);
			gc.drawLine(x1-7, y1+7, x1+7, y1-7);
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
			Logger.info("draw line "+x1+" "+y1+"  "+x2+" "+y2+"   " + x +" " + y + "  " + xa + " " + ya);
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

		@FunctionalInterface
		public
		interface PolygonDrawer {
			void drawImgPolygon(Graphics2D gc, int[] xs, int[] ys, int len);
		}

		public void drawPolygon(Object[] points, PolygonDrawer polygonDrawer) {
			int len = points.length;
			int[] xs = new int[len];
			int[] ys = new int[len];
			for (int i = 0; i < len; i++) {
				double[] p = (double[]) points[i];
				xs[i] = (int) ((p[0] + xoff) * xscale);
				ys[i] = (int) ((yoff - p[1]) * yscale);
				//Logger.info("polygon point " + xs[i] + " " + ys[i]);
			}
			polygonDrawer.drawImgPolygon(gc, xs, ys, len);			
		}
	}

	/*public static void traversePointsCross(DataSource datasource, Drawer drawer) {
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
			Logger.info("layer.GetGeomType() " + layer.GetGeomType());
			layer.ResetReading();
			Feature feature = layer.GetNextFeature();
			while(feature != null) {
				Geometry geometry = feature.GetGeometryRef();
				drawGeometryPolygons(geometry, drawer);
				feature = layer.GetNextFeature();
			}
		}
	}*/

	public static void drawGeometryPointsCross(Geometry geometry, Drawer drawer) {
		int type = geometry.GetGeometryType();
		switch (type) {
		case 1: {
			double x = geometry.GetX();
			double y = geometry.GetY();
			Logger.info("point " + x + " " + y);
			drawer.drawPointCross(x, y);
			break;
		}
		default:
			//Logger.warn("unknown geometry " + type);
		}
	}

	public static void drawGeometryPointsTop(Geometry geometry, Drawer drawer) {
		int type = geometry.GetGeometryType();
		switch (type) {
		case 1: { // POINT
			double x = geometry.GetX();
			double y = geometry.GetY();
			Logger.info("point " + x + " " + y);
			drawer.drawPointTop(x, y);
			break;
		}
		default: 
			//Logger.warn("unknown geometry " + type + "  " + geometry.GetGeometryName());
		}
	}

	public static void drawGeometryLines(Geometry geometry, Drawer drawer) {
		int type = geometry.GetGeometryType();
		switch (type) {
		case -2147483643: { // MULTILINESTRING 
			/*double x = geometry.GetX();
			double y = geometry.GetY();
			Logger.info("point " + x + " " + y);
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
			Logger.info("point " + x + " " + y);
			drawer.drawPointTop(x, y);*/

			double[][] points = geometry.GetPoints();
			Logger.info(points.length);
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
			//Logger.warn("unknown geometry " + type + "  " + geometry.GetGeometryName());
		}
	}

	public static void drawGeometryPolygons(Geometry geometry, Drawer drawer) {
		int type = geometry.GetGeometryType();
		switch (type) {
		case 6: { // MULTIPOLYGON
			/*double x = geometry.GetX();
			double y = geometry.GetY();
			Logger.info("point " + x + " " + y);
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
			Logger.info("point " + x + " " + y);
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
			Logger.info("point " + x + " " + y);
			drawer.drawPointTop(x, y);*/

			double[][] points = geometry.GetPoints();
			drawer.drawPolygon(points);
			/*Logger.info(points.length);
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
			Logger.warn("unknown geometry " + type + "  " + geometry.GetGeometryName());
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

	private static void collectDataSource(DataSource datasource, Object sync, Vec<Point2d> points, Vec<Object[]> lines, Vec<Object[]> polygons, Vec<GeoLabel> labels, util.GeoUtil.Transformer transformer, String labelField) {
		int layerCount = datasource.GetLayerCount();
		for(int layerIndex=0; layerIndex<layerCount; layerIndex++) {
			Layer layer = datasource.GetLayerByIndex(layerIndex);
			collectLayer(layer, sync, points, lines, polygons, labels, transformer, labelField);
		}
	}

	private static void collectLayer(Layer layer, Object sync, Vec<Point2d> points, Vec<Object[]> lines, Vec<Object[]> polygons, Vec<GeoLabel> labels, util.GeoUtil.Transformer transformer, String labelField) { // layer iterator and layer filter possibly not parallel
		synchronized (sync) {
			layer.SetSpatialFilter(null); // clear filter
			layer.ResetReading(); // reset iterator
			Feature feature = layer.GetNextFeature();
			while(feature != null) {
				Geometry geometry = feature.GetGeometryRef();
				if(geometry != null) {
					collectGeometry(geometry, points, lines, polygons, transformer);
					if(labelField != null) {
						String label = feature.GetFieldAsString(labelField);
						if(label != null && !label.isBlank()) {
							Geometry clabel = geometry.Centroid();
							if(clabel != null) {
								double xlabel = clabel.GetX();
								double ylabel = clabel.GetY();
								if(Double.isFinite(xlabel) && Double.isFinite(ylabel)) {
									if(transformer != null) {
										double[] pLabel = transformer.transformWithAxisOrderCorrection(xlabel, ylabel);
										xlabel = pLabel[0];
										ylabel = pLabel[1];
									}									
									if(Double.isFinite(xlabel) && Double.isFinite(ylabel)) {
										GeoLabel geoLabel = new GeoLabel(label, xlabel, ylabel);
										labels.add(geoLabel);
									}
								}
							}
						}
					}
				} else {
					Logger.warn("missing geometry");
				}
				feature = layer.GetNextFeature();
			}
		}
	}

	private static void collectGeometry(Geometry geometry, Vec<Point2d> points, Vec<Object[]> lines, Vec<Object[]> polygons, util.GeoUtil.Transformer transformer) {
		int type = geometry.GetGeometryType();
		switch (type) {
		case 1:  // 1  POINT
		case -2147483647: { //  0x80000001 Point25D
			//Logger.info("read point");
			double x = geometry.GetX();
			double y = geometry.GetY();
			if(transformer != null) {
				double[] p = transformer.transformWithAxisOrderCorrection(x, y);
				x = p[0];
				y = p[1];
			}
			points.add(new Point2d(x, y));
			break;
		}
		case 2: // 2  LINESTRING
		case -2147483646: { // 0x80000002  LineString25D
			//Logger.info("read line");
			double[][] linePoints = geometry.GetPoints();
			if(transformer != null) {
				transformer.transformWithAxisOrderCorrection(linePoints);
			}
			lines.add(linePoints);
			break;
		}
		case 3: // 3  POLYGON 
		case -2147483645: { // 0x80000003  Polygon25D			
			int geoCount = geometry.GetGeometryCount();
			for(int i=0; i<geoCount; i++) {
				Geometry subGeo = geometry.GetGeometryRef(i);
				int subType = subGeo.GetGeometryType();
				switch (subType) {
				case 2: // 2  LINESTRING
				case -2147483646: { // 0x80000002  LineString25D
					//Logger.info("read polygon");
					double[][] polygonPoints = subGeo.GetPoints();
					if(transformer != null) {
						transformer.transformWithAxisOrderCorrection(polygonPoints);
					}
					polygons.add(polygonPoints);
					break;
				}
				default: 
					Logger.warn("unknown POLYGON sub geometry " + subType + "  "+ Long.toHexString(Integer.toUnsignedLong(subType)) + "  " + subGeo.GetGeometryName());
				}
			}
			break;
		}
		case 4: // 4  MultiPoint
		case -2147483644: { // 0x80000004  MultiPoint25D
			//Logger.info("read points");
			double[][] ps = geometry.GetPoints();
			if(transformer != null) {
				transformer.transformWithAxisOrderCorrection(ps);
			}
			for(Object p : ps) {
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
					//Logger.info("read lines");
					double[][] linePoints = subGeo.GetPoints(3); // 3 dimensions needed for correct parameter TransformPoints
					if(transformer != null) {
						transformer.transformWithAxisOrderCorrection(linePoints);
					}
					lines.add(linePoints);
					break;
				}
				default: 
					Logger.warn("unknown MULTILINESTRING sub geometry " + subType + "  "+ Long.toHexString(Integer.toUnsignedLong(subType)) + "  " + subGeo.GetGeometryName());
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
							//Logger.info("read polygons");
							double[][] polygonPoints = subsubGeo.GetPoints();
							if(transformer != null) {
								transformer.transformWithAxisOrderCorrection(polygonPoints);
							}
							polygons.add(polygonPoints);

							break;
						}
						default: 
							Logger.warn("unknown MULTIPOLYGON POLYGON sub geometry " + subsubType + "  "+ Long.toHexString(Integer.toUnsignedLong(subsubType)) + "  " + subsubGeo.GetGeometryName());
						}
					}
					break;
				}
				default: 
					Logger.warn("unknown MULTIPOLYGON sub geometry " + subType + "  "+ Long.toHexString(Integer.toUnsignedLong(subType)) + "  " + subGeo.GetGeometryName());
				}
			}
			break;
		}
		default: 
			Logger.warn("unknown geometry " + type + "  "+ Long.toHexString(Integer.toUnsignedLong(type)) + "  " + geometry.GetGeometryName());
		}		
	}
}
