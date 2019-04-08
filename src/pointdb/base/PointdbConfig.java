package pointdb.base;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.UserIdentity;

import broker.Informal;
import broker.acl.ACL;
import broker.acl.EmptyACL;
import util.collections.vec.ReadonlyVecView;
import util.collections.vec.Vec;
import util.yaml.YamlMap;

public class PointdbConfig {
	private static final Logger log = LogManager.getLogger();

	private LinkedHashSet<Path> importDirectories = new LinkedHashSet<Path>();
	private LinkedHashSet<Path> importFiles = new LinkedHashSet<Path>();
	private Path dbDirectory = Paths.get("db");
	private String tileDbFilename = "pointdb";
	private String tileMetaDbFilename = "metadb";
	private boolean transactions;
	private String proj4 = "";
	private int epsg = 0;
	private Vec<String> poiGroupNames = new Vec<String>();
	private Vec<String> roiGroupNames = new Vec<String>();
	private boolean classified_ground = false;
	private boolean classified_vegetation = false;
	private ACL acl = EmptyACL.ADMIN;
	private Informal informal = Informal.EMPTY;
	private String rasterdb = "";


	public String name;

	public PointdbConfig(String name, String db) {
		this.name = name;
		this.dbDirectory = Paths.get(db);
	}

	public static PointdbConfig ofYAML(YamlMap yamlMap) {
		String name = yamlMap.getString("name");
		String db = yamlMap.getString("db");
		PointdbConfig pc = new PointdbConfig(name, db);
		for(String dir:yamlMap.optList("import").asStrings()) {
			pc.importDirectories.add(Paths.get(dir));
		}
		pc.poiGroupNames.addAll(yamlMap.optList("poi").asStrings());
		pc.roiGroupNames.addAll(yamlMap.optList("roi").asStrings());
		if(yamlMap.contains("rasterdb")) {
			pc.rasterdb = yamlMap.getString("rasterdb");
		}
		yamlMap.optFunString("proj4", s->pc.proj4 = s);
		yamlMap.optFunInt("epsg", i->pc.epsg = i);
		for(String c:yamlMap.optList("classified").asStrings()) {
			String cc = c.toLowerCase();
			switch(cc) {
			case "true":
				pc.classified_ground = true;
				pc.classified_vegetation = true;
				break;
			case "ground":
				pc.classified_ground = true;
				break;
			case "vegetation":
				pc.classified_vegetation = true;
				break;
			default:
				log.warn("unknown classification: " + c + "  in "+yamlMap);
			}
		}
		//yamlMap.optFunBoolean("classified", c->{pc.classified_ground = c; pc.classified_vegetation = c;});
		pc.acl = ACL.of(yamlMap.optList("acl").asStrings());
		pc.informal = Informal.ofYaml(yamlMap);
		return pc;
	}

	public Set<Path> getImportDirectories() {
		return Collections.unmodifiableSet(importDirectories);
	}

	public Set<Path> getImportFiles() {
		return Collections.unmodifiableSet(importFiles);
	}

	public Path getDbPath() {
		return dbDirectory;
	}

	public String getTileDbFullPath() {
		return dbDirectory.toString()+'/'+tileDbFilename;
	}

	public String getTileMetaDbFullPath() {
		return dbDirectory.toString()+'/'+tileMetaDbFilename;
	}

	public boolean isTransactions() {
		return transactions;
	}

	public PointdbConfig disableTransactions() {
		this.transactions = false;
		return this;
	}

	public PointdbConfig setTileDbFilename(String filename) {
		tileDbFilename = filename;
		return this;
	}

	public PointdbConfig setTileMetaDbFilename(String filename) {
		tileMetaDbFilename = filename;
		return this;
	}

	@Override
	public String toString() {
		return "PointdbConfig [importDirectories=" + importDirectories + ", importFiles=" + importFiles
				+ ", dbDirectory=" + dbDirectory + ", tileDbFilename=" + tileDbFilename + ", tileMetaDbFilename="
				+ tileMetaDbFilename + ", transactions=" + transactions + ", name=" + name + "]";
	}

	public String getProj4() {
		return proj4;
	}

	public ReadonlyVecView<String> getPoiGroupNames() {
		return poiGroupNames.readonlyView();
	}

	public ReadonlyVecView<String> getRoiGroupNames() {
		return roiGroupNames.readonlyView();
	}

	public String getIndexedStorageFilename() {
		return dbDirectory.toString()+'/'+"pointdb_indexed";
	}

	public int getEPSG() {
		return epsg;
	}

	public boolean isClassified_ground() {
		return classified_ground;
	}

	public boolean isClassified_vegetation() {
		return classified_vegetation;
	}

	public ACL getAcl() {
		return acl;
	}

	public void check(UserIdentity userIdentity) {
		acl.check(userIdentity);
	}

	public boolean isAllowed(UserIdentity userIdentity) {
		return acl.isAllowed(userIdentity);
	}

	public Informal informal() {
		return informal;
	}
	
	public boolean hasRasterDB() {
		return !rasterdb.isEmpty();
	}
	
	public String getRasterDB() {
		return rasterdb;
	}
}
