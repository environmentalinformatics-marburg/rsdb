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
@Param(name="pointcloud", type="layer_id", desc="ID of new PointCloud layer (target)", example="pointcloud1")
@Param(name="source", format="path", desc="folder with *.las / *.laz files to import (located on server) (recursive)", example="las/folder1")
@Param(name="transactions", type="boolean", desc="use power failer safe (and slow) PointCloud operation mode (default false) (obsolete for TileStorage)", example="false", required=false)
@Param(name="cellsize", type="number", desc="size of cells (default: 100 -> 100 meter)", example="10", required=false)
@Param(name="cellscale", type="number", desc="resolution of points (default: 100 -> resolution of points 1/100 = 0.01 meter)", example="1000", required=false)
@Param(name="storage_type", desc="storage type of new PointCloud: RasterUnit (default) or TileStorage", format="RasterUnit or TileStorage", example="TileStorage", required=false)
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
		pointcloud.getGriddb().getStorage().flush();
		setMessage("finished import");
	}
}
