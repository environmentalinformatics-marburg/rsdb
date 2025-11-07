package util.tiff;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

import util.tiff.TiffReader.IfdEntry;

public class IFD_shorts extends IFD_Entry {
	
	public final short[] values;
	
	public IFD_shorts(short id, short[] values) {
		super(id);
		Objects.requireNonNull(values);
		this.values = values;
	}
	
	@Override
	public void writeIFD_entryTIFF(DataOutput out, int data_pos, int image_data_pos) throws IOException {
		int len = values.length;
		out.writeShort(id);
		out.writeShort(0x0003);//--- data type ushort
		out.writeInt(len);  //--- element count
		if(len==0) {
			out.writeInt(0x00_00_00_00); //--- (not data offset): fill bytes
		} else if(len==1) {
			out.writeShort(values[0]);//--- (not data offset): value 
			out.writeShort(0x0000);//--- (not data offset): fill bytes
		} else if(len==2) {
			out.writeShort(values[0]);//--- (not data offset): value1 
			out.writeShort(values[1]);//--- (not data offset): value2
		} else {
			out.writeInt(data_pos); //--- data offset
		}
	}
	
	@Override
	public void writeIFD_entryBigTIFF(DataOutput out, long data_pos, long image_data_pos) throws IOException {
		long len = values.length;
		out.writeShort(id);
		out.writeShort(0x0003);//--- data type ushort
		out.writeLong(len);  //--- element count
		if(len==0) {
			out.writeLong(0x00_00_00_00__00_00_00_00l); //--- (not data offset): fill bytes
		} else if(len==1) {
			out.writeShort(values[0]);//--- (not data offset): value 
			out.writeShort(0x0000);//--- (not data offset): fill bytes
			out.writeInt(0x00_00_00_00); //--- (not data offset): fill bytes
		} else if(len==2) {
			out.writeShort(values[0]);//--- (not data offset): value1 
			out.writeShort(values[1]);//--- (not data offset): value2
			out.writeInt(0x00_00_00_00); //--- (not data offset): fill bytes
		} else if(len==3) {
			out.writeShort(values[0]);//--- (not data offset): value1 
			out.writeShort(values[1]);//--- (not data offset): value2
			out.writeShort(values[2]);//--- (not data offset): value3
			out.writeShort(0x0000);//--- (not data offset): fill bytes
		} else if(len==4) {
			out.writeShort(values[0]);//--- (not data offset): value1 
			out.writeShort(values[1]);//--- (not data offset): value2
			out.writeShort(values[2]);//--- (not data offset): value3
			out.writeShort(values[3]);//--- (not data offset): value4
		} else {
			out.writeLong(data_pos); //--- data offset
		}
	}

	@Override
	public int data_sizeTIFF() {
		return values.length<3?0:values.length*2;
	}
	
	@Override
	public long data_sizeBigTIFF() {
		long len = values.length;
		return len<5?0:len*2;
	}

	@Override
	public void write_dataTIFF(DataOutput out) throws IOException {
		if(values.length>2) {
			for(short v:values) {
				out.writeShort(v);
			}
		}
	}
	
	@Override
	public void write_dataBigTIFF(DataOutput out) throws IOException {
		if(values.length>4) {
			for(short v:values) {
				out.writeShort(v);
			}
		}
	}
	
	@Override
	public String toString() {
			return IfdEntry.tagToText(id) + " uint16 " + uint16stoString(values);
	}
	
	public static String uint16stoString(short[] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(Short.toUnsignedInt(a[i]));
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }
}