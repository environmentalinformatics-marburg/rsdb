package server;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Server;

import broker.Broker;


public class RunServer {
	

	public static void main(String[] args) {
		
		Broker broker = null;
		try {
			broker = new Broker();
			run(broker);
		} catch (Throwable e) {
			//e.printStackTrace();
			Logger.error("sever not started "+e);
		} finally {			
			try {
				if(broker!=null) {
					broker.close();
				}
			} catch(Exception e) {
				e.printStackTrace();
				Logger.error(e);
			} finally {
				Logger.info("server stopped");
				System.exit(0);
			}
		}
	}

	public static void run(Broker broker) {
		try {
			Server server = RSDBServer.createServer(broker);
			server.start();
			RSDBServer.printServerEntrypoint(server, broker);
			System.out.println("RSDB server running...   (press ctrl-c to stop)");
			addShutdownHook(server, broker);
			server.join();
		} catch(Exception e) {
			//e.printStackTrace();
			Logger.error(e);
		}
	}

	public static void addShutdownHook(Server server, Broker broker) {
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				System.out.println();
				System.out.println("stop RSDB server...");
				try {
					server.stop();
				} catch(Exception e) {
					e.printStackTrace();
					Logger.error(e);
				}
				try {
					broker.close();
				} catch(Exception e) {
					e.printStackTrace();
					Logger.error(e);
				}
			}
		});
	}
}
