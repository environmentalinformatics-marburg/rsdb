package broker;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.function.BiConsumer;

import util.yaml.YamlMap;
import util.yaml.YamlUtil;

public class PublicAccessManager {
	

	private final Path publicAccessPath;
	private HashMap<String, PublicAccess> publicAccessMap;
	private Runnable[] changedListeners;

	public PublicAccessManager(Path publicAccessPath) {
		this.publicAccessPath = publicAccessPath;	
		read();
		changed();
	}

	public synchronized boolean read() {
		HashMap<String, PublicAccess> publicAccessMap = new HashMap<String, PublicAccess>();
		if(publicAccessPath.toFile().exists()) {
			YamlMap yamlMap = YamlUtil.readYamlMap(publicAccessPath);
			YamlMap publicMap = yamlMap.optMap("public");


			publicMap.forEachKey((entryMap, id) -> {
				YamlMap map = entryMap.getMap(id);
				PublicAccess publicAccess = PublicAccess.ofYAML(id, map);
				if(publicAccess != null) {
					publicAccessMap.put(id, publicAccess);
				}
			});
		}
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

	public synchronized void set(PublicAccess publicAccess) {
		HashMap<String, PublicAccess> publicAccessMap = new HashMap<String, PublicAccess>();
		publicAccessMap.putAll(this.publicAccessMap);
		publicAccessMap.put(publicAccess.id, publicAccess);
		this.publicAccessMap = publicAccessMap;
		write();
		changed();
	}

	public synchronized void remove(String id) {
		HashMap<String, PublicAccess> publicAccessMap = new HashMap<String, PublicAccess>();
		publicAccessMap.putAll(this.publicAccessMap);
		publicAccessMap.remove(id);
		this.publicAccessMap = publicAccessMap;
		write();
		changed();
	}

	public synchronized void changeListenerAdd(Runnable listener) {		
		if(changedListeners == null) {
			changedListeners = new Runnable[] {listener};
		} else {
			int len = changedListeners.length;
			Runnable[] cs = new Runnable[len + 1];
			for (int i = 0; i < len; i++) {
				if(changedListeners[i] == listener) {
					return;
				}
				cs[i] = changedListeners[i];
			}
			cs[len] = listener;
			changedListeners = cs;
		}
	}

	public synchronized void changeListenerRemove(Runnable listener) {
		if(changedListeners != null) {
			int len = changedListeners.length;
			Runnable[] cs = new Runnable[len - 1];
			int pos = 0;
			for (int i = 0; i < len; i++) {
				if(changedListeners[i] != listener) {
					if(pos == len) {
						return;
					}
					cs[i] = changedListeners[pos++];
				}				
			}
			changedListeners = cs;
		}
	}

	private void changed() {
		Runnable[] cs = changedListeners;
		if(cs != null) {
			for(Runnable c : cs) {
				c.run();
			}
		}
	}
}
