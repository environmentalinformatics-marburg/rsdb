package broker.acl;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

import util.CarpException;

public abstract class ACL {
	//private static final Logger log = LogManager.getLogger();

	public static final String ROLE_ADMIN = "admin";

	protected ACL() {
	}

	public abstract Object toYaml();

	public abstract void writeJSON(JSONWriter json);
	
	public abstract void collectRoles(Collection<String> collector);

	/**
	 * 
	 * @param userIdentity
	 *            == null --> allowed
	 * @return
	 */
	public abstract boolean isAllowed(UserIdentity userIdentity);

	/**
	 * 
	 * @param userIdentity
	 *            == null --> allowed
	 * @return
	 */
	public void check(UserIdentity userIdentity) {
		if (!isAllowed(userIdentity)) {
			throw new CarpException("not allowed for: " + userIdentity);
		}
	}

	public static ACL of(String... roles) {
		if (roles == null || roles.length == 0) {
			return EmptyACL.ADMIN;
		}
		if (roles.length == 1) {
			return new OneACL(roles[0]);
		}
		return new ArrayACL(roles);
	}

	public static ACL of(Collection<String> roles) {
		if (roles == null || roles.size() == 0) {
			return EmptyACL.ADMIN;
		}
		if (roles.size() == 1) {
			return new OneACL(roles.iterator().next());
		}
		return new ArrayACL(roles.toArray(new String[0]));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ACL)) {
			return false;
		}
		ACL other = (ACL) obj;
		HashSet<String> setA = new HashSet<>();
		HashSet<String> setB = new HashSet<>();
		this.collectRoles(setA);
		other.collectRoles(setB);
		return setA.equals(setB);
	}
}