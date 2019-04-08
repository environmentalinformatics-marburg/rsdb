package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

public abstract class TiffComposite_4_uint8_int32_2_1_0_3 extends TiffComposite_4_uint8 {

	public TiffComposite_4_uint8_int32_2_1_0_3(int width, int height) {
		super(width, height);//short[] bitsPerSample
	}

	protected abstract int[] getData();

	@Override
	public void writeData(DataOutput out) throws IOException {
		int[] data = getData();
		int w = width;
		int h = height;
		int len = w*h;
		if(data.length != len) {
			throw new RuntimeException();
		}
		byte[] target = new byte[len*4];
		int pos = 0;
		for(int i = 0; i < len; i++) {
			int v = data[i];
			target[pos] = (byte) (0xff & (v >> 16));
			target[pos + 1] = (byte) (0xff & (v >> 8));
			target[pos + 2] = (byte) (0xff & v);
			target[pos + 3] = (byte) (0xff & (v >> 24));
			pos += 4;
		}
		out.write(target);
	}	

}
