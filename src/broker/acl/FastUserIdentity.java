package broker.acl;

import java.security.Principal;

import javax.security.auth.Subject;


import org.tinylog.Logger;
import org.eclipse.jetty.server.UserIdentity;

public class FastUserIdentity implements UserIdentity {
	
	private static final String[] NO_ROLES = new String[]{};

	public final Subject subject;
	public final Principal principal;

	public static FastUserIdentity of(Subject subject, Principal principal, String... roles) {
		if(roles == null || roles.length == 0) {
			return new FastUserIdentity(subject, principal);
		} else if (roles.length == 1) {
			return new FastOneRoleUserIdentity(subject, principal, roles[0]);
		} else {
			return new FastRolesUserIdentity(subject, principal, roles);
		}
	}

	public FastUserIdentity(Subject subject, Principal principal) {
		this.subject = subject;
		this.principal = principal;
	}

	@Override
	public Subject getSubject() {
		return subject;
	}

	@Override
	public Principal getUserPrincipal() {
		//Logger.info("getUserPrincipal");
		return principal;
	}

	@Override
	public boolean isUserInRole(String role, Scope scope) {
		return false;
	}

	@Override
	public String toString() {
		String s = "User[";
		s += ']';
		return s;
	}
	
	public String[] getRoles() {
		return NO_ROLES;
	}
}
