package broker.acl;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

public class OneACL extends ACL {

	private final String role;

	public OneACL(String role) {
		this.role = role;
	}

	@Override
	public boolean isAllowed(UserIdentity userIdentity) {
		return userIdentity == null 
				|| userIdentity.isUserInRole(ACL.ROLE_ADMIN, null)
				|| userIdentity.getUserPrincipal().getName().equals(role)
				|| userIdentity.isUserInRole(role, null);
	}

	@Override
	public Object toYaml() {
		ArrayList<String> list = new ArrayList<String>(1);
		list.add(role);
		return list;
	}

	@Override
	public void writeJSON(JSONWriter json) {
		json.array();
		json.value(role);
		json.endArray();
	}

	@Override
	public String toString() {
		return "ACL[" + role + "]";
	}

	@Override
	public void collectRoles(Collection<String> collector) {
		collector.add(role);		
	}
}
