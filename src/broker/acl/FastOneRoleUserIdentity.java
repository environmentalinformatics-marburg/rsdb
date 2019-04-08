package broker.acl;

import java.security.Principal;

import javax.security.auth.Subject;

public class FastOneRoleUserIdentity extends FastUserIdentity {

	private final String role;
	
	public FastOneRoleUserIdentity(Subject subject, Principal principal, String role) {
		super(subject, principal);
		this.role = role;
	}

	@Override
	public boolean isUserInRole(String role, Scope scope) {
		return role.equals(this.role);
	}
	
	@Override
	public String toString() {
		return "User " + principal + " [" + role + "]";
	}
	
	@Override
	public String[] getRoles() {
		return new String[]{role};
	}
}
