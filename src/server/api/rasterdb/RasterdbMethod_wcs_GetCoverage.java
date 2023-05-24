package server.api.rasterdb;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.TranslateOptions;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.tinylog.Logger;

import broker.TimeSlice;
import jakarta.servlet.http.HttpServletResponse;
import pointcloud.Rect2d;
import rasterdb.BandProcessor;
import rasterdb.CustomWCS;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.TimeBand;
import rasterdb.TimeFrameProducer;
import server.api.rasterdb.RequestProcessor.TiffDataType;
import util.GeoUtil;
import util.Range2d;
import util.ResponseReceiver;
import util.TimeUtil;
import util.Timer;
import util.Web;
import util.frame.DoubleFrame;
import util.frame.FloatFrame;
import util.frame.ShortFrame;
import util.tiff.TiffBand;
import util.tiff.TiffWriter;

public class RasterdbMethod_wcs_GetCoverage {

	private static final AtomicLong memFileIdCounter = new AtomicLong(0);

	public static void handle_GetCoverage(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		try {
			//Logger.info("handle");
			Timer.start("WCS processing");

			CustomWCS customWCS = null;
			if(!target.isEmpty()) {
				Logger.info("target |" + target + "|");
				customWCS = rasterdb.customWcsMapReadonly.get(target);
				if(customWCS == null) {
					throw new RuntimeException("custom WCS not found |" + target + "|");
				}
			}

			GeoReference ref = rasterdb.ref();
			boolean doReprojection = false;
			int layerEPSG = 0;
			int wcsEPSG = 0;
			String crsParameter = Web.getString(request, "CRS", null);
			if(crsParameter != null) {
				if(ref.has_code()) {
					try {
						layerEPSG = ref.getEPSG(0);
						wcsEPSG = GeoUtil.parseEPSG(crsParameter, 0);
						if(layerEPSG > 0 && wcsEPSG > 0 && layerEPSG != wcsEPSG) {
							doReprojection = true;
						}
					} catch(Exception e) {
						throw new RuntimeException("CRS error");
					}
				}
			} else if(customWCS != null && customWCS.hasEPSG()) {
				wcsEPSG = customWCS.epsg;
				if(layerEPSG > 0 && wcsEPSG > 0 && layerEPSG != wcsEPSG) {
					doReprojection = true;
				}
			}

			int dstWidth = Web.getInt(request, "WIDTH", -1);
			int dstHeight = Web.getInt(request, "HEIGHT", -1);

			String[] bbox = request.getParameter("BBOX").split(",");			
			Rect2d wcsRect = Rect2d.parse(bbox[0], bbox[1], bbox[2], bbox[3]);
			double wcsResx = (wcsRect.xmax - wcsRect.xmin) / dstWidth;
			double wcsResy = (wcsRect.ymax - wcsRect.ymin) / dstHeight;

			ResponseReceiver resceiver = new ResponseReceiver(response);

			Range2d range2d = ref.rect2dToRange2d(wcsRect);

			int timestamp = 0;
			if(Web.has(request, "TIME")) {
				String timeText = Web.getString(request, "TIME");
				TimeSlice timeSlice = rasterdb.getTimeSliceByName(timeText);
				if(timeSlice == null) {
					int[] timestampRange = null;
					try{
						timestampRange = TimeUtil.getTimestampRangeOrNull(timeText);
						if(timestampRange != null) {
							timeSlice = new TimeSlice(timestampRange[0], timeText);
						}
					}catch(Exception e) {
						//Logger.warn("could not parse timestamp: " + timeText);
						throw new RuntimeException("tim slice not found");
					}
				}
				if(timeSlice != null) {
					timestamp = timeSlice.id;
				}
			} else if(!rasterdb.rasterUnit().timeKeysReadonly().isEmpty()){			
				timestamp = rasterdb.rasterUnit().timeKeysReadonly().last();
			}

			Collection<rasterdb.Band> bands = rasterdb.bandMapReadonly.values();
			List<TimeBand> processingBands = TimeBand.of(timestamp, bands);
			
			TiffDataType tiffdataType = RequestProcessorBandsWriters.getTiffDataType(processingBands); // all bands need same data type for tiff reader compatibility (e.g. GDAL)
			TiffWriter tiffWriter = new TiffWriter(dstWidth, dstHeight, wcsRect.xmin, wcsRect.ymin, wcsResx, wcsResy, (short) wcsEPSG);

			if(doReprojection) {				
				TimeFrameReprojector processor = new TimeFrameReprojector(rasterdb, layerEPSG, wcsEPSG, wcsRect, dstWidth, dstHeight);
				directWrite(processor, processingBands, tiffdataType, dstWidth, dstHeight, tiffWriter);
			} else {
				BandProcessor processor = new BandProcessor(rasterdb, range2d, timestamp, dstWidth, dstHeight);
				Range2d srcRange = processor.getDstRange();
				if(srcRange.getPixelCount() > 16777216) { // 4096*4096
					throw new RuntimeException("requested raster too large: " + srcRange.getWidth() + " x " + srcRange.getHeight());
				}

				//Timer.start("raster convert");
				boolean direct = (tiffdataType == TiffDataType.INT16 || tiffdataType == TiffDataType.FLOAT32) && ScaleDownMax2.validScaleDownMax2(srcRange.getWidth(), srcRange.getHeight(), dstWidth, dstHeight);
				//boolean direct = false;
				if(direct) {
					//Logger.info("direct");
					directConvert(processor, processingBands, tiffdataType, dstWidth, dstHeight, tiffWriter);
				} else {
					Logger.info("GDAL  " + tiffdataType + "   " + srcRange.getWidth() + " x " + srcRange.getHeight() + "  ->  " + dstWidth + " x " + dstHeight);
					GDALconvert(processor, processingBands, tiffdataType, dstWidth, dstHeight, tiffWriter);
				}
				//Logger.info(Timer.stop("raster convert"));
			}
			Logger.info(Timer.stop("WCS processing"));

			Timer.start("WCS transfer");
			resceiver.setStatus(HttpServletResponse.SC_OK);
			resceiver.setContentType(Web.MIME_TIFF);
			long tiffSize = tiffWriter.exactSizeOfWriteAuto();
			resceiver.setContentLength(tiffSize);
			tiffWriter.writeAuto(new DataOutputStream(resceiver.getOutputStream()));
			Logger.info(Timer.stop("WCS transfer") + "  " + (tiffSize >= 1024*1024 ? ((tiffSize / (1024*1024)) + " MBytes") : (tiffSize + " Bytes") ) + "  " + dstWidth + " x " + dstHeight + " pixel  " + processingBands.size() + " bands of " + tiffdataType);
		} catch (EofException e) {
			try {
				response.closeOutput();
			} catch(Exception e1) {
				Logger.warn(e1);
			}
			Throwable eCause = e.getCause();
			if(eCause != null && eCause instanceof IOException) {
				Logger.info(eCause.getMessage());
			} else {
				Logger.warn(e);
			}			
		} catch (Exception e) {
			try {
				response.closeOutput();
			} catch(Exception e1) {
				Logger.warn(e1);
			}
			Logger.warn(e);
		}
	}
	
