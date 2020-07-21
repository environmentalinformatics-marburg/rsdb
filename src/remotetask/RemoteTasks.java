package remotetask;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import remotetask.pointcloud.Task_rasterize;
import remotetask.pointcloud.task_pointcloud;
import remotetask.pointdb.Task_index_raster;
import remotetask.pointdb.Task_to_pointcloud;
import remotetask.pointdb.Task_verify;
import remotetask.pointdb.task_pointdb;
import remotetask.rasterdb.Task_count_pixels;
import remotetask.rasterdb.Task_create;
import remotetask.rasterdb.Task_create_band;
import remotetask.rasterdb.Task_import;
import remotetask.rasterdb.Task_rebuild;
import remotetask.rasterdb.Task_rebuild_pyramid;
import remotetask.rasterdb.Task_refresh_extent;
import remotetask.rasterdb.Task_remove_bands;
import remotetask.rasterdb.Task_remove_timestamps;
import remotetask.rasterdb.task_rasterdb;
import remotetask.vectordb.RefreshCatalogEntryRemoteTask;
import remotetask.vectordb.task_vectordb;
import remotetask.voxeldb.task_voxeldb;
import util.collections.vec.Vec;

public class RemoteTasks {
	private static final Logger log = LogManager.getLogger();

	private static final Class<?>[] CONSTRUCTOR_ARGS = new Class[] {Context.class};
	
	final static Map<String, RemoteTaskInfo> task_rasterdbMap = new ConcurrentHashMap<>();
	final static Map<String, RemoteTaskInfo> task_pointdbMap = new ConcurrentHashMap<>();
	final static Map<String, RemoteTaskInfo> task_pointcloudMap = new ConcurrentHashMap<>();
	final static Map<String, RemoteTaskInfo> task_voxeldbMap = new ConcurrentHashMap<>();
	final static Map<String, RemoteTaskInfo> task_vectordbMap = new ConcurrentHashMap<>();

	static {
		//task_rasterdb
		put(Task_remove_timestamps.class);
		put(Task_remove_bands.class);
		put(Task_create.class);
		put(Task_create_band.class);
		put(Task_import.class);
		put(Task_rebuild.class);
		put(Task_rebuild_pyramid.class);
		put(Task_count_pixels.class);
		put(Task_refresh_extent.class);		

		//task_pointdb
		put(remotetask.pointdb.Task_import.class);
		put(remotetask.pointdb.Task_rasterize.class);
		put(Task_index_raster.class);
		put(Task_to_pointcloud.class);
		put(Task_verify.class);

		//task_pointcloud
		put(remotetask.pointcloud.Task_import.class);
		put(Task_rasterize.class);
		put(remotetask.pointcloud.Task_verify.class);
		put(remotetask.pointcloud.Task_rebuild.class);
		put(remotetask.pointcloud.Task_index_raster.class);
		put(remotetask.pointcloud.Task_coverage.class);
		put(remotetask.pointcloud.Task_to_voxel.class);
		
		//task_voxeldb
		put(remotetask.voxeldb.Task_voxel_to_pointcloud.class);
		put(remotetask.voxeldb.Task_rasterize.class);

		//task_vectordb
		put(RefreshCatalogEntryRemoteTask.class);
	}
	
