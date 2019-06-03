package remotetask;

import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONObject;

import broker.Broker;

public class Context {
	
	public final Broker broker;
	public final JSONObject task;
	public final UserIdentity userIdentity;
	
	public Context(Broker broker, JSONObject task, UserIdentity userIdentity) {
		this.broker = broker;
		this.task = task;
		this.userIdentity = userIdentity;
	}
}
