package server.api.main;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

import broker.Broker;
import broker.acl.EmptyACL;
import server.api.APIHandler;
import util.Web;

public class APIHandler_roles extends APIHandler {
	private static final Logger log = LogManager.getLogger();

	public APIHandler_roles(Broker broker) {
		super(broker, "roles");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		log.info("request roles");
		UserIdentity userIdentity = Web.getUserIdentity(request);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("roles");
		String[] roles = broker.catalog.getRoles();
		if(!EmptyACL.ADMIN.isAllowed(userIdentity)) {
			roles = Arrays.stream(roles).filter(role -> userIdentity.isUserInRole(role, null)).toArray(String[]::new);
		}
		json.value(roles);
		json.endObject();		
	}
}
