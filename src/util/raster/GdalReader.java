package util.raster;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


import org.tinylog.Logger;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.GCP;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

import util.MinMax2d;
import util.Timer;
import util.Util;

public class GdalReader {
	

	public static final int GDAL_BYTE = 1;
	public static final int GDAL_UINT16 = 2;
	public static final int GDAL_INT16 = 3;
	public static final int GDAL_UINT32 = 4;
	public static final int GDAL_INT32 = 5;
	public static final int GDAL_FLOAT32  = 6;
	public static final int GDAL_FLOAT64 = 7;

	private String filename;

	static final int GGA_InverseDistanceToAPower = 1;                
	static final int GGA_MovingAverage = 2;               
	static final int GGA_NearestNeighbor = 3;    
	static final int GGA_MetricMinimum = 4;   
	static final int GGA_MetricMaximum = 5;       
	static final int GGA_MetricRange = 6;

	/*//not supported
	static final int GGA_MetricCount = 7; 
	static final int GGA_MetricAverageDistance = 8;
	static final int GGA_MetricAverageDistancePts = 9;
	static final int GGA_Linear = 10;
	static final int GGA_InverseDistanceToAPowerNearestNeighbor = 11;
	 */

	public static Map<String, String> wktMap = new HashMap<String, String>();
	static {
		wktMap.put("PROJCS[\"WGS 84 / Pseudo-Mercator\",GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0],UNIT[\"degree\",0.0174532925199433],AUTHORITY[\"EPSG\",\"4326\"]],PROJECTION[\"Mercator_1SP\"],PARAMETER[\"central_meridian\",0],PARAMETER[\"scale_factor\",1],PARAMETER[\"false_easting\",0],PARAMETER[\"false_northing\",0],UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]],EXTENSION[\"PROJ4\",\"+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs\"],AUTHORITY[\"EPSG\",\"3857\"]]",
				"EPSG:3857");
	}

	public static Map<String, String> codeMap = new HashMap<String, String>();
	static {
		codeMap.put("EPSG:3857", "+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs ");
	}

	static {
		gdal.AllRegister();
		Logger.info("GDAL version "+gdal.VersionInfo());
	}

	public double[] transform(double[] tr, double px, double py) {
		double Xp = tr[0] + px*tr[1] + py*tr[2]; 
		double Yp = tr[3] + px*tr[4] + py*tr[5];
		return new double[]{Xp,Yp, 0d};
	}

	public double[] transform(CoordinateTransformation ct, double[] p) {
		ct.TransformPoint(p);
		return p;
	}

	public void info() {
		Logger.info(filename);
		Logger.info(x_range+" "+y_range); 
		int rasterCount = dataset.getRasterCount();
		Logger.info("RasterCount "+rasterCount);
		Logger.info("GetMetadata_Lis "+dataset.GetMetadata_List());
		Logger.info("MetadataDomainList "+dataset.GetMetadataDomainList());
		Logger.info("Projection "+dataset.GetProjection());
		Logger.info("Description "+dataset.GetDescription());
		Logger.info("GCPProjection "+dataset.GetGCPProjection());
		Logger.info("ProjectionRef "+dataset.GetProjectionRef());
		Logger.info(dataset.GetMetadata_List("IMAGE_STRUCTURE"));
		Logger.info("GeoTransform "+Arrays.toString(getGeoRef()));
		Logger.info(dataset.GetMetadataItem("TIFFTAG_IMAGEDESCRIPTION"));
	}

	public String getWKT() {
		return dataset.GetProjectionRef();
	}

	/**
	 * 
	 * @return null if missing code
	 */
	public String getProj4() {
		try {
			SpatialReference sprSrc = new SpatialReference("");	
			String projRef = dataset.GetProjectionRef();
			if(projRef == null || projRef.isEmpty()) {
				return null;
			}
			sprSrc.ImportFromWkt(projRef);
			String proj4 = sprSrc.ExportToProj4();
			return proj4;
		} catch (Exception e) {
			Logger.warn(e);
			return null;
		}
	}

