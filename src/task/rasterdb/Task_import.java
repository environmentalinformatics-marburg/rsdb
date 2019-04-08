package task.rasterdb;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import broker.Broker;
import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.RasterDBimporter;

public class Task_import {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;

	public Task_import(Broker broker, JSONObject task) {
		this.broker = broker;
		this.task = task;
	}

	public void run() {
		String name = task.getString("rasterdb");
		RasterDB rasterdb =  broker.createOrGetRasterdb(name);
		String filename = task.getString("file");

		Band band = null;
		if(task.has("band")) {
			int band_number = task.getInt("band");
			band = rasterdb.getBandByNumber(band_number);
			if(band == null) {
				throw new RuntimeException("band number not found "+band_number);
			}
		}

		RasterDBimporter importer = new RasterDBimporter(rasterdb);

		try {
			importer.importFile_GDAL(Paths.get(filename), band, false, 0);
		} catch (IOException e) {
			log.error(e);
		}
	}
}
