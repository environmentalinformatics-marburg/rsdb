package broker;

import util.yaml.YamlMap;

public class PostgisConfig {
	
	private static final String DEFAULT_URL = "";
	private static final String DEFAULT_USER = "";
	private static final String DEFAULT_PASSWORD = "";
	
	public final String url;
	public final String user;
	public final String password;
	
	public PostgisConfig() {
		this(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
	}
	
	public PostgisConfig(String url, String user, String password) {
		this.url = url;
		this.user = user;
		this.password = password;
	}
	
	public static PostgisConfig ofYAML(YamlMap yamlMap) {
		String url = yamlMap.optString("url", DEFAULT_URL);
		String user = yamlMap.optString("user", DEFAULT_USER);
		String password = yamlMap.optString("password", DEFAULT_PASSWORD);
		return new PostgisConfig(url, user, password);
	}
	
	public boolean hasUrl() {
		return !url.isBlank();
	}
}
