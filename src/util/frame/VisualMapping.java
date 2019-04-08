package util.frame;



import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum VisualMapping {

	GREY,
	COLOR;

	private static final Logger log = LogManager.getLogger();

	/**
	 * 
	 * @param mapping nullable
	 * @return
	 */
	public static VisualMapping parse(String mapping) {
		String text = mapping==null?"default":mapping.trim().toLowerCase();
		switch(text) {
		case "grey":
			return GREY;
		case "color":
			return COLOR;
		case "default":
			return DEFAULT;
		default:
			log.warn("unknown mapping -> set default  "+mapping);
			return DEFAULT;
		}
	}

	public static final VisualMapping DEFAULT = GREY; 

}
