package remotetask;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RemoteTaskExecutor {
	private static final Logger log = LogManager.getLogger();

	private final static Map<Long, RemoteTask> executingTaskMap = new ConcurrentHashMap<>();
	
	public static RemoteTask createTask_RemoteTask(RemoteTaskInfo rti, Context ctx) {
		try {
			RemoteTask remoteTask = rti.constructor.newInstance(ctx);
			return remoteTask;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if(cause == null) {
				throw new RuntimeException(e);
			} else {
				throw new RuntimeException(cause);
			}
		}
	}
	
	public static RemoteTask createTask_rasterdb(String name, Context ctx) {
		RemoteTaskInfo rti = RemoteTasks.task_rasterdbMap.get(name);
		if(rti == null) {
			throw new RuntimeException("rasterdb_task not found: " + name);
		}
		return createTask_RemoteTask(rti, ctx);
	}
	
	public static RemoteTask createTask_pointdb(String name, Context ctx) {
		RemoteTaskInfo rti = RemoteTasks.task_pointdbMap.get(name);
		if(rti == null) {
			throw new RuntimeException("pointdb_task not found: " + name);
		}
		return createTask_RemoteTask(rti, ctx);
	}
	
	public static RemoteTask createTask_pointcloud(String name, Context ctx) {
		RemoteTaskInfo rti = RemoteTasks.task_pointcloudMap.get(name);
		if(rti == null) {
			throw new RuntimeException("pointcloud_task not found: " + name);
		}
		return createTask_RemoteTask(rti, ctx);
	}
	
	public static RemoteTask createTask_voxeldb(String name, Context ctx) {
		RemoteTaskInfo rti = RemoteTasks.task_voxeldbMap.get(name);
		if(rti == null) {
			throw new RuntimeException("voxeldb_task not found: " + name);
		}
		return createTask_RemoteTask(rti, ctx);
	}

	public static RemoteTask createTask_vectordb(String name, Context ctx) {
		RemoteTaskInfo rti = RemoteTasks.task_vectordbMap.get(name);
		if(rti == null) {
			throw new RuntimeException("vectordb_task not found: " + name);
		}
		return createTask_RemoteTask(rti, ctx);
	}

	public static RemoteTask createTask(Context ctx) {
		String task_rasterdb = ctx.task.optString("task_rasterdb", null);
		String task_pointdb = ctx.task.optString("task_pointdb", null);
		String task_pointcloud = ctx.task.optString("task_pointcloud", null);
		String task_vectordb = ctx.task.optString("task_vectordb", null);
		String task_voxeldb = ctx.task.optString("task_voxeldb", null);
		if(task_rasterdb != null && task_pointdb == null && task_pointcloud == null && task_vectordb == null && task_voxeldb == null) {
			return createTask_rasterdb(task_rasterdb, ctx);
		} else if(task_rasterdb == null && task_pointdb != null && task_pointcloud == null && task_vectordb == null && task_voxeldb == null) {
			return createTask_pointdb(task_pointdb, ctx);
		} else if(task_rasterdb == null && task_pointdb == null && task_pointcloud != null && task_vectordb == null && task_voxeldb == null) {
			return createTask_pointcloud(task_pointcloud, ctx);
		} else if(task_rasterdb == null && task_pointdb == null && task_pointcloud == null && task_vectordb != null && task_voxeldb == null) {
			return createTask_vectordb(task_vectordb, ctx);
		} else if(task_rasterdb == null && task_pointdb == null && task_pointcloud == null && task_vectordb == null && task_voxeldb != null) {
			return createTask_voxeldb(task_voxeldb, ctx);
		} else {
			throw new RuntimeException("unknown task type: " + ctx.task);
		}
	}

	public static RemoteProxyTask insertToExecute(RemoteProxy remoteProxy, Context ctx) {
		RemoteProxyTask remoteProxyTask = RemoteProxyTask.of(remoteProxy, ctx);
		insertToExecute(remoteProxyTask);
		return remoteProxyTask;
	}
	
	public static void insertToExecute(RemoteTask remoteTask) {
		executingTaskMap.put(remoteTask.id, remoteTask);
		ForkJoinPool.commonPool().execute(remoteTask);
	}

	public static RemoteTask getTaskByID(long id) {
		return executingTaskMap.get(id);		
	}
	
	public static List<RemoteTask> getRemoteTasks() {
		return executingTaskMap.values().stream().sorted(RemoteTask.START_TIME_COMPARATOR).collect(Collectors.toList());
	}
}
