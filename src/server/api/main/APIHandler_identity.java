package server.api.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.ForkJoinPool;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;

import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Authentication.User;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.UserIdentity;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.json.JSONWriter;
import org.tinylog.Logger;

import broker.Account;
import broker.Broker;
import broker.MetaDataSchema;
import broker.acl.FastUserIdentity;
import jakarta.servlet.http.HttpServletResponse;
import server.api.APIHandler;
import util.FileUtil;
import util.JsonUtil;
import util.Web;
import util.collections.vec.Vec;

public class APIHandler_identity extends APIHandler {
	
	static {
		gdal.AllRegister();
		Logger.info("GDAL version "+gdal.VersionInfo());
	}

	private static final Field FIELD_ROLES;

	static {
		Field fieldRoles = null;
		try {
			fieldRoles = DefaultUserIdentity.class.getDeclaredField("_roles");
			fieldRoles.setAccessible(true);
		} catch(Exception e) {}
		FIELD_ROLES = fieldRoles;		
	}

	public APIHandler_identity(Broker broker) {
		super(broker, "identity");
	}

	public static String[] getRoles(UserIdentity userIdentity) {
		if(userIdentity == null) {
			return new String[] {"admin"};
		}
		if(userIdentity instanceof FastUserIdentity) {
			return ((FastUserIdentity) userIdentity).getRoles();
		}
		if(userIdentity instanceof DefaultUserIdentity && FIELD_ROLES != null) {
			try {
				return (String[]) FIELD_ROLES.get(userIdentity);
			} catch(Exception e) {
				Logger.warn("could not get roles " + e);
				return new String[] {};
			}
		}
		if(userIdentity instanceof Account) {
			Account account = ((Account) userIdentity);
			return account.roles;
		}
		Logger.warn("could not get roles " + userIdentity.getClass());
		return new String[] {};
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {		
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		String user = "anonymous";
		String authMethod = "";
		String[] roles = new String[]{};

		Authentication authentication = request.getAuthentication();
		if(authentication != null && (authentication instanceof User)) {
			User authUser = (User) authentication;
			UserIdentity userIdentity = authUser.getUserIdentity();
			user = userIdentity.getUserPrincipal().getName();
			authMethod = authUser.getAuthMethod();
			roles = getRoles(userIdentity);
		} else {
			roles = new String[]{"admin"};
		}

		json.object();
		json.key("ip");
		json.value(request.getRemoteAddr());
		json.key("user");
		json.value(user);
		json.key("auth_method");
		json.value(authMethod.isEmpty() ? "no" : authMethod);
		json.key("roles");
		json.value(roles);
		json.key("secure");
		json.value(request.isSecure());
		StringBuffer url = request.getRequestURL();
		String host = url.substring(url.indexOf("://") + 3, url.indexOf("/api/identity"));
		if(host.indexOf(':') > -1) {
			host = host.substring(0, host.indexOf(':'));
		}
		String url_prefix = broker.brokerConfig.server().url_prefix;
		if(broker.brokerConfig.server().useHTTP()) {
			json.key("plain_wms_url");
			json.value("http://"+host+":"+broker.brokerConfig.server().port + url_prefix +  "/rasterdb_wms");
		}
		if(broker.brokerConfig.server().useHTTPS()) {
			json.key("secure_wms_url");
			json.value("https://"+request.getLocalAddr()+":"+broker.brokerConfig.server().secure_port + url_prefix + "/rasterdb_wms");
		}
		json.key("session");
		json.value(APIHandler_session.createSession());
		/*json.key("https_port");
		json.value(broker.brokerConfig.server().secure_port);*/


		if(request.getHttpChannel() != null && request.getHttpChannel().getServer() != null) {
			Server server = request.getHttpChannel().getServer();
			JsonUtil.optPutString(json, "http_port", server.getAttribute("digest-http-connector"));
			JsonUtil.optPutString(json, "https_port", server.getAttribute("basic-https-connector"));
			if(server.getAttribute("jws-http-connector") != null) {
				JsonUtil.optPutString(json, "jws_protocol", "http:");
				JsonUtil.optPutString(json, "jws_port", server.getAttribute("jws-http-connector"));
			}
			if(server.getAttribute("jws-https-connector") != null) {
				JsonUtil.optPutString(json, "jws_protocol", "https:");
				JsonUtil.optPutString(json, "jws_port", server.getAttribute("jws-https-connector"));
			}
		}

		json.key("url_base");
		json.value(url.substring(0, url.indexOf("/api/identity")));
		
		
		MetaDataSchema metaDataSchema = broker.metaDataSchema();
		json.key("meta_data_schema");
		json.object();
		json.key("description");
		json.value(metaDataSchema.meta_data_schema_description);
		json.key("keys");
		metaDataSchema.toJSON(json);
		json.endObject();
		

		try {			
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			pw.println(System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch") + " Operating system");
			pw.println(Runtime.version() + " Java version");
			try {
				pw.println(gdal.VersionInfo("version"));
			} catch(Exception e) {
				Logger.warn(e);
			}

			Runtime r = Runtime.getRuntime();			
			r.gc();

			try {
				MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

				Object attributeValaue_Uptime = mBeanServer.getAttribute(new ObjectName("java.lang","type","Runtime"), "Uptime");
				long uptime_s_total = Long.parseLong(attributeValaue_Uptime.toString()) / 1000;

				long uptime_s = (uptime_s_total % 60);
				long uptime_min_total = uptime_s_total / 60;

				long uptime_min = (uptime_min_total % 60);
				long uptime_hour_total = uptime_min_total / 60;

				long uptime_hour = (uptime_hour_total % 24);
				long uptime_day_total = uptime_hour_total / 24;

				pw.println(uptime_day_total + " days " + String.format("%02d", uptime_hour) + ":" + String.format("%02d", uptime_min) + ":" +  String.format("%02d", uptime_s) + " RSDB uptime");

				CompositeDataSupport cHeapMemoryUsage = (CompositeDataSupport) mBeanServer.getAttribute(new ObjectName("java.lang","type","Memory"), "HeapMemoryUsage");
				CompositeDataSupport cNonHeapMemoryUsage = (CompositeDataSupport) mBeanServer.getAttribute(new ObjectName("java.lang","type","Memory"), "NonHeapMemoryUsage");
				long usedHeapMemoryUsage = Long.parseLong(cHeapMemoryUsage.get("used").toString());
				long usedNonHeapMemoryUsage = Long.parseLong(cNonHeapMemoryUsage.get("used").toString());				
				pw.println();
				pw.println(String.format(Locale.ENGLISH, "%.3f", (usedHeapMemoryUsage / (1024*1024)) / 1024d) + " GB RSDB heap memory usage");
				pw.println(String.format(Locale.ENGLISH, "%.3f", (usedNonHeapMemoryUsage / (1024*1024)) / 1024d) + " GB RSDB non heap memory usage");

				/*Set<ObjectName> mbeans = mBeanServer.queryNames(null, null);
				for (ObjectName mbean : mbeans)
				{
					MBeanInfo info = mBeanServer.getMBeanInfo(mbean);
					MBeanAttributeInfo[] attrInfo = info.getAttributes();

					pw.println("Attributes for object: " + mbean);
					for (MBeanAttributeInfo attr : attrInfo)
					{
						pw.println("  " + attr.getName());
					}
				}


				Set<ObjectName> names = mBeanServer.queryNames(null, null);
				for(ObjectName name:names) {
					String propName = name.getCanonicalName();
					pw.println(propName);
				}*/
			} catch(Exception e) {
				Logger.warn(e);
			}

			pw.println();
			pw.println("" + r.availableProcessors() + " stated processor cores");
			pw.println("" + ForkJoinPool.commonPool().getParallelism() + " common thread pool parallelism");
			pw.println();
			pw.println(String.format(Locale.ENGLISH, "%.3f", (r.maxMemory() / (1024*1024)) / 1024d) + " GB maximum for RSDB Java allocatable memory");
			pw.println(String.format(Locale.ENGLISH, "%.3f", (r.totalMemory() / (1024*1024)) / 1024d) + " GB for RSDB Java currently allocated memory");
			pw.println(String.format(Locale.ENGLISH, "%.3f", (r.freeMemory() / (1024*1024)) / 1024d) + " GB RSDB Java free memory (from currently allocated memory)");
			pw.println(String.format(Locale.ENGLISH, "%.3f", (((r.maxMemory() - r.totalMemory() + r.freeMemory()) ) / (1024*1024)) / 1024d) + " GB RSDB Java free memory (from max allocatable memory)");
			pw.println(String.format(Locale.ENGLISH, "%.3f", ((r.totalMemory() - r.freeMemory()) / (1024*1024)) / 1024d) + " GB RSDB Java used memory (all memmory not in use at this moment has been garbage collected)");

			try {
				long rasterdbSize = FileUtil.getFolderSize(Broker.rasterdb_root);
				long pointcloudSize = FileUtil.getFolderSize(Broker.pointcloud_root);
				long vectordbSize = FileUtil.getFolderSize(Broker.vectordb_root);
				long voxeldbSize = FileUtil.getFolderSize(Broker.voxeldb_root);
				long pointdbSize = FileUtil.getFolderSize(Paths.get("pointdb"));
				//long totalSize = getFolderSize(Paths.get("."));  // possibly slow
				long totalSize = rasterdbSize + pointcloudSize + vectordbSize + voxeldbSize + pointdbSize;
				pw.println();
				pw.println(String.format(Locale.ENGLISH, "%.3f", (rasterdbSize / (1024*1024)) / 1024d) + " GB rasterdb layers used disk space");
				pw.println(String.format(Locale.ENGLISH, "%.3f", (pointcloudSize / (1024*1024)) / 1024d) + " GB pointcloud layers used disk space");
				pw.println(String.format(Locale.ENGLISH, "%.3f", (vectordbSize / (1024*1024)) / 1024d) + " GB vectordb layers used disk space");
				pw.println(String.format(Locale.ENGLISH, "%.3f", (voxeldbSize / (1024*1024)) / 1024d) + " GB voxeldb layers used disk space");
				if(pointdbSize > 0) {
					pw.println(String.format(Locale.ENGLISH, "%.3f", (pointdbSize / (1024*1024)) / 1024d) + " GB pointdb layers used disk space");
				}
				pw.println();
				pw.println(String.format(Locale.ENGLISH, "%.3f", (totalSize / (1024*1024)) / 1024d) + " GB layers total used disk space");
			} catch(Exception e) {
				Logger.warn(e);
			}

			try {
				long rsdbFree = Paths.get(".").toFile().getUsableSpace();
				pw.println();
				pw.println(String.format(Locale.ENGLISH, "%.3f", (rsdbFree / (1024*1024)) / 1024d) + " GB RSDB free disk space");
			} catch(Exception e) {
				Logger.warn(e);
			}

			try {
				int driverCount = gdal.GetDriverCount();
				pw.println();
				pw.println();
				pw.println("GDAL supported file formats: " + driverCount);
				pw.println();
				Vec<String> formats = new Vec<String>();
				for (int i = 0; i < driverCount; i++) {
					Driver driver = gdal.GetDriver(i);
					formats.add(driver.getShortName() + " - " + driver.getLongName());
				}
				formats.sort(String.CASE_INSENSITIVE_ORDER);
				formats.forEach(pw::println);
			} catch(Exception e) {
				Logger.warn(e);
			}			


			pw.flush();
			String s = sw.toString();

			json.key("diagnostics");
			json.value(s);
		} catch(Exception e) {
			Logger.warn(e);
		}

		json.endObject();		
	}
}