	/**
	 * currently wrong code if not set in WKT !!!!! (ENVI files)
	 * @return null if missing code
	 */
	public String getCRS_code() {
		try {
			SpatialReference sprSrc = new SpatialReference(""); 			
			String projRef = dataset.GetProjectionRef();
			if(projRef == null || projRef.isEmpty()) {
				return null;
			}
			Logger.info(projRef);
			sprSrc.ImportFromWkt(projRef);
			if(sprSrc.GetAuthorityName("PROJCS") != null) {
				return sprSrc.GetAuthorityName("PROJCS")+":"+sprSrc.GetAuthorityCode("PROJCS");
			}
			Logger.info("--------------GET ------------- "+sprSrc.GetAuthorityCode("GEOGCS"));
			Logger.info("--------------GET ------------- "+sprSrc.GetAuthorityName("GEOGCS"));
			String name = sprSrc.GetAuthorityName("GEOGCS");
			String code = sprSrc.GetAuthorityCode("GEOGCS");
			return code == null ? null : ((name == null ? "USER":name)+":"+code);
		} catch (Exception e) {
			Logger.warn(e);
			return null;
		}
	}

	private void reproject() {
		gdal.AllRegister();
		Dataset dataset = gdal.Open(filename);
		int x_range = dataset.getRasterXSize();
		int y_range = dataset.getRasterYSize();
		int srcSize = x_range*y_range;

		Logger.info(filename);
		Logger.info(x_range+" "+y_range);
		int rasterCount = dataset.getRasterCount();
		Logger.info("bands "+rasterCount);
		Logger.info(dataset.GetMetadata_List());
		Logger.info(dataset.GetMetadataDomainList());
		Logger.info(dataset.GetProjection());
		Logger.info(dataset.GetDescription());
		Logger.info(dataset.GetGCPProjection());
		Logger.info(dataset.GetProjectionRef());
		Logger.info(dataset.GetMetadata_List("IMAGE_STRUCTURE"));

		/*Vector<GCP> gcps = dataset.GetGCPs();
		for(GCP gcp:gcps) {
			Logger.info(gcp.getGCPLine()+" "+gcp.getGCPPixel()+" "+gcp.getGCPX()+" "+gcp.getGCPY()+" "+gcp.getGCPZ());
		}*/



		//short[] in = new short[srcSize];
		//short[][] data = null;

		Timer.start("GDAL read");
		/*for(int bandIndex=1;bandIndex<=rasterCount;bandIndex++) {
			Band band = dataset.GetRasterBand(bandIndex);
			Logger.info(band.GetMetadata_Dict());
			int result = band.ReadRaster(0, 0, x_range, y_range, in);
			Logger.info(bandIndex+" result "+result);
			data = Util.arrayToArrayArray(in, x_range,data);



		}*/

		String proj4 = "+proj=utm +zone=32 +ellps=WGS84 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs ";
		SpatialReference sprDst = new SpatialReference();
		sprDst.ImportFromProj4(proj4);


		SpatialReference sprSrc = new SpatialReference("");		
		sprSrc.ImportFromWkt(dataset.GetGCPProjection());
		CoordinateTransformation ct = CoordinateTransformation.CreateCoordinateTransformation(sprSrc, sprDst);

		Vector<GCP> gcps = dataset.GetGCPs();
		Logger.info("gcps "+gcps.size());
		double[] p = new double[]{0d, 0d, 0d};
		MinMax2d minmax = new MinMax2d();
		for(GCP gcp:gcps) {
			p[0] = gcp.getGCPX();
			p[1] = gcp.getGCPY();
			ct.TransformPoint(p);
			minmax.apply(p[0], p[1]);
		}
		if(gcps.size()<4) { // TODO change
			double[] trSrc = dataset.GetGeoTransform();
			Logger.info("src "+Arrays.toString(trSrc));
			minmax.apply(transform(ct, transform(trSrc, 0, 0)));
			minmax.apply(transform(ct, transform(trSrc, x_range-1, 0)));
			minmax.apply(transform(ct, transform(trSrc, 0, y_range-1)));
			minmax.apply(transform(ct, transform(trSrc, x_range-1, y_range-1)));
		}

		Logger.info(minmax);

		double pixel_width = 20;

		int dstWidth = (int) (minmax.xrange() / pixel_width);
		int dstHeight = (int) (minmax.yrange() / pixel_width);
		int dstSize = dstWidth*dstHeight;
		Logger.info(srcSize+" = "+x_range+" * "+y_range);
		Logger.info(dstSize+" = "+dstWidth+" * "+dstHeight);


		Driver driver = gdal.GetDriverByName("GTIFF");
		Dataset outDataset = driver.Create("c:/temp_sentinel/S1A_IW_GRDH_1SDV_20141221T053417_20141221T053442_003813_0048ED_51F1.SAFE/measurement/test.tif", dstWidth, dstHeight, rasterCount, gdalconst.GDT_UInt16);


		String wkt = sprDst.ExportToWkt();
		Logger.info(wkt);

		outDataset.SetProjection(wkt);

		//Xp = padfTransform[0] + px*padfTransform[1] + py*padfTransform[2];
		//Yp = padfTransform[3] + px*padfTransform[4] + py*padfTransform[5];

		//Xp = minmax.xmin + px*pixel_width;
		//Yp = minmax.ymax + py*pixel_width;

		//px = (Xp - minmax.xmin) / pixel_width;
		//py = (Yp - minmax.ymax) / pixel_width;

		double[] tr = new double[6];		
		tr[0] = minmax.xmin; //left pixel
		tr[1] = pixel_width; //pixel width
		tr[2] = 0; //zero
		tr[3] = minmax.ymax; //upper pixel 
		tr[4] = 0; //zero
		tr[5] = -pixel_width; //-pixel height

		outDataset.SetGeoTransform(tr);

		Logger.info("dst "+Arrays.toString(outDataset.GetGeoTransform()));

		Timer.start("reproject");
		//gdal.ReprojectImage(dataset, outDataset);
		Dataset src_ds = dataset;
		Dataset dst_ds = outDataset;
		String src_wkt = null;
		String dst_wkt = null;
		int eResampleAlg = GGA_MovingAverage;
		double WarpMemoryLimit = 500*1024*1024d;
		double maxerror = 0.125d; //default value in gdalwarp 0.125d //TODO check!
		gdal.ReprojectImage(src_ds, dst_ds, src_wkt, dst_wkt, eResampleAlg, WarpMemoryLimit, maxerror);
		//gdal.ReprojectImage(src_ds, dst_ds);
		Logger.info(Timer.stop("reproject"));

		Logger.info(Timer.stop("GDAL read"));	
	}

