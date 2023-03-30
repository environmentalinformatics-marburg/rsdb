package server.api.main;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.Base64;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONWriter;

import broker.Broker;
import server.api.APIHandler;
import util.Web;

public class APIHandler_session extends APIHandler {
	

	private static final SecureRandom random = new SecureRandom();
	
	public static String createSession() {
		return Base64.getUrlEncoder().encodeToString(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(random.nextLong()).array()).substring(0, 6);
	}
	
	public static long decodeSession(String base64) {
		byte[] decodedBytes = Base64.getUrlDecoder().decode(base64+"AAAAA");
		long decodedSession = ByteBuffer.wrap(decodedBytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
		return decodedSession;
	}

	public APIHandler_session(Broker broker) {
		super(broker, "session");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {		
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());

		String base64 = createSession();
		Logger.info(decodeSession(base64));

		json.object();
		json.key("session");
		json.value(base64);
		json.endObject();


	}
}
