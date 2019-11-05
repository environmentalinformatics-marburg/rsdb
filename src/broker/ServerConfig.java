package broker;

import util.yaml.YamlMap;

public class ServerConfig {
	
	private static final int DEFAULT_PORT = 8081;
	private static final int DEFAULT_SECURE_PORT = 8082;
	private static final boolean DEFAULT_LOGIN = true;
	private static final String DEFAULT_KEYSTORE_PASSWORD = "";
	private static final String DEFAULT_HTTP_AUTHENTICATION = "digest";
	
	public final int port;
	public final String http_authentication;
	public final int secure_port;
	public final boolean login;	
	public final int jws_port;
	public final String keystore_password;
	
	public ServerConfig() {
		this(DEFAULT_PORT, DEFAULT_SECURE_PORT, false, 0, DEFAULT_KEYSTORE_PASSWORD, DEFAULT_HTTP_AUTHENTICATION);
	}
	
	public ServerConfig(int port, int secure_port, boolean login, int jws_port, String keystore_password, String http_authentication) {
		this.port = port;
		this.secure_port = secure_port;
		this.login = login;
		this.jws_port = jws_port;
		this.keystore_password = keystore_password;
		this.http_authentication = http_authentication;
	}
	
	public static ServerConfig ofYAML(YamlMap yamlMap) {
		int port = yamlMap.optInt("port", DEFAULT_PORT);
		int secure_port = yamlMap.optInt("secure_port", DEFAULT_SECURE_PORT);
		boolean login = yamlMap.optBoolean("login", DEFAULT_LOGIN);
		int jws_port = yamlMap.optInt("jws_port", 0);
		String keystore_password = yamlMap.optString("keystore_password", DEFAULT_KEYSTORE_PASSWORD);
		String http_authentication = yamlMap.optString("http_authentication", DEFAULT_HTTP_AUTHENTICATION);
		return new ServerConfig(port, secure_port, login, jws_port, keystore_password, http_authentication);
	}
	
	public boolean useJwsPort() {
		return jws_port > 0;
	}
}
