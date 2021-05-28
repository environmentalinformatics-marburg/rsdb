package remotetask.pointcloud;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import pointcloud.PointCloud;
import pointcloud.Rebuild;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;

@task_pointcloud("rebuild")
@Description("Create new pointcloud (without fragmented free space, with ordered cells, with other storage type and/or with recompressed data) form source pointcloud and with name [source]_rebuild.")
@Param(name="pointcloud", type="pointcloud", desc="ID of PointCloud layer. (source)", example="pointcloud1")
@Param(name="compression_level", type="integer", desc="Level of compression. (0 to 100) If missing no recompression is applied.", example="1", required=false)
@Param(name="storage_type", desc="Storage type of new PointCloud. (default: TileStorage)", format="RasterUnit or TileStorage", example="TileStorage", required=false)
public class Task_rebuild extends RemoteTask {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	private final PointCloud src;
	private final String storage_type;
	private final int compression_level;

	public Task_rebuild(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("pointcloud");
		src = broker.getPointCloud(name);
		src.check(ctx.userIdentity);
		EmptyACL.ADMIN.check(ctx.userIdentity);
		storage_type = task.optString("storage_type", "TileStorage");
		log.info("set storage type: " + storage_type);
		compression_level = task.optInt("compression_level", Integer.MIN_VALUE);
	}

	@Override
	public void process() throws IOException {
		setMessage("prepare");
		Rebuild rebuild = new Rebuild(src, broker.getPointCloudRoot(), storage_type, compression_level != Integer.MIN_VALUE, compression_level);
		setMessage("rebuild");
		rebuild.run();
		setMessage("done");
		broker.refreshPointcloudConfigs();
		broker.catalog.updateCatalog();
	}


}
