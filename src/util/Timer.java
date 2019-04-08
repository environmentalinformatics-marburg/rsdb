package util;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Timer measures duration of time-span between start and stop calls. Multiple time measurements are distinguished by name.
 * <p>
 * thread-safe
 * @author woellauer
 *
 */
public final class Timer implements AutoCloseable, Serializable, Iterable<Timer> {
	private static final Timers TIMERS = Timers.DEFAULT;

	public final String name;
	public long begin;
	public long end;

	public Timer(String name, long begin, long end) {
		this.name = name;
		this.begin = begin;
		this.end = end;
	}

	public Timer(String name, long begin) {
		this(name, begin, -1);
	}

	public Timer(String name) {
		this(name, System.currentTimeMillis());
	}

	@Override
	public void close() {
		stop();		
	}
	public Timer stop() {
		if(end==-1) {
			end = System.currentTimeMillis();
		}
		return this;
	}
	public Timer restart() {
		this.begin = System.currentTimeMillis();
		this.end = -1;
		return this;
	}
	public Timer resume() {
		if(end != -1) {
			long diff = end - begin;
			this.begin = System.currentTimeMillis() - diff;
			this.end = -1;
		}
		return this;
	}
	@Override
	public String toString() {
		return msToText(begin, end)+" "+name;
	}
	public void stopAndPrint() {
		stop();
		System.out.println(toString());
	}
	public void stopAndPrintText(String text) {
		System.out.println(stop()+" "+text);
	}	

	/**
	 * Start a new timer or restart an existing timer.
	 * @param name
	 * @return start time-stamp
	 */
	public static Timer start(String name) {
		return TIMERS.start(name); 
	}
	
	/**
	 * Stop a started timer.
	 * @param name
	 * @return interval of time
	 */
	public static Timer stop(String name) {
		return TIMERS.stop(name);
	}
	
	public static Timer resume(String name) {
		return TIMERS.resume(name); 
	}

	/**
	 * Stop a started timer and Print duration.
	 * @param name
	 */
	public static void stopAndPrint(String name) {
		TIMERS.stopAndPrint(name);
	}

	/**
	 * Stop a started timer and Print duration with additional text.
	 * @param name
	 * @param text additional text
	 */
	public static void stopAndPrint(String name, String text) {
		TIMERS.stopAndPrint(name, text);
	}

	/**
	 * Convert duration to text
	 * @param start
	 * @param end
	 * @return
	 */
	public static String msToText(long start, long end) {
		if(end==-1) {
			end = System.currentTimeMillis();
		}
		if(start<0 || end<0) {
			return "---";
		}
		long diff = end-start;
		if(diff<0) {
			return "---";
		}
		long h = diff%1000/100;
		long z = diff%100/10;
		long e = diff%10;
		return diff/1000+"."+h+z+e+" s";
	}

	/**
	 * All timers to text separated by newline.
	 * @return
	 */
	public static String toStringAll() {
		return TIMERS.toString();
	}

	/**
	 * Removes timer.
	 * @param name
	 * @return
	 */
	public Timer remove(String name) {
		return TIMERS.remove(name);
	}

	public static Timer put(Timer it) {
		return TIMERS.put(it);
	}

	public static Timer get(String name) {
		return TIMERS.get(name);
	}

	@Override
	public Iterator<Timer> iterator() {		
		return new Iterator<Timer>() {
			boolean open = true;
			@Override
			public boolean hasNext() {
				if(open) {
					open = false;
					return true;
				}
				stop();
				return false;
			}
			@Override
			public Timer next() {
				return Timer.this;
			}			
		};
	}
}
