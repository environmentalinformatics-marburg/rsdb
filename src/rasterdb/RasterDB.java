package rasterdb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.UserIdentity;
import org.locationtech.proj4j.CRSFactory;
import org.tinylog.Logger;
import org.yaml.snakeyaml.Yaml;

import broker.Associated;
import broker.Informal;
import broker.TimeSlice;
import broker.TimeSlice.TimeSliceBuilder;
import broker.acl.ACL;
import broker.acl.AclUtil;
import broker.acl.EmptyACL;
import rasterdb.tile.Processing;
import rasterdb.tile.TilePixel;
import rasterunit.RasterUnit;
import rasterunit.RasterUnitStorage;
import rasterunit.TileStorage;
import rasterunit.TileStorageConfig;
import util.Range2d;
import util.Util;
import util.yaml.YamlMap;

public class RasterDB implements AutoCloseable {

	private static final String TYPE = "RasterDB";

	public static final String PYRAMID_TYPE_FILES_DIV4 = "files_div4";
	public static final String PYRAMID_TYPE_COMPACT_DIV2 = "compact_div2";

	public static final String STORAGE_TYPE_TILE_STORAGE = "TileStorage";

	private final ConcurrentSkipListMap<Integer, TimeSlice> timeMap = new ConcurrentSkipListMap<Integer, TimeSlice>();
	public final NavigableMap<Integer, TimeSlice> timeMapReadonly = Collections.unmodifiableNavigableMap(timeMap);

	private final ConcurrentSkipListMap<Integer, Band> bandMap;
	public final  NavigableMap<Integer, Band> bandMapReadonly;

	private final ConcurrentSkipListMap<String, CustomWMS> customWmsMap = new ConcurrentSkipListMap<String, CustomWMS>();
	public final NavigableMap<String, CustomWMS> customWmsMapReadonly = Collections.unmodifiableNavigableMap(customWmsMap);
	
	private final ConcurrentSkipListMap<String, CustomWCS> customWcsMap = new ConcurrentSkipListMap<String, CustomWCS>();
	public final NavigableMap<String, CustomWCS> customWcsMapReadonly = Collections.unmodifiableNavigableMap(customWcsMap);

	private RasterUnitStorage rasterUnit = null;
	private RasterUnitStorage rasterPyr1Unit = null;
	private RasterUnitStorage rasterPyr2Unit = null;
	private RasterUnitStorage rasterPyr3Unit = null;
	private RasterUnitStorage rasterPyr4Unit = null;

	private GeoReference ref = GeoReference.EMPTY_DEFAULT;
	public Associated associated = new Associated();
	private Informal informal = Informal.EMPTY;
	public int version = 1;
	private ACL acl = EmptyACL.ADMIN;
	private ACL acl_mod = EmptyACL.ADMIN;
	private ACL acl_owner = EmptyACL.ADMIN;

	private boolean calcExactLocalRange = true;
	private Range2d local_extent = null;

	private final Path metaPath;
	public final RasterdbConfig config;

	private final Path path;
	private String storageType = null;
	private int tilePixelLen = 0;
	private String pyramidType = null;

	private static final CRSFactory CRS_FACTORY = new CRSFactory();

	public RasterDB(RasterdbConfig config) {
		this.config = config;
		path = config.getPath();
		path.toFile().mkdirs();
		bandMap = new ConcurrentSkipListMap<Integer, Band>();
		bandMapReadonly = Collections.unmodifiableNavigableMap(bandMap);

		this.metaPath = path.resolve("meta.yaml");

		// *** Set config properties for new RasterDB, will be overwritten from meta if RasterDB already exists.
		this.storageType = config.preferredStorageType == null ? STORAGE_TYPE_TILE_STORAGE : config.preferredStorageType;
		this.tilePixelLen = config.preferredTilePixelLen;
		//Logger.info("tilePixelLen " + tilePixelLen);
		this.pyramidType = config.preferredPyramidType;
		// ***

		readMeta(); // possibly overwrite config properties from meta
		//Logger.info("tilePixelLen " + tilePixelLen);
	}

