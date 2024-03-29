package broker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.UserIdentity;
import org.gdal.osr.SpatialReference;
import org.tinylog.Logger;

import broker.acl.ACL;
import broker.acl.AclUtil;
import broker.catalog.Catalog;
import broker.catalog.CatalogKey;
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
import postgis.PostgisLayer;
import postgis.PostgisLayerManager;
import postgis.PostgisPOIcreator;
import postgis.PostgisROIcreator;
import rasterdb.RasterDB;
import rasterdb.RasterdbConfig;
import remotetask.MessageSink;
import server.api.vectordbs.VectordbDetails;
import util.GeoUtil;
import util.Timer;
import util.Util;
import util.collections.vec.Vec;
import vectordb.VectorDB;
import vectordb.VectordbConfig;
import voxeldb.VoxelDB;
import voxeldb.VoxeldbConfig;

/**
 * Broker manages a set of DBs.
 * thread-safe
 * @author woellauer
 *
 */
public class Broker implements AutoCloseable {
	

	public final BrokerConfig brokerConfig;

	public static final Path rasterdb_root = Paths.get("rasterdb");
	public static final Path pointcloud_root = Paths.get("pointcloud");
	public static final Path vectordb_root = Paths.get("vectordb");
	public static final Path voxeldb_root = Paths.get("voxeldb");

	//private static final Path REALM_PROPERTIES_PATH = Paths.get("realm.properties");

	ConcurrentSkipListMap<String, RasterdbConfig> rasterdbConfigMap = new ConcurrentSkipListMap<String, RasterdbConfig>();
	ConcurrentSkipListMap<String, PointCloudConfig> pointcloudConfigMap = new ConcurrentSkipListMap<String, PointCloudConfig>();
	ConcurrentSkipListMap<String, VectordbConfig> vectordbConfigMap = new ConcurrentSkipListMap<String, VectordbConfig>();
	ConcurrentSkipListMap<String, VoxeldbConfig> voxeldbConfigMap = new ConcurrentSkipListMap<String, VoxeldbConfig>();

	ConcurrentSkipListMap<String,PointDB> pointdbMap = new ConcurrentSkipListMap<String, PointDB>();
	ConcurrentSkipListMap<String, RasterDB> rasterdbMap = new ConcurrentSkipListMap<String, RasterDB>();
	ConcurrentSkipListMap<String, PointCloud> pointcloudMap = new ConcurrentSkipListMap<String, PointCloud>();
	ConcurrentSkipListMap<String, VectorDB> vectordbMap = new ConcurrentSkipListMap<String, VectorDB>();
	ConcurrentSkipListMap<String, VoxelDB> voxeldbMap = new ConcurrentSkipListMap<String, VoxelDB>();

	TreeMap<String, PoiGroup> poiGroupMap = new TreeMap<String, PoiGroup>();
	TreeMap<String, RoiGroup> roiGroupMap = new TreeMap<String, RoiGroup>(); 
	public final Catalog catalog;
	
	private AccountManager accountManager;
	private volatile AccountManager accountManagerVolatile;
	private Object accountManagerLoadLock = new Object();
	
	private PublicAccessManager publicAccessManager;
	private volatile PublicAccessManager publicAccessManagerVolatile;
	private Object publicAccessManagerLoadLock = new Object();
	
	private MetaDataSchema metaDataSchema;
	private volatile MetaDataSchema metaDataSchemaVolatile;
	private Object metaDataSchemaLoadLock = new Object();
	
	private PostgisLayerManager postgisLayerManager;
	private volatile PostgisLayerManager postgisLayerManagerVolatile;
	private Object postgisLayerManagerLoadLock = new Object();

	public Broker() {		
		if(new File("config.yaml").exists()) {
			brokerConfig = new BrokerConfigYaml("config.yaml");
		} else {
			Logger.info("no config found: config.yaml file missing");
			brokerConfig = new BrokerConfig(); // empty default config
		}		
		refreshRasterdbConfigs();
		refreshPointcloudConfigs();
		refreshVoxeldbConfigs();
		refreshVectordbConfigs();
		this.catalog = new Catalog(this);
		refreshPoiGroupMap();
		refreshRoiGroupMap();
	}

	public Path getPointCloudRoot() {
		return pointcloud_root;
	}

	public Path getRasterDBRoot() {
		return rasterdb_root;
	}	

