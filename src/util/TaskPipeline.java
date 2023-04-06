package util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.tinylog.Logger;

public class TaskPipeline {

	private static final ForkJoinPool executor = ForkJoinPool.commonPool();

	public static abstract class PipelineTask implements Runnable {
		private TaskPipeline taskPipeline;
		private volatile boolean done = false;

		public PipelineTask() {
		}

		@Override
		public final void run() {
			if(done) {
				throw new RuntimeException("internal error task already run");
			}
			process();
			taskPipeline.done(this);			
		}

		private void setTaskPipeline(TaskPipeline taskPipeline) {
			this.taskPipeline = taskPipeline;
		}

		protected abstract void process();

		protected abstract void finish();
		
		protected final void removeFromFinishQueue() {
			taskPipeline.removeFromFinish(this);
		}

		@Override
		public String toString() {
			return (done ? "X" : ".");
		}
		
		protected final boolean isDone() {
			return done;
		}
	}

	private final ArrayBlockingQueue<PipelineTask> tasks;
	private final Semaphore semaphoreEmpty = new Semaphore(1);
	private volatile boolean closed = false;
	ReentrantReadWriteLock submitLock = new ReentrantReadWriteLock();

	public TaskPipeline(int capacity) throws InterruptedException {
		tasks = new ArrayBlockingQueue<PipelineTask>(capacity);
		semaphoreEmpty.acquire();
	}

	/**
	 * Blocking submit
	 * @throws InterruptedException
	 */
	public final void submit(PipelineTask pipelineTask) throws InterruptedException {
		submitLock.readLock().lock();
		try {
			if(!closed) {
				pipelineTask.setTaskPipeline(this);
				tasks.put(pipelineTask); // blocking
				executor.execute(pipelineTask);
			} else {
				throw new RuntimeException("TaskPipeline closed already");
			}
		} finally {
			submitLock.readLock().unlock();
		}		
	}
	
	private void removeFromFinish(PipelineTask pipelineTask) {
		submitLock.readLock().lock();
		try {
			tasks.remove(pipelineTask);
		} finally {
			submitLock.readLock().unlock();
		}	
	}

	private synchronized void done(PipelineTask pipelineTask) {
		pipelineTask.done = true;
		while(true) {
			Logger.info(tasks);
			PipelineTask top = tasks.peek();
			if(top != null && top.done) {
				top = tasks.poll();
				if(top != null && top.done) {
					top.finish();
				} else {
					throw new RuntimeException("internal error task not in queue");
				}
			} else {
				if(top == null) {					
					submitLock.readLock().lock();
					try {
						if(closed) {
							semaphoreEmpty.release();
						}
					} finally {
						submitLock.readLock().unlock();
					}
				}
				return;
			}
		}
	}

	public final void join() throws InterruptedException {
		/*while(!tasks.isEmpty()) {
			Thread.sleep(125);
		}*/
		submitLock.writeLock().lock();
		try {
			if(closed) {
				throw new RuntimeException("closed already");
			}
			closed = true;
			if(tasks.isEmpty()) {
				semaphoreEmpty.release();
				return;
			}
		} finally {
			submitLock.writeLock().unlock();
		}
		semaphoreEmpty.acquire(); // wait for empty queue
		semaphoreEmpty.release();
	}
}
