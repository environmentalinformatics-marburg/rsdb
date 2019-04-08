package broker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broker.acl.ACL;
import broker.acl.DynamicPropertyUserStore;
import broker.acl.EmptyACL;
import broker.catalog.Catalog;
import broker.group.ExternalGroupConfig;
import broker.group.Poi;
import broker.group.PoiGroup;
import broker.group.Roi;
import broker.group.RoiGroup;
import pointcloud.PointCloud;
import pointcloud.PointCloudConfig;
import pointdb.PdbException;
import pointdb.PointDB;
import pointdb.base.PointdbConfig;
import rasterdb.RasterDB;
import rasterdb.RasterdbConfig;
import util.Timer;
import util.Util;
import vectordb.VectorDB;
import vectordb.VectordbConfig;

/**
 * Broker manages a set of DBs.
 * thread-safe
 * @author woellauer
 *
 */
public class Broker implements AutoCloseable {
	private static final Logger log = LogManager.getLogger();

	public final BrokerConfig brokerConfig;

	private static final Path rasterdb_root = Paths.get("rasterdb");
	private static final Path pointcloud_root = Paths.get("pointcloud");
	private static final Path vectordb_root = Paths.get("vectordb");
	
	private static final Path REALM_PROPERTIES_PATH = Paths.get("realm.properties");

	ConcurrentSkipListMap<String, RasterdbConfig> rasterdbConfigMap = new ConcurrentSkipListMap<String, RasterdbConfig>();
	ConcurrentSkipListMap<String, PointCloudConfig> pointcloudConfigMap = new ConcurrentSkipListMap<String, PointCloudConfig>();
	ConcurrentSkipListMap<String, VectordbConfig> vectordbConfigMap = new ConcurrentSkipListMap<String, VectordbConfig>();

	ConcurrentSkipListMap<String,PointDB> pointdbMap = new ConcurrentSkipListMap<String, PointDB>();
	ConcurrentSkipListMap<String, RasterDB> rasterdbMap = new ConcurrentSkipListMap<String, RasterDB>();
	ConcurrentSkipListMap<String, PointCloud> pointcloudMap = new ConcurrentSkipListMap<String, PointCloud>();
	ConcurrentSkipListMap<String, VectorDB> vectordbMap = new ConcurrentSkipListMap<String, VectorDB>();
	
	TreeMap<String, PoiGroup> poiGroupMap = new TreeMap<String, PoiGroup>();
	TreeMap<String, RoiGroup> roiGroupMap = new TreeMap<String, RoiGroup>(); 
	public final Catalog catalog;
	private DynamicPropertyUserStore userStore;

	public Broker() {		
		if(new File("config.yaml").exists()) {
			brokerConfig = new BrokerConfigYaml("config.yaml");
		} else {
			log.error("no config found");
			brokerConfig = new BrokerConfig(); // empty default config
		}		
		loadPoiGroupMap();
		loadRoiGroupMap();
		refreshRasterdbConfigs();
		refreshPointcloudConfigs();
		refreshVectordbConfigs();
		this.catalog = new Catalog(this);
	}

	public Path getPointCloudRoot() {
		return pointcloud_root;
	}

	private void loadPoiGroupMap() {
		Map<String, ExternalGroupConfig> map = brokerConfig.poiGroupMap();
		for(Entry<String, ExternalGroupConfig> entry:map.entrySet()) {
			try {
				ExternalGroupConfig conf = entry.getValue();
				poiGroupMap.put(conf.name, new PoiGroup(conf.name, conf.informal, conf.acl, Poi.readPoiCsv(conf.filename)));
			} catch(Exception e) {
				log.error(e);
			}
		}
	}

	private void loadRoiGroupMap() {
		Map<String, ExternalGroupConfig> map = brokerConfig.roiGroupMap();
		for(Entry<String, ExternalGroupConfig> entry:map.entrySet()) {
			try {
				ExternalGroupConfig conf = entry.getValue();
				roiGroupMap.put(conf.name, new RoiGroup(conf.name, conf.informal, conf.acl, Roi.readRoiGeoJSON(Paths.get(conf.filename))));
			} catch(Exception e) {
				log.error(e);
			}
		}

	}

	public Collection<PoiGroup> getPoiGroups() {
		return poiGroupMap.values();
	}

