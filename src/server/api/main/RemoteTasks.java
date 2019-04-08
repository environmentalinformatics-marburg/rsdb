package server.api.main;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONObject;

import broker.Broker;

public class RemoteTasks {
	
	private final static Map<Long, RemoteTask> map = new ConcurrentHashMap<>();
	
	public static RemoteTask parseTask(Broker broker, JSONObject args, UserIdentity userIdentity) {
		String task_rasterdb = args.optString("task_rasterdb", null);
		String task_pointdb = args.optString("task_pointdb", null);
		String task_pointcloud = args.optString("task_pointcloud", null);
		String task_vectordb = args.optString("task_vectordb", null);
		if(task_rasterdb != null && task_pointdb == null && task_pointcloud == null && task_vectordb == null) {
			return parseRasterdbTask(broker, task_rasterdb, args, userIdentity);
		} else if(task_rasterdb == null && task_pointdb != null && task_pointcloud == null && task_vectordb == null) {
			throw new RuntimeException("not implemented");
		} else if(task_rasterdb == null && task_pointdb == null && task_pointcloud != null && task_vectordb == null) {
			throw new RuntimeException("not implemented");
		} else if(task_rasterdb == null && task_pointdb == null && task_pointcloud == null && task_vectordb != null) {
			return parseVectordbTask(broker, task_vectordb, args, userIdentity);
		} else {
			throw new RuntimeException("unknown task type: " + args);
		}
	}
	
	public static RemoteTask parseRasterdbTask(Broker broker, String taskName, JSONObject args, UserIdentity userIdentity) {
		switch(taskName) {
		case "remove_timestamps":
			return new RemoveTimestampsRemoteTask(broker, args, userIdentity);
		default:
			throw new RuntimeException("unknown RasterDB task: " + taskName);
		}	
	}
	
	public static RemoteTask parseVectordbTask(Broker broker, String taskName, JSONObject args, UserIdentity userIdentity) {
		switch(taskName) {
		case "refreh_catalog_entry":
			return new RefreshCatalogEntryRemoteTask(broker, args, userIdentity);
		default:
			throw new RuntimeException("unknown RasterDB task: " + taskName);
		}	
	}
	
	public static void insert(RemoteTask remoteTask) {
		map.put(remoteTask.id, remoteTask);
		ForkJoinPool.commonPool().execute(remoteTask);
	}

	public static RemoteTask get(long id) {
		return map.get(id);		
	}

}
