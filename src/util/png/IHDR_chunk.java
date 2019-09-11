package util.png;

import java.io.IOException;

class IHDR_chunk extends PNGchunk {

	public final int width;
	public final int height;
	public final byte bit_depth;
	public final byte color_type;
	public final byte compression_method;
	public final byte filter_method;
	public final byte interlace_method;

	public IHDR_chunk(int width, int height, byte bit_depth, byte color_type, byte compression_method, byte filter_method, byte interlace_method) {
		super(13, "IHDR");
		this.width = width;
		this.height = height;
		this.bit_depth = bit_depth;
		this.color_type = color_type;
		this.compression_method = compression_method;
		this.filter_method = filter_method;
		this.interlace_method = interlace_method;
	}

	public IHDR_chunk(int width, int height, byte bit_depth, byte color_type, byte interlace_method) {
		this(width, height, bit_depth, color_type, (byte) 0, (byte) 0, interlace_method);
	}

	public IHDR_chunk(int width, int height, byte bit_depth, byte color_type) {
		this(width, height, bit_depth, color_type, (byte) 0);
	}

	public static IHDR_chunk ofRGBA(int width, int height) {
		return new IHDR_chunk(width, height, (byte) 8, (byte) 6);
	}

	@Override
	protected void chunkWrite(CRC32DataOutput out) throws IOException {
		out.writeInt(width);
		out.writeInt(height);
		out.writeByte(bit_depth);
		out.writeByte(color_type);
		out.writeByte(compression_method);
		out.writeByte(filter_method);
		out.writeByte(interlace_method);			
	}		
}