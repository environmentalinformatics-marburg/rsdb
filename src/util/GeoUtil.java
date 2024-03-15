package util;

import java.util.concurrent.ConcurrentHashMap;

import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.locationtech.proj4j.CRSFactory;
import org.tinylog.Logger;

import pointcloud.Rect2d;

public class GeoUtil {

	public static final int EPSG_WGS84 = 4326;
	public static final int EPSG_WEB_MERCATOR = 3857;
	public static final SpatialReference WGS84_SPATIAL_REFERENCE = spatialReferenceFromEPSG(EPSG_WGS84);
	public static final SpatialReference WEB_MERCATOR_SPATIAL_REFERENCE = spatialReferenceFromEPSG(EPSG_WEB_MERCATOR);	
	public static final CRSFactory CRS_FACTORY = new CRSFactory();

	public static Driver GDAL_MEM_DRIVER = null;

	private static final ConcurrentHashMap<Integer, SpatialReference> epsgSrMap = new ConcurrentHashMap<Integer, SpatialReference>();
	private static final ConcurrentHashMap<Integer, String> epsgWKTMap = new ConcurrentHashMap<Integer, String>();
	private static final ConcurrentHashMap<Long, Transformer> transMap = new ConcurrentHashMap<Long, Transformer>();

	static {
		gdal.AllRegister();
		GDAL_MEM_DRIVER = gdal.GetDriverByName("MEM");
	}

	public static class Transformer {
		public final SpatialReference srcSr;
		public final SpatialReference dstSr;
		public final CoordinateTransformation coordinateTransformation;
		public final int srcFirstAxis; //description: https://gdal.org/development/rfc/rfc20_srs_axes.html
		public final int dstFirstAxis;

		public Transformer(SpatialReference srcSr, SpatialReference dstSr) {
			this.srcSr = srcSr;
			this.dstSr = dstSr;
			this.coordinateTransformation = CoordinateTransformation.CreateCoordinateTransformation(srcSr, dstSr);
			this.srcFirstAxis = GeoUtil.WGS84_SPATIAL_REFERENCE.IsSame(srcSr) != 0 ? 1 : srcSr.GetAxisOrientation(null, 0); // workaround for swapped axis but not in GetAxisOrientation for EPSG:4326
			this.dstFirstAxis = GeoUtil.WGS84_SPATIAL_REFERENCE.IsSame(dstSr) != 0 ? 1 : dstSr.GetAxisOrientation(null, 0); // workaround for swapped axis but not in GetAxisOrientation for EPSG:4326
		}

		public void transformWithAxisOrderCorrection(double[][] points) {
			swapOrderIfNeeded(srcFirstAxis, points);
			coordinateTransformation.TransformPoints(points);
			swapOrderIfNeeded(dstFirstAxis, points);			
		}
		
		public void transformWithAxisOrderCorrection(double[] p) {
			swapOrderIfNeeded(srcFirstAxis, p);
			coordinateTransformation.TransformPoint(p);
			swapOrderIfNeeded(dstFirstAxis, p);				
		}

		public double[] transformWithAxisOrderCorrection(double x, double y) {
			double[] p = transformSwapOrderIfNeeded(srcFirstAxis, dstFirstAxis, coordinateTransformation, x, y);
			return p;		
		}

		@Override
		public String toString() {
			return "Transformer [\nsrcSr=" + srcSr.ExportToProj4() + ", \ndstSr=" + dstSr.ExportToProj4() + ", \nsrcFirstAxis=" + srcFirstAxis
					+ ", \ndstFirstAxis=" + dstFirstAxis + "\n]";
		}				
	}

	private GeoUtil() {}	

	private static SpatialReference spatialReferenceFromEPSG(int epsg) {
		SpatialReference sr = new SpatialReference("");
		sr.ImportFromEPSG(epsg);
		return sr;
	}

	public static double[] createGeotranform(double ref_x, double ref_y, double pixelSize_x, double pixelSize_y) {
		return new double[] {ref_x, pixelSize_x, 0, ref_y, 0, pixelSize_y};
	}

	public static int parseEPSG(String crs, int missing) {
		if(crs != null && !crs.isBlank()) {
			String epsgText = crs.toLowerCase();
			if (epsgText.startsWith("epsg:")) {
				String codeText = epsgText.substring(5);
				try {
					return Integer.parseUnsignedInt(codeText.trim());
				} catch (Exception e) {
					Logger.warn(e);
					return missing;
				}
			}
			return missing;
		} else {
			return missing;
		}

	}

