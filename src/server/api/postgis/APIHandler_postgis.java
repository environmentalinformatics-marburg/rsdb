package server.api.postgis;

import java.io.IOException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;
import org.tinylog.Logger;

import broker.Broker;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.Web;

public class APIHandler_postgis extends AbstractHandler {

	private final Broker broker;
	private final APIHandler_postgis_layers apihandler_postgis_layers;	

	public APIHandler_postgis(Broker broker) {
		this.broker = broker;
		apihandler_postgis_layers = new APIHandler_postgis_layers(broker);
	}

	@Override
	public void handle(String target, Request request, HttpServletRequest r, HttpServletResponse response) throws IOException, ServletException {
		request.setHandled(true);
		UserIdentity userIdentity = Web.getUserIdentity(request);
		try {
			if(target.equals("/")) {
				handleList(request, response, userIdentity);
			} else {
				int i = target.indexOf('/', 1);
				if(i == 1) {
					throw new RuntimeException("no name in: "+target);
				}			
				String name = i < 0 ? target.substring(1) : target.substring(1, i);
				String next = i < 0 ? "/" : target.substring(i);
				switch(name) {
				case "layers":
					apihandler_postgis_layers.handle(next, request, (Response)response, userIdentity);
					break;
				default:
					throw new RuntimeException("unknown target");
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			response.setContentType(Web.MIME_TEXT);
			response.getWriter().println(e);
		}
	}

	private void handleList(Request request, HttpServletResponse response, UserIdentity userIdentity) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("postgis");
		json.value("ok");
		json.endObject();
	}
}
