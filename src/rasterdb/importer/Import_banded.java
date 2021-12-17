package rasterdb.importer;

import java.nio.file.Path;
import java.time.LocalDateTime;


import org.tinylog.Logger;

import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.tile.TilePixel;
import run.ModisPreprocess;
import util.TimeUtil;
import util.Timer;
import util.Util;

public class Import_banded {
	

	private final RasterDB rasterdb;
	//private final RasterUnit rasterUnit;
	private final RasterDBimporter rasterdbimporter;

	public Import_banded(RasterDB rasterdb) {
		this.rasterdb = rasterdb;
		//this.rasterUnit = rasterdb.rasterUnit();
		this.rasterdbimporter = new RasterDBimporter(rasterdb);
	}

	public void importDirectoryRecursive(Path root) throws Exception {
		Timer.start("import "+root);
		Path[] paths = null;
		if(root.toFile().isFile()) {
			paths = new Path[]{root};
		} else {
			paths = Util.getPaths(root);
			for(Path path:paths) {
				if(path.toFile().isDirectory()) {
					importDirectoryRecursive(path);
				}
			}
		}
		for(Path path:paths) {
			if(path.toFile().isFile()) {				
				try {
					String filename = path.getFileName().toString().toLowerCase();
					String ext = filename.substring(filename.lastIndexOf('.')+1);
					if(ext.equals("tif")) {
						Logger.info("import file "+path);					
						importFile(path);
					} else {
						//Logger.info("skip file "+path);	
					}
				} catch(Exception e) {
					e.printStackTrace();
					Logger.error(e);
				}

			}
		}
		Logger.info(Timer.stop("import "+root));
	}

	private void importFile(Path path) {
		try {
			String filename = path.getFileName().toString();
			String title = filename.substring(0, filename.lastIndexOf('.'));
			int timestamp = 0;
			try {
				String parent = path.getParent().getFileName().toString();
				String dateTimeText = parent.substring(0, 17);
				Logger.info("dateTimeText "+dateTimeText);
				LocalDateTime datetime = LocalDateTime.parse(dateTimeText, ModisPreprocess.DATE_TIME_FORMATER2);
				timestamp = TimeUtil.toTimestamp(datetime);
			} catch(Exception e) {
				Logger.warn(e);
			}
			Band band = rasterdb.getBandByTitle(title);
			if(band == null) {
				band = rasterdb.createBand(TilePixel.TYPE_SHORT, title, null);
			}
			boolean useExistingBands = false; // don't load band
			rasterdbimporter.importFile_GDAL(path, band, useExistingBands, timestamp);	
		} catch(Exception e) {
			Logger.error(e);
		}
	}

}
