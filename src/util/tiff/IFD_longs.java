package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

/**
 * writes uint64 values for BigTIFF
 * 
 * writes uint32 values for TIFF (reports no warnings for long to int conversion)
 * @author woellauer
 *
 */
public class IFD_longs extends IFD_Entry  {

	int[] valuesInt;
	long[] valuesLong;

	public IFD_longs(short id, int... values) {
		super(id);
		this.valuesInt = values;
	}
	
	public IFD_longs(short id, long... values) {
		super(id);
		this.valuesLong = values;
	}

	@Override
	public void writeIFD_entryTIFF(DataOutput out, int data_pos, int image_data_start_pos) throws IOException {
		out.writeShort(id);
		out.writeShort(IFD.DATA_TYPE_UINT32);
		int len = valuesLong == null ? valuesInt.length : valuesLong.length;
		out.writeInt(len);  //--- element count
		if(len==0) {
			out.writeInt(0x00_00_00_00); //--- (not data offset): fill bytes
		} else if(len==1) {
			out.writeInt(valuesLong == null ? valuesInt[0] : (int) valuesLong[0]);//--- (not data offset): value, long to int conversion
		} else {
			out.writeInt(data_pos); //--- data offset
		}
	}
	
	@Override
	public void writeIFD_entryBigTIFF(DataOutput out, long data_pos, long image_data_start_pos) throws IOException {
		out.writeShort(id);		
		out.writeShort(IFD.DATA_TYPE_UINT64);
		long len = valuesLong == null ? valuesInt.length : valuesLong.length;
		out.writeLong(len);  //--- element count
		if(len==0) {
			out.writeLong(0x00_00_00_00__00_00_00_00l); //--- (not data offset): fill bytes
		} else if(len==1) {
			out.writeLong(valuesLong == null ? valuesInt[0] : valuesLong[0]);
		} else {
			out.writeLong(data_pos); //--- data offset
		}
	}

	@Override
	public int data_sizeTIFF() {
		int len = valuesLong == null ? valuesInt.length : valuesLong.length;
		return len<2?0:len*4;
	}
	
	@Override
	public long data_sizeBigTIFF() {
		int len = valuesLong == null ? valuesInt.length : valuesLong.length;
		return len<2?0:len*8;
	}

	@Override
	public void write_dataTIFF(DataOutput out) throws IOException {
		int len = valuesLong == null ? valuesInt.length : valuesLong.length;
		if(len>1) {
			if(valuesLong == null) {
				for(int v:valuesInt) {
					out.writeInt(v);
				}
			} else {
				for(long v:valuesLong) {
					out.writeInt((int) v); // long to int conversion
				}
			}			
		}
	}
	
	@Override
	public void write_dataBigTIFF(DataOutput out) throws IOException {
		int len = valuesLong == null ? valuesInt.length : valuesLong.length;
		if(len>1) {
			if(valuesLong == null) {
				for(int v:valuesInt) {
					out.writeLong(v);
				}
			} else {
				for(long v:valuesLong) {
					out.writeLong(v);
				}
			}			
		}
	}

}
