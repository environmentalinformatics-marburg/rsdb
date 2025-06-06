package remotetask.rasterdb;

import java.io.IOException;


import org.tinylog.Logger;

import broker.Broker;
import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.cell.CellInt16;
import rasterdb.cell.CellInt8;
import rasterdb.cell.CellType;
import rasterdb.cell.CellUint16;
import rasterdb.tile.ProcessingChar;
import rasterdb.tile.ProcessingFloat;
import rasterdb.tile.ProcessingShort;
import rasterdb.tile.TileFloat;
import rasterdb.tile.TilePixel;
import rasterunit.RasterUnitStorage;
import remotetask.MessageSink;
import remotetask.RemoteProxy;
import util.Util;
import util.collections.vec.Vec;
import util.frame.ByteFrame;
import util.frame.CharFrame;
import util.frame.FloatFrame;
import util.frame.ShortFrame;
import util.raster.GdalReader;

public class ImportProcessor extends RemoteProxy {	

	private Broker broker;
	private RasterDB rasterdb;
	private GdalReader gdalreader;
	private Vec<BandSpec> bandSpecs;
	private final double file_pixel_size_y;
	private final boolean update_pyramid;
	private final boolean update_catalog;

	public ImportProcessor(Broker broker, RasterDB rasterdb, GdalReader gdalreader, Vec<BandSpec> bandSpecs, double file_pixel_size_y, boolean update_pyramid, boolean update_catalog) {
		this.broker = broker;
		this.rasterdb = rasterdb;
		this.gdalreader = gdalreader;
		this.bandSpecs = bandSpecs;
		this.file_pixel_size_y = file_pixel_size_y;
		this.update_pyramid = update_pyramid;
		this.update_catalog = update_catalog;
	}

	@Override
	public void close() {
		broker = null;
		rasterdb = null;
		gdalreader = null;
		bandSpecs = null;
	}

	@Override
	public void process() throws IOException {
		setMessage("importing");
		double[] gdal_ref = gdalreader.getGeoRef();
		double easting = gdal_ref[0];
		double northing = gdal_ref[1];
		double corrected_northing = northing - (gdalreader.y_range * file_pixel_size_y);
		int pixelXmin = rasterdb.ref().geoXToPixel(easting);
		int pixelYmin = rasterdb.ref().geoYToPixel(corrected_northing);


		final int maxPixels = 134_217_728;
		int xRange = gdalreader.x_range;
		int yRange = gdalreader.y_range;
		int maxLines = maxPixels / ((xRange < 1) ? 1 : xRange);
		if(maxLines < 1) {
			maxLines = 1;
		}

		for(BandSpec bandSpec:bandSpecs) {
			if(bandSpec.import_band) {
				setMessage("importing band " + bandSpec.file_band_index);
				int importBandTimestamp = bandSpec.timestamp;
				Logger.info("pixelYmin "+ pixelYmin);
				int yoff = 0;
				while(yoff < yRange) {
					int ysize = yoff + maxLines <= yRange ? maxLines : yRange - yoff;			
					Logger.info("full size "+ yRange + "  batch from " + yoff + " to " + (yoff + (ysize -1)) + " size " + ysize);
					int yEnd = yoff + (ysize - 1);
					int yEndOffset = (yRange - 1) - yEnd;
					int batchPixelYmin = pixelYmin + yEndOffset; 
					//int batchPixelYmin = pixelYmin + ( (yStart + yRange - 1) - (yStart + yBatchSize - 1) );
					Logger.info("batchPixelYmin "+ batchPixelYmin);
					if(yoff > 0) {
						setMessage("importing band " + bandSpec.file_band_index + " (" + ((yoff*100) / yRange) + "%)");
					}
					importBand(rasterdb, gdalreader, bandSpec, pixelXmin, batchPixelYmin, importBandTimestamp, yoff, ysize, getMessageSink());
					yoff += ysize;
				}			
			}
		}

		/*for(BandSpec bandSpec:bandSpecs) {
		 * if(bandSpec.import_band) {
			setMessage("importing band " + bandSpec.file_band_index);
			importBand(rasterdb, gdalreader, bandSpec, pixelXmin, pixelYmin, timestamp);
			}
		}*/

		rasterdb.invalidateLocalRange();

		/*setMessage("calculate extent");
		Range2d localRange = rasterdb.getLocalRange(true);
		Logger.info("new local range " + localRange);*/

		setMessage("flush");
		rasterdb.flush();

		if(update_pyramid) {
			setMessage("rebuild pyramid");
			rasterdb.rebuildPyramid(true, this.getMessageSink());
		}

		if(update_catalog) {
			setMessage("update catalog");
			broker.catalog.refreshCatalog();
		}
		setMessage("import done");
	}

