package remotetask;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONObject;

import broker.Broker;

public class RemoteTaskExecutor {
	private static final Logger log = LogManager.getLogger();

	private final static Map<Long, RemoteTask> executingTaskMap = new ConcurrentHashMap<>();
	
	public static RemoteTask createTask_rasterdb(String name, Broker broker, JSONObject task, UserIdentity userIdentity) {
		Constructor<? extends RemoteTask> constructor = RemoteTasks.task_rasterdbMap.get(name);
		if(constructor == null) {
			throw new RuntimeException("rasterdb_task not found: " + name);
		}
		try {
			RemoteTask remoteTask = constructor.newInstance(broker, task, userIdentity);
			return remoteTask;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static RemoteTask createTask_pointdb(String name, Broker broker, JSONObject task, UserIdentity userIdentity) {
		Constructor<? extends RemoteTask> constructor = RemoteTasks.task_pointdbMap.get(name);
		if(constructor == null) {
			throw new RuntimeException("pointdb_task not found: " + name);
		}
		try {
			RemoteTask remoteTask = constructor.newInstance(broker, task, userIdentity);
			return remoteTask;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static RemoteTask createTask_pointcloud(String name, Broker broker, JSONObject task, UserIdentity userIdentity) {
		Constructor<? extends RemoteTask> constructor = RemoteTasks.task_pointcloudMap.get(name);
		if(constructor == null) {
			throw new RuntimeException("pointcloud_task not found: " + name);
		}
		try {
			RemoteTask remoteTask = constructor.newInstance(broker, task, userIdentity);
			return remoteTask;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public static RemoteTask createTask_vectordb(String name, Broker broker, JSONObject task, UserIdentity userIdentity) {
		Constructor<? extends RemoteTask> constructor = RemoteTasks.task_vectordbMap.get(name);
		if(constructor == null) {
			throw new RuntimeException("vectordb_task not found: " + name);
		}
		try {
			RemoteTask remoteTask = constructor.newInstance(broker, task, userIdentity);
			return remoteTask;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public static RemoteTask createTask(Broker broker, JSONObject task, UserIdentity userIdentity) {
		String task_rasterdb = task.optString("task_rasterdb", null);
		String task_pointdb = task.optString("task_pointdb", null);
		String task_pointcloud = task.optString("task_pointcloud", null);
		String task_vectordb = task.optString("task_vectordb", null);
		if(task_rasterdb != null && task_pointdb == null && task_pointcloud == null && task_vectordb == null) {
			return createTask_rasterdb(task_rasterdb, broker,  task, userIdentity);
		} else if(task_rasterdb == null && task_pointdb != null && task_pointcloud == null && task_vectordb == null) {
			return createTask_pointdb(task_pointdb, broker,  task, userIdentity);
		} else if(task_rasterdb == null && task_pointdb == null && task_pointcloud != null && task_vectordb == null) {
			return createTask_pointcloud(task_pointcloud, broker,  task, userIdentity);
		} else if(task_rasterdb == null && task_pointdb == null && task_pointcloud == null && task_vectordb != null) {
			return createTask_vectordb(task_vectordb, broker, task, userIdentity);
		} else {
			throw new RuntimeException("unknown task type: " + task);
		}
	}

	public static void insertToExecute(RemoteTask remoteTask) {
		executingTaskMap.put(remoteTask.id, remoteTask);
		ForkJoinPool.commonPool().execute(remoteTask);
	}

	public static RemoteTask getTaskByID(long id) {
		return executingTaskMap.get(id);		
	}
}