	public final Dataset dataset;
	public final int x_range;
	public final int y_range;
	public final int src_size;

	public GdalReader(String filename) {
		this.filename = filename;
		this.dataset = gdal.Open(filename);
		if(dataset == null) {
			String message = gdal.GetLastErrorMsg();
			if(message != null) {
				throw new RuntimeException("could not open in GDAL: " + filename + "  " + message);		
			} else {
				throw new RuntimeException("could not open in GDAL: " + filename);
			}
		}
		this.x_range = dataset.getRasterXSize();
		this.y_range = dataset.getRasterYSize();
		this.src_size = x_range*y_range;		
	}

	public GdalReader(Dataset dataset) {
		this.filename = "";
		this.dataset = dataset;
		this.x_range = dataset.getRasterXSize();
		this.y_range = dataset.getRasterYSize();
		this.src_size = x_range*y_range;		
	}

	/**
	 * Get all Data of one band
	 * @param band index number of band (starting with zero)
	 * @param targetData maybe null, will be zero-filled at start.
	 * @return targetData filled with data or new array if targetData is null or wrong dimensions
	 * @throws IOException
	 */
	public short[][] getDataShort(int bandIndex, short[][] targetData) throws IOException {
		Band band = dataset.GetRasterBand(bandIndex);
		short[] in = new short[src_size];
		int result = band.ReadRaster(0, 0, x_range, y_range, in);
		if(result!=0) {
			throw new RuntimeException("read error "+result+"   in "+filename);
		}
		return Util.arrayToArrayArray(in, x_range,targetData);
	}

	/**
	 * Get all Data of one band
	 * @param band index number of band (starting with zero)
	 * @param targetData maybe null, will be zero-filled at start.
	 * @return targetData filled with data or new array if targetData is null or wrong dimensions
	 * @throws IOException
	 */
	public short[][] getDataShort(int bandIndex, short[][] targetData, int yoff, int ysize) throws IOException {
		Band band = dataset.GetRasterBand(bandIndex);
		short[] in = new short[x_range * ysize];
		int result = band.ReadRaster(0, yoff, x_range, ysize, in);
		if(result!=0) {
			throw new RuntimeException("read error "+result+"   in "+filename);
		}
		return Util.arrayToArrayArray(in, x_range,targetData);
	}
	
	/**
	 * Get all Data of one band
	 * @param band index number of band (starting with zero)
	 * @param targetData maybe null, will be zero-filled at start.
	 * @return targetData filled with data or new array if targetData is null or wrong dimensions
	 * @throws IOException
	 */
	public byte[][] getDataByte(int bandIndex, byte[][] targetData, int yoff, int ysize) throws IOException {
		Band band = dataset.GetRasterBand(bandIndex);
		byte[] in = new byte[x_range * ysize];
		int result = band.ReadRaster(0, yoff, x_range, ysize, in);
		if(result!=0) {
			throw new RuntimeException("read error "+result+"   in "+filename);
		}
		return Util.arrayToArrayArray(in, x_range,targetData);
	}

