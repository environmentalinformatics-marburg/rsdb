package voxeldb;

import java.util.LinkedHashMap;
import java.util.Map;

import util.yaml.YamlMap;

public class VoxelGeoRef {
	
	public static final VoxelGeoRef DEFAULT = new VoxelGeoRef("");
	
	public static final String PROPERTY_PROJ4 = "proj4";
	
	public final String proj4;
	
	private double originX;
	private double originY;
	private double originZ;
	
	private double voxelSizeX;
	private double voxelSizeY;
	private double voxelSizeZ;
	
	public VoxelGeoRef(String proj4) {
		this.proj4 = proj4;
	}
	
	public boolean hasProj4() {
		return !proj4.isEmpty();
	}
	
	public Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		if (hasProj4()) {
			map.put(PROPERTY_PROJ4, proj4);
		}
		return map;
	}

	public static VoxelGeoRef ofYaml(YamlMap yamlMap) {
		String proj4 = yamlMap.optString(PROPERTY_PROJ4, "");
		return new VoxelGeoRef(proj4);
	}

}
