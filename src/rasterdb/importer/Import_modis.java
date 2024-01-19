package rasterdb.importer;

import java.nio.file.Path;


import org.tinylog.Logger;

import broker.Broker;
import rasterdb.RasterDB;
import remotetask.MessageSink;
import util.Timer;
import util.Util;

public class Import_modis {
	

	//private final Broker broker;
	private final RasterDB rasterdb;
	private final RasterDBimporter rasterdbimporter;

	public Import_modis(Broker broker, String name) {
		//this.broker = broker;
		this.rasterdb = broker.createOrGetRasterdb(name);
		this.rasterdbimporter = new RasterDBimporter(rasterdb);
	}
	
	public void importDirectory(Path root) throws Exception {
		Timer.start("import_modis "+root);		
		importDirectoryInternal(root, root.getFileName().toString());
		Logger.info(Timer.stop("import_modis "+root));
		rasterdb.rebuildPyramid(true, MessageSink.MESSAGE_SINK_LOG);
	}
	
	private void importDirectoryInternal(Path root, String dataName) throws Exception {
		for(Path path:Util.getPaths(root)) {
			if(path.toFile().isFile()) {
				if(path.getFileName().toString().endsWith(".tif")) {
					Logger.info("import modis "+path);
					importFile(path);
				}
			} else if(path.toFile().isDirectory()) {
				importDirectoryInternal(path, path.getFileName().toString());
			}
		}
	}
	
	
	public void importFile(Path path) throws Exception {
		rasterdbimporter.importFile_GDAL(path, null, false, 0);
	}
}
