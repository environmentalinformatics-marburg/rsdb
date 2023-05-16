package server.api.rasterdb;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.tinylog.Logger;

import pointcloud.Rect2d;
import rasterdb.Band;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.TimeBand;
import rasterdb.TimeBandProcessor;
import rasterdb.TimeFrameProducer;
import util.GeoUtil;
import util.Range2d;
import util.Timer;
import util.frame.DoubleFrame;
import util.frame.FloatFrame;
import util.frame.ShortFrame;

public class TimeFrameReprojector implements TimeFrameProducer {

	private final RasterDB rasterdb;
	private final GeoReference ref;
	private final TimeBandProcessor timeBandProcessor;
	private final Rect2d layerRect;
	private final int width;
	private final int height;
	private final Rect2d wmsRect;
	private final String srLayerWKT;
	private final String srWmsWKT;
	private final double[] geoTranformLayer;
	private final double[] geoTranformWMS;
	//private final int eResampleAlg = gdalconst.GRA_NearestNeighbour;
	private final int eResampleAlg = gdalconst.GRA_Bilinear;
	//private final int eResampleAlg = gdalconst.GRA_Cubic;
	private final double warpMemoryLimit = 0d;
	private final double maxexrror = 0.5d;
	//private final double maxexrror = 0.0d;

	public TimeFrameReprojector(RasterDB rasterdb, int layerEPSG, int wmsEPSG, Rect2d wmsRect, int width, int height) {
		this.rasterdb = rasterdb;
		this.width = width;
		this.height = height;
		this.ref = rasterdb.ref();
		this.wmsRect = wmsRect;
		CoordinateTransformation ctToLayer = GeoUtil.getCoordinateTransformation(wmsEPSG, layerEPSG);
		double[][] rePoints = wmsRect.createPoints9();
		ctToLayer.TransformPoints(rePoints);
		this.layerRect = Rect2d.ofPoints(rePoints);		
		Range2d range2d = ref.rect2dToRange2d(layerRect);
		this.timeBandProcessor = new TimeBandProcessor(rasterdb, range2d, width, height);
		srLayerWKT = GeoUtil.getWKT(layerEPSG);
		srWmsWKT = GeoUtil.getWKT(wmsEPSG);
		int scale = timeBandProcessor.getScale();
		Range2d layerRange2d = timeBandProcessor.getDstRange();
		double layerGeoXmin = ref.pixelXToGeo(layerRange2d.xmin * scale);
		double layerGeoYmin = ref.pixelYToGeo(layerRange2d.ymin * scale);
		double layerPixelSizeX = ref.pixel_size_x * scale;
		double layerPixelSizeY = ref.pixel_size_y * scale;
		geoTranformLayer = GeoUtil.createGeotranform(layerGeoXmin, layerGeoYmin, layerPixelSizeX, layerPixelSizeY);
		double dstResX = wmsRect.width() / width;
		double dstResY = wmsRect.height() / height;
		geoTranformWMS = GeoUtil.createGeotranform(wmsRect.xmin, wmsRect.ymin, dstResX, dstResY);
	}

	@Override
	public ShortFrame getShortFrame(int timestamp, Band band) {
		Dataset datasetSrc = null;
		Dataset datasetDst = null;
		try {
			Timer.start("getShortFrame");
			ShortFrame srcFrame = timeBandProcessor.getShortFrame(timestamp, band);		
			datasetSrc = GeoUtil.GDAL_MEM_DRIVER.Create("", srcFrame.width, srcFrame.height, 1, gdalconst.GDT_UInt16);
			datasetSrc.SetProjection(srLayerWKT);
			datasetSrc.SetGeoTransform(geoTranformLayer);
			org.gdal.gdal.Band gdalBandSrc = datasetSrc.GetRasterBand(1);
			for(int y = 0; y < srcFrame.height; y++) {
				gdalBandSrc.WriteRaster(0, y, srcFrame.width, 1, srcFrame.data[y]);
			}

			datasetDst = GeoUtil.GDAL_MEM_DRIVER.Create("", width, height, 1, gdalconst.GDT_UInt16);
			datasetDst.SetProjection(srWmsWKT);
			datasetDst.SetGeoTransform(geoTranformWMS);
			int ret = gdal.ReprojectImage(datasetSrc, datasetDst, srLayerWKT, srWmsWKT, eResampleAlg, warpMemoryLimit, maxexrror);
			datasetSrc.delete();
			datasetSrc = null;
			if(ret != 0) {
				throw new RuntimeException("reprojection error");
			}
			org.gdal.gdal.Band gdalBandDst = datasetDst.GetRasterBand(1);
			short[][] dstData = new short[height][width];
			//Logger.info("start read raster " + width + "  " + height);
			for(int y = 0; y < height; y++) {
				gdalBandDst.ReadRaster(0, y, width, 1, gdalconst.GDT_UInt16, dstData[y]);
			}
			datasetDst.delete();
			datasetDst = null;
			//Logger.info("end read raster");
			ShortFrame dstFrame = new ShortFrame(dstData, 0, 0, width - 1, height - 1);
			Logger.info(Timer.stop("getShortFrame"));
			return dstFrame;
		} finally {
			if(datasetSrc != null) {
				datasetSrc.delete();
				datasetSrc = null;
			}
			if(datasetDst != null) {
				datasetDst.delete();
				datasetDst = null;
			}
		}
	}

