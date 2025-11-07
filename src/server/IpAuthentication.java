package server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.tinylog.Logger;

import broker.Account;
import broker.AccountManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Injects Authentication into Request if IP is in ipMap and user is in loginService.
 * @author woellauer
 *
 */
public class IpAuthentication extends AbstractHandler {
	

	private final AccountManager userStore;
	private Map<String, String> ipMap = new HashMap<String, String>();

	private ConcurrentHashMap<String, Account> userHelperCache = new ConcurrentHashMap<String, Account>();

	/**
	 * Create IpAuthentication handler.
	 * @param userStore with user mapping (live lookup)
	 * @param ipMap (entries are copied. lookup at creation time)
	 */
	IpAuthentication(AccountManager userStore, Map<String, String> ipMap) {
		this.userStore = userStore;
		this.ipMap = new HashMap<String, String>(ipMap);
	}

	@Override
	public void handle(String target, Request request, HttpServletRequest req, HttpServletResponse response) throws IOException, ServletException {
		String ip = request.getRemoteAddr();
		//Logger.info("ip "+ip);
		String user = ipMap.get(ip);
		if(user != null) {
			//Logger.info("user "+user);
			Account account = userStore.getAccount(user);
			if(account == null) {
				Logger.warn("no identiy for user "+user);
				account = userHelperCache.computeIfAbsent(user, name -> {
					return new Account(name, user, new String[0], false, null, null);
				});
			}
			//Logger.info("identity "+userIdentity);
			//Subject subject = userIdentity.getSubject();
			//Logger.info(subject.getPrivateCredentials().iterator().next().getClass());
			//Logger.info(subject.getPublicCredentials());
			//Logger.info(userIdentity.getUserPrincipal().getClass());
			Authentication authentication = new UserAuthentication("IP", account);
			request.setAuthentication(authentication);	
		}		
	}
}
