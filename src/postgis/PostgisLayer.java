package postgis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;
import org.tinylog.Logger;
import org.yaml.snakeyaml.Yaml;

import broker.Informal;
import broker.acl.ACL;
import broker.acl.AclUtil;
import broker.acl.EmptyACL;
import net.postgis.jdbc.PGbox2d;
import net.postgis.jdbc.PGgeometry;
import net.postgis.jdbc.geometry.Geometry;
import net.postgis.jdbc.geometry.MultiPolygon;
import net.postgis.jdbc.geometry.Point;
import net.postgis.jdbc.geometry.Polygon;
import pointcloud.Rect2d;
import util.Timer;
import util.Util;
import util.collections.array.ReadonlyArray;
import util.collections.vec.Vec;
import util.yaml.YamlMap;

public class PostgisLayer extends PostgisLayerBase {

	private static final String TYPE = "PostGIS";

	private final PostgisConnector postgisConnector;

	private final PostgisColumn[] colsAll;
	public final ReadonlyArray<PostgisColumn> columnsAll;
	private final PostgisColumn[] flds;
	public final ReadonlyArray<PostgisColumn> fields;
	public final String primaryGeometryColumn;	
	private final String gmlQuerySelectorWithFields;
	private final String geoJSONQuerySelector;
	private final String geoJSONQuerySelectorWithFields;

	private ReadonlyArray<String> class_attributes;

	private ACL acl = EmptyACL.ADMIN;
	private ACL acl_mod = EmptyACL.ADMIN;
	private ACL acl_owner = EmptyACL.ADMIN;
	private Informal informal = Informal.EMPTY;

	private final Path metaPathTemp;
	private final File metaFileTemp;

	public static class PostgisColumn {

		public String name;
		public String type;

		public PostgisColumn(String name, String type) {
			this.name = name;
			this.type = type;
		}
	}

	public PostgisLayer(PostgisLayerConfig postgisLayerConfig, PostgisConnector postgisConnector) {	
		super(postgisLayerConfig.name, postgisLayerConfig.metaPath);
		class_attributes = new ReadonlyArray<String>(new String[]{});

		metaPathTemp = Paths.get(postgisLayerConfig.metaPath.toString()+"_temp");
		metaFileTemp = metaPathTemp.toFile();

		readMeta();

		Util.checkStrictDotID(postgisLayerConfig.name);		
		this.postgisConnector = postgisConnector;

		try (Connection conn = postgisConnector.getConnection()) {
			String sql = String.format("SELECT * FROM %s WHERE false",  name);	
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			ResultSetMetaData meta = rs.getMetaData();
			int cnt = meta.getColumnCount();
			colsAll = new PostgisColumn[cnt];
			for (int i = 0; i < cnt; i++) {
				String colName = meta.getColumnName(i + 1);
				String colType = meta.getColumnTypeName(i + 1);
				PostgisColumn postgisColumn = new PostgisColumn(colName, colType);
				colsAll[i] = postgisColumn;
			}
			columnsAll = new ReadonlyArray<PostgisColumn>(colsAll);

			String pgc = null;
			Vec<PostgisColumn> f = new Vec<PostgisColumn>();
			for(PostgisColumn col : colsAll) {
				if(col.type.equals("geometry")) {
					if(pgc == null) {
						pgc = col.name;
					} else {
						Logger.warn("more than one geometry column in table");
					}
				} else {
					f.add(col);
				}
			}
			if(pgc == null) {
				throw new RuntimeException("missing geometry column");
			}
			primaryGeometryColumn = pgc;
			flds = f.toArray(PostgisColumn[]::new);
			fields = new ReadonlyArray<PostgisColumn>(flds);			

			{
				String s = "ST_AsGML(3," + primaryGeometryColumn + ",8,2)";
				for (PostgisColumn field : flds) {
					s += "," + field.name;
				}
				gmlQuerySelectorWithFields = s;
			}

			{
				String s = "ST_AsGeoJSON(" + primaryGeometryColumn + ")";
				geoJSONQuerySelector = s;
				for (PostgisColumn field : flds) {
					s += "," + field.name;
				}
				geoJSONQuerySelectorWithFields = s;
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}


	}

	public long getFeatures() {
		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");

			String sql = String.format("SELECT LB_AKT FROM %s WHERE ST_Intersects(ST_MakeEnvelope(529779, 5363962, 530354, 5364330, 25832), geom)",  name);			
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			long featureCount = 0;
			while(rs.next()) {
				String area = rs.getString(1);
				Logger.info(area);
				featureCount++;
			}
			Logger.info("features " + featureCount);
			Logger.info(Timer.stop("query"));
			return featureCount;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;	
		}	
	}