	private static void directWrite(TimeFrameProducer processor, List<TimeBand> processingBands, TiffDataType tiffdataType, int dstWidth, int dstHeight, TiffWriter tiffWriter) {
		Short noDataValue = null;
		for(TimeBand timeband : processingBands) {	
			switch(tiffdataType) {
			case INT16:	{
				ShortFrame frame = processor.getShortFrame(timeband);
				short na = timeband.band.getInt16NA();
				tiffWriter.addTiffBand(TiffBand.ofInt16(frame.data, timeband.toDescription()));
				if(noDataValue == null) {
					noDataValue = timeband.band.getInt16NA();
				}
				break;
			}			
			case FLOAT32: {
				FloatFrame frame = processor.getFloatFrame(timeband);
				tiffWriter.addTiffBand(TiffBand.ofFloat32(frame.data, timeband.toDescription()));
				break;
			}
			case FLOAT64: {
				DoubleFrame frame = processor.getDoubleFrame(timeband);
				tiffWriter.addTiffBand(TiffBand.ofFloat64(frame.data, timeband.toDescription()));
				break;
			}			
			default:
				throw new RuntimeException("unknown tiff data type");
			}
		}
		tiffWriter.setNoDataValue(noDataValue);
	}

	private static void directConvert(BandProcessor processor, List<TimeBand> processingBands, TiffDataType tiffdataType, int dstWidth, int dstHeight, TiffWriter tiffWriter) {
		Range2d srcRange = processor.getDstRange();
		Short noDataValue = null;
		for(TimeBand timeband : processingBands) {	
			switch(tiffdataType) {
			case INT16:	{
				ShortFrame frame = processor.getShortFrame(timeband);
				short na = timeband.band.getInt16NA();
				short[][] dstData = ScaleDownMax2.scaleDownMax2(frame.data, srcRange.getWidth(), srcRange.getHeight(), dstWidth, dstHeight, na);
				tiffWriter.addTiffBand(TiffBand.ofInt16(dstData, timeband.toDescription()));
				if(noDataValue == null) {
					noDataValue = timeband.band.getInt16NA();
				}
				break;
			}			
			case FLOAT32: {
				FloatFrame frame = processor.getFloatFrame(timeband);
				float[][] dstData = ScaleDownMax2.scaleDownMax2(frame.data, srcRange.getWidth(), srcRange.getHeight(), dstWidth, dstHeight);
				tiffWriter.addTiffBand(TiffBand.ofFloat32(dstData, timeband.toDescription()));
				break;
			}
			default:
				throw new RuntimeException("unknown tiff data type");
			}
		}
		tiffWriter.setNoDataValue(noDataValue);
	}

