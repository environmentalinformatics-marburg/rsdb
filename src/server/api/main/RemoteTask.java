package server.api.main;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class RemoteTask implements Runnable {
	private static final Logger log = LogManager.getLogger();
	
	private static final AtomicLong cnt = new AtomicLong(0);
	
	public final long id = cnt.incrementAndGet();
	private long tstart = -1;
	private long tend = -1;
	private String message = "init";

	public static enum Status {
		READY,
		RUNNING,
		DONE,
		ERROR,
	}

	public volatile Status status = Status.READY;

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
		} finally {
			close();
			tend = System.currentTimeMillis(); 
		}
	}

	protected abstract void process() throws Exception;
	protected void close() {}

	public boolean isActive() {
		return status == Status.READY || status == Status.RUNNING;
	}
	
	public long runtime() {
		return  tstart == -1 ? 0 : (tend == -1 ? System.currentTimeMillis() - tstart : tend - tstart);
	}
	
	public void setMessage(String message) {
		log.info("message: " + message);
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
}
