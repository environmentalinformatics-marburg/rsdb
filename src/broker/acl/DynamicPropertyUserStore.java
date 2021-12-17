package broker.acl;

import java.io.IOException;
import java.lang.reflect.Field;


import org.tinylog.Logger;
import org.eclipse.jetty.security.PropertyUserStore;

public class DynamicPropertyUserStore extends PropertyUserStore {
	
	
	public DynamicPropertyUserStore() {
		try {
			Field field = PropertyUserStore.class.getSuperclass().getDeclaredField("_identityService");
			field.setAccessible(true);
			field.set(this, FastIdentityService.DEFAULT);
			//Logger.info("ref " + field.get(this));
		} catch (Exception e) {
			Logger.warn(e);
		}
	}
	
	public void reloadAccounts() throws IOException {
		super.loadUsers();
	}

}
