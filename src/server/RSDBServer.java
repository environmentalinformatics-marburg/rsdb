package server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.CompressedContentFormat;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.OptionalSslConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.ShutdownHandler;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.gdal.gdal.gdal;

import broker.Broker;
import server.api.main.MainAPICollectionHandler;
import server.api.poi_groups.APIHandler_poi_groups;
import server.api.pointclouds.APIHandler_pointclouds;
import server.api.pointdb.APICollectionHandler;
import server.api.pointdbs.APIHandler_pointdbs;
import server.api.rasterdb.RasterdbHandler;
import server.api.rasterdb.RasterdbsHandler;
import server.api.rasterdb.WmsHandler;
import server.api.roi_groups.APIHandler_roi_groups;
import server.api.vectordbs.VectordbsHandler;
import server.api.voxeldbs.APIHandler_voxeldbs;
import util.Table;
import util.Table.ColumnReaderString;
import util.Util;

public class RSDBServer {
	static final Logger log = LogManager.getLogger();
	public static final Marker MARKER_REQ = MarkerManager.getMarker("REQ");

	private static final long DATA_TRANSFER_TIMEOUT_MILLISECONDS = 2*60*60*1000; // set timeout to 2 hours
	private static final int BIND_BACKLOG_SIZE = 50;

	private static final String POINTDB_API_URL = "/pointdb";
	private static final String RASTERDB_API_URL = "/rasterdb";
	private static final String MAIN_API_URL = "/api";
	private static final String WEBCONTENT_URL = "/web";
	private static final String WEBFILES_URL = "/files";
	private static final String POINTCLOUDS_URL = "/pointclouds";
	private static final String VOXELDBS_URL = "/voxeldbs";
	private static final String VECTORDBS_URL = "/vectordbs";
	private static final String POI_GROUPS_URL = "/poi_groups";
	private static final String ROI_GROUPS_URL = "/roi_groups";

	private static final String WEBCONTENT_PATH = "webcontent"; //static files for web pages (no folder listing)
	private static final String WEBFILES_PATH = "webfiles"; //static files for user download (with folder listing)
	private static final String WEBFILES_CSS_FILE = WEBCONTENT_PATH+"/jetty-dir.css"; // stylesheet for webfiles directory listing

	private static final boolean useHTTP2 = false; // QGIS WMS seems to not support (optional) HTTP2

	private static final File KEYSTORE_FILE = new File("keystore");

	private static HttpConfiguration createBaseHttpConfiguration() {
		HttpConfiguration httpConfiguration = new HttpConfiguration();
		httpConfiguration.setSendServerVersion(false);
		httpConfiguration.setSendDateHeader(false);
		httpConfiguration.setSendXPoweredBy(false);
		httpConfiguration.addCustomizer(new ForwardedRequestCustomizer());
		/*httpConfiguration.addCustomizer(new Customizer() {
			@Override
			public void customize(Connector connector, HttpConfiguration channelConfig, Request request) {
				String prefix = request.getHeader("X-Forwarded-Prefix");
				if(prefix != null) {
					String path = request.getPathInfo();
					String original_path = prefix + path; 
					request.setPathInfo(original_path);
					log.info(path + " -> " + original_path);
				}
			}});*/
		return httpConfiguration;
	}

	private static HttpConfiguration createJwsHttpConfiguration() {
		HttpConfiguration httpConfiguration = createBaseHttpConfiguration();
		httpConfiguration.addCustomizer(new Customizer() {			
			@Override
			public void customize(Connector connector, HttpConfiguration channelConfig, Request request) {
				request.setAttribute("JWS", "JWS");				
			}
		});
		return httpConfiguration;
	}

	private static HttpConfiguration createBaseHttpsConfiguration(int https_port) {
		HttpConfiguration httpsConfiguration = createBaseHttpConfiguration();
		httpsConfiguration.setSecureScheme("https");
		httpsConfiguration.setSecurePort(https_port);
		SecureRequestCustomizer src = new SecureRequestCustomizer();
		src.setStsMaxAge(2000);
		src.setStsIncludeSubDomains(true);
		httpsConfiguration.addCustomizer(src);
		return httpsConfiguration;
	}

