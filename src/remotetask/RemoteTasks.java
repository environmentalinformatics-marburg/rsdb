package remotetask;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONObject;

import broker.Broker;
import remotetask.pointcloud.Task_rasterize;
import remotetask.pointcloud.task_pointcloud;
import remotetask.pointdb.Task_index_raster;
import remotetask.pointdb.Task_to_pointcloud;
import remotetask.pointdb.task_pointdb;
import remotetask.rasterdb.RemoveTimestampsRemoteTask;
import remotetask.rasterdb.Task_count_pixels;
import remotetask.rasterdb.Task_create;
import remotetask.rasterdb.Task_create_band;
import remotetask.rasterdb.Task_import;
import remotetask.rasterdb.Task_rebuild_pyramid;
import remotetask.rasterdb.Task_refresh_extent;
import remotetask.rasterdb.task_rasterdb;
import remotetask.vectordb.RefreshCatalogEntryRemoteTask;
import remotetask.vectordb.task_vectordb;

public class RemoteTasks {
	private static final Logger log = LogManager.getLogger();

	final static Map<String, Constructor<? extends RemoteTask>> task_rasterdbMap = new ConcurrentHashMap<>();
	final static Map<String, Constructor<? extends RemoteTask>> task_pointdbMap = new ConcurrentHashMap<>();
	final static Map<String, Constructor<? extends RemoteTask>> task_pointcloudMap = new ConcurrentHashMap<>();
	final static Map<String, Constructor<? extends RemoteTask>> task_vectordbMap = new ConcurrentHashMap<>();

	static {
		//task_rasterdb
		put(RemoveTimestampsRemoteTask.class);
		put(Task_create.class);
		put(Task_create_band.class);
		put(Task_import.class);
		put(Task_rebuild_pyramid.class);
		put(Task_count_pixels.class);
		put(Task_refresh_extent.class);

		//task_pointdb
		put(Task_index_raster.class);
		put(Task_to_pointcloud.class);
		
		//task_pointcloud
		put(Task_rasterize.class);

		//task_vectordb
		put(RefreshCatalogEntryRemoteTask.class);
	}

	private static <T extends RemoteTask> void put(Class<T> clazz) {
		boolean inserted = false;
		if(clazz.isAnnotationPresent(task_rasterdb.class)) {
			try {
				String name = clazz.getAnnotation(task_rasterdb.class).value();
				Class<?>[] constructorArgs = new Class[] {Broker.class, JSONObject.class, UserIdentity.class};
				Constructor<T> constructor = clazz.getDeclaredConstructor(constructorArgs);
				if(task_rasterdbMap.containsKey(name)) {
					log.warn("task with name already inserted, overwrite: " + name);
				}
				task_rasterdbMap.put(name, constructor);
				inserted = true;
			} catch(Exception e) {
				log.error("could not put task: "+ clazz.getName() + e);
			}
		}

		if(clazz.isAnnotationPresent(task_pointdb.class)) {
			try {
				String name = clazz.getAnnotation(task_pointdb.class).value();
				Class<?>[] constructorArgs = new Class[] {Broker.class, JSONObject.class, UserIdentity.class};
				Constructor<T> constructor = clazz.getDeclaredConstructor(constructorArgs);
				if(task_pointdbMap.containsKey(name)) {
					log.warn("task with name already inserted, overwrite: " + name);
				}
				task_pointdbMap.put(name, constructor);
				inserted = true;
			} catch(Exception e) {
				log.error("could not put task: "+ clazz.getName() + e);
			}
		}
		
		if(clazz.isAnnotationPresent(task_pointcloud.class)) {
			try {
				String name = clazz.getAnnotation(task_pointcloud.class).value();
				Class<?>[] constructorArgs = new Class[] {Broker.class, JSONObject.class, UserIdentity.class};
				Constructor<T> constructor = clazz.getDeclaredConstructor(constructorArgs);
				if(task_pointcloudMap.containsKey(name)) {
					log.warn("task with name already inserted, overwrite: " + name);
				}
				task_pointcloudMap.put(name, constructor);
				inserted = true;
			} catch(Exception e) {
				log.error("could not put task: "+ clazz.getName() + e);
			}
		}

		if(clazz.isAnnotationPresent(task_vectordb.class)) {
			try {
				String name = clazz.getAnnotation(task_vectordb.class).value();
				Class<?>[] constructorArgs = new Class[] {Broker.class, JSONObject.class, UserIdentity.class};
				Constructor<T> constructor = clazz.getDeclaredConstructor(constructorArgs);
				if(task_vectordbMap.containsKey(name)) {
					log.warn("task with name already inserted, overwrite: " + name);
				}
				task_vectordbMap.put(name, constructor);
				inserted = true;
			} catch(Exception e) {
				log.error("could not put task: "+ clazz.getName() + e);
			}
		}

		if(!inserted) {
			log.warn("could not put task: "+ clazz.getName());
		}
	}
	
	private static TreeMap<String, Constructor<? extends RemoteTask>> collectAsTreeMap(Map<String, Constructor<? extends RemoteTask>> map) {
		TreeMap<String, Constructor<? extends RemoteTask>> m = new TreeMap<>();
		m.putAll(map);
		return m;
	}
	
	public static Map<String, TreeMap<String, Constructor<? extends RemoteTask>>> list() {
		LinkedHashMap<String,TreeMap<String, Constructor<? extends RemoteTask>>> map = new LinkedHashMap<>();
		map.put("task_rasterdb", collectAsTreeMap(task_rasterdbMap));
		map.put("task_pointdb", collectAsTreeMap(task_pointdbMap));
		map.put("task_pointcloud", collectAsTreeMap(task_pointcloudMap));
		map.put("task_vectordb", collectAsTreeMap(task_vectordbMap));
		return map;
	}
}
