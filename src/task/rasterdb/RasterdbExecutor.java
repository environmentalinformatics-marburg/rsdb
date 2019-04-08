package task.rasterdb;

import org.json.JSONObject;

import broker.Broker;

public class RasterdbExecutor {


	public static void run(Broker broker, String taskName, JSONObject args) {
		switch(taskName) {
		case "create":
			new Task_create(broker, args).run();
			break;
		case "create_band":
			new Task_create_band(broker, args).run();
			break;
		case "import":
			new Task_import(broker, args).run();
			break;
		case "rebuild_pyramid":
			new Task_rebuild_pyramid(broker, args).run();
			break;
		case "count_pixels":
			new Task_count_pixels(broker, args).run();
			break;
		case "refresh_extent":
			new Task_refresh_extent(broker, args).run();
			break;			
		default:
			throw new RuntimeException("unknown RasterDB task: " + taskName);
		}	
	}

}
