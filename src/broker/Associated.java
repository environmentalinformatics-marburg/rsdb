package broker;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONWriter;

import util.JsonUtil;
import util.yaml.YamlMap;

public class Associated {	

	private String pointdb = "";
	private String pointcloud = "";
	private String rasterdb = "";

	private List<String> poi_groups = java.util.Collections.EMPTY_LIST;
	private List<String> roi_groups = java.util.Collections.EMPTY_LIST;

	public void setPointDB(String name) {
		this.pointdb = name;
	}

	public String getPointDB() {
		return pointdb;
	}

	public boolean hasPointDB() {
		return !pointdb.isEmpty();
	}

	public void setPointCloud(String name) {
		this.pointcloud = name;
	}

	public String getPointCloud() {
		return pointcloud;
	}
	
	public void setRasterDB(String name) {
		this.rasterdb = name;
	}
	
	public String getRasterDB() {
		return rasterdb;
	}

	public boolean hasPointCloud() {
		return !pointcloud.isEmpty();
	}
	
	public boolean hasRasterDB() {
		return !rasterdb.isEmpty();
	}

	public void setPoi_groups(List<String> poi_groups) {
		this.poi_groups = poi_groups;
	}

	public void setRoi_groups(List<String> roi_groups) {
		this.roi_groups = roi_groups;
	}

	public List<String> getPoiGroups() {
		return poi_groups;
	}

	public List<String> getRoiGroups() {
		return roi_groups;
	}

	public boolean hasPoi_groups() {
		return !poi_groups.isEmpty();
	}

	public boolean hasRoi_groups() {
		return !roi_groups.isEmpty();
	}

	public Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		if(hasPointDB()) {
			map.put("PointDB", pointdb);
		}
		if(hasPointCloud()) {
			map.put("pointcloud", pointcloud);
		}
		if(hasRasterDB()) {
			map.put("rasterdb", rasterdb);
		}
		if(hasPoi_groups()) {
			map.put("poi_groups", poi_groups.size() == 1 ? poi_groups.get(0) : poi_groups);
		}
		if(hasRoi_groups()) {
			map.put("roi_groups", roi_groups.size() == 1 ? roi_groups.get(0) : roi_groups);
		}
		return map;
	}

	public static Associated ofYaml(YamlMap yamlMap) {
		Associated associated = new Associated();
		if(yamlMap.contains("PointDB")) {
			associated.setPointDB(yamlMap.getString("PointDB"));
		}
		if(yamlMap.contains("pointcloud")) {
			associated.setPointCloud(yamlMap.getString("pointcloud"));
		}
		if(yamlMap.contains("rasterdb")) {
			associated.setRasterDB(yamlMap.getString("rasterdb"));
		}
		if(yamlMap.contains("poi_groups")) {
			associated.setPoi_groups(Collections.unmodifiableList(yamlMap.getList("poi_groups").asStrings()));
		}
		if(yamlMap.contains("roi_groups")) {
			associated.setRoi_groups(Collections.unmodifiableList(yamlMap.getList("roi_groups").asStrings()));
		}
		return associated;
	}

	public void writeJSON(JSONWriter json) {
		json.object();
		if(hasPointDB()) {
			json.key("PointDB");
			json.value(pointdb);
		}
		if(hasPointCloud()) {
			json.key("pointcloud");
			json.value(pointcloud);
		}
		if(hasRasterDB()) {
			json.key("rasterdb");
			json.value(rasterdb);
		}
		JsonUtil.writeOptList(json, "poi_groups", poi_groups);
		JsonUtil.writeOptList(json, "roi_groups", roi_groups);
		json.endObject();
	}

	public static Associated parseJSON(JSONObject json) {
		Associated associated = new Associated();
		if(json.has("PointDB")) {
			associated.setPointDB(json.getString("PointDB"));
		}
		if(json.has("pointcloud")) {
			associated.setPointCloud(json.getString("pointcloud"));
		}
		if(json.has("rasterdb")) {
			associated.setRasterDB(json.getString("rasterdb"));
		}
		if(json.has("poi_groups")) {
			associated.setPoi_groups(Collections.unmodifiableList(JsonUtil.optStringList(json, "poi_groups")));
		}
		if(json.has("roi_groups")) {
			associated.setRoi_groups(Collections.unmodifiableList(JsonUtil.optStringList(json, "roi_groups")));
		}
		return associated;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((poi_groups == null) ? 0 : poi_groups.hashCode());
		result = prime * result + ((pointcloud == null) ? 0 : pointcloud.hashCode());
		result = prime * result + ((pointdb == null) ? 0 : pointdb.hashCode());
		result = prime * result + ((roi_groups == null) ? 0 : roi_groups.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Associated other = (Associated) obj;
		if (poi_groups == null) {
			if (other.poi_groups != null)
				return false;
		} else if (!poi_groups.equals(other.poi_groups))
			return false;
		if (pointcloud == null) {
			if (other.pointcloud != null)
				return false;
		} else if (!pointcloud.equals(other.pointcloud))
			return false;
		if (pointdb == null) {
			if (other.pointdb != null)
				return false;
		} else if (!pointdb.equals(other.pointdb))
			return false;
		if (roi_groups == null) {
			if (other.roi_groups != null)
				return false;
		} else if (!roi_groups.equals(other.roi_groups))
			return false;
		return true;
	}


}
