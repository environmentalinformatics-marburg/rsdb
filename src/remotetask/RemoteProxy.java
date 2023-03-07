package remotetask;


import org.tinylog.Logger;

public abstract class RemoteProxy implements AutoCloseable {	
	
	private MessageProxy messageProxy = MessageProxy.MESSAGE_PROXY_NULL;
	
	public final void setMessageProxy(MessageProxy messageProxy) {
		this.messageProxy = messageProxy;
	}
	
	protected final void setMessage(String message) {
		messageProxy.setMessage(message);
	}
	
	protected final void log(String message) {
		messageProxy.log(message);
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
		Logger.info("cancel not supported");
	}
	
	public boolean isCanceled() {
		return false;
	}
}