	private static void GDALconvert(BandProcessor processor, List<TimeBand> processingBands, TiffDataType tiffdataType, int dstWidth, int dstHeight, TiffWriter tiffWriter) {
		int gdalDataType = getGdalDatatypeFromTiffDatatype(tiffdataType);
		Range2d srcRange = processor.getDstRange();

		String srcFilename = null;
		Dataset datasetSrc = null;
		Dataset datasetDst = null;
		String dstFilename = null;

		try {
			srcFilename = ""; // parameter not usable
			datasetSrc = GeoUtil.GDAL_MEM_DRIVER.Create(srcFilename, srcRange.getWidth(), srcRange.getHeight(), processingBands.size(), gdalDataType);

			try {
				int bandIndex = 1;
				for(TimeBand timeband : processingBands) {
					Band gdalBand = datasetSrc.GetRasterBand(bandIndex);
					switch(tiffdataType) {
					case INT16:	{
						ShortFrame frame = processor.getShortFrame(timeband);
						for(int y = 0; y < frame.height; y++) {
							gdalBand.WriteRaster(0, y, frame.width, 1, frame.data[y]);
						}
						break;
					}
					case FLOAT32:	{
						FloatFrame frame = processor.getFloatFrame(timeband);
						for(int y = 0; y < frame.height; y++) {
							gdalBand.WriteRaster(0, y, frame.width, 1, frame.data[y]);
						}
						break;
					}
					case FLOAT64: {
						DoubleFrame frame = processor.getDoubleFrame(timeband);
						for(int y = 0; y < frame.height; y++) {
							gdalBand.WriteRaster(0, y, frame.width, 1, frame.data[y]);
						}
						break;
					}
					default:
						throw new RuntimeException("unknown tiff data type");
					}	
					bandIndex++;
				}

				Vector<String> options = new Vector<String>();
				options.add("-outsize");
				options.add(""+dstWidth);
				options.add(""+dstHeight);

				options.add("-r");
				options.add("cubic");
				TranslateOptions translateOptions = new TranslateOptions(options);
				dstFilename = "/vsimem/rsdb_wcs_in_memory_output"+ memFileIdCounter.incrementAndGet() +".tif";
				datasetDst = gdal.Translate(dstFilename, datasetSrc, translateOptions);

				/*Vector<String> v = gdal.ReadDirRecursive("/vsimem");
				if(v.isEmpty()) {
					Logger.info("dir FILES no");
				}
				for(String e:v) {
					Logger.info("dir FILE |" + e + "|");
				}*/

			} finally {
				/*if(srcFilename != null) { // srcFilename not usable
					try {						
						Dataset dataset2 = gdal.Open(srcFilename);
						Logger.info(dataset2.getRasterXSize() + "  " + dataset2.getRasterYSize());
						dataset2.delete();
					} catch(Exception e) {
						Logger.warn(e);
					}
				}*/	
				if(datasetSrc != null) {					
					/*try { // no FileList						
						Vector<String> v = datasetSrc.GetFileList();
						if(v.isEmpty()) {
							Logger.info("src FILES no");
						}
						for(String e:v) {
							Logger.info("src FILE |" + e + "|");
						}
					} catch(Exception e) {
						Logger.warn(e);
					}*/
					try {
						datasetSrc.delete();
						datasetSrc = null;
					} catch(Exception e) {
						Logger.warn(e);
					}
				}			
				/*if(srcFilename != null) { // srcFilename not usable
					try {						
						int unlinkRes = gdal.Unlink(srcFilename);
						Logger.info("unlink [" + unlinkRes + "]  " + srcFilename);
						srcFilename = null;
					} catch(Exception e) {
						Logger.warn(e);
					}
				}*/			
			}

			int bandIndex = 1;
			Short noDataValue = null;
			for(TimeBand timeband : processingBands) {	
				Band gdalBand = datasetDst.GetRasterBand(bandIndex);
				switch(tiffdataType) {
				case INT16:	{
					short[][] dstData = new short[dstHeight][dstWidth];
					for(int y = 0; y < dstHeight; y++) {
						gdalBand.ReadRaster(0, y, dstWidth, 1, gdalDataType, dstData[y]);
					}
					tiffWriter.addTiffBand(TiffBand.ofInt16(dstData, timeband.toDescription()));
					if(noDataValue == null) {
						noDataValue = timeband.band.getInt16NA();
					}
					break;
				}
				case FLOAT32: {
					float[][] dstData = new float[dstHeight][dstWidth];
					for(int y = 0; y < dstHeight; y++) {
						gdalBand.ReadRaster(0, y, dstWidth, 1, gdalDataType, dstData[y]);
					}
					tiffWriter.addTiffBand(TiffBand.ofFloat32(dstData, timeband.toDescription()));
					break;
				}
				case FLOAT64: {
					double[][] dstData = new double[dstHeight][dstWidth];
					for(int y = 0; y < dstHeight; y++) {
						gdalBand.ReadRaster(0, y, dstWidth, 1, gdalDataType, dstData[y]);
					}
					tiffWriter.addTiffBand(TiffBand.ofFloat64(dstData, timeband.toDescription()));
					break;
				}
				default:
					throw new RuntimeException("unknown tiff data type");
				}
				bandIndex++;
			}
			tiffWriter.setNoDataValue(noDataValue);
		} finally {
			/*if(dstFilename != null) { // testing
				try {						
					Dataset dataset2 = gdal.Open(dstFilename);
					Logger.info(dataset2.getRasterXSize() + "  " + dataset2.getRasterYSize());
					dataset2.delete();
				} catch(Exception e) {
					Logger.warn(e);
				}
			}*/	
			if(datasetDst != null) {
				/*try {
					Vector<String> v = datasetDst.GetFileList();
					if(v.isEmpty()) {
						Logger.info("dst FILES no");
					}
					for(String e:v) {
						Logger.info("dst FILE |" + e + "|");
					}
				} catch(Exception e) {
					Logger.warn(e);
				}*/
				try {
					datasetDst.delete();
					datasetDst = null;
				} catch(Exception e) {
					Logger.warn(e);
				}
			}
			if(dstFilename != null) {
				try {
					int unlinkRes = gdal.Unlink(dstFilename);
					if(unlinkRes != 0) {
						Logger.warn("unlink [" + unlinkRes + "]  " + dstFilename);
					}
					dstFilename = null;
				} catch(Exception e) {
					Logger.warn(e);
				}
			}
			if(datasetSrc != null) {					
				/*try { // no FileList						
					Vector<String> v = datasetSrc.GetFileList();
					if(v.isEmpty()) {
						Logger.info("src FILES no");
					}
					for(String e:v) {
						Logger.info("src FILE |" + e + "|");
					}
				} catch(Exception e) {
					Logger.warn(e);
				}*/
				try {
					datasetSrc.delete();
					datasetSrc = null;
				} catch(Exception e) {
					Logger.warn(e);
				}
			}			
			/*if(srcFilename != null) { // srcFilename not usable
				try {						
					int unlinkRes = gdal.Unlink(srcFilename);
					Logger.info("unlink [" + unlinkRes + "]  " + srcFilename);
					srcFilename = null;
				} catch(Exception e) {
					Logger.warn(e);
				}
			}*/
		}		
	}

	public static int getGdalDatatypeFromTiffDatatype(TiffDataType tiffdataType) {
		switch(tiffdataType) {
		case INT16:			
			return gdalconstConstants.GDT_UInt16;
		case FLOAT32:
			return gdalconstConstants.GDT_Float32;
		case FLOAT64:
			return gdalconstConstants.GDT_Float64;
		default:
			throw new RuntimeException("unknown tiff data type");
		}
	}
}