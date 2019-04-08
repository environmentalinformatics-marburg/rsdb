package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

public abstract class TiffBandUint8ofInt32Byte3 extends TiffBandUint8ofInt32 {

	public TiffBandUint8ofInt32Byte3(int width, int height) {
		super(width, height);
	}
	
	@Override
	public void writeData(DataOutput out) throws IOException {
		int[] data = getData();
		int w = width;
		int h = height;
		int len = w*h;
		if(data.length != len) {
			throw new RuntimeException();
		}
		byte[] target = new byte[len];
		for(int i = (len - 1); i >= 0; i--) {
			target[i] = (byte) (0xff & (data[i] >> 24));
		}
		out.write(target);
	}	
}
