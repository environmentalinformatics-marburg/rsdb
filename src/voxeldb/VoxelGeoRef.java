package voxeldb;

import java.util.LinkedHashMap;
import java.util.Map;

import util.Extent3d;
import util.Range2d;
import util.Range3d;
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

	public VoxelGeoRef withVoxelSize(double voxelSize) {
		return new VoxelGeoRef(proj4, epsg, originX, originY, originZ, voxelSize, voxelSize, voxelSize);
	}

	public VoxelGeoRef withOrigin(double originX, double originY, double originZ) {
		return new VoxelGeoRef(proj4, epsg, originX, originY, originZ, voxelSizeX, voxelSizeY, voxelSizeZ);
	}
	
	public VoxelGeoRef with(double originX, double originY, double originZ, double voxelSizeX, double voxelSizeY, double voxelSizeZ) {
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

	public double voxelXtoGeo(int x) {
		return (x * voxelSizeX) + originX;
	}

	public double voxelYtoGeo(int y) {
		return (y * voxelSizeY) + originY;
	}

	public double voxelZtoGeo(int z) {
		return (z * voxelSizeZ) + originZ;
	}

	public Extent3d toGeoExtent(Range3d range) {
		return new Extent3d(voxelXtoGeo(range.xmin), voxelYtoGeo(range.ymin), voxelZtoGeo(range.zmin), voxelXtoGeo(range.xmax + 1), voxelYtoGeo(range.ymax + 1), voxelZtoGeo(range.zmax + 1));
	}

	public Range3d geoToRange(double geoXmin, double geoYmin, double geoZmin, double geoXmax, double geoYmax, double geoZmax) {
		int xmin = geoXtoVoxel(geoXmin);
		int ymin = geoYtoVoxel(geoYmin);
		int zmin = geoZtoVoxel(geoZmin);
		int xmax = geoXtoVoxel(geoXmax);
		int ymax = geoYtoVoxel(geoYmax);
		int zmax = geoZtoVoxel(geoZmax);
		return new Range3d(xmin, ymin, zmin, xmax, ymax, zmax);
	}
	
	public Range2d geoToRange(double geoXmin, double geoYmin, double geoXmax, double geoYmax) {
		int xmin = geoXtoVoxel(geoXmin);
		int ymin = geoYtoVoxel(geoYmin);
		int xmax = geoXtoVoxel(geoXmax);
		int ymax = geoYtoVoxel(geoYmax);
		return new Range2d(xmin, ymin, xmax, ymax);
	}

	@Override
	public String toString() {
		return "VoxelGeoRef [proj4=" + proj4 + ", epsg=" + epsg + ", originX=" + originX + ", originY=" + originY
				+ ", originZ=" + originZ + ", voxelSizeX=" + voxelSizeX + ", voxelSizeY=" + voxelSizeY + ", voxelSizeZ="
				+ voxelSizeZ + "]";
	}
}
