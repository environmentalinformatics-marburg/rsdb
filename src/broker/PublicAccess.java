package broker;

import java.util.LinkedHashMap;


import org.tinylog.Logger;

import util.yaml.YamlMap;

public abstract class PublicAccess {
	

	public static class RasterDbWMS extends PublicAccess {
		public static final String TYPE = "RasterDB_WMS";
		
		public String rasterdb;

		public RasterDbWMS(String id, String rasterdb) {
			super(id, TYPE);
			this.rasterdb = rasterdb;
		}
		
		public static RasterDbWMS ofYAML(String id, YamlMap map) {
			String rasterdb = map.getString("rasterdb");
			return new RasterDbWMS(id, rasterdb);
		}

		@Override
		public LinkedHashMap<String, Object> toMap() {
			LinkedHashMap<String, Object> map = super.toMap();
			map.put("rasterdb", rasterdb);
			return map;
		}
	}
	
	public final String id;
	public final String type;

	public PublicAccess(String id, String type) {
		this.id = id;
		this.type = type;
	}

	public static PublicAccess ofYAML(String id, YamlMap map) {
		String type = map.optString("type", "unknown");
		PublicAccess publicAccess = null;
		switch(type) {
		case RasterDbWMS.TYPE:
			publicAccess = RasterDbWMS.ofYAML(id, map);
			break;
		default:
			Logger.warn("unknown type: " + type);			
		}
		return publicAccess;
	}
	
	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("type", type);
		return map;
	}
}

