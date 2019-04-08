package util.tiff;

public abstract class TiffBandUint8ofInt32 extends TiffBand {

	public TiffBandUint8ofInt32(int width, int height) {
		super(width, height);
	}
	
	protected abstract int[] getData();

	@Override
	public short getBitsPerSample() {
		return 8;
	}

	@Override
	public short getSampleFormat() {
		return 1; // unsigned integer data
	}
}
