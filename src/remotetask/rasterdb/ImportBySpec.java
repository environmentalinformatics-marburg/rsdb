package remotetask.rasterdb;

import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broker.Broker;
import rasterdb.Band;
import rasterdb.RasterDB;
import util.raster.GdalReader;

public class ImportBySpec {
	private static final Logger log = LogManager.getLogger();
	
	public static ImportRemoteTask importPerpare(Broker broker, Path path, String id, ImportSpec spec) {
		if(broker == null) {
			throw new RuntimeException("no broker");
		}
		if(path == null) {
			throw new RuntimeException("no path");
		}
		if(id == null) {
			throw new RuntimeException("no id");
		}
		if(spec == null) {
			throw new RuntimeException("no spec");
		}

		if(!Double.isFinite(spec.pixel_size_x)) {
			throw new RuntimeException("no pixel_size_x");
		}
		if(!Double.isFinite(spec.pixel_size_y)) {
			throw new RuntimeException("no pixel_size_y");
		}
		if(!Double.isFinite(spec.rasterdb_geo_offset_x)) {
			throw new RuntimeException("no rasterdb_geo_offset_x");
		}
		if(!Double.isFinite(spec.rasterdb_geo_offset_y)) {
			throw new RuntimeException("no rasterdb_geo_offset_y");
		}
		if(spec.geo_code == null) {
			throw new RuntimeException("no geo_code");
		}
		if(spec.proj4 == null) {
			throw new RuntimeException("no proj4");
		}
		if(spec.bandSpecs == null) {
			throw new RuntimeException("no bandSpecs");
		}		

		log.info(path);
		GdalReader gdalreader = new GdalReader(path.toString());		

		RasterDB rasterdb;
		if(spec.strategy.isCreate()) {
			rasterdb = broker.createNewRasterdb(id, true, spec.storage_type);
			rasterdb.setPixelSize(spec.pixel_size_x, spec.pixel_size_y, spec.rasterdb_geo_offset_x, spec.rasterdb_geo_offset_y);
			rasterdb.setCode(spec.geo_code);
			rasterdb.setProj4(spec.proj4);

			rasterdb.setInformal(spec.inf.build());

			rasterdb.setACL(spec.acl);
			rasterdb.setACL_mod(spec.acl_mod);
		} else {
			rasterdb = broker.getRasterdb(id);
		}

		if(spec.addBands != null) {
			for(BandSpec bandSpec:spec.addBands) {
				if(!rasterdb.bandMap.containsKey(bandSpec.rasterdb_band_index)) {
					Band band = Band.ofSpectralBand(bandSpec.rastedb_band_data_type, bandSpec.rasterdb_band_index, bandSpec.wavelength, bandSpec.fwhm, bandSpec.band_name, bandSpec.visualisation);
					rasterdb.setBand(band);
				}
			}
		}

		if(spec.bandSpecs != null) {
			for(BandSpec bandSpec:spec.bandSpecs) {
				if(bandSpec.import_band) {
					if(!rasterdb.bandMap.containsKey(bandSpec.rasterdb_band_index)) {
						Band band = Band.ofSpectralBand(bandSpec.rastedb_band_data_type, bandSpec.rasterdb_band_index, bandSpec.wavelength, bandSpec.fwhm, bandSpec.band_name, bandSpec.visualisation);
						rasterdb.setBand(band);
					}
					Band refBand = rasterdb.bandMap.get(bandSpec.rasterdb_band_index); // correct potential user spec errors
					bandSpec.band_name = refBand.title;
					bandSpec.rastedb_band_data_type = refBand.type;
					bandSpec.wavelength = refBand.wavelength;
					bandSpec.fwhm = refBand.fwhm;
					bandSpec.visualisation = refBand.visualisation;
				}
			}
		}

		ImportRemoteTask importRemoteTask = new ImportRemoteTask(broker, rasterdb, gdalreader, spec.bandSpecs, spec.pixel_size_y, spec.update_pyramid, spec.update_catalog, spec.generalTimestamp);
		return importRemoteTask;
	}
}