package task.pointcloud;

import org.json.JSONObject;

import broker.Broker;

public class PointcloudExecutor {

	public static void run(Broker broker, String taskName, JSONObject args) {
		switch(taskName) {
		case "rasterize":
			new Task_rasterize(broker, args).run();
			break;
		default:
			throw new RuntimeException("unknown PointCloud task: " + taskName);
		}	
	}

}