	private static void put(Class<? extends RemoteTask> clazz) {
		boolean inserted = false;
		if(clazz.isAnnotationPresent(task_rasterdb.class)) {
			try {
				String name = clazz.getAnnotation(task_rasterdb.class).value();
				RemoteTaskInfo rti = createRTI(clazz, name);
				if(task_rasterdbMap.containsKey(name)) {
					log.warn("task with name already inserted, overwrite: " + name);
				}
				task_rasterdbMap.put(name, rti);
				inserted = true;
			} catch(Exception e) {
				log.error("could not put task: "+ clazz.getName() + e);
			}
		}

		if(clazz.isAnnotationPresent(task_pointdb.class)) {
			try {
				String name = clazz.getAnnotation(task_pointdb.class).value();
				RemoteTaskInfo rti = createRTI(clazz, name);
				if(task_pointdbMap.containsKey(name)) {
					log.warn("task with name already inserted, overwrite: " + name);
				}
				task_pointdbMap.put(name, rti);
				inserted = true;
			} catch(Exception e) {
				log.error("could not put task: "+ clazz.getName() + e);
			}
		}

		if(clazz.isAnnotationPresent(task_pointcloud.class)) {
			try {
				String name = clazz.getAnnotation(task_pointcloud.class).value();
				RemoteTaskInfo rti = createRTI(clazz, name);
				if(task_pointcloudMap.containsKey(name)) {
					log.warn("task with name already inserted, overwrite: " + name);
				}
				task_pointcloudMap.put(name, rti);
				inserted = true;
			} catch(Exception e) {
				log.error("could not put task: "+ clazz.getName() + e);
			}
		}
		
		if(clazz.isAnnotationPresent(task_voxeldb.class)) {
			try {
				String name = clazz.getAnnotation(task_voxeldb.class).value();
				RemoteTaskInfo rti = createRTI(clazz, name);
				if(task_voxeldbMap.containsKey(name)) {
					log.warn("task with name already inserted, overwrite: " + name);
				}
				task_voxeldbMap.put(name, rti);
				inserted = true;
			} catch(Exception e) {
				log.error("could not put task: "+ clazz.getName() + e);
			}
		}

		if(clazz.isAnnotationPresent(task_vectordb.class)) {
			try {
				String name = clazz.getAnnotation(task_vectordb.class).value();
				RemoteTaskInfo rti = createRTI(clazz, name);
				if(task_vectordbMap.containsKey(name)) {
					log.warn("task with name already inserted, overwrite: " + name);
				}
				task_vectordbMap.put(name, rti);
				inserted = true;
			} catch(Exception e) {
				log.error("could not put task: "+ clazz.getName() + e);
			}
		}

		if(!inserted) {
			log.warn("could not put task: "+ clazz.getName());
		}
	}
	
	private static RemoteTaskInfo createRTI(Class<? extends RemoteTask> clazz, String name) throws NoSuchMethodException, SecurityException {
		Constructor<? extends RemoteTask> constructor = clazz.getDeclaredConstructor(CONSTRUCTOR_ARGS);
		String description = getTaskDescription(clazz);
		Vec<RemoteTaskParameter> params = getParams(clazz);
		return new RemoteTaskInfo(name, constructor, description, params);		
	}

	private static String getTaskDescription(Class<?> clazz) {
		if(clazz.isAnnotationPresent(Description.class)) {
			return clazz.getAnnotation(Description.class).value();
		} else {
			return "";
		}
	}
	
	private static Vec<RemoteTaskParameter> getParams(Class<?> clazz) {
		//log.info("getParams");
		Vec<RemoteTaskParameter> params = new Vec<RemoteTaskParameter>();
		Param[] ps = clazz.getAnnotationsByType(Param.class);
		for(Param p:ps) {
			RemoteTaskParameter param = RemoteTaskParameter.of(p);
			//log.info("add param");
			params.add(param);
		}
		return params;
	}

	private static TreeMap<String, RemoteTaskInfo> collectAsTreeMap(Map<String, RemoteTaskInfo> map) {
		TreeMap<String, RemoteTaskInfo> m = new TreeMap<>();
		m.putAll(map);
		return m;
	}

	public static Map<String, TreeMap<String, RemoteTaskInfo>> list() {
		LinkedHashMap<String,TreeMap<String, RemoteTaskInfo>> map = new LinkedHashMap<>();
		map.put("task_rasterdb", collectAsTreeMap(task_rasterdbMap));
		map.put("task_pointdb", collectAsTreeMap(task_pointdbMap));
		map.put("task_pointcloud", collectAsTreeMap(task_pointcloudMap));
		map.put("task_voxeldb", collectAsTreeMap(task_voxeldbMap));
		map.put("task_vectordb", collectAsTreeMap(task_vectordbMap));
		return map;
	}
}