	/**
	 * 
	 * @param rect2d  nullable
	 * @param consumer
	 * @return
	 */
	public long forEachGeoJSON(Rect2d rect2d, Consumer<String> consumer) {
		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");
			String sql;
			if(rect2d == null) {
				sql = String.format("SELECT %s FROM %s",  geoJSONQuerySelector, name);
			} else {
				sql = String.format("SELECT %s FROM %s WHERE geom && ST_MakeEnvelope(%s, %s, %s, %s, %s)",  geoJSONQuerySelector, name, rect2d.xmin, rect2d.ymin, rect2d.xmax, rect2d.ymax, getEPSG());	
			}
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			long featureCount = 0;
			while(rs.next()) {
				String geojson = rs.getString(1);
				//Logger.info(geojson);
				consumer.accept(geojson);
				featureCount++;
			}
			Logger.info("features " + featureCount);
			Logger.info(Timer.stop("query"));
			return featureCount;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;	
		}	
	}

	public interface FeatureConsumer {
		void acceptFeatureStart(boolean isFirstFeature);
		void acceptFeatureGeometry(String geometry);
		void acceptFeatureFieldsStart();
		void acceptFeatureField(PostgisColumn field, String fieldValue, boolean isFirstFeatureField);
		void acceptFeatureFieldNull(PostgisColumn field, boolean isFirstFeatureField);
		void acceptFeatureFieldInt32(PostgisColumn field, int fieldValue, boolean isFirstFeatureField);
		void acceptFeatureFieldsEnd();
		void acceptFeatureEnd();
	}

	/**
	 * 
	 * @param rect2d  nullable
	 * @param consumer
	 * @return
	 */
	public long forEachGeoJSONWithProperties(Rect2d rect2d, FeatureConsumer consumer) {
		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");
			String sql;
			if(rect2d == null) {
				sql = String.format("SELECT %s FROM %s",  geoJSONQuerySelectorWithFields, name);
			} else {
				sql = String.format("SELECT %s FROM %s WHERE geom && ST_MakeEnvelope(%s, %s, %s, %s, %s)",  geoJSONQuerySelectorWithFields, name, rect2d.xmin, rect2d.ymin, rect2d.xmax, rect2d.ymax, getEPSG());	
			}
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			long featureCount = 0;
			while(rs.next()) {
				consumer.acceptFeatureStart(featureCount == 0);
				String geometry = rs.getString(1);
				consumer.acceptFeatureGeometry(geometry);
				consumer.acceptFeatureFieldsStart();
				for (int i = 0; i < flds.length; i++) {
					PostgisColumn field = flds[i];
					switch(field.type) {
					case "serial":
					case "int4": {
						int fieldValue = rs.getInt(i + 2);
						if(rs.wasNull()) {
							consumer.acceptFeatureFieldNull(field, i == 0);
						} else {
							consumer.acceptFeatureFieldInt32(field, fieldValue, i == 0);
						}
						break;
					}
					case "varchar":
					default: {
						//Logger.info("field type: " + field.type);
						String fieldValue = rs.getString(i + 2);
						if(rs.wasNull()) {
							consumer.acceptFeatureFieldNull(field, i == 0);
						} else {
							consumer.acceptFeatureField(field, fieldValue, i == 0);
						}
					}
					}
				}
				consumer.acceptFeatureFieldsEnd();
				consumer.acceptFeatureEnd();
				featureCount++;
			}
			Logger.info("features " + featureCount);
			Logger.info(Timer.stop("query"));
			return featureCount;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;	
		}	
	}

	public long getGeo(JSONWriter json) {
		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");

			String sql = String.format("SELECT ST_AsGeoJSON(geom) FROM %s",  name);			
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			long featureCount = 0;
			json.array();
			while(rs.next()) {
				String area = rs.getString(1);
				Logger.info(area);

				json.object();

				json.key("type");
				json.value("Feature");

				json.key("geometry");
				json.value(area);

				json.endObject();				

				featureCount++;
			}
			json.endArray();
			Logger.info("features " + featureCount);
			Logger.info(Timer.stop("query"));
			return featureCount;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;	
		}	
	}

	public long forEachGML(Rect2d rect2d, Consumer<String> consumer) {
		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");
			String sql;
			if(rect2d == null) {
				sql = String.format("SELECT ST_AsGML(3, geom, 8, 2) FROM %s",  name);		
			} else {
				sql = String.format("SELECT ST_AsGML(3, geom, 8, 2) FROM %s WHERE geom && ST_MakeEnvelope(%s, %s, %s, %s, %s)",  name, rect2d.xmin, rect2d.ymin, rect2d.xmax, rect2d.ymax, getEPSG());	
			}	
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			long featureCount = 0;
			while(rs.next()) {
				String gml = rs.getString(1);
				//Logger.info(gml);
				consumer.accept(gml);
				featureCount++;
			}
			Logger.info("features " + featureCount);
			Logger.info(Timer.stop("query"));
			return featureCount;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;	
		}	
	}

	public ResultSet queryGML(Rect2d rect2d) {
		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");
			String sql;
			if(rect2d == null) {
				sql = String.format("SELECT %s FROM %s", gmlQuerySelectorWithFields, name);		
			} else {
				sql = String.format("SELECT %s FROM %s WHERE geom && ST_MakeEnvelope(%s, %s, %s, %s, %s)", gmlQuerySelectorWithFields, name, rect2d.xmin, rect2d.ymin, rect2d.xmax, rect2d.ymax, getEPSG());	
			}	
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			return rs;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;	
		}	
	}

	public int getEPSG() {
		try (Connection conn = postgisConnector.getConnection()) {
			String sql = String.format("SELECT ST_SRID(geom) FROM %s LIMIT 1",  name);	
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				int epsg = rs.getInt(1);
				return epsg;
			} else {
				throw new RuntimeException("no features for SRID");
			}			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}	
	}

	private String getName() {
		return name;
	}

	public synchronized void readMeta() { // throws if read error
		try {
			if (metaFile.exists()) {
				YamlMap map;
				try(InputStream in = new FileInputStream(metaFile)) {
					map = YamlMap.ofObject(new Yaml().load(in));
				}
				yamlToMeta(map);
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
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(metaFileTemp)));
			yaml.dump(map, out);
			out.close();
			Files.move(metaPathTemp, metaPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.warn(e);
			throw new RuntimeException(e);
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
		class_attributes = yamlMap.optList("class_attributes").asReadonlyStrings();
	}

	private synchronized void metaToYaml(LinkedHashMap<String, Object> map) {
		map.put("type", TYPE);
		map.put("acl", acl.toYaml());
		map.put("acl_mod", acl_mod.toYaml());
		map.put("acl_owner", acl_owner.toYaml());
		informal.writeYaml(map);
		map.put("class_attributes", class_attributes);
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
		AclUtil.check(acl_owner, acl_mod, acl, userIdentity, "postgis " + this.getName() + " read");
	}

	public void check(UserIdentity userIdentity, String location) {
		AclUtil.check(acl_owner, acl_mod, acl, userIdentity,  "postgis " + this.getName() + " read " + " at " + location);
	}

	public boolean isAllowedMod(UserIdentity userIdentity) {
		return AclUtil.isAllowed(acl_owner, acl_mod, userIdentity);
	}

	public void checkMod(UserIdentity userIdentity) {
		AclUtil.check(acl_owner, acl_mod, userIdentity, "postgis " + this.getName() + " modifiy");
	}

	public void checkMod(UserIdentity userIdentity, String location) {
		AclUtil.check(acl_owner, acl_mod, userIdentity, "postgis " + this.getName() + " modify " + " at " + location);
	}

	public boolean isAllowedOwner(UserIdentity userIdentity) {
		return AclUtil.isAllowed(acl_owner, userIdentity);
	}

	public void checkOwner(UserIdentity userIdentity) {
		AclUtil.check(acl_owner, userIdentity, "postgis " + this.getName() + " owner");
	}

	public void checkOwner(UserIdentity userIdentity, String location) {
		AclUtil.check(acl_owner, userIdentity, "postgis " + this.getName() + " owner" + " at " + location);
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

	public Informal informal() {
		return informal;
	}

	public void setInformal(Informal informal) {
		this.informal = informal;
		writeMeta();
	}

	public ReadonlyArray<String> getClass_attributes() {
		return class_attributes;
	}

	public interface GeometryConsumer {

		void acceptPolygon(Polygon polygon);

		default void acceptMultiPolygon(MultiPolygon multiPolygon) {
			Polygon[] polygons = multiPolygon.getPolygons();
			for(Polygon polygon : polygons) {
				acceptPolygon(polygon);
			}
		}
	}

	public long forEachGeometry(Rect2d rect2d, GeometryConsumer consumer) {
		Logger.info("getGeometry");

		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");
			String sql;
			if(rect2d == null) {
				sql = String.format("SELECT %s FROM %s", primaryGeometryColumn, name);		
			} else {
				sql = String.format("SELECT %s FROM %s WHERE geom && ST_MakeEnvelope(%s, %s, %s, %s, %s)", primaryGeometryColumn, name, rect2d.xmin, rect2d.ymin, rect2d.xmax, rect2d.ymax, getEPSG());	
			}	
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			long featureCount = 0;
			while(rs.next()) {
				Object obj = rs.getObject(1);
				if(obj != null) {
					if(obj instanceof PGgeometry) {
						PGgeometry pgGeometry = (PGgeometry) obj;
						Geometry geo = pgGeometry.getGeometry();
						if(geo instanceof Polygon) {
							consumer.acceptPolygon((Polygon) geo);
						} else if(geo instanceof MultiPolygon) {
							consumer.acceptMultiPolygon((MultiPolygon) geo);
						} else {
							Logger.info("unknown geometry: " + geo.getClass());
						}
						featureCount++;
					} else {
						Logger.info("unknown object: " + obj.getClass());
					}
				} else {
					//Logger.info("object null");
				}
			}
			Logger.info("features " + featureCount);
			Logger.info(Timer.stop("query"));
			return featureCount;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;	
		}
	}

	private Rect2d extent = null;

	public Rect2d getExtent() {
		Rect2d e = extent;
		if(e == null) {
			e = calcExtent();
			extent = e;
		}
		return e;
	}

	private synchronized Rect2d calcExtent() {
		if(extent != null) {
			return extent;
		}
		Logger.info("calcExtent");

		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");
			String sql = String.format("SELECT ST_Extent(%s) FROM %s", primaryGeometryColumn, name);		

			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				Object obj = rs.getObject(1);
				Logger.info(Timer.stop("query"));
				if(obj != null) {
					if(obj instanceof PGbox2d) {
						PGbox2d box = (PGbox2d) obj;
						Point p1 = box.getLLB();
						Point p2 = box.getURT();
						Rect2d rect2d = new Rect2d(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), Math.max(p1.x, p2.x), Math.max(p1.y, p2.y));						
						return rect2d;
					} else {
						Logger.info(obj.getClass());
						return null;
					}
				} else {
					return null;
				}
			} else {
				return null;
			}			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;	
		}
	}

	private int itemCount = Integer.MIN_VALUE;

	public int getItemCount() {
		int i = itemCount;
		if(i == Integer.MIN_VALUE) {
			i = calcItemCount();
			itemCount = i;
		}
		return i;
	}

	private synchronized int calcItemCount() {
		if(itemCount != Integer.MIN_VALUE) {
			return itemCount;
		}
		Logger.info("calcItemCount");

		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");
			String sql = String.format("SELECT count(*) FROM %s", name);		

			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				int i = rs.getInt(1);
				return i;
			} else {
				return -1;
			}			
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;	
		}
	}

	ReadonlyArray<String> geometryTypes = null;

	public ReadonlyArray<String> getGeometryTypes() {
		ReadonlyArray<String> e = geometryTypes;
		if(e == null) {
			e = calcGeometryTypes();
			geometryTypes = e;
		}
		return e;
	}

	private synchronized ReadonlyArray<String> calcGeometryTypes() {
		if(geometryTypes != null) {
			return geometryTypes;
		}
		Logger.info("calcGeometryTypes");

		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");
			String sql = String.format("SELECT DISTINCT ST_GeometryType(%s) FROM %s", primaryGeometryColumn, name);		

			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			Vec<String> vec = new Vec<String>();
			while(rs.next()) {
				String s = rs.getString(1);
				if(s != null && !s.isBlank()) {
					if(s.startsWith("ST_")) {
						vec.add(s.substring(3));
					} else {
						vec.add(s);	
					}
				}
			}			
			return vec.copyReadonly();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;	
		}
	}
}
