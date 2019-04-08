package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;

import broker.Broker;


public class RunServer {
	private static final Logger log = LogManager.getLogger();

	public static void main(String[] args) {
		
		Broker broker = null;
		try {
			broker = new Broker();
			run(broker);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("sever not started "+e);
		} finally {			
			try {
				if(broker!=null) {
					broker.close();
				}
			} catch(Exception e) {
				e.printStackTrace();
				log.error(e);
			} finally {
				log.info("server stopped");
				System.exit(0);
			}
		}
	}

	public static void run(Broker broker) {
		try {
			Server server = PointDBServer.createServer(broker);
			server.start();
			PointDBServer.printServerEntrypoint(server, broker);
			System.out.println("server ready...");
			addShutdownHook(server, broker);
			server.join();
		} catch(Exception e) {
			e.printStackTrace();
			log.error(e);
		}
	}

	public static void addShutdownHook(Server server, Broker broker) {
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				System.out.println();
				System.out.println("stop server...");
				try {
					server.stop();
				} catch(Exception e) {
					e.printStackTrace();
					log.error(e);
				}
				try {
					broker.close();
				} catch(Exception e) {
					e.printStackTrace();
					log.error(e);
				}
			}
		});
	}
}
