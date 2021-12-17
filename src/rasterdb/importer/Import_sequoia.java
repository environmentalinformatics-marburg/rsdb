package rasterdb.importer;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;


import org.tinylog.Logger;

import broker.Broker;
import broker.acl.ACL;
import rasterdb.RasterDB;
import remotetask.rasterdb.BandSpec;
import remotetask.rasterdb.ImportBySpec;
import remotetask.rasterdb.ImportProcessor;
import remotetask.rasterdb.ImportSpec;
import server.api.main.APIHandler_inspect;
import server.api.main.APIHandler_inspect.Strategy;
import util.TimeUtil;
import util.Timer;
import util.Util;

public class Import_sequoia {
	

	private final Broker broker;
	private final String namePrefix;
	private final String corresponding_contact;

	public Import_sequoia(Broker broker, String namePrefix, String corresponding_contact) {
		this.broker = broker;
		this.namePrefix = namePrefix;
		this.corresponding_contact = corresponding_contact;
	}

	public void importDirectory(Path root) throws Exception {
		Timer.start("import_sequoia "+root);		
		importDirectoryInternal(root, root.getFileName().toString());
		Logger.info(Timer.stop("import_sequoia "+root));
	}

	private void importDirectoryInternal(Path root, String dataName) throws Exception {
		for(Path path:Util.getPaths(root)) {
			if(path.toFile().isFile()) {
				if(path.getFileName().toString().endsWith(".tif")) {
					Logger.info("import sequoia "+path);
					importFile(path);
				}
			} else if(path.toFile().isDirectory()) {
				importDirectoryInternal(path, path.getFileName().toString());
			}
		}
	}

	private static final DateTimeFormatter UNDERSCORE_DATE = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.DAY_OF_MONTH, 2)
			.appendLiteral('_')
			.appendValue(ChronoField.MONTH_OF_YEAR, 2)
			.appendLiteral('_')
			.appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)            
			.toFormatter();

	public void importFile(Path path) throws Exception {
		String filename = path.getFileName().toString();
		if(!filename.contains("sequoia_reflectance")) {
			Logger.info("skip filename "+filename);
			return;
		}
		Logger.info("filename "+filename);
		int layerNameIndex = filename.indexOf('_');
		String layerSubName = filename.substring(0, layerNameIndex);
		int dateIndex = filename.lastIndexOf('_');
		String dateText = filename.substring(layerNameIndex + 1, dateIndex);
		dateIndex = dateText.lastIndexOf('_');
		dateText = dateText.substring(0, dateIndex);
		Logger.info("dateText " + dateText);
		LocalDate date = LocalDate.parse(dateText, UNDERSCORE_DATE);
		LocalDateTime datetime = LocalDateTime.of(date, LocalTime.MIDNIGHT);
		Logger.info("datetime " + datetime);
		int timestamp = TimeUtil.toTimestamp(datetime);
		String dateName = datetime.getYear() + "_" + (datetime.getMonthValue() <= 9 ? "0" : "") + datetime.getMonthValue() + "_" + (datetime.getDayOfMonth() <= 9 ? "0" : "") + datetime.getDayOfMonth();
		String layerName = namePrefix + '_' + layerSubName + "_" + dateName;
		Logger.info("layerName "+layerName);

		Strategy strategy = Strategy.CREATE;
		RasterDB rasterdb = null;
		if(this.broker.hasRasterdb(layerName)) {
			rasterdb = this.broker.getRasterdb(layerName);
			strategy = Strategy.EXISTING_MERGE;
		}

		ImportSpec spec = APIHandler_inspect.createSpec(path, strategy, filename, layerName, rasterdb, false, null, null);
		spec.storage_type = "TileStorage";
		spec.generalTimestamp = timestamp;
		//spec.inf.title = "uav sequoia " + layerSubName;
		spec.inf.description = "UAV sequoia reflectance values at " + layerSubName;
		spec.inf.acquisition_date = "(time series)";
		spec.inf.corresponding_contact = corresponding_contact;
		spec.inf.setTags("exploratories", "UAV", "sequoia", "reflectance", layerSubName);
		spec.acl = ACL.of("be", "exploratories_cartography");
		Logger.info(spec.bandSpecs);

		switch(spec.bandSpecs.size()) {
		case 4: {
			BandSpec band1 = spec.bandSpecs.get(0);
			band1.band_name = "Green";
			band1.visualisation = "blue";
			band1.wavelength = 550;
			band1.fwhm = 40;

			BandSpec band2 = spec.bandSpecs.get(1);
			band2.band_name = "Red";
			band2.visualisation = "";
			band2.wavelength = 660;
			band2.fwhm = 40;

			BandSpec band3 = spec.bandSpecs.get(2);
			band3.band_name = "RedEdge";
			band3.visualisation = "green";
			band3.wavelength = 735;
			band3.fwhm = 10;

			BandSpec band4 = spec.bandSpecs.get(3);
			band4.band_name = "NIR";
			band4.visualisation = "red";
			band4.wavelength = 790;
			band4.fwhm = 40;
			break;
		}
		case 3: { // special case with missing band 4
			BandSpec band1 = spec.bandSpecs.get(0);
			band1.band_name = "Green";
			band1.visualisation = "blue";
			band1.wavelength = 550;
			band1.fwhm = 40;

			BandSpec band2 = spec.bandSpecs.get(1);
			band2.band_name = "Red";
			band2.visualisation = "green";
			band2.wavelength = 660;
			band2.fwhm = 40;

			BandSpec band3 = spec.bandSpecs.get(2);
			band3.band_name = "RedEdge";
			band3.visualisation = "red";
			band3.wavelength = 735;
			band3.fwhm = 10;
			break;
		}
		default: {
			throw new RuntimeException("unknown bands");
		}
		}

		ImportProcessor importProcessor = ImportBySpec.importPerpare(broker, path, layerName, spec);
		importProcessor.process();
	}
}
