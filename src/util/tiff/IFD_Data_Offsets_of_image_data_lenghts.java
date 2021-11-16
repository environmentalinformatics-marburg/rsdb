package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

public class IFD_Data_Offsets_of_image_data_lenghts extends IFD_Entry {

	int[] image_data_lenghtsTIFF;
	long[] image_data_lenghtsBigTIFF;

	int image_data_posTIFF;
	long image_data_posBigTIFF;

	public IFD_Data_Offsets_of_image_data_lenghts(short id, int... image_data_lenghtsTIFF) {
		super(id);
		this.image_data_lenghtsTIFF = image_data_lenghtsTIFF;
	}
	
	public IFD_Data_Offsets_of_image_data_lenghts(short id, long... image_data_lenghtsBigTIFF) {
		super(id);
		this.image_data_lenghtsBigTIFF = image_data_lenghtsBigTIFF;
	}

	@Override
	public void writeIFD_entryTIFF(DataOutput out, int data_pos, int image_data_start_pos) throws IOException {
		this.image_data_posTIFF = image_data_start_pos;
		out.writeShort(id);
		out.writeShort(0x0004); //--- data type 32 bit unsigned integer
		int len = image_data_lenghtsTIFF == null ? image_data_lenghtsBigTIFF.length : image_data_lenghtsTIFF.length;
		if(len==0) {
			out.writeInt(1);  //--- element count
			out.writeInt(image_data_start_pos); //--- (not data offset): fill bytes
		} else if(len==1) {
			out.writeInt(1);  //--- element count
			out.writeInt(image_data_start_pos);//--- (not data offset): value
		} else {
			out.writeInt(len);  //--- element count
			out.writeInt(data_pos); //--- data offset
		}
	}
	
	@Override
	public void writeIFD_entryBigTIFF(DataOutput out, long data_pos, long image_data_start_pos) throws IOException {
		this.image_data_posBigTIFF = image_data_start_pos;
		out.writeShort(id);		
		out.writeShort(IFD.DATA_TYPE_UINT64);
		long len = image_data_lenghtsTIFF == null ? image_data_lenghtsBigTIFF.length : image_data_lenghtsTIFF.length;
		if(len==0) {
			out.writeLong(1);  //--- element count
			out.writeLong(image_data_start_pos); //--- (not data offset): value
		} else if(len==1) {
			out.writeLong(1);  //--- element count
			out.writeLong(image_data_start_pos);//--- (not data offset): value
		} else {
			out.writeLong(len);  //--- element count
			out.writeLong(data_pos); //--- data offset
		}
	}

	@Override
	public int data_sizeTIFF() {
		int len = image_data_lenghtsTIFF == null ? image_data_lenghtsBigTIFF.length : image_data_lenghtsTIFF.length;
		return len<2?0:len*4;
	}
	
	@Override
	public long data_sizeBigTIFF() {
		long len = image_data_lenghtsTIFF == null ? image_data_lenghtsBigTIFF.length : image_data_lenghtsTIFF.length;
		return len<2?0:len*8;
	}

	@Override
	public void write_dataTIFF(DataOutput out) throws IOException {
		long len = image_data_lenghtsTIFF == null ? image_data_lenghtsBigTIFF.length : image_data_lenghtsTIFF.length;
		if(len>1) {
			int offset = image_data_posTIFF;
			if(image_data_lenghtsTIFF == null) {
				for(long v:image_data_lenghtsBigTIFF) {
					out.writeInt(offset);
					offset += v;  // long to int conversion
				}		
			} else {
				for(int v:image_data_lenghtsTIFF) {
					out.writeInt(offset);
					offset += v;
				}	
			}			
		}
	}
	
	@Override
	public void write_dataBigTIFF(DataOutput out) throws IOException {
		long len = image_data_lenghtsTIFF == null ? image_data_lenghtsBigTIFF.length : image_data_lenghtsTIFF.length;
		if(len>1) {
			long offset = image_data_posBigTIFF;
			if(image_data_lenghtsTIFF == null) {
				for(long v:image_data_lenghtsBigTIFF) {
					out.writeLong(offset);
					offset += v;
				}		
			} else {
				for(int v:image_data_lenghtsTIFF) {   
					out.writeLong(offset);
					offset += v; // int to long conversion
				}	
			}			
		}
	}

}