	public synchronized void refreshPoiGroupMap() {
		TreeMap<String, PoiGroup> map = new TreeMap<String, PoiGroup>();
		Map<String, ExternalGroupConfig> configMap = brokerConfig.poiGroupMap();
		for(Entry<String, ExternalGroupConfig> entry:configMap.entrySet()) {
			try {
				ExternalGroupConfig conf = entry.getValue();
				map.put(conf.name, new PoiGroup(conf.name, conf.informal, conf.acl, Poi.readPoiCsv(conf.filename)));
			} catch(Exception e) {
				Logger.error(e);
			}
		}

		catalog.getSorted(CatalogKey.TYPE_VECTORDB, null)
		.filter(entry -> entry.structuredAccess != null && entry.structuredAccess.poi)
		.forEachOrdered(entry -> {
			try {
				VectorDB vectordb = this.getVectorDB(entry.name);
				Vec<String> messages = new Vec<String>();
				Poi[] pois = vectordb.getPOIs(messages).toArray(Poi[]::new);
				ACL acl_read = AclUtil.union(vectordb.getACL_owner(), vectordb.getACL_mod(), vectordb.getACL());
				VectordbDetails details = vectordb.getDetails();
				PoiGroup poiGroup = new PoiGroup(vectordb.getName(), vectordb.informal(), acl_read, details.epsg, details.proj4, messages, pois);
				map.put(poiGroup.name, poiGroup);
			} catch(Exception e) {
				Logger.warn(e.getMessage());
			}
		});
		
		catalog.getSorted(CatalogKey.TYPE_POSTGIS, null)
		.filter(entry -> entry.structuredAccess != null && entry.structuredAccess.poi)
		.forEachOrdered(entry -> {
			try {
				PostgisLayer postgis = this.postgisLayerManager().getPostgisLayer(entry.name);
				Vec<String> messages = new Vec<String>();
				Poi[] pois = PostgisPOIcreator.create(postgis, messages).toArray(Poi[]::new);
				ACL acl_read = AclUtil.union(postgis.getACL_owner(), postgis.getACL_mod(), postgis.getACL());
				int epsg = postgis.getEPSG();
				String proj4 = "";
				SpatialReference sr = GeoUtil.getSpatialReferenceFromEPSG(postgis.getEPSG());
				if(sr != null) {
					proj4 = sr.ExportToProj4();
					if(proj4 == null) {
						proj4 = "";
					}
				}
				PoiGroup poiGroup = new PoiGroup(postgis.name, postgis.informal(), acl_read, "EPSG:" + epsg, proj4, messages, pois);
				map.put(poiGroup.name, poiGroup);
			} catch(Exception e) {
				Logger.warn(e.getMessage());
			}
		});

		poiGroupMap = map;
	}

