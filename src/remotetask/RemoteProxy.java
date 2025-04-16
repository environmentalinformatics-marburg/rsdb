package remotetask;

public abstract class RemoteProxy implements AutoCloseable {	
	
	private MessageSink messageSink = MessageSink.MESSAGE_SINK_LOG;
	
	public final void setMessageSink(MessageSink messageSink) {
		this.messageSink = messageSink == null ? MessageSink.MESSAGE_SINK_LOG : messageSink;
	}
	
	/**
	 * 
	 * @return not null
	 */
	public final MessageSink getMessageSink() {
		return messageSink;
	}
	
	protected final void setMessage(String message) {
		messageSink.setMessage(message);
	}
	
	protected final void log(String message) {
		messageSink.log(message);
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
		setMessage("cancel not supported");
	}
	
	public boolean isCanceled() {
		return false;
	}
}
