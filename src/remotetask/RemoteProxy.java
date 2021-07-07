package remotetask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class RemoteProxy {
	private static final Logger log = LogManager.getLogger();
	
	private MessageProxy messageProxy = MessageProxy.MESSAGE_PROXY_NULL;
	
	public final void setMessageProxy(MessageProxy messageProxy) {
		this.messageProxy = messageProxy;
	}
	
	protected final void setMessage(String message) {
		messageProxy.setMessage(message);
	}
	
	public String getName() {		
		Class<? extends RemoteProxy> clazz = this.getClass();		
		return clazz.getSimpleName();
	}
	
	public abstract void process() throws Exception;
	
	public void close() {}
	
	public boolean isCancelable() {
		return false;
	}
	
	public void cancel() {
		log.info("cancel not supported");
	}
	
	public boolean isCanceled() {
		return false;
	}
}
