package util.tiff;

public class GeoKey_Entry {

	public final short id;
	public final short target;
	public final short count;
	public final short offset;

	public GeoKey_Entry(short id, short target, short count, short offset) {
		this.id = id;
		this.target = target;
		this.count = count;
		this.offset = offset;
	}



}
