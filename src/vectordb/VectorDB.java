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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;
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
import org.json.JSONWriter;
import org.locationtech.proj4j.CRSFactory;
import org.yaml.snakeyaml.Yaml;

import broker.Informal;
import broker.StructuredAccess;
import broker.acl.ACL;
import broker.acl.EmptyACL;
import broker.group.Poi;
import broker.group.Roi;
import pointdb.base.Point2d;
import server.api.vectordbs.VectordbDetails;
import util.Extent2d;
import util.Util;
import util.collections.ReadonlyList;
import util.collections.vec.Vec;
import util.yaml.YamlMap;

public class VectorDB {
	private static final Logger log = LogManager.getLogger();
	private static final String TYPE = "vectordb";

	public static final SpatialReference WEB_MERCATOR_SPATIAL_REFERENCE = new SpatialReference("");
	private static final int WEB_MERCATOR_EPSG = 3857;
	private static final CRSFactory CRS_FACTORY = new CRSFactory();

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

	private boolean structured_access_poi = false;
	private boolean structured_access_roi = false;

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
		LinkedHashMap<String, Object> structuredAccessMap = new LinkedHashMap<String, Object>();
		structuredAccessMap.put("poi", structured_access_poi);
		structuredAccessMap.put("roi", structured_access_roi);
		map.put("structured_access", structuredAccessMap);
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
		YamlMap structuredAccessMap = yamlMap.optMap("structured_access");
		structured_access_poi = structuredAccessMap.optBoolean("poi", false);
		structured_access_roi = structuredAccessMap.optBoolean("roi", false);
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

	public void writeTableJSON(JSONWriter json) {
		VectordbDetails details = getDetails();
		ReadonlyList<String> attributes = details.attributes;
		DataSource datasource = getDataSource();
		try {
			json.object();
			json.key("attributes");
			json.value(attributes);
			json.key("data");
			json.array();
			int layerCount = datasource.GetLayerCount();
			for(int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
				Layer layer = datasource.GetLayerByIndex(layerIndex);
				layer.ResetReading();
				Feature feature = layer.GetNextFeature();
				while(feature != null) {
					json.array();
					for(String attibute:attributes) {
						String value = feature.GetFieldAsString(attibute);
						json.value(value);
					}
					json.endArray();
					feature = layer.GetNextFeature();
				}
			}
			json.endArray();			
			json.endObject();
		} finally {
			closeDataSource(datasource);
		}
	}


	public Vec<String> getGML() {
		Vec<String> gmls = new Vec<>();
		DataSource datasource = getDataSource();
		try {
			int layerCount = datasource.GetLayerCount();
			for(int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
				Layer layer = datasource.GetLayerByIndex(layerIndex);
				layer.ResetReading();
				Feature feature = layer.GetNextFeature();
				while(feature != null) {
					Geometry geometry = feature.GetGeometryRef();
					if(geometry != null) {						
						Vector<String> options = new Vector<String>();
						options.add("FORMAT=GML3");
						options.add("GML3_LONGSRS=NO");
						String gml = geometry.ExportToGML(options);	
						//log.info(gml);
						gmls.add(gml);
					} else {
						log.warn("missing geometry in feature : " + feature.GetFID());
					}
					feature = layer.GetNextFeature();
				}
			}
		} finally {
			closeDataSource(datasource);
		}
		return gmls;
	}

