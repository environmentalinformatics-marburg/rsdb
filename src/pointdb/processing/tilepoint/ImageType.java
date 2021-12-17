package pointdb.processing.tilepoint;


import org.tinylog.Logger;

public enum ImageType {
	INTENSITY,
	Z,
	INTENSITY_Z;
	
	

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
			Logger.warn("image type not parsed :"+text);
			return defaultType;
		}
	}
}