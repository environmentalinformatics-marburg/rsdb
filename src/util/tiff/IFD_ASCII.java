package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IFD_ASCII extends IFD_Entry { // 32 bit unsigned integer
	private static final Logger log = LogManager.getLogger();
	
	CharSequence text;

	public IFD_ASCII(short id, CharSequence ascii) {
		super(id);
		this.text = ascii;
	}

	@Override
	public void writeIFD_entryTIFF(DataOutput out, int data_pos, int image_data_pos) throws IOException {
		out.writeShort(id);
		out.writeShort(0x0002); //--- 8-bit byte that contains a 7-bit ASCII code; the last byte must be NUL (binary zero).
		int len = text.length();
		out.writeInt(len+1);  //--- element count
		if(len==0) {
			out.writeInt(0x00_00_00_00); //--- (not data offset): nul byte + fill bytes
		} else if(len==1) {
			out.writeByte(text.charAt(0));
			out.writeByte(0x00); // nul byte
			out.writeByte(0x00); //fill byte
			out.writeByte(0x00); //fill byte
		} else if(len==2) {
			out.writeByte(text.charAt(0));
			out.writeByte(text.charAt(1));
			out.writeByte(0x00); // nul byte
			out.writeByte(0x00); //fill byte
		} else if(len==3) {
			out.writeByte(text.charAt(0));
			out.writeByte(text.charAt(1));
			out.writeByte(text.charAt(2));
			out.writeByte(0x00); // nul byte
		} else {
			out.writeInt(data_pos); //--- data offset
		}
	}
	
	@Override
	public void writeIFD_entryBigTIFF(DataOutput out, long data_pos, long image_data_pos) throws IOException {
		out.writeShort(id);
		out.writeShort(0x0002); //--- 8-bit byte that contains a 7-bit ASCII code; the last byte must be NUL (binary zero).
		long len = text.length();
		out.writeLong(len+1);  //--- element count
		if(len==0) {
			out.writeLong(0x00_00_00_00__00_00_00_00l); //--- (not data offset): nul byte + fill bytes
		} else if(len==1) {
			out.writeByte(text.charAt(0));
			out.writeByte(0x00); // nul byte
			out.writeByte(0x00); //fill byte
			out.writeByte(0x00); //fill byte			
			out.writeByte(0x00); //fill byte
			out.writeByte(0x00); //fill byte
			out.writeByte(0x00); //fill byte
			out.writeByte(0x00); //fill byte
		} else if(len==2) {
			out.writeByte(text.charAt(0));
			out.writeByte(text.charAt(1));
			out.writeByte(0x00); // nul byte
			out.writeByte(0x00); //fill byte
			out.writeByte(0x00); //fill byte
			out.writeByte(0x00); //fill byte
			out.writeByte(0x00); //fill byte
			out.writeByte(0x00); //fill byte
		} else if(len==3) {
			out.writeByte(text.charAt(0));
			out.writeByte(text.charAt(1));
			out.writeByte(text.charAt(2));
			out.writeByte(0x00); // nul byte
			out.writeByte(0x00); //fill byte
			out.writeByte(0x00); //fill byte
			out.writeByte(0x00); //fill byte
			out.writeByte(0x00); //fill byte
		} else if(len==4) {
			out.writeByte(text.charAt(0));
			out.writeByte(text.charAt(1));
			out.writeByte(text.charAt(2));
			out.writeByte(text.charAt(3));
			out.writeByte(0x00); // nul byte
			out.writeByte(0x00); //fill byte
			out.writeByte(0x00); //fill byte
			out.writeByte(0x00); //fill byte	
		} else if(len==5) {
			out.writeByte(text.charAt(0));
			out.writeByte(text.charAt(1));
			out.writeByte(text.charAt(2));
			out.writeByte(text.charAt(3));
			out.writeByte(text.charAt(4));
			out.writeByte(0x00); // nul byte
			out.writeByte(0x00); //fill byte
			out.writeByte(0x00); //fill byte
		} else if(len==6) {
			out.writeByte(text.charAt(0));
			out.writeByte(text.charAt(1));
			out.writeByte(text.charAt(2));
			out.writeByte(text.charAt(3));
			out.writeByte(text.charAt(4));
			out.writeByte(text.charAt(5));
			out.writeByte(0x00); // nul byte
			out.writeByte(0x00); //fill byte
		} else if(len==7) {
			out.writeByte(text.charAt(0));
			out.writeByte(text.charAt(1));
			out.writeByte(text.charAt(2));
			out.writeByte(text.charAt(3));
			out.writeByte(text.charAt(4));
			out.writeByte(text.charAt(5));
			out.writeByte(text.charAt(6));
			out.writeByte(0x00); // nul byte
		} else {
			out.writeLong(data_pos); //--- data offset
			//log.info("bigTIFF " + Util.hex(id) + " ascii " + Util.hex(data_pos));
		}
	}

	@Override
	public int data_sizeTIFF() {
		int len = text.length();
		return len<4?0:len+1;
	}
	
	@Override
	public long data_sizeBigTIFF() {
		long len = text.length();
		return len<8?0:len+1;
	}

	@Override
	public void write_dataTIFF(DataOutput out) throws IOException {
		int len = text.length();
		if(len>3) {
			for(int i=0;i<len;i++) {
				out.writeByte(text.charAt(i));
			}
			out.writeByte(0x00); // nul byte
		}
	}
	
	@Override
	public void write_dataBigTIFF(DataOutput out) throws IOException {
		int len = text.length();
		if(len>7) {
			for(int i=0;i<len;i++) {
				out.writeByte(text.charAt(i));
			}
			out.writeByte(0x00); // nul byte
		}
	}
}