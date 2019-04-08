package broker;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import broker.group.ExternalGroupConfig;
import pointdb.base.PointdbConfig;
import util.collections.ReadonlyList;
import util.collections.vec.Vec;

public class BrokerConfig {

	protected Map<String, PointdbConfig> pointdbMap = new TreeMap<String, PointdbConfig>();
	protected TreeMap<String, ExternalGroupConfig> poiGroupMap = new TreeMap<String, ExternalGroupConfig>();
	protected TreeMap<String, ExternalGroupConfig> roiGroupMap = new TreeMap<String, ExternalGroupConfig>();
	protected ServerConfig serverConfig = new ServerConfig();
	protected Vec<JwsConfig> jwsConfigs = new Vec<JwsConfig>();

	public ServerConfig server() {
		return serverConfig;
	}
	
	public ReadonlyList<JwsConfig> jws() {
		return jwsConfigs.readonlyView();
	}

	public Map<String, ExternalGroupConfig> poiGroupMap() {
		return Collections.unmodifiableMap(poiGroupMap);
	}
	
	public Map<String, ExternalGroupConfig> roiGroupMap() {
		return Collections.unmodifiableMap(roiGroupMap);
	}

	public Map<String, PointdbConfig> pointdbMap() {
		return Collections.unmodifiableMap(pointdbMap);
	}
}
