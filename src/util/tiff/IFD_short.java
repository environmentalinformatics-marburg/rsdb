package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

public class IFD_short extends IFD_Entry { // 16 bit unsigned integer
	public final short value;		
	public IFD_short(short id, short value) {
		super(id);
		this.value = value;
	}
	@Override
	public void writeIFD_entryTIFF(DataOutput out, int data_pos, int image_data_pos) throws IOException {
		out.writeShort(id);
		out.writeShort(0x0003);//--- data type ushort
		out.writeInt(0x00_00_00_01);  //--- element count 1
		out.writeShort(value);//--- (not data offset): value 
		out.writeShort(0x00_00);//--- (not data offset): fill bytes
	}
	@Override
	public void writeIFD_entryBigTIFF(DataOutput out, long data_pos, long image_data_pos) throws IOException {
		out.writeShort(id);
		out.writeShort(0x0003);//--- data type ushort
		out.writeLong(0x00_00_00_00__00_00_00_01l);  //--- element count 1
		out.writeShort(value);//--- (not data offset): value 
		out.writeShort(0x00_00);//--- (not data offset): fill bytes
		out.writeInt(0x00_00_00_00);//--- (not data offset): fill bytes	
	}
}