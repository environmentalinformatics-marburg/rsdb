package server;

import java.io.File;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.SessionCookieConfig;

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
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.OptionalSslConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
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
import util.Table;
import util.Table.ColumnReaderString;
import util.Util;
import util.Web;

public class PointDBServer {
	static final Logger log = LogManager.getLogger();
	public static final Marker MARKER_REQ = MarkerManager.getMarker("REQ");

	private static final long DATA_TRANSFER_TIMEOUT_MILLISECONDS = 2*60*60*1000; // set timeout to 2 hours

	private static final String POINTDB_API_URL = "/pointdb";
	//private static final String SPECTRALDB_API_URL = "/spectraldb";
	private static final String RASTERDB_API_URL = "/rasterdb";
	private static final String MAIN_API_URL = "/api";
	private static final String POINTDB_WEB_URL = "/web";
	private static final String WEBFILES_URL = "/files";
	private static final String POINTCLOUDS_URL = "/pointclouds";
	private static final String VECTORDBS_URL = "/vectordbs";
	private static final String POI_GROUPS_URL = "/poi_groups";
	private static final String ROI_GROUPS_URL = "/roi_groups";

	private static final String WEBCONTENT_PATH = "webcontent"; //static files for web pages (no folder listing)
	private static final String WEBFILES_PATH = "webfiles"; //static files for user download (with folder listing)
	private static final String WEBFILES_CSS_FILE = WEBCONTENT_PATH+"/jetty-dir.css"; // stylesheet for webfiles directory listing

	private static final File KEYSTORE_FILE = new File("keystore");
	
	private static HttpConfiguration createBaseHttpConfiguration() {
		HttpConfiguration httpConfiguration = new HttpConfiguration();
		httpConfiguration.setSendServerVersion(false);
		httpConfiguration.setSendDateHeader(false);
		httpConfiguration.setSendXPoweredBy(false);
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
		httpsConfiguration.setOutputBufferSize(32768);
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
		httpServerConnector.setAcceptQueueSize(0);
		return httpServerConnector;
	}
	
	private static ServerConnector createHttpsConnector(Server server, int https_port, String keystore_password, HttpConfiguration https_config) {
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
		httpsServerConnector.setAcceptQueueSize(0);
		return httpsServerConnector;
	}	


