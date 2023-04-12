package util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.tinylog.Logger;

public class TaskPipeline {

	private static final ForkJoinPool executor = ForkJoinPool.commonPool();

	public static abstract class PipelineTask implements Runnable {
		private TaskPipeline taskPipeline;
		private volatile boolean done = false;
		private volatile Exception exception = null;

		public PipelineTask() {
		}

		@Override
		public final void run() {
			if(done) {
				Logger.warn("internal error task already run");
			} else {
				if(isError()) {
					//Logger.warn(exception.getMessage());
				} else {
					try {
						process();
					} catch(Exception e) {
						this.exception = e;
					}
				}
				taskPipeline.done(this);
			}
		}

		private void setTaskPipeline(TaskPipeline taskPipeline) {
			this.taskPipeline = taskPipeline;
		}

		protected abstract void process() throws Exception;

		protected abstract void finish() throws Exception;

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

		protected final boolean isError() {
			return exception != null;
		}
	}

	private final ArrayBlockingQueue<PipelineTask> tasks;
	private final Semaphore semaphoreEmpty = new Semaphore(1);
	private volatile boolean closed = false;
	ReentrantReadWriteLock submitLock = new ReentrantReadWriteLock();
	private final CopyOnWriteArrayList<Exception> exceptions = new CopyOnWriteArrayList<Exception>();

	public TaskPipeline(int capacity) throws InterruptedException {
		tasks = new ArrayBlockingQueue<PipelineTask>(capacity);
		semaphoreEmpty.acquire();
	}

	/**
	 * Blocking submit
	 * @throws InterruptedException
	 */
	public final void submit(PipelineTask pipelineTask) throws Exception {
		if(closed) {
			throw new RuntimeException("TaskPipeline closed already");
		} else {
			submitLock.readLock().lock();
			try {
				if(closed) {
					if(exceptions.isEmpty()) {
						throw new RuntimeException("TaskPipeline closed already");
					} else {
						/*for(Exception e:exceptions) {
							Logger.warn(e.getMessage());
						}*/
						//throw new RuntimeException("Exceptions in TaskPipeline", exceptions.get(0));
						throw new RuntimeException(exceptions.get(0));
					}
				} else {
					pipelineTask.setTaskPipeline(this);
					tasks.put(pipelineTask); // blocking
					if(!exceptions.isEmpty()) {
						pipelineTask.exception = new RuntimeException("closed exceptionally");
					}
					executor.execute(pipelineTask);
				}
			} finally {
				submitLock.readLock().unlock();
			}		
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
					if(top.isError()) {
						if(exceptions.isEmpty()) {
							exceptions.add(top.exception);
							closeExceptionally();							
						} else {
							exceptions.add(top.exception);
						}
					} else {
						if(exceptions.isEmpty()) {
							try {
								top.finish();
							} catch (Exception e) {
								top.exception = e;
								if(exceptions.isEmpty()) {
									exceptions.add(top.exception);
									closeExceptionally();							
								} else {
									exceptions.add(top.exception);
								}
							}
						} else {
							top.exception = new RuntimeException("closed exceptionally");
							exceptions.add(top.exception);
						}
					}
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

	private final void closeExceptionally() {
		//Logger.info("closeExceptionally");
		RuntimeException e = new RuntimeException("closed exceptionally");
		for(PipelineTask t:tasks) {
			if(!t.isError()) {
				t.exception = e;
			}
		}
		new Thread(() -> {
			close();
			for(PipelineTask t:tasks) {
				if(!t.isError()) {
					t.exception = e;
				}
			}
			//Logger.info("closeExceptionally closed");
		}).start();
	}

	private final void close() {
		if(!closed) {
			submitLock.writeLock().lock();
			try {
				/*if(closed) {
				throw new RuntimeException("closed already");
			}*/
				closed = true;
				if(tasks.isEmpty()) {
					semaphoreEmpty.release();
					return;
				}
			} finally {
				submitLock.writeLock().unlock();
			}
		}
	}

	public final void join() throws Exception {
		/*while(!tasks.isEmpty()) {
			Thread.sleep(125);
		}*/
		close();
		semaphoreEmpty.acquire(); // wait for empty queue
		semaphoreEmpty.release();
		if(!exceptions.isEmpty()) {
			/*for(Exception e:exceptions) {
				Logger.warn(e.getMessage());
			}*/
			//throw new RuntimeException("Exceptions in TaskPipeline", exceptions.get(0));
			throw new RuntimeException(exceptions.get(0));
		}
	}
}
