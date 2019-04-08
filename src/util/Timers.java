package util;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Timers implements Serializable {
	private static final Logger log = LogManager.getLogger();
	public static final Timers DEFAULT = new Timers();
	
	private Map<String,Timer> map = new ConcurrentHashMap<String,Timer>();
	
	/**
	 * Start a new timer or restart an existing timer.
	 * @param name
	 * @return start time-stamp
	 */
	public Timer start(String name) {
		return put(new Timer(name)); 
	}

	/**
	 * Stop a started timer.
	 * @param name
	 * @return interval of time
	 */
	public Timer stop(String name) {
		return get(name).stop();
	}
	
	public Timer resume(String name) {
		Timer timer = map.get(name);
		if(timer == null) {
			return start(name);
		} else {
			return timer.resume();
		}
	}

	/**
	 * Stop a started timer.
	 * @param name
	 * @return text of duration and name
	 */
	public String stopToString(String name) {
		return stop(name).toString();
	}

	/**
	 * Stop a started timer and Print duration.
	 * @param name
	 */
	public void stopAndPrint(String name) {
		System.out.println(stopToString(name));
	}

	/**
	 * Stop a started timer and Print duration with additional text.
	 * @param name
	 * @param text additional text
	 */
	public  void stopAndPrint(String name, String text) {
		System.out.println(stopToString(name)+" "+text);
	}

	/**
	 * Text of duration of timer from start to stop or to current time if timer is not stopped.	
	 * @param name
	 * @return
	 */
	public String toString(String name) {		
		return get(name).toString();
	}

	/**
	 * All timers to text separated by newline.
	 * @return
	 */
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for(Timer it:map.values()) {
			s.append('\n');
			s.append(it.toString());
		}
		return s.toString();
	}

	/**
	 * Removes timer.
	 * @param name
	 * @return
	 */
	public Timer remove(String name) {
		return map.remove(name);
	}

	public Timer put(Timer it) {
		map.put(it.name, it);
		return it;
	}

	public Timer get(String name) {
		Timer it = map.get(name);
		if(it==null) {
			log.warn("timer not started: "+name);
			it = new Timer(name, -1, -1);
		}
		return it;
	}
}
