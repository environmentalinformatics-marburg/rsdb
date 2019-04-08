package server.api.main;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.Broker;
import server.api.APIHandler;
import util.Web;

public class APIHandler_catalog_json extends APIHandler {
	//private static final Logger log = LogManager.getLogger();

	public APIHandler_catalog_json(Broker broker) {
		super(broker, "catalog.json");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		UserIdentity userIdentity = Web.getUserIdentity(request);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		broker.catalog.writeJSON(response.getWriter(), userIdentity, false);
	}
}
