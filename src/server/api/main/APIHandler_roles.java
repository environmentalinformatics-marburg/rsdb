package server.api.main;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONWriter;
import org.tinylog.Logger;

import broker.Broker;
import jakarta.servlet.http.HttpServletResponse;
import server.api.APIHandler;
import util.Web;

public class APIHandler_roles extends APIHandler {

	public APIHandler_roles(Broker broker) {
		super(broker, "roles");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		Logger.info("request roles");
		//UserIdentity userIdentity = Web.getUserIdentity(request);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		
		HashSet<String> collector = new HashSet<>();
		broker.accountManager().collectRoles(collector);
		broker.catalog.collectRoles(collector);
		String[] roles = collector.toArray(new String[0]);
		Arrays.sort(roles, String.CASE_INSENSITIVE_ORDER);		
		
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("roles");
		/*// List all roles for all users
		if(!EmptyACL.ADMIN.isAllowed(userIdentity)) {
			roles = Arrays.stream(roles).filter(role -> userIdentity.isUserInRole(role, null)).toArray(String[]::new);
		}*/
		json.value(roles);
		json.endObject();		
	}
}