	public Collection<RoiGroup> getRoiGroups() {
		return roiGroupMap.values();
	}

	public PoiGroup getPoiGroup(String name) {
		PoiGroup poiGroup = poiGroupMap.get(name);
		if(poiGroup==null) {
			throw new RuntimeException("POI Group not found: "+name);
		}
		return poiGroup;
	}

	public RoiGroup getRoiGroup(String name) {
		RoiGroup roiGroup = roiGroupMap.get(name);
		if(roiGroup==null) {
			throw new RuntimeException("ROI Group not found: "+name);
		}
		return roiGroup;
	}

	public Poi getPoiByPath(String path) {
		String[] id = path.split("/");
		return getPoiByPath(id[0], id[1]);
	}

	public Poi getPoiByPath(String group, String name) {
		PoiGroup poigroup = getPoiGroup(group);
		for(Poi p:poigroup.pois) {
			if(p.name.equals(name)) {
				return p;
			}
		}
		return null;
	}

	public Roi getRoiByPath(String path) {
		String[] id = path.split("/");
		return getRoiByPath(id[0], id[1]);
	}

	public Roi getRoiByPath(String group, String name) {
		RoiGroup roigroup = getRoiGroup(group);
		for(Roi r:roigroup.rois) {
			if(r.name.equals(name)) {
				return r;
			}
		}
		return null;
	}

	/**
	 * get existing PointDB with name.
	 * @param name
	 * @return
	 */
	public PointDB getPointdb(String name) {
		return getPointdb(name, false);
	}

	/**
	 * get PointDB with name.
	 * @param name
	 * @param createIfmissing
	 * @return
	 */
	public PointDB getPointdb(String name, boolean createIfmissing) {
		PointDB pointdb = pointdbMap.get(name);
		if(pointdb!=null) {
			return pointdb;
		}
		return loadPointdb(name, createIfmissing);
	}

	private synchronized PointDB loadPointdb(String name, boolean createIfmissing) { //guarantee to load each PointDB once only
		PointDB pointdb = pointdbMap.get(name);
		if(pointdb!=null) {
			return pointdb;
		}
		PointdbConfig config = brokerConfig.pointdbMap().get(name);
		if(config==null) {
			throw new PdbException("no config found for "+name);
		}
		pointdb = new PointDB(config, createIfmissing);
		PointDB ret = pointdbMap.put(name, pointdb);
		if(ret!=null) {
			throw new PdbException("double load");
		}
		return pointdb;
	}


	@Override
	public synchronized void close() {
		if(pointdbMap !=null || rasterdbMap!= null || pointcloudMap!= null) {
			log.info("broker close...");
			Timer.start("broker close");
			//ForkJoinPool exe = new ForkJoinPool(); // may be queued
			ExecutorService exe = Executors.newCachedThreadPool(); //ensures that tasks are run.

			if(pointdbMap!=null) {
				for(Entry<String, PointDB> e:pointdbMap.entrySet()) {
					String name = e.getKey();
					PointDB pointdb = e.getValue();
					exe.execute(()->{log.info("run close pointdb "+name);pointdb.close();log.info("pointdb closed "+name);});
				}
			}

			if(rasterdbMap!=null) {
				for(Entry<String, RasterDB> e:rasterdbMap.entrySet()) {
					String name = e.getKey();
					RasterDB rasterdb = e.getValue();
					exe.execute(()->{log.info("run close rasterdb "+name);rasterdb.close();log.info("rasterdb closed "+name);});
				}
			}

			if(pointcloudMap!=null) {
				for(Entry<String, PointCloud> e:pointcloudMap.entrySet()) {
					String name = e.getKey();
					PointCloud pointcloud = e.getValue();
					exe.execute(()->{log.info("run close pointcloud "+name);pointcloud.close();log.info("pointcloud closed "+name);});
				}
			}

			try {
				exe.shutdown();
				if(!exe.awaitTermination(5, TimeUnit.MINUTES)) {
					log.error("broker close timeout   "+Timer.stop("broker close"));
				} else {
					log.info("broker closed.   "+Timer.stop("broker close"));
				}
			} catch (InterruptedException e) {
				log.error(e+"   "+Timer.stop("broker close"));
			}
			pointdbMap = null;
			rasterdbMap = null;
			pointcloudMap = null;
		}
	}

