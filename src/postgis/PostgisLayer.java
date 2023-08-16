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
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.jetty.server.UserIdentity;
import org.locationtech.jts.io.WKBReader;
import org.tinylog.Logger;
import org.yaml.snakeyaml.Yaml;

import broker.Informal;
import broker.acl.ACL;
import broker.acl.AclUtil;
import broker.acl.EmptyACL;
import net.postgis.jdbc.PGbox2d;
import net.postgis.jdbc.PGgeometry;
import net.postgis.jdbc.geometry.Geometry;
import net.postgis.jdbc.geometry.Point;
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
	private String geometrySQL;
	private final String selectorFields;

	private ReadonlyArray<String> class_fields;

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
		class_fields = new ReadonlyArray<String>(new String[]{});

		metaPathTemp = Paths.get(postgisLayerConfig.metaPath.toString()+"_temp");
		metaFileTemp = metaPathTemp.toFile();

		readMeta();

		Util.checkStrictDotID(postgisLayerConfig.name);		
		this.postgisConnector = postgisConnector;

		try (Connection conn = postgisConnector.getConnection()) {
			String sql = String.format("SELECT * FROM %s WHERE false",  name);	
			Logger.info(sql);
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
			geometrySQL = primaryGeometryColumn;
			flds = f.toArray(PostgisColumn[]::new);
			fields = new ReadonlyArray<PostgisColumn>(flds);			

			{
				String s = "";
				boolean first = true;
				for (PostgisColumn field : flds) {
					if(first) {
						first = false;
					} else {
						s += ", ";
					}
					s += field.name;
				}
				selectorFields = s;
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		
		if(isInvalid() != null) {			
			geometrySQL = String.format("ST_MakeValid(%s)", primaryGeometryColumn);
		}
	}

	public int getEPSG() {
		try (Connection conn = postgisConnector.getConnection()) {
			String sql = String.format("SELECT ST_SRID(%s) FROM %s LIMIT 1", primaryGeometryColumn,  name);	
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
	
	public Vec<String> isInvalid() {
		Vec<String> vec = null;
		try (Connection conn = postgisConnector.getConnection()) {
			//String sql = String.format("SELECT ST_IsValidDetail(%s) FROM %s WHERE NOT ST_IsValid(%s)", primaryGeometryColumn,  name, primaryGeometryColumn);
			String sql = String.format("WITH v as (SELECT ST_IsValidDetail(%s) as va FROM %s) SELECT (va).reason, ST_AsText((va).location) FROM v WHERE NOT (va).valid", primaryGeometryColumn,  name);
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				String reason = rs.getString(1);
				String location = rs.getString(2);
				String s = reason + "  " + location;
				Logger.info(s);
				if(vec == null) {
					vec = new Vec<String>();
				}
				vec.add(s);
			}			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return vec;	
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
		class_fields = yamlMap.optList("class_fields").asReadonlyStrings();
		if(class_fields.isEmpty()) {
			class_fields = yamlMap.optList("class_attributes").asReadonlyStrings(); // legacy
		}
	}

	private synchronized void metaToYaml(LinkedHashMap<String, Object> map) {
		map.put("type", TYPE);
		map.put("acl", acl.toYaml());
		map.put("acl_mod", acl_mod.toYaml());
		map.put("acl_owner", acl_owner.toYaml());
		informal.writeYaml(map);
		map.put("class_attributes", class_fields);
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

	public ReadonlyArray<String> getClass_fields() {
		return class_fields;
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

	private static String SQL_ST_MakeEnvelope(Rect2d rect2d, int srid) {
		return String.format("ST_MakeEnvelope(%s, %s, %s, %s, %s)", rect2d.xmin, rect2d.ymin, rect2d.xmax, rect2d.ymax, srid);
	}
	
	private static String SQL_ST_Intersects(String column, Rect2d rect2d, int srid) {
		String sql_ST_MakeEnvelope = SQL_ST_MakeEnvelope(rect2d, srid);
		return String.format("ST_Intersects(%s, %s)", column, sql_ST_MakeEnvelope);
	}
	
	private static String SQL_ST_Intersection(String column, Rect2d rect2d, int srid) {
		String sql_ST_MakeEnvelope = SQL_ST_MakeEnvelope(rect2d, srid);
		return String.format("ST_Intersection(%s, %s)", column, sql_ST_MakeEnvelope);
		//return String.format("ST_MakeValid(ST_Intersection(%s, %s))", column, sql_ST_MakeEnvelope);
		//return String.format("ST_Intersection(ST_MakeValid(%s), %s)", column, sql_ST_MakeEnvelope);
		//return String.format("ST_Buffer(ST_Intersection(%s, %s), 0.0)", column, sql_ST_MakeEnvelope);
		//return String.format("ST_Intersection(ST_Buffer(%s, 0.0), %s)", column, sql_ST_MakeEnvelope);
	}
	
	private static String SQL_ST_AsGML(String column) {
		return String.format("ST_AsGML(3, %s, 8, 2)", column);
	}
	
	private static int hexToInt(int h) {
		if ('0' <= h && h <= '9') {
			return h - '0';
		}
		if ('A' <= h && h <= 'F') {
			return h - 'A' + 10;
		}
		return -1;
	}
	
	public long forEachGeometry(Rect2d rect2d, boolean crop, GeometryConsumer consumer) {
		//Logger.info("getGeometry");

		try (Connection conn = postgisConnector.getConnection()) {
			//Timer.start("query");
			String sql;
			if(rect2d == null) {
				sql = String.format("SELECT %s FROM %s", geometrySQL, name);		
			} else {
				int epsg = getEPSG();
				String sql_ST_Intersects = SQL_ST_Intersects(primaryGeometryColumn, rect2d, epsg);				
				if(crop) {
					String sql_ST_Intersection = SQL_ST_Intersection(geometrySQL, rect2d, epsg);
					sql = String.format("SELECT %s FROM %s WHERE %s", sql_ST_Intersection, name, sql_ST_Intersects);	
				} else {
					sql = String.format("SELECT %s FROM %s WHERE %s", geometrySQL, name, sql_ST_Intersects);
				}				}	
			//Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			long featureCount = 0;
			while(rs.next()) {
				Object obj = rs.getObject(1);
				if(obj != null) {
					if(obj instanceof PGgeometry) {
						PGgeometry pgGeometry = (PGgeometry) obj;
						Geometry geometry = pgGeometry.getGeometry();
						consumer.acceptGeometry(geometry);						
						featureCount++;
					} else {
						Logger.info("unknown object: " + obj.getClass());
					}
				} else {
					//Logger.info("object null");
				}
			}
			//Logger.info("features " + featureCount);
			//Logger.info(Timer.stop("query"));
			return featureCount;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;	
		}
	}
	
	public long forEachJTSGeometry(Rect2d rect2d, boolean crop, JTSGeometryConsumer consumer) {
		//Logger.info("getGeometry");

		try (Connection conn = postgisConnector.getConnection()) {
			//Timer.start("query");
			String sql;
			if(rect2d == null) {
				sql = String.format("SELECT %s FROM %s", geometrySQL, name);		
			} else {				
				int epsg = getEPSG();
				String sql_ST_Intersects = SQL_ST_Intersects(primaryGeometryColumn, rect2d, epsg);				
				if(crop) {
					String sql_ST_Intersection = SQL_ST_Intersection(geometrySQL, rect2d, epsg);
					sql = String.format("SELECT %s FROM %s WHERE %s", sql_ST_Intersection, name, sql_ST_Intersects);	
				} else {
					sql = String.format("SELECT %s FROM %s WHERE %s", geometrySQL, name, sql_ST_Intersects);
				}				
			}	
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			long featureCount = 0;
			WKBReader wkbReader = new WKBReader();
			while(rs.next()) {
				byte[] raw = rs.getBytes(1);
				if(raw != null) {
					if(raw.length % 2 != 0) {
						throw new RuntimeException("read error");
					}
					byte[] bytes = new byte[raw.length / 2];
					for (int i = 0; i < bytes.length; i++) {
						int j = i << 1;
						bytes[i] = (byte) ((hexToInt(raw[j]) << 4) + hexToInt(raw[j + 1]));
					}
					org.locationtech.jts.geom.Geometry geometry = wkbReader.read(bytes);
					//Logger.info(geometry.getClass());					
					consumer.acceptGeometry(geometry);					
				} else {
					//Logger.info("null");
				}
			}
			//Logger.info("features " + featureCount);
			//Logger.info(Timer.stop("query"));
			return featureCount;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;	
		}
	}

	public <T extends JTSGeometryConsumer> long forEachJTSGeometryByGroup(Rect2d rect2d, boolean crop, String groupField, Map<Object, T> groupMap, Supplier<T>  factory) {
		//Logger.info("getGeometry");

		try (Connection conn = postgisConnector.getConnection()) {
			//Timer.start("query");
			String sql;
			if(rect2d == null) {
				sql = String.format("SELECT %s, %s FROM %s", geometrySQL, groupField, name);		
			} else {
				int epsg = getEPSG();
				String sql_ST_Intersects = SQL_ST_Intersects(primaryGeometryColumn, rect2d, epsg);				
				if(crop) {
					String sql_ST_Intersection = SQL_ST_Intersection(geometrySQL, rect2d, epsg);
					sql = String.format("SELECT %s, %s FROM %s WHERE %s", sql_ST_Intersection, groupField, name, sql_ST_Intersects);	
				} else {
					sql = String.format("SELECT %s, %s FROM %s WHERE %s", geometrySQL, groupField, name, sql_ST_Intersects);
				}
			}	
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			long featureCount = 0;
			WKBReader wkbReader = new WKBReader();
			while(rs.next()) {
				byte[] raw = rs.getBytes(1);
				if(raw != null) {
					if(raw.length % 2 != 0) {
						throw new RuntimeException("read error");
					}
					byte[] bytes = new byte[raw.length / 2];
					for (int i = 0; i < bytes.length; i++) {
						int j = i << 1;
						bytes[i] = (byte) ((hexToInt(raw[j]) << 4) + hexToInt(raw[j + 1]));
					}
					org.locationtech.jts.geom.Geometry geometry = wkbReader.read(bytes);
					//Logger.info(geometry.getClass());

					Object group = rs.getObject(2);
					JTSGeometryConsumer consumer = groupMap.computeIfAbsent(group, key -> {
						return factory.get();
					});

					consumer.acceptGeometry(geometry);					
				} else {
					//Logger.info("null");
				}
			}
			//Logger.info("features " + featureCount);
			//Logger.info(Timer.stop("query"));
			return featureCount;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 
	 * @param rect2d  nullable
	 * @param consumer
	 * @return
	 */
	public long forEachGeoJSON(Rect2d rect2d, boolean crop, Consumer<String> consumer) {
		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");
			String sql;
			if(rect2d == null) {
				sql = String.format("SELECT ST_AsGeoJSON(%s) FROM %s",  geometrySQL, name);
			} else {
				int epsg = getEPSG();
				String sql_ST_Intersects = SQL_ST_Intersects(primaryGeometryColumn, rect2d, epsg);				
				if(crop) {
					String sql_ST_Intersection = SQL_ST_Intersection(geometrySQL, rect2d, epsg);
					sql = String.format("SELECT ST_AsGeoJSON(%s) FROM %s WHERE %s", sql_ST_Intersection, name, sql_ST_Intersects);	
				} else {
					sql = String.format("SELECT ST_AsGeoJSON(%s) FROM %s WHERE %s", geometrySQL, name, sql_ST_Intersects);
				}
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
	
	/**
	 * 
	 * @param rect2d  nullable
	 * @param consumer
	 * @return
	 */
	public long forEachGeoJSONWithFields(Rect2d rect2d, boolean crop, FeatureConsumer consumer) {
		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");
			String sql;
			if(rect2d == null) {
				sql = String.format("SELECT ST_AsGeoJSON(%s), %s FROM %s", geometrySQL, selectorFields, name);
			} else {				
				int epsg = getEPSG();
				String sql_ST_Intersects = SQL_ST_Intersects(primaryGeometryColumn, rect2d, epsg);				
				if(crop) {
					String sql_ST_Intersection = SQL_ST_Intersection(geometrySQL, rect2d, epsg);
					sql = String.format("SELECT ST_AsGeoJSON(%s), %s FROM %s WHERE %s", sql_ST_Intersection, selectorFields, name, sql_ST_Intersects);	
				} else {
					sql = String.format("SELECT ST_AsGeoJSON(%s), %s FROM %s WHERE %s", geometrySQL, selectorFields, name, sql_ST_Intersects);
				}
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
	
	public ResultSet queryGMLWithFields(Rect2d rect2d, boolean crop) {
		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");
			String sql;
			if(rect2d == null) {
				String sql_ST_AsGML = SQL_ST_AsGML(geometrySQL);
				sql = String.format("SELECT %s, %s FROM %s", sql_ST_AsGML, selectorFields, name);		
			} else {				
				int epsg = getEPSG();
				String sql_ST_Intersects = SQL_ST_Intersects(primaryGeometryColumn, rect2d, epsg);				
				if(crop) {
					String sql_ST_Intersection = SQL_ST_Intersection(geometrySQL, rect2d, epsg);
					String sql_ST_AsGML = SQL_ST_AsGML(sql_ST_Intersection);
					sql = String.format("SELECT %s, %s FROM %s WHERE %s", sql_ST_AsGML, selectorFields, name, sql_ST_Intersects);	
				} else {
					String sql_ST_AsGML = SQL_ST_AsGML(geometrySQL);
					sql = String.format("SELECT %s, %s FROM %s WHERE %s", sql_ST_AsGML, selectorFields, name, sql_ST_Intersects);
				}
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
}
