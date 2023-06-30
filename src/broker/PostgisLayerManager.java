package broker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.tinylog.Logger;

public class PostgisLayerManager {

	private final Broker broker;

	private Connection conn = null;

	public PostgisLayerManager(Broker broker) {
		this.broker = broker;
		if(broker.brokerConfig.postgis().hasUrl()) {			
			connect();		
		}
	}

	private void connect() {
		try {
			PostgisConfig config = broker.brokerConfig.postgis();
			conn = DriverManager.getConnection(config.url, config.user, config.password);
		} catch (SQLException e) {
			//throw new RuntimeException(e);
			Logger.warn(e.getMessage());
		}
	}

	public PostgisLayer getPostgisLayer(String name) {
		return new PostgisLayer(conn, name);
	}
}
