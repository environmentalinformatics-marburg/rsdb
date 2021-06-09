package server;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.URIUtil;

/**
 * 
 * derived from: org.eclipse.jetty.server.handler.SecuredRedirectHandler;
 * 
 * If there is no secure port configured, HTTP is passed and not redirected to HTTPS.
 *
 */
public class OptionalSecuredRedirectHandler extends AbstractHandler {
	static final Logger log = LogManager.getLogger();
	
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		//log.info("handle");
		//new RuntimeException().printStackTrace();
		//if (baseRequest.isSecure() || (channel == null)) { // changed
		if (baseRequest.isSecure()) { // changed
			// nothing to do
			return;
		}

		HttpChannel channel = baseRequest.getHttpChannel(); // changed
		if (channel == null) {// changed
			response.sendError(HttpStatus.FORBIDDEN_403,"No http cannel available");// changed
			baseRequest.setHandled(true);  // changed
			return;// changed
		}// changed

		HttpConfiguration httpConfig = channel.getHttpConfiguration();
		if (httpConfig == null) {
			// no config, show error
			response.sendError(HttpStatus.FORBIDDEN_403,"No http configuration available");
			baseRequest.setHandled(true);  // changed
			return;
		}

		if (httpConfig.getSecurePort() > 0) {
			String scheme = httpConfig.getSecureScheme();
			int port = httpConfig.getSecurePort();

			String url = URIUtil.newURI(scheme,baseRequest.getServerName(),port,baseRequest.getRequestURI(),baseRequest.getQueryString());
			response.setContentLength(0);
			response.sendRedirect(url);
		}
		else {
			//response.sendError(HttpStatus.FORBIDDEN_403,"Not Secure"); // changed
			return; // changed
		}

		baseRequest.setHandled(true);
	}
}

