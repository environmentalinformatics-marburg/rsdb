package postgis;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;

import org.eclipse.jetty.server.UserIdentity;
import org.tinylog.Logger;
import org.yaml.snakeyaml.Yaml;

import broker.acl.ACL;
import broker.acl.AclUtil;
import util.Util;
import util.yaml.YamlMap;

public class PostgisLayerConfig extends PostgisLayerBase {

	private YamlMap map = null;

	public PostgisLayerConfig(String name, Path path) {
		super(name, path);
	}

	private static String pathToName(Path path) {
		String filename = path.getFileName().toString().trim();
		if(filename.isEmpty()) {
			return path.toString().trim();
		} else {
			return filename;
		}		
	}

	public static PostgisLayerConfig ofPath(Path path) {
		String name = pathToName(path);
		name = name.substring(0, name.length() - 5);
		if(name.isBlank()) {
			throw new RuntimeException("empty name");
		}
		Util.checkStrictDotID(name);
		Logger.info(name);
		return new PostgisLayerConfig(name, path);
	}

	@Override
	public boolean isAllowed(UserIdentity userIdentity) {
		if(userIdentity == null) {
			return true;
		}
		
		if(this.map == null) {
			File file = metaPath.toFile();
			try {
				if (file.exists()) {
					try(InputStream in = new FileInputStream(file)) {
						Object yml = new Yaml().load(in);
						if(yml != null) {
							this.map = YamlMap.ofObject(yml);
						}
					}

				}
				//return AclUtil.isAllowed(userIdentity);
			} catch (Exception e) {
				e.printStackTrace();
				Logger.warn(e);
				//return AclUtil.isAllowed(userIdentity);
			}
		}

		if(this.map != null) {
			ACL acl = ACL.ofRoles(this.map.optList("acl").asStrings());
			ACL acl_mod = ACL.ofRoles(this.map.optList("acl_mod").asStrings());
			ACL acl_owner = ACL.ofRoles(this.map.optList("acl_owner").asStrings());
			return AclUtil.isAllowed(acl_owner, acl_mod, acl, userIdentity);
		} else {
			Logger.warn("missing");
			return AclUtil.isAllowed(userIdentity);
		}
	}
}
