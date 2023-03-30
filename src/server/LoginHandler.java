package server;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.security.auth.Subject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


import org.tinylog.Logger;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.AbstractHandler;

import broker.Account;
import broker.Broker;
import server.api.main.APIHandler_identity;
import util.Hex;
import util.Nonce;
import util.TemplateUtil;
import util.Web;

public class LoginHandler extends AbstractHandler {
	

	public static final String salt = Nonce.get(8);

	public static final String user_salt = Nonce.get(8);

	public static final int user_hash_size = 1;

	public static final Charset charset = StandardCharsets.UTF_8;

	private final Broker broker;

	private static MessageDigest getHasher() {
		/*SHA3_512 md = new SHA3_512(); // get SHA-3 512
		return md;*/
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512"); // get SHA-2 512
			return md;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] doHash(String username, String password, String salt) {
		byte[] user_bytes = username.getBytes(charset);
		byte[] password_bytes = password.getBytes(charset);
		Logger.info(password);
		Logger.info(Arrays.toString(password_bytes));
		byte[] salt_bytes = salt.getBytes(charset);	
		MessageDigest md = getHasher();
		md.update(salt_bytes);
		md.update(user_bytes);
		md.update(salt_bytes);
		md.update(password_bytes);
		md.update(salt_bytes);
		byte[] digest = md.digest();
		return Hex.bytesToHex(digest).getBytes(charset);
	}

	public static String doUser_hash(String username) {
		{
			MessageDigest md = getHasher();
			md.update(new byte[] {});
			byte[] digest = md.digest();
			Logger.info("self check " + Hex.bytesToHex(digest)); 
		}
		byte[] user_bytes = username.getBytes(charset);
		byte[] user_salt_bytes = user_salt.getBytes(charset);	
		MessageDigest md = getHasher();
		md.update(user_salt_bytes);
		md.update(user_bytes);
		md.update(user_salt_bytes);
		byte[] digest = md.digest();
		String hash = Hex.bytesToHex(digest);
		Logger.info(user_salt + username + user_salt);
		Logger.info(username + "   user_hash " + hash + "   salt " + user_salt);
		int hash_size = hash.length();
		return hash.substring(hash_size - user_hash_size, hash_size);
	}

	public boolean validate(String username, String password, String server_nonce, String client_nonce, String client_hash) {
		if(!JWSAuthentication.serverNonceMap.replace(server_nonce, Boolean.TRUE, Boolean.FALSE)) {
			return false;
		}
		Logger.info("validate username: " + username);
		//Logger.info("validate password: " + password);
		Logger.info("validate salt: " + salt);
		Logger.info("validate server_nonce: " + server_nonce);
		Logger.info("validate client_nonce: " + client_nonce);
		Logger.info("validate client_hash: " + client_hash);
		byte[] server_nonce_bytes = server_nonce.getBytes(charset);
		byte[] client_nonce_bytes = client_nonce.getBytes(charset);
		byte[] hash_bytes = doHash(username, password, salt);
		String inner_hash = Hex.bytesToHex(hash_bytes);
		Logger.info(username + "  inner_hash  " + inner_hash);
		MessageDigest md = getHasher();
		md.update(server_nonce_bytes);
		md.update(client_nonce_bytes);
		md.update(hash_bytes);
		md.update(client_nonce_bytes);
		md.update(server_nonce_bytes);
		String server_hash = Hex.bytesToHex(md.digest());
		if(server_hash.equals(client_hash)) {
			Logger.info("valid: " + username);
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

	private Account getAccount(String user_hash, String server_nonce, String client_nonce, String client_hash) {
		Account[] validAccount = new Account[1];
		broker.accountManager().foreachAccount(account -> {
			if(validAccount[0] == null) {
				String username = account.user;
				String username_hash = doUser_hash(username);
				Logger.info(username + "    " + username_hash + " cmp " + user_hash+ "    user_salt " + user_salt);
				if(username_hash.equals(user_hash)) {
					Logger.info(username + "    " + username_hash + " cmp " + user_hash + "   OK");
					String password = account.password;					
					if(validate(username, password, server_nonce, client_nonce, client_hash)) {
						Logger.info(username_hash + "  " + username + "  " + user_hash + "  VALID");
						validAccount[0] = account;
					}
				}
			}
		});
		return validAccount[0];
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String loc = "/entrypoint";
		String ref = request.getParameter("ref");
		Logger.info("ref " + ref);
		if(ref != null) {
			loc = ref;
		}		
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

			Account account = getAccount(user_hash, server_nonce, client_nonce, client_hash);
			if(account == null) {
				Logger.warn("missing identity");
				baseRequest.setHandled(true);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				HashMap<String, Object> ctx = new HashMap<>();
				ctx.put("ref", loc);
				ctx.put("error", "wrong user / password");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.setContentType(Web.MIME_HTML);
				TemplateUtil.getTemplate("login_error.mustache", true).execute(ctx, response.getWriter());
				baseRequest.setHandled(true);
				return;
			}

			HttpSession session = request.getSession(false);
			if(session != null) {
				session.invalidate();
			}
			session = request.getSession(true);	
			Authentication authentication = new UserAuthentication("login", account);
			session.setAttribute("authentication", authentication);
			response.setHeader(HttpHeader.LOCATION.asString(), loc);
			response.setStatus(HttpServletResponse.SC_FOUND);
			response.setContentLength(0);
			baseRequest.setHandled(true);
			return;
		} catch(LoginException e) {
			Logger.warn(e);
			baseRequest.setHandled(true);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			HashMap<String, Object> ctx = new HashMap<>();
			ctx.put("ref", loc);
			ctx.put("error", e.getMessage());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType(Web.MIME_HTML);
			TemplateUtil.getTemplate("login_error.mustache", true).execute(ctx, response.getWriter());
		}
	}

}