	public static Server createServer(Broker broker) {

		final int http_port = broker.brokerConfig.server().port;		
		Util.checkPortNotListening(http_port);

		//Server server = new Server(new QueuedThreadPool(7)); //min 7
		Server server = new Server();

		RequestLog requestLog = new RequestLog() {			
			@Override
			public void log(Request request, Response response) {
				log.info(MARKER_REQ, Web.getRequestLogString("REQ",request.getRequestURL().toString(),request));
				//log.info("*** request   "+user+" "+request.getLocalAddr()+" "+request.getRequestURL()+"  "+request.getQueryString()+"  "+response.getStatus());
			}
		};
		server.setRequestLog(requestLog);

		ServerConnector httpServerConnector = createHttpConnector(server, http_port, createBaseHttpConfiguration());		

		ServerConnector jwsServerConnector = null;
		if(broker.brokerConfig.server().useJwsPort()) {
			if(KEYSTORE_FILE.exists()) {
				jwsServerConnector = createHttpsConnector(server, broker.brokerConfig.server().jws_port, broker.brokerConfig.server().keystore_password, createJwsHttpsConfiguration(broker.brokerConfig.server().jws_port));					
			} else {
				jwsServerConnector = createHttpConnector(server, broker.brokerConfig.server().jws_port, createJwsHttpConfiguration());				
			}
		}

		if(KEYSTORE_FILE.exists()) {
			ServerConnector httpsServerConnector = createHttpsConnector(server, broker.brokerConfig.server().secure_port, broker.brokerConfig.server().keystore_password, createBaseHttpsConfiguration(broker.brokerConfig.server().secure_port));
			
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

	private static final String setPlainText;
	private static final String setBoldText;
	static {
		if(System.console()!=null) {
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

		Object digest_http_connector = server.getAttribute("digest-http-connector");
		Object basic_https_connector = server.getAttribute("basic-https-connector");
		Object jws_http_connector = server.getAttribute("jws-http-connector");
		Object jws_https_connector = server.getAttribute("jws-https-connector");

		System.out.println();
		if(digest_http_connector != null) {
			System.out.println("HTTP (digest authentication)\t\t" + toBold("http://[HOSTNAME]:" + digest_http_connector));
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
		System.out.println("Stop server with linux shell command: "+toBold("curl --proxy '' --request POST http://localhost:"+broker.brokerConfig.server().port+"/shutdown?token=pointdb"));
		System.out.println("-------------------------------------------------------------------");
	}

	private static ContextHandler[] createContextHanlders(Broker broker) {
		ContextHandler[] contexts = new ContextHandler[] {
				createContext(POINTDB_API_URL, new APICollectionHandler(broker)),
				createContext("/pointdbs", true, new APIHandler_pointdbs(broker)),
				//createContext(SPECTRALDB_API_URL, new SpectraldbAPICollectionHandler(broker)),
				createContext(RASTERDB_API_URL, new RasterdbHandler(broker)),
				createContext(POINTCLOUDS_URL, true, new APIHandler_pointclouds(broker)),
				createContext(VECTORDBS_URL, true, new VectordbsHandler(broker)),
				createContext(POI_GROUPS_URL, true, new APIHandler_poi_groups(broker)),
				createContext(ROI_GROUPS_URL, true, new APIHandler_roi_groups(broker)),
				createContext(MAIN_API_URL, new MainAPICollectionHandler(broker)),
				createContext("/rasterdb_wms", true, new WmsHandler(broker)),
				createContext("/rasterdbs.json", true, new RasterdbsHandler(broker)),
				createContext(POINTDB_WEB_URL, createWebcontentHandler()),
				createContext(WEBFILES_URL, createWebfilesHandler()),
				createContext("/entrypoint", true, new BaseRedirector(POINTDB_WEB_URL + "/admin2/")),
				createContext(createShutdownHandler()),
				createContext(new BaseRedirector(POINTDB_WEB_URL+"/")),
				createContext(new InvalidUrlHandler("unknown request")),
		};
		return contexts;
	}

	private static ContextHandler createContext(Handler handler) {
		ContextHandler context = new ContextHandler();		
		context.setHandler(handler);
		return context;
	}

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

	private static Handler createShutdownHandler() {
		Handler handler = new ShutdownHandler("pointdb", false, true);
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
		//HashLoginService loginService = new HashLoginService("server");
		HashLoginService loginService = new HashLoginService(""); // RCurl bug empty realm
		loginService.setUserStore(broker.getUserStore());
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
				if(broker.getUserStore().getUserIdentity(user) == null) {
					log.warn("user does not exist entry of "+ip+" with "+user+"    in "+REALM_IP_CSV_FILE);
				}
				ipMap.put(ip, user);
			}
		}

		IpAuthentication ipAuthentication = new IpAuthentication(broker.getUserStore(), ipMap);

		JWSAuthentication jwsAuthentication = new JWSAuthentication(broker);



		Constraint constraint = new Constraint();
		constraint.setName("constraint");
		constraint.setAuthenticate(true);
		constraint.setRoles(new String[] {"**"}); // any authenticated user is permitted

		ConstraintMapping constraintMapping = new ConstraintMapping();
		constraintMapping.setPathSpec("/*");
		constraintMapping.setConstraint(constraint);

		DigestAuthenticator digestAuthenticator = new DigestAuthenticator();
		ConstraintSecurityHandler securityDigestHandler = new ConstraintSecurityHandler();
		securityDigestHandler.setAuthenticator(digestAuthenticator);
		securityDigestHandler.setLoginService(loginService);
		securityDigestHandler.setConstraintMappings(Collections.singletonList(constraintMapping));
		securityDigestHandler.setHandler(handler);		
		PredicateHandler predicacteHttpHandler = new PredicateHandler(r->!r.isSecure());
		predicacteHttpHandler.setHandler(securityDigestHandler);

		BasicAuthenticator basicAuthenticator = new BasicAuthenticator();
		ConstraintSecurityHandler securityBasicHandler = new ConstraintSecurityHandler();
		securityBasicHandler.setAuthenticator(basicAuthenticator);
		securityBasicHandler.setLoginService(loginService);
		securityBasicHandler.setConstraintMappings(Collections.singletonList(constraintMapping));
		securityBasicHandler.setHandler(handler);		
		PredicateHandler predicacteHttpsHandler = new PredicateHandler(r->r.isSecure());
		predicacteHttpsHandler.setHandler(securityBasicHandler);		

		handlerList.addHandler(jwsAuthentication);
		handlerList.addHandler(ipAuthentication);
		handlerList.addHandler(predicacteHttpHandler);
		handlerList.addHandler(predicacteHttpsHandler);

		return handlerList;
	}


}
