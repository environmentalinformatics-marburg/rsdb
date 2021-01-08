package remotetask.pointcloud;

import java.io.IOException;

import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import pointcloud.PointCloud;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteProxyTask;

@task_pointcloud("rename")
@Description("Renames an existing Pointcloud layer to a not yet existing Pointcloud layer ID")
@Param(name="pointcloud", type="pointcloud", desc="ID of Pointcloud layer to rename.", example="pointcloud1")
@Param(name="new_name", type="layer_id", desc="ID of new Pointcloud layer.", example="pointcloudA")
public class Task_rename extends RemoteProxyTask {
	//private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	private final PointCloud src;
	private final String dst;

	public Task_rename(Context ctx) {
		EmptyACL.ADMIN.check(ctx.userIdentity);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String pointcloud = task.getString("pointcloud");
		this.src = broker.getPointCloud(pointcloud);
		src.check(ctx.userIdentity);
		this.dst = task.getString("new_name");
	}

	@Override
	public void process() throws IOException {
		setMessage("rename Pointcloud '" + src.getName() + "' to '" + dst + "'");
		broker.renamePointcloud(src.getName(), dst);
		setMessage("done, renamed Pointcloud '" + src.getName() + "' to '" + dst + "'");
	}
}
