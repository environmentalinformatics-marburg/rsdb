package rasterdb.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rasterdb.Band;
import rasterdb.BandProcessing;
import rasterdb.RasterDB;
import rasterdb.tile.ProcessingFloat;
import rasterdb.tile.ProcessingShort;
import rasterdb.tile.TileFloat;
import rasterdb.tile.TilePixel;
import rasterunit.RasterUnit;
import rasterunit.RasterUnitStorage;
import util.TimeUtil;
import util.Timer;
import util.Util;
import util.raster.GdalReader;
import util.raster.Hyperspectral;

public class RasterDBimporter {
	private static final Logger log = LogManager.getLogger();

	private static HashSet<String> FILE_EXTENSIONS = new HashSet<String>(){{
		add("bsq");
		add("tif");
	}};

	private final RasterDB rasterdb;
	private final RasterUnitStorage rasterUnit;

	public RasterDBimporter(RasterDB rasterdb) {
		this.rasterdb = rasterdb;
		this.rasterUnit = rasterdb.rasterUnit();
	}

	public void importDirectoryRecursive(Path root) throws Exception {
		importDirectoryRecursive(root , -1);
	}

	public void importDirectoryRecursive(Path root, int default_timestamp) throws Exception {
		Timer.start("import "+root);
		Path[] paths = null;
		if(root.toFile().isFile()) {
			paths = new Path[]{root};
		} else {
			paths = Util.getPaths(root);
			for(Path path:paths) {
				if(path.toFile().isDirectory()) {
					importDirectoryRecursive(path, default_timestamp);
				}
			}
		}
		for(Path path:paths) {
			if(path.toFile().isFile()) {				
				try {
					String filename = path.getFileName().toString().toLowerCase();
					String ext = filename.substring(filename.lastIndexOf('.')+1);
					if(FILE_EXTENSIONS.contains(ext)) {
						log.info("import file "+path);					
						importFile(path, default_timestamp);
					} else {
						//log.info("skip file "+path);	
					}
				} catch(Exception e) {
					e.printStackTrace();
					log.error(e);
				}

			}
		}
		log.info(Timer.stop("import "+root));
	}

	public void importFile(Path path, int default_timestamp) throws IOException {
		String filename = path.getFileName().toString();
		String ext = filename.substring(filename.lastIndexOf('.')+1);
		if(ext.equals("bsq")) {
			int t = default_timestamp < 0 ? 0 : default_timestamp;
			importFile_ENVI(path, t);
		} else if(ext.equals("tif")) {
			int timestamp = default_timestamp >= 0 ? default_timestamp : TimeUtil.tryParseTimestamp(filename);
			int t = timestamp < 0 ? 0 : timestamp;
			importFile_GDAL(path, null, true, t);
		} else {
			throw new RuntimeException("unknown file "+path);
		}
	}

	private static Map<String, String[]> mapinfoIDcache = new HashMap<String, String[]>();

