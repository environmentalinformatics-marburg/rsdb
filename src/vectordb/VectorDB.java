package vectordb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.UserIdentity;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Feature;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.FieldDefn;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.yaml.snakeyaml.Yaml;

import broker.Informal;
import broker.acl.ACL;
import broker.acl.EmptyACL;
import broker.group.Poi;
import server.api.vectordbs.VectordbDetails;
import util.Util;
import util.collections.vec.Vec;
import util.yaml.YamlMap;

public class VectorDB {
	private static final Logger log = LogManager.getLogger();
	private static final String TYPE = "vectordb";

	public static final SpatialReference WEB_MERCATOR_SPATIAL_REFERENCE = new SpatialReference("");
	private static final int WEB_MERCATOR_EPSG = 3857;

	static {
		WEB_MERCATOR_SPATIAL_REFERENCE.ImportFromEPSG(3857);
	}

	private final VectordbConfig config;
	private Informal informal = Informal.EMPTY;
	private ACL acl = EmptyACL.ADMIN;
	private ACL acl_mod = EmptyACL.ADMIN;

	private String dataFilename = "";
	private String nameAttribute = "";

	private String datatag = "";

	public VectorDB(VectordbConfig config) {
		this.config = config;		
		this.readMeta();
		refreshDatatag();
	}

