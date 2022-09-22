package remotetask.rasterdb;

import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import rasterdb.RasterDB;
import remotetask.CancelableRemoteProxyTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;

@task_rasterdb("rebuild")
@Description("Create new RasterDB (possibly with other storage type) form source RasterDB and with name [source]_rebuild.")
@Param(name="rasterdb", type="rasterdb", desc="ID of RasterDB layer. (source)", example="rasterdb1")
@Param(name="storage_type", desc="Storage type of new RasterDB. (default: TileStorage)", format="RasterUnit or TileStorage", example="TileStorage", required=false)
@Param(name="pyramid_type", desc="Pyramid type of new RasterDB. (default: compact_div2)", format="files_div4 or compact_div2", example="files_div4", required=false)
public class Task_rebuild extends CancelableRemoteProxyTask {
	//

	private final Broker broker;
	private final JSONObject task;
	private final RasterDB src;
	private final String storage_type;
	private final String pyramid_type; // nullable

	public Task_rebuild(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("rasterdb");
		src = broker.getRasterdb(name);
		src.checkMod(ctx.userIdentity);
		storage_type = task.optString("storage_type", RasterDB.STORAGE_TYPE_TILE_STORAGE);
		pyramid_type = task.optString("pyramid_type", RasterDB.PYRAMID_TYPE_COMPACT_DIV2);
		if(pyramid_type != null && !RasterDB.isValidPyramidTypeString(pyramid_type)) {
			throw new RuntimeException("unknown pyramid_type: " + pyramid_type);
		}
	}

	@Override
	public void process() throws Exception {
		setMessage("prepare");
		Rebuild rebuild = new Rebuild(src, broker.getRasterDBRoot(), storage_type, pyramid_type);
		setMessage("build");
		setRemoteProxyAndRunAndClose(rebuild);
		broker.refreshRasterdbConfigs();
		setMessage("done");
	}
}
