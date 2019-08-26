package remotetask.rasterdb;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import rasterdb.RasterDB;
import remotetask.CancelableRemoteTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;

@task_rasterdb("rebuild")
@Description("create new RasterDB (possibly with other storage type) form source RasterDB and with name [source]_rebuild")
@Param(name="rasterdb", type="rasterdb", desc="ID of RasterDB layer (source)")
@Param(name="storage_type", desc="storage type of new RasterDB RasterUnit (default) or TileStorage", required=false)
public class Task_rebuild extends CancelableRemoteTask {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	private final RasterDB src;
	private final String storage_type;

	public Task_rebuild(Context ctx) {
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("rasterdb");
		src = broker.getRasterdb(name);
		src.check(ctx.userIdentity);
		EmptyACL.ADMIN.check(ctx.userIdentity);
		storage_type = task.optString("storage_type", "RasterUnit");
	}

	@Override
	public void process() throws IOException {
		setMessage("prepare");
		Rebuild rebuild = new Rebuild(src, broker.getRasterDBRoot(), storage_type);
		setMessage("build");
		rebuild.run();
		setMessage("done");
	}


}
