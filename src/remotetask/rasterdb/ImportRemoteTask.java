package remotetask.rasterdb;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broker.Broker;
import broker.acl.EmptyACL;
import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.tile.ProcessingFloat;
import rasterdb.tile.ProcessingShort;
import rasterdb.tile.TileFloat;
import rasterdb.tile.TilePixel;
import rasterunit.RasterUnit;
import rasterunit.RasterUnitStorage;
import remotetask.RemoteTask;
import server.api.main.APIHandler_import.BandSpec;
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
			rasterdb.rebuildPyramid();
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
			short[][] dataShort = null;
			if(bandSpec.no_data_value == null) {
				switch(bandSpec.gdal_raster_data_type) {
				case GdalReader.GDAL_FLOAT64:
				case GdalReader.GDAL_FLOAT32:
				case GdalReader.GDAL_BYTE:
				case GdalReader.GDAL_UINT16:
				case GdalReader.GDAL_INT16: {
					if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_FLOAT64) {
						log.warn("convert float64 raster data to int16");
					}
					if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_FLOAT32) {
						log.warn("convert float32 raster data to int16");
					}
					if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_UINT16) {
						log.warn("convert uint16 raster data to int16");
					}					
					dataShort = gdalreader.getDataShort(bandSpec.file_band_index, null, yoff, ysize);					
					break;
				}
				default:
					throw new RuntimeException("gdal data type not implemented for band type TYPE_SHORT " + bandSpec.gdal_raster_data_type);				
				}				
			} else {
				short bandNaShort = rasterdbBand.getShortNA();
				switch(bandSpec.gdal_raster_data_type) {
				case GdalReader.GDAL_BYTE:
				case GdalReader.GDAL_UINT16:
				case GdalReader.GDAL_INT16: {
					if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_UINT16) {
						log.warn("convert uint16 raster data to int16");
					}
					dataShort = gdalreader.getDataShort(bandSpec.file_band_index, null, yoff, ysize);
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
					float gdalNaFloat = bandSpec.no_data_value.floatValue();
					short gdalNaFloatToShort = (short) gdalNaFloat;
					if(gdalNaFloat == ((float)gdalNaFloatToShort)) { // exact conversion
						dataShort = gdalreader.getDataShort(bandSpec.file_band_index, null, yoff, ysize);
						if(gdalNaFloatToShort != bandNaShort) {
							log.info("convert no data value " + gdalNaFloatToShort + " to " + bandNaShort);
							ProcessingShort.convertNA(dataShort, gdalNaFloatToShort, bandNaShort);
						}
					} else {
						log.info("convert no data value " + gdalNaFloat + " to " + bandNaShort);
						float[][] dataFloat = gdalreader.getDataFloat(bandSpec.file_band_index, null, gdalNaFloat, yoff, ysize);
						dataShort = TileFloat.floatToShort(dataFloat, null, bandNaShort);
					}
					break;
				}
				default:
					throw new RuntimeException("gdal data type not implemented for band type TYPE_SHORT " + bandSpec.gdal_raster_data_type);				
				}					
			}			
			dataShort = Util.flipRows(dataShort);
			int cnt = ProcessingShort.writeMerge(rasterUnit, timestamp, rasterdbBand, dataShort, pixelYmin, pixelXmin);
			rasterUnit.commit();
			break;
		}
		case TilePixel.TYPE_FLOAT: {
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
				break;
			}
			default:
				throw new RuntimeException("gdal data type not implemented for band type TYPE_FLOAT " + bandSpec.gdal_raster_data_type);				
			}
			break;
		}
		default:
			throw new RuntimeException("RasterDB band data type not implemented " + bandSpec.rastedb_band_data_type);			
		}
	}

	/*private static void importBand(RasterDB rasterdb, GdalReader gdalreader, BandSpec bandSpec, int pixelXmin, int pixelYmin, int timestamp) throws IOException {
		org.gdal.gdal.Band gdalRasterBand = gdalreader.dataset.GetRasterBand(bandSpec.file_band_index);
		Band rasterdbBand = rasterdb.getBandByNumber(bandSpec.rasterdb_band_index);
		if(rasterdbBand.type != bandSpec.rastedb_band_data_type) {
			throw new RuntimeException("wrong rastedb_band_data_type");
		}
		int gdalRasterDataType = gdalRasterBand.GetRasterDataType();
		if(gdalRasterDataType != bandSpec.gdal_raster_data_type) {
			throw new RuntimeException("wrong rasterdb_band_data_type");
		}
		RasterUnit rasterUnit = rasterdb.rasterUnit();
		switch(bandSpec.rastedb_band_data_type) {
		case TilePixel.TYPE_SHORT: {
			short[][] dataShort = null;
			if(bandSpec.no_data_value == null) {
				switch(bandSpec.gdal_raster_data_type) {
				case GdalReader.GDAL_FLOAT64:
				case GdalReader.GDAL_FLOAT32:
				case GdalReader.GDAL_BYTE:
				case GdalReader.GDAL_UINT16:
				case GdalReader.GDAL_INT16: {
					if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_FLOAT64) {
						log.warn("convert float64 raster data to int16");
					}
					if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_FLOAT32) {
						log.warn("convert float32 raster data to int16");
					}
					if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_UINT16) {
						log.warn("convert uint16 raster data to int16");
					}					
					dataShort = gdalreader.getDataShort(bandSpec.file_band_index, null);					
					break;
				}
				default:
					throw new RuntimeException("gdal data type not implemented for band type TYPE_SHORT " + bandSpec.gdal_raster_data_type);				
				}				
			} else {
				short bandNaShort = rasterdbBand.getShortNA();
				switch(bandSpec.gdal_raster_data_type) {
				case GdalReader.GDAL_BYTE:
				case GdalReader.GDAL_UINT16:
				case GdalReader.GDAL_INT16: {
					if(bandSpec.gdal_raster_data_type == GdalReader.GDAL_UINT16) {
						log.warn("convert uint16 raster data to int16");
					}
					dataShort = gdalreader.getDataShort(bandSpec.file_band_index, null);
					short gdalNaShort = bandSpec.no_data_value.shortValue();
					if(gdalNaShort != bandNaShort) {
						log.info("convert no data value " + gdalNaShort + " to " + bandNaShort);
						ProcessingShort.convertNA(dataShort, gdalNaShort, bandNaShort);
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
					float gdalNaFloat = bandSpec.no_data_value.floatValue();
					short gdalNaFloatToShort = (short) gdalNaFloat;
					if(gdalNaFloat == ((float)gdalNaFloatToShort)) { // exact conversion
						dataShort = gdalreader.getDataShort(bandSpec.file_band_index, null);
						if(gdalNaFloatToShort != bandNaShort) {
							log.info("convert no data value " + gdalNaFloatToShort + " to " + bandNaShort);
							ProcessingShort.convertNA(dataShort, gdalNaFloatToShort, bandNaShort);
						}
					} else {
						log.info("convert no data value " + gdalNaFloat + " to " + bandNaShort);
						float[][] dataFloat = gdalreader.getDataFloat(bandSpec.file_band_index, null, gdalNaFloat);
						dataShort = TileFloat.floatToShort(dataFloat, null, bandNaShort);
					}
					break;
				}
				default:
					throw new RuntimeException("gdal data type not implemented for band type TYPE_SHORT " + bandSpec.gdal_raster_data_type);				
				}					
			}			
			dataShort = Util.flipRows(dataShort);
			int cnt = ProcessingShort.writeMerge(rasterUnit, timestamp, rasterdbBand, dataShort, pixelYmin, pixelXmin);
			rasterUnit.commit();
			break;
		}
		case TilePixel.TYPE_FLOAT: {
			switch(bandSpec.gdal_raster_data_type) {
			case GdalReader.GDAL_FLOAT64:
				log.warn("convert float64 raster data to float32");
			case GdalReader.GDAL_FLOAT32:
			case GdalReader.GDAL_BYTE:
			case GdalReader.GDAL_UINT16:
			case GdalReader.GDAL_INT16: {
				float[][] dataFloat;
				if(bandSpec.no_data_value == null) {
					dataFloat = gdalreader.getDataFloat(bandSpec.file_band_index, null);					
				} else {
					dataFloat = gdalreader.getDataFloat(bandSpec.file_band_index, null, bandSpec.no_data_value.floatValue());
				}
				dataFloat = Util.flipRows(dataFloat);
				int cnt = ProcessingFloat.writeMerge(rasterUnit, timestamp, rasterdbBand, dataFloat, pixelYmin, pixelXmin);
				rasterUnit.commit();
				break;
			}
			default:
				throw new RuntimeException("gdal data type not implemented for band type TYPE_FLOAT " + bandSpec.gdal_raster_data_type);				
			}
			break;
		}
		default:
			throw new RuntimeException("RasterDB band data type not implemented " + bandSpec.rastedb_band_data_type);			
		}
	}*/
}
