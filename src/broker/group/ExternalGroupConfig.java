package broker.group;

import broker.Informal;
import broker.acl.ACL;
import broker.acl.EmptyACL;
import util.yaml.YamlMap;

public class ExternalGroupConfig {
	
	public final String name;
	public final Informal informal;
	public final String filename;
	public final ACL acl;
	
	public ExternalGroupConfig(String name, Informal informal, String filename, ACL acl) {
		this.name = name;
		this.informal =  informal;
		this.filename = filename;
		this.acl = acl;
	}
	
	/*public ExternalGroupConfig(String name, Informal informal, String filename) {
		this(name, informal, filename, EmptyACL.ADMIN);
	}*/

	public static ExternalGroupConfig ofYAML(YamlMap yamlMap) {
		String file = yamlMap.getString("file");
		String name = yamlMap.optString("name", file);
		Informal informal = Informal.ofYaml(yamlMap);
		ACL acl = ACL.ofRoles(yamlMap.optList("acl").asStrings());
		return new ExternalGroupConfig(name, informal, file, acl);
	}
}
