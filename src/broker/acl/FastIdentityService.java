package broker.acl;

import java.security.Principal;

import javax.security.auth.Subject;

import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.server.UserIdentity;

public class FastIdentityService extends DefaultIdentityService {
	
	public static final FastIdentityService DEFAULT = new FastIdentityService(); 
	
	private FastIdentityService() {}

	@Override
	public UserIdentity newUserIdentity(Subject subject, Principal userPrincipal, String[] roles) {
		return FastUserIdentity.of(subject, userPrincipal, roles);
	}
}
