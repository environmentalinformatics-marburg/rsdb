package util.indexedstorage;

public class Box {
	public byte[] buf;
	public int len;

	public Box(byte[] buf) {
		this.buf = buf;
		this.len = 0;
	}
}