	private static HttpConfiguration createJwsHttpsConfiguration(int https_port) {
		HttpConfiguration httpsConfiguration = createBaseHttpsConfiguration(https_port);
		httpsConfiguration.addCustomizer(new Customizer() {			
			@Override
			public void customize(Connector connector, HttpConfiguration channelConfig, Request request) {
				request.setAttribute("JWS", "JWS");				
			}
		});
		return httpsConfiguration;
	}

	private static ServerConnector createHttpConnector(Server server, int http_port, HttpConfiguration httpConfiguration) {
		HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfiguration);		
		ServerConnector httpServerConnector = new ServerConnector(server, httpConnectionFactory);
		httpServerConnector.setPort(http_port);
		httpServerConnector.setIdleTimeout(DATA_TRANSFER_TIMEOUT_MILLISECONDS);
		httpServerConnector.setAcceptQueueSize(BIND_BACKLOG_SIZE);
		return httpServerConnector;
	}

	private static ServerConnector createHttpsConnector(Server server, int https_port, String keystore_password, HttpConfiguration https_config) {
		SslContextFactory sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setKeyStorePath(KEYSTORE_FILE.getAbsolutePath());
		sslContextFactory.setKeyStorePassword(keystore_password);

		SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString());

		OptionalSslConnectionFactory optionalSslConnectionFactory = new OptionalSslConnectionFactory(sslConnectionFactory, HttpVersion.HTTP_1_1.asString());

		HttpConnectionFactory httpsConnectionFactory = new HttpConnectionFactory(https_config);

		ServerConnector httpsServerConnector = new ServerConnector(server, optionalSslConnectionFactory, sslConnectionFactory, httpsConnectionFactory);
		httpsServerConnector.setPort(https_port);
		httpsServerConnector.setIdleTimeout(DATA_TRANSFER_TIMEOUT_MILLISECONDS);
		httpsServerConnector.setAcceptQueueSize(BIND_BACKLOG_SIZE);
		return httpsServerConnector;
	}	

	private static ServerConnector createHttps2Connector(Server server, int https_port, String keystore_password, HttpConfiguration https_config) {
		HTTP2ServerConnectionFactory https2ConnectionFactory = new HTTP2ServerConnectionFactory(https_config);
		ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
		alpn.setDefaultProtocol("h2");

		SslContextFactory sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setKeyStorePath(KEYSTORE_FILE.getAbsolutePath());
		sslContextFactory.setKeyStorePassword(keystore_password);
		sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);

		//SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString());
		SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

		OptionalSslConnectionFactory optionalSslConnectionFactory = new OptionalSslConnectionFactory(sslConnectionFactory, HttpVersion.HTTP_1_1.asString());

		HttpConnectionFactory httpsConnectionFactory = new HttpConnectionFactory(https_config);

		//ServerConnector httpsServerConnector = new ServerConnector(server, optionalSslConnectionFactory, sslConnectionFactory, httpsConnectionFactory);
		ServerConnector httpsServerConnector = new ServerConnector(server, optionalSslConnectionFactory, sslConnectionFactory, alpn, https2ConnectionFactory, httpsConnectionFactory);
		httpsServerConnector.setPort(https_port);
		httpsServerConnector.setIdleTimeout(DATA_TRANSFER_TIMEOUT_MILLISECONDS);
		httpsServerConnector.setAcceptQueueSize(BIND_BACKLOG_SIZE);
		return httpsServerConnector;
	}	


	public static Server createServer(Broker broker) {

		final int http_port = broker.brokerConfig.server().port;		
		Util.checkPortNotListening(http_port);

		//Server server = new Server(new QueuedThreadPool(7)); //min 7
		Server server = new Server();

		/*RequestLog requestLog = new RequestLog() {			
			@Override
			public void log(Request request, Response response) {
				log.info(MARKER_REQ, Web.getRequestLogString("REQ",request.getRequestURL().toString(),request));
				//log.info("*** request   "+user+" "+request.getLocalAddr()+" "+request.getRequestURL()+"  "+request.getQueryString()+"  "+response.getStatus());
			}
		};
		server.setRequestLog(requestLog);*/

		ServerConnector httpServerConnector = createHttpConnector(server, http_port, createBaseHttpConfiguration());		

		ServerConnector jwsServerConnector = null;
		if(broker.brokerConfig.server().useJwsPort()) {
			if(KEYSTORE_FILE.exists()) {
				if(useHTTP2) {
					jwsServerConnector = createHttps2Connector(server, broker.brokerConfig.server().jws_port, broker.brokerConfig.server().keystore_password, createJwsHttpsConfiguration(broker.brokerConfig.server().jws_port));
				} else {
					jwsServerConnector = createHttpsConnector(server, broker.brokerConfig.server().jws_port, broker.brokerConfig.server().keystore_password, createJwsHttpsConfiguration(broker.brokerConfig.server().jws_port));
				}
			} else {
				jwsServerConnector = createHttpConnector(server, broker.brokerConfig.server().jws_port, createJwsHttpConfiguration());				
			}
		}

		if(KEYSTORE_FILE.exists()) {
			ServerConnector httpsServerConnector;
			if(useHTTP2) {
				httpsServerConnector = createHttps2Connector(server, broker.brokerConfig.server().secure_port, broker.brokerConfig.server().keystore_password, createBaseHttpsConfiguration(broker.brokerConfig.server().secure_port));
			} else {
				httpsServerConnector = createHttpsConnector(server, broker.brokerConfig.server().secure_port, broker.brokerConfig.server().keystore_password, createBaseHttpsConfiguration(broker.brokerConfig.server().secure_port));
			}

			if(broker.brokerConfig.server().useJwsPort()) {
				Connector[] connectors = new Connector[]{httpServerConnector, httpsServerConnector, jwsServerConnector};
				server.setConnectors(connectors);
				server.setAttribute("digest-http-connector", httpServerConnector.getPort());
				server.setAttribute("basic-https-connector", httpsServerConnector.getPort());
				server.setAttribute("jws-https-connector", jwsServerConnector.getPort());
			} else {
				Connector[] connectors = new Connector[]{httpServerConnector, httpsServerConnector};
				server.setConnectors(connectors);
				server.setAttribute("digest-http-connector", httpServerConnector.getPort());
				server.setAttribute("basic-https-connector", httpsServerConnector.getPort());
			}

		} else {
			if(broker.brokerConfig.server().useJwsPort()) {
				Connector[] connectors = new Connector[]{httpServerConnector, jwsServerConnector};
				server.setConnectors(connectors);
				server.setAttribute("digest-http-connector", httpServerConnector.getPort());
				server.setAttribute("jws-http-connector", jwsServerConnector.getPort());
			} else {				
				Connector[] connectors = new Connector[]{httpServerConnector};
				server.setConnectors(connectors);
				server.setAttribute("digest-http-connector", httpServerConnector.getPort());
			}
		}

		DefaultSessionIdManager sessionIdManager = new DefaultSessionIdManager(server);
		sessionIdManager.setWorkerName(null);
		SessionHandler sessionHandler = new SessionHandler();
		sessionHandler.setSessionIdManager(sessionIdManager);
		sessionHandler.setHttpOnly(true);
		sessionHandler.setSessionCookie("session");
		SessionCookieConfig sessionCokkieConfig = sessionHandler.getSessionCookieConfig();
		sessionCokkieConfig.setPath("/");

		ContextHandlerCollection contextCollection = new ContextHandlerCollection();
		contextCollection.setHandlers(createContextHanlders(broker));


		HandlerList handlerList = new HandlerList();
		handlerList.addHandler(new AbstractHandler() {			
			@Override
			public void handle(String target, Request request, HttpServletRequest httpRequest, HttpServletResponse response)
					throws IOException, ServletException {
				String prefix = request.getHeader("X-Forwarded-Prefix");
				if(prefix != null) {
					String path = request.getPathInfo();
					String original_path = prefix + path; 
					request.setPathInfo(original_path);
					log.info(path + " -> " + original_path);
				}				
			}
		});
		handlerList.addHandler(new OptionalSecuredRedirectHandler());
		handlerList.addHandler(sessionHandler);
		handlerList.addHandler(new InjectHandler());

		if(broker.brokerConfig.server().login) {
			createAuthentication(handlerList, contextCollection, broker);
		} else {
			handlerList.addHandler(contextCollection);
		}

		server.setHandler(handlerList);
		return server;
	}
	
	private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
	private static final boolean OS_WINDOWS = OS_NAME.contains("win");

	private static final String setPlainText;
	private static final String setBoldText;
	static {
		if(System.console() != null && !OS_WINDOWS) {
			setPlainText = "\033[0;0m";
			//setPlainText = "\033[47;1m\033[34;1m";
			setBoldText = "\033[32;1m";
			//setBoldText = "\033[46;1m\033[34;1m";
		} else {
			setPlainText = "";
			setBoldText = "";
		}
	}

	private static String toBold(String s) {
		return setBoldText+s+setPlainText;
	}

	public static void printServerEntrypoint(Server server, Broker broker) {

		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			System.out.println(gdal.VersionInfo("version"));
			try {
				System.out.println((Runtime.getRuntime().maxMemory() / (1024*1024)) + " MBytes max for RSDB allocated memory");
			} catch(Exception e) {
				log.warn(e);
			}
			System.out.println("-------------------------------------------------------------------");
			System.out.println("Hostnames: ");
			System.out.println();
			while(networkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();				
				for(InterfaceAddress interfaceAddress:networkInterface.getInterfaceAddresses()) {
					InetAddress inetAddress = interfaceAddress.getAddress();
					if(inetAddress.getAddress().length==4) {
						System.out.println(inetAddress.getHostAddress()/*+"\t"+networkInterface.getName()*/+"\t\t\t"+ networkInterface.getDisplayName());
					}
				}

			}
		} catch (Exception e) {
			log.error(e);
		}

		Object http_connector = server.getAttribute("digest-http-connector");
		Object basic_https_connector = server.getAttribute("basic-https-connector");
		Object jws_http_connector = server.getAttribute("jws-http-connector");
		Object jws_https_connector = server.getAttribute("jws-https-connector");

		System.out.println();
		if(http_connector != null) {			
			System.out.println("HTTP ("+ broker.brokerConfig.server().http_authentication +" authentication)\t\t" + toBold("http://[HOSTNAME]:" + http_connector));
		}
		if(basic_https_connector != null) {
			System.out.println("HTTPS (basic authentication)\t\t" + toBold("https://[HOSTNAME]:" + basic_https_connector));
		}
		if(jws_http_connector != null) {
			System.out.println("HTTP (JWS authentication)\t\t" + toBold("http://[HOSTNAME]:" + jws_http_connector));
		}
		if(jws_https_connector != null) {
			System.out.println("HTTPS (JWS authentication)\t\t" + toBold("https://[HOSTNAME]:" + jws_https_connector));
		}
		System.out.println();
		//System.out.println("Stop server directly with key combination '"+toBold("Ctrl-C")+"'");
		System.out.println("Stop RSDB server with linux shell command: "+toBold("curl --proxy '' --request POST http://localhost:"+broker.brokerConfig.server().port+"/shutdown?token=rsdb"));
		System.out.println("-------------------------------------------------------------------");
	}

	private static ContextHandler[] createContextHanlders(Broker broker) {
		String prefixPath = broker.brokerConfig.server().url_prefix;
		ContextHandler[] contexts = new ContextHandler[] {
				createContext(prefixPath + POINTDB_API_URL, new APICollectionHandler(broker)),
				createContext(prefixPath + "/pointdbs", true, new APIHandler_pointdbs(broker)),
				createContext(prefixPath + RASTERDB_API_URL, new RasterdbHandler(broker)),
				createContext(prefixPath + POINTCLOUDS_URL, true, new APIHandler_pointclouds(broker)),
				createContext(prefixPath + VOXELDBS_URL, true, new APIHandler_voxeldbs(broker)),
				createContext(prefixPath + VECTORDBS_URL, true, new VectordbsHandler(broker)),
				createContext(prefixPath + POI_GROUPS_URL, true, new APIHandler_poi_groups(broker)),
				createContext(prefixPath + ROI_GROUPS_URL, true, new APIHandler_roi_groups(broker)),
				createContext(prefixPath + MAIN_API_URL, new MainAPICollectionHandler(broker)),
				createContext(prefixPath + "/rasterdb_wms", true, new WmsHandler(broker)),
				createContext(prefixPath + "/rasterdbs.json", true, new RasterdbsHandler(broker)),
				createContext(prefixPath + WEBCONTENT_URL, createWebcontentHandler()),
				createContext(prefixPath + WEBFILES_URL, createWebfilesHandler()),
				createContext(prefixPath + "/entrypoint", true, new BaseRedirector(prefixPath + WEBCONTENT_URL + "/admin2/")),
				createContext(prefixPath + "/logout", true, new LogoutHandler(broker)),
				createContext(prefixPath, createShutdownHandler("rsdb")),
				createContext(prefixPath, new BaseRedirector(prefixPath + "/entrypoint")),
				createContext(prefixPath, new InvalidUrlHandler("unknown request")),
		};
		return contexts;
	}

	/*private static ContextHandler createContext(Handler handler) {
		ContextHandler context = new ContextHandler();		
		context.setHandler(handler);
		return context;
	}*/

	private static ContextHandler createContext(String contextPath, Handler handler) {
		ContextHandler context = new ContextHandler(contextPath);
		context.setHandler(handler);
		return context;
	}

	private static ContextHandler createContext(String contextPath, boolean allowNullPath, Handler handler) {
		ContextHandler context = new ContextHandler(contextPath);
		context.setAllowNullPathInfo(allowNullPath);
		context.setHandler(handler);
		return context;
	}

	private static Handler createShutdownHandler(String token) {
		Handler handler = new ShutdownHandler(token, false, true);
		return handler;
	}

	private static Handler createWebcontentHandler() {
		ResourceHandler resource_handler = new ResourceHandler();
		resource_handler.setCacheControl("no-store,no-cache,must-revalidate"); // don't cache
		resource_handler.setPrecompressedFormats(new CompressedContentFormat[]{CompressedContentFormat.BR, CompressedContentFormat.GZIP});
		//MimeTypes mimeTypes = resource_handler.getMimeTypes();
		//resource_handler.setMinMemoryMappedContentLength(-1); // not memory mapped to prevent file locking, removed in jetty 9.4
		resource_handler.setDirectoriesListed(false); // don't show directory content
		resource_handler.setResourceBase(WEBCONTENT_PATH);
		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] {resource_handler, new InvalidUrlHandler("webcontent not found")});
		return handlers;
	}

	private static Handler createWebfilesHandler() {
		ResourceHandler resource_handler = new ResourceHandler();
		resource_handler.setCacheControl("no-store,no-cache,must-revalidate"); // don't cache
		//MimeTypes mimeTypes = resource_handler.getMimeTypes();
		//resource_handler.setMinMemoryMappedContentLength(-1); // not memory mapped to prevent file locking, removed in jetty 9.4
		resource_handler.setDirectoriesListed(true); // show directory content
		resource_handler.setResourceBase(WEBFILES_PATH);
		resource_handler.setStylesheet(WEBFILES_CSS_FILE);
		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] {resource_handler, new InvalidUrlHandler("webfile not found")});
		return handlers;
	}

	private static final File REALM_IP_CSV_FILE = new File("realm_ip.csv");

	private static Handler createAuthentication(HandlerList handlerList, Handler handler, Broker broker) {
		/*HashLoginService loginService = new HashLoginService(""); // RCurl bug empty realm
		loginService.setUserStore(broker.getUserStore());*/
		LoginService loginService = broker.accountManager();
		Map<String, String> ipMap = new HashMap<String, String>();
		if(REALM_IP_CSV_FILE.exists()) {
			Table ipTable = Table.readCSV(REALM_IP_CSV_FILE, ',');
			ColumnReaderString ipReader = ipTable.createColumnReader("ip");
			ColumnReaderString userReader = ipTable.createColumnReader("user");
			for(String[] row:ipTable.rows) {
				String ip = ipReader.get(row);
				String user = userReader.get(row);
				if(ipMap.containsKey(ip)) {
					log.warn("overwrite existing entry of "+ip+"  "+ipMap.get(ip)+" with "+user+"    in "+REALM_IP_CSV_FILE);
				}
				if(broker.accountManager().getUserIdentity(user) == null) {
					log.warn("user does not exist entry of "+ip+" with "+user+"    in "+REALM_IP_CSV_FILE);
				}
				ipMap.put(ip, user);
			}
		}

		IpAuthentication ipAuthentication = new IpAuthentication(broker.accountManager(), ipMap);

		JWSAuthentication jwsAuthentication = new JWSAuthentication(broker);



		Constraint constraint = new Constraint();
		constraint.setName("constraint");
		constraint.setAuthenticate(true);
		constraint.setRoles(new String[] {"**"}); // any authenticated user is permitted

		ConstraintMapping constraintMapping = new ConstraintMapping();
		constraintMapping.setPathSpec("/*");
		constraintMapping.setConstraint(constraint);

		ConstraintSecurityHandler httpSecurityHandler = new ConstraintSecurityHandler();
		String a = broker.brokerConfig.server().http_authentication;
		switch(a) {
		case "basic": {
			BasicAuthenticator basicAuthenticator = new BasicAuthenticator();
			httpSecurityHandler.setAuthenticator(basicAuthenticator);
			break;
		}
		case "digest": {
			DigestAuthenticator digestAuthenticator = new DigestAuthenticator();
			httpSecurityHandler.setAuthenticator(digestAuthenticator);
			break;
		}
		default:
			throw new RuntimeException("unknown authenticator: " + a);
		}

		httpSecurityHandler.setLoginService(loginService);
		httpSecurityHandler.setConstraintMappings(Collections.singletonList(constraintMapping));
		httpSecurityHandler.setHandler(handler);		
		PredicateHandler predicacteHttpHandler = new PredicateHandler(r->!r.isSecure());
		predicacteHttpHandler.setHandler(httpSecurityHandler);

		ConstraintSecurityHandler httpsSecurityHandler = new ConstraintSecurityHandler();
		BasicAuthenticator basicAuthenticator = new BasicAuthenticator();
		httpsSecurityHandler.setAuthenticator(basicAuthenticator);
		httpsSecurityHandler.setLoginService(loginService);
		httpsSecurityHandler.setConstraintMappings(Collections.singletonList(constraintMapping));
		httpsSecurityHandler.setHandler(handler);		
		PredicateHandler predicacteHttpsHandler = new PredicateHandler(r->r.isSecure());
		predicacteHttpsHandler.setHandler(httpsSecurityHandler);		

		String prefixPath = broker.brokerConfig.server().url_prefix;
		handlerList.addHandler(createContext(prefixPath + "/login", true, new LoginHandler(broker)));
		handlerList.addHandler(jwsAuthentication);
		handlerList.addHandler(ipAuthentication);
		handlerList.addHandler(predicacteHttpHandler);
		handlerList.addHandler(predicacteHttpsHandler);

		return handlerList;
	}




}
