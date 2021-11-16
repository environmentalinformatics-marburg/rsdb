package util.tiff;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.Util;
import util.tiff.TiffReader.IfdEntry;

public class IFD_ints extends IFD_Entry { // 32 bit unsigned integer
	private static final Logger log = LogManager.getLogger();
	
	int[] values;

	public IFD_ints(short id, int... values) {
		super(id);
		this.values = values;
	}

	@Override
	public void writeIFD_entryTIFF(DataOutput out, int data_pos, int image_data_pos) throws IOException {
		out.writeShort(id);
		out.writeShort(0x0004); //--- data type 32 bit unsigned integer
		int len = values.length;
		out.writeInt(len);  //--- element count
		if(len==0) {
			out.writeInt(0x00_00_00_00); //--- (not data offset): fill bytes
		} else if(len==1) {
			out.writeInt(values[0]);//--- (not data offset): value
		} else {
			out.writeInt(data_pos); //--- data offset
			//log.info(hex(id) + " ints " + hex(data_pos));
		}
	}
	
	@Override
	public void writeIFD_entryBigTIFF(DataOutput out, long data_pos, long image_data_pos) throws IOException {
		out.writeShort(id);
		out.writeShort(0x0004); //--- data type 32 bit unsigned integer
		long len = values.length;
		out.writeLong(len);  //--- element count
		if(len==0) {
			out.writeLong(0x00_00_00_00__00_00_00_00l); //--- (not data offset): fill bytes
		} else if(len==1) {
			out.writeInt(values[0]);//--- (not data offset): value
			out.writeInt(0x00_00_00_00); //--- (not data offset): fill bytes
		} else if(len==2) {
			out.writeInt(values[0]);//--- (not data offset): value
			out.writeInt(values[1]);//--- (not data offset): value
		} else {
			out.writeLong(data_pos); //--- data offset
			log.info("bigTIFF " + Util.hex(id) + " ints " + Util.hex(data_pos));
		}
	}
	
	

	@Override
	public int data_sizeTIFF() {
		int len = values.length;
		return len<2?0:len*4;
	}
	
	@Override
	public long data_sizeBigTIFF() {
		long len = values.length;
		return len<3?0:len*4;
	}

	@Override
	public void write_dataTIFF(DataOutput out) throws IOException {
		if(values.length>1) {
			for(int v:values) {
				out.writeInt(v);
				//log.info(" ints int " + v + " ->  " + hex(v));
			}		
		}
	}
	
	@Override
	public void write_dataBigTIFF(DataOutput out) throws IOException {
		if(values.length>2) {
			for(int v:values) {
				out.writeInt(v);
				log.info("bigTIFF ints int " + v + " ->  " + Util.hex(v));
			}		
		}
	}
	
	@Override
	public String toString() {
			return IfdEntry.tagToText(id) + " uint32 " + Arrays.toString(values);
	}
}