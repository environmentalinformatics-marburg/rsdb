package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

import util.image.ImageBufferARGB;

public abstract class TiffComposite {
	
	public final int width;
	public final int height;
	
	public TiffComposite(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public abstract short[] getBitsPerSample();
	public abstract short[] getSampleFormats();
	public abstract int getBytesPerPixel();

	public abstract void writeData(DataOutput out) throws IOException;
	
	public static TiffComposite ofImageBufferARGB(ImageBufferARGB imageBufferARGB) {
		return of4Uint8ofInt32(imageBufferARGB.width, imageBufferARGB.height, imageBufferARGB.data);
	}

	private static TiffComposite of4Uint8ofInt32(int width, int height, int[] data) {
		return new TiffComposite_4_uint8_int32_2_1_0_3(width, height) {			
			@Override
			protected int[] getData() {
				return data;
			}
		};
	}

}
