package run;

import java.io.IOException;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Server;

import broker.Broker;
import pointdb.PointDB;
import server.RSDBServer;
import util.Timer;

public class RunImport_RunServer {
	
	
	public static void main(String[] args) throws IOException {
		PointDB pointdb = null;
		try {
			//pointdb = new PointDB();
			Broker broker = new Broker();
			Server server = RSDBServer.createServer(broker);
			server.start();
			RSDBServer.printServerEntrypoint(server, broker);
			Logger.info("server ready...");
			//server.join();
			Timer.start("import");
			RunImport runImport = new RunImport(pointdb);
			runImport.loadAll();
			Logger.info(Timer.stop("import"));
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("sever not started "+e);
		} finally {
			if(pointdb!=null) {
				pointdb.close();
			}
		}
		Logger.info("server stopped");
		System.exit(0);
	}

}
