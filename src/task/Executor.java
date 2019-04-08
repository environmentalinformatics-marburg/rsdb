package task;

import org.json.JSONArray;
import org.json.JSONObject;

import broker.Broker;
import task.pointcloud.PointcloudExecutor;
import task.pointdb.PointdbExecutor;
import task.rasterdb.RasterdbExecutor;

public class Executor {

	public static void run(Broker broker, JSONArray tasks) {
		int len = tasks.length();
		for (int i = 0; i < len; i++) {
			run(broker, tasks.getJSONObject(i));
		}
	}

	public static void run(Broker broker, JSONObject task) {
		String task_rasterdb = task.optString("task_rasterdb", null);
		String task_pointdb = task.optString("task_pointdb", null);
		String task_pointcloud = task.optString("task_pointcloud", null);
		if(task_rasterdb != null && task_pointdb == null && task_pointcloud == null) {
			RasterdbExecutor.run(broker, task_rasterdb, task);
		} else if(task_rasterdb == null && task_pointdb != null && task_pointcloud == null) {
			PointdbExecutor.run(broker, task_pointdb, task);
		} else if(task_rasterdb == null && task_pointdb == null && task_pointcloud != null) {
			PointcloudExecutor.run(broker, task_pointcloud, task);
		} else {
			throw new RuntimeException("unknown task type: " + task);
		}
	}
}
