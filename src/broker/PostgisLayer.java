package broker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.json.JSONWriter;
import org.tinylog.Logger;

import pointcloud.Rect2d;
import util.Timer;
import util.Util;
import util.collections.array.ReadonlyArray;

public class PostgisLayer {

	private final Connection conn;
	public final String name;
	private final String[] colAllNames;
	private final String[] colAllTypes;	
	public final ReadonlyArray<String> columnAllNames;
	public final ReadonlyArray<String> columnAllTypes;
	public final String geoColumnName;
	private final String[] fldNames;
	private final String[] fldTypes;
	public final ReadonlyArray<String> fieldNames;
	public final ReadonlyArray<String> fieldTypes;
	private final String gmlQuerySelector;

	public PostgisLayer(Connection conn, String name) {		
		Util.checkStrictID(name);		
		this.name = name;
		this.conn = conn;
		
		try {
			String sql = String.format("SELECT * FROM %s WHERE false",  name);	
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			ResultSetMetaData meta = rs.getMetaData();
			int cnt = meta.getColumnCount();
			colAllNames = new String[cnt];
			colAllTypes = new String[cnt];
			for (int i = 0; i < cnt; i++) {
				String colName = meta.getColumnName(i + 1);
				String colType = meta.getColumnTypeName(i + 1);
				colAllNames[i] = colName;
				colAllTypes[i] = colType;
			}
			columnAllNames = new ReadonlyArray<String>(colAllNames);
			columnAllTypes = new ReadonlyArray<String>(colAllTypes);
			
			Logger.info(columnAllNames);
			Logger.info(columnAllTypes);
			
			int geometryIndex = columnAllTypes.indexOf("geometry");
			if(geometryIndex < 0) {
				throw new RuntimeException("missing geometry column in table");
			}
			if(geometryIndex != columnAllTypes.lastIndexOf("geometry")) {
				throw new RuntimeException("more than one geometry column in table");
			}
			geoColumnName = colAllNames[geometryIndex];
			fldNames = new String[cnt - 1];
			fldTypes = new String[cnt - 1];
			int pos = 0;
			for (int i = 0; i < cnt; i++) {
				if(i != geometryIndex) {
					fldNames[pos] = colAllNames[i];
					fldTypes[pos] = colAllTypes[i];
					pos++;
				}
			}
			fieldNames = new ReadonlyArray<String>(fldNames);
			fieldTypes = new ReadonlyArray<String>(fldTypes);
			
			String s = "ST_AsGML(3," + geoColumnName + ",8,2)";
			for (String fldName : fldNames) {
				s += "," + fldName;
			}
			gmlQuerySelector = s;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		
		
	}

	public long getFeatures() {
		try {
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
	
	public long getGeo(JSONWriter json) {
		try {
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
				json.value(area);
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
		try {
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
		try {
			Timer.start("query");
			String sql;
			if(rect2d == null) {
				sql = String.format("SELECT %s FROM %s", gmlQuerySelector, name);		
			} else {
				sql = String.format("SELECT %s FROM %s WHERE geom && ST_MakeEnvelope(%s, %s, %s, %s, %s)", gmlQuerySelector, name, rect2d.xmin, rect2d.ymin, rect2d.xmax, rect2d.ymax, getEPSG());	
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
		try {
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
}