	public void importFile_ENVI(Path path, int timestamp) throws IOException {
		Hyperspectral hyperspectral = new Hyperspectral(path.toString());
		double pixel_sizeX = Math.abs(hyperspectral.enviHdr.mapinfo_pixelSizeX);
		double pixel_sizeY = Math.abs(hyperspectral.enviHdr.mapinfo_pixelSizeY);
		log.info("pixel_size "+pixel_sizeX+"  "+pixel_sizeY);

		double easting = hyperspectral.enviHdr.mapinfo_pixelEasting;
		double northing = hyperspectral.enviHdr.mapinfo_pixelNorthing - (hyperspectral.lines * pixel_sizeY);
		log.info("geo "+easting+"  "+northing);
		if(!rasterdb.ref().has_pixel_size()) {
			double rasterdb_geo_offset_x = easting;
			double rasterdb_geo_offset_y = northing;
			rasterdb.setPixelSize(pixel_sizeX, pixel_sizeY, rasterdb_geo_offset_x, rasterdb_geo_offset_y);
		}
		if(pixel_sizeX != rasterdb.ref().pixel_size_x || pixel_sizeY != rasterdb.ref().pixel_size_y) {
			throw new RuntimeException("pixel_size of file need to be same as rasterDB");
		}
		int pixelXmin = rasterdb.ref().geoXToPixel(easting);
		int pixelYmin = rasterdb.ref().geoYToPixel(northing);
		log.info("pixel "+ pixelXmin + "  " + pixelYmin);

		String mapInfoID = "mapinfoID ";
		mapInfoID += "+proj="+hyperspectral.enviHdr.mapinfo_projectionName;
		mapInfoID += " +zone="+hyperspectral.enviHdr.mapinfo_projectionZone;
		mapInfoID += " +"+hyperspectral.enviHdr.mapinfo_northOrSouth;
		mapInfoID += " +units="+hyperspectral.enviHdr.mapinfo_units;
		mapInfoID += " +ellps="+hyperspectral.enviHdr.mapinfo_datum;
		log.info(mapInfoID);
		if(!mapinfoIDcache.containsKey(mapInfoID)) {
			try {
				GdalReader gdalreader = new GdalReader(path.toString());
				String proj4 = gdalreader.getProj4();
				String code = gdalreader.getCRS_code();
				log.info(proj4);
				log.info(code);
				mapinfoIDcache.put(mapInfoID, new String[]{code, proj4});
			} catch(Exception e) {
				log.error(e);
				mapinfoIDcache.put(mapInfoID, new String[0]);
			}
		}

		if(mapinfoIDcache.containsKey(mapInfoID) && mapinfoIDcache.get(mapInfoID).length == 2) {
			String[] refs = mapinfoIDcache.get(mapInfoID);
			/* WRONG code
			if(!rasterdb.ref.has_code()) {
				rasterdb.setCode(refs[0]);				
			}
			if(!refs[0].equals(rasterdb.ref.code)) {
				throw new RuntimeException("code of file need to be same as rasterDB "+rasterdb.ref.code+"  "+refs[0]);
			}
			 */
			if(!rasterdb.ref().has_proj4()) {
				rasterdb.setProj4(refs[1]);
			}
			if(!refs[1].equals(rasterdb.ref().proj4)) {
				throw new RuntimeException("proj4 of file need to be same as rasterDB"+rasterdb.ref().proj4+"  "+refs[1]);
			}
		} else {
			log.warn("no projection reference");
		}

		int bandCount = hyperspectral.getBandCount();
		short[][] targetData = null;
		for(int fileBandIndex = 0; fileBandIndex < bandCount; fileBandIndex++) {
			double wavelength = hyperspectral.enviHdr.wavelength_picometre[fileBandIndex] / 1000d;
			double fwhm = hyperspectral.enviHdr.fwhm_picometre[fileBandIndex] / 1000d;
			Band band = BandProcessing.matchSpectralBand(rasterdb, wavelength, fwhm, 0.01d);
			if(band == null) {
				band = rasterdb.createSpectralBand(TilePixel.TYPE_SHORT, wavelength, fwhm, null, null);
			}			
			targetData = hyperspectral.getData(fileBandIndex, targetData);			
			targetData = Util.flipRows(targetData);
			int cnt = ProcessingShort.writeMerge(rasterUnit, timestamp, band, targetData, pixelYmin, pixelXmin);
			log.info(targetData.length + " x " + targetData[0].length+" b "+fileBandIndex+" tiles "+cnt);

			rasterUnit.commit();

			/*if(fileBandIndex == 0) {
				Frame frame = new Frame(targetData, 0, 0, targetData[0].length-1, targetData.length-1);
				ImageProducerGrey imageProducerGrey = new ImageProducerGrey(frame);
				OutputStream out = new FileOutputStream("c:/pointdb_data/testing_rasterdb/img.png");
				imageProducerGrey.produce(2.5f);
				imageProducerGrey.writePng(out, 0);
				out.close();
			}*/

		}				

		log.info("db tileKey count "+rasterUnit.getTileCount());
	}





