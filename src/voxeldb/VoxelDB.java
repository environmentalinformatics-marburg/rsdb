package voxeldb;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;


import org.tinylog.Logger;
import org.eclipse.jetty.server.UserIdentity;

import broker.Associated;
import broker.Informal;
import broker.TimeSlice;
import broker.TimeSlice.TimeSliceBuilder;
import broker.acl.ACL;
import broker.acl.EmptyACL;
import griddb.Encoding;
import griddb.GridDB;
import griddb.GridDB.ExtendedMeta;
import rasterunit.TileKey;
import util.Range3d;
import util.collections.ReadonlyNavigableSetView;
import util.yaml.YamlMap;

/**
cell.x -> voxelcell.z
<br>
cell.y -> voxelcell.x
<br>
cell.b -> voxelcell.y
<br>
<br>
voxelcell.z -> cell.x
<br>
voxelcell.x -> cell.y
<br>
voxelcell.y -> cell.b
 **/
public class VoxelDB implements AutoCloseable {
	

	private static final int CURRENT_VERSION_MAJOR = 1;
	private static final int CURRENT_VERSION_MINOR = 0;
	private static final int CURRENT_VERSION_PATCH = 0;

	private final GridDB griddb;

	private final ConcurrentSkipListMap<Integer, TimeSlice> timeMap = new ConcurrentSkipListMap<Integer, TimeSlice>();
	public final NavigableMap<Integer, TimeSlice> timeMapReadonly = Collections.unmodifiableNavigableMap(timeMap);

	private int cellsize = 100;
	private ACL acl = EmptyACL.ADMIN;
	private ACL acl_mod = EmptyACL.ADMIN;
	private Associated associated = new Associated();
	private Informal informal = Informal.EMPTY;
	private int version_major = CURRENT_VERSION_MAJOR;
	private int version_minor = CURRENT_VERSION_MINOR;
	private int version_patch = CURRENT_VERSION_PATCH;

	public final VoxeldbConfig config;

	private VoxelGeoRef geoRef = VoxelGeoRef.DEFAULT;

	private volatile Range3d local_range = null;

	public VoxelDB(VoxeldbConfig config) {
		this.config = config;
		griddb = new GridDB(config.path, "voxeldb", new ExtendedMetaHook(), config.preferredStorageType, config.transaction);
		griddb.readMeta();
		loadAttributes();
		griddb.writeMetaIfNeeded();
	}

	private void loadAttributes() {
		griddb.getOrAddAttribute("count", Encoding.ENCODING_INT32);
	}

	public GridDB getGriddb() {
		return griddb;
	}

	private class ExtendedMetaHook implements ExtendedMeta {

		private static final String TYPE = "voxeldb";

		@Override
		public void read(YamlMap yamlMap) {
			synchronized (griddb) {
				String type = yamlMap.getString("type");
				if (!type.equals(TYPE)) {
					throw new RuntimeException("wrong type: " + type);
				}
				String versionText = yamlMap.getString("version");
				String[] versionParts = versionText.split("\\.");
				if(versionParts.length < 1 || versionParts.length > 3) {
					throw new RuntimeException("version error");
				} else if(versionParts.length == 1) {
					version_major = Integer.parseInt(versionParts[0]);
					version_minor = 0;
					version_patch = 0;
				} else if(versionParts.length == 2) {
					version_major = Integer.parseInt(versionParts[0]);
					version_minor = Integer.parseInt(versionParts[1]);
					version_patch = 0;
				} else if(versionParts.length == 3) {
					version_major = Integer.parseInt(versionParts[0]);
					version_minor = Integer.parseInt(versionParts[1]);
					version_patch = Integer.parseInt(versionParts[2]);
				}

				cellsize = yamlMap.getInt("cellsize");

				acl = ACL.of(yamlMap.optList("acl").asStrings());
				acl_mod = ACL.of(yamlMap.optList("acl_mod").asStrings());
				associated = new Associated();
				if(yamlMap.contains("associated")) {
					associated = Associated.ofYaml(yamlMap.getMap("associated"));
				}
				informal = Informal.ofYaml(yamlMap);				
				geoRef = yamlMap.contains("ref") ? VoxelGeoRef.ofYaml(yamlMap.getMap("ref")) : VoxelGeoRef.DEFAULT;
				timeMap.clear();
				if(yamlMap.contains("time_slices")) {
					TimeSlice.yamlToTimeMap((Map<?, Object>) yamlMap.getObject("time_slices"), timeMap);					
				}				
				if(yamlMap.contains("local_range")) {
					local_range = Range3d.ofYaml(yamlMap.getMap("local_range"));
				} else {
					local_range = null;
				}
			}
		}	

		@Override
		public synchronized void write(LinkedHashMap<String, Object> map) {
			synchronized (griddb) {
				map.put("type", TYPE);
				String version_text = version_major + "." + version_minor + "." + version_patch;
				map.put("version", version_text);
				map.put("cellsize", cellsize);
				map.put("acl", acl.toYaml());
				map.put("acl_mod", acl_mod.toYaml());
				map.put("associated", associated.toYaml());
				informal.writeYaml(map);
				map.put("ref", geoRef.toYaml());
				if(!timeMap.isEmpty()) {
					map.put("time_slices", TimeSlice.timeMapToYaml(timeMap));					
				}
				if(local_range != null) {
					map.put("local_range", local_range.toYaml());
				}
			}
		}	
	}

	public void commit() {
		griddb.commit();
	}

