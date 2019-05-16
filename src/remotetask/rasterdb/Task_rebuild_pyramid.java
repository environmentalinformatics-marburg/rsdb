package remotetask.rasterdb;

import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONObject;

import broker.Broker;
import rasterdb.RasterDB;
import remotetask.RemoteTask;

@task_rasterdb("rebuild_pyramid")
public class Task_rebuild_pyramid extends RemoteTask {
	//private static final Logger log = LogManager.getLogger();
	
	private final Broker broker;
	private final JSONObject task;
	
	public Task_rebuild_pyramid(Broker broker, JSONObject task, UserIdentity userIdentity) {
		this.broker = broker;
		this.task = task;
	}

	@Override
	public void process() {
		String name = task.getString("rasterdb");
		RasterDB rasterdb =  broker.getRasterdb(name);
		rasterdb.rebuildPyramid();		
	}

}
