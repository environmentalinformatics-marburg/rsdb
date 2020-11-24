package remotetask;

public interface MessageProxy {	
	public static final MessageProxy MESSAGE_PROXY_NULL = (String message) -> {};
	
	void setMessage(String message);	
}
