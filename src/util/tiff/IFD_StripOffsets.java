package util.tiff;

public class IFD_StripOffsets extends IFD_Data_Offsets {

	public IFD_StripOffsets(int... image_data_lenghtsTIFF) {
		super((short) 0x111, image_data_lenghtsTIFF);
	}
	
	public IFD_StripOffsets(long... image_data_lenghtsBigTIFF) {
		super((short) 0x111, image_data_lenghtsBigTIFF);
	}
}