package server;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.AbstractHandler;

import broker.acl.FastUserIdentity;

/**
 * Injects Authentication into Request if IP is in ipMap and user is in loginService.
 * @author woellauer
 *
 */
public class IpAuthentication extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();

	private final UserStore userStore;
	private Map<String, String> ipMap = new HashMap<String, String>();

	private ConcurrentHashMap<String, FastUserIdentity> userHelperCache = new ConcurrentHashMap<String, FastUserIdentity>();

	/**
	 * Create IpAuthentication handler.
	 * @param userStore with user mapping (live lookup)
	 * @param ipMap (entries are copied. lookup at creation time)
	 */
	IpAuthentication(UserStore userStore, Map<String, String> ipMap) {
		this.userStore = userStore;
		this.ipMap = new HashMap<String, String>(ipMap);
	}

	@Override
	public void handle(String target, Request request, HttpServletRequest req, HttpServletResponse response) throws IOException, ServletException {



		String ip = request.getRemoteAddr();
		//log.info("ip "+ip);
		String user = ipMap.get(ip);
		if(user != null) {
			//log.info("user "+user);
			UserIdentity userIdentity = userStore.getUserIdentity(user);
			if(userIdentity == null) {
				log.warn("no identiy for user "+user);
				userIdentity = userHelperCache.computeIfAbsent(user, name -> {
					Principal principal = new AbstractLoginService.UserPrincipal(name, null);
					Subject subject = new Subject();
					subject.getPrincipals().add(principal);
					return new FastUserIdentity(subject, principal);
				});
			}
			//log.info("identity "+userIdentity);
			//Subject subject = userIdentity.getSubject();
			//log.info(subject.getPrivateCredentials().iterator().next().getClass());
			//log.info(subject.getPublicCredentials());
			//log.info(userIdentity.getUserPrincipal().getClass());
			Authentication authentication = new UserAuthentication("IP", userIdentity);
			request.setAuthentication(authentication);	
		}		
	}
}
