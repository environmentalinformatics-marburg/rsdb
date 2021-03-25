package rasterdb.importer;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broker.Broker;
import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.tile.TilePixel;
import util.TimeUtil;
import util.Timer;
import util.Util;

public class Import_rapideye {
	private static final Logger log = LogManager.getLogger();

	//private final Broker broker;
	private final RasterDB rasterdb;
	private final RasterDBimporter rasterdbimporter;
	
	private static final String[] bandTitles = new String[]{
			"Blue",
			"Green",
			"Red",
			"Red Edge",
			"Near IR",
	};

	private static final String[] bandVisualisations = new String[]{
			"blue",
			"green",
			"red",
			null,
			null,
	};

	private static final double[] bandWvmin = new double[]{
			440,
			520,
			630,
			690,
			760,
	};

	private static final double[] bandWvmax = new double[]{	
			510,
			590,
			685,
			730,
			850,
	};


	public Import_rapideye(Broker broker, String name) {
		//this.broker = broker;
		this.rasterdb = broker.getRasterdb(name);
		this.rasterdbimporter = new RasterDBimporter(rasterdb);
	}
	
	public void importDirectory(Path root) throws Exception {
		Timer.start("import_rapideye "+root);
		
		for (int i = 0; i < 5; i++) {
			double wvmin = bandWvmin[i];
			double wvmax = bandWvmax[i];
			Band band = Band.ofSpectralBand(TilePixel.TYPE_SHORT, i + 1, (wvmin + wvmax) / 2, (wvmax - wvmin) / 2, bandTitles[i], bandVisualisations[i]);
			rasterdb.setBand(band, true);
		}		
		
		importDirectoryInternal(root, root.getFileName().toString());
		log.info(Timer.stop("import_rapideye "+root));
		rasterdb.rebuildPyramid(true);
	}
	
	private void importDirectoryInternal(Path root, String dataName) throws Exception {
		for(Path path:Util.getPaths(root)) {
			if(path.toFile().isFile()) {
				if(path.getFileName().toString().equals(dataName+".tif")) {
					log.info("import rapideye "+path);
					importFile(path);				}
			} else if(path.toFile().isDirectory()) {
				importDirectoryInternal(path, path.getFileName().toString());
			}
		}
	}
	
	
	public void importFile(Path path) throws Exception {
		String filename = path.getFileName().toString();
		int dtStart = filename.indexOf('_') + 1;
		String dtText = filename.substring(dtStart, dtStart + 10);
		log.info("dtText "+dtText);
		LocalDate date = LocalDate.parse(dtText, DateTimeFormatter.ISO_LOCAL_DATE);
		LocalDateTime datetime = LocalDateTime.of(date, LocalTime.MIDNIGHT);
		int timestamp = TimeUtil.toTimestamp(datetime);
		log.info("dtText "+dtText+"  timestamp "+timestamp);
		rasterdbimporter.importFile_GDAL(path, null, true, timestamp);
	}
	

	/*public void importDirectory2(Path root) throws Exception {
		Timer.start("import_rapideye"+root);
		TreeMap<String, Path> fileMap = new TreeMap<String, Path>();
		for(Path path:Util.getPaths(root)) {
			if(path.toFile().isFile()) {
				try {
					String filename = path.getFileName().toString().toLowerCase();
					String ext = filename.substring(filename.lastIndexOf('.')+1);
					if(ext.equals("tif")) {
						//log.info("import file "+path);
						String title = filename.substring(0, filename.lastIndexOf('.'));
						log.info("title "+title);
						String bandTitle = title.substring(title.lastIndexOf('_')+1);
						log.info("bandTitle "+bandTitle);
						if(fileMap.containsKey(bandTitle)) {
							log.warn("band already inserted. overwrite");
						}
						fileMap.put(bandTitle, path);

						//rasterdbimporter.importFile_GDAL(filename);


						//importFile(path.toString());
					} else {
						//log.info("skip file "+path);	
					}
				} catch(Exception e) {
					e.printStackTrace();
					log.error(e);
				}
			}
		}

		for(int i=0;i<bandIndices.length;i++) {
			try {
				String bandTitle = bandIDs[i];
				int bandIndex = bandIndices[i];
				Path filename = fileMap.get(bandTitle);
				double wvmin = bandWvmin[i] * 1000d;
				double wvmax = bandWvmax[i] * 1000d;

				Band band = Band.ofSpectralBand(TilePixel.TYPE_SHORT, bandIndex, (wvmin + wvmax) / 2, (wvmax - wvmin) / 2, bandTitles[i], bandVisualisations[i]);
				switch(bandRes[i]) {
				case 30:
					rasterdb_30m.setBand(band);
					rasterdbimporter_30m.importFile_GDAL(filename.toString(), band);
					break;
				case 15:
					rasterdb_15m.setBand(band);
					rasterdbimporter_15m.importFile_GDAL(filename.toString(), band);
					break;
				default:
					throw new RuntimeException();
				}
			} catch(Exception e) {
				log.warn(e);
			}
		}

		log.info(Timer.stop("import_rapideye"+root));
		rasterdb.rebuildPyramid();
	}*/
}
