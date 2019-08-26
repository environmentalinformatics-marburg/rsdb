package server.api.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import broker.Broker;
import server.api.APIHandler;
import util.Util;

public class APIHandler_proxy extends APIHandler {
	private static final Logger log = LogManager.getLogger();

	private static final Path PROXY_ROOT = Paths.get("proxy/osm");
	
	private static final String[] hosts = {"http://a.tile.openstreetmap.org/","http://b.tile.openstreetmap.org/","http://c.tile.openstreetmap.org/"};

	public APIHandler_proxy(Broker broker) {
		super(broker, "proxy");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {		
		log.info(target);
		if(target.startsWith("/") || target.contains("..") || target.contains("./") || target.contains("\\") || target.contains("~")) {
			throw new RuntimeException("invalid proxy path");
		}
		

		Path path = PROXY_ROOT.resolve(target);
		Util.checkIsParent(PROXY_ROOT, path);

		byte[] data = null;
		if(path.toFile().exists()) {
			data = Files.readAllBytes(path);
			log.info("from cache");
		} else {
			try {
				HttpClient httpClient = new HttpClient();
				httpClient.setFollowRedirects(false);
				httpClient.start();
				String host = hosts[ThreadLocalRandom.current().nextInt(hosts.length)];
				String url = host + target;
				log.info("GET " + url);
				ContentResponse r = httpClient.GET(url);
				if(r.getStatus() == 200) {
					data = r.getContent();
					log.info("write " + path);
					path.getParent().toFile().mkdirs();
					Files.write(path, data);
				} else {
					log.info("returned " + r.getStatus());
				}
				httpClient.stop();
			} catch (Exception e) {
				log.error(e);
			}
		}
		if(data != null) {
			response.getOutputStream().write(data);
		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}
}
