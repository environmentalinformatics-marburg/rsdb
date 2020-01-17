package server;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.github.aelstad.keccakj.fips202.SHA3_512;

import broker.Broker;
import server.api.main.APIHandler_identity;
import util.Hex;
import util.Nonce;
import util.TemplateUtil;

public class LoginHandler extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;

	public static byte[] doHash(String username, String password, String salt) {
		byte[] user_bytes = username.getBytes(JWSAuthentication.charset);
		byte[] password_bytes = password.getBytes(JWSAuthentication.charset);
		log.info(password);
		log.info(Arrays.toString(password_bytes));
		byte[] salt_bytes = salt.getBytes(JWSAuthentication.charset);	
		SHA3_512 md = new SHA3_512();
		md.update(salt_bytes);
		md.update(user_bytes);
		md.update(salt_bytes);
		md.update(password_bytes);
		md.update(salt_bytes);
		byte[] digest = md.digest();
		return Hex.bytesToHex(digest).getBytes(JWSAuthentication.charset);
	}

	public static String doUser_hash(String username) {
		byte[] user_bytes = username.getBytes(JWSAuthentication.charset);
		byte[] user_salt_bytes = JWSAuthentication.user_salt.getBytes(JWSAuthentication.charset);	
		SHA3_512 md = new SHA3_512();
		md.update(user_salt_bytes);
		md.update(user_bytes);
		md.update(user_salt_bytes);
		byte[] digest = md.digest();
		String hash = Hex.bytesToHex(digest);
		int hash_size = hash.length();
		return hash.substring(hash_size - JWSAuthentication.user_hash_size, hash_size);
	}

	public boolean validate(String username, String password, String server_nonce, String client_nonce, String client_hash) {
		if(!JWSAuthentication.serverNonceMap.replace(server_nonce, Boolean.TRUE, Boolean.FALSE)) {
			return false;
		}
		log.info("validate username: " + username);
		//log.info("validate password: " + password);
		log.info("validate salt: " + JWSAuthentication.salt);
		log.info("validate server_nonce: " + server_nonce);
		log.info("validate client_nonce: " + client_nonce);
		log.info("validate client_hash: " + client_hash);
		byte[] server_nonce_bytes = server_nonce.getBytes(JWSAuthentication.charset);
		byte[] client_nonce_bytes = client_nonce.getBytes(JWSAuthentication.charset);
		byte[] hash_bytes = doHash(username, password, JWSAuthentication.salt);
		SHA3_512 md = new SHA3_512();
		md.update(server_nonce_bytes);
		md.update(client_nonce_bytes);
		md.update(hash_bytes);
		md.update(client_nonce_bytes);
		md.update(server_nonce_bytes);
		String server_hash = Hex.bytesToHex(md.digest());
		if(server_hash.equals(client_hash)) {
			log.info("valid: " + username);
			return true;
		}
		return false;		
	}

	public LoginHandler(Broker broker) {		
		this.broker = broker;
	}

	private static class LoginException extends RuntimeException {
		private static final long serialVersionUID = 8687116037426881282L;
		public LoginException(String message) {
			super(message);
		}
	}

	private UserIdentity getUser(String user_hash, String server_nonce, String client_nonce, String client_hash) {
		Map<String, UserIdentity> userMap = broker.getUserStore().getKnownUserIdentities();
		for(Entry<String, UserIdentity> e:userMap.entrySet()) {
			String username = e.getKey();
			String username_hash = doUser_hash(username);
			log.info(username_hash + "  " + username + "  " + user_hash);
			if(username_hash.equals(user_hash)) {
				log.info(username_hash + "  " + username + "  " + user_hash + "  OK");
				UserIdentity identity = e.getValue();
				String password;
				try {
					Set<Object> cp = identity.getSubject().getPrivateCredentials();
					log.info(cp.size());
					log.info(cp.iterator().next());
					Object passwordObject = cp.iterator().next();
					Field f = passwordObject.getClass().getDeclaredField("_pw"); //NoSuchFieldException
					f.setAccessible(true);
					password = (String) f.get(passwordObject); //IllegalAccessException
					if(validate(username, password, server_nonce, client_nonce, client_hash)) {
						log.info(username_hash + "  " + username + "  " + user_hash + "  VALID");
						return identity;
					}
				} catch(Exception e1) {
					log.warn(e1);
				}
			}
		}
		return null;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {		
		try {
			response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
			String user_hash = baseRequest.getParameter("user");
			if(user_hash == null || user_hash.trim().isEmpty()) {
				throw new LoginException("missing user");
			}
			String server_nonce = baseRequest.getParameter("server_nonce");
			if(!Nonce.isValid(server_nonce, 8)) {
				throw new LoginException("missing server_nonce");
			}
			String client_nonce = baseRequest.getParameter("client_nonce");
			if(!Nonce.isValid(client_nonce, 8)) {
				throw new LoginException("missing client_nonce");
			}
			String client_hash = baseRequest.getParameter("hash");
			if(!Hex.isValid(client_hash, 128)) {
				throw new LoginException("missing hash");
			}
			
			UserIdentity identity = getUser(user_hash, server_nonce, client_nonce, client_hash);
			if(identity == null) {
				HashMap<String, Object> ctx = new HashMap<>();
				ctx.put("error", "wrong user / password");
				TemplateUtil.getTemplate("login_local_error.mustache", true).execute(ctx, response.getWriter());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				baseRequest.setHandled(true);
				return;
			}

			String[] roles = APIHandler_identity.getRoles(identity);

			HttpSession session = request.getSession(false);
			if(session != null) {
				session.invalidate();
			}
			session = request.getSession(true);	

			Subject subject = new Subject();
			Principal principal = new AbstractLoginService.UserPrincipal(identity.getUserPrincipal().getName(), null);
			UserIdentity userIdentity = new DefaultUserIdentity(subject, principal, roles);
			Authentication authentication = new UserAuthentication("local_login", userIdentity);
			session.setAttribute("authentication", authentication);
			
			String loc = "/entrypoint";
			String ref = request.getParameter("ref");
			log.info("ref " + ref);
			if(ref != null) {
				loc = ref;
			}

			response.setHeader(HttpHeader.LOCATION.asString(), loc);
			response.setStatus(HttpServletResponse.SC_FOUND);
			response.setContentLength(0);
			baseRequest.setHandled(true);
			return;
		} catch(LoginException e) {
			log.warn(e);
			baseRequest.setHandled(true);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			HashMap<String, Object> ctx = new HashMap<>();
			ctx.put("error", e.getMessage());
			TemplateUtil.getTemplate("login_local_error.mustache", true).execute(ctx, response.getWriter());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

}
