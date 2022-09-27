package broker.acl;

import java.security.Principal;
import java.util.LinkedHashSet;

import org.eclipse.jetty.server.UserIdentity;

import util.CarpException;

public class AclUtil {
	
	public static boolean isAllowed(UserIdentity userIdentity) {
		return userIdentity == null || userIdentity.isUserInRole(ACL.ROLE_ADMIN, null);		
	}
	
	public static boolean isAllowed(ACL acl1, UserIdentity userIdentity) {
		return acl1.isAllowed(userIdentity);		
	}
	
	public static boolean isAllowed(ACL acl1, ACL acl2, UserIdentity userIdentity) {
		return acl1.isAllowed(userIdentity) || acl2.isAllowed(userIdentity);		
	}
	
	public static boolean isAllowed(ACL acl1, ACL acl2, ACL acl3, UserIdentity userIdentity) {
		return acl1.isAllowed(userIdentity) || acl2.isAllowed(userIdentity)|| acl3.isAllowed(userIdentity);		
	}
	
	public static void check(UserIdentity userIdentity, String location) {
		if (!isAllowed(userIdentity)) {
			Principal principal = userIdentity.getUserPrincipal();
			if(principal != null) {
				String name = principal.getName();
				throw new CarpException("not allowed for account: " + (name != null ? name : userIdentity) + " at " + location);
			} else {
				throw new CarpException("not allowed for: " + userIdentity + " at " + location);
			}
		}
	}
	
	public static void check(ACL acl1, UserIdentity userIdentity, String location) {
		if (!isAllowed(acl1, userIdentity)) {
			Principal principal = userIdentity.getUserPrincipal();
			if(principal != null) {
				String name = principal.getName();
				throw new CarpException("not allowed for account: " + (name != null ? name : userIdentity) + " at " + location);
			} else {
				throw new CarpException("not allowed for: " + userIdentity + " at " + location);
			}
		}
	}
	
	public static void check(ACL acl1, ACL acl2, UserIdentity userIdentity, String location) {
		if (!isAllowed(acl1, acl2, userIdentity)) {
			Principal principal = userIdentity.getUserPrincipal();
			if(principal != null) {
				String name = principal.getName();
				throw new CarpException("not allowed for account: " + (name != null ? name : userIdentity) + " at " + location);
			} else {
				throw new CarpException("not allowed for: " + userIdentity + " at " + location);
			}
		}
	}
	
	public static void check(ACL acl1, ACL acl2, ACL acl3, UserIdentity userIdentity, String location) {
		if (!isAllowed(acl1, acl2, acl3, userIdentity)) {
			Principal principal = userIdentity.getUserPrincipal();
			if(principal != null) {
				String name = principal.getName();
				throw new CarpException("not allowed for account: " + (name != null ? name : userIdentity) + " at " + location);
			} else {
				throw new CarpException("not allowed for: " + userIdentity + " at " + location);
			}
		}
	}

	public static ACL union(ACL acl1, ACL acl2) {
		LinkedHashSet<String> roles = new LinkedHashSet<String>();
		acl1.collectRoles(roles);
		acl2.collectRoles(roles);
		return ACL.ofRoles(roles);
	}
	
	public static ACL union(ACL acl1, ACL acl2, ACL acl3) {
		LinkedHashSet<String> roles = new LinkedHashSet<String>();
		acl1.collectRoles(roles);
		acl2.collectRoles(roles);
		acl3.collectRoles(roles);
		return ACL.ofRoles(roles);
	}
}
