package pointdb.processing.tilepoint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum ImageType {
	INTENSITY,
	Z,
	INTENSITY_Z;
	
	private static final Logger log = LogManager.getLogger();

	public static ImageType parse(String text, ImageType defaultType) {
		if(text==null||text.isEmpty()) {
			return defaultType;
		}
		switch(text.trim().toUpperCase()) {
		case "INTENSITY":
			return INTENSITY;
		case "Z":
			return Z;
		case "INTENSITY_Z":
			return INTENSITY_Z;
		default:
			log.warn("image type not parsed :"+text);
			return defaultType;
		}
	}
}