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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.eclipse.jetty.server.UserIdentity;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.tinylog.Logger;
import org.yaml.snakeyaml.Yaml;

import broker.Informal;
import broker.StructuredAccess;
import broker.acl.ACL;
import broker.acl.AclUtil;
import broker.acl.EmptyACL;
import net.postgis.jdbc.PGbox2d;
import net.postgis.jdbc.PGgeometry;
import pointcloud.Rect2d;
import postgis.style.StyleProvider;
import postgis.style.StyleProviderFactory;
import util.Interruptor;
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

	private String nameField = ""; // not null
	private ReadonlyArray<String> class_fields;

	private ACL acl = EmptyACL.ADMIN;
	private ACL acl_mod = EmptyACL.ADMIN;
	private ACL acl_owner = EmptyACL.ADMIN;
	private Informal informal = Informal.EMPTY;

	private final Path metaPathTemp;
	private final File metaFileTemp;

	private StyleProvider styleProvider = StyleProviderFactory.DEFAULT_STYLE_PROVIDER;

	private StructuredAccess structuredAccess = StructuredAccess.DEFAULT;

	public static class PostgisColumn {
		
		public static final Comparator<PostgisColumn> CASE_INSENSITIVE_ORDER = new Comparator<PostgisLayer.PostgisColumn>() {			
			@Override
			public int compare(PostgisColumn o1, PostgisColumn o2) {
				return String.CASE_INSENSITIVE_ORDER.compare(o1.name, o2.name);
			}
		};

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

	public String getWKT_SRS() {
		int epsg = getEPSG();
		try (Connection conn = postgisConnector.getConnection()) {
			String sql1 = String.format("SELECT srtext FROM spatial_ref_sys WHERE srid = %s", epsg);	
			PreparedStatement stmt1 = conn.prepareStatement(sql1);
			ResultSet rs1 = stmt1.executeQuery();
			if(rs1.next()) {
				String text = rs1.getString(1);
				Logger.info(text);
				return text;
			} else {
				throw new RuntimeException("no text SRID " + epsg);
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
				//Logger.info(s);
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
		nameField = yamlMap.optString("name_field", "");
		class_fields = yamlMap.optList("class_fields").asReadonlyStrings();		
		if(class_fields.isEmpty()) {
			class_fields = yamlMap.optList("class_attributes").asReadonlyStrings(); // legacy
		}
		YamlMap styleMap = yamlMap.optMap("style");
		styleProvider = StyleProviderFactory.ofYaml(styleMap);		
		structuredAccess = StructuredAccess.ofYaml(yamlMap.optMap("structured_access"));		
	}

	private synchronized void metaToYaml(LinkedHashMap<String, Object> map) {
		map.put("type", TYPE);
		map.put("acl", acl.toYaml());
		map.put("acl_mod", acl_mod.toYaml());
		map.put("acl_owner", acl_owner.toYaml());
		informal.writeYaml(map);
		map.put("name_field", nameField);
		map.put("class_fields", class_fields);
		if(styleProvider != StyleProviderFactory.DEFAULT_STYLE_PROVIDER) {
			map.put("style", styleProvider.toYaml());
		}		
		map.put("structured_access", structuredAccess.toYaml());
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
						net.postgis.jdbc.geometry.Point p1 = box.getLLB();
						net.postgis.jdbc.geometry.Point p2 = box.getURT();
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
			e = calcUsedGeometryTypes();
			geometryTypes = e;
		}
		return e;
	}

	private synchronized ReadonlyArray<String> calcUsedGeometryTypes() {
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

	private static String SQL_Polygon(double[][] points, int srid) {		
		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;
		for(double[] p : points) {
			if(isFirst) {
				isFirst = false;
			} else {
				sb.append(',');
				sb.append(' ');
			}
			sb.append(p[0]);
			sb.append(' ');
			sb.append(p[1]);
		}
		return String.format("ST_SetSRID(ST_GeomFromText('POLYGON((%s))'), %s)", sb, srid);
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

	public int forEachPGgeometry(Rect2d layerRect, boolean crop, PGgeometryConsumer consumer) {
		//Logger.info("getGeometry");

		try (Connection conn = postgisConnector.getConnection()) {
			//Timer.start("query");
			String sql;
			if(layerRect == null) {
				sql = String.format("SELECT %s FROM %s", geometrySQL, name);		
			} else {
				int epsg = getEPSG();
				String sql_ST_Intersects = SQL_ST_Intersects(primaryGeometryColumn, layerRect, epsg);				
				if(crop) {
					String sql_ST_Intersection = SQL_ST_Intersection(geometrySQL, layerRect, epsg);
					sql = String.format("SELECT %s FROM %s WHERE %s", sql_ST_Intersection, name, sql_ST_Intersects);	
				} else {
					sql = String.format("SELECT %s FROM %s WHERE %s", geometrySQL, name, sql_ST_Intersects);
				}				}	
			//Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			int featureCount = 0;
			while(rs.next()) {
				Object obj = rs.getObject(1);
				if(obj != null) {
					if(obj instanceof PGgeometry) {
						PGgeometry pgGeometry = (PGgeometry) obj;
						net.postgis.jdbc.geometry.Geometry geometry = pgGeometry.getGeometry();
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

	public int forEachJtsGeometry(Rect2d layerRect, int dstEPSG, boolean crop, Interruptor interruptor, Consumer<Geometry> consumer) {
		//Logger.info("getGeometry");

		boolean reproject = dstEPSG > 0;

		if(Interruptor.isInterrupted(interruptor)) {
			return -1;
		}

		try (Connection conn = postgisConnector.getConnection()) {
			//Timer.start("query");
			String sql;
			if(layerRect == null) {
				if(reproject) {
					sql = String.format("SELECT ST_Transform(%s, %s) FROM %s", geometrySQL, dstEPSG, name);
				} else {
					sql = String.format("SELECT %s FROM %s", geometrySQL, name);
				}
			} else {				
				int layerEPSG = getEPSG();
				String sql_ST_Intersects = SQL_ST_Intersects(primaryGeometryColumn, layerRect, layerEPSG);				
				if(crop) {
					String sql_ST_Intersection = SQL_ST_Intersection(geometrySQL, layerRect, layerEPSG);
					if(reproject) {
						sql = String.format("SELECT ST_Transform(%s, %s) FROM %s WHERE %s", sql_ST_Intersection, dstEPSG, name, sql_ST_Intersects);
					} else {
						sql = String.format("SELECT %s FROM %s WHERE %s", sql_ST_Intersection, name, sql_ST_Intersects);
					}
				} else {
					if(reproject) {
						sql = String.format("SELECT ST_Transform(%s, %s) FROM %s WHERE %s", geometrySQL, dstEPSG, name, sql_ST_Intersects);
					} else {
						sql = String.format("SELECT %s FROM %s WHERE %s", geometrySQL, name, sql_ST_Intersects);
					}
				}				
			}	
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			int featureCount = 0;
			WKBReader wkbReader = new WKBReader();
			if(Interruptor.isInterrupted(interruptor)) {
				return -1;
			}
			while(rs.next()) {
				byte[] raw = rs.getBytes(1);
				if(raw != null) {
					Geometry geometry = bytesToJtsGeometry(raw, wkbReader);
					consumer.accept(geometry);
					featureCount++;
				} else {
					//Logger.info("null");
				}
				if(Interruptor.isInterrupted(interruptor)) {
					return -1;
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

	@FunctionalInterface
	public static interface IntJtsGeometryConsumer {
		void accept(int value, Geometry geometry);
	}

	@FunctionalInterface
	public static interface IntObjectJtsGeometryConsumer {
		void accept(int value, Object object, Geometry geometry);
	}

	private static Geometry bytesToJtsGeometry(byte[] raw, WKBReader wkbReader) throws ParseException {
		if(raw.length % 2 != 0) {
			throw new RuntimeException("read error");
		}
		byte[] bytes = new byte[raw.length / 2];
		for (int i = 0; i < bytes.length; i++) {
			int j = i << 1;
			bytes[i] = (byte) ((hexToInt(raw[j]) << 4) + hexToInt(raw[j + 1]));
		}
		Geometry geometry = wkbReader.read(bytes);
		return geometry;
	}

	public <T extends JtsGeometryConsumer> int forEachIntJtsGeometry(Rect2d layerRect, int dstEPSG, boolean crop, String valueField, Interruptor interruptor, IntJtsGeometryConsumer consumer) {
		//Logger.info("getGeometry");

		boolean reproject = dstEPSG > 0;

		if(Interruptor.isInterrupted(interruptor)) {
			Logger.info("interrupted " + interruptor.id + "     out");
			return -1;
		}

		try (Connection conn = postgisConnector.getConnection()) {
			//Timer.start("query");
			String sql;
			if(layerRect == null) {
				if(reproject) {
					sql = String.format("SELECT ST_Transform(%s, %s), %s FROM %s", geometrySQL, dstEPSG, valueField, name);
				} else {
					sql = String.format("SELECT %s, %s FROM %s", geometrySQL, valueField, name);
				}
			} else {
				int layerEPSG = getEPSG();
				String sql_ST_Intersects = SQL_ST_Intersects(primaryGeometryColumn, layerRect, layerEPSG);				
				if(crop) {
					String sql_ST_Intersection = SQL_ST_Intersection(geometrySQL, layerRect, layerEPSG);
					if(reproject) {
						sql = String.format("SELECT ST_Transform(%s, %s), %s FROM %s WHERE %s", sql_ST_Intersection, dstEPSG, valueField, name, sql_ST_Intersects);
					} else {
						sql = String.format("SELECT %s, %s FROM %s WHERE %s", sql_ST_Intersection, valueField, name, sql_ST_Intersects);
					}
				} else {
					if(reproject) {
						sql = String.format("SELECT ST_Transform(%s, %s), %s FROM %s WHERE %s", geometrySQL, dstEPSG, valueField, name, sql_ST_Intersects);
					} else {
						sql = String.format("SELECT %s, %s FROM %s WHERE %s", geometrySQL, valueField, name, sql_ST_Intersects);
					}
				}
			}	
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			int featureCount = 0;
			WKBReader wkbReader = new WKBReader();
			if(Interruptor.isInterrupted(interruptor)) {
				Logger.info("interrupted " + interruptor.id);
				return -1;
			}
			while(rs.next()) {
				byte[] raw = rs.getBytes(1);
				if(raw != null) {
					Geometry geometry = bytesToJtsGeometry(raw, wkbReader);
					int value = rs.getInt(2);
					consumer.accept(value, geometry);
					featureCount++;
				} else {
					//Logger.info("null");
				}
				if(Interruptor.isInterrupted(interruptor)) {
					Logger.info("interrupted " + interruptor.id + "     inner");
					return -1;
				}
			}
			//Logger.info("features " + featureCount);
			//Logger.info(Timer.stop("query"));
			return featureCount;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public <T extends JtsGeometryConsumer> int forEachIntObjectJtsGeometry(Rect2d layerRect, int dstEPSG, boolean crop, String valueField, String objectField, Interruptor interruptor, IntObjectJtsGeometryConsumer consumer) {
		//Logger.info("getGeometry");

		boolean reproject = dstEPSG > 0;

		if(Interruptor.isInterrupted(interruptor)) {
			Logger.info("interrupted " + interruptor.id + "     out");
			return -1;
		}

		try (Connection conn = postgisConnector.getConnection()) {
			//Timer.start("query");
			String sql;
			if(layerRect == null) {
				if(reproject) {
					sql = String.format("SELECT ST_Transform(%s, %s), %s %s FROM %s", geometrySQL, dstEPSG, valueField, objectField, name);
				} else {
					sql = String.format("SELECT %s, %s, %s FROM %s", geometrySQL, valueField, objectField, name);
				}
			} else {
				int layerEPSG = getEPSG();
				String sql_ST_Intersects = SQL_ST_Intersects(primaryGeometryColumn, layerRect, layerEPSG);				
				if(crop) {
					String sql_ST_Intersection = SQL_ST_Intersection(geometrySQL, layerRect, layerEPSG);
					if(reproject) {
						sql = String.format("SELECT ST_Transform(%s, %s), %s, %s FROM %s WHERE %s", sql_ST_Intersection, dstEPSG, valueField, objectField, name, sql_ST_Intersects);
					} else {
						sql = String.format("SELECT %s, %s, %s FROM %s WHERE %s", sql_ST_Intersection, valueField, objectField, name, sql_ST_Intersects);
					}
				} else {
					if(reproject) {
						sql = String.format("SELECT ST_Transform(%s, %s), %s, %s FROM %s WHERE %s", geometrySQL, dstEPSG, valueField, objectField, name, sql_ST_Intersects);
					} else {
						sql = String.format("SELECT %s, %s, %s FROM %s WHERE %s", geometrySQL, valueField, objectField, name, sql_ST_Intersects);
					}
				}
			}	
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			int featureCount = 0;
			WKBReader wkbReader = new WKBReader();
			if(Interruptor.isInterrupted(interruptor)) {
				Logger.info("interrupted " + interruptor.id);
				return -1;
			}
			while(rs.next()) {
				byte[] raw = rs.getBytes(1);
				if(raw != null) {
					Geometry geometry = bytesToJtsGeometry(raw, wkbReader);
					int value = rs.getInt(2);
					Object object = rs.getObject(3);
					consumer.accept(value, object, geometry);
					featureCount++;
				} else {
					//Logger.info("null");
				}
				if(Interruptor.isInterrupted(interruptor)) {
					Logger.info("interrupted " + interruptor.id + "     inner");
					return -1;
				}
			}
			//Logger.info("features " + featureCount);
			//Logger.info(Timer.stop("query"));
			return featureCount;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@FunctionalInterface
	public static interface ObjectJtsGeometryConsumer {
		void accept(Object value, Geometry geometry);
	}

	public <T extends JtsGeometryConsumer> int forEachObjectJtsGeometry(Rect2d layerRect, int dstEPSG, boolean crop, String valueField, Interruptor interruptor, ObjectJtsGeometryConsumer consumer) {
		//Logger.info("getGeometry");

		boolean reproject = dstEPSG > 0;

		if(Interruptor.isInterrupted(interruptor)) {
			Logger.info("interrupted " + interruptor.id + "     out");
			return -1;
		}

		try (Connection conn = postgisConnector.getConnection()) {
			//Timer.start("query");
			String sql;
			if(layerRect == null) {
				if(reproject) {
					sql = String.format("SELECT ST_Transform(%s, %s), %s FROM %s", geometrySQL, dstEPSG, valueField, name);
				} else {
					sql = String.format("SELECT %s, %s FROM %s", geometrySQL, valueField, name);
				}
			} else {
				int layerEPSG = getEPSG();
				String sql_ST_Intersects = SQL_ST_Intersects(primaryGeometryColumn, layerRect, layerEPSG);				
				if(crop) {
					String sql_ST_Intersection = SQL_ST_Intersection(geometrySQL, layerRect, layerEPSG);
					if(reproject) {
						sql = String.format("SELECT ST_Transform(%s, %s), %s FROM %s WHERE %s", sql_ST_Intersection, dstEPSG, valueField, name, sql_ST_Intersects);
					} else {
						sql = String.format("SELECT %s, %s FROM %s WHERE %s", sql_ST_Intersection, valueField, name, sql_ST_Intersects);	
					}
				} else {
					if(reproject) {
						sql = String.format("SELECT ST_Transform(%s, %s), %s FROM %s WHERE %s", geometrySQL, dstEPSG, valueField, name, sql_ST_Intersects);
					} else {
						sql = String.format("SELECT %s, %s FROM %s WHERE %s", geometrySQL, valueField, name, sql_ST_Intersects);
					}
				}
			}	
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			int featureCount = 0;
			WKBReader wkbReader = new WKBReader();

			if(Interruptor.isInterrupted(interruptor)) {
				Logger.info("interrupted " + interruptor.id + "     out");
				return -1;
			}

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
					Geometry geometry = wkbReader.read(bytes);
					//Logger.info(geometry.getClass());

					Object value = rs.getObject(2);
					consumer.accept(value, geometry);	
					featureCount++;
				} else {
					//Logger.info("null");
				}
				if(Interruptor.isInterrupted(interruptor)) {
					Logger.info("interrupted " + interruptor.id + "     out");
					return -1;
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
	 * @param srcRect
	 * @param srcEpsg
	 * @return nullable
	 */
	public Rect2d projectToLayer(Rect2d srcRect, int srcEpsg) {

		try (Connection conn = postgisConnector.getConnection()) {
			int layerEpsg = getEPSG();			
			//String sqlGeometry = SQL_ST_MakeEnvelope(srcRect, srcEpsg);

			double[][] polyPoints = srcRect.createPoly9();
			String sqlGeometry = SQL_Polygon(polyPoints, srcEpsg);
			String sql = String.format("SELECT ST_Transform(%s, %s)", sqlGeometry, layerEpsg);	
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				byte[] raw = rs.getBytes(1);
				if(raw != null) {
					WKBReader wkbReader = new WKBReader();
					Geometry geometry = bytesToJtsGeometry(raw, wkbReader);
					Logger.info(geometry);		
					Envelope envelope = geometry.getEnvelopeInternal();
					Logger.info(envelope);					
					Rect2d dstRect = new Rect2d(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
					Logger.info(dstRect);
					return dstRect;
				}
			}				
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	/**
	 * 
	 * @param layerRect  nullable
	 * @param consumer
	 * @return
	 */
	public int forEachGeoJSON(Rect2d layerRect, boolean crop, Consumer<String> consumer) {
		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");
			String sql;
			if(layerRect == null) {
				sql = String.format("SELECT ST_AsGeoJSON(%s) FROM %s",  geometrySQL, name);
			} else {
				int epsg = getEPSG();
				String sql_ST_Intersects = SQL_ST_Intersects(primaryGeometryColumn, layerRect, epsg);				
				if(crop) {
					String sql_ST_Intersection = SQL_ST_Intersection(geometrySQL, layerRect, epsg);
					sql = String.format("SELECT ST_AsGeoJSON(%s) FROM %s WHERE %s", sql_ST_Intersection, name, sql_ST_Intersects);	
				} else {
					sql = String.format("SELECT ST_AsGeoJSON(%s) FROM %s WHERE %s", geometrySQL, name, sql_ST_Intersects);
				}
			}
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			int featureCount = 0;
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
	 * @param layerRect  nullable
	 * @param consumer
	 * @return
	 */
	public int forEachGeoJSONWithFields(Rect2d layerRect, boolean crop, FeatureConsumer consumer) {
		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");
			String sql;
			if(layerRect == null) {
				sql = String.format("SELECT ST_AsGeoJSON(%s), %s FROM %s", geometrySQL, selectorFields, name);
			} else {				
				int epsg = getEPSG();
				String sql_ST_Intersects = SQL_ST_Intersects(primaryGeometryColumn, layerRect, epsg);				
				if(crop) {
					String sql_ST_Intersection = SQL_ST_Intersection(geometrySQL, layerRect, epsg);
					sql = String.format("SELECT ST_AsGeoJSON(%s), %s FROM %s WHERE %s", sql_ST_Intersection, selectorFields, name, sql_ST_Intersects);	
				} else {
					sql = String.format("SELECT ST_AsGeoJSON(%s), %s FROM %s WHERE %s", geometrySQL, selectorFields, name, sql_ST_Intersects);
				}
			}
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			consumer.acceptFields(fields);
			consumer.acceptStart();
			int featureCount = 0;
			while(rs.next()) {
				consumer.acceptFeatureStart(featureCount);
				String geometry = rs.getString(1);
				consumer.acceptGeometry(geometry);
				consumer.acceptCellsStart(featureCount);
				for (int i = 0; i < flds.length; i++) {
					PostgisColumn field = flds[i];

					switch(field.type) {
					case "int2": {
						short fieldValue = rs.getShort(i + 2);
						if(rs.wasNull()) {
							consumer.acceptCellNull(i);
						} else {
							consumer.acceptCellInt16(i, fieldValue);
						}
						break;
					}
					case "serial":
					case "int4": {
						int fieldValue = rs.getInt(i + 2);
						if(rs.wasNull()) {
							consumer.acceptCellNull(i);
						} else {
							consumer.acceptCellInt32(i, fieldValue);
						}
						break;
					}	
					case "float8": {
						double fieldValue = rs.getDouble(i + 2);
						if(rs.wasNull()) {
							consumer.acceptCellNull(i);
						} else {
							consumer.acceptCellFloat64(i, fieldValue);
						}
						break;
					}	
					case "text":
					case "timestamp": // included as text for simplicity (data type: java.sql.Timestamp)
					case "varchar":
					default: {
						//Logger.info("field type: " + field.type);
						String fieldValue = rs.getString(i + 2);
						if(rs.wasNull()) {
							consumer.acceptCellNull(i);
						} else {
							consumer.acceptCell(i, fieldValue);
						}
					}
					}
				}
				consumer.acceptCellsEnd(featureCount);
				consumer.acceptFeatureEnd(featureCount);
				featureCount++;
			}
			consumer.acceptEnd();
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
	 * @param layerRect  nullable
	 * @param consumer
	 * @return
	 */
	public int forEachWithFields(Rect2d layerRect, FieldsConsumer consumer) {
		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");
			String sql;
			if(layerRect == null) {
				sql = String.format("SELECT %s FROM %s", selectorFields, name);
			} else {				
				int epsg = getEPSG();
				String sql_ST_Intersects = SQL_ST_Intersects(primaryGeometryColumn, layerRect, epsg);
				sql = String.format("SELECT %s FROM %s WHERE %s", selectorFields, name, sql_ST_Intersects);				
			}
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			consumer.acceptFields(fields);
			int featureCount = 0;
			while(rs.next()) {
				consumer.acceptCellsStart(featureCount);
				for (int i = 0; i < flds.length; i++) {
					PostgisColumn field = flds[i];
					switch(field.type) {
					case "int2": {
						short fieldValue = rs.getShort(i + 1);
						if(rs.wasNull()) {
							consumer.acceptCellNull(i);
						} else {
							consumer.acceptCellInt16(i, fieldValue);
						}
						break;
					}
					case "serial":
					case "int4": {
						int fieldValue = rs.getInt(i + 1);
						if(rs.wasNull()) {
							consumer.acceptCellNull(i);
						} else {
							consumer.acceptCellInt32(i, fieldValue);
						}
						break;
					}	
					case "float8": {
						double fieldValue = rs.getDouble(i + 1);
						if(rs.wasNull()) {
							consumer.acceptCellNull(i);
						} else {
							consumer.acceptCellFloat64(i, fieldValue);
						}
						break;
					}	
					case "text":
					case "timestamp": // included as text for simplicity (data type: java.sql.Timestamp)
					case "varchar":
					default: {
						/*if(!(field.type.equals("varchar") || field.type.equals("timestamp") || field.type.equals("text"))) {
							Logger.info("field type: " + field.type);
						}*/
						String fieldValue = rs.getString(i + 1);
						if(rs.wasNull()) {
							consumer.acceptCellNull(i);
						} else {
							consumer.acceptCell(i, fieldValue);
						}
					}
					}
				}
				consumer.acceptCellsEnd(featureCount);
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

	public ResultSet queryGMLWithFields(Rect2d rect2d, boolean crop, int maxFeatures) {
		try (Connection conn = postgisConnector.getConnection()) {
			Timer.start("query");
			String sql;
			if(rect2d == null) {
				String sql_ST_AsGML = SQL_ST_AsGML(geometrySQL);
				sql = String.format("SELECT %s, %s FROM %s LIMIT %d", sql_ST_AsGML, selectorFields, name, maxFeatures);		
			} else {				
				int epsg = getEPSG();
				String sql_ST_Intersects = SQL_ST_Intersects(primaryGeometryColumn, rect2d, epsg);				
				if(crop) {
					String sql_ST_Intersection = SQL_ST_Intersection(geometrySQL, rect2d, epsg);
					String sql_ST_AsGML = SQL_ST_AsGML(sql_ST_Intersection);
					sql = String.format("SELECT %s, %s FROM %s WHERE %s LIMIT %d", sql_ST_AsGML, selectorFields, name, sql_ST_Intersects, maxFeatures);	
				} else {
					String sql_ST_AsGML = SQL_ST_AsGML(geometrySQL);
					sql = String.format("SELECT %s, %s FROM %s WHERE %s LIMIT %d", sql_ST_AsGML, selectorFields, name, sql_ST_Intersects, maxFeatures);
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

	/**
	 * 
	 * @param fieldName nullable
	 * @return
	 */
	public boolean hasFieldName(String fieldName) {
		if(fieldName == null || fieldName.isBlank()) {
			return false;
		}
		for(PostgisColumn fld : flds) {
			if(fld.name.equals(fieldName)) {
				return true;
			}
		}
		return false;
	}

	public StyleProvider getStyleProvider() {
		return styleProvider;
	}

	public int forEachIntUniqueSorted(String valueField, IntConsumer consumer) {
		try (Connection conn = postgisConnector.getConnection()) {
			String sql = String.format("SELECT DISTINCT %s FROM %s ORDER BY %s", valueField, name, valueField);		
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			int count = 0;
			while(rs.next()) {
				int value = rs.getInt(1);
				consumer.accept(value);
				count++;
			}
			return count;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public int forEachObjectUniqueSorted(String valueField, Consumer<Object> consumer) {
		try (Connection conn = postgisConnector.getConnection()) {
			String sql = String.format("SELECT DISTINCT %s FROM %s ORDER BY %s", valueField, name, valueField);		
			Logger.info(sql);
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			int count = 0;
			while(rs.next()) {
				Object value = rs.getObject(1);
				consumer.accept(value);
				count++;
			}
			return count;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public StructuredAccess getStructuredAccess() {
		return structuredAccess;
	}

	public void setStructuredAccess(StructuredAccess structuredAccess) {
		this.structuredAccess = structuredAccess;
		writeMeta();
	}

	public String getNameField() {
		return nameField;
	}

	public boolean hasNameField() {
		return nameField != null && !nameField.isBlank();
	}
}
