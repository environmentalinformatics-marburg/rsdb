package remotetask.rasterdb;

import java.io.IOException;
import java.nio.file.Paths;


import org.tinylog.Logger;
import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.importer.RasterDBimporter;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;

@task_rasterdb("import_OLD")
@Description("Import raster file data into rasterdb layer.")
@Param(name="rasterdb", type="layer_id", desc="ID of RasterDB layer. (new or existing)", example="rasterdb1")
@Param(name="file", format="path", desc="Raster file to import. (located on server)", example="data/raster.tiff")
@Param(name="band", type="integer", desc="Existing band number as import target.", example="1", required=false)
public class Task_import_OLD extends RemoteTask {
	

	private final Broker broker;
	private final JSONObject task;

	public Task_import_OLD(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		EmptyACL.ADMIN.check(ctx.userIdentity);
	}

	@Override
	public void process() {
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
			Logger.error(e);
		}
	}
}
