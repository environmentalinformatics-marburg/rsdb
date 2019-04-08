package task.pointcloud;

import org.json.JSONObject;

import broker.Broker;
import pointcloud.PointCloud;
import rasterdb.RasterDB;

public class Task_rasterize {
	//private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;

	public Task_rasterize(Broker broker, JSONObject task) {
		this.broker = broker;
		this.task = task;
	}

	public void run() {
		String name = task.getString("pointcloud");
		PointCloud pointcloud = broker.getPointCloud(name);
		String rasterdb_name = task.getString("rasterdb");
		boolean transactions = true;
		if(task.has("transactions")) {
			transactions = task.getBoolean("transactions");
		}
		RasterDB rasterdb = broker.createRasterdb(rasterdb_name, transactions);		
		pointcloud.Rasterizer rasterizer = new pointcloud.Rasterizer(pointcloud, rasterdb);
		rasterizer.run();
		rasterdb.commit();
		rasterdb.rebuildPyramid();
	}


}
