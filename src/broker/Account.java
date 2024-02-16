package broker;

import java.security.Principal;
import java.util.Arrays;
import java.util.LinkedHashMap;

import javax.security.auth.Subject;

import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.security.Password;

import util.yaml.YamlMap;

public class Account implements UserIdentity, Principal {
	
	private static final String[] NO_ROLES = new String[0];

	public final String user;
	public final String password;
	public final String[] roles; // not null
	public final boolean managed;	
	public final String date_created; // nullable
	public final String comment; // nullable

	private final Credential credential;
	
	public static class Builder {

		public String user;
		public String password;
		public String[] roles = NO_ROLES;
		public boolean managed;
		public String date_created; // nullable
		public String comment; // nullable
		
		public Builder(Account account) {
			this.user = account.user;
			this.password = account.password;
			this.roles = account.roles;
			this.managed = account.managed;
			this.date_created = account.date_created;
			this.comment = account.comment;
		}
		
		public Account build() {
			return new Account(user, password, roles == null ? NO_ROLES : roles, managed, date_created, comment);
		}
	}

	public Account(String user, String password, String[] roles, boolean managed, String date_created, String comment) {
		this.user = user;
		this.password = password;
		this.roles = roles == null ? NO_ROLES : roles;
		this.managed = managed;
		this.date_created = date_created;
		this.comment = comment;
		this.credential = new Password(password);
	}

	public static Account ofYAML(YamlMap yamlMap, boolean managed) {
		String user = yamlMap.getString("user");
		String password = yamlMap.getString("password");
		String[] roles = yamlMap.optList("roles").asStringArray();
		String date_created = yamlMap.optString("date_created");
		String comment = yamlMap.optString("comment");
		return new Account(user, password, roles, managed, date_created, comment);
	}

	public LinkedHashMap<String, Object> toMap() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("user", user);
		map.put("password", password);
		map.put("roles", roles);
		if(date_created != null) {
			map.put("date_created", date_created);
		}
		if(comment != null && !comment.isBlank()) {
			map.put("comment", comment);
		}
		return map;
	}

	// Principal
	@Override
	public String getName() {
		return user;
	}

	// UserIdentity
	@Override
	public Subject getSubject() {
		throw new RuntimeException("not implemented");
	}

	// UserIdentity
	@Override
	public Principal getUserPrincipal() {
		return this;
	}

	// UserIdentity
	@Override
	public boolean isUserInRole(String role, Scope scope) {
		for (String r:roles) {
            if (r.equals(role))
                return true;
        }
        return false;
	}
	
	public Builder builder() {
		return new Builder(this);
	}
	
	public boolean checkCredentials(Object credentials) {
		return this.credential.check(credentials);
	}

	@Override
	public String toString() {
		return "Account [user=" + user + ", roles=" + Arrays.toString(roles) + ", managed=" + managed
				+ ", date_created=" + date_created + ", comment=" + comment + "]";
	}		
}
