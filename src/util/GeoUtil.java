package util;

import java.util.concurrent.ConcurrentHashMap;

import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.locationtech.proj4j.CRSFactory;
import org.tinylog.Logger;

public class GeoUtil {

	public static final int EPSG_WGS84 = 4326;
	public static final int EPSG_WEB_MERCATOR = 3857;
	public static final SpatialReference WGS84_SPATIAL_REFERENCE = spatialReferenceFromEPSG(EPSG_WGS84);
	public static final SpatialReference WEB_MERCATOR_SPATIAL_REFERENCE = spatialReferenceFromEPSG(EPSG_WEB_MERCATOR);	
	public static final CRSFactory CRS_FACTORY = new CRSFactory();

	public static Driver GDAL_MEM_DRIVER = null;
	
	private static final ConcurrentHashMap<Integer, SpatialReference> epsgSrMap = new ConcurrentHashMap<Integer, SpatialReference>();
	private static final ConcurrentHashMap<Integer, String> epsgWKTMap = new ConcurrentHashMap<Integer, String>();
	private static final ConcurrentHashMap<Long, CoordinateTransformation> transMap = new ConcurrentHashMap<Long, CoordinateTransformation>();

	static {
		gdal.AllRegister();
		GDAL_MEM_DRIVER = gdal.GetDriverByName("MEM");
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

	public static SpatialReference getSpatialReference(int epsg) {
		if(epsg <= 0) {
			return null;
		}
		SpatialReference sr = epsgSrMap.computeIfAbsent(epsg, GeoUtil::spatialReferenceFromEPSG);
		return sr;
	}
	
	private static CoordinateTransformation createCoordinateTransformation(int srcEPSG, int dstEPSG) {
		SpatialReference srcSr = getSpatialReference(srcEPSG);
		SpatialReference dstSr = getSpatialReference(dstEPSG);
		if(srcSr == null || dstSr == null) {
			return null;
		}
		CoordinateTransformation ct = CoordinateTransformation.CreateCoordinateTransformation(srcSr, dstSr);
		return ct;
	}
	
	private static CoordinateTransformation createCoordinateTransformation(long srcEPSG_dstEPSG) {
		int srcEPSG = (int) (srcEPSG_dstEPSG >> 32);
		int dstEPSG = (int) srcEPSG_dstEPSG;	
		return createCoordinateTransformation(srcEPSG, dstEPSG);
	}
	
	public static CoordinateTransformation getCoordinateTransformation(int srcEPSG, int dstEPSG) {
		long srcEPSG_dstEPSG = (((long) srcEPSG) << 32) | (dstEPSG & 0xffffffffL);
		CoordinateTransformation ct = transMap.computeIfAbsent(srcEPSG_dstEPSG, GeoUtil::createCoordinateTransformation);
		return ct;
		
	}
	
	private static String wktFromEPSG(int epsg) {
		SpatialReference sr = getSpatialReference(epsg);
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
}
