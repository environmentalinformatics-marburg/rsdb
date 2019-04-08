package task.pointdb;

import org.json.JSONObject;

import broker.Broker;

public class PointdbExecutor {


	public static void run(Broker broker, String taskName, JSONObject args) {
		switch(taskName) {
		case "index_raster":
			new Task_index_raster(broker, args).run();
			break;
		case "to_pointcloud":
			new Task_to_pointcloud(broker, args).run();
			break;
		default:
			throw new RuntimeException("unknown PointDB task: " + taskName);
		}	
	}

}