	public void refreshRasterdbConfigs() {
		try {
			ConcurrentSkipListMap<String, RasterdbConfig> map = new ConcurrentSkipListMap<String, RasterdbConfig>();
			Path[] paths = Util.getPaths(rasterdb_root);
			for(Path path:paths) {
				RasterdbConfig rasterdbConfig = RasterdbConfig.ofPath(path);
				map.put(rasterdbConfig.getName(), rasterdbConfig);
			}
			rasterdbConfigMap = map;
		} catch (Exception e) {
			log.error(e);
		}
	}

	public void refreshPointcloudConfigs() {
		try {
			ConcurrentSkipListMap<String, PointCloudConfig> map = new ConcurrentSkipListMap<String, PointCloudConfig>();
			Path[] paths = Util.getPaths(pointcloud_root);
			for(Path path:paths) {
				PointCloudConfig config = PointCloudConfig.ofPath(path, true);
				map.put(config.name, config);
			}
			pointcloudConfigMap = map;
		} catch (Exception e) {
			log.error(e);
		}
	}
	
	public void refreshVectordbConfigs() {
		log.info("refreshVectordbConfigs");
		try {
			ConcurrentSkipListMap<String, VectordbConfig> map = new ConcurrentSkipListMap<String, VectordbConfig>();
			Path[] paths = Util.getPaths(vectordb_root);
			for(Path path:paths) {
				VectordbConfig config = VectordbConfig.ofPath(path);
				map.put(config.name, config);
			}
			vectordbConfigMap = map;
		} catch (Exception e) {
			log.error(e);
		}
	}

	public NavigableSet<String> getRasterdbNames() {
		return rasterdbConfigMap.keySet();
	}

	public RasterDB getRasterdb(String name) {
		RasterDB rasterdb = rasterdbMap.get(name);
		if(rasterdb!=null) {
			return rasterdb;
		}
		return openRasterdb(name);
	}
	
	public boolean hasRasterdb(String id) {
		return rasterdbConfigMap.containsKey(id);
	}

	private synchronized RasterDB openRasterdb(String name) { //guarantee to load each PointDB once only
		RasterDB rastrdb = rasterdbMap.get(name);
		if(rastrdb!=null) {
			return rastrdb;
		}
		RasterdbConfig config = rasterdbConfigMap.get(name);
		if(config==null) {			
			refreshRasterdbConfigs();
			config = rasterdbConfigMap.get(name);
			if(config==null) {
				throw new RasterDBNotFoundExeption("RasterDB not found: "+name);
			}
		}
		rastrdb = new RasterDB(config);
		RasterDB ret = rasterdbMap.put(name, rastrdb);
		if(ret!=null) {
			throw new PdbException("double load: "+name);
		}
		log.info("opened " + name + "   " + rasterdbMap.get(name));
		return rastrdb;
	}

	public synchronized RasterDB createOrGetRasterdb(String name) {
		return createOrGetRasterdb(name, true);
	}

	public synchronized RasterDB createOrGetRasterdb(String name, boolean transaction) {
		Util.checkStrictID(name);
		RasterdbConfig rasterdbConfig = RasterdbConfig.ofPath(rasterdb_root.resolve(name));
		if(!transaction) {
			rasterdbConfig.set_fast_unsafe_import(true);
		}
		rasterdbConfigMap.put(rasterdbConfig.getName(), rasterdbConfig);
		return getRasterdb(name);
	}
	
	public static class RasterDBNotFoundExeption extends PdbException {
		public RasterDBNotFoundExeption(String message) {
			super(message);
		}		
	}



	/**
	 * create new rasterdb 
	 * if exists then delete old
	 * @param name
	 * @param transaction
	 * @return
	 */
	public synchronized RasterDB createRasterdb(String name) {
		return createRasterdb(name, true);
	}
	
	public synchronized void closeRasterdb(String name) {
		log.info("try close rasterdb " + name);
		RasterDB rasterdb = rasterdbMap.get(name);
		if(rasterdb != null) {
			log.info("close rasterdb " + name);
			rasterdb.close();
			rasterdbMap.remove(name);
		}
	}
	
	public synchronized void closePointCloud(String name) {
		log.info("try close pointcloud " + name);
		PointCloud pointcloud = pointcloudMap.get(name);
		if(pointcloud != null) {
			log.info("close pointcloud " + name);
			pointcloud.close();
			pointcloudMap.remove(name);
		}
	}
	
