package broker.acl;

import java.security.Principal;
import java.util.Arrays;

import javax.security.auth.Subject;

public class FastRolesUserIdentity extends FastUserIdentity {

	private final String[] roles;

	public FastRolesUserIdentity(Subject subject, Principal principal, String[] roles) {
		super(subject, principal);
		this.roles = roles;
	}

	@Override
	public boolean isUserInRole(String role, Scope scope) {
		for (String r : roles)
			if (role.equals(r)) {
				return true;
			}
		return false;
	}

	@Override
	public String toString() {
		String s = "User[";
		boolean isFirst = true;
		for (String role : roles) {
			if (isFirst) {
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
	public String[] getRoles() {
		return Arrays.copyOf(roles, roles.length);
	}
}
