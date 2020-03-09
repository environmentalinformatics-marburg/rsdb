package run;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;

import broker.Broker;
import pointdb.PointDB;
import server.RSDBServer;
import util.Timer;

public class RunImport_RunServer {
	private static final Logger log = LogManager.getLogger();
	
	public static void main(String[] args) throws IOException {
		PointDB pointdb = null;
		try {
			//pointdb = new PointDB();
			Broker broker = new Broker();
			Server server = RSDBServer.createServer(broker);
			server.start();
			RSDBServer.printServerEntrypoint(server, broker);
			log.info("server ready...");
			//server.join();
			Timer.start("import");
			RunImport runImport = new RunImport(pointdb);
			runImport.loadAll();
			log.info(Timer.stop("import"));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("sever not started "+e);
		} finally {
			if(pointdb!=null) {
				pointdb.close();
			}
		}
		log.info("server stopped");
		System.exit(0);
	}

}
