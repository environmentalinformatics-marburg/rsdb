package remotetask.pointcloud;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import pointcloud.Importer;
import pointcloud.PointCloud;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;

@task_pointcloud("import")
@Description("import directory of files into new PointCloud layer")
@Param(name="pointcloud", desc="ID of new PointCloud layer (target)")
@Param(name="source", desc="folder with *.las / *.laz files to import (located on server) (recursive)")
@Param(name="transactions", desc="use power failer safe (and slow) PointCloud operation mode (default false)", required=false)
@Param(name="cellsize", desc="cell size (default: 100 -> 100 meter)", required=false)
@Param(name="cellscale", desc="cell size (default: 100 -> resolution of points 0.01 meter)", required=false)
@Param(name="storage_type", desc="RasterUnit (default) or TileStorage", required=false)
public class Task_import extends RemoteTask {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;

	public Task_import(Context ctx) {
		this.broker = ctx.broker;
		this.task = ctx.task;
		EmptyACL.ADMIN.check(ctx.userIdentity);
	}

	@Override
	public void process() throws IOException {		
		String name = task.getString("pointcloud");
		String storage_type = task.optString("storage_type", "RasterUnit");
		boolean transactions = task.optBoolean("transactions", false);
		PointCloud pointcloud = broker.createNewPointCloud(name, storage_type, transactions);
		double cellsize = task.optDouble("cellsize", 100);
		pointcloud.trySetCellsize(cellsize);
		double cellscale = task.optDouble("cellscale", 100);
		pointcloud.trySetCellscale(cellscale);
		Importer imprter = new Importer(pointcloud);
		String source = task.getString("source");
		Path root = Paths.get(source);
		setMessage("start import");
		imprter.importDirectory(root);
	}
}