	/**
	 * 
	 * @param rasterdb
	 * @param gdalreader
	 * @param bandSpec
	 * @param pixelXmin
	 * @param pixelYmin
	 * @param timestamp
	 * @param yoff
	 * @param ysize
	 * @param messageSink not null
	 * @throws IOException
	 */
	private static void importBand(RasterDB rasterdb, GdalReader gdalreader, BandSpec bandSpec, int pixelXmin, int pixelYmin, int timestamp, int yoff, int ysize, MessageSink messageSink) throws IOException {
		org.gdal.gdal.Band gdalRasterBand = gdalreader.dataset.GetRasterBand(bandSpec.file_band_index);
		Band rasterdbBand = rasterdb.getBandByNumber(bandSpec.rasterdb_band_index);
		if(rasterdbBand.type != bandSpec.rastedb_band_data_type) {
			throw new RuntimeException("wrong rastedb_band_data_type");
		}
		int gdalRasterDataType = gdalRasterBand.GetRasterDataType();
		if(gdalRasterDataType != bandSpec.gdal_raster_data_type) {
			throw new RuntimeException("wrong rasterdb_band_data_type");
		}
		RasterUnitStorage rasterUnit = rasterdb.rasterUnit();
		switch(bandSpec.rastedb_band_data_type) {
		case TilePixel.TYPE_SHORT: {
			importBand_TYPE_SHORT(rasterdb, bandSpec, gdalreader, rasterUnit, timestamp, rasterdbBand, pixelXmin, pixelYmin, yoff, ysize, false, messageSink);
			break;
		}
		case TilePixel.TYPE_FLOAT: {
			importBand_TYPE_FLOAT(rasterdb, bandSpec, gdalreader, rasterUnit, timestamp, rasterdbBand, pixelXmin, pixelYmin, yoff, ysize, messageSink);
			break;
		}
		case CellType.INT16: {
			importBand_TYPE_SHORT(rasterdb, bandSpec, gdalreader, rasterUnit, timestamp, rasterdbBand, pixelXmin, pixelYmin, yoff, ysize, true, messageSink);
			break;
		}
		case CellType.INT8: {
			importBand_TYPE_BYTE(rasterdb, bandSpec, gdalreader, rasterUnit, timestamp, rasterdbBand, pixelXmin, pixelYmin, yoff, ysize, messageSink);
			break;
		}
		case CellType.UINT16: {
			importBand_TYPE_CHAR(rasterdb, bandSpec, gdalreader, rasterUnit, timestamp, rasterdbBand, pixelXmin, pixelYmin, yoff, ysize, messageSink);
			break;
		}
		default:
			throw new RuntimeException("RasterDB band data type not implemented " + bandSpec.rastedb_band_data_type);			
		}
	}

