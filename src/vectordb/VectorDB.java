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

import org.eclipse.jetty.server.UserIdentity;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Feature;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.FieldDefn;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;
import org.gdal.osr.SpatialReference;
import org.json.JSONWriter;
import org.tinylog.Logger;
import org.yaml.snakeyaml.Yaml;

import broker.Informal;
import broker.StructuredAccess;
import broker.acl.ACL;
import broker.acl.AclUtil;
import broker.acl.EmptyACL;
import broker.group.Poi;
import broker.group.Roi;
import pointcloud.Rect2d;
import pointdb.base.Point2d;
import pointdb.base.PolygonUtil.PolygonWithHoles;
import server.api.vectordbs.VectordbDetails;
import util.GeoUtil;
import util.GeoUtil.Transformer;
import util.Util;
import util.collections.ReadonlyList;
import util.collections.vec.Vec;
import util.yaml.YamlMap;
import vectordb.style.Style;

public class VectorDB {

	private static final String TYPE = "vectordb";

	private final VectordbConfig config;
	private Informal informal = Informal.EMPTY;
	private ACL acl = EmptyACL.ADMIN;
	private ACL acl_mod = EmptyACL.ADMIN;
	private ACL acl_owner = EmptyACL.ADMIN;

	private String dataFilename = "";
	private String nameAttribute = "";

	private String datatag = "";

	private boolean structured_access_poi = false;
	private boolean structured_access_roi = false;

