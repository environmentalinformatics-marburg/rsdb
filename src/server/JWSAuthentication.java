package server;

import java.io.IOException;
import java.io.PrintWriter;
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

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
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
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolver;

/**
 * Injects Authentication into Request if IP is in ipMap and user is in loginService.
 * @author woellauer
 *
 */
public class JWSAuthentication extends AbstractHandler {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;

	private final static int clock_skew = 60;

	// algorithm: ES512 (ECDSA using P-521 and SHA-512) (a standard JWS algorithm)
	static final SignatureAlgorithm ALGORITHM = SignatureAlgorithm.ES512;
	static KeyFactory KEY_FACTORY = getKeyFactory("EC");

	// private key format: PKCS#8 (Base64 encoded)

	// public key format: X.509 (Base64 encoded)

	public static KeyFactory getKeyFactory(String name) {
		try {
			return KeyFactory.getInstance(name);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static PrivateKey stringToPrivateKey(String s) {
		byte[] bytes = Base64.getDecoder().decode(s);		
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
		try {
			return KEY_FACTORY.generatePrivate(spec);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}

	public static PublicKey stringToPublicKey(String s) {
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
		
		@Override
		public Key resolveSigningKey(JwsHeader header, String plaintext) {
			throw new RuntimeException("not implemented");
		}
		
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
	JWSAuthentication(Broker broker) {
		this.broker = broker;
		//this.userStore = broker.getUserStore();
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

			/*Cookie jwsCooky = getCooky(request, "jws");
			if(jwsCooky != null) {
				log.info("handle jws cooky");
				handleCooky(jwsCooky, request, response);
				return;
			}*/

			hanndleAuthenticationRequired(request, response);
		}
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
		response.setContentType("text/html;charset=utf-8");
		PrintWriter out = response.getWriter();
		out.println("<html>");
		out.println("<head>");
		out.println("<meta name=\"robots\" content=\"noindex, nofollow\" />");
		out.println("<title>Authentication Required</title>");
		out.println("</head>");
		out.println("<body style=\"text-align: center;\">");
		out.println("<h1>Authentication Required</h1>");
		out.println("<hr>");

		for(JwsConfig jwsConfig:broker.brokerConfig.jws()) {
			String clientJws = Jwts.builder()
					.setPayload(req)
					.setHeaderParam(JwsHeader.KEY_ID, jwsConfig.client_key_id)
					.signWith(stringToPrivateKey(jwsConfig.client_private_key))
					.compact();
			
			String redirect_target = jwsConfig.provider_url + "?jws="+clientJws;
			out.println("<a href=\""+ redirect_target + "\">" + jwsConfig.link_text + "</a> " + jwsConfig.link_description);
			out.println("<br>");
			out.println("<br>");
		}

		out.println("<br>");
		out.println("<br>");
		out.println("<br>");
		out.println("<br>");
		out.println("<hr>");
		out.println("<i>By login action you agree to store identifiing cookies in your browser.</i>");
		out.println("</body>");
		out.println("</html>");
	}

	public void handleJwsParameterRedirect(String compactJws, Request request, HttpServletResponse response) throws IOException {
		log.info("handleJwsParameterRedirect: " + compactJws);
		request.setHandled(true);

		String redirect_target = request.getRequestURL().toString();
		String qs = request.getQueryString();
		log.info("qs " + qs);
		int jwsIndex = qs.indexOf("jws=");
		if(jwsIndex < 0) {
			throw new RuntimeException("url JWS error");
		}
		if(jwsIndex > 0) {
			redirect_target += "?" + qs.substring(0, jwsIndex - 1);
		}	

		try {
			Jws<Claims> jws = Jwts.parser().setSigningKeyResolver(signingKeyResolver).setAllowedClockSkewSeconds(clock_skew).parseClaimsJws(compactJws);
			/*Cookie cookie = new Cookie("jws", compactJws);
			cookie.setPath(COOKIE_PATH);
			cookie.setHttpOnly(true);
			//cookie.setSecure(true);
			response.addCookie(cookie);*/

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
				/*Cookie cookie = new Cookie("jws", null);
				cookie.setPath(COOKIE_PATH);
				cookie.setMaxAge(0);
				response.addCookie(cookie);*/
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
				/*Cookie cookie = new Cookie("jws", null);
				cookie.setPath(COOKIE_PATH);
				cookie.setMaxAge(0);
				response.addCookie(cookie);*/
				HttpSession session = request.getSession(false);
				if(session != null) {
					session.invalidate();
				}
				response.setContentType("text/html;charset=utf-8");
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				PrintWriter out = response.getWriter();
				out.println("<html>");
				out.println("<head>");
				out.println("<meta name=\"robots\" content=\"noindex, nofollow\" />");
				out.println("<title>User Authentication Faild</title>");
				out.println("</head>");
				out.println("<body>");
				out.println("<h1>User Authentication Faild</h1>");
				out.println("<a href=\""+ redirect_target + "\">try again</a>");
				out.println("<br><br><hr>");
				out.println("Reason:<br><br>");
				out.println("<i>" + e.getMessage() + "</i>");
				out.println("</body>");
				out.println("</html>");
				return;
			}
		}			
	}

	public Cookie getCooky(Request request, String name) {
		Cookie[] cookies = request.getCookies();
		if(cookies == null) {
			return null;
		}
		for(Cookie cooky : cookies) {
			if(cooky.getName().equals(name)) {
				return cooky;
			}
		}
		return null;
	}

	public boolean handleJwsParameterAPI(String jwsParam, Request request, HttpServletResponse response) throws IOException {
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
		} catch (JwtException e) {
			PrintWriter out = response.getWriter();
			out.println("<html>");
			out.println("<head>");
			out.println("<meta name=\"robots\" content=\"noindex, nofollow\" />");
			out.println("<title>User Authentication Faild</title>");
			out.println("</head>");
			out.println("<body>");
			out.println("<h1>User Authentication Faild</h1>");
			out.println("<br><br><hr>");
			out.println("Reason:<br><br>");
			out.println("<i>" + e.getMessage() + "</i>");
			out.println("</body>");
			out.println("</html>");
			request.setHandled(true);
		}
		return true;

	}

	public boolean handleCooky(Cookie jwsCooky, Request request, HttpServletResponse response) throws IOException {
		String compactJws = jwsCooky.getValue();
		try {
			Jws<Claims> jws = Jwts.parser().setSigningKeyResolver(signingKeyResolver).setAllowedClockSkewSeconds(clock_skew).parseClaimsJws(compactJws);
			Claims claims = jws.getBody();
			String userName = claims.getSubject();
			
			JwsConfig jwsConfig = broker.brokerConfig.jws().first();

			Subject subject = new Subject();
			Principal principal = new AbstractLoginService.UserPrincipal(userName, null);
			UserIdentity userIdentity = new DefaultUserIdentity(subject, principal, jwsConfig.roles);
			Authentication authentication = new UserAuthentication("jws", userIdentity);
			request.setAuthentication(authentication);
		} catch (JwtException e) {
			Cookie cookie = new Cookie("jws", null);
			cookie.setPath(COOKIE_PATH);
			cookie.setMaxAge(0);
			response.addCookie(cookie);
			response.setContentType("text/html;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			PrintWriter out = response.getWriter();
			out.println("<html>");
			out.println("<head>");
			out.println("<meta name=\"robots\" content=\"noindex, nofollow\" />");
			out.println("<title>User Authentication Faild</title>");
			out.println("</head>");
			out.println("<body>");
			out.println("<h1>User Authentication Faild</h1>");
			out.println("<a href=\""+ request.getRequestURL().toString() + "\">try again</a>");
			out.println("<br><br><hr>");
			out.println("Reason:<br><br>");
			out.println("<i>" + e.getMessage() + "</i>");
			out.println("</body>");
			out.println("</html>");
			request.setHandled(true);
		}
		return true;

	}

	private static final String COOKIE_PATH = "/";
}
