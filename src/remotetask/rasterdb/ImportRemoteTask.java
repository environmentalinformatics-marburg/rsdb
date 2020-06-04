package remotetask.rasterdb;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broker.Broker;
import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.cell.CellInt16;
import rasterdb.cell.CellType;
import rasterdb.tile.ProcessingFloat;
import rasterdb.tile.ProcessingShort;
import rasterdb.tile.TileFloat;
import rasterdb.tile.TilePixel;
import rasterunit.RasterUnitStorage;
import remotetask.RemoteTask;
import util.Range2d;
import util.Util;
import util.collections.vec.Vec;
import util.raster.GdalReader;

public class ImportRemoteTask extends RemoteTask {
	private static final Logger log = LogManager.getLogger();

	private Broker broker;
	private RasterDB rasterdb;
	private GdalReader gdalreader;
	private Vec<BandSpec> bandSpecs;
	private final double file_pixel_size_y;
	private final int generealTimestamp;
	private final boolean update_pyramid;
	private final boolean update_catalog;

	public ImportRemoteTask(Broker broker, RasterDB rasterdb, GdalReader gdalreader, Vec<BandSpec> bandSpecs, double file_pixel_size_y, boolean update_pyramid, boolean update_catalog, int generalTimestamp) {
		this.broker = broker;
		this.rasterdb = rasterdb;
		this.gdalreader = gdalreader;
		this.bandSpecs = bandSpecs;
		this.file_pixel_size_y = file_pixel_size_y;
		this.update_pyramid = update_pyramid;
		this.update_catalog = update_catalog;
		this.generealTimestamp = generalTimestamp;
	}

	@Override
	protected void close() {
		broker = null;
		rasterdb = null;
		gdalreader = null;
		bandSpecs = null;
	}

