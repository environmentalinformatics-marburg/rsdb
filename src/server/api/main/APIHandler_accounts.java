package server.api.main;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import broker.Account;
import broker.Account.Builder;
import broker.AccountManager;
import broker.Broker;
import broker.acl.EmptyACL;
import server.api.APIHandler;
import util.JsonUtil;
import util.Web;

/**
 * Reload accounts from source.
 * list names and roles.
 * @author woellauer
 *
 */
public class APIHandler_accounts extends APIHandler {
	

	public APIHandler_accounts(Broker broker) {
		super(broker, "accounts");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		switch(request.getMethod()) {
		case "GET":
			handleGET(target, request, response);
			break;
		case "POST":
			handlePOST(target, request, response);
			break;
		default:
			throw new RuntimeException("invalid HTTP method: " + request.getMethod());
		}		
	}

	private void handleGET(String target, Request request, Response response) throws IOException {		
		EmptyACL.ADMIN.check(Web.getUserIdentity(request));
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("accounts");
		json.array();
		AccountManager accountManager = broker.accountManager();
		try {
			accountManager.read();
		} catch (Exception e) {
			Logger.warn(e);
		}
		accountManager.foreachAccount(account -> {
			json.object();
			json.key("name");
			json.value(account.user);
			json.key("roles");
			json.value(account.roles);
			json.key("managed");
			json.value(account.managed);
			json.endObject();
		});
		json.endArray();
		json.endObject();		
	}

	private void handlePOST(String target, Request request, Response response) throws IOException {
		EmptyACL.ADMIN.check(Web.getUserIdentity(request));
		try {
			JSONObject json = new JSONObject(Web.requestContentToString(request));
			JSONArray actions = json.getJSONArray("actions");
			int actionsLen = actions.length();
			for (int i = 0; i < actionsLen; i++) {
				JSONObject action = actions.getJSONObject(i);
				String key = action.getString("action");
				switch(key) {
				case "add_account": {
					String user = action.getString("user");
					String password = action.getString("password");
					String[] roles = JsonUtil.optStringTrimmedArray(action, "roles");
					Account account = new Account(user, password, roles, true);
					broker.accountManager().addAccount(account);
					Logger.info(user);
					break;
				}
				case "remove_account": {
					String user = action.getString("user");					
					broker.accountManager().remvoeAccount(user);
					Logger.info(user);
					break;
				}
				case "set_account": {
					String user = action.getString("user");
					AccountManager accountManager = broker.accountManager();
					synchronized (accountManager) {
						Account account = accountManager.getAccount(user);
						if(account == null) {
							throw new RuntimeException("account not found");
						}
						Builder builder = account.builder();
						if(action.has("roles")) {
							builder.roles = JsonUtil.optStringTrimmedArray(action, "roles");
						}
						if(action.has("password")) {
							//Logger.info("has pw");
							String password = action.getString("password").trim();
							if(!password.isEmpty()) {
								builder.password = password;
								//Logger.info("set pw  " + builder.password);
							}
						}
						accountManager.setAccount(builder.build());
					}					
					Logger.info(user);
					break;
				}
				default: throw new RuntimeException("unknown key: "+key);
				}
			}			
			response.setContentType(MIME_JSON);
			JSONWriter jsonResponse = new JSONWriter(response.getWriter());
			jsonResponse.object();
			jsonResponse.key("response");
			jsonResponse.object();
			jsonResponse.endObject();
			jsonResponse.endObject();
		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e);			
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().append(e.toString());
		}
	}
}
