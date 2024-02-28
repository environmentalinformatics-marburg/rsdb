package remotetask.rsdb;

import org.json.JSONObject;

import broker.Broker;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;

@task_rsdb("refresh")
@Description("Refresh RSDB layers list an catalog on the server for (manually) added or removed layers. After this you need to refresh (or reload) the web interface to be up to date.")
@Param(name="rebuild_catalog", type="boolean", desc="Delete layer catalog and recreate all catalog entries. This may take some time. (default: false)", example="true", required=false)

public class Task_refresh extends RemoteTask {

	private final JSONObject task;

	public Task_refresh(Context ctx) {
		super(ctx);	
		this.task = ctx.task;
	}

	@Override
	protected void process() throws Exception {
		boolean rebuild_catalog = false;
		if(task.has("rebuild_catalog")) {
			rebuild_catalog = task.getBoolean("rebuild_catalog");
		}
		
		setMessage("start refresh");
		Broker broker = ctx.broker;
		
		broker.refreshAllLayers(this);		
		
		if(rebuild_catalog) {
			setMessage("rebuild Catalog entries");
			broker.catalog.rebuildCatalog(this);
		} else {
			setMessage("refresh Catalog entries");
			broker.catalog.refreshCatalog();
		}		
		
		setMessage("finish refresh");
	}
}
