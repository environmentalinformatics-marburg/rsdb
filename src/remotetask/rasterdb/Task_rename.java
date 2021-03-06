package remotetask.rasterdb;

import java.io.IOException;

import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import rasterdb.RasterDB;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteProxyTask;

@task_rasterdb("rename")
@Description("Renames an existing RasterDB layer to a not yet existing RasterDB layer ID")
@Param(name="rasterdb", type="rasterdb", desc="ID of RasterDB layer to rename.", example="raster1")
@Param(name="new_name", type="layer_id", desc="ID of new RasterDB layer.", example="rasterA")
public class Task_rename extends RemoteProxyTask {
	//private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	private final RasterDB src;
	private final String dst;

	public Task_rename(Context ctx) {
		EmptyACL.ADMIN.check(ctx.userIdentity);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String rasterdb = task.getString("rasterdb");
		this.src = broker.getRasterdb(rasterdb);
		src.check(ctx.userIdentity);
		this.dst = task.getString("new_name");
	}

	@Override
	public void process() throws IOException {
		setMessage("rename RasterDB '" + src.config.getName() + "' to '" + dst + "'");
		broker.renameRasterdb(src.config.getName(), dst);
		setMessage("done, renamed RasterDB '" + src.config.getName() + "' to '" + dst + "'");
	}
}
