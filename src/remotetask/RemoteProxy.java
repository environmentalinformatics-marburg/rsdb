package remotetask;

public abstract class RemoteProxy {
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
	
	protected abstract void process() throws Exception;
	protected void close() {}
}
