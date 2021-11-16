package util.tiff;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import util.tiff.TiffReader.IfdEntry;

public class IFD_doubles extends IFD_Entry { // 64 bit float
	double[] values;

	public  IFD_doubles(short id, double... values) {
		super(id);
		this.values = values;
	}

	@Override
	public void writeIFD_entryTIFF(DataOutput out, int data_pos, int image_data_pos) throws IOException {
		out.writeShort(id);
		out.writeShort(0x000c); //12 --- Double precision (8-byte) IEEE format.
		int len = values.length;
		out.writeInt(len);  //--- element count
		if(len==0) {
			out.writeInt(0x00_00_00_00); //--- (not data offset): fill bytes
		} else {
			out.writeInt(data_pos); //--- data offset
		}
	}

	@Override
	public void writeIFD_entryBigTIFF(DataOutput out, long data_pos, long image_data_pos) throws IOException {
		out.writeShort(id);
		out.writeShort(0x000c); //12 --- Double precision (8-byte) IEEE format.
		long len = values.length;
		out.writeLong(len);  //--- element count
		if(len==0) {
			out.writeLong(0x00_00_00_00__00_00_00_00l); //--- (not data offset): fill bytes
		} else if(len==1) {
			out.writeDouble(values[0]);
		} else {
			out.writeLong(data_pos); //--- data offset
		}
	}

	@Override
	public int data_sizeTIFF() {
		int len = values.length;
		return len==0?0:len*8;
	}
	
	@Override
	public long data_sizeBigTIFF() {
		long len = values.length;
		return len<2?0:len*8;
	}

	@Override
	public void write_dataTIFF(DataOutput out) throws IOException {
		if(values.length>0) {
			for(double v:values) {
				out.writeDouble(v);
			}		
		}
	}
	
	@Override
	public void write_dataBigTIFF(DataOutput out) throws IOException {
		if(values.length>1) {
			for(double v:values) {
				out.writeDouble(v);
			}		
		}
	}
	
	@Override
	public String toString() {
			return IfdEntry.tagToText(id) + " float32 " + Arrays.toString(values);
	}
}
