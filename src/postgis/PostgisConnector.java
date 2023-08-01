package postgis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PostgisConnector {
	
	private final PostgisConfig config;

	public PostgisConnector(PostgisConfig config) {
		this.config = config;
	}
	
	public Connection getConnection() {
		try {
			Connection conn = DriverManager.getConnection(config.url, config.user, config.password);
			return conn;
		} catch (SQLException e) {
			throw new RuntimeException(e);
			//Logger.warn(e.getMessage());
		}
	}
}