	private Style style = null;

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
			Logger.warn(e);
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
			Logger.warn(e);
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
		map.put("acl_owner", acl_owner.toYaml());
		informal.writeYaml(map);
		map.put("data_filename", dataFilename);
		map.put("name_attribute", nameAttribute);
		LinkedHashMap<String, Object> structuredAccessMap = new LinkedHashMap<String, Object>();
		structuredAccessMap.put("poi", structured_access_poi);
		structuredAccessMap.put("roi", structured_access_roi);
		map.put("structured_access", structuredAccessMap);
		if(style != null) {
			try {
				map.put("style", style.toYaml());
			} catch(Exception e) {
				Logger.error(e);
			}
		}
	}

	private synchronized void yamlToMeta(YamlMap yamlMap) {
		String type = yamlMap.getString("type");
		if (!type.equals(TYPE)) {
			throw new RuntimeException("wrong type: " + type);
		}
		acl = ACL.ofRoles(yamlMap.optList("acl").asStrings());
		acl_mod = ACL.ofRoles(yamlMap.optList("acl_mod").asStrings());
		acl_owner = ACL.ofRoles(yamlMap.optList("acl_owner").asStrings());
		informal = Informal.ofYaml(yamlMap);
		dataFilename = yamlMap.optString("data_filename", "");
		nameAttribute = yamlMap.optString("name_attribute", "");
		YamlMap structuredAccessMap = yamlMap.optMap("structured_access");
		structured_access_poi = structuredAccessMap.optBoolean("poi", false);
		structured_access_roi = structuredAccessMap.optBoolean("roi", false);
		try {
			style = yamlMap.optMapConv("style", Style::ofYaml, null);
		} catch(Exception e) {
			style = null;
			Logger.error(e);
		}
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

	public synchronized void writeTableJSON(JSONWriter json) { // layer iterator and layer filter possibly not parallel
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
				layer.SetSpatialFilter(null); // clear filter
				layer.ResetReading(); // reset iterator
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


	public synchronized Vec<String> getGML() { // layer iterator and layer filter possibly not parallel
		Vec<String> gmls = new Vec<>();
		DataSource datasource = getDataSource();
		try {
			int layerCount = datasource.GetLayerCount();
			for(int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
				Layer layer = datasource.GetLayerByIndex(layerIndex);
				layer.SetSpatialFilter(null); // clear filter
				layer.ResetReading();  // reset iterator
				Feature feature = layer.GetNextFeature();
				while(feature != null) {
					Geometry geometry = feature.GetGeometryRef();
					if(geometry != null) {						
						Vector<String> options = new Vector<String>();
						options.add("FORMAT=GML3");
						options.add("GML3_LONGSRS=NO");
						String gml = geometry.ExportToGML(options);	
						//Logger.info(gml);
						gmls.add(gml);
					} else {
						Logger.warn("missing geometry in feature : " + feature.GetFID());
					}
					feature = layer.GetNextFeature();
				}
			}
		} finally {
			closeDataSource(datasource);
		}
		return gmls;
	}

	/**
	 * 
	 * @param epsg
	 * @param justNameAttribute
	 * @param ext nullable
	 * @return
	 */
	public synchronized String getGeoJSON(int epsg, boolean justNameAttribute, Rect2d ext) { // layer iterator and layer filter possibly not parallel
		String nameAttribute = null;
		ReadonlyList<String> attributes = null;
		if(justNameAttribute) {
			nameAttribute = getNameAttribute();
		} else {
			attributes = getDetails().attributes;
		}
		SpatialReference dstSr = null;
		if(epsg >= 0) {
			if(epsg == GeoUtil.EPSG_WEB_MERCATOR) {
				dstSr = GeoUtil.WEB_MERCATOR_SPATIAL_REFERENCE;
			} else {
				dstSr = GeoUtil.getSpatialReferenceFromEPSG(epsg);		
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
					Logger.warn(e);
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
				try {
					SpatialReference layerSr = layer.GetSpatialRef();
					Transformer transformer = null;
					if(dstSr != null && layerSr != null) {
						transformer = GeoUtil.createCoordinateTransformer(layerSr, dstSr);
					}
					if(ext == null) {
						layer.SetSpatialFilter(null); // clear filter
					} else {
						layer.SetSpatialFilterRect(ext.xmin, ext.ymin, ext.xmax, ext.ymax);
					}
					layer.ResetReading();  // reset iterator
					Feature feature = layer.GetNextFeature();
					while(feature != null) {
						Geometry geometry = feature.GetGeometryRef();
						if(geometry != null) {
							if(transformer != null) {
								geometry.Transform(transformer.coordinateTransformation);
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
							Logger.warn("missing geometry in feature : " + feature.GetFID() + "  " + attr1);
						}
						feature = layer.GetNextFeature();
					}
				} finally {
					layer.SetSpatialFilter(null); // clear filter
					layer.ResetReading(); // reset iterator
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
	public synchronized String getGeoJSONAsCollection(int epsg) { // layer iterator and layer filter possibly not parallel
		DataSource datasource = getDataSource();
		try {
			/*Logger.info("datasource.GetLayerCount() " + datasource.GetLayerCount());
		Layer layer = datasource.GetLayerByIndex(0);
		Logger.info("layer.GetFeatureCount() " + layer.GetFeatureCount());
		Feature feature = layer.GetNextFeature();
		Geometry geometry = feature.GetGeometryRef();		
		String json = geometry.ExportToJson();*/


			SpatialReference srcSr = null;
			int layerCount = datasource.GetLayerCount();
			Logger.info("datasource.GetLayerCount() " + layerCount);
			Geometry resultGeo = new Geometry(7);// GEOMETRYCOLLECTION
			Logger.info(resultGeo+" "+resultGeo.GetGeometryName());
			//Logger.info(resultGeo.ExportToJson());
			for(int layerIndex=0; layerIndex<layerCount; layerIndex++) {
				Layer layer = datasource.GetLayerByIndex(layerIndex);
				SpatialReference layerRef = layer.GetSpatialRef();
				if(layerRef != null && srcSr == null) {
					srcSr = layerRef;
				}
				layer.SetSpatialFilter(null); // clear filter
				layer.ResetReading();  // reset iterator
				Feature feature = layer.GetNextFeature();
				while(feature != null) {
					Geometry geometry = feature.GetGeometryRef();
					if(geometry != null) {
						resultGeo.AddGeometry(geometry);
					} else {
						Logger.warn("missing geometry");
					}
					feature = layer.GetNextFeature();
				}
			}

			if(epsg >= 0) {
				SpatialReference dstSr;
				if(epsg == GeoUtil.EPSG_WEB_MERCATOR) {
					dstSr = GeoUtil.WEB_MERCATOR_SPATIAL_REFERENCE;
				} else {
					dstSr = GeoUtil.getSpatialReferenceFromEPSG(epsg);		
				}
				Transformer transformer = GeoUtil.createCoordinateTransformer(srcSr, dstSr);
				resultGeo.Transform(transformer.coordinateTransformation);
			}

			String json = resultGeo.ExportToJson();		
			//Logger.info(json);
			return json;
		} finally {
			closeDataSource(datasource);
		}
	}

	public static void deconGeo(Geometry geometry, Vec<Object[]> ps) {
		//Logger.info("geometry.GetGeometryName() " + geometry.GetGeometryName());
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
				Logger.warn("missing sub geometry");
			}
		}		
	}

	public DataSource getDataSource() {
		if(!hasDataFilename()) {
			throw new RuntimeException("no datasource in vector layer " + this.getName());
		}
		String fullFileName = config.dataPath.resolve(dataFilename).toString();
		//Logger.info(fullFileName);
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
			return getPoints(datasource, this);
		} finally {
			closeDataSource(datasource);
		}
	}

	public static double[][] getPoints(DataSource datasource, Object sync) { // layer iterator and layer filter possibly not parallel
		synchronized (sync) {		
			int layerCount = datasource.GetLayerCount();
			Vec<Object[]> ps = new Vec<Object[]>();
			for(int layerIndex=0; layerIndex<layerCount; layerIndex++) {
				Layer layer = datasource.GetLayerByIndex(layerIndex);
				layer.SetSpatialFilter(null); // clear filter
				layer.ResetReading();  // reset iterator
				Feature feature = layer.GetNextFeature();
				while(feature != null) {
					Geometry geometry = feature.GetGeometryRef();
					if(geometry != null) {
						deconGeo(geometry, ps);
					} else {
						Logger.warn("missing geometry");
					}
					feature = layer.GetNextFeature();
				}
			}
			int len = (int) ps.stream().mapToInt(points -> points.length).sum();
			double[][] points = new double[len][];
			int pos = 0;
			for(Object o:ps) {
				Object[] p = (Object[]) o;
				//Logger.info("len " + p.length);
				System.arraycopy(p, 0, points, pos, p.length);
				pos += p.length;
			}
			Logger.info("datasource.GetLayerCount() " + layerCount + "   points " + points.length);
			return points;	
		}
	}

	public static Rect2d getExtent(double[][] points) {
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
		return new Rect2d(xmin, ymin, xmax, ymax);
	}

	public Rect2d getExtent() {
		Rect2d extent = null;
		DataSource datasource = getDataSource();
		try {		
			extent = VectorDB.getExtent(getPoints(datasource, this));
		} finally {
			VectorDB.closeDataSource(datasource);
		}
		return extent;
	}

	public synchronized SpatialReference getSpatialReference() { // layer iterator and layer filter possibly not parallel
		String fullFileName = config.dataPath.resolve(dataFilename).toString();
		//Logger.info(fullFileName);
		DataSource datasource = ogr.Open(fullFileName);
		Layer layer = datasource.GetLayerByIndex(0);
		Feature feature = layer.GetNextFeature();
		Geometry geometry = feature.GetGeometryRef();
		if(geometry != null) {
			SpatialReference ref = geometry.GetSpatialReference();
			return ref;
		} else {
			Logger.warn("missing geometry");
			SpatialReference ref = layer.GetSpatialRef();
			return ref;
		}
	}

	public List<Path> getDataFilenames() {
		File[] fullfiles = config.dataPath.toFile().listFiles();
		if(fullfiles == null) {
			//throw new RuntimeException("data directory does not exist");
			Logger.warn("data directory does not exist");
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
	
	public boolean hasNameAttribute() {
		return nameAttribute != null && !nameAttribute.isBlank();
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

	public ACL getACL_owner() {
		return acl_owner;
	}

	public boolean isAllowed(UserIdentity userIdentity) {
		return AclUtil.isAllowed(acl_owner, acl_mod, acl, userIdentity);
	}

	public void check(UserIdentity userIdentity) {
		AclUtil.check(acl_owner, acl_mod, acl, userIdentity, "vectordb " + this.getName() + " read");
	}

	public void check(UserIdentity userIdentity, String location) {
		AclUtil.check(acl_owner, acl_mod, acl, userIdentity,  "vectordb " + this.getName() + " read " + " at " + location);
	}

	public boolean isAllowedMod(UserIdentity userIdentity) {
		return AclUtil.isAllowed(acl_owner, acl_mod, userIdentity);
	}

	public void checkMod(UserIdentity userIdentity) {
		AclUtil.check(acl_owner, acl_mod, userIdentity, "vectordb " + this.getName() + " modifiy");
	}

	public void checkMod(UserIdentity userIdentity, String location) {
		AclUtil.check(acl_owner, acl_mod, userIdentity, "vectordb " + this.getName() + " modify " + " at " + location);
	}

	public boolean isAllowedOwner(UserIdentity userIdentity) {
		return AclUtil.isAllowed(acl_owner, userIdentity);
	}

	public void checkOwner(UserIdentity userIdentity) {
		AclUtil.check(acl_owner, userIdentity, "vectordb " + this.getName() + " owner");
	}

	public void checkOwner(UserIdentity userIdentity, String location) {
		AclUtil.check(acl_owner, userIdentity, "vectordb " + this.getName() + " owner" + " at " + location);
	}

	public void setACL(ACL acl) {
		this.acl = acl;
		writeMeta();	
	}

	public void setACL_mod(ACL acl_mod) {
		this.acl_mod = acl_mod;
		writeMeta();	
	}

	public void setACL_owner(ACL acl_owner) {
		this.acl_owner = acl_owner;
		writeMeta();		
	}

	public String getDatatag() {
		return datatag;
	}

	public void refreshDatatag() {
		datatag = Long.toHexString(ThreadLocalRandom.current().nextLong());
	}

	public VectordbDetails getDetails() {
		//Logger.info("|getDetails()| " + getName());
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
						if("EPSG".equals(layerRef.GetAuthorityName("PROJCS"))) {
							String layerEPSG = layerRef.GetAuthorityCode("PROJCS");
							details.epsg = layerEPSG == null || layerEPSG.isBlank() ? "" : layerEPSG;
							//Logger.info(details.epsg);
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
				if((details.epsg == null || details.epsg.isBlank()) && !details.proj4.isEmpty()) {
					String epsg = GeoUtil.CRS_FACTORY.readEpsgFromParameters(details.proj4);
					details.epsg = epsg == null || epsg.isBlank() ? "" : epsg;
					//Logger.info(details.epsg);
				}
				//Logger.info("|" + details.proj4 + "|");
				details.attributes = attributes.readonlyWeakView();
			} finally {
				closeDataSource(datasource);
			}
		} catch(Exception e) {
			Logger.warn(e);
		}
		return details;
	}

	@FunctionalInterface
	public interface LayerFieldDefinitionConsumer {
		void consume(int layerIndex, int fieldIndex, String fieldName);
	}

	public void forEachFieldDefinition(LayerFieldDefinitionConsumer consumer) {
		DataSource datasource = null;
		try {
			datasource = getDataSource();			
			int layerCount = datasource.GetLayerCount();
			for(int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
				Layer layer = datasource.GetLayerByIndex(layerIndex);
				FeatureDefn featureDfn = layer.GetLayerDefn();
				int fieldCount = featureDfn.GetFieldCount();
				for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
					FieldDefn fieldDfn = featureDfn.GetFieldDefn(fieldIndex);
					String fieldName = fieldDfn.GetName();
					consumer.consume(layerIndex, fieldIndex, fieldName);
				}
			}
		} catch(Exception e) {
			Logger.warn(e);
		} finally {
			closeDataSource(datasource);
		}
	}

	@FunctionalInterface
	public interface VectorFeatureConsumer {
		void consume(VectorFeature vectorFeature) throws Exception;
	}

	public synchronized void forEachFeature(VectorFeatureConsumer consumer) { // layer iterator and layer filter possibly not parallel
		forEachFeature(null, consumer);
	}

	/**
	 * 
	 * @param ext nullable
	 * @param consumer
	 */
	public synchronized void forEachFeature(Rect2d ext, VectorFeatureConsumer consumer) { // layer iterator and layer filter possibly not parallel
		DataSource datasource = null;
		VectorFeature vectorFeature = new VectorFeature(null);
		try {
			datasource = getDataSource();
			int layerCount = datasource.GetLayerCount();
			for(int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
				Layer layer = datasource.GetLayerByIndex(layerIndex);
				try {
					if(ext == null) {
						layer.SetSpatialFilter(null); // clear filter
					} else {
						layer.SetSpatialFilterRect(ext.xmin, ext.ymin, ext.xmax, ext.ymax);
					}
					layer.ResetReading(); // reset iterator
					Feature feature = layer.GetNextFeature();
					while(feature != null) {
						vectorFeature.feature = feature;
						consumer.consume(vectorFeature);
						feature = layer.GetNextFeature();
					}
				} finally {
					layer.SetSpatialFilter(null); // clear filter
					layer.ResetReading(); // reset iterator
				}
			}			
		} catch(Exception e) {
			Logger.warn(e);
		} finally {
			vectorFeature = null;
			closeDataSource(datasource);
			datasource = null;
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

	public synchronized Vec<Poi> getPOIs(Vec<String> messages) { // layer iterator and layer filter possibly not parallel
		String nameAttr = getNameAttribute();
		DataSource datasource = getDataSource();
		try {
			Vec<Poi> pois = new Vec<Poi>();
			HashSet<String> poiNames = new HashSet<String>();
			int layerCount = datasource.GetLayerCount();
			for(int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
				Layer layer = datasource.GetLayerByIndex(layerIndex);
				int geomType = layer.GetGeomType();
				switch(geomType) {
				case 1: // Point
				case 2001: // PointM
				case 3001: // PointZM
				case 0x80000001: // Point25D
				case 4: // MultiPoint
				case 2004: // MultiPointM
				case 3004: // MultiPointZM
				case 0x80000004: // MultiPoint25D
				case 7: // GeometryCollection
				case 2007: // GeometryCollectionM
				case 3007: // GeometryCollectionZM
				case 0x80000007: { // GeometryCollection25D
					int fieldIndex = layer.FindFieldIndex(nameAttr, 1);
					if(fieldIndex < 0) {
						messages.add("Unnamed POIs: name attribute not found: " + nameAttr + "  in " + getName());
					}
					layer.SetSpatialFilter(null); // clear filter
					layer.ResetReading();  // reset iterator
					Feature feature = layer.GetNextFeature();
					while(feature != null) {
						collectPOI(feature, fieldIndex, poiNames, pois, messages);
						feature = layer.GetNextFeature();
					}
					break;
				}
				default: {
					messages.add("Skip POIs: unknown geometry type for POIs: " + geomType + "  in " + getName());	
				}					
				}
			}
			Logger.info("get POIs of " + getName() + "   count: " + pois.size());
			return pois;
		} catch(Exception e) {
			Logger.warn(e);
			throw e;
		} finally {
			closeDataSource(datasource);
		}
	}

	public synchronized Vec<Roi> getROIs(Vec<String> messages) { // layer iterator and layer filter possibly not parallel
		String nameAttr = getNameAttribute();
		DataSource datasource = getDataSource();
		try {
			Vec<Roi> rois = new Vec<Roi>();
			HashSet<String> roiNames = new HashSet<String>();
			int layerCount = datasource.GetLayerCount();
			for(int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
				Layer layer = datasource.GetLayerByIndex(layerIndex);
				int geomType = layer.GetGeomType();
				switch(geomType) {
				case 3: // Polygon
				case 2003: // PolygonM
				case 3003: // PolygonZM
				case 0x80000003: // Polygon25D
				case 6: // MultiPolygon
				case 2006: // MultiPolygonM
				case 3006: // MultiPolygonZM
				case 0x80000006: // MultiPolygon25D
				case 7: // GeometryCollection
				case 2007: // GeometryCollectionM
				case 3007: // GeometryCollectionZM
				case 0x80000007: { // GeometryCollection25D					
					int fieldIndex = layer.FindFieldIndex(nameAttr, 1);
					if(fieldIndex < 0) {
						messages.add("Unnamed ROIs: name attribute not found: " + nameAttr + "  in " + getName());
					}
					layer.SetSpatialFilter(null); // clear filter
					layer.ResetReading();  // reset iterator
					Feature feature = layer.GetNextFeature();
					while(feature != null) {
						collectROI(feature, fieldIndex, roiNames, rois, messages);
						feature = layer.GetNextFeature();
					}					
					break;
				}
				default: {
					messages.add("Skip ROIs: unknown geometry type for ROIs: " + geomType + "  in " + getName());	
				}					
				}
			}
			Logger.info("get ROIs of " + getName() + "   count: " + rois.size());
			return rois;
		} catch(Exception e) {
			Logger.warn(e);
			throw e;
		} finally {
			closeDataSource(datasource);
		}
	}

	private void collectPOI(Feature feature, int fieldIndex, HashSet<String> poiNames, Vec<Poi> pois, Vec<String> messages) {
		String orgName = null;
		if(fieldIndex >= 0 && feature.IsFieldSet(fieldIndex)) {
			orgName = feature.GetFieldAsString(fieldIndex);
		}
		if(orgName == null || orgName.isEmpty()) {
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
			collectPOI_geometry(geometry, name, pois, messages);				
		} else {
			messages.add("Skip POI: missing geometry in " + getName() + " : " + name);
		}

	}

	private void collectPOI_geometry(Geometry geometry, String name, Vec<Poi> pois, Vec<String> messages) {
		int geometryType = geometry.GetGeometryType();
		int geoCount = geometry.GetGeometryCount();
		//Logger.info("geometry: " + geometryType +" with " + geoCount + " in " + getName() + " : " + name);
		switch(geometryType) {
		case 1: // Point
		case 2001: // PointM
		case 3001: // PointZM
		case 0x80000001: { // Point25D
			if(geoCount == 0) {
				double x = geometry.GetX();
				double y = geometry.GetY();
				pois.add(new Poi(name, x, y));
			} else {
				messages.add("Skip POI: sub geometries in Point  in " + getName() + " : " + name);
			}					
			break;
		}
		case 4: // MultiPoint
		case 2004: // MultiPointM
		case 3004: // MultiPointZM
		case 0x80000004: { // MultiPoint25D
			if(geoCount > 0) {
				if(geoCount == 1) {
					Geometry subGeometry = geometry.GetGeometryRef(0);
					if(subGeometry != null) {
						collectPOI_geometry(subGeometry, name, pois, messages);
					} else {
						messages.add("Skip POI: missing sub geometry of MultiPoint  in " + getName() + " : " + name);
					}
				} else {
					messages.add("Skip POI: more than one sub geometry of MultiPoint: " + geoCount +  " in " + getName() + " : " + name);
				}
			} else {
				messages.add("Skip POI: no sub geometry of MultiPoint  in " + getName() + " : " + name);
			}					
			break;
		}
		case 7: // GeometryCollection
		case 2007: // GeometryCollectionM
		case 3007: // GeometryCollectionZM
		case 0x80000007: { // GeometryCollection25D
			if(geoCount > 0) {
				if(geoCount == 1) {
					Geometry subGeometry = geometry.GetGeometryRef(0);
					if(subGeometry != null) {
						collectPOI_geometry(subGeometry, name, pois, messages);
					} else {
						messages.add("Skip POI: missing sub geometry of GeometryCollection  in " + getName() + " : " + name);
					}
				} else {
					messages.add("Skip POI: more than one sub geometry of GeometryCollection: " + geoCount +  " in " + getName() + " : " + name);
				}
			} else {
				messages.add("Skip POI: no sub geometry of GeometryCollection  in " + getName() + " : " + name);
			}					
			break;
		}
		default: {
			messages.add("Skip POI: unknown geometry type " + geometryType + "  in " + getName() + " : " + name);
		}
		}
	}

	private void collectROI(Feature feature, int fieldIndex, HashSet<String> roiNames, Vec<Roi> rois, Vec<String> messages) {
		String orgName = null;
		if(fieldIndex >= 0 && feature.IsFieldSet(fieldIndex)) {
			orgName = feature.GetFieldAsString(fieldIndex);
		}
		if(orgName == null || orgName.isEmpty()) {
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
			try {
				Vec<PolygonWithHoles> polygons = new Vec<PolygonWithHoles>();
				collectROI_geometry(geometry, name, polygons, messages);
				if(!polygons.isEmpty()) {
					//Logger.info(polygons);
					PolygonWithHoles[] pwhs = polygons.toArray(PolygonWithHoles[]::new);
					//Logger.info(Arrays.toString(pwhs));
					Roi roi = new Roi(name, pwhs);
					rois.add(roi);
				}
			} catch(Exception e) {
				messages.add("Skip ROI: error in geometry: " + e.getMessage() + "   in " + getName() + " : " + name);
			}
		} else {
			messages.add("Skip ROI: missing geometry in " + getName() + " : " + name);
		}
	}

	private void collectROI_geometry(Geometry geometry, String name, Vec<PolygonWithHoles> polygons, Vec<String> messages) {
		int geometryType = geometry.GetGeometryType();
		int geoCount = geometry.GetGeometryCount();
		//Logger.info("geometry: " + geometryType +" with " + geoCount + " in " + getName() + " : " + name);
		switch(geometryType) {
		case 3: // Polygon
		case 2003: // PolygonM
		case 3003: // PolygonZM
		case 0x80000003: { // Polygon25D
			collectROI_polygon(geometry, name, polygons, messages);		
			break;
		}
		case 6: // MultiPolygon
		case 2006: // MultiPolygonM
		case 3006: // MultiPolygonZM
		case 0x80000006: { // MultiPolygon25D
			if(geoCount > 0) {
				if(geoCount == 1) {
					Geometry subGeometry = geometry.GetGeometryRef(0);
					if(subGeometry != null) {
						int subGeometryType = subGeometry.GetGeometryType();
						switch(subGeometryType) {
						case 3: // Polygon
						case 2003: // PolygonM
						case 3003: // PolygonZM
						case 0x80000003: { // Polygon25D
							collectROI_polygon(subGeometry, name, polygons, messages);						
							break;
						}
						default: {
							messages.add("Skip ROI: unknown sub geometry type of MultiPolygon " + subGeometryType + "  in " + getName() + " : " + name);
						}
						}
					} else {
						messages.add("Skip ROI: missing sub geometry of MultiPolygon  in " + getName() + " : " + name);
					}
				} else {
					messages.add("Skip ROI: more than one sub geometry of MultiPolygon: " + geoCount +  " in " + getName() + " : " + name);
				}
			} else {
				messages.add("Skip ROI: no sub geometry of MultiPolygon  in " + getName() + " : " + name);
			}					
			break;
		}
		case 7: // GeometryCollection
		case 2007: // GeometryCollectionM
		case 3007: // GeometryCollectionZM
		case 0x80000007: { // GeometryCollection25D
			if(geoCount > 0) {
				if(geoCount == 1) {
					Geometry subGeometry = geometry.GetGeometryRef(0);
					if(subGeometry != null) {
						collectROI_geometry(subGeometry, name, polygons, messages);
					} else {
						messages.add("Skip ROI: missing sub geometry of GeometryCollection  in " + getName() + " : " + name);
					}
				} else {
					messages.add("Skip ROI: more than one sub geometry of GeometryCollection: " + geoCount +  " in " + getName() + " : " + name);
				}
			} else {
				messages.add("Skip ROI: no sub geometry of GeometryCollection  in " + getName() + " : " + name);
			}					
			break;
		}
		default: {
			messages.add("Skip ROI: unknown geometry type " + geometryType + "  in " + getName() + " : " + name);
		}
		}	
	}

	private void collectROI_polygon(Geometry geometry, String name, Vec<PolygonWithHoles> polygons, Vec<String> messages) {
		int geometryCount = geometry.GetGeometryCount();
		if(geometryCount > 0) {			
			Point2d[][] rings = new Point2d[geometryCount][];
			for (int subGeometryIndex = 0; subGeometryIndex < geometryCount; subGeometryIndex++) {
				Geometry subGeometry = geometry.GetGeometryRef(subGeometryIndex);
				if(subGeometry != null) {
					int subGeometryType = subGeometry.GetGeometryType();								
					switch(subGeometryType) {
					case 2: // LineString
					case 2002: // LineStringM
					case 3002: // LineStringZM
					case 0x80000002: { // LineString25D
						Object[] polygonPoints = subGeometry.GetPoints();
						int pointsLen = polygonPoints.length;
						//Logger.info("LINESTRING points: " + pointsLen);
						Point2d[] points = new Point2d[pointsLen];
						for(int i = 0; i < pointsLen; i++) {
							double[] p = (double[]) polygonPoints[i];
							points[i] = new Point2d(p[0], p[1]);
						}
						for(int i = 0; i < pointsLen - 1; i++) {
							Point2d p0 = points[i];
							Point2d p1 = points[i + 1];
							if(p0.equals(p1)) {
								messages.add("ROI warning: same point " + (i+1) + " and " + (i+2) + ": (" + p0.x + ", " + p0.y + ") and (" + p1.x + ", " + p1.y +")  in " + getName() + " : " + name);
							}
						}
						rings[subGeometryIndex] = points;
						break;
					}
					default: {
						messages.add("Skip ROI: unknown sub geometry" + subGeometryIndex + " type of Polygon " + subGeometryType + "  in " + getName() + " : " + name);
						return;
					}
					}
				} else {
					messages.add("Skip ROI: missing sub geometry " + subGeometryIndex + " in Polygon  in " + getName() + " : " + name);
					return;
				}
			}
			try {
				PolygonWithHoles polygon = PolygonWithHoles.ofRings(rings);
				polygons.add(polygon);
			} catch(Exception e) {		
				messages.add("Skip ROI: not valid polygon points:  " + e.getMessage() + "  in" + getName() + " : " + name);
				return;
			}

			/*if(geometryCount == 1) {
				Geometry subGeometry = geometry.GetGeometryRef(0);
				if(subGeometry != null) {
					int subGeometryType = subGeometry.GetGeometryType();								
					switch(subGeometryType) {
					case 2: // LineString
					case 2002: // LineStringM
					case 3002: // LineStringZM
					case 0x80000002: { // LineString25D
						Object[] polygonPoints = subGeometry.GetPoints();
						int pointsLen = polygonPoints.length;
						//Logger.info("LINESTRING points: " + pointsLen);
						Point2d[] points = new Point2d[pointsLen];
						for(int i = 0; i < pointsLen; i++) {
							double[] p = (double[]) polygonPoints[i];
							points[i] = new Point2d(p[0], p[1]);
						}
						try {
							NamedPolygon polygon = new NamedPolygon(name, points);
							polygons.add(polygon);
						} catch(Exception e) {		
							messages.add("Skip ROI: not valid polygon points:  " + e.getMessage() + "  in" + getName() + " : " + name);
							return;
						}
						break;
					}
					default: {
						messages.add("Skip ROI: unknown sub geometry type of Polygon " + subGeometryType + "  in " + getName() + " : " + name);
						return;
					}
					}
				} else {
					messages.add("Skip ROI: missing sub geometry in Polygon  in " + getName() + " : " + name);
					return;
				}
			} else {
				messages.add("Skip ROI: multiple sub geometries in Polygon, count: " + geometryCount + "  in " + getName() + " : " + name);
				return;
			}*/
		} else {
			messages.add("Skip ROI: no sub geometry in Polygon  in " + getName() + " : " + name);
			return;
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

	public void setStyle(Style style) {
		this.style = style;		
	}

	public Style getStyle() {
		return style;
	}
}
