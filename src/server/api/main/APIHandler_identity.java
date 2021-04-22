package server.api.main;

import java.io.IOException;
import java.lang.reflect.Field;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Authentication.User;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

import broker.Account;
import broker.Broker;
import broker.acl.FastUserIdentity;
import server.api.APIHandler;
import util.JsonUtil;

public class APIHandler_identity extends APIHandler {
	private static final Logger log = LogManager.getLogger();

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
				log.warn("could not get roles " + e);
				return new String[] {};
			}
		}
		if(userIdentity instanceof Account) {
			Account account = ((Account) userIdentity);
			return account.roles;
		}
		log.warn("could not get roles " + userIdentity.getClass());
		return new String[] {};
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {		
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
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
		json.key("plain_wms_url");
		json.value("http://"+host+":"+broker.brokerConfig.server().port + url_prefix +  "/rasterdb_wms");
		json.key("secure_wms_url");
		json.value("https://"+request.getLocalAddr()+":"+broker.brokerConfig.server().secure_port + url_prefix + "/rasterdb_wms");
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

		json.endObject();		
	}
}
