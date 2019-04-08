package server.api.main;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

import broker.Broker;
import broker.acl.DynamicPropertyUserStore;
import broker.acl.EmptyACL;
import server.api.APIHandler;
import util.Web;

/**
 * Reload accounts from source.
 * list names and roles.
 * @author woellauer
 *
 */
public class APIHandler_accounts extends APIHandler {
	//private static final Logger log = LogManager.getLogger();
	
	public APIHandler_accounts(Broker broker) {
		super(broker, "accounts");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		EmptyACL.ADMIN.check(Web.getUserIdentity(request));
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("accounts");
		json.array();
		DynamicPropertyUserStore userStore = broker.getUserStore();
		userStore.reloadAccounts();
		Map<String, UserIdentity> ui = broker.getUserStore().getKnownUserIdentities();
		for(UserIdentity userIdentity:ui.values()) {
			json.object();
			json.key("name");
			json.value(userIdentity.getUserPrincipal().getName());
			json.key("roles");
			json.value(APIHandler_identity.getRoles(userIdentity));
			json.endObject();
		}
		json.endArray();
		json.endObject();		
	}
}
