package broker;

import java.util.LinkedHashMap;


import org.tinylog.Logger;

import util.yaml.YamlMap;

public abstract class PublicAccess {

	public static abstract class RasterDbAbstract extends PublicAccess {

		public String rasterdb;

		public RasterDbAbstract(String id, String rasterdb, String type) {
			super(id, type);
			this.rasterdb = rasterdb;
		}

		@Override
		public LinkedHashMap<String, Object> toMap() {
			LinkedHashMap<String, Object> map = super.toMap();
			map.put("rasterdb", rasterdb);
			return map;
		}
	}	

	public static class RasterDbWMS extends RasterDbAbstract {
		public static final String TYPE = "RasterDB_WMS";

		public RasterDbWMS(String id, String rasterdb) {
			super(id, rasterdb, TYPE);
		}

		public static RasterDbWMS ofYAML(String id, YamlMap map) {
			String rasterdb = map.getString("rasterdb");
			return new RasterDbWMS(id, rasterdb);
		}
	}

	public static class RasterDbWCS extends RasterDbAbstract {
		public static final String TYPE = "RasterDB_WCS";

		public RasterDbWCS(String id, String rasterdb) {
			super(id, rasterdb, TYPE);
		}

		public static RasterDbWCS ofYAML(String id, YamlMap map) {
			String rasterdb = map.getString("rasterdb");
			return new RasterDbWCS(id, rasterdb);
		}
	}

	public static abstract class VectorDbAbstract extends PublicAccess {

		public String vectordb;

		public VectorDbAbstract(String id, String vectordb, String type) {
			super(id, type);
			this.vectordb = vectordb;
		}

		@Override
		public LinkedHashMap<String, Object> toMap() {
			LinkedHashMap<String, Object> map = super.toMap();
			map.put("vectordb", vectordb);
			return map;
		}
	}
	
	public static class VectorDbWFS extends VectorDbAbstract {
		public static final String TYPE = "VectorDB_WFS";

		public VectorDbWFS(String id, String vectordb) {
			super(id, vectordb, TYPE);
		}

		public static VectorDbWFS ofYAML(String id, YamlMap map) {
			String vectordb = map.getString("vectordb");
			return new VectorDbWFS(id, vectordb);
		}
	}
	
	public static class VectorDbWMS extends VectorDbAbstract {
		public static final String TYPE = "VectorDB_WMS";

		public VectorDbWMS(String id, String vectordb) {
			super(id, vectordb, TYPE);
		}

		public static VectorDbWMS ofYAML(String id, YamlMap map) {
			String vectordb = map.getString("vectordb");
			return new VectorDbWMS(id, vectordb);
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
		case RasterDbWCS.TYPE:
			publicAccess = RasterDbWCS.ofYAML(id, map);
			break;
		case VectorDbWFS.TYPE:
			publicAccess = VectorDbWFS.ofYAML(id, map);
			break;
		case VectorDbWMS.TYPE:
			publicAccess = VectorDbWMS.ofYAML(id, map);
			break;			
		case "unknown":
			Logger.warn("missing type");
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