	public synchronized void flush() throws IOException {
		if (rasterUnit != null) {
			rasterUnit.flush();
		}
		if (rasterPyr1Unit != null) {
			rasterPyr1Unit.flush();
		}
		if (rasterPyr2Unit != null) {
			rasterPyr2Unit.flush();
		}
		if (rasterPyr3Unit != null) {
			rasterPyr3Unit.flush();
		}
		if (rasterPyr4Unit != null) {
			rasterPyr4Unit.flush();
		}
		writeMeta();
	}

	@Override
	public synchronized void close() {
		Logger.info("close rasterdb " + config.getName()+" ...");
		try {
			if (rasterUnit != null) {
				//Logger.info("close rasterUnit");
				rasterUnit.close();
				rasterUnit = null;
			}
			if (rasterPyr1Unit != null) {
				rasterPyr1Unit.close();
				rasterPyr1Unit = null;
			}
			if (rasterPyr2Unit != null) {
				rasterPyr2Unit.close();
				rasterPyr2Unit = null;
			}
			if (rasterPyr3Unit != null) {
				rasterPyr3Unit.close();
				rasterPyr3Unit = null;
			}
			if (rasterPyr4Unit != null) {
				rasterPyr4Unit.close();
				rasterPyr4Unit = null;
			}
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		writeMeta();
	}

	public boolean isInternalPyramid() {
		return pyramidType != null && pyramidType.equals(PYRAMID_TYPE_COMPACT_DIV2);
	}

	public void rebuildPyramid(boolean flush) throws IOException {
		getLocalRange(true);

		if(isInternalPyramid()) {
			RasterUnitStorage dstStorage = rasterPyr1Unit();
			dstStorage.removeAllTiles();
			Processing.rebuildPyramid(this, rasterUnit(), dstStorage, 2);
		} else {
			Logger.info("build map 1:4");
			long c1 = Processing.writeStorageDiv(this, rasterUnit(), rasterPyr1Unit(), 4);
			Logger.info("tiles written " + c1);
			Logger.info("build map 1:16");
			long c2 = Processing.writeStorageDiv(this, rasterPyr1Unit(), rasterPyr2Unit(), 4);
			Logger.info("tiles written " + c2);
			Logger.info("build map 1:64");
			long c3 = Processing.writeStorageDiv(this, rasterPyr2Unit(), rasterPyr3Unit(), 4);
			Logger.info("tiles written " + c3);
			Logger.info("build map 1:256");
			long c4 = Processing.writeStorageDiv(this, rasterPyr3Unit(), rasterPyr4Unit(), 4);
			Logger.info("tiles written " + c4);
		}		

		if(flush) {		
			flush();
		}
	}

	public synchronized void writeMeta() {
		try {
			LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
			map.put("type", TYPE);
			map.put("version", version);
			if (ref != null) {
				map.put("ref", ref.toYaml());
			}
			if (!bandMap.isEmpty()) {
				LinkedHashMap<Integer, Object> m = new LinkedHashMap<Integer, Object>();
				for (Band band : bandMap.values()) {
					m.put(band.index, band.toYamlWithoutIndex());
				}
				map.put("bands", m);
			}
			map.put("associated", associated.toYaml());
			map.put("acl", acl.toYaml());
			map.put("acl_mod", acl_mod.toYaml());
			map.put("acl_owner", acl_owner.toYaml());
			informal.writeYaml(map);
			if(local_extent != null) {
				map.put("local_extent", local_extent.toYaml());
			}
			map.put("storage_type", storageType);
			if(pyramidType != null) {
				map.put("pyramid_type", pyramidType);
			}
			map.put("tile_pixel_len", tilePixelLen);
			if(!timeMap.isEmpty()) {
				map.put("time_slices", TimeSlice.timeMapToYaml(timeMap));					
			}
			if(!customWmsMap.isEmpty()) {
				LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
				customWmsMap.forEach((key, customWMS) -> {
					m.put(key, customWMS.toYaml());	
				});
				map.put("custom_wms", m);	
			}
			if(!customWcsMap.isEmpty()) {
				LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
				customWcsMap.forEach((key, customWCS) -> {
					m.put(key, customWCS.toYaml());	
				});
				map.put("custom_wcs", m);	
			}
			Yaml yaml = new Yaml();
			Path writepath = Paths.get(metaPath.toString()+"_temp");
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(writepath.toFile())));
			yaml.dump(map, out);
			out.close();
			Files.move(writepath, metaPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (Exception e) {
			Logger.warn(e);
		}
	}

	public synchronized void readMeta() {
		try {
			ref = GeoReference.EMPTY_DEFAULT;
			associated = new Associated();
			bandMap.clear();
			if (metaPath.toFile().exists()) {
				InputStream in = new FileInputStream(metaPath.toFile());
				YamlMap yamlMap = YamlMap.ofObject(new Yaml().load(in));
				in.close();
				String type = yamlMap.getString("type");
				if (!type.equals(TYPE)) {
					throw new RuntimeException("wrong type: " + type);
				}
				version = yamlMap.optInt("version", version);
				if (yamlMap.contains("ref")) {
					ref = GeoReference.ofYaml(yamlMap.getMap("ref"));
				}
				if (yamlMap.contains("bands")) {
					Map<Number, Object> m = (Map<Number, Object>) yamlMap.getObject("bands");
					for (Entry<Number, Object> entry : m.entrySet()) {
						int index = entry.getKey().intValue();
						YamlMap bm = YamlMap.ofObject(entry.getValue());
						Band band = Band.ofYaml(bm, index);

						bandMap.put(index, band);
					}
				}
				if (yamlMap.contains("associated")) {
					associated = Associated.ofYaml(yamlMap.getMap("associated"));
				}
				acl = ACL.ofRoles(yamlMap.optList("acl").asStrings());
				acl_mod = ACL.ofRoles(yamlMap.optList("acl_mod").asStrings());
				acl_owner = ACL.ofRoles(yamlMap.optList("acl_owner").asStrings());
				informal = Informal.ofYaml(yamlMap);
				if(yamlMap.contains("local_extent")) {
					local_extent = Range2d.ofYaml(yamlMap.getMap("local_extent"));
				} else {
					local_extent = null;
				}
				storageType = yamlMap.optString("storage_type", "RasterUnit"); // Compatibility with old formats
				pyramidType = yamlMap.optString("pyramid_type", PYRAMID_TYPE_FILES_DIV4); // Compatibility with old formats
				tilePixelLen = yamlMap.optInt("tile_pixel_len", 256); // Compatibility with old formats
				timeMap.clear();
				if(yamlMap.contains("time_slices")) {
					TimeSlice.yamlToTimeMap((Map<?, Object>) yamlMap.getObject("time_slices"), timeMap);					
				}
				customWmsMap.clear();
				if(yamlMap.contains("custom_wms")) {
					yamlMap.getMap("custom_wms").forEachKey((yaml, key) -> {
						CustomWMS customWMS = CustomWMS.ofYaml(yaml.getMap(key));
						customWmsMap.put(key, customWMS);
					});
				}
				customWcsMap.clear();
				if(yamlMap.contains("custom_wcs")) {
					yamlMap.getMap("custom_wcs").forEachKey((yaml, key) -> {
						CustomWCS customWCS = CustomWCS.ofYaml(yaml.getMap(key));
						customWcsMap.put(key, customWCS);
					});
				}
			}
			//Logger.info("*** ref *** " + ref + "    of   " + metaPath);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.warn(e);
		}
	}

	public synchronized Band createSpectralBand(int type, double wavelength, double fwhm, String title, String visualisation) {
		if(type == TilePixel.TYPE_SHORT && tilePixelLen != 256) {
			throw new RuntimeException("tile type 1  tile_pixel_len = 256 is supported only");
		}
		if(type == TilePixel.TYPE_FLOAT && tilePixelLen != 256) {
			throw new RuntimeException("tile type 2  tile_pixel_len = 256 is supported only");
		}
		int index = 0;
		for (Band band : bandMap.values()) {
			index = band.index;
		}
		index++;
		Band band = Band.ofSpectralBand(type, index, wavelength, fwhm, title, visualisation);
		Logger.info("create spectral band " + band.index + "  " + band.wavelength + "  " + band.fwhm);
		bandMap.put(index, band);
		writeMeta();
		return band;
	}

	public synchronized Band createBand(int type, String title, String visualisation) {
		if(type == TilePixel.TYPE_SHORT && tilePixelLen != 256) {
			throw new RuntimeException("tile type 1  tile_pixel_len = 256 is supported only");
		}
		if(type == TilePixel.TYPE_FLOAT && tilePixelLen != 256) {
			throw new RuntimeException("tile type 2  tile_pixel_len = 256 is supported only");
		}
		int index = 0;
		for (Band band : bandMap.values()) {
			index = band.index;
		}
		index++;
		Band band = Band.of(type, index, title, visualisation);
		Logger.info("create band " + band.index);
		setBand(band, false);
		return band;
	}

	public synchronized void setBand(Band band, boolean replaceIfExisting) {
		if (bandMap.containsKey(band.index)) {
			if(replaceIfExisting) {
				Logger.warn("replace band" + band);
			} else {
				throw new RuntimeException("band already existing: " + band.toString());
			}
		}
		if(band.type == TilePixel.TYPE_SHORT && tilePixelLen != 256) {
			throw new RuntimeException("tile type 1  tile_pixel_len = 256 is supported only");
		}
		if(band.type == TilePixel.TYPE_FLOAT && tilePixelLen != 256) {
			throw new RuntimeException("tile type 2  tile_pixel_len = 256 is supported only");
		}
		bandMap.put(band.index, band);
		writeMeta();
	}

	public synchronized Band removeBand(int band_number) {
		Band removedBand = bandMap.remove(band_number);
		writeMeta();
		return removedBand; 
	}

	public synchronized void invalidateLocalRange() {
		local_extent = null;
		writeMeta();
	}

	/**
	 * 
	 * @param update
	 * @return range or null
	 */
	public synchronized Range2d getLocalRange(boolean update) {
		if(local_extent == null || update) {
			synchronized(this) {
				if(local_extent == null || update) {
					if(calcExactLocalRange) {
						Range2d localRangeUpdated = LocalExtentCalculator.calc(this);
						if(localRangeUpdated == null) {
							if(local_extent != null) {
								local_extent = null;
								writeMeta();
							}
						} else if(!localRangeUpdated.equals(local_extent)) {
							local_extent = localRangeUpdated;
							writeMeta();
						}
					} else {
						Range2d tileRange = rasterUnit().getTileRange2d();
						if(tileRange == null) {
							if(local_extent != null) {
								local_extent = null;
								writeMeta();
							}
						} else {
							Range2d localRangeUpdated = tileRange.mul(TilePixel.PIXELS_PER_ROW).add(0, 0, TilePixel.PIXELS_PER_ROW_1, TilePixel.PIXELS_PER_ROW_1);
							if(!localRangeUpdated.equals(local_extent)) {
								local_extent = localRangeUpdated;
								writeMeta();
							}
						}
					}
				}
			}
		}
		return local_extent;
	}

	public void setPixelSize(double pixel_size_x, double pixel_size_y, double offset_x, double offset_y) {
		ref = ref.withPixelSize(pixel_size_x, pixel_size_y, offset_x, offset_y);
		writeMeta();
	}

	public void setCode(String code) {
		ref = ref.withCode(code, GeoReference.code_wms_transposed.contains(code));
		Logger.info(ref + "   " + GeoReference.code_wms_transposed.contains(code));
		writeMeta();
	}

	public void setRef(GeoReference ref) {
		this.ref = ref;
		writeMeta();
	}

	public void setAssociatedPointDB(String pointdb) {
		associated.setPointDB(pointdb);
		writeMeta();
	}

	public void setAssociatedPointCloud(String pointcloud) {
		associated.setPointCloud(pointcloud);
		writeMeta();
	}

	public void setAssociatedVoxelDB(String voxeldb) {
		associated.setVoxelDB(voxeldb);
		writeMeta();
	}

	public void setAssociatedPoiGroups(List<String> poi_groups) {
		associated.setPoi_groups(poi_groups);
		writeMeta();
	}

	public void setAssociatedRoiGroups(List<String> roi_groups) {
		associated.setRoi_groups(roi_groups);
		writeMeta();
	}

	public void setProj4(String proj4) {
		Logger.info("setProj4 " + proj4);
		ref = ref.withProj4(proj4);
		Logger.info(ref);
		writeMeta();
		if(!ref.has_code() && ref.has_proj4()) {
			try {
				Logger.info("try get EPSG from PROJ4 '" + ref.proj4 + "'");
				String epsg = CRS_FACTORY.readEpsgFromParameters(ref.proj4);
				if(epsg != null) {
					String c = "EPSG:" + epsg;
					Logger.info("EPSG from PROJ4 '" + ref.proj4 + "' -> " + c);
					setCode(c);
				} else {
					Logger.info("EPSG from PROJ4 not found '" + ref.proj4 + "' -> ");
				}
			} catch(Exception e) {
				Logger.warn(e);
			}
		}
	}

	public GeoReference ref() {
		return ref;
	}

	public Band getBandByTitle(String title) {
		for (Band band : bandMap.values()) {
			if (band.has_title() && band.title.equals(title)) {
				return band;
			}
		}
		return null;
	}

	public Band getBandByNumber(int band_number) {
		return bandMap.get(band_number);
	}

	public Band getBandByNumberThrow(int band_number) {
		Band band = bandMap.get(band_number);
		if(band == null) {
			throw new RuntimeException("band number not fount: " + band_number);
		}
		return band;
	}
	
	public TimeBand getTimeBand(int timestamp, int bandIndex) {
		Band band = getBandByNumber(bandIndex);		
		return band == null ? null : new TimeBand(timestamp, band);		
	}
	
	public void getTimeBand() {
		
	}

	public Informal informal() {
		return informal;
	}

	public void setInformal(Informal informal) {
		this.informal = informal;
		writeMeta();
	}

	public ACL getACL() {
		return acl;
	}

	public ACL getACL_mod() {
		return acl_mod;
	}
	
	public ACL getACL_owner() {
		return acl_owner;
	}

	public void setACL(ACL acl) {
		this.acl = acl;
		writeMeta();	
	}

	public void setACL_mod(ACL acl_mod) {
		this.acl_mod = acl_mod;
		writeMeta();	
	}
	
	public void setACL_owner(ACL acl_owner) {
		this.acl_owner = acl_owner;
		writeMeta();	
	}

	public boolean isAllowed(UserIdentity userIdentity) {
		return AclUtil.isAllowed(acl_owner, acl_mod, acl, userIdentity);
	}

	public void check(UserIdentity userIdentity) {
		AclUtil.check(acl_owner, acl_mod, acl, userIdentity, "rasterdb " + this.config.getName() + " read");
	}

	public void check(UserIdentity userIdentity, String location) {
		AclUtil.check(acl_owner, acl_mod, acl, userIdentity, "rasterdb " + this.config.getName() + " read" + " at " + location);
	}

	public boolean isAllowedMod(UserIdentity userIdentity) {
		return AclUtil.isAllowed(acl_owner, acl_mod, userIdentity);
	}

	public void checkMod(UserIdentity userIdentity) {
		AclUtil.check(acl_owner, acl_mod, userIdentity, "rasterdb " + this.config.getName() + " modify");
	}

	public void checkMod(UserIdentity userIdentity, String location) {
		AclUtil.check(acl_owner, acl_mod, userIdentity, "rasterdb " + this.config.getName() + " modify" + " at " + location);
	}
	
	public boolean isAllowedOwner(UserIdentity userIdentity) {
		return AclUtil.isAllowed(acl_owner, userIdentity);
	}

	public void checkOwner(UserIdentity userIdentity) {
		AclUtil.check(acl_owner, userIdentity, "rasterdb " + this.config.getName() + " owner");
	}

	public void checkOwner(UserIdentity userIdentity, String location) {
		AclUtil.check(acl_owner, userIdentity, "rasterdb " + this.config.getName() + " owner" + " at " + location);
	}

	public RasterUnitStorage rasterUnit() {
		RasterUnitStorage r = rasterUnit;
		return r == null ? loadRasterUnit() : r;
	}

	public RasterUnitStorage rasterPyr1Unit() {
		RasterUnitStorage r = rasterPyr1Unit;
		return r == null ? loadRasterPyr1Unit() : r;
	}

	public RasterUnitStorage rasterPyr2Unit() {
		RasterUnitStorage r = rasterPyr2Unit;
		return r == null ? loadRasterPyr2Unit() : r;
	}

	public RasterUnitStorage rasterPyr3Unit() {
		RasterUnitStorage r = rasterPyr3Unit;
		return r == null ? loadRasterPyr3Unit() : r;
	}

	public RasterUnitStorage rasterPyr4Unit() {
		RasterUnitStorage r = rasterPyr4Unit;
		return r == null ? loadRasterPyr4Unit() : r;
	}

	private RasterUnitStorage openStorage(String name) {
		switch(storageType) {
		case "RasterUnit":
			return new RasterUnit(path, name, config.is_fast_unsafe_import()); // 4^0 = 1
		case "TileStorage":
			TileStorageConfig tileStorageConfig = new TileStorageConfig(path, name);
			try {
				return new TileStorage(tileStorageConfig);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		default:
			throw new RuntimeException("unknown storage_type: |" + storageType + "|");
		}		
	}

	private synchronized RasterUnitStorage loadRasterUnit() {
		if (rasterUnit == null) {
			rasterUnit = openStorage("raster"); // 4^0 = 1			
		}
		return rasterUnit;
	}

	private synchronized RasterUnitStorage loadRasterPyr1Unit() {
		if (rasterPyr1Unit == null) {
			rasterPyr1Unit = openStorage("raster1"); // 4^1 = 4
		}
		return rasterPyr1Unit;
	}

	private synchronized RasterUnitStorage loadRasterPyr2Unit() {
		if (rasterPyr2Unit == null) {
			rasterPyr2Unit = openStorage("raster2"); // 4^2 = 16
		}
		return rasterPyr2Unit;
	}

	private synchronized RasterUnitStorage loadRasterPyr3Unit() {
		if (rasterPyr3Unit == null) {
			rasterPyr3Unit = openStorage("raster3"); // 4^3 = 64
		}
		return rasterPyr3Unit;
	}

	private synchronized RasterUnitStorage loadRasterPyr4Unit() {
		if (rasterPyr4Unit == null) {
			rasterPyr4Unit = openStorage("raster4"); // 4^4 = 256
		}
		return rasterPyr4Unit;
	}

	public synchronized boolean hasRasterUnit() {
		switch(storageType) {
		case "RasterUnit":
			return Files.exists(path.resolve("raster"));
		case "TileStorage":
			return Files.exists(path.resolve("raster.idx"));
		default:
			throw new RuntimeException("unknown storage_type: |" + storageType + "|");
		}	

	}

	public int getTilePixelLen() {
		return tilePixelLen;
	}

	public synchronized void setTimeSlice(TimeSlice timeSlice) {
		timeMap.put(timeSlice.id, timeSlice);
		writeMeta();
	}

	public synchronized void setTimeSlices(Iterable<TimeSlice> timeSlices) {
		for(TimeSlice timeSlice:timeSlices) {
			if(timeSlice != null) {
				timeMap.put(timeSlice.id, timeSlice);
			}
		}
		writeMeta();
	}

	public synchronized TimeSlice getTimeSliceByName(String name) {
		for(TimeSlice value : timeMap.values()) {
			if(value.hasName() && value.name.equals(name)) {
				return value;
			}
		}
		return null;
	}

	public synchronized TimeSlice getOrCreateTimeSliceByName(String name) {
		TimeSlice timeSlice = getTimeSliceByName(name);
		return timeSlice == null ? createTimeSlice(new TimeSliceBuilder(name)) : timeSlice;
	}

	public synchronized TimeSlice createTimeSlice(TimeSliceBuilder timeSliceBuilder) {
		int id = 0;
		while(timeMap.containsKey(id)) {
			id++;
		}
		TimeSlice timeSlice = new TimeSlice(id, timeSliceBuilder);
		timeMap.put(timeSlice.id, timeSlice);
		writeMeta();
		return timeSlice;
	}

	public void removeTimeSlice(int timeSlice) {
		timeMap.remove(timeSlice);
		writeMeta();
	}

	public void setAssociated(Associated associated) {
		this.associated = associated;
		writeMeta();
	}

	public static boolean isValidPyramidTypeString(String pyramidType) {
		if(pyramidType == null || pyramidType.isEmpty()) {
			return false;
		}
		if(PYRAMID_TYPE_FILES_DIV4.equals(pyramidType) || PYRAMID_TYPE_COMPACT_DIV2.equals(pyramidType)) {
			return true;
		}
		return false;
	}

	/*public synchronized void unsafeSetPyramidType(String pyramidType) {
		if(isValidPyramidTypeString(pyramidType)) {
			this.pyramidType = pyramidType;
			writeMeta();
		} else {
			throw new RuntimeException("unknown pyramid_type: " + pyramidType);
		}		
	}*/

	public synchronized void setCustomWMS(Map<String, CustomWMS> map) {
		customWmsMap.clear();
		customWmsMap.putAll(map);
		writeMeta();
	}
	
	public synchronized void setCustomWCS(Map<String, CustomWCS> map) {
		customWcsMap.clear();
		customWcsMap.putAll(map);
		writeMeta();
	}

	public List<Path> getAttachmentFilenames() {		
		File[] fullfiles = this.config.getAttachmentFolderPath().toFile().listFiles();
		if(fullfiles == null) {
			//throw new RuntimeException("data directory does not exist");
			//Logger.warn("attachment directory does not exist");
			return java.util.Collections.emptyList();
		}
		List<Path> files = Arrays.stream(fullfiles).map(file -> {
			//return file.toPath();//.relativize(config.dataPath);
			return this.config.getAttachmentFolderPath().relativize(file.toPath());
		}).collect(Collectors.toList());
		return files;		
	}

	public Path getAttachmentFilePath(String targetFilename) {
		List<Path> filenames = getAttachmentFilenames();
		Path filenamePath = null; 
		for(Path filename : filenames) {
			if(filename.toString().equals(targetFilename)) {
				filenamePath = filename;
				break;
			}				
		}
		if(filenamePath == null) {
			throw new RuntimeException("file not found");
		}
		Path root = config.getAttachmentFolderPath();
		Path filePath = root.resolve(filenamePath);
		return filePath;
	}

	public void removeAttachmentFile(String filename) throws IOException {
		Path path = getAttachmentFilePath(filename);
		Util.safeDeleteIfExists(this.config.getAttachmentFolderPath(), path);
	}
}
