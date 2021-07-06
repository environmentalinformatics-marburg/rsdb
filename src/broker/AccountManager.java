package broker;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import javax.security.auth.Subject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.RunAsToken;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.security.Credential;

import jakarta.servlet.ServletRequest;
import util.collections.vec.Vec;
import util.yaml.YamlMap;
import util.yaml.YamlUtil;


public class AccountManager extends UserStore implements IdentityService, LoginService {
	private static final Logger log = LogManager.getLogger();

	private final Path accountsPath;
	private final Path realmPropertiesPath;
	private TreeMap<String, Account> accountMap = new TreeMap<String, Account>();

	public AccountManager(Path accountsPath, Path realmPropertiesPath) {
		this.accountsPath = accountsPath;
		this.realmPropertiesPath = realmPropertiesPath;
		if(!read()) {
			write();
			if(!read()) {
				throw new RuntimeException("could not create accounts storage file");
			}
		}		
	}

	private synchronized void readRealmProperties(TreeMap<String, Account> accountMap) {
		try {
			if(Files.exists(realmPropertiesPath) ) {
				Properties properties = new Properties();
				try(FileInputStream in = new FileInputStream(realmPropertiesPath.toFile())) {
					properties.load(in);
				}
				for(Entry<Object, Object> entry : properties.entrySet()) {
					String username = ((String)entry.getKey()).trim();
					String credentialsText = ((String)entry.getValue()).trim();
					String rolesText = null;
					int c = credentialsText.indexOf(',');
					if (c >= 0) {
						rolesText = credentialsText.substring(c + 1).trim();
						credentialsText = credentialsText.substring(0, c).trim();
					}
					if (username.length() > 0) {
						String[] roles = new String[] {};
						if (rolesText != null && rolesText.length() > 0) {
							roles = StringUtil.csvSplit(rolesText);
						}
						Account account = new Account(username, credentialsText, roles, false);
						accountMap.put(account.user, account);
					}
				}
			} else {
				log.info("realm.properties file: " + realmPropertiesPath);
			}
		} catch(Exception e) {
			log.warn(e);
		}		
	}

	public synchronized boolean read() {
		TreeMap<String, Account> accountMap = new TreeMap<String, Account>();
		if(!Files.exists(accountsPath)) {
			log.info("no accounts file: " + accountsPath);
			readRealmProperties(accountMap);
			return false;			
		}
		YamlMap yamlMap = YamlUtil.readYamlMap(accountsPath);
		List<YamlMap> accountList = yamlMap.optList("accounts").asMaps();

		for(YamlMap a:accountList) {
			Account account = Account.ofYAML(a, true);
			accountMap.put(account.user, account);		}
		readRealmProperties(accountMap);
		this.accountMap = accountMap;
		return true;
	}

	public synchronized void write() {
		LinkedHashMap<String, Object> yamlMap = new LinkedHashMap<String, Object>();
		Vec<Map<String, Object>> vec = new Vec<Map<String, Object>>();
		for(broker.Account account:accountMap.values()) {
			if(account.managed) {
				vec.add(account.toMap());
			}
		}
		yamlMap.put("accounts", vec);		
		YamlUtil.writeSafeYamlMap(accountsPath, yamlMap);
	}

	//UserStore
	@Override
	public void addUser(String arg0, Credential arg1, String[] arg2) {
		throw new RuntimeException("not implemented");
	}

	//UserStore, LoginService
	@Override
	public IdentityService getIdentityService() {
		return this;
	}

	/*//UserStore
	@Override
	public Account getUserIdentity(String userName) {
		Account account = accountMap.get(userName);
		return account;
	}*/

	//UserStore
	@Override
	public void removeUser(String username) {
		throw new RuntimeException("not implemented");
	}

	//IdentityService
	@Override
	public Object associate(UserIdentity arg0) {
		//log.warn("not implemented");
		return null;
	}

	//IdentityService
	@Override
	public void disassociate(Object arg0) {
		//log.warn("not implemented: " + arg0);			
	}

	//IdentityService
	@Override
	public UserIdentity getSystemUserIdentity() {
		throw new RuntimeException("not implemented");
	}

	//IdentityService
	@Override
	public RunAsToken newRunAsToken(String arg0) {
		throw new RuntimeException("not implemented");
	}

	//IdentityService
	@Override
	public UserIdentity newUserIdentity(Subject arg0, Principal arg1, String[] arg2) {
		throw new RuntimeException("not implemented");
	}

	//IdentityService
	@Override
	public Object setRunAs(UserIdentity arg0, RunAsToken arg1) {
		throw new RuntimeException("not implemented");
	}

	//IdentityService
	@Override
	public void unsetRunAs(Object arg0) {
		throw new RuntimeException("not implemented");			
	}

	//LoginService
	@Override
	public String getName() {
		return "";
	}

	//LoginService
	@Override
	public UserIdentity login(String userName, Object credentials, ServletRequest request) {
		if(userName == null) {
			return null;
		}
		Account account = accountMap.get(userName);
		if(account == null) {
			return null;
		}
		if(account.checkCredentials(credentials)) {
			return account;
		}
		return null;
	}

	//LoginService
	@Override
	public void logout(UserIdentity arg0) {
		throw new RuntimeException("not implemented");			
	}

	//LoginService
	@Override
	public void setIdentityService(IdentityService arg0) {
		throw new RuntimeException("not implemented");	
	}

	//LoginService
	@Override
	public boolean validate(UserIdentity arg0) {
		throw new RuntimeException("not implemented");
	}

	public void foreachAccount(Consumer<Account> consumer) {
		accountMap.values().forEach(consumer);		
	}

	public synchronized void addAccount(Account account) {
		if(!account.managed) {
			throw new RuntimeException("account needs to be managed");
		}
		TreeMap<String, Account> accountMap = new TreeMap<String, Account>();
		accountMap.putAll(this.accountMap);
		if(accountMap.containsKey(account.user)) {
			throw new RuntimeException("account already exists: " + account.user);	
		}
		accountMap.put(account.user, account);
		this.accountMap = accountMap;
		write();
		if(!read()) {
			throw new RuntimeException("could not read accounts");
		}
	}

	public synchronized void remvoeAccount(String user) {
		TreeMap<String, Account> accountMap = new TreeMap<String, Account>();
		accountMap.putAll(this.accountMap);
		Account account = accountMap.get(user);
		if(account == null) {
			throw new RuntimeException("account does not exist: " + user);	
		}
		if(!account.managed) {
			throw new RuntimeException("account needs to be managed");
		}
		accountMap.remove(user);
		this.accountMap = accountMap;
		write();
		if(!read()) {
			throw new RuntimeException("could not read accounts");
		}
	}
	
	public synchronized void setAccount(Account account) {
		if(!account.managed) {
			throw new RuntimeException("account needs to be managed");
		}
		TreeMap<String, Account> accountMap = new TreeMap<String, Account>();
		accountMap.putAll(this.accountMap);
		accountMap.put(account.user, account);
		this.accountMap = accountMap;
		write();
		if(!read()) {
			throw new RuntimeException("could not read accounts");
		}	
	}

	public void collectRoles(Set<String> roles) {
		for(Account account:accountMap.values()) {
			for(String role:account.roles) {
				roles.add(role);
			}
		}
	}
	
	public Account getAccount(String user) {
		return accountMap.get(user);
	}
}
