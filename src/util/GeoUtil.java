package util;

import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.osr.SpatialReference;
import org.locationtech.proj4j.CRSFactory;

public class GeoUtil {

	public static final int EPSG_WGS84 = 4326;
	public static final int EPSG_WEB_MERCATOR = 3857;
	public static final SpatialReference WGS84_SPATIAL_REFERENCE = spatialReferenceFromEPSG(EPSG_WGS84);
	public static final SpatialReference WEB_MERCATOR_SPATIAL_REFERENCE = spatialReferenceFromEPSG(EPSG_WEB_MERCATOR);	
	public static final CRSFactory CRS_FACTORY = new CRSFactory();

	public static Driver GDAL_MEM_DRIVER = null;

	static {
		gdal.AllRegister();
		GDAL_MEM_DRIVER = gdal.GetDriverByName("MEM");
	}

	private GeoUtil() {}	

	public static SpatialReference spatialReferenceFromEPSG(int epsg) {
		SpatialReference sr = new SpatialReference("");
		sr.ImportFromEPSG(epsg);
		return sr;
	}

	public static double[] createGeotranform(double ref_x, double ref_y, double pixelSize_x, double pixelSize_y) {
		return new double[] {ref_x, pixelSize_x, 0, ref_y, 0, pixelSize_y};
	}
}