	@Override
	public FloatFrame getFloatFrame(int timestamp, Band band) {
		Dataset datasetSrc = null;
		Dataset datasetDst = null;
		try {
			Timer.start("getFloatFrame");
			FloatFrame srcFrame = timeBandProcessor.getFloatFrame(timestamp, band);		
			datasetSrc = GeoUtil.GDAL_MEM_DRIVER.Create("", srcFrame.width, srcFrame.height, 1, gdalconst.GDT_Float32);
			datasetSrc.SetProjection(srLayerWKT);
			datasetSrc.SetGeoTransform(geoTranformLayer);
			org.gdal.gdal.Band gdalBandSrc = datasetSrc.GetRasterBand(1);
			for(int y = 0; y < srcFrame.height; y++) {
				gdalBandSrc.WriteRaster(0, y, srcFrame.width, 1, srcFrame.data[y]);
			}

			datasetDst = GeoUtil.GDAL_MEM_DRIVER.Create("", width, height, 1, gdalconst.GDT_Float32);
			datasetDst.SetProjection(srWmsWKT);
			datasetDst.SetGeoTransform(geoTranformWMS);
			int ret = gdal.ReprojectImage(datasetSrc, datasetDst, srLayerWKT, srWmsWKT, eResampleAlg, warpMemoryLimit, maxexrror);
			datasetSrc.delete();
			datasetSrc = null;
			if(ret != 0) {
				throw new RuntimeException("reprojection error");
			}
			org.gdal.gdal.Band gdalBandDst = datasetDst.GetRasterBand(1);
			float[][] dstData = new float[height][width];
			//Logger.info("start read raster " + width + "  " + height);
			for(int y = 0; y < height; y++) {
				gdalBandDst.ReadRaster(0, y, width, 1, gdalconst.GDT_Float32, dstData[y]);
			}
			datasetDst.delete();
			datasetDst = null;
			//Logger.info("end read raster");
			FloatFrame dstFrame = new FloatFrame(dstData, 0, 0, width - 1, height - 1);
			Logger.info(Timer.stop("getFloatFrame"));
			return dstFrame;
		} finally {
			if(datasetSrc != null) {
				datasetSrc.delete();
				datasetSrc = null;
			}
			if(datasetDst != null) {
				datasetDst.delete();
				datasetDst = null;
			}
		}
	}

	@Override
	public DoubleFrame getDoubleFrame(int timestamp, Band band) {
		Dataset datasetSrc = null;
		Dataset datasetDst = null;
		try {
			Timer.start("getDoubleFrame");
			DoubleFrame srcFrame = timeBandProcessor.getDoubleFrame(timestamp, band);		
			datasetSrc = GeoUtil.GDAL_MEM_DRIVER.Create("", srcFrame.width, srcFrame.height, 1, gdalconst.GDT_Float64);
			datasetSrc.SetProjection(srLayerWKT);
			datasetSrc.SetGeoTransform(geoTranformLayer);
			org.gdal.gdal.Band gdalBandSrc = datasetSrc.GetRasterBand(1);
			for(int y = 0; y < srcFrame.height; y++) {
				gdalBandSrc.WriteRaster(0, y, srcFrame.width, 1, srcFrame.data[y]);
			}

			datasetDst = GeoUtil.GDAL_MEM_DRIVER.Create("", width, height, 1, gdalconst.GDT_Float64);
			datasetDst.SetProjection(srWmsWKT);
			datasetDst.SetGeoTransform(geoTranformWMS);
			int ret = gdal.ReprojectImage(datasetSrc, datasetDst, srLayerWKT, srWmsWKT, eResampleAlg, warpMemoryLimit, maxexrror);
			datasetSrc.delete();
			datasetSrc = null;
			if(ret != 0) {
				throw new RuntimeException("reprojection error");
			}
			org.gdal.gdal.Band gdalBandDst = datasetDst.GetRasterBand(1);
			double[][] dstData = new double[height][width];
			//Logger.info("start read raster " + width + "  " + height);
			for(int y = 0; y < height; y++) {
				gdalBandDst.ReadRaster(0, y, width, 1, gdalconst.GDT_Float64, dstData[y]);
			}
			datasetDst.delete();
			datasetDst = null;
			//Logger.info("end read raster");
			DoubleFrame dstFrame = new DoubleFrame(dstData, 0, 0, width - 1, height - 1);
			Logger.info(Timer.stop("getDoubleFrame"));
			return dstFrame;
		} finally {
			if(datasetSrc != null) {
				datasetSrc.delete();
				datasetSrc = null;
			}
			if(datasetDst != null) {
				datasetDst.delete();
				datasetDst = null;
			}
		}
	}
	
	@Override
	public DoubleFrame getDoubleFrameConst(double value) {		
		DoubleFrame doubleFrame = DoubleFrame.ofRange2d(width, height, new Range2d(0, 0, width - 1, height - 1));
		doubleFrame.fill(value);
		return doubleFrame;
	}
	
	@Override
	public Band getBand(int index) {
		return rasterdb.bandMapReadonly.get(index);		
	}
	
	@Override
	public TimeBand getTimeBand(int timestamp, int bandIndex) {
		Band band = getBand(bandIndex);		
		return band == null ? null : new TimeBand(timestamp, band);		
	}
}
