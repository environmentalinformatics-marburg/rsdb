package util;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


import org.tinylog.Logger;

public class BlokingTaskSubmitter {	

	private static final ForkJoinPool executor = ForkJoinPool.commonPool();
	//private static final ForkJoinPool executor = new ForkJoinPool(1);

	private static final int SUBMIT_CHECK_INTERVAL_COUNT = 64;
	private static final int MAX_QUEUED_COUNT = 128;
	private static final int MIN_QUEUED_COUNT = 96;
	private static final int WAIT_CHECK_INTERVAL_MS = 125;
	private static final int WAIT_FINISH_INTERVAL_MS = 5000;

	private int submit_remaining_count = SUBMIT_CHECK_INTERVAL_COUNT;

	private final Phaser phaser = new Phaser();

	public static abstract class PhasedTask extends ForkJoinTask<Void> {
		private final Phaser phaser;

		public PhasedTask(BlokingTaskSubmitter blokingTaskSubmitter) {
			this.phaser = blokingTaskSubmitter.phaser;
			phaser.register();
		}

		@Override
		public final Void getRawResult() { 
			return null; 
		}

		@Override
		public final void setRawResult(Void v) {
		}

		@Override
		public final boolean exec() { 
			try {
				run();
			} finally {
				phaser.arriveAndDeregister();
			}
			return true;
		}

		public abstract void run();
	}

	public static  class PhasedTaskRunner extends PhasedTask {
		private final Runnable runnable;

		public PhasedTaskRunner(BlokingTaskSubmitter blokingTaskSubmitter, Runnable runnable) {
			super(blokingTaskSubmitter);
			this.runnable = runnable;
		}

		@Override
		public void run() {
			runnable.run();
		}
	}

	public BlokingTaskSubmitter() {
		//Logger.info("int registered " + phaser.getRegisteredParties());
		phaser.register();
		//Logger.info("start registered " + phaser.getRegisteredParties());
	}

	public void submit(Runnable r) {
		submit(new PhasedTaskRunner(this, r));
	}

	public void submit(PhasedTask task) {
		if(submit_remaining_count == 0) {
			check();
		}
		submit_remaining_count--;
		executor.execute(task);
	}

	private void check() {		
		submit_remaining_count = SUBMIT_CHECK_INTERVAL_COUNT; 
		int registered = phaser.getRegisteredParties();
		if(registered >= MAX_QUEUED_COUNT) {
			//Logger.info("backpressure");
			while(registered >= MIN_QUEUED_COUNT) {
				try {
					//Logger.info("sleep registered " + registered);
					Thread.sleep(WAIT_CHECK_INTERVAL_MS);
				} catch (InterruptedException e) {
					Logger.warn(e);
				}
				registered = phaser.getRegisteredParties();
			}
			//Logger.info("remaining registered " + registered);
			if(registered <= 2) {
				Logger.info("empty queue " + registered + "   " + executor.getQueuedSubmissionCount()+"   " + phaser.getArrivedParties()+"   " + phaser.getUnarrivedParties()+"   " + phaser.getRegisteredParties()+"   " + phaser.getPhase());
			}
		}
	}

	public void finish() {
		Logger.info("try finish " + executor.getQueuedSubmissionCount()+"   " + phaser.getArrivedParties()+"   " + phaser.getUnarrivedParties()+"   " + phaser.getRegisteredParties()+"   " + phaser.getPhase());
		int phase = phaser.arriveAndDeregister();
		while(true) {
			try {
				phaser.awaitAdvanceInterruptibly(phase, WAIT_FINISH_INTERVAL_MS, TimeUnit.MILLISECONDS);
				break;
			} catch (InterruptedException e) {
				Logger.error("interrupted");
				throw new RuntimeException(e);
			} catch (TimeoutException e) {
				Logger.info("wait for finish " + executor.getQueuedSubmissionCount()+"   " + phaser.getArrivedParties()+"   " + phaser.getUnarrivedParties()+"   " + phaser.getRegisteredParties()+"   " + phaser.getPhase());
			}
		}
		Logger.info("finished " + executor.getQueuedSubmissionCount()+"   " + phaser.getArrivedParties()+"   " + phaser.getUnarrivedParties()+"   " + phaser.getRegisteredParties()+"   " + phaser.getPhase());
	}
}
