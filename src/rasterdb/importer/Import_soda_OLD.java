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

import broker.Broker;
import broker.Informal.Builder;
import rasterdb.RasterDB;
import util.TimeUtil;
import util.Timer;
import util.Util;

public class Import_soda_OLD {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final String namePrefix;
	//private final RasterDB rasterdb;
	//private final RasterDBimporter rasterdbimporter;

	public Import_soda_OLD(Broker broker, String namePrefix) {
		this.broker = broker;
		this.namePrefix = namePrefix;
		//this.rasterdb = broker.createOrGetRasterdb(name);
		//this.rasterdbimporter = new RasterDBimporter(rasterdb);
	}
	
	public void importDirectory(Path root) throws Exception {
		Timer.start("import_soda "+root);		
		importDirectoryInternal(root, root.getFileName().toString());
		log.info(Timer.stop("import_soda "+root));
		//rasterdb.rebuildPyramid();
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
             .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
             .appendLiteral('_')
             .appendValue(ChronoField.MONTH_OF_YEAR, 2)
             .appendLiteral('_')
             .appendValue(ChronoField.DAY_OF_MONTH, 2)
             .toFormatter();
	
	
	public void importFile(Path path) throws Exception {
		String filename = path.getFileName().toString();
		log.info("filename "+filename);
		int layerNameIndex = filename.indexOf('_');
		String layerSubName = filename.substring(0, layerNameIndex);
		log.info("layerName "+layerSubName);
		int dateIndex = filename.lastIndexOf('_');
		String dateText = filename.substring(layerNameIndex + 1, dateIndex);
		log.info("dateText " + dateText);
		LocalDate date = LocalDate.parse(dateText, UNDERSCORE_DATE);
		LocalDateTime datetime = LocalDateTime.of(date, LocalTime.MIDNIGHT);
		log.info("datetime " + datetime);
		int timestamp = TimeUtil.toTimestamp(datetime);
		String layerName = namePrefix + '_' + layerSubName;
		
		RasterDB rasterdb = broker.createOrGetRasterdb(layerName, false);
		RasterDBimporter rasterdbimporter = new RasterDBimporter(rasterdb);
		rasterdbimporter.importFile_GDAL(path, null, true, timestamp);
		rasterdb.rebuildPyramid(true);
		Builder informal = rasterdb.informal().toBuilder();
		informal.setTags(namePrefix);
		rasterdb.setInformal(informal.build());
		rasterdb.close();
	}
}
