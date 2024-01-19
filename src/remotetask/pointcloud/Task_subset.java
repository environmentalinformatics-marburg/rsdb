package remotetask.pointcloud;

import org.json.JSONArray;
import org.json.JSONObject;

import broker.Broker;
import pointcloud.PointCloud;
import pointcloud.Subset;
import remotetask.CancelableRemoteProxyTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;

@task_pointcloud("subset")
@Description("Create new pointcloud from a subset of an existing pointlcoud.")
@Param(name="pointcloud", type="pointcloud", desc="ID of PointCloud layer. (source)", example="pointcloud1")
@Param(name="rect", type="number_rect", desc="Only pointcloud cells overlapping the rect are included.", format="list of coordinates: xmin, ymin, xmax, ymax", example="609000.1, 5530100.7, 609094.1, 5530200.9")
public class Task_subset extends CancelableRemoteProxyTask {

	private final Broker broker;
	private final JSONObject task;
	private PointCloud src;

	public Task_subset(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("pointcloud");
		src = broker.getPointCloud(name);
		src.check(ctx.userIdentity, "task pointcloud subset");
	}

	public void process() throws Exception {
		int t = 0;

		JSONArray rect_Text = task.getJSONArray("rect");
		double xmin = rect_Text.getDouble(0);
		double ymin = rect_Text.getDouble(1);
		double xmax = rect_Text.getDouble(2);
		double ymax = rect_Text.getDouble(3);
		
		setMessage("prepare of " + xmin + ", " + ymin +" to "+ xmax + ", " + ymax);
		Subset subset = new Subset(src, broker.getPointCloudRoot(), ctx.userIdentity, t, xmin, ymin, xmax, ymax);
		setMessage("subset");
		setRemoteProxyAndRunAndClose(subset);
		setMessage("done");
		broker.refreshPointcloudConfigs();
		broker.catalog.refreshCatalog();		
	}
}
