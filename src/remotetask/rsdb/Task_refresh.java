package remotetask.rsdb;

import broker.Broker;
import remotetask.Context;
import remotetask.Description;
import remotetask.RemoteTask;

@task_rsdb("refresh")
@Description("Refresh RSDB layers list an catalog on the server for (manually) added or removed layers. After this you need to refresh (or reload) the web interface to be up to date.")
public class Task_refresh extends RemoteTask {

	public Task_refresh(Context ctx) {
		super(ctx);	
	}

	@Override
	protected void process() throws Exception {
		setMessage("start refresh");
		Broker broker = ctx.broker;
		
		setMessage("refresh RasterDB layers");
		broker.refreshRasterdbConfigs();
		
		setMessage("refresh Pointcloud layers");
		broker.refreshPointcloudConfigs();
		
		setMessage("refresh VoxelDB layers");
		broker.refreshVoxeldbConfigs();
		
		setMessage("refresh VectorDB layers");
		broker.refreshVectordbConfigs();
		
		setMessage("refresh PostGIS layers");
		broker.postgisLayerManager().refresh();
		
		setMessage("refresh PoiGroup layers");
		broker.refreshPoiGroupMap();
		
		setMessage("refresh RoiGroup layers");
		broker.refreshRoiGroupMap();
		
		setMessage("refresh Catalog entries");
		broker.catalog.refreshCatalog();
		
		setMessage("finish refresh");
	}
}
