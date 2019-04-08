package broker.acl;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

public class ArrayACL extends ACL {	
	private final String[] roles;
	
	public ArrayACL(String... roles) {
		this.roles = roles;
	}
	
	@Override
	public boolean isAllowed(UserIdentity userIdentity) {
		if(userIdentity == null || userIdentity.isUserInRole(ACL.ROLE_ADMIN, null)) {
			return true;
		}
		for(String role:roles) {
			if(userIdentity.isUserInRole(role, null)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public Object toYaml() {
		ArrayList<String> list = new ArrayList<String>();
		for(String role:roles) {
				list.add(role);
		}
		return list;
	}

	@Override
	public void writeJSON(JSONWriter json) {
		json.array();
		for(String role:roles) {
				json.value(role);
		}
		json.endArray();		
	}
	
	@Override
	public String toString() {
		String s = "ACL[";
		boolean isFirst = true;
		for(String role:roles) {
			if(isFirst) {
				s += role;
				isFirst = false;
			} else {
				s += ',' + role;
			}
		}
		s += ']';		
		return s;
	}

	@Override
	public void collectRoles(Collection<String> collector) {
		String[] r = roles;
		int len = r.length;
		for (int i = 0; i < len; i++) {
			collector.add(r[i]);
		}
	}
}