	public static SpatialReference getSpatialReferenceFromEPSG(int epsg) {
		if(epsg <= 0) {
			return null;
		}
		SpatialReference sr = epsgSrMap.computeIfAbsent(epsg, GeoUtil::spatialReferenceFromEPSG);
		return sr;
	}

	public static Transformer createCoordinateTransformer(int srcEPSG, int dstEPSG) {
		SpatialReference srcSr = getSpatialReferenceFromEPSG(srcEPSG);
		SpatialReference dstSr = getSpatialReferenceFromEPSG(dstEPSG);
		return createCoordinateTransformer(srcSr, dstSr);
	}
	
	public static Transformer createCoordinateTransformer(SpatialReference srcSr, SpatialReference dstSr) {
		if(srcSr == null || dstSr == null) {
			return null;
		}
		Transformer transformer = new Transformer(srcSr, dstSr);
		return transformer;
	}

	private static Transformer createCoordinateTransformer(long srcEPSG_dstEPSG) {
		int srcEPSG = (int) (srcEPSG_dstEPSG >> 32);
		int dstEPSG = (int) srcEPSG_dstEPSG;	
		return createCoordinateTransformer(srcEPSG, dstEPSG);
	}

	public static Transformer getCoordinateTransformer(int srcEPSG, int dstEPSG) {
		long srcEPSG_dstEPSG = (((long) srcEPSG) << 32) | (dstEPSG & 0xffffffffL);
		Transformer transformer = transMap.computeIfAbsent(srcEPSG_dstEPSG, GeoUtil::createCoordinateTransformer);
		return transformer;

	}

	private static String wktFromEPSG(int epsg) {
		SpatialReference sr = getSpatialReferenceFromEPSG(epsg);
		if(sr == null) {
			return null;
		}
		String wkt = sr.ExportToWkt();
		return wkt;
	}

	public static String getWKT(int epsg) {
		String wkt = epsgWKTMap.computeIfAbsent(epsg, GeoUtil::wktFromEPSG);
		return wkt;
	}

	/**
	 * 
	 * @param srcEPSG
	 * @param srcRect
	 * @return null if not transformed
	 */
	public static Rect2d toWGS84(int srcEPSG, Rect2d srcRect) {
		try {
			Transformer transformer = getCoordinateTransformer(srcEPSG, GeoUtil.EPSG_WGS84);
			if(transformer == null) {
				return null;
			}
			double[][] rePoints = srcRect.createPoints9();
			transformer.transformWithAxisOrderCorrection(rePoints);
			Rect2d wgs84Rect = Rect2d.ofPoints(rePoints);
			return wgs84Rect;		
		} catch(Exception e) {
			Logger.warn(e);
			return null;
		}
	}

	public static void swapOrder(double[] p) {
		double p0 = p[0];
		p[0] = p[1];
		p[1] = p0;
	}

	public static void swapOrder(double[][] points) {
		for (double[] p : points) {
			double p0 = p[0];
			p[0] = p[1];
			p[1] = p0;
		}
	}

	public static void swapOrderIfNeeded(int firstAxis, double[] p) {
		switch(firstAxis) {
		case 1: // OAO_North --> swap;
			Logger.info("swap order");
			swapOrder(p);
			break;
		case 3: // OAO_East --> correct
		default:
			// not further checked if order is correct
		}		
	}

	public static void swapOrderIfNeeded(int firstAxis, double[][] points) {
		switch(firstAxis) {
		case 1: // OAO_North --> swap;
			Logger.info("swap order");
			swapOrder(points);
			break;
		case 3: // OAO_East --> correct
		default:
			// not further checked if order is correct
		}		
	}
	
	public static double[] transformSwapOrderIfNeeded(int srcFirstAxis, int dstFirstAxis, CoordinateTransformation coordinateTransformation, double x, double y) {
		double[] p;
		switch(srcFirstAxis) {
		case 1: {// OAO_North --> swap;
			Logger.info("swap order");
			p = coordinateTransformation.TransformPoint(y, x);
			break;
		}
		case 3: // OAO_East --> correct
		default: {
			// not further checked if order is correct
			p = coordinateTransformation.TransformPoint(x, y);
		}
		}
		swapOrderIfNeeded(dstFirstAxis, p);
		return p;
	}
}
