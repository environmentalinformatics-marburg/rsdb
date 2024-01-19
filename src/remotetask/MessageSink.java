package remotetask;

import org.tinylog.Logger;

public interface MessageSink {	
	public static final MessageSink MESSAGE_SINK_NULL = new MessageSink() {		
		@Override
		public void setMessage(String message) { }
		
		@Override
		public void log(String message) { }
	};
	
	public static final MessageSink MESSAGE_SINK_LOG = new MessageSink() {		
		@Override
		public void setMessage(String message) { 
			log(message);
		}
		
		@Override
		public void log(String message) {
			Logger.info(message);
		}
	};
	
	void setMessage(String message);
	void log(String message);
}
