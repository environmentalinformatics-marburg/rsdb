package util.tiff;

public class GeoKey_ASCII extends GeoKey_Entry {

	public GeoKey_ASCII(short id, String content, StringBuilder asciiCollector) {
		super(id, (short)0x87B1, (short)content.length(), (short)asciiCollector.length()); // target GeoAsciiParamsTag
		asciiCollector.append(content);
	}

}
