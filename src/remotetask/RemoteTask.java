package remotetask;

import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import remotetask.pointcloud.task_pointcloud;
import remotetask.pointdb.task_pointdb;
import remotetask.rasterdb.task_rasterdb;
import remotetask.vectordb.task_vectordb;

public abstract class RemoteTask implements Runnable, MessageProxy {
	private static final Logger log = LogManager.getLogger();
	
	private ConcurrentLinkedQueue<String> logMessages = new ConcurrentLinkedQueue<String>();
	
	private static final AtomicLong cnt = new AtomicLong(0);
	
	public static Comparator<RemoteTask> START_TIME_COMPARATOR = new Comparator<RemoteTask>() {
		@Override
		public int compare(RemoteTask o1, RemoteTask o2) {
			return Long.compare(o1.tstart, o2.tstart);
		}
	};
	
	public final long id = cnt.incrementAndGet();
	private long tstart = -1;
	private long tend = -1;
	private String message = "init";
	private long lastMessageTime = -1;
	private final long messageDuration = 1000;

	public static enum Status {
		READY,
		RUNNING,
		DONE,
		ERROR,
	}

	private volatile Status status = Status.READY;

	@Override
	public final void run() {
		tstart = System.currentTimeMillis();
		status = Status.RUNNING;
		try {
			process();
			status = Status.DONE;
		} catch(Exception e) {
			status = Status.ERROR;
			setMessage("Error: " + e.getMessage());
			log.error(e.getMessage());
			e.printStackTrace();
		} catch(Throwable e) {
			status = Status.ERROR;
			setMessage("Error: " + e.getMessage());
			log.error(e.getMessage());
			e.printStackTrace();
			throw e;
		} finally {
			close();
			tend = System.currentTimeMillis(); 
		}
	}

	protected abstract void process() throws Exception;
	protected void close() {}

	public final boolean isActive() {
		return status == Status.READY || status == Status.RUNNING;
	}
	
	public final long getRuntimeMillis() {
		return  tstart == -1 ? 0 : (tend == -1 ? System.currentTimeMillis() - tstart : tend - tstart);
	}
	
	public final void setMessage(String message) {
		lastMessageTime = System.currentTimeMillis();
		this.message = message;
		log(message);
	}
	
	protected boolean isMessageTime() {
		return lastMessageTime + messageDuration <= System.currentTimeMillis();
	}
	
	public final String getMessage() {
		return message;
	}

	public Status getStatus() {
		return status;
	}
	
	public String getName() {		
		Class<? extends RemoteTask> clazz = this.getClass();
		if(clazz.isAnnotationPresent(task_rasterdb.class)) {
			return "rasterdb/" + clazz.getAnnotation(task_rasterdb.class).value();
		}
		if(clazz.isAnnotationPresent(task_pointdb.class)) {
			return "pointdb/" + clazz.getAnnotation(task_pointdb.class).value();
		}		
		if(clazz.isAnnotationPresent(task_pointcloud.class)) {
			return "pointcloud/" + clazz.getAnnotation(task_pointcloud.class).value();
		}
		if(clazz.isAnnotationPresent(task_vectordb.class)) {
			return "vectordb/" + clazz.getAnnotation(task_vectordb.class).value();
		}		
		return clazz.getSimpleName();
	}
	
	public boolean isCancelable() {
		return false;
	}
	
	public void cancel() {
		log.info("cancel not supported");
	}
	
	public boolean isCanceled() {
		return false;
	}
	
	protected void log(String message) {
		//logMessages.add(message);
		log.info(message);
	}

	public ConcurrentLinkedQueue<String> getLog() {
		return logMessages;
	}
	
	public MessageReceiver getMessageReceiver() {
		return new MessageReceiver() {			
			@Override
			public void setMessage(String message) {
				RemoteTask.this.setMessage(message);				
			}
		};
	}
}