	public short[][] getDataShortOfByte(int bandIndex, short[][] targetData) throws IOException {
		Band band = dataset.GetRasterBand(bandIndex);
		byte[] in = new byte[src_size];
		int result = band.ReadRaster(0, 0, x_range, y_range, in);
		if(result!=0) {
			throw new RuntimeException("read error "+result+"   in "+filename);
		}
		/*for (int i = 0; i < in.length; i++) {
			byte v = in[i];
			short w = (short) (v & 0xff);
			if(v == -1) {
				System.out.println(v + " -> " + (w & 0xff));
			}
		}*/
		return Util.arrayToArrayArrayOfByte(in, x_range,targetData);
	}

	public float[][] getDataFloat(int bandIndex, float[][] targetData) throws IOException {
		Band band = dataset.GetRasterBand(bandIndex);
		float[] in = new float[src_size];
		int result = band.ReadRaster(0, 0, x_range, y_range, in);
		if(result!=0) {
			throw new RuntimeException("read error "+result+"   in "+filename);
		}
		return Util.arrayToArrayArray(in, x_range, targetData);
	}

	public float[][] getDataFloat(int bandIndex, float[][] targetData, int yoff, int ysize) throws IOException {
		Band band = dataset.GetRasterBand(bandIndex);
		float[] in = new float[x_range * ysize];
		int result = band.ReadRaster(0, yoff, x_range, ysize, in);
		if(result!=0) {
			throw new RuntimeException("read error "+result+"   in "+filename);
		}
		return Util.arrayToArrayArray(in, x_range, targetData);
	}

	public float[][] getDataFloat(int bandIndex, float[][] targetData, float noDataValue) throws IOException {
		Band band = dataset.GetRasterBand(bandIndex);
		float[] in = new float[src_size];
		int result = band.ReadRaster(0, 0, x_range, y_range, in);
		if(result!=0) {
			throw new RuntimeException("read error "+result+"   in "+filename);
		}
		if(noDataValue != Float.NaN) {
			for (int i = 0; i < src_size; i++) {
				in[i] = (in[i] == noDataValue) ? Float.NaN : in[i];
			}
		}
		return Util.arrayToArrayArray(in, x_range, targetData);
	}

	public float[][] getDataFloat(int bandIndex, float[][] targetData, float noDataValue, int yoff, int ysize) throws IOException {
		Band band = dataset.GetRasterBand(bandIndex);
		int len = x_range * ysize;
		float[] in = new float[len];
		int result = band.ReadRaster(0, yoff, x_range, ysize, in);
		if(result!=0) {
			throw new RuntimeException("read error "+result+"   in "+filename);
		}
		if(noDataValue != Float.NaN) {
			for (int i = 0; i < len; i++) {
				in[i] = (in[i] == noDataValue) ? Float.NaN : in[i];
			}
		}
		return Util.arrayToArrayArray(in, x_range, targetData);
	}

	public double[] getGeoRef() {
		double[] geo = dataset.GetGeoTransform();
		return new double[]{geo[0],geo[3]};
	}

	private static final double EPSILON = 0.000000000001;

	public double getPixelSize() {
		double[] geo = dataset.GetGeoTransform();
		if(Math.abs(Math.abs(geo[1]) - Math.abs(geo[5])) > EPSILON) {
			throw new RuntimeException("no square pixels "+Arrays.toString(geo));
		}
		return Math.abs(geo[1]);
	}

	public double getPixelSize_x() {
		double[] geo = dataset.GetGeoTransform();
		return Math.abs(geo[1]);
	}

	public double getPixelSize_y() {
		double[] geo = dataset.GetGeoTransform();
		return Math.abs(geo[5]);
	}

	public int getRasterCount() {
		return dataset.getRasterCount();
	}

	public static int gdalBandDataTypeToNumber(String name) {
		switch(name) {
		case "BYTE": {
			return GdalReader.GDAL_BYTE;
		}
		case "UINT16": {
			return GdalReader.GDAL_UINT16;
		}
		case "INT16": {
			return GdalReader.GDAL_INT16;
		}
		case "UINT32": {
			return GdalReader.GDAL_UINT32;
		}
		case "INT32": {
			return GdalReader.GDAL_INT32;
		}
		case "FLOAT32": {
			return GdalReader.GDAL_FLOAT32;
		}
		case "FLOAT64": {
			return GdalReader.GDAL_FLOAT64;
		}
		default:
			throw new RuntimeException("unknown");				
		}

	}
}
