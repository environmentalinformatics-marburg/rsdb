package voxeldb;

import java.util.LinkedHashMap;
import java.util.Map;

import util.yaml.YamlMap;

public class VoxelGeoRef {

	public static final VoxelGeoRef DEFAULT = new VoxelGeoRef("", 0, 0, 0, 0, 1, 1, 1);

	public static final String PROPERTY_PROJ4 = "proj4";
	public static final String PROPERTY_EPSG = "epsg";
	public static final String PROPERTY_VOXEL_SIZE = "voxel_size";
	public static final String PROPERTY_ORIGIN = "origin";

	public final String proj4;
	public final int epsg;

	public final double originX;
	public final double originY;
	public final double originZ;

	public final double voxelSizeX;
	public final double voxelSizeY;
	public final double voxelSizeZ;

	public VoxelGeoRef(String proj4, int epsg, double originX, double originY, double originZ,  double voxelSizeX, double voxelSizeY, double voxelSizeZ) {
		this.proj4 = proj4;
		this.epsg = epsg;
		this.originX = originX;
		this.originY = originY;
		this.originZ = originZ;
		this.voxelSizeX = voxelSizeX;
		this.voxelSizeY = voxelSizeY;
		this.voxelSizeZ = voxelSizeZ;
	}

	public boolean hasProj4() {
		return !proj4.isEmpty();
	}

	public boolean hasEpsg() {
		return epsg > 0;
	}

	public Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		if(hasProj4()) {
			map.put(PROPERTY_PROJ4, proj4);
		}
		if(hasEpsg()) {
			map.put(PROPERTY_EPSG, epsg);
		}
		LinkedHashMap<String, Object> om = new LinkedHashMap<String, Object>();
		om.put("x", originX);
		om.put("y", originY);
		om.put("z", originZ);
		map.put(PROPERTY_ORIGIN, om);
		LinkedHashMap<String, Object> vsm = new LinkedHashMap<String, Object>();
		vsm.put("x", voxelSizeX);
		vsm.put("y", voxelSizeY);
		vsm.put("z", voxelSizeZ);
		map.put(PROPERTY_VOXEL_SIZE, vsm);
		return map;
	}

	public static VoxelGeoRef ofYaml(YamlMap yamlMap) {
		String proj4 = yamlMap.optString(PROPERTY_PROJ4, "");
		int epsg = yamlMap.optInt(PROPERTY_EPSG, 0);
		
		YamlMap om = yamlMap.optMap(PROPERTY_ORIGIN);
		double originX = om.optDouble("x", 0);
		double originY = om.optDouble("y", 0);
		double originZ = om.optDouble("z", 0);
		
		YamlMap vsm = yamlMap.optMap(PROPERTY_VOXEL_SIZE);
		double voxelSizeX = vsm.optDouble("x", 1);
		double voxelSizeY = vsm.optDouble("y", 1);
		double voxelSizeZ = vsm.optDouble("z", 1);
		
		return new VoxelGeoRef(proj4, epsg, originX, originY, originZ, voxelSizeX, voxelSizeY, voxelSizeZ);
	}

	public VoxelGeoRef withProj4(String proj4) {
		return new VoxelGeoRef(proj4, epsg, originX, originY, originZ, voxelSizeX, voxelSizeY, voxelSizeZ);
	}
	
	public VoxelGeoRef withEpsg(int epsg) {
		return new VoxelGeoRef(proj4, epsg, originX, originY, originZ, voxelSizeX, voxelSizeY, voxelSizeZ);
	}

	public VoxelGeoRef withVoxelSize(double voxelsize) {
		return new VoxelGeoRef(proj4, epsg, originX, originY, originZ, voxelsize, voxelsize, voxelsize);
	}
	
	public VoxelGeoRef withOrigin(double originX, double originY, double originZ) {
		return new VoxelGeoRef(proj4, epsg, originX, originY, originZ, voxelSizeX, voxelSizeY, voxelSizeZ);
	}

	public int geoXtoVoxel(double x) {
		return (int) Math.floor((x - originX) / voxelSizeX);		
	}
	
	public int geoYtoVoxel(double y) {
		return (int) Math.floor((y - originY) / voxelSizeY);		
	}
	
	public int geoZtoVoxel(double z) {
		return (int) Math.floor((z - originZ) / voxelSizeZ);		
	}
}