	@Override
	public void close() {
		try {
			griddb.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	public int getCellsize() {
		return cellsize;
	}

	public File getMetaFile() {
		return griddb.metaFile;
	}

	public String getName() {
		return config.name;
	}

	public ACL getACL() {
		return acl;
	}

	public ACL getACL_mod() {
		return acl_mod;
	}

	public ReadonlyNavigableSetView<TileKey> getTileKeys() {
		return griddb.getTileKeys();
	}

	public boolean isEmpty() {
		return griddb.isEmpty();
	}

	public boolean isAllowed(UserIdentity userIdentity) {
		return acl.isAllowed(userIdentity);
	}

	public void check(UserIdentity userIdentity) {
		acl.check(userIdentity, "voxeldb " + this.getName() + " read");
	}
	
	public void check(UserIdentity userIdentity, String location) {
		acl.check(userIdentity, "voxeldb " + this.getName() + " read" + " at " + location);
	}

	public boolean isAllowedMod(UserIdentity userIdentity) {
		return acl_mod.isAllowed(userIdentity);
	}

	public void checkMod(UserIdentity userIdentity) {
		acl_mod.check(userIdentity, "voxeldb " + this.getName() + " modify");
	}
	
	public void checkMod(UserIdentity userIdentity, String location) {
		acl_mod.check(userIdentity, "voxeldb " + this.getName() + " modify" + " at " + location);
	}

	public void setACL(ACL acl) {
		this.acl = acl;
		griddb.writeMeta();		
	}

	public void setACL_mod(ACL acl_mod) {
		this.acl_mod = acl_mod;
		griddb.writeMeta();		
	}

	public Associated getAssociated() {
		return associated;
	}

	public Informal informal() {
		return informal;
	}

	public void setInformal(Informal informal) {
		this.informal = informal;
		griddb.writeMeta();
	}

	public void setAssociatedRasterDB(String name) {
		associated.setRasterDB(name);
		griddb.writeMeta();
	}

	public void setAssociatedPoiGroups(List<String> poi_groups) {
		associated.setPoi_groups(poi_groups);
		griddb.writeMeta();
	}

	public void setAssociatedRoiGroups(List<String> roi_groups) {
		associated.setRoi_groups(roi_groups);
		griddb.writeMeta();
	}

	public void commitMeta() {
		griddb.writeMeta();
	}

	public boolean isVersion(int version_major, int version_minor, int version_patch) {
		return this.version_major == version_major && this.version_minor == version_minor && this.version_patch == version_patch;
	}

	public boolean isVersionOrNewer(int version_major, int version_minor, int version_patch) {
		if(this.version_major < version_major) {
			return false;
		}
		if(this.version_minor < version_minor) {
			return false;
		}
		if(this.version_patch < version_patch) {
			return false;
		}
		return true;
	}

	public VoxelGeoRef geoRef() {
		return geoRef;
	}

	public void setProj4(String proj4) {		
		geoRef = geoRef.withProj4(proj4);
		griddb.writeMeta();
	}

	public void setEpsg(int epsg) {
		geoRef = geoRef.withEpsg(epsg);
		griddb.writeMeta();
	}

	public boolean trySetCellsize(int cellsize) {
		synchronized (griddb) {
			if(griddb.isEmpty()) {
				this.cellsize = cellsize;
				griddb.writeMeta();
				return true;
			} else {
				return false;
			}
		}		
	}

	public boolean trySetVoxelsize(double voxelsize) {
		synchronized (griddb) {
			if(griddb.isEmpty()) {
				geoRef = geoRef.withVoxelSize(voxelsize);
				griddb.writeMeta();
				return true;
			} else {
				return false;
			}
		}		
	}

	public boolean trySetOrigin(double originX, double originY, double originZ) {
		synchronized (griddb) {
			if(griddb.isEmpty()) {
				geoRef = geoRef.withOrigin(originX, originY, originZ);
				griddb.writeMeta();
				return true;
			} else {
				return false;
			}
		}			
	}

	public void setTimeSlice(TimeSlice timeslice) {
		synchronized (griddb) {
			timeMap.put(timeslice.id, timeslice);
			griddb.writeMeta();
		}
	}

	public TimeSlice addTimeSlice(TimeSliceBuilder timeSliceBuilder) {
		synchronized (griddb) {
			int id = 0;
			while(timeMap.containsKey(id)) {
				id++;
			}
			TimeSlice timeSlice = new TimeSlice(id, timeSliceBuilder);
			timeMap.put(timeSlice.id, timeSlice);
			griddb.writeMeta();
			return timeSlice;
		}
	}

	public TimeSlice getTimeSliceByName(String name) {
		if(name == null || name.isEmpty()) {
			return null;
		}
		for(TimeSlice timeSlice : timeMap.values()) {
			if(timeSlice.hasName() && timeSlice.name.equals(name)) {
				return timeSlice;
			}
		}
		return null;
	}

	public Range3d getLocalRange(boolean update) {
		synchronized(griddb) {
			if(local_range == null || update) {
				LocalRangeCalculator localRangeCalculator = new LocalRangeCalculator(this);
				try {
					local_range = localRangeCalculator.calc();
					griddb.writeMeta();
				} catch (IOException e) {
					Logger.error(e);
				}
			}
			return local_range;	
		}
	}

	public void invalidateLocalRange(boolean writeMetaIfNeeded) {
		if(local_range != null) {
			synchronized(griddb) {
				if(local_range != null) {
					local_range = null;
					griddb.setUnsavedMetaChanges();
				}
			}
		}
		if(writeMetaIfNeeded) {
			griddb.writeMetaIfNeeded();
		}
	}
}
