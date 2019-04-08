package broker;

import util.yaml.YamlMap;

public class JwsConfig {
	
	private static final String DEFAULT_LINK_TEXT = "-->authenticate by [Provider]<--";
	private static final String DEFAULT_LINK_DESCRIPTION = "(By clicking this link, login infos are received from [Provider].)";
	
	public final String provider_url;
	public final String link_text;
	public final String link_description;
	public final String[] roles;
	
	public final String client_key_id;
	
	// private key format: PKCS#8 (Base64 encoded)
	public final String client_private_key;
	
	public final String provider_key_id;
	
	// public key format: X.509 (Base64 encoded)
	public final String provider_public_key;

	public JwsConfig(String provider_url, String link_text, String link_description, String client_key_id, String client_private_key, String provider_key_id, String provider_public_key, String[] roles) {
		this.provider_url = provider_url;
		this.link_text = link_text;
		this.link_description = link_description;
		this.client_key_id = client_key_id;
		this.client_private_key = client_private_key;
		this.provider_key_id = provider_key_id;
		this.provider_public_key = provider_public_key;
		this.roles = roles;
	}
	
	public static JwsConfig ofYAML(YamlMap yamlMap) {
		String provider_url = yamlMap.getString("provider_url");
		String link_text = yamlMap.optString("link_text", DEFAULT_LINK_TEXT);
		String link_description = yamlMap.optString("link_description", DEFAULT_LINK_DESCRIPTION);
		String client_key_id = yamlMap.optString("client_key_id", null); 
		String client_private_key = yamlMap.getString("client_private_key");
		String provider_key_id = yamlMap.optString("provider_key_id", null);
		String provider_public_key = yamlMap.getString("provider_public_key");
		String[] roles = yamlMap.optList("roles").asStringArray();
		return new JwsConfig(provider_url, link_text, link_description, client_key_id, client_private_key, provider_key_id, provider_public_key, roles);
	}
}
