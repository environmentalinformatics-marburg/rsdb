package server;

import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

import broker.Broker;
import broker.JwsConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolver;
import util.TemplateUtil;
import util.collections.vec.Vec;

/**
 * Injects Authentication by JWS.
 * @author woellauer
 *
 */
public class JWSAuthentication extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();

	private static final boolean ALWAYS_REFRESH_MUSTACHE = true;





	private final Broker broker;

	private final static int clock_skew = 60;

	// algorithm: ES512 (ECDSA using P-521 and SHA-512) (a standard JWS algorithm)
	static final SignatureAlgorithm ALGORITHM = SignatureAlgorithm.ES512;
	static KeyFactory KEY_FACTORY = getKeyFactory("EC");

	// private key format: PKCS#8 (Base64 encoded)

	// public key format: X.509 (Base64 encoded)

	private static KeyFactory getKeyFactory(String name) {
		try {
			return KeyFactory.getInstance(name);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static PrivateKey stringToPrivateKey(String s) {
		byte[] bytes = Base64.getDecoder().decode(s);		
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
		try {
			return KEY_FACTORY.generatePrivate(spec);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}

	private static PublicKey stringToPublicKey(String s) {
		byte[] bytes = Base64.getDecoder().decode(s);	
		X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
		try {
			return KEY_FACTORY.generatePublic(spec);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}

	//private final UserStore userStore;

	private SigningKeyResolver signingKeyResolver = new SigningKeyResolver() {

		@SuppressWarnings("rawtypes")
		@Override
		public Key resolveSigningKey(JwsHeader header, String plaintext) {
			throw new RuntimeException("not implemented");
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Key resolveSigningKey(JwsHeader header, Claims claims) {
			return stringToPublicKey(getKey(header.getKeyId()));
		}

		private String getKey(String keyID) {
			if(keyID == null) { // set first key from config
				return broker.brokerConfig.jws().first().provider_public_key;
			}
			for(JwsConfig jwsConfig : broker.brokerConfig.jws()) {
				if(keyID.equals(jwsConfig.provider_key_id)) {
					return jwsConfig.provider_public_key;
				}
			}
			throw new RuntimeException("keyID not found");
		}
	};

	/**
	 * Create IpAuthentication handler.
	 * @param userStore with user mapping (live lookup)
	 * @param ipMap (entries are copied. lookup at creation time)
	 */
	public JWSAuthentication(Broker broker) {
		this.broker = broker;
	}

	@Override
	public void handle(String target, Request request, HttpServletRequest req_, HttpServletResponse response) throws IOException, ServletException {
		if(request.getAttribute("JWS") != null) {
			String JWSParam = request.getParameter("JWS");
			if(JWSParam != null) {
				handleJwsParameterAPI(JWSParam, request, response);
				return;
			}

			String jwsParam = request.getParameter("jws");
			if(jwsParam != null) {
				handleJwsParameterRedirect(jwsParam, request, response);
				return;
			}

			HttpSession session = request.getSession(false);
			if(session != null) {
				Authentication authentication = (Authentication) session.getAttribute("authentication");
				if(authentication == null) {
					throw new RuntimeException("missing authentication");
				}
				request.setAuthentication(authentication);
				return;
			}

			hanndleAuthenticationRequired(request, response);
		}
	}

	public static ConcurrentHashMap<String, Boolean> serverNonceMap = new ConcurrentHashMap<String, Boolean>();

	public static String createServerNonce() {
		String nonce = util.Nonce.get(8);
		while(serverNonceMap.putIfAbsent(nonce, Boolean.TRUE) != null) {
			nonce = util.Nonce.get(8);
		}
		return nonce;
	}

	private void hanndleAuthenticationRequired(Request request, HttpServletResponse response) throws IOException {
		String reqUrl = request.getRequestURL().toString();
		String reqUrlQs = request.getQueryString();
		log.info("reqUrlQs " + reqUrlQs);
		String req = reqUrl;
		if(reqUrlQs != null) {
			req += '?' + reqUrlQs;
		}

		request.setHandled(true);
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		
		String server_nonce = createServerNonce();

		String auth_req = "login_sha2_512";
		auth_req += " server_nonce=\"" + server_nonce + "\"";
		auth_req += ", user_hash_size=\"" + LoginHandler.user_hash_size + "\"";
		auth_req += ", user_salt=\"" + LoginHandler.user_salt + "\"";
		auth_req += ", salt=\"" + LoginHandler.salt + "\"";

		
		response.setHeader("WWW-Authenticate", auth_req);
		response.setContentType("text/html;charset=utf-8");

		HashMap<String, Object> ctx = new HashMap<>();
		ctx.put("server_nonce", server_nonce);
		ctx.put("user_hash_size", LoginHandler.user_hash_size);
		ctx.put("user_salt", LoginHandler.user_salt);
		ctx.put("salt", LoginHandler.salt);
		Vec<Map<String, Object>> jwsList = new Vec<Map<String, Object>>();
		for(JwsConfig jwsConfig:broker.brokerConfig.jws()) {
			String clientJws = Jwts.builder()
					.setPayload(req)
					.setHeaderParam(JwsHeader.KEY_ID, jwsConfig.client_key_id)
					.signWith(stringToPrivateKey(jwsConfig.client_private_key))
					.compact();			
			
			/*String testReq = "http://example.com";
			String clientTextJws = Jwts.builder()
					.setPayload(testReq)
					.setHeaderParam(JwsHeader.KEY_ID, jwsConfig.client_key_id)
					.signWith(stringToPrivateKey(jwsConfig.client_private_key))
					.compact();			
			System.out.println("++clientTextJws++");
			System.out.println(clientTextJws);
			System.out.println("--clientTextJws--");*/			
			
			String redirect_target = jwsConfig.provider_url + "?jws="+clientJws;

			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("redirect_target", redirect_target);
			map.put("link_text", jwsConfig.link_text);
			map.put("link_description", jwsConfig.link_description);
			jwsList.add(map);
		}
		ctx.put("jws", jwsList);
		TemplateUtil.getTemplate("login.mustache", true).execute(ctx, response.getWriter());
	}

	private void handleJwsParameterRedirect(String compactJws, Request request, HttpServletResponse response) throws IOException {
		log.info("handleJwsParameterRedirect: " + compactJws);
		request.setHandled(true);
		String redirect_target = request.getRequestURL().toString();
		try {
			String qs = request.getQueryString();
			log.info("qs " + qs);
			int jwsIndex = qs.indexOf("jws=");
			if(jwsIndex < 0) {
				throw new RuntimeException("url JWS error");
			}
			if(jwsIndex > 0) {
				redirect_target += "?" + qs.substring(0, jwsIndex - 1);
			}	

			Jws<Claims> jws = Jwts.parser().setSigningKeyResolver(signingKeyResolver).setAllowedClockSkewSeconds(clock_skew).parseClaimsJws(compactJws);

			HttpSession session = request.getSession(false);
			if(session != null) {
				session.invalidate();
			}
			session = request.getSession(true);			

			session.setAttribute("jws", compactJws);
			Claims claims = jws.getBody();
			String userName = claims.getSubject();
			JwsConfig jwsConfig = broker.brokerConfig.jws().first();
			Subject subject = new Subject();
			Principal principal = new AbstractLoginService.UserPrincipal(userName, null);
			UserIdentity userIdentity = new DefaultUserIdentity(subject, principal, jwsConfig.roles);
			Authentication authentication = new UserAuthentication("jws", userIdentity);
			session.setAttribute("authentication", authentication);

			response.setHeader(HttpHeader.LOCATION.asString(), redirect_target);
			response.setStatus(HttpServletResponse.SC_FOUND);
			response.setContentLength(0);
			return;
		} catch (Exception e) {
			if(compactJws != null && compactJws.equals("logout")) {
				HttpSession session = request.getSession(false);
				if(session != null) {
					session.invalidate();
				}
				response.setHeader(HttpHeader.LOCATION.asString(), redirect_target);
				response.setStatus(HttpServletResponse.SC_FOUND);
				response.setContentLength(0);
				return;
			} else {
				e.printStackTrace();
				log.warn(e);
				HttpSession session = request.getSession(false);
				if(session != null) {
					session.invalidate();
				}
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setContentType("text/html;charset=utf-8");
				HashMap<String, Object> ctx = new HashMap<>();
				ctx.put("error", e.getMessage());
				ctx.put("redirect_target", redirect_target);
				TemplateUtil.getTemplate("jws_error.mustache", ALWAYS_REFRESH_MUSTACHE).execute(ctx, response.getWriter());
				return;
			}
		}			
	}

	private boolean handleJwsParameterAPI(String jwsParam, Request request, HttpServletResponse response) throws IOException {
		try {
			Jws<Claims> jws = Jwts.parser().setSigningKeyResolver(signingKeyResolver).setAllowedClockSkewSeconds(clock_skew).parseClaimsJws(jwsParam);
			Claims claims = jws.getBody();
			String userName = claims.getSubject();

			JwsConfig jwsConfig = broker.brokerConfig.jws().first();

			Subject subject = new Subject();
			Principal principal = new AbstractLoginService.UserPrincipal(userName, null);
			UserIdentity userIdentity = new DefaultUserIdentity(subject, principal, jwsConfig.roles);
			Authentication authentication = new UserAuthentication("jws", userIdentity);
			request.setAuthentication(authentication);
		} catch (Exception e) {			
			HashMap<String, Object> ctx = new HashMap<>();
			ctx.put("error", e.getMessage());
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("text/html;charset=utf-8");
			TemplateUtil.getTemplate("api_jws_error.mustache", ALWAYS_REFRESH_MUSTACHE).execute(ctx, response.getWriter());
			request.setHandled(true);
		}
		return true;

	}
}
