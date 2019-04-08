package broker.acl;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

public class EmptyACL extends ACL {
	public static final EmptyACL ADMIN = new EmptyACL();
	
	private EmptyACL() {}
	
	@Override
	public boolean isAllowed(UserIdentity userIdentity) {
		return userIdentity == null || userIdentity.isUserInRole(ACL.ROLE_ADMIN, null);
	}
	
	@Override
	public Object toYaml() {
		return new ArrayList<String>();
	}

	@Override
	public void writeJSON(JSONWriter json) {
		json.array();
		json.endArray();		
	}

	@Override
	public String toString() {
		return "ACL[]";
	}

	@Override
	public void collectRoles(Collection<String> collector) {
		// nothing		
	}
}