	public synchronized void deleteRasterdb(String name) {
		Util.checkStrictID(name);
		log.info("check " + name + "   " + rasterdbMap.get(name));
		closeRasterdb(name);
		Path rasterdbPath = rasterdb_root.resolve(name);
		if(Files.exists(rasterdbPath)) {
			log.info("delete RasterDB: " + name);
			try {
				Files.deleteIfExists(rasterdbPath.resolve("raster"));
				Files.deleteIfExists(rasterdbPath.resolve("raster1"));
				Files.deleteIfExists(rasterdbPath.resolve("raster2"));
				Files.deleteIfExists(rasterdbPath.resolve("raster3"));
				Files.deleteIfExists(rasterdbPath.resolve("raster4"));
				Files.deleteIfExists(rasterdbPath.resolve("raster.cache"));
				Files.deleteIfExists(rasterdbPath.resolve("raster1.cache"));
				Files.deleteIfExists(rasterdbPath.resolve("raster2.cache"));
				Files.deleteIfExists(rasterdbPath.resolve("raster3.cache"));
				Files.deleteIfExists(rasterdbPath.resolve("raster4.cache"));
				Files.deleteIfExists(rasterdbPath.resolve("meta.yaml"));
				Files.deleteIfExists(rasterdbPath);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		refreshRasterdbConfigs();
		catalog.updateCatalog();
	}
	
	public synchronized void deletePointCloud(String name) {
		log.info("check " + name + "   " + pointcloudMap.get(name));
		closePointCloud(name);
		Path pointcloudPath = pointcloud_root.resolve(name);
		if(Files.exists(pointcloudPath)) {
			log.info("delete PointCloud: " + name + " in   " + pointcloudPath);
			try {
				boolean r1 = Files.deleteIfExists(pointcloudPath.resolve("pointcloud.dat"));
				boolean r2 = Files.deleteIfExists(pointcloudPath.resolve("pointcloud.yml"));
				boolean r3 = Files.deleteIfExists(pointcloudPath);
				log.info(r1 + "  " + r2 + "  " + r3);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		refreshPointcloudConfigs();
		catalog.updateCatalog();
	}

	/**
	 * create new rasterdb 
	 * if exists then delete old
	 * @param name
	 * @param transaction
	 * @return
	 */
	public synchronized RasterDB createRasterdb(String name, boolean transaction) {
		deleteRasterdb(name);
		return createOrGetRasterdb(name, transaction);
	}

	public NavigableSet<String> getPointCloudNames() {
		return pointcloudConfigMap.keySet();
	}

	public PointCloud getPointCloud(String name) {
		PointCloud pointcloud = pointcloudMap.get(name);
		if(pointcloud != null) {
			return pointcloud;
		}
		return openPointCloud(name);
	}

	private synchronized PointCloud openPointCloud(String name) { // guarantee to load each PointDB once only
		PointCloud pointcloud = pointcloudMap.get(name);
		if(pointcloud != null) {
			return pointcloud;
		}
		PointCloudConfig config = pointcloudConfigMap.get(name);
		if(config==null) {			
			refreshPointcloudConfigs();
			config = pointcloudConfigMap.get(name);
			if(config==null) {
				throw new PdbException("PointCloud not found: "+name);
			}
		}
		pointcloud = new PointCloud(config);
		PointCloud ret = pointcloudMap.put(name, pointcloud);
		if(ret!=null) {
			throw new PdbException("double load: "+name);
		}
		return pointcloud;
	}

	public synchronized PointCloud getOrCreatePointCloud(String name, boolean transactions) {
		PointCloudConfig config = PointCloudConfig.ofPath(pointcloud_root.resolve(name), transactions);
		pointcloudConfigMap.put(config.name, config);
		return getPointCloud(name);
	}

	public synchronized PointCloud createNewPointCloud(String name, boolean transactions) {
		if(pointcloudConfigMap.containsKey(name)) {
			throw new RuntimeException("PointCloud already exist: "+name);
		}
		return getOrCreatePointCloud(name, transactions);
	}

	public ACL getPointCloudACL(String name) {
		PointCloud pointcloud = pointcloudMap.get(name);
		if(pointcloud != null) {
			return pointcloud.getACL();
		}
		PointCloudConfig pointcloudConfig = pointcloudConfigMap.get(name);
		if(pointcloudConfig != null) {
			return pointcloudConfig.readACL();
		}
		return EmptyACL.ADMIN;
	}

	public Informal getPointCloudInformal(String name) {
		PointCloud pointcloud = pointcloudMap.get(name);
		if(pointcloud != null) {
			return pointcloud.informal();
		}
		PointCloudConfig pointcloudConfig = pointcloudConfigMap.get(name);
		if(pointcloudConfig != null) {
			return pointcloudConfig.readInformal();
		}
		return Informal.EMPTY;
	}

	private synchronized DynamicPropertyUserStore openUserStore() {
		if(this.userStore == null) {
			DynamicPropertyUserStore userStore = new DynamicPropertyUserStore();
			userStore.setConfigPath(REALM_PROPERTIES_PATH);
			try {
				userStore.start();
			} catch (Exception e) {
				log.error(e);
			}
			this.userStore = userStore;
		}
		return userStore;
	}

	public DynamicPropertyUserStore getUserStore() {
		DynamicPropertyUserStore us = userStore;
		return us == null ? openUserStore() : us;
	}
	
	private synchronized VectorDB openVectorDB(String name) { // guarantee to load each VectorDB once only
		VectorDB vectordb = vectordbMap.get(name);
		if(vectordb != null) {
			return vectordb;
		}
		VectordbConfig config = vectordbConfigMap.get(name);
		if(config==null) {			
			refreshVectordbConfigs();
			config = vectordbConfigMap.get(name);
			if(config==null) {
				throw new PdbException("VectorDB not found: "+name);
			}
		}
		vectordb = new VectorDB(config);
		VectorDB ret = vectordbMap.put(name, vectordb);
		if(ret != null) {
			throw new PdbException("double load: "+name);
		}
		return vectordb;
	}
	
	public VectorDB getVectorDB(String name) {
		VectorDB vectordb = vectordbMap.get(name);
		if(vectordb != null) {
			return vectordb;
		}
		return openVectorDB(name);
	}
	
	public NavigableSet<String> getVectordbNames() {
		return vectordbConfigMap.keySet();
	}
	
	public void createVectordb(String name) {
		if(name == null || name.isEmpty()) {
			throw new RuntimeException("no name");
		}
		Util.checkStrictID(name);
		Path subPath = vectordb_root.resolve(name);
		if(subPath.toFile().exists()) {
			throw new RuntimeException("path exists");
		}
		if(!subPath.toFile().mkdir()) {
			throw new RuntimeException("could not create path");
		}
		
		Path dataPath = subPath.resolve("data");
		if(dataPath.toFile().exists()) {
			throw new RuntimeException("data path exists");
		}
		if(!dataPath.toFile().mkdir()) {
			throw new RuntimeException("could not create data path");
		}
		VectorDB vectordb = getVectorDB(name);
		vectordb.writeMeta();
		refreshVectordbConfigs();
		catalog.updateCatalog();
	}
	
	
	public synchronized void closeVectordb(String name) {
		VectorDB vectordb = vectordbMap.get(name);
		if(vectordb != null) {
			vectordb.close();
			vectordbMap.remove(name);
		}
	}

	public synchronized boolean deleteVectordb(String name) {
		Util.checkID(name);
		closeVectordb(name);
		System.gc(); // close GDAL opened files ?
		Path vectordbPath = vectordb_root.resolve(name);
		Util.checkIsParent(vectordb_root, vectordbPath);
		if(Files.exists(vectordbPath)) {
			log.info("delete VectorDB: " + name);
			try {
				Path dataPath = vectordbPath.resolve("data");
				Util.checkIsParent(vectordbPath, dataPath);
				File[] files = dataPath.toFile().listFiles();
				if(files != null) {
					for(File file:files) {
						Util.safeDeleteIfExists(vectordbPath, file.toPath());
					}
				}
				Util.safeDeleteIfExists(vectordbPath, dataPath);
				Util.safeDeleteIfExists(vectordbPath, vectordbPath.resolve("meta.yml"));
				Util.safeDeleteIfExists(vectordb_root, vectordbPath);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		refreshVectordbConfigs();
		log.info("update catalog");
		catalog.updateCatalog();
		return !Files.exists(vectordbPath);
	}
}
