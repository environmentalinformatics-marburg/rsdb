package voxeldb;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.UserIdentity;

import broker.Associated;
import broker.Informal;
import broker.acl.ACL;
import broker.acl.EmptyACL;
import griddb.Attribute;
import griddb.GridDB;
import griddb.GridDB.ExtendedMeta;
import pointcloud.AttributeSelector;
import rasterunit.BandKey;
import rasterunit.Tile;
import rasterunit.TileKey;
import util.Range2d;
import util.collections.ReadonlyNavigableSetView;
import util.yaml.YamlMap;

public class VoxelDB implements AutoCloseable {
	private static final Logger log = LogManager.getLogger();

	private static final int CURRENT_VERSION_MAJOR = 1;
	private static final int CURRENT_VERSION_MINOR = 0;
	private static final int CURRENT_VERSION_PATCH = 0;

	private final GridDB griddb;

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

	public VoxelDB(VoxeldbConfig config) {
		this.config = config;
		griddb = new GridDB(config.path, "voxeldb", new ExtendedMetaHook(), config.preferredStorageType, config.transaction);
		griddb.readMeta();
		griddb.writeMeta();
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
				if (yamlMap.contains("associated")) {
					associated = Associated.ofYaml(yamlMap.getMap("associated"));
				}
				informal = Informal.ofYaml(yamlMap);
				
				
				geoRef = yamlMap.contains("ref") ? VoxelGeoRef.ofYaml(yamlMap.getMap("ref")) : VoxelGeoRef.DEFAULT;
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
			}
		}	
	}

	public void writeTile(Tile tile) throws IOException {
		griddb.writeTile(tile);		
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

	public double getCellsize() {
		return cellsize;
	}

	/**
	 * 
	 * @return range or null
	 */
	public Range2d getCellRange() {
		return griddb.getTileRange();
	}

	public Range2d getCellRangeOfSubset(Range2d subsetCellRange) {
		return griddb.getTileRangeOfSubset(new BandKey(0, 0), subsetCellRange);
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
		acl.check(userIdentity);
	}

	public boolean isAllowedMod(UserIdentity userIdentity) {
		return acl_mod.isAllowed(userIdentity);
	}

	public void checkMod(UserIdentity userIdentity) {
		acl_mod.check(userIdentity);
	}

	public void setACL(ACL acl) {
		this.acl = acl;
		griddb.writeMeta();		
	}

	public void setACL_mod(ACL acl_mod) {
		this.acl_mod = acl_mod;
		griddb.writeMeta();		
	}

	public AttributeSelector getSelector() {
		AttributeSelector selector = new AttributeSelector();
		for(Attribute attribute : griddb.getAttributes()) {
			selector.set(attribute.name);
		}
		return selector;
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
}
