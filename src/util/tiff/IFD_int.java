package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

public class IFD_int extends IFD_Entry { // 32 bit unsigned integer
	public final int value;
	
	public static int UINT16_MAX_VALUE = (int) Math.pow(2, 16);
	
	
	public static IFD_Entry ofAuto(short id, int value) {
		if(value <= UINT16_MAX_VALUE) {
			return new IFD_short(id, (short) value);
		}
		return new IFD_int(id, value);
	}
	
	public IFD_int(short id, int value) {
		super(id);
		this.value = value;
	}
	@Override
	public void writeIFD_entryTIFF(DataOutput out, int data_pos, int image_data_pos) throws IOException {
		out.writeShort(id);
		out.writeShort(0x0004);//--- data type 32 bit unsigned integer
		out.writeInt(0x00_00_00_01);  //--- element count 1
		out.writeInt(value);//--- (not data offset): value 
	}
	@Override
	public void writeIFD_entryBigTIFF(DataOutput out, long data_pos, long image_data_pos) throws IOException {
		out.writeShort(id);
		out.writeShort(0x0004);//--- data type 32 bit unsigned integer
		out.writeLong(0x00_00_00_00__00_00_00_01l);  //--- element count 1
		out.writeInt(value);//--- (not data offset): value
		out.writeInt(0x00_00_00_00);//--- (not data offset): fill bytes		
	}
	
	@Override
	public String toString() {
		return TiffReader.IfdEntry.tagToText(id) + " " + Integer.toUnsignedString(value);
	}
}