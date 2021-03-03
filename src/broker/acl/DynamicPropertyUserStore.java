package broker.acl;

import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.PropertyUserStore;

public class DynamicPropertyUserStore extends PropertyUserStore {
	private static final Logger log = LogManager.getLogger();
	
	public DynamicPropertyUserStore() {
		try {
			Field field = PropertyUserStore.class.getSuperclass().getDeclaredField("_identityService");
			field.setAccessible(true);
			field.set(this, FastIdentityService.DEFAULT);
			//log.info("ref " + field.get(this));
		} catch (Exception e) {
			log.warn(e);
		}
	}
	
	public void reloadAccounts() throws IOException {
		super.loadUsers();
	}

}
