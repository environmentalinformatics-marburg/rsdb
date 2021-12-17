package util.frame;




import org.tinylog.Logger;

public enum VisualMapping {

	GREY,
	COLOR;

	

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
			Logger.warn("unknown mapping -> set default  "+mapping);
			return DEFAULT;
		}
	}

	public static final VisualMapping DEFAULT = GREY; 

}