	public void importFile_GDAL(Path path, Band existingBand, boolean useExistingBandsOrCreate, int timestamp) throws IOException {
		String filename = path.toString();
		log.info("import "+filename);
		GdalReader gdalreader = new GdalReader(filename);
		log.info("gdalreader.dataset.GetDescription "+gdalreader.dataset.GetDescription());
		//log.info("data type "+gdalreader.dataset.GetRasterBand(1).getDataType());

		String fileCode = null;		
		String wkt = gdalreader.getWKT();
		log.info("|||"+wkt+"|||");
		if(GdalReader.wktMap.containsKey(wkt)) {
			fileCode = GdalReader.wktMap.get(wkt);
		}

		if(fileCode == null) {
			fileCode = gdalreader.getCRS_code();
		}
		log.info("fileCode "+fileCode);

		if(fileCode != null) {
			if(rasterdb.ref().has_code()) {
				if(!rasterdb.ref().code.equals(fileCode)) {
					throw new RuntimeException("ref code diff " + rasterdb.ref().code+ "   " + fileCode );
				}
			} else {
				rasterdb.setCode(fileCode);
				log.info("************************set crs code   "+fileCode);
			}
		} else {
			log.warn("no ref code");
		}

		String fileProj4 = null;
		if(fileCode != null) {
			if(GdalReader.codeMap.containsKey(fileCode)) {
				fileProj4 = GdalReader.codeMap.get(fileCode);
			}
		}
		if(fileProj4 == null) {
			fileProj4 = gdalreader.getProj4();
		}
		if(fileProj4 != null) {
			if(rasterdb.ref().has_proj4()) {
				if(!rasterdb.ref().proj4.equals(fileProj4)) {
					throw new RuntimeException("ref proj4 diff " + rasterdb.ref().proj4+ "   " + fileProj4 );
				}
			} else {
				rasterdb.setProj4(fileProj4);
				log.info("************************set crs proj4   "+fileProj4);
			}
		} else {
			log.warn("no ref proj4");
		}

		double[] gdal_ref = gdalreader.getGeoRef();
		log.info("gdal "+Arrays.toString(gdalreader.dataset.GetGeoTransform()));
		double easting = gdal_ref[0];
		double northing = gdal_ref[1];
		log.info("geo "+easting+"  "+northing);

		double pixel_size_x = gdalreader.getPixelSize_x();
		double pixel_size_y = gdalreader.getPixelSize_y();
		double corrected_northing = northing - (gdalreader.y_range * pixel_size_y);
		if(!rasterdb.ref().has_pixel_size()) {
			double rasterdb_geo_offset_x = easting;
			double rasterdb_geo_offset_y = corrected_northing;
			rasterdb.setPixelSize(pixel_size_x, pixel_size_y, rasterdb_geo_offset_x, rasterdb_geo_offset_y);
			log.info("************************set pixel size   "+pixel_size_x+"  "+pixel_size_y);
		}
		if(pixel_size_x != rasterdb.ref().pixel_size_x || pixel_size_y != rasterdb.ref().pixel_size_y) {
			throw new RuntimeException("pixel_size of file need to be same as rasterDB "+rasterdb.ref().pixel_size_x+"  "+pixel_size_x+"    "+rasterdb.ref().pixel_size_y+"  "+pixel_size_y);
		}
		log.info("pixel_size "+pixel_size_x+"  "+pixel_size_y);

		int pixelXmin = rasterdb.ref().geoXToPixel(easting);
		//int pixelYmin = rasterdb.ref().geoYToPixel(northing) - gdalreader.y_range;
		int pixelYmin = rasterdb.ref().geoYToPixel(corrected_northing);
		log.info("pixel "+ pixelXmin + "  " + pixelYmin + "  " + (pixelXmin + gdalreader.x_range - 1) + "  " + (pixelYmin + gdalreader.y_range - 1) );
		log.info("geo   "+ rasterdb.ref().pixelXToGeo(pixelXmin) + "  " + rasterdb.ref().pixelYToGeo(pixelYmin) + "  " + rasterdb.ref().pixelXToGeo(pixelXmin + gdalreader.x_range -1) + "  " + rasterdb.ref().pixelYToGeo(pixelYmin + gdalreader.y_range - 1) );

		int bandCount = gdalreader.getRasterCount();
		if(existingBand != null) {
			if(bandCount > 1) {
				log.warn("import first band only");
				bandCount = 1;
			}
		}

		short[][] targetDataShort = null;
		float[][] targetDataFloat = null;

		for(int fileBandIndex = 1; fileBandIndex <= bandCount; fileBandIndex++) {
			log.info("fileBandIndex" + fileBandIndex);
			org.gdal.gdal.Band gdalRasterBand = gdalreader.dataset.GetRasterBand(fileBandIndex);
			Double[] noDataValueHolder = new Double[1];
			gdalRasterBand.GetNoDataValue(noDataValueHolder);
			log.info("NA "+Arrays.toString(noDataValueHolder));
			int gdalRasterDataType = gdalRasterBand.GetRasterDataType();
			Band band = existingBand;
			if(band == null && useExistingBandsOrCreate) {
				band = rasterdb.bandMap.get(fileBandIndex);
			}
			if(band == null) {
				int tileDataType = TilePixel.TYPE_SHORT;
				switch(gdalRasterDataType) {
				case GdalReader.GDAL_BYTE:
				case GdalReader.GDAL_UINT16:
				case GdalReader.GDAL_INT16: 
					tileDataType = TilePixel.TYPE_SHORT;
					break;
				case GdalReader.GDAL_FLOAT64:
				case GdalReader.GDAL_FLOAT32:
					tileDataType = TilePixel.TYPE_FLOAT;
					break;
				default:
					tileDataType = TilePixel.TYPE_SHORT;
				}
				band = rasterdb.createBand(tileDataType, null, null);
			}

			switch(band.type) {
			case TilePixel.TYPE_SHORT: {
				switch(gdalRasterDataType) {
				case GdalReader.GDAL_FLOAT64:
				case GdalReader.GDAL_FLOAT32:
					log.warn("convert float32/64 raster data to int16");
				case GdalReader.GDAL_BYTE:
				case GdalReader.GDAL_UINT16:
				case GdalReader.GDAL_INT16: {				
					if(gdalRasterDataType == GdalReader.GDAL_BYTE) {
						targetDataShort = gdalreader.getDataShortOfByte(fileBandIndex, targetDataShort);
						if(noDataValueHolder[0] != null) {
							short gdalNa = noDataValueHolder[0].shortValue();
							short bandNA = band.getShortNA();
							if(gdalNa != bandNA) {
								log.info("convert no data value " + gdalNa +" to " + bandNA);
								ProcessingShort.convertNA(targetDataShort, gdalNa, bandNA);
							}
						}
					} else if(gdalRasterDataType == GdalReader.GDAL_FLOAT32 || gdalRasterDataType == GdalReader.GDAL_FLOAT64) {
						if(noDataValueHolder[0] != null) {
							float noDataValue = noDataValueHolder[0].floatValue();
							targetDataFloat = gdalreader.getDataFloat(fileBandIndex, targetDataFloat, noDataValue);
						} else {
							targetDataFloat = gdalreader.getDataFloat(fileBandIndex, targetDataFloat);
						}
						targetDataShort = TileFloat.floatToShort(targetDataFloat, targetDataShort, (short)0);
					} else {
						targetDataShort = gdalreader.getDataShort(fileBandIndex, targetDataShort);
						if(noDataValueHolder[0] != null) {
							short gdalNa = noDataValueHolder[0].shortValue();
							short bandNA = band.getShortNA();
							if(gdalNa != bandNA) {
								log.info("convert no data value " + gdalNa +" to " + bandNA);
								ProcessingShort.convertNA(targetDataShort, gdalNa, bandNA);
							}
						}
					}
					targetDataShort = Util.flipRows(targetDataShort);
					int cnt = ProcessingShort.writeMerge(rasterUnit, timestamp, band, targetDataShort, pixelYmin, pixelXmin);
					log.info(targetDataShort.length + " x " + targetDataShort[0].length+" b "+fileBandIndex+"->"+band.index+" t "+timestamp+" tiles "+cnt);
					rasterUnit.commit();
					break;
				}
				default:
					throw new RuntimeException("gdal data type not implemented for band type TYPE_SHORT "+gdalRasterDataType);				
				}
				break;
			}
			case TilePixel.TYPE_FLOAT: {
				switch(gdalRasterDataType) {
				case GdalReader.GDAL_FLOAT64:
					log.warn("convert float64 raster data to float32");
				case GdalReader.GDAL_FLOAT32: {
					if(noDataValueHolder[0] != null) {
						float noDataValue = noDataValueHolder[0].floatValue();
						targetDataFloat = gdalreader.getDataFloat(fileBandIndex, targetDataFloat, noDataValue);
					} else {
						targetDataFloat = gdalreader.getDataFloat(fileBandIndex, targetDataFloat);
					}
					targetDataFloat = Util.flipRows(targetDataFloat);
					if(band.type != TilePixel.TYPE_FLOAT) {
						throw new RuntimeException("wrong band data type "+band.type+"  for "+TilePixel.TYPE_FLOAT);
					}
					int cnt = ProcessingFloat.writeMerge(rasterUnit, timestamp, band, targetDataFloat, pixelYmin, pixelXmin);
					log.info(targetDataFloat.length + " x " + targetDataFloat[0].length+" b "+fileBandIndex+" tiles "+cnt);
					rasterUnit.commit();
					break;
				}
				default:
					throw new RuntimeException("gdal data type not implemented for band type TYPE_FLOAT "+gdalRasterDataType);				
				}
				break;
			}
			default:
				throw new RuntimeException("band data type not implemented "+band.type);			
			}
		}
	}
}
