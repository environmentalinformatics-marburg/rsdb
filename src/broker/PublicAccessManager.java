package broker;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class PublicAccessManager {
	private static final Logger log = LogManager.getLogger();

	private final Path publicAccessPath;
	private HashMap<String, PublicAccess> publicAccessMap;

	public PublicAccessManager(Path publicAccessPath) {
		this.publicAccessPath = publicAccessPath;	
		read();
	}

	public synchronized boolean read() {
		YamlMap yamlMap = YamlUtil.readYamlMap(publicAccessPath);
		YamlMap publicMap = yamlMap.optMap("public");
		HashMap<String, PublicAccess> publicAccessMap = new HashMap<String, PublicAccess>();
		
		publicMap.forEachKey((entryMap, id) -> {
			YamlMap map = entryMap.getMap(id);
			PublicAccess publicAccess = PublicAccess.ofYAML(id, map);
			if(publicAccess != null) {
				publicAccessMap.put(id, publicAccess);
			}
		});
		this.publicAccessMap = publicAccessMap;
		return true;
	}

	public synchronized void write() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		LinkedHashMap<String, Object> publicMap = new LinkedHashMap<String, Object>();
		yamlMap.put("public", publicMap);
		publicAccessMap.forEach((id, publicAccess) -> {
			LinkedHashMap<String, Object> map = publicAccess.toMap();
			publicMap.put(id, map);
		});	
		YamlUtil.writeSafeYamlMap(publicAccessPath, yamlMap);
	}

	public void forEach(BiConsumer<String, PublicAccess> action) {
		publicAccessMap.forEach(action);		
	}
}