	private static void importBand_TYPE_SHORT(RasterDB rasterdb, BandSpec bandSpec, GdalReader gdalreader, RasterUnitStorage rasterUnit, int timestamp, Band rasterdbBand, int pixelXmin, int pixelYmin, int yoff, int ysize, boolean isCellInt16, MessageSink messageSink) throws IOException {
		short[][] dataShort = null;		
		switch(bandSpec.gdal_raster_data_type) {
		case GdalReader.GDAL_BYTE:
		case GdalReader.GDAL_UINT16:
		case GdalReader.GDAL_INT16:
		case GdalReader.GDAL_UINT32:
		case GdalReader.GDAL_INT32: {
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_UINT16) {
				messageSink.log("WARN: The conversion of UINT16 to INT16 does not preserve the full value range.");
			}
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_UINT32) {
				messageSink.log("WARN: The conversion of UINT32 to INT16 does not preserve the full value range.");
			}
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_INT32) {
				messageSink.log("WARN: The conversion of INT32 to INT16 does not preserve the full value range.");
			}
			dataShort = gdalreader.getDataShort(bandSpec.file_band_index, null, yoff, ysize);			
			if(bandSpec.no_data_value != null) {
				short bandNaShort = rasterdbBand.getInt16NA();
				short gdalNaShort = bandSpec.no_data_value.shortValue();
				if(bandSpec.no_data_value < Short.MIN_VALUE) { // gdal mapping of uint16 to int16
					gdalNaShort = Short.MIN_VALUE;
				}
				if(bandSpec.no_data_value > Short.MAX_VALUE) { // gdal mapping of uint16 to int16
					gdalNaShort = Short.MAX_VALUE;
				}
				if(gdalNaShort != bandNaShort) {
					messageSink.log("convert file no data value float "+bandSpec.no_data_value + " ==>   short " + gdalNaShort + " to band short " + bandNaShort);
					ProcessingShort.convertNA(dataShort, gdalNaShort, bandNaShort);
				}				
			}			
			break;
		}
		case GdalReader.GDAL_FLOAT64:
		case GdalReader.GDAL_FLOAT32: {
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_FLOAT64) {				
				messageSink.log("WARN: The conversion of FLOAT64 to INT16 does not preserve the full value range and precision.");
			}
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_FLOAT32) {
				messageSink.log("WARN: The conversion of FLOAT32 to INT16 does not preserve the full value range and precision.");
			}			
			float[][] dataFloat = gdalreader.getDataFloat(bandSpec.file_band_index, null, yoff, ysize);
			short bandNaShort = rasterdbBand.getInt16NA();
			if(bandSpec.no_data_value == null) {				
				dataShort = TileFloat.floatToShort(dataFloat, null, bandNaShort);						
			} else {
				float gdalNaFloat = bandSpec.no_data_value.floatValue();
				messageSink.log("convert no data value " + gdalNaFloat + " to " + bandNaShort);
				dataShort = TileFloat.floatToShort(dataFloat, null, gdalNaFloat, bandNaShort);
			}			
			break;
		}
		default:
			throw new RuntimeException("gdal data type not implemented for band type TYPE_SHORT " + bandSpec.gdal_raster_data_type);				
		}
		if(Double.isFinite(bandSpec.value_scale)) {
			messageSink.log("value_scale " + bandSpec.value_scale);
			ShortFrame.rawMul(dataShort, bandSpec.value_scale);
		}
		if(Double.isFinite(bandSpec.value_offset)) {
			messageSink.log("value_offset " + bandSpec.value_offset);
			ShortFrame.rawAdd(dataShort, bandSpec.value_offset);
		}
		dataShort = Util.flipRows(dataShort);
		int cnt;
		if(isCellInt16) {
			CellInt16 cellInt16 = new CellInt16(rasterdb.getTilePixelLen());
			int xlen = dataShort[0].length;
			int ylen = dataShort.length;
			cnt = cellInt16.writeMerge(rasterUnit, timestamp, rasterdbBand, dataShort, pixelYmin, pixelXmin, xlen, ylen);			
		} else {
			cnt = ProcessingShort.writeMerge(rasterUnit, timestamp, rasterdbBand, dataShort, pixelYmin, pixelXmin);
		}		
		rasterUnit.commit();
		messageSink.log("tiles written: " + cnt);
	}

	private static void importBand_TYPE_FLOAT(RasterDB rasterdb, BandSpec bandSpec, GdalReader gdalreader, RasterUnitStorage rasterUnit, int timestamp, Band rasterdbBand, int pixelXmin, int pixelYmin, int yoff, int ysize, MessageSink messageSink) throws IOException {
		switch(bandSpec.gdal_raster_data_type) {
		case GdalReader.GDAL_FLOAT64:
		case GdalReader.GDAL_FLOAT32:
		case GdalReader.GDAL_BYTE:
		case GdalReader.GDAL_UINT16:
		case GdalReader.GDAL_INT16:
		case GdalReader.GDAL_UINT32:
		case GdalReader.GDAL_INT32: {
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_FLOAT64) {
				messageSink.log("WARN: The conversion of FLOAT64 to FLOAT32 may not be completely lossless.");				
			}
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_UINT32) {
				messageSink.log("WARN: The conversion of UINT32 to FLOAT32 may not be completely lossless.");				
			}
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_INT32) {
				messageSink.log("WARN: The conversion of INT32 to FLOAT32 may not be completely lossless.");				
			}
			float[][] dataFloat;
			if(bandSpec.no_data_value == null) {
				dataFloat = gdalreader.getDataFloat(bandSpec.file_band_index, null, yoff, ysize);					
			} else {
				dataFloat = gdalreader.getDataFloat(bandSpec.file_band_index, null, bandSpec.no_data_value.floatValue(), yoff, ysize);
			}
			if(Double.isFinite(bandSpec.value_scale)) {
				messageSink.log("value_scale " + bandSpec.value_scale);
				FloatFrame.rawMul(dataFloat, bandSpec.value_scale);
			}
			if(Double.isFinite(bandSpec.value_offset)) {
				messageSink.log("value_offset " + bandSpec.value_offset);
				FloatFrame.rawAdd(dataFloat, bandSpec.value_offset);
			}
			dataFloat = Util.flipRows(dataFloat);
			int cnt = ProcessingFloat.writeMerge(rasterUnit, timestamp, rasterdbBand, dataFloat, pixelYmin, pixelXmin);
			rasterUnit.commit();
			messageSink.log("tiles written: " + cnt);
			break;
		}
		default:
			throw new RuntimeException("gdal data type not implemented for band type TYPE_FLOAT " + bandSpec.gdal_raster_data_type);				
		}
	}

	private static void importBand_TYPE_BYTE(RasterDB rasterdb, BandSpec bandSpec, GdalReader gdalreader, RasterUnitStorage rasterUnit, int timestamp, Band rasterdbBand, int pixelXmin, int pixelYmin, int yoff, int ysize, MessageSink messageSink) throws IOException {
		byte[][] dataBytes = null;		
		switch(bandSpec.gdal_raster_data_type) {
		case GdalReader.GDAL_BYTE:
		case GdalReader.GDAL_UINT16:
		case GdalReader.GDAL_INT16:
		case GdalReader.GDAL_UINT32:
		case GdalReader.GDAL_INT32:
		case GdalReader.GDAL_FLOAT64:
		case GdalReader.GDAL_FLOAT32: {
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_UINT16) {
				messageSink.log("WARN: The conversion of UINT16 to INT8 does not preserve the full value range.");
			}
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_INT16) {
				messageSink.log("WARN: The conversion of INT16 to INT8 does not preserve the full value range.");
			}
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_UINT32) {
				messageSink.log("WARN: The conversion of UINT32 to INT8 does not preserve the full value range.");
			}
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_INT32) {
				messageSink.log("WARN: The conversion of INT32 to INT8 does not preserve the full value range.");
			}
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_FLOAT64) {
				messageSink.log("WARN: The conversion of FLOAT64 to INT8 does not preserve the full value range and precision.");
			}
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_FLOAT32) {
				messageSink.log("WARN: The conversion of FLOAT32 to INT8 does not preserve the full value range and precision.");
			}
			dataBytes = gdalreader.getDataByte(bandSpec.file_band_index, null, yoff, ysize);	
			break;
		}
		default:
			throw new RuntimeException("gdal data type not implemented for band type int8 " + bandSpec.gdal_raster_data_type);				
		}
		if(Double.isFinite(bandSpec.value_scale)) {
			messageSink.log("value_scale " + bandSpec.value_scale);
			ByteFrame.rawMul(dataBytes, bandSpec.value_scale);
		}
		if(Double.isFinite(bandSpec.value_offset)) {
			messageSink.log("value_offset " + bandSpec.value_offset);
			ByteFrame.rawAdd(dataBytes, bandSpec.value_offset);
		}
		dataBytes = Util.flipRows(dataBytes);
		int cnt;
		CellInt8 cellInt8 = new CellInt8(rasterdb.getTilePixelLen());
		int xlen = dataBytes[0].length;
		int ylen = dataBytes.length;
		cnt = cellInt8.writeMerge(rasterUnit, timestamp, rasterdbBand, dataBytes, pixelYmin, pixelXmin, xlen, ylen);		
		rasterUnit.commit();
		messageSink.log("tiles written: " + cnt);
	}

	private static void importBand_TYPE_CHAR(RasterDB rasterdb, BandSpec bandSpec, GdalReader gdalreader, RasterUnitStorage rasterUnit, int timestamp, Band rasterdbBand, int pixelXmin, int pixelYmin, int yoff, int ysize, MessageSink messageSink) throws IOException {
		char[][] dataChar = null;		
		switch(bandSpec.gdal_raster_data_type) {
		case GdalReader.GDAL_BYTE:
		case GdalReader.GDAL_UINT16:
		case GdalReader.GDAL_INT16:
		case GdalReader.GDAL_UINT32:
		case GdalReader.GDAL_INT32: {
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_INT16) {
				messageSink.log("WARN: The conversion of INT16 to UINT16 may not preserve the full value range.");
			}
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_INT32) {
				messageSink.log("WARN: The conversion of INT32 to UINT16 may not preserve the full value range.");
			}
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_UINT32) {
				messageSink.log("WARN: The conversion of UINT32 to UINT16 may not preserve the full value range.");
			}
			dataChar = gdalreader.getDataChar(bandSpec.file_band_index, null, yoff, ysize);			
			if(bandSpec.no_data_value != null) {
				char bandNaChar = rasterdbBand.getUint16NA();
				char gdalNaChar = (char) bandSpec.no_data_value.intValue();
				if(bandSpec.no_data_value < Character.MIN_VALUE) { // gdal mapping
					gdalNaChar = Character.MIN_VALUE;
				}
				if(bandSpec.no_data_value > Character.MAX_VALUE) { // gdal mapping
					gdalNaChar = Character.MAX_VALUE;
				}
				if(gdalNaChar != bandNaChar) {
					messageSink.log("convert file no data value float "+bandSpec.no_data_value + " ==>   char " + ((int)gdalNaChar) + " to band char " + ((int)bandNaChar));
					ProcessingChar.convertNA(dataChar, gdalNaChar, bandNaChar);
				}				
			}			
			break;
		}
		case GdalReader.GDAL_FLOAT64:
		case GdalReader.GDAL_FLOAT32: {
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_FLOAT64) {				
				messageSink.log("WARN: The conversion of FLOAT64 to UINT16 may not preserve the full value range and precision.");
			}
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_FLOAT32) {
				messageSink.log("WARN: The conversion of FLOAT32 to UINT16 may not preserve the full value range and precision.");
			}			
			float[][] dataFloat = gdalreader.getDataFloat(bandSpec.file_band_index, null, yoff, ysize);
			char bandNaChar = rasterdbBand.getUint16NA();
			if(bandSpec.no_data_value == null) {				
				dataChar = TileFloat.floatToChar(dataFloat, null, bandNaChar);						
			} else {
				float gdalNaFloat = bandSpec.no_data_value.floatValue();
				messageSink.log("convert no data value " + gdalNaFloat + " to " + ((char)bandNaChar));
				dataChar = TileFloat.floatToChar(dataFloat, null, gdalNaFloat, bandNaChar);
			}			
			break;
		}
		default:
			throw new RuntimeException("gdal data type not implemented for band type TYPE_CHAR " + bandSpec.gdal_raster_data_type);				
		}
		if(Double.isFinite(bandSpec.value_scale)) {
			messageSink.log("value_scale " + bandSpec.value_scale);
			CharFrame.rawMul(dataChar, bandSpec.value_scale);
		}
		if(Double.isFinite(bandSpec.value_offset)) {
			messageSink.log("value_offset " + bandSpec.value_offset);
			CharFrame.rawAdd(dataChar, bandSpec.value_offset);
		}
		dataChar = Util.flipRows(dataChar);
		int cnt;

		CellUint16 cellUint16 = new CellUint16(rasterdb.getTilePixelLen());
		int xlen = dataChar[0].length;
		int ylen = dataChar.length;
		cnt = cellUint16.writeMerge(rasterUnit, timestamp, rasterdbBand, dataChar, pixelYmin, pixelXmin, xlen, ylen);			

		rasterUnit.commit();
		messageSink.log("tiles written: " + cnt);
	}
}