	public synchronized void refreshRoiGroupMap() {
		TreeMap<String, RoiGroup> map = new TreeMap<String, RoiGroup>();
		Map<String, ExternalGroupConfig> configMap = brokerConfig.roiGroupMap();
		for(Entry<String, ExternalGroupConfig> entry:configMap.entrySet()) {
			try {
				ExternalGroupConfig conf = entry.getValue();
				map.put(conf.name, new RoiGroup(conf.name, conf.informal, conf.acl, null, Roi.readRoiGeoJSON(Paths.get(conf.filename))));
			} catch(Exception e) {
				Logger.error(e);
			}
		}

		catalog.getSorted(CatalogKey.TYPE_VECTORDB, null)
		.filter(entry -> entry.structuredAccess != null && entry.structuredAccess.roi)
		.forEachOrdered(entry -> {
			try {
				VectorDB vectordb = this.getVectorDB(entry.name);
				Vec<String> messages = new Vec<String>();
				Roi[] rois = vectordb.getROIs(messages).toArray(Roi[]::new);
				ACL acl_read = AclUtil.union(vectordb.getACL_owner(), vectordb.getACL_mod(), vectordb.getACL());
				VectordbDetails details = vectordb.getDetails();
				RoiGroup roiGroup = new RoiGroup(vectordb.getName(), vectordb.informal(), acl_read, details.epsg, details.proj4, messages, rois);
				map.put(roiGroup.name, roiGroup);
			} catch(Exception e) {
				Logger.warn(e.getMessage());
			}
		});
		
		catalog.getSorted(CatalogKey.TYPE_POSTGIS, null)
		.filter(entry -> entry.structuredAccess != null && entry.structuredAccess.poi)
		.forEachOrdered(entry -> {
			try {
				PostgisLayer postgis = this.postgisLayerManager().getPostgisLayer(entry.name);
				Vec<String> messages = new Vec<String>();
				Roi[] rois = PostgisROIcreator.create(postgis, messages).toArray(Roi[]::new);
				ACL acl_read = AclUtil.union(postgis.getACL_owner(), postgis.getACL_mod(), postgis.getACL());
				int epsg = postgis.getEPSG();
				String proj4 = "";
				SpatialReference sr = GeoUtil.getSpatialReferenceFromEPSG(postgis.getEPSG());
				if(sr != null) {
					proj4 = sr.ExportToProj4();
					if(proj4 == null) {
						proj4 = "";
					}
				}
				RoiGroup roiGroup = new RoiGroup(postgis.name, postgis.informal(), acl_read, "EPSG:" + epsg, proj4, messages, rois);
				map.put(roiGroup.name, roiGroup);
			} catch(Exception e) {
				Logger.warn(e.getMessage());
			}
		});

		roiGroupMap = map;
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
			//Logger.info("close broker ...");
			Timer.start("broker close");
			//ForkJoinPool exe = new ForkJoinPool(); // may be queued
			ExecutorService exe = Executors.newCachedThreadPool(); //ensures that tasks are run.

			if(pointdbMap!=null) {
				for(Entry<String, PointDB> e:pointdbMap.entrySet()) {
					String name = e.getKey();
					PointDB pointdb = e.getValue();
					exe.execute(()->{/*Logger.info("run close pointdb "+name);*/pointdb.close();/*Logger.info("pointdb closed "+name);*/});
				}
			}

			if(rasterdbMap!=null) {
				for(Entry<String, RasterDB> e:rasterdbMap.entrySet()) {
					String name = e.getKey();
					RasterDB rasterdb = e.getValue();
					exe.execute(()->{/*Logger.info("run close rasterdb "+name);*/rasterdb.close();/*Logger.info("rasterdb closed "+name);*/});
				}
			}

			if(pointcloudMap!=null) {
				for(Entry<String, PointCloud> e:pointcloudMap.entrySet()) {
					String name = e.getKey();
					PointCloud pointcloud = e.getValue();
					exe.execute(()->{/*Logger.info("run close pointcloud "+name);*/pointcloud.close();/*Logger.info("pointcloud closed "+name);*/});
				}
			}

			if(voxeldbMap!=null) {
				for(Entry<String, VoxelDB> e : voxeldbMap.entrySet()) {
					String name = e.getKey();
					VoxelDB voxeldb = e.getValue();
					exe.execute(()->{/*Logger.info("run close voxeldb "+name);*/voxeldb.close();/*Logger.info("voxeldb closed "+name);*/});
				}
			}

			try {
				exe.shutdown();
				if(!exe.awaitTermination(5, TimeUnit.MINUTES)) {
					Logger.error("broker close timeout   "+Timer.stop("broker close"));
				} else {
					Logger.info("broker closed.   "+Timer.stop("broker close").timeToString());
				}
			} catch (InterruptedException e) {
				Logger.error(e+"   "+Timer.stop("broker close"));
			}
			pointdbMap = null;
			rasterdbMap = null;
			pointcloudMap = null;
		}
	}
	
	public synchronized void refreshAllLayers(MessageSink messageSink) {
		messageSink.setMessage("refresh RasterDB layers");
		refreshRasterdbConfigs();
		
		messageSink.setMessage("refresh Pointcloud layers");
		refreshPointcloudConfigs();
		
		messageSink.setMessage("refresh VoxelDB layers");
		refreshVoxeldbConfigs();
		
		messageSink.setMessage("refresh VectorDB layers");
		refreshVectordbConfigs();
		
		messageSink.setMessage("refresh PostGIS layers");
		postgisLayerManager().refresh();
		
		messageSink.setMessage("refresh VoxelDB layers");
		refreshVoxeldbConfigs();
		
		messageSink.setMessage("refresh PoiGroup layers");
		refreshPoiGroupMap();
		
		messageSink.setMessage("refresh RoiGroup layers");
		refreshRoiGroupMap();
	}

	public synchronized void refreshRasterdbConfigs() {
		if(!rasterdb_root.toFile().exists()) {
			Logger.trace("no rasterdb layers: rasterdb folder missing");
			return;
		}
		try {
			ConcurrentSkipListMap<String, RasterdbConfig> map = new ConcurrentSkipListMap<String, RasterdbConfig>();			
			Path[] paths = Util.getPaths(rasterdb_root);
			for(Path path:paths) {
				RasterdbConfig rasterdbConfig = RasterdbConfig.ofPath(path, null);
				map.put(rasterdbConfig.getName(), rasterdbConfig);
			}
			rasterdbConfigMap = map;
		} catch (Exception e) {
			Logger.error(e);
		}
	}

	public synchronized void refreshPointcloudConfigs() {
		if(!pointcloud_root.toFile().exists()) {
			Logger.trace("no pointcloud layers: pointcloud folder missing");
			return;
		}
		try {
			ConcurrentSkipListMap<String, PointCloudConfig> map = new ConcurrentSkipListMap<String, PointCloudConfig>();
			Path[] paths = Util.getPaths(pointcloud_root);
			for(Path path:paths) {
				PointCloudConfig config = PointCloudConfig.ofPath(path, null, true);
				map.put(config.name, config);
			}
			pointcloudConfigMap = map;
		} catch (Exception e) {
			Logger.error(e);
		}
	}

	public synchronized void refreshVoxeldbConfigs() {
		if(!voxeldb_root.toFile().exists()) {
			Logger.trace("no voxeldb layers: voxeldb folder missing");
			return;
		}
		try {
			ConcurrentSkipListMap<String, VoxeldbConfig> map = new ConcurrentSkipListMap<String, VoxeldbConfig>();
			Path[] paths = Util.getPaths(voxeldb_root);
			for(Path path:paths) {
				VoxeldbConfig config = VoxeldbConfig.ofPath(path, null, true);
				map.put(config.name, config);
			}
			voxeldbConfigMap = map;
		} catch (Exception e) {
			Logger.error(e);
		}
	}

	public synchronized void refreshVectordbConfigs() {
		if(!vectordb_root.toFile().exists()) {
			Logger.trace("no vectordb layers: vectordb folder missing");
			return;
		}
		try {
			ConcurrentSkipListMap<String, VectordbConfig> map = new ConcurrentSkipListMap<String, VectordbConfig>();
			Path[] paths = Util.getPaths(vectordb_root);
			for(Path path:paths) {
				VectordbConfig config = VectordbConfig.ofPath(path);
				map.put(config.name, config);
			}
			vectordbConfigMap = map;
		} catch (Exception e) {
			Logger.error(e);
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
				throw new RasterDBNotFoundExeption("RasterDB not found: ["+name+"]");
			}
		}
		rastrdb = new RasterDB(config);
		RasterDB ret = rasterdbMap.put(name, rastrdb);
		if(ret!=null) {
			throw new PdbException("double load: "+name);
		}
		//Logger.info("opened " + name + "   " + rasterdbMap.get(name));
		return rastrdb;
	}

	public synchronized RasterDB createOrGetRasterdb(String name) {
		return createOrGetRasterdb(name, true);
	}

	public synchronized RasterDB createOrGetRasterdb(String name, boolean transaction) {
		return createOrGetRasterdb(name, transaction, "TileStorage");
	}
	
	public RasterdbConfig createRasterdbConfig(String name) {
		Util.checkStrictID(name);
		RasterdbConfig rasterdbConfig = RasterdbConfig.ofPath(rasterdb_root.resolve(name));
		return rasterdbConfig;
	}

	public synchronized RasterDB createOrGetRasterdb(String name, boolean transaction, String storage_type) {
		Util.checkStrictID(name);
		Logger.info("createOrGetRasterdb with storage_type " + storage_type);
		RasterdbConfig rasterdbConfig = RasterdbConfig.ofPath(rasterdb_root.resolve(name), storage_type);
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
	public synchronized RasterDB createNewRasterdb(String name) {
		return createNewRasterdb(name, true);
	}
	
	public synchronized RasterDB createNewRasterdb(RasterdbConfig config) {
		String name = config.getName();
		Util.checkStrictID(name);
		deleteRasterdb(config.getName());
		rasterdbConfigMap.put(name, config);
		return getRasterdb(name);
	}

	public synchronized boolean closeRasterdb(String name) {
		Logger.info("try close rasterdb " + name);
		RasterDB rasterdb = rasterdbMap.get(name);
		if(rasterdb == null) {
			return false;
		}
		Logger.info("close rasterdb " + name);
		rasterdb.close();
		rasterdbMap.remove(name);
		return true;
	}

	public synchronized boolean closePointcloud(String name) {
		Logger.info("try close pointcloud " + name);
		PointCloud pointcloud = pointcloudMap.get(name);
		if(pointcloud == null) {
			return false;
		}
		Logger.info("close pointcloud " + name);
		pointcloud.close();
		pointcloudMap.remove(name);
		return true;
	}

	public synchronized boolean closeVoxeldb(String name) {
		Logger.info("try close VoxelDB " + name);
		VoxelDB voxeldb = voxeldbMap.get(name);
		if(voxeldb == null) {
			return false;
		}
		Logger.info("close voxeldb " + name);
		voxeldb.close();
		voxeldbMap.remove(name);
		return true;
	}

	public synchronized boolean closeVectordb(String name) {
		VectorDB vectordb = vectordbMap.get(name);
		if(vectordb == null) {
			return false;
		}
		vectordb.close();
		vectordbMap.remove(name);
		return true;
	}	

	public synchronized void deleteRasterdb(String name) {
		Util.checkStrictID(name);
		Logger.info("check " + name + "   " + rasterdbMap.get(name));
		closeRasterdb(name);
		Path rasterdbPath = rasterdb_root.resolve(name);
		if(Files.exists(rasterdbPath)) {
			Logger.info("delete RasterDB: " + name);
			try {
				//RasterUnit
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
				//TileStorage
				Files.deleteIfExists(rasterdbPath.resolve("raster.tst"));
				Files.deleteIfExists(rasterdbPath.resolve("raster1.tst"));
				Files.deleteIfExists(rasterdbPath.resolve("raster2.tst"));
				Files.deleteIfExists(rasterdbPath.resolve("raster3.tst"));
				Files.deleteIfExists(rasterdbPath.resolve("raster4.tst"));
				Files.deleteIfExists(rasterdbPath.resolve("raster.idx"));
				Files.deleteIfExists(rasterdbPath.resolve("raster1.idx"));
				Files.deleteIfExists(rasterdbPath.resolve("raster2.idx"));
				Files.deleteIfExists(rasterdbPath.resolve("raster3.idx"));
				Files.deleteIfExists(rasterdbPath.resolve("raster4.idx"));
				Files.deleteIfExists(rasterdbPath.resolve("raster.DIRTY"));
				Files.deleteIfExists(rasterdbPath.resolve("raster1.DIRTY"));
				Files.deleteIfExists(rasterdbPath.resolve("raster2.DIRTY"));
				Files.deleteIfExists(rasterdbPath.resolve("raster3.DIRTY"));
				Files.deleteIfExists(rasterdbPath.resolve("raster4.DIRTY"));
				//meta
				Files.deleteIfExists(rasterdbPath.resolve("meta.yaml"));
				Files.deleteIfExists(rasterdbPath);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		refreshRasterdbConfigs();
		catalog.refreshCatalog();
	}

	public synchronized void deletePointCloud(String name) {
		Logger.info("check " + name + "   " + pointcloudMap.get(name));
		closePointcloud(name);
		Path pointcloudPath = pointcloud_root.resolve(name);
		if(Files.exists(pointcloudPath)) {
			Logger.info("delete PointCloud: " + name + " in   " + pointcloudPath);
			try {
				boolean r1 = Files.deleteIfExists(pointcloudPath.resolve("pointcloud.dat")); // type: RasterUnit
				boolean r2 = Files.deleteIfExists(pointcloudPath.resolve("pointcloud.idx")); // type: TileStorage
				boolean r3 = Files.deleteIfExists(pointcloudPath.resolve("pointcloud.tst")); // type: TileStorage
				boolean r4 = Files.deleteIfExists(pointcloudPath.resolve("pointcloud.DIRTY")); // type: TileStorage
				boolean r5 = Files.deleteIfExists(pointcloudPath.resolve("pointcloud.yml")); // meta data
				boolean r6 = Files.deleteIfExists(pointcloudPath);
				Logger.info(r1 + "  " + r2 + "  " + r3 + "  " + r4 + "  " + r5 + "  " + r6);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		try {
			Thread.sleep(100); // wait for filesystem update
		} catch (InterruptedException e) {
			Logger.warn(e);
		}
		refreshPointcloudConfigs();
		catalog.refreshCatalog();
	}

	public synchronized void deleteVoxeldb(String name) {
		Logger.info("check " + name + "   " + voxeldbMap.get(name));
		closeVoxeldb(name);
		Path voxeldbPath = voxeldb_root.resolve(name);
		if(Files.exists(voxeldbPath)) {
			Logger.info("delete VoxelDB: " + name + " in   " + voxeldbPath);
			try {
				boolean r1 = Files.deleteIfExists(voxeldbPath.resolve("voxeldb.dat")); // type: RasterUnit
				boolean r2 = Files.deleteIfExists(voxeldbPath.resolve("voxeldb.idx")); // type: TileStorage
				boolean r3 = Files.deleteIfExists(voxeldbPath.resolve("voxeldb.tst")); // type: TileStorage
				boolean r4 = Files.deleteIfExists(voxeldbPath.resolve("voxeldb.DIRTY")); // type: TileStorage
				boolean r5 = Files.deleteIfExists(voxeldbPath.resolve("voxeldb.yml")); // meta data
				boolean r6 = Files.deleteIfExists(voxeldbPath);
				Logger.info(r1 + "  " + r2 + "  " + r3 + "  " + r4 + "  " + r5 + "  " + r6);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		try {
			Thread.sleep(100); // wait for filesystem update
		} catch (InterruptedException e) {
			Logger.warn(e);
		}
		refreshVoxeldbConfigs();
		catalog.refreshCatalog();
	}


	public synchronized RasterDB createNewRasterdb(String name, boolean transaction, String storage_type) {
		deleteRasterdb(name);
		return createOrGetRasterdb(name, transaction, storage_type);
	}

	/**
	 * create new rasterdb 
	 * if exists then delete old
	 * @param name
	 * @param transaction
	 * @return
	 */
	public synchronized RasterDB createNewRasterdb(String name, boolean transaction) {
		deleteRasterdb(name);
		return createOrGetRasterdb(name, transaction);
	}

	public NavigableSet<String> getPointCloudNames() {
		return pointcloudConfigMap.keySet();
	}

	public NavigableSet<String> getVoxeldbNames() {
		return voxeldbConfigMap.keySet();
	}

	public PointCloud getPointCloud(String name) {
		PointCloud pointcloud = pointcloudMap.get(name);
		if(pointcloud != null) {
			return pointcloud;
		}
		return openPointCloud(name);
	}
	
	public boolean hasPointCloud(String name) {
		return pointcloudConfigMap.containsKey(name);
	}

	public VoxelDB getVoxeldb(String name) {
		VoxelDB voxeldb = voxeldbMap.get(name);
		if(voxeldb != null) {
			return voxeldb;
		}
		return openVoxeldb(name);
	}
	
	public boolean hasVoxeldb(String name) {
		return voxeldbConfigMap.containsKey(name);
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

	private synchronized VoxelDB openVoxeldb(String name) { // guarantee to load each VoxelDB once only
		VoxelDB voxeldb = voxeldbMap.get(name);
		if(voxeldb != null) {
			return voxeldb;
		}
		VoxeldbConfig config = voxeldbConfigMap.get(name);
		if(config==null) {			
			refreshVoxeldbConfigs();
			config = voxeldbConfigMap.get(name);
			if(config==null) {
				throw new PdbException("VoxelDB not found: "+name);
			}
		}
		voxeldb = new VoxelDB(config);
		VoxelDB ret = voxeldbMap.put(name, voxeldb);
		if(ret!=null) {
			throw new PdbException("double load: "+name);
		}
		return voxeldb;
	}

	public synchronized PointCloud getOrCreatePointCloud(String name, String storageType, boolean transactions) {
		PointCloudConfig config = PointCloudConfig.ofPath(pointcloud_root.resolve(name), storageType, transactions);
		pointcloudConfigMap.put(config.name, config);
		return getPointCloud(name);
	}

	public synchronized VoxelDB getOrCreateVoxeldb(String name, String storageType, boolean transactions) {
		VoxeldbConfig config = VoxeldbConfig.ofPath(voxeldb_root.resolve(name), storageType, transactions);
		voxeldbConfigMap.put(config.name, config);
		return getVoxeldb(name);
	}

	public synchronized PointCloud createNewPointCloud(String name, String storageType, boolean transactions) {
		if(pointcloudConfigMap.containsKey(name)) {
			throw new RuntimeException("PointCloud already exist: "+name);
		}
		return getOrCreatePointCloud(name, storageType, transactions);
	}

	public synchronized VoxelDB createNewVoxeldb(String name, String storageType, boolean transactions) {
		if(voxeldbConfigMap.containsKey(name)) {
			throw new RuntimeException("VoxelDB already exist: "+name);
		}
		return getOrCreateVoxeldb(name, storageType, transactions);
	}

	public boolean isAllowedPointCloud(String name, UserIdentity userIdentity) {
		PointCloud pointcloud = pointcloudMap.get(name);
		if(pointcloud != null) {
			return pointcloud.isAllowed(userIdentity);
		}
		PointCloudConfig pointcloudConfig = pointcloudConfigMap.get(name);
		if(pointcloudConfig != null) {
			return pointcloudConfig.isAllowed(userIdentity);
		}
		return AclUtil.isAllowed(userIdentity);
	}
	
	public boolean isAllowedVoxeldb(String name, UserIdentity userIdentity) {
		VoxelDB voxeldb = voxeldbMap.get(name);
		if(voxeldb != null) {
			return voxeldb.isAllowed(userIdentity);
		}
		VoxeldbConfig voxeldbConfig = voxeldbConfigMap.get(name);
		if(voxeldbConfig != null) {
			return voxeldbConfig.isAllowed(userIdentity);
		}
		return AclUtil.isAllowed(userIdentity);
	}

	public Informal getPointCloudInformal(String name) {
		PointCloud pointcloud = pointcloudMap.get(name);
		if(pointcloud != null) {
			return pointcloud.informal();
		}
		PointCloudConfig pointcloudConfig = pointcloudConfigMap.get(name);
		if(pointcloudConfig != null) {
			//Logger.info("readInformal: " + name);
			return pointcloudConfig.readInformal();
		}
		Logger.warn("missing PointCloudConfig: " + name);
		return Informal.EMPTY;
	}

	public Informal getVoxeldbInformal(String name) {
		VoxelDB voxeldb = voxeldbMap.get(name);
		if(voxeldb != null) {
			return voxeldb.informal();
		}
		VoxeldbConfig voxeldbConfig = voxeldbConfigMap.get(name);
		if(voxeldbConfig != null) {
			//Logger.info("readInformal: " + name);
			return voxeldbConfig.readInformal();
		}
		Logger.warn("missing VoxeldbConfig: " + name);
		return Informal.EMPTY;
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

	public VectorDB createVectordb(String name) {
		if(name == null || name.isEmpty()) {
			throw new RuntimeException("no name");
		}
		Util.checkStrictID(name);
		if(!vectordb_root.toFile().exists()) {
			if(!vectordb_root.toFile().mkdirs()) {
				throw new RuntimeException("could not create vectordb root folder");
			}
			Logger.info("created vectordb root folder");
		}
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
		catalog.refreshCatalog();
		return vectordb;
	}

	public synchronized boolean deleteVectordb(String name) {
		Util.checkIDForSafePath(name);
		closeVectordb(name);
		System.gc(); // close GDAL opened files ?
		Path vectordbPath = vectordb_root.resolve(name);
		Util.checkIsParent(vectordb_root, vectordbPath);
		if(Files.exists(vectordbPath)) {
			Logger.info("delete VectorDB: " + name);
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
		Logger.info("update catalog");
		catalog.refreshCatalog();
		return !Files.exists(vectordbPath);
	}

	public synchronized void renameRasterdb(String name, String newName) throws IOException {
		if(!closeRasterdb(name)) {
			throw new RasterDBNotFoundExeption("RasterDB not found: ["+name+"]");
		}
		refreshRasterdbConfigs();
		RasterdbConfig config = rasterdbConfigMap.get(name);
		if(config==null) {
			throw new RasterDBNotFoundExeption("RasterDB not found: ["+ name +"]");
		}
		if(rasterdbConfigMap.containsKey(newName)) {
			throw new RasterDBNotFoundExeption("RasterDB already exists: ["+ newName +"]");
		}
		Path oldPath = config.getPath();		
		Path newPath = rasterdb_root.resolve(newName);
		Logger.info("rename " + oldPath + "  " + newPath);
		Files.move(oldPath, newPath, StandardCopyOption.ATOMIC_MOVE);
		refreshRasterdbConfigs();
		if(!rasterdbConfigMap.containsKey(newName)) {
			throw new RasterDBNotFoundExeption("renamed RasterDB not found: ["+ newName +"]");
		}
	}

	public synchronized void renamePointcloud(String name, String newName) throws IOException {
		if(!closePointcloud(name)) {
			throw new RuntimeException("PointCloud not found: ["+name+"]");
		}
		refreshPointcloudConfigs();
		PointCloudConfig config = pointcloudConfigMap.get(name);
		if(config==null) {
			throw new RuntimeException("PointCloud not found: ["+ name +"]");
		}
		if(pointcloudConfigMap.containsKey(newName)) {
			throw new RuntimeException("PointCloud already exists: ["+ newName +"]");
		}
		Path oldPath = config.path;		
		Path newPath = pointcloud_root.resolve(newName);
		Logger.info("rename " + oldPath + "  " + newPath);
		Files.move(oldPath, newPath, StandardCopyOption.ATOMIC_MOVE);
		refreshPointcloudConfigs();
		if(!pointcloudConfigMap.containsKey(newName)) {
			throw new RuntimeException("renamed PointCloud not found: ["+ newName +"]");
		}
	}

	public synchronized void renameVoxeldb(String name, String newName) throws IOException {
		if(!closeVoxeldb(name)) {
			throw new RuntimeException("Voxeldb not found: ["+name+"]");
		}
		refreshVoxeldbConfigs();
		VoxeldbConfig config = voxeldbConfigMap.get(name);
		if(config==null) {
			throw new RuntimeException("Voxeldb not found: ["+ name +"]");
		}
		if(voxeldbConfigMap.containsKey(newName)) {
			throw new RuntimeException("Voxeldb already exists: ["+ newName +"]");
		}
		Path oldPath = config.path;		
		Path newPath = voxeldb_root.resolve(newName);
		Logger.info("rename " + oldPath + "  " + newPath);
		Files.move(oldPath, newPath, StandardCopyOption.ATOMIC_MOVE);
		refreshVoxeldbConfigs();
		if(!voxeldbConfigMap.containsKey(newName)) {
			throw new RuntimeException("renamed Voxeldb not found: ["+ newName +"]");
		}
	}

	public synchronized void renameVectordb(String name, String newName) throws IOException {
		if(!closeVectordb(name)) {
			throw new RuntimeException("Vectordb not found: ["+name+"]");
		}
		refreshVectordbConfigs();
		VectordbConfig config = vectordbConfigMap.get(name);
		if(config==null) {
			throw new RuntimeException("Vectordb not found: ["+ name +"]");
		}
		if(vectordbConfigMap.containsKey(newName)) {
			throw new RuntimeException("Vectordb already exists: ["+ newName +"]");
		}
		Path oldPath = config.path;		
		Path newPath = vectordb_root.resolve(newName);
		Logger.info("rename " + oldPath + "  " + newPath);
		Files.move(oldPath, newPath, StandardCopyOption.ATOMIC_MOVE);
		refreshVectordbConfigs();
		if(!vectordbConfigMap.containsKey(newName)) {
			throw new RuntimeException("renamed Vectordb not found: ["+ newName +"]");
		}
	}
	
	public AccountManager accountManager() {		
		return accountManager != null ? accountManager : loadAccountManager();
	}

	private AccountManager loadAccountManager() {
		AccountManager a = accountManagerVolatile;
		if(a != null) {
			return a;
		}
		synchronized (accountManagerLoadLock) {
			a = accountManagerVolatile;
			if(a != null) {
				return a;
			}
			a = new AccountManager(Paths.get("accounts.yaml"), Paths.get("realm.properties"));
			accountManagerVolatile = a;
			accountManager = a;
			return a;
		}
	}
	
	public PublicAccessManager publicAccessManager() {		
		return publicAccessManager != null ? publicAccessManager: loadPublicAccessManager();
	}

	private PublicAccessManager loadPublicAccessManager() {
		PublicAccessManager  a = publicAccessManagerVolatile;
		if(a != null) {
			return a;
		}
		synchronized (publicAccessManagerLoadLock) {
			a = publicAccessManagerVolatile;
			if(a != null) {
				return a;
			}
			a = new PublicAccessManager(Paths.get("public_access.yaml"));
			publicAccessManagerVolatile = a;
			publicAccessManager = a;
			return a;
		}
	}
	
	public MetaDataSchema metaDataSchema() {		
		return metaDataSchema != null ? metaDataSchema: loadMetaDataSchema();
	}

	private MetaDataSchema loadMetaDataSchema() {
		MetaDataSchema  a = metaDataSchemaVolatile;
		if(a != null) {
			return a;
		}
		synchronized (metaDataSchemaLoadLock) {
			a = metaDataSchemaVolatile;
			if(a != null) {
				return a;
			}
			a = new MetaDataSchema(Paths.get(brokerConfig.serverConfig.meta_data_schema_file), brokerConfig.serverConfig.meta_data_schema_description);
			metaDataSchemaVolatile = a;
			metaDataSchema = a;
			return a;
		}
	}
	
	public PostgisLayerManager postgisLayerManager() {		
		return postgisLayerManager != null ? postgisLayerManager : loadPostgisLayerManager();
	}

	private PostgisLayerManager loadPostgisLayerManager() {
		PostgisLayerManager p = postgisLayerManagerVolatile;
		if(p != null) {
			return p;
		}
		synchronized (postgisLayerManagerLoadLock) {
			p = postgisLayerManagerVolatile;
			if(p != null) {
				return p;
			}
			p = new PostgisLayerManager(this);
			postgisLayerManagerVolatile = p;
			postgisLayerManager = p;
			return p;
		}
	}
}
