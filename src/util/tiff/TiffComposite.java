package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

import util.collections.ReadonlyList;
import util.collections.array.ReadonlyArray;
import util.image.ImageBufferARGB;

public abstract class TiffComposite {
	
	public static final ReadonlyList<String> DEFAULT_BAND_DESCRIPTIONS = new ReadonlyArray<String>(new String[] {"red", "green", "blue", "alpha"});
	
	public final int width;
	public final int height;
	public final ReadonlyList<String> bandDescriptions;
	
	public TiffComposite(int width, int height, ReadonlyList<String> bandDescriptions) {
		this.width = width;
		this.height = height;
		this.bandDescriptions = bandDescriptions;
	}
	
	public abstract short[] getBitsPerSample();
	public abstract short[] getSampleFormats();
	public abstract int getBytesPerPixel();

	public abstract void writeData(DataOutput out) throws IOException;
	
	public final long getDataSize() {
		long w = width;
		long h = height;
		long b = getBytesPerPixel();
		return w * h * b;	
	}
	
	public static TiffComposite ofImageBufferARGB(ImageBufferARGB imageBufferARGB) {
		return ofImageBufferARGB(imageBufferARGB, DEFAULT_BAND_DESCRIPTIONS);
	}
	
	public static TiffComposite ofImageBufferARGB(ImageBufferARGB imageBufferARGB, ReadonlyList<String> bandDescriptions) {
		return of4Uint8ofInt32(imageBufferARGB.width, imageBufferARGB.height, imageBufferARGB.data, bandDescriptions);
	}

	private static TiffComposite of4Uint8ofInt32(int width, int height, int[] data, ReadonlyList<String> bandDescriptions) {
		return new TiffComposite_4_uint8_int32_2_1_0_3(width, height, bandDescriptions) {			
			@Override
			protected int[] getData() {
				return data;
			}
		};
	}

}
