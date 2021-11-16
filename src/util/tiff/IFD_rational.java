package util.tiff;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import util.tiff.TiffReader.IfdEntry;

public class IFD_rational extends IFD_Entry { // 2 * 32 bit unsigned integer
	
	public final int a;
	public final int b;	
	
	public IFD_rational(short id, int a, int b) {
		super(id);
		this.a = a;
		this.b = b;
	}
	
	@Override
	public void writeIFD_entryTIFF(DataOutput out, int data_pos, int image_data_pos) throws IOException {
		out.writeShort(id);
		out.writeShort(0x0005); //--- data type rational
		out.writeInt(0x00_00_00_01);  //--- element count 1
		out.writeInt(data_pos); //--- data offset
	}
	
	@Override
	public void writeIFD_entryBigTIFF(DataOutput out, long data_pos, long image_data_pos) throws IOException {
		out.writeShort(id);
		out.writeShort(0x0005); //--- data type rational
		out.writeLong(0x00_00_00_00__00_00_00_01l);  //--- element count 1
		out.writeInt(a);
		out.writeInt(b);
	}
	
	@Override
	public int data_sizeTIFF() {
		return 8; //2 * 32 bit unsigned integer
	}
	
	@Override
	public long data_sizeBigTIFF() {
		return 0;
	}
	
	@Override
	public void write_dataTIFF(DataOutput out) throws IOException {
		out.writeInt(a);
		out.writeInt(b);
	}
	
	@Override
	public void write_dataBigTIFF(DataOutput out) throws IOException {
		// nothing
	}
	
	@Override
	public String toString() {
			return IfdEntry.tagToText(id) + " rational64 " + a + "/" + b + "=" + (a / (double) b);
	}
}