	@Override
	protected void process() throws IOException {
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
				int importBandTimestamp = bandSpec.timestamp == -1 ? generealTimestamp : bandSpec.timestamp;
				log.info("pixelYmin "+ pixelYmin);
				int yoff = 0;
				while(yoff < yRange) {
					int ysize = yoff + maxLines <= yRange ? maxLines : yRange - yoff;			
					log.info("full size "+ yRange + "  batch from " + yoff + " to " + (yoff + (ysize -1)) + " size " + ysize);
					int yEnd = yoff + (ysize - 1);
					int yEndOffset = (yRange - 1) - yEnd;
					int batchPixelYmin = pixelYmin + yEndOffset; 
					//int batchPixelYmin = pixelYmin + ( (yStart + yRange - 1) - (yStart + yBatchSize - 1) );
					log.info("batchPixelYmin "+ batchPixelYmin);
					if(yoff > 0) {
						setMessage("importing band " + bandSpec.file_band_index + " (" + ((yoff*100) / yRange) + "%)");
					}
					importBand(rasterdb, gdalreader, bandSpec, pixelXmin, batchPixelYmin, importBandTimestamp, yoff, ysize);
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
		
		setMessage("calculate extent");
		Range2d localRange = rasterdb.getLocalRange(true);
		log.info("new local range " + localRange);

		if(update_pyramid) {
			setMessage("rebuild pyramid");
			rasterdb.rebuildPyramid(true);
		}

		if(update_catalog) {
			setMessage("update catalog");
			broker.catalog.updateCatalog();
		}
		setMessage("import done");
	}

	private static void importBand(RasterDB rasterdb, GdalReader gdalreader, BandSpec bandSpec, int pixelXmin, int pixelYmin, int timestamp, int yoff, int ysize) throws IOException {
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
			importBand_TYPE_SHORT(bandSpec, gdalreader, rasterUnit, timestamp, rasterdbBand, pixelXmin, pixelYmin, yoff, ysize, false);
			break;
		}
		case TilePixel.TYPE_FLOAT: {
			importBand_TYPE_FLOAT(bandSpec, gdalreader, rasterUnit, timestamp, rasterdbBand, pixelXmin, pixelYmin, yoff, ysize);
			break;
		}
		case CellType.INT16: {
			importBand_TYPE_SHORT(bandSpec, gdalreader, rasterUnit, timestamp, rasterdbBand, pixelXmin, pixelYmin, yoff, ysize, true);
			break;
		}
		default:
			throw new RuntimeException("RasterDB band data type not implemented " + bandSpec.rastedb_band_data_type);			
		}
	}
	
	private static void importBand_TYPE_SHORT(BandSpec bandSpec, GdalReader gdalreader, RasterUnitStorage rasterUnit, int timestamp, Band rasterdbBand, int pixelXmin, int pixelYmin, int yoff, int ysize, boolean isCellInt16) throws IOException {
		short[][] dataShort = null;		
		switch(bandSpec.gdal_raster_data_type) {
		case GdalReader.GDAL_BYTE:
		case GdalReader.GDAL_UINT16:
		case GdalReader.GDAL_INT16: {
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_UINT16) {
				log.warn("convert uint16 raster data to int16");
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
					log.info("convert file no data value float "+bandSpec.no_data_value + " ==>   short " + gdalNaShort + " to band short " + bandNaShort);
					ProcessingShort.convertNA(dataShort, gdalNaShort, bandNaShort);
				}				
			}			
			break;
		}
		case GdalReader.GDAL_FLOAT64:
		case GdalReader.GDAL_FLOAT32: {
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_FLOAT64) {
				log.warn("convert float64 raster data to int16");
			}
			if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_FLOAT32) {
				log.warn("convert float32 raster data to int16");
			}			
			float[][] dataFloat = gdalreader.getDataFloat(bandSpec.file_band_index, null, yoff, ysize);
			short bandNaShort = rasterdbBand.getInt16NA();
			if(bandSpec.no_data_value == null) {				
				dataShort = TileFloat.floatToShort(dataFloat, null, bandNaShort);						
			} else {
				float gdalNaFloat = bandSpec.no_data_value.floatValue();
				log.info("convert no data value " + gdalNaFloat + " to " + bandNaShort);
				dataShort = TileFloat.floatToShort(dataFloat, null, gdalNaFloat, bandNaShort);
			}			
			break;
		}
		default:
			throw new RuntimeException("gdal data type not implemented for band type TYPE_SHORT " + bandSpec.gdal_raster_data_type);				
		}			
		dataShort = Util.flipRows(dataShort);
		int cnt;
		if(isCellInt16) {
			CellInt16 cellInt16 = new CellInt16(rasterUnit.getTilePixelLen());
			cnt = cellInt16.writeMerge(rasterUnit, timestamp, rasterdbBand, dataShort, pixelYmin, pixelXmin);			
		} else {
			cnt = ProcessingShort.writeMerge(rasterUnit, timestamp, rasterdbBand, dataShort, pixelYmin, pixelXmin);
		}		
		rasterUnit.commit();
		log.info("tiles written: " + cnt);
	}
	
	private static void importBand_TYPE_FLOAT(BandSpec bandSpec, GdalReader gdalreader, RasterUnitStorage rasterUnit, int timestamp, Band rasterdbBand, int pixelXmin, int pixelYmin, int yoff, int ysize) throws IOException {
		switch(bandSpec.gdal_raster_data_type) {
		case GdalReader.GDAL_FLOAT64:
			log.warn("convert float64 raster data to float32");
		case GdalReader.GDAL_FLOAT32:
		case GdalReader.GDAL_BYTE:
		case GdalReader.GDAL_UINT16:
		case GdalReader.GDAL_INT16: {
			float[][] dataFloat;
			if(bandSpec.no_data_value == null) {
				dataFloat = gdalreader.getDataFloat(bandSpec.file_band_index, null, yoff, ysize);					
			} else {
				dataFloat = gdalreader.getDataFloat(bandSpec.file_band_index, null, bandSpec.no_data_value.floatValue(), yoff, ysize);
			}
			dataFloat = Util.flipRows(dataFloat);
			int cnt = ProcessingFloat.writeMerge(rasterUnit, timestamp, rasterdbBand, dataFloat, pixelYmin, pixelXmin);
			rasterUnit.commit();
			log.info("tiles written: " + cnt);
			break;
		}
		default:
			throw new RuntimeException("gdal data type not implemented for band type TYPE_FLOAT " + bandSpec.gdal_raster_data_type);				
		}
	}


}
