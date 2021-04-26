package util.tiff;

import util.collections.ReadonlyList;

public abstract class TiffComposite_4_uint8 extends TiffComposite {

	public TiffComposite_4_uint8(int width, int height, ReadonlyList<String> bandDescriptions) {
		super(width, height, bandDescriptions);
	}
	
	@Override
	public short[] getBitsPerSample() {
		return new short[] {8, 8, 8, 8};
	}

	@Override
	public short[] getSampleFormats() {
		return new short[] {1, 1, 1, 1}; // unsigned integer data
	}

	@Override
	public int getBytesPerPixel() {
		return 4;
	}

}