	public String getGeoJSON(int epsg, boolean justNameAttribute) {
		String nameAttribute = null;
		ReadonlyList<String> attributes = null;
		if(justNameAttribute) {
			nameAttribute = getNameAttribute();
		} else {
			attributes = getDetails().attributes;
		}
		SpatialReference refDst = null;
		if(epsg >= 0) {
			if(epsg == WEB_MERCATOR_EPSG) {
				refDst = WEB_MERCATOR_SPATIAL_REFERENCE;
			} else {
				refDst = new SpatialReference("");
				refDst.ImportFromEPSG(epsg);			
			}
		}
		DataSource datasource = getDataSource();
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("{\"type\":\"FeatureCollection\"");
			int targetEPSG = epsg;
			if(targetEPSG <= 0) {
				try {
					targetEPSG = Integer.parseInt(this.getDetails().epsg);
				} catch(Exception e) {
					log.warn(e);
				}
			}
			if(targetEPSG > 0) {
				sb.append(",\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"urn:ogc:def:crs:EPSG::" + targetEPSG + "\"}}");
			} else {
				sb.append(",\"crs\":null");				
			}
			sb.append(",\"features\":");
			sb.append("[");
			boolean isFirst = true;
			int cnt = 0;
			int layerCount = datasource.GetLayerCount();
			for(int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
				Layer layer = datasource.GetLayerByIndex(layerIndex);
				SpatialReference layerRef = layer.GetSpatialRef();
				CoordinateTransformation ct = null;
				if(refDst != null && layerRef != null) {
					ct = CoordinateTransformation.CreateCoordinateTransformation(layerRef, refDst);	
					//log.info("refDst " + refDst);
					//log.info("layerRef " + layerRef);
				}
				layer.ResetReading();
				Feature feature = layer.GetNextFeature();
				while(feature != null) {
					Geometry geometry = feature.GetGeometryRef();
					if(geometry != null) {
						if(ct != null) {
							geometry.Transform(ct);
						}
						String json = geometry.ExportToJson();
						if(isFirst) {
							isFirst = false;
						} else {
							sb.append(",");						
						}
						sb.append("{\"type\":\"Feature\",\"geometry\":");
						sb.append(json);
						String id = "" + cnt++;

						sb.append(",\"id\":\"" + id +"\"");
						if(justNameAttribute) {
							addPropertiesJustNameAttribute(sb, feature, id, nameAttribute);
						} else {
							addPropertiesAllAttributes(sb, feature, attributes);
						}

						sb.append("}");
					} else {
						String attr1 = feature.GetFieldAsString(0);
						log.warn("missing geometry in feature : " + feature.GetFID() + "  " + attr1);
					}
					feature = layer.GetNextFeature();
				}
			}
			sb.append("]");
			sb.append("}");
			return sb.toString();
		} finally {
			closeDataSource(datasource);
		}
	}

	private static void addPropertiesJustNameAttribute(StringBuilder sb, Feature feature, String id, String nameAttribute) {
		String name = id;
		if(!nameAttribute.isEmpty()) {
			String featureName = feature.GetFieldAsString(nameAttribute);
			if(featureName != null) {
				name = featureName;
			}

		}
		sb.append(",\"properties\":{\"name\": \""+ name +"\"}");		
	}

	private static void addPropertiesAllAttributes(StringBuilder sb, Feature feature, ReadonlyList<String> attributes) {		
		sb.append(",\"properties\":{");		
		Iterator<String> it = attributes.iterator();
		boolean isFollowUp = false;
		while(it.hasNext()) {
			String attributeName = it.next();
			String attributeValue = feature.GetFieldAsString(attributeName);
			if(attributeValue != null) {
				if(isFollowUp) {
					sb.append(", ");	
				} else {
					isFollowUp = true;
				}
				sb.append("\""+ attributeName +"\": \""+ attributeValue +"\"");
			}
		}
		sb.append("}");		
	}


	/**
	 * 
	 * @param epsg  if epsg < 0 then no transformation
	 * @return
	 */
	public String getGeoJSONAsCollection(int epsg) {
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
					if(geometry != null) {
						resultGeo.AddGeometry(geometry);
					} else {
						log.warn("missing geometry");
					}
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
			if(subGeo != null) {
				deconGeo(subGeo, ps);
			} else {
				log.warn("missing sub geometry");
			}
		}		
	}

	public DataSource getDataSource() {
		if(!hasDataFilename()) {
			throw new RuntimeException("no datasource in vector layer " + this.getName());
		}
		String fullFileName = config.dataPath.resolve(dataFilename).toString();
		//log.info(fullFileName);
		DataSource datasource = ogr.Open(fullFileName, 0); // read only
		if(datasource == null) {
			throw new RuntimeException("could not open datasource: " + fullFileName);
		}
		return datasource;
	}

	public static void closeDataSource(DataSource dataSource) {
		if(dataSource != null) {
			dataSource.delete();
		}
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
				if(geometry != null) {
					deconGeo(geometry, ps);
				} else {
					log.warn("missing geometry");
				}
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

	public static Extent2d getExtent(double[][] points) {
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
		return new Extent2d(xmin, ymin, xmax, ymax);
	}
	
	public Extent2d getExtent() {
		Extent2d extent = null;
		DataSource datasource = getDataSource();
		try {		
			extent = VectorDB.getExtent(VectorDB.getPoints(datasource));
		} finally {
			VectorDB.closeDataSource(datasource);
		}
		return extent;
	}

	public SpatialReference getSpatialReference() {
		String fullFileName = config.dataPath.resolve(dataFilename).toString();
		//log.info(fullFileName);
		DataSource datasource = ogr.Open(fullFileName);
		Layer layer = datasource.GetLayerByIndex(0);
		Feature feature = layer.GetNextFeature();
		Geometry geometry = feature.GetGeometryRef();
		if(geometry != null) {
			SpatialReference ref = geometry.GetSpatialReference();
			return ref;
		} else {
			log.warn("missing geometry");
			SpatialReference ref = layer.GetSpatialRef();
			return ref;
		}
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
				if(!details.proj4.isEmpty()) {
					String epsg = CRS_FACTORY.readEpsgFromParameters(details.proj4);
					details.epsg = epsg == null ? "" : epsg;
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

	@FunctionalInterface
	public interface Layer0DefinitionsConsumer {
		void consume(int fieldIndex, String fieldName);
	}

	public void forEachLayer0Definition(Layer0DefinitionsConsumer consumer) {
		DataSource datasource = null;
		try {
			datasource = getDataSource();
			if(datasource.GetLayerCount() == 0) {
				return;
			}
			Layer layer = datasource.GetLayerByIndex(0);
			FeatureDefn featureDfn = layer.GetLayerDefn();
			int fieldCount = featureDfn.GetFieldCount();
			for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
				FieldDefn fieldDfn = featureDfn.GetFieldDefn(fieldIndex);
				String fieldName = fieldDfn.GetName();
				consumer.consume(fieldIndex, fieldName);
			}
		} catch(Exception e) {
			log.warn(e);
		} finally {
			closeDataSource(datasource);
		}
	}

	@FunctionalInterface
	public interface Layer0FeaturesConsumer {
		void consume(VectorFeature vectorFeature) throws Exception;
	}

	public void forEachLayer0Feature(Layer0FeaturesConsumer consumer) {
		DataSource datasource = null;
		VectorFeature vectorFeature = new VectorFeature(null);
		try {
			datasource = getDataSource();
			if(datasource.GetLayerCount() == 0) {
				return;
			}
			Layer layer = datasource.GetLayerByIndex(0);
			layer.ResetReading();
			Feature feature = layer.GetNextFeature();
			while(feature != null) {
				vectorFeature.feature = feature;
				consumer.consume(vectorFeature);
				feature = layer.GetNextFeature();
			}
		} catch(Exception e) {
			log.warn(e);
		} finally {
			vectorFeature = null;
			closeDataSource(datasource);
		}
	}



	public static final class VectorFeature {
		private volatile Feature feature;
		
		private VectorFeature() {}
		
		@FunctionalInterface
		public interface VectorFeatureFieldConsumer {
			void consume(VectorFeatureField vectorFeatureField) throws Exception;
		}

		public final class VectorFeatureField {
			private volatile int fieldIndex = -1;
			
			private VectorFeatureField() {}
			
			public int getFieldIndex() {
				return fieldIndex;
			}
			
			public String getName() {
				FieldDefn fieldDefn = feature.GetFieldDefnRef(fieldIndex);
				if(fieldDefn == null) {
					return null;
				}
				return fieldDefn.GetName();
			}
			
			public String getAsString() {
				return feature.GetFieldAsString(fieldIndex);
			}
		}

		private VectorFeature(Feature feature) {
			this.feature = feature;
		}

		public String getGeometryGML() {
			Geometry geometry = feature.GetGeometryRef();
			if(geometry == null) {
				return null;
			}
			Vector<String> options = new Vector<String>();
			options.add("FORMAT=GML3");
			options.add("GML3_LONGSRS=NO");
			String gml = geometry.ExportToGML(options);
			return gml;
		}

		public void forEachField(VectorFeatureFieldConsumer consumer) throws Exception {
			int fieldCount = feature.GetFieldCount();
			VectorFeatureField vectorFeatureField = new VectorFeatureField();
			try {
				for(int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
					vectorFeatureField.fieldIndex = fieldIndex;
					consumer.consume(vectorFeatureField);
				}
			} finally {
				vectorFeatureField.fieldIndex = -1;
			}
		}
	}

	public static String createValidID(String text) {
		return text.replaceAll("[^a-zA-Z0-9_]", "_");
	}

	public Vec<Poi> getPOIs() {
		String nameAttr = getNameAttribute();
		DataSource datasource = getDataSource();
		try {
			Vec<Poi> pois = new Vec<Poi>();
			HashSet<String> poiNames = new HashSet<String>();
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
								String orgName = feature.GetFieldAsString(fieldIndex);
								if(orgName.isEmpty()) {
									orgName = "POI";
								}
								orgName = createValidID(orgName);
								String name = orgName;
								int nameIndex = 1;
								while(poiNames.contains(name)) {
									name = orgName + "_" + (++nameIndex);
								}
								poiNames.add(name);
								Geometry geometry = feature.GetGeometryRef();
								if(geometry != null) {
									double x = geometry.GetX();
									double y = geometry.GetY();
									pois.add(new Poi(name, x, y));
								} else {
									log.warn("missing geometry in " + getName());
								}
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

	public Vec<Roi> getROIs() {
		String nameAttr = getNameAttribute();
		DataSource datasource = getDataSource();
		try {
			Vec<Roi> rois = new Vec<Roi>();
			HashSet<String> roiNames = new HashSet<String>();
			int layerCount = datasource.GetLayerCount();
			for(int layerIndex=0; layerIndex<layerCount; layerIndex++) {
				Layer layer = datasource.GetLayerByIndex(layerIndex);
				int geomType = layer.GetGeomType();
				//log.info("getROIs geomType " + geomType);
				if(geomType == 6 || geomType == -2147483642) { // 6  MultiPolygon 0x80000006  MultiPolygon25D
					log.warn("just first polygon will be included in multipolygon in " + getName());
					int fieldIndex = layer.FindFieldIndex(nameAttr, 1);
					if(fieldIndex >= 0) {
						layer.ResetReading();
						Feature feature = layer.GetNextFeature();
						while(feature != null) {
							collectROI(feature, fieldIndex, roiNames, rois);
							feature = layer.GetNextFeature();
						}
					}
				} else if(geomType == 3 || geomType == -2147483645) { // 3  POLYGON      0x80000003  Polygon25D
					int fieldIndex = layer.FindFieldIndex(nameAttr, 1);
					if(fieldIndex >= 0) {
						layer.ResetReading();
						Feature feature = layer.GetNextFeature();
						while(feature != null) {
							collectROI(feature, fieldIndex, roiNames, rois);
							feature = layer.GetNextFeature();
						}
					}
				}
			}
			return rois;
		} finally {
			closeDataSource(datasource);
		}
	}

	private void collectROI(Feature feature, int fieldIndex, HashSet<String> roiNames, Vec<Roi> rois) {
		if(feature.IsFieldSet(fieldIndex)) {
			String orgName = feature.GetFieldAsString(fieldIndex);
			if(orgName.isEmpty()) {
				orgName = "ROI";
			}
			orgName = createValidID(orgName);
			String name = orgName;
			int nameIndex = 1;
			while(roiNames.contains(name)) {
				name = orgName + "_" + (++nameIndex);
			}
			roiNames.add(name);
			Geometry geometry = feature.GetGeometryRef();
			if(geometry != null) {
				int geoCount = geometry.GetGeometryCount();
				if(geoCount != 1) {
					if(geoCount > 1) {
						log.warn("just first sub geometry will be included in polygon: " + geoCount+" in " + getName() + " : " + name);
					} else {
						log.warn("missing sub geometry in polygon: " + geoCount+" in " + getName() + " : " + name);
						return;
					}
				}
				Geometry subGeo = geometry.GetGeometryRef(0);
				if(subGeo != null) {
					int subType = subGeo.GetGeometryType();
					//log.info("subgeomerty: " + subType + " in " + getName() + " : " + name);
					if(subType == 3 || subType == -2147483645) { // 3  POLYGON   0x80000003  Polygon25D fix subType polygon
						int subsubCount = subGeo.GetGeometryCount();
						if(subsubCount > 1) {
							log.warn("just first sub sub geometry will be included in polygon: " + geoCount+" in " + getName() + " : " + name);
						}
						Geometry subsubGeo = subGeo.GetGeometryRef(0);
						if(subsubGeo != null) { 
							int subsubType = subsubGeo.GetGeometryType();
							//log.warn("fix subtype polygon -> subgeomerty: " + subType + " in " + getName() + " : " + name + "  " + subsubCount);
							subGeo = subsubGeo;
							subType = subsubType;
						} else {
							log.warn("missing sub sub geometry");
						}
					}
					switch (subType) {
					case 2: // 2  LINESTRING
					case -2147483646: { // 0x80000002  LineString25D
						Object[] polygonPoints = subGeo.GetPoints();
						int pointsLen = polygonPoints.length;
						Point2d[] points = new Point2d[pointsLen];									
						int len = points.length;
						for (int i = 0; i < pointsLen; i++) {
							double[] p = (double[]) polygonPoints[i];
							points[i] = new Point2d(p[0], p[1]);
						}									
						rois.add(new Roi(name, points));									
						break;
					}
					default: 
						log.warn("unknown POLYGON sub geometry " + subType + "  "+ Long.toHexString(Integer.toUnsignedLong(subType)) + "  " + subGeo.GetGeometryName());
					}
				} else {
					log.warn("missing sub geometry");
				}
			} else {
				log.warn("missing geometry");
			}
		}		
	}


	public boolean getStructuredAccessPOI() {
		return structured_access_poi;
	}

	public boolean getStructuredAccessROI() {
		return structured_access_roi;
	}

	public void setStructuredAccessPOI(boolean poi) {
		this.structured_access_poi = poi;
	}

	public void setStructuredAccessROI(boolean roi) {
		this.structured_access_roi = roi;
	}

	public StructuredAccess getStructuredAccess() {
		return new StructuredAccess(structured_access_poi, structured_access_roi);
	}
}
