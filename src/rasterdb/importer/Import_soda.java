package rasterdb.importer;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONWriter;

import broker.Broker;
import broker.acl.ACL;
import remotetask.rasterdb.BandSpec;
import remotetask.rasterdb.ImportBySpec;
import remotetask.rasterdb.ImportProcessor;
import remotetask.rasterdb.ImportSpec;
import server.api.main.APIHandler_inspect;
import server.api.main.APIHandler_inspect.Strategy;
import util.TimeUtil;
import util.Timer;
import util.Util;

public class Import_soda {
	private static final Logger log = LogManager.getLogger();
	
	private final Broker broker;
	private final String namePrefix;
	private final String corresponding_contact;
	
	public Import_soda(Broker broker, String namePrefix, String corresponding_contact) {
		this.broker = broker;
		this.namePrefix = namePrefix;
		this.corresponding_contact = corresponding_contact;
	}
	
	public void importDirectory(Path root) throws Exception {
		Timer.start("import_soda "+root);		
		importDirectoryInternal(root, root.getFileName().toString());
		log.info(Timer.stop("import_soda "+root));
	}
	
	private void importDirectoryInternal(Path root, String dataName) throws Exception {
		for(Path path:Util.getPaths(root)) {
			if(path.toFile().isFile()) {
				if(path.getFileName().toString().endsWith(".tif")) {
					log.info("import soda "+path);
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
		if(!filename.contains("soda")) {
			log.info("skip filename "+filename);
			return;
		}
		log.info("filename "+filename);
		int layerNameIndex = filename.indexOf('_');
		String layerSubName = filename.substring(0, layerNameIndex);
		int dateIndex = filename.lastIndexOf('_');
		String dateText = filename.substring(layerNameIndex + 1, dateIndex);		
		log.info("dateText " + dateText);
		LocalDate date = LocalDate.parse(dateText, UNDERSCORE_DATE);
		LocalDateTime datetime = LocalDateTime.of(date, LocalTime.MIDNIGHT);
		log.info("datetime " + datetime);
		int timestamp = TimeUtil.toTimestamp(datetime);
		String layerName = namePrefix + '_' + layerSubName;
		log.info("layerName "+layerName);
		
		StringBuilder sb = new StringBuilder();
		APIHandler_inspect.createJSONspec(path, Strategy.CREATE, filename, layerName, null, false, new JSONWriter(sb), null);
		String s = sb.toString();
		JSONObject json = new JSONObject(s);
		log.info(json.toString(1));
		ImportSpec spec = new ImportSpec();
		spec.parse(json);
		
		spec.storage_type = "TileStorage";
		spec.generalTimestamp = timestamp;
		spec.inf.title = "uav soda " + layerSubName;
		spec.inf.description = "UAV soda at " + layerSubName;
		spec.inf.acquisition_date = "(time series planned)";
		spec.inf.corresponding_contact = corresponding_contact;
		spec.inf.setTags("exploratories", "UAV", "soda", layerSubName);
		spec.acl = ACL.of("be");
		log.info(spec.bandSpecs);
		BandSpec band1 = spec.bandSpecs.get(0);
		band1.band_name = "Red";
		band1.visualisation = "red";
		band1.no_data_value = 0d;
		
		BandSpec band2 = spec.bandSpecs.get(1);
		band2.band_name = "Green";
		band2.visualisation = "green";
		band2.no_data_value = 0d;
		
		BandSpec band3 = spec.bandSpecs.get(2);
		band3.band_name = "Blue";
		band3.visualisation = "blue";
		band3.no_data_value = 0d;
		
		ImportProcessor importProcessor = ImportBySpec.importPerpare(broker, path, layerName, spec);
		importProcessor.process();		
	}
}
