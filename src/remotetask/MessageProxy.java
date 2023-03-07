package remotetask;

public interface MessageProxy {	
	public static final MessageProxy MESSAGE_PROXY_NULL = new MessageProxy() {		
		@Override
		public void setMessage(String message) { }
		
		@Override
		public void log(String message) { }
	};
	
	void setMessage(String message);
	void log(String message);
}
