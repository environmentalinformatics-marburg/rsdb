package voxeldb;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;

import org.eclipse.jetty.server.UserIdentity;
import org.tinylog.Logger;
import org.yaml.snakeyaml.Yaml;

import broker.Informal;
import broker.acl.ACL;
import broker.acl.AclUtil;
import util.yaml.YamlMap;

public class VoxeldbConfig {
	
	public final String name;
	public final Path path;
	public final boolean transaction;
	public final String preferredStorageType; // nullable if pointcloud is existing

	public VoxeldbConfig(String name, Path path, String storageType, boolean transaction) {
		this.name = name;
		this.path = path;
		this.transaction = transaction;
		this.preferredStorageType = storageType;
	}

	public static VoxeldbConfig ofPath(Path path, String storageType, boolean transaction) {
		String name = pathToName(path);
		return new VoxeldbConfig(name, path, storageType, transaction);
	}

	private static String pathToName(Path path) {
		String filename = path.getFileName().toString().trim();
		if(filename.isEmpty()) {
			return path.toString().trim();
		} else {
			return filename;
		}		
	}
	
	public boolean isAllowed(UserIdentity userIdentity) {
		String fileMetaName = "voxeldb.yml";
		Path metaPath = path.resolve(fileMetaName);
		File metaFile = metaPath.toFile();
		try {
			if (metaPath.toFile().exists()) {
				YamlMap map;
				try(InputStream in = new FileInputStream(metaFile)) {
					map = YamlMap.ofObject(new Yaml().load(in));
				}
				ACL acl = ACL.ofRoles(map.optList("acl").asStrings());
				ACL acl_mod = ACL.ofRoles(map.optList("acl_mod").asStrings());
				ACL acl_owner = ACL.ofRoles(map.optList("acl_owner").asStrings());
				return AclUtil.isAllowed(acl_owner, acl_mod, acl, userIdentity);
			}
			return AclUtil.isAllowed(userIdentity);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.warn(e);
			return AclUtil.isAllowed(userIdentity);
		}
	}

	public Informal readInformal() {
		String fileMetaName = "voxeldb.yml";
		Path metaPath = path.resolve(fileMetaName);
		File metaFile = metaPath.toFile();
		try {
			if (metaPath.toFile().exists()) {
				YamlMap yamlMap;
				try(InputStream in = new FileInputStream(metaFile)) {
					yamlMap = YamlMap.ofObject(new Yaml().load(in));
				}
				return Informal.ofYaml(yamlMap);
			} else {
				Logger.warn("missing meta: " + name+ "    " + metaPath);
			}
			return Informal.EMPTY;
		} catch (Exception e) {
			e.printStackTrace();
			Logger.warn(e);
			return Informal.EMPTY;
		}
	}
}
