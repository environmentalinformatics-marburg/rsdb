package broker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.tinylog.Logger;

import util.Timer;
import util.Util;

public class PostgisLayer {

	private final Connection conn;
	private final String name;

	public PostgisLayer(Connection conn, String name) {		
		Util.checkStrictID(name);		
		this.name = name;
		this.conn = conn;
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
}