	public synchronized void readMeta() { // throws if read error
		try {
			if (config.metaFile.exists()) {
				YamlMap map;
				try(InputStream in = new FileInputStream(config.metaFile)) {
					map = YamlMap.ofObject(new Yaml().load(in));
				}
				yamlToMeta(map);
			}
			if(dataFilename.isEmpty()) {
				dataFilename = guessDataFilename();
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.warn(e);
			throw new RuntimeException(e);
		}
	}

	public synchronized void writeMeta() {
		try {
			LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
			metaToYaml(map);
			Yaml yaml = new Yaml();
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(config.metaTempFile)));
			yaml.dump(map, out);
			out.close();
			Files.move(config.metaTempPath, config.metaPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (Exception e) {
			e.printStackTrace();
			log.warn(e);
			throw new RuntimeException(e);
		}
	}

	public String guessDataFilename() {
		List<Path> filenames = getDataFilenames();
		if(filenames.isEmpty()) {
			return "";
		}
		if(filenames.size() == 1) {
			return filenames.get(0).toString();
		}
		for(Path filename:filenames) {
			if(filename.toString().endsWith(".shp")) {
				return filename.toString();
			}
		}
		for(Path filename:filenames) {
			if(filename.toString().endsWith(".gpkg")) {
				return filename.toString();
			}
		}
		return filenames.get(0).toString();
	}

	private synchronized void metaToYaml(LinkedHashMap<String, Object> map) {
		map.put("type", TYPE);
		map.put("acl", acl.toYaml());
		map.put("acl_mod", acl_mod.toYaml());
		informal.writeYaml(map);
		map.put("data_filename", dataFilename);
		map.put("name_attribute", nameAttribute);
	}

	private synchronized void yamlToMeta(YamlMap yamlMap) {
		String type = yamlMap.getString("type");
		if (!type.equals(TYPE)) {
			throw new RuntimeException("wrong type: " + type);
		}
		acl = ACL.of(yamlMap.optList("acl").asStrings());
		acl_mod = ACL.of(yamlMap.optList("acl_mod").asStrings());
		informal = Informal.ofYaml(yamlMap);
		dataFilename = yamlMap.optString("data_filename", "");
		nameAttribute = yamlMap.optString("name_attribute", "");
	}

	public String getName() {
		return config.name;
	}

	public Informal informal() {
		return informal;
	}

	public void setInformal(Informal informal) {
		this.informal = informal;
	}

	static {
		ogr.RegisterAll();
	}

	/**
	 * 
	 * @param epsg  if epsg < 0 then no transformation
	 * @return
	 */
	public String getGeoJSON(int epsg) {
		DataSource datasource = getDataSource();
		try {
			/*log.info("datasource.GetLayerCount() " + datasource.GetLayerCount());
		Layer layer = datasource.GetLayerByIndex(0);
		log.info("layer.GetFeatureCount() " + layer.GetFeatureCount());
		Feature feature = layer.GetNextFeature();
		Geometry geometry = feature.GetGeometryRef();		
		String json = geometry.ExportToJson();*/


			SpatialReference refSrc = null;
			int layerCount = datasource.GetLayerCount();
			log.info("datasource.GetLayerCount() " + layerCount);
			Geometry resultGeo = new Geometry(7);// GEOMETRYCOLLECTION
			log.info(resultGeo+" "+resultGeo.GetGeometryName());
			//log.info(resultGeo.ExportToJson());
			for(int layerIndex=0; layerIndex<layerCount; layerIndex++) {
				Layer layer = datasource.GetLayerByIndex(layerIndex);
				SpatialReference layerRef = layer.GetSpatialRef();
				if(layerRef != null && refSrc == null) {
					refSrc = layerRef;
				}
				layer.ResetReading();
				Feature feature = layer.GetNextFeature();
				while(feature != null) {
					Geometry geometry = feature.GetGeometryRef();

					resultGeo.AddGeometry(geometry);

					feature = layer.GetNextFeature();
				}
			}

			if(epsg >= 0) {
				SpatialReference refDst;
				if(epsg == WEB_MERCATOR_EPSG) {
					refDst = WEB_MERCATOR_SPATIAL_REFERENCE;
				} else {
					refDst = new SpatialReference("");
					refDst.ImportFromEPSG(epsg);			
				}
				CoordinateTransformation ct = CoordinateTransformation.CreateCoordinateTransformation(refSrc, refDst);		
				resultGeo.Transform(ct);
			}

			String json = resultGeo.ExportToJson();		
			//log.info(json);
			return json;
		} finally {
			closeDataSource(datasource);
		}
	}

	public static void deconGeo(Geometry geometry, Vec<Object[]> ps) {
		//log.info("geometry.GetGeometryName() " + geometry.GetGeometryName());
		int pointCount = geometry.GetPointCount();
		if(pointCount > 0) {
			Object[] points = geometry.GetPoints();
			if(points != null && points.length > 0) {
				ps.add(points);
			}
		}
		int geoCount = geometry.GetGeometryCount();
		for(int i=0; i<geoCount; i++) {
			Geometry subGeo = geometry.GetGeometryRef(i);
			deconGeo(subGeo, ps);
		}
	}

	public DataSource getDataSource() {
		if(!hasDataFilename()) {
			throw new RuntimeException("no datasource in vector layer " + this.getName());
		}
		String fullFileName = config.dataPath.resolve(dataFilename).toString();
		log.info(fullFileName);
		DataSource datasource = ogr.Open(fullFileName, 0); // read only
		if(datasource == null) {
			throw new RuntimeException("could not open datasource: " + fullFileName);
		}
		return datasource;
	}

	public static void closeDataSource(DataSource dataSource) {
		dataSource.delete();
	}

	public double[][] getPoints() {		
		DataSource datasource = getDataSource();
		try {
			return getPoints(datasource);
		} finally {
			closeDataSource(datasource);
		}
	}

	public static double[][] getPoints(DataSource datasource) {
		int layerCount = datasource.GetLayerCount();
		Vec<Object[]> ps = new Vec<Object[]>();
		for(int layerIndex=0; layerIndex<layerCount; layerIndex++) {
			Layer layer = datasource.GetLayerByIndex(layerIndex);
			layer.ResetReading();
			Feature feature = layer.GetNextFeature();
			while(feature != null) {
				Geometry geometry = feature.GetGeometryRef();
				deconGeo(geometry, ps);				
				feature = layer.GetNextFeature();
			}
		}
		int len = (int) ps.stream().mapToInt(points -> points.length).sum();
		double[][] points = new double[len][];
		int pos = 0;
		for(Object o:ps) {
			Object[] p = (Object[]) o;
			//log.info("len " + p.length);
			System.arraycopy(p, 0, points, pos, p.length);
			pos += p.length;
		}
		log.info("datasource.GetLayerCount() " + layerCount + "   points " + points.length);
		return points;	
	}

	public static double[] getExtent(double[][] points) {
		double xmin = Double.POSITIVE_INFINITY;
		double ymin = Double.POSITIVE_INFINITY;
		double xmax = Double.NEGATIVE_INFINITY;
		double ymax = Double.NEGATIVE_INFINITY;
		for(double[] p:points) {
			double x = p[0];
			double y = p[1];
			if(x < xmin) {
				xmin = x;
			}
			if(y < ymin) {
				ymin = y;
			}
			if(x > xmax) {
				xmax = x;
			}
			if(y > ymax) {
				ymax = y;
			}
		}
		return new double[] {xmin, ymin, xmax, ymax};
	}

	public SpatialReference getSpatialReference() {
		String fullFileName = config.dataPath.resolve(dataFilename).toString();
		//log.info(fullFileName);
		DataSource datasource = ogr.Open(fullFileName);
		Layer layer = datasource.GetLayerByIndex(0);
		Feature feature = layer.GetNextFeature();
		Geometry geometry = feature.GetGeometryRef();
		SpatialReference ref = geometry.GetSpatialReference();
		return ref;
	}

	public List<Path> getDataFilenames() {
		File[] fullfiles = config.dataPath.toFile().listFiles();
		if(fullfiles == null) {
			//throw new RuntimeException("data directory does not exist");
			log.warn("data directory does not exist");
			return java.util.Collections.emptyList();
		}
		List<Path> files = Arrays.stream(fullfiles).map(file -> {
			//return file.toPath();//.relativize(config.dataPath);
			return config.dataPath.relativize(file.toPath());
		}).collect(Collectors.toList());
		return files;		
	}

	public String getDataFilename() {
		return dataFilename;
	}

	public boolean hasDataFilename() {
		return !dataFilename.isEmpty();
	}

	public String getNameAttribute() {
		return nameAttribute;
	}

	public void close() {		
	}

	public Path getDataPath() {
		return config.dataPath;
	}

	public void removeFile(String filename) throws IOException {
		List<Path> filenames = getDataFilenames();		
		if(!filenames.stream().anyMatch(path -> path.toString().equals(filename))) {
			throw new RuntimeException("file not found");
		}
		Path path = config.dataPath.resolve(filename);
		Util.safeDeleteIfExists(config.dataPath, path);
	}

	public void setDataFilename(String dataFilename) {
		this.dataFilename = dataFilename;	
	}

	public void setNameAttribute(String nameAttribute) {
		this.nameAttribute = nameAttribute;	
	}

	public ACL getACL() {
		return acl;
	}

	public ACL getACL_mod() {
		return acl_mod;
	}

	public void setACL(ACL acl) {
		this.acl = acl;
		writeMeta();	
	}

	public void setACL_mod(ACL acl_mod) {
		this.acl_mod = acl_mod;
		writeMeta();	
	}

	public boolean isAllowed(UserIdentity userIdentity) {
		return acl.isAllowed(userIdentity);
	}

	public void check(UserIdentity userIdentity) {
		acl.check(userIdentity);
	}

	public boolean isAllowedMod(UserIdentity userIdentity) {
		return acl_mod.isAllowed(userIdentity);
	}

	public void checkMod(UserIdentity userIdentity) {
		acl_mod.check(userIdentity);
	}

	public String getDatatag() {
		return datatag;
	}

	public void refreshDatatag() {
		datatag = Long.toHexString(ThreadLocalRandom.current().nextLong());
	}

	public VectordbDetails getDetails() {
		VectordbDetails details = new VectordbDetails();
		try {
			DataSource datasource = getDataSource();
			try {
				Vec<String> attributes = new Vec<String>();
				int layerCount = datasource.GetLayerCount();
				for(int layerIndex=0; layerIndex<layerCount; layerIndex++) {
					Layer layer = datasource.GetLayerByIndex(layerIndex);
					if(details.proj4.isEmpty()) {
						SpatialReference layerRef = layer.GetSpatialRef();
						String proj4 = layerRef.ExportToProj4();
						if(proj4 != null) {
							details.proj4 = proj4;
						}
						FeatureDefn featureDfn = layer.GetLayerDefn();
						int fieldCount = featureDfn.GetFieldCount();
						for (int i = 0; i < fieldCount; i++) {
							FieldDefn fieldDfn = featureDfn.GetFieldDefn(i);
							String attribute = fieldDfn.GetName();
							attributes.addIfNotContained(attribute);
						}
					}
				}
				details.attributes = attributes.readonlyWeakView();
			} finally {
				closeDataSource(datasource);
			}
		} catch(Exception e) {
			log.warn(e);
		}
		return details;
	}

	public Vec<Poi> getPOIs() {
		String nameAttr = getNameAttribute();
		DataSource datasource = getDataSource();
		try {
			Vec<Poi> pois = new Vec<Poi>();
			int layerCount = datasource.GetLayerCount();
			for(int layerIndex=0; layerIndex<layerCount; layerIndex++) {
				Layer layer = datasource.GetLayerByIndex(layerIndex);
				int geomType = layer.GetGeomType();
				if(geomType == 1 || geomType == -2147483647) { // 1  POINT   0x80000001 Point25D
					int fieldIndex = layer.FindFieldIndex(nameAttr, 1);
					if(fieldIndex >= 0) {
						layer.ResetReading();
						Feature feature = layer.GetNextFeature();
						while(feature != null) {
							if(feature.IsFieldSet(fieldIndex)) {
								String name = feature.GetFieldAsString(fieldIndex);
								Geometry geometry = feature.GetGeometryRef();
								double x = geometry.GetX();
								double y = geometry.GetY();
								pois.add(new Poi(name, x, y));
							}
							feature = layer.GetNextFeature();
						}
					}
				}
			}
			return pois;
		} finally {
			closeDataSource(datasource);
		}
	}
}
