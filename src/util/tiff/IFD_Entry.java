package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

public abstract class IFD_Entry implements Comparable<IFD_Entry> {

	public final short id;

	public IFD_Entry(short id) {
		this.id = id;
	}

	@Override
	public int compareTo(IFD_Entry o) {
		return Integer.compare(Short.toUnsignedInt(id), Short.toUnsignedInt(o.id));
	}

	public abstract void writeIFD_entryTIFF(DataOutput out, int data_pos, int image_data_pos) throws IOException;
	public abstract void writeIFD_entryBigTIFF(DataOutput out, long data_pos, long image_data_pos) throws IOException;
	
	public int data_sizeTIFF() {
		return 0;
	}
	
	public long data_sizeBigTIFF() {
		return 0;
	}

	public void write_dataTIFF(DataOutput out) throws IOException {
		//nothing
	}
	
	public void write_dataBigTIFF(DataOutput out) throws IOException {
		//nothing
	}
}