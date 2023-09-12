package util;

public class Interruptor {

	public volatile boolean interrupted;
	public final long id;

	public Interruptor(long id) {
		this.id = id;
	}

	/**
	 * 
	 * @param interruptor nullable
	 */
	public static boolean isInterrupted(Interruptor interruptor) {
		return interruptor != null && interruptor.interrupted;
	}

	/**
	 * 
	 * @param interruptor nullable
	 */
	public static void checkInterrupted(Interruptor interruptor) {
		//Logger.info("checkInterrupted " + interruptor);
		if(interruptor != null && interruptor.interrupted) {
			throw new InterruptorInterruptedException();
		}
	}
}