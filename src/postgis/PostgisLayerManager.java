package postgis;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import org.eclipse.jetty.server.UserIdentity;
import org.tinylog.Logger;

import broker.Broker;

public class PostgisLayerManager {
	
	public static final Path postgis_root = Paths.get("postgis");

	private final Broker broker;
	
	private final PostgisConnector postgisConnector;

	private ConcurrentSkipListMap<String, PostgisLayerConfig> postgisLayerConfigMap = new ConcurrentSkipListMap<String, PostgisLayerConfig>();
	private ConcurrentSkipListMap<String, PostgisLayer> postgisLayerMap = new ConcurrentSkipListMap<String, PostgisLayer>();

	public PostgisLayerManager(Broker broker) {
		this.broker = broker;
		this.postgisConnector = new PostgisConnector(broker.brokerConfig.postgis());
		refresh();
	}	
	
	public PostgisLayer getPostgisLayer(String name) {
		PostgisLayer postgisLayer = postgisLayerMap.get(name);
		if(postgisLayer != null) {
			return postgisLayer;
		}
		return openPostgisLayer(name);
	}
	
	private synchronized PostgisLayer openPostgisLayer(String name) { // guarantee to load each PostgisLayer once only
		PostgisLayer postgisLayer = postgisLayerMap.get(name);
		if(postgisLayer != null) {
			return postgisLayer;
		}
		PostgisLayerConfig config = postgisLayerConfigMap.get(name);
		if(config == null) {			
			refresh();
			config = postgisLayerConfigMap.get(name);
			if(config == null) {
				throw new RuntimeException("Postgis layer not found: " + name);
			}
		}
		postgisLayer = new PostgisLayer(config, postgisConnector);
		PostgisLayer ret = postgisLayerMap.put(name, postgisLayer);
		if(ret != null) {
			throw new RuntimeException("double load: "+name);
		}
		return postgisLayer;
	}

	/*public PostgisLayer getPostgisLayer(String name) {
		try {
			return new PostgisLayer(conn, name);
		} catch(Exception e) {
			Logger.info(e.getMessage());
			connect();
			return new PostgisLayer(conn, name);
		}
	}*/
	
	private static Path[] getYamlFiles(Path root) throws IOException {
		DirectoryStream<Path> dirStream = Files.newDirectoryStream(root, "*.yaml");
		Path[] paths = StreamSupport.stream(dirStream.spliterator(), false)
				.sorted()
				.toArray(Path[]::new);
		dirStream.close();
		return paths;
	}
	
	public synchronized void refresh() {
		if(!postgis_root.toFile().exists()) {
			Logger.trace("no postgis layers: postgis folder missing");
			return;
		}
		try {
			ConcurrentSkipListMap<String, PostgisLayerConfig> map = new ConcurrentSkipListMap<String, PostgisLayerConfig>();
			Path[] paths = getYamlFiles(postgis_root);
			for(Path path:paths) {
				Logger.info("found postgis layer " + path);
				PostgisLayerConfig config = PostgisLayerConfig.ofPath(path);
				map.put(config.name, config);
			}
			postgisLayerConfigMap = map;
		} catch (Exception e) {
			Logger.error(e);
		}
	}
	
	public NavigableSet<String> getNames() {
		return postgisLayerConfigMap.keySet();
	}
	
	public void forEach(UserIdentity userIdentity, Consumer<PostgisLayerBase> consumer) {
		for(PostgisLayerConfig postgisLayerConfig : postgisLayerConfigMap.values()) {
			
			PostgisLayerBase postgisLayerBase = postgisLayerMap.get(postgisLayerConfig.name);
			if(postgisLayerBase == null) {
				postgisLayerBase = postgisLayerConfig;
			}			
			
			if(postgisLayerConfig.isAllowed(userIdentity)) {
				consumer.accept(postgisLayerBase);
			}
		}
	}
}
