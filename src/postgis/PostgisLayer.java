package postgis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;
import org.tinylog.Logger;

import pointcloud.Rect2d;
import util.Timer;
import util.Util;
import util.collections.array.ReadonlyArray;
import util.collections.vec.Vec;

public class PostgisLayer extends PostgisLayerBase {

	private final PostgisLayerConfig postgisLayerConfig;
	private final PostgisConnector postgisConnector;
	
	private final PostgisColumn[] colsAll;
	public final ReadonlyArray<PostgisColumn> columnsAll;
	private final PostgisColumn[] flds;
	public final ReadonlyArray<PostgisColumn> fields;
	public final String primaryGeometryColumn;	
	private final String gmlQuerySelectorWithFields;
	private final String geoJSONQuerySelector;
	private final String geoJSONQuerySelectorWithFields;



	public static class PostgisColumn {

		public String name;
		public String type;

		public PostgisColumn(String name, String type) {
			this.name = name;
			this.type = type;
		}
	}

	public PostgisLayer(PostgisLayerConfig postgisLayerConfig, PostgisConnector postgisConnector) {	
		super(postgisLayerConfig.name, postgisLayerConfig.path);
		this.postgisLayerConfig = postgisLayerConfig;
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

	@Override
	public boolean isAllowed(UserIdentity userIdentity) {
		return postgisLayerConfig.isAllowed(userIdentity);
	}
}
