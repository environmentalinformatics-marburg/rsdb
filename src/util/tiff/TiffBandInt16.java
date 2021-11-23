package util.tiff;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.zip.Deflater;

import com.github.luben.zstd.ZstdOutputStream;

import util.Serialisation;

public abstract class TiffBandInt16 extends TiffBand {

	public TiffBandInt16(int width, int height, String description) {
		super(width, height, description);
	}

	protected abstract short[][] getData();

	@Override
	public short getBitsPerSample() {
		return 16;
	}

	@Override
	public short getSampleFormat() {
		return 2; // two’s complement signed integer data
	}

	@Override
	public void writeData(DataOutput out) throws IOException {
		writeData(out, getData(), width, height);
	}

	public static void writeData(DataOutput out, short[][] data, int width, int height) throws IOException {
		if(data.length != height) {
			throw new RuntimeException("data.length = " + data.length + " expected height = " + height);
		}
		byte[] target = null;
		for(int y = (height - 1); y >= 0; y--) {
			short[] row = data[y];
			if(row.length != width) {
				throw new RuntimeException("row.length  = " + row.length  + " expected width = " +width);
			}
			target = Serialisation.shortToByteArrayBigEndian(row, target);
			out.write(target);
		}
	}

	public static void writeDataDeflate(DataOutput out, short[][] data, int width, int height) throws IOException {
		if(data.length != height) {
			throw new RuntimeException("data.length = " + data.length + " expected height = " + height);
		}
		byte[] deflateOutput = new byte[1024*1024];
		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, false);
		//Deflater deflater = new Deflater(Deflater.BEST_SPEED, false);

		byte[] target = null;
		boolean isFollowing = false;
		for(int y = (height - 1); y >= 0; y--) {
			if(isFollowing) {				
				while(true) {
					int writtenByteCount = deflater.deflate(deflateOutput);
					if(writtenByteCount == 0) {
						break;
					}
					out.write(deflateOutput, 0, writtenByteCount);
				}
			}
			short[] row = data[y];
			if(row.length != width) {
				throw new RuntimeException("row.length  = " + row.length  + " expected width = " +width);
			}
			target = Serialisation.shortToByteArrayBigEndian(row, target);
			deflater.setInput(target);
			isFollowing = true;
		}		
		deflater.finish();
		while(true) {
			int writtenByteCount = deflater.deflate(deflateOutput);
			if(writtenByteCount == 0) {
				break;
			}
			out.write(deflateOutput, 0, writtenByteCount);
		}
		deflater.end();
	}

	public static void writeDataZSTD(DataOutput out, short[][] data, int width, int height) throws IOException {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		//try(ZstdOutputStream zstdOut = new ZstdOutputStream(byteOut, 1)) {
		try(ZstdOutputStream zstdOut = new ZstdOutputStream(byteOut)) {
		//try(ZstdOutputStream zstdOut = new ZstdOutputStream(byteOut, 100)) {
			if(data.length != height) {
				throw new RuntimeException("data.length = " + data.length + " expected height = " + height);
			}
			byte[] target = null;
			for(int y = (height - 1); y >= 0; y--) {
				short[] row = data[y];
				if(row.length != width) {
					throw new RuntimeException("row.length  = " + row.length  + " expected width = " +width);
				}
				target = Serialisation.shortToByteArrayBigEndian(row, target);
				zstdOut.write(target);
			}
		}
		out.write(byteOut.toByteArray());
	}
	
	public static void writeDataDiffZSTD(DataOutput out, short[][] data, int width, int height) throws IOException {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		//try(ZstdOutputStream zstdOut = new ZstdOutputStream(byteOut)) {
		try(ZstdOutputStream zstdOut = new ZstdOutputStream(byteOut, 100)) {
			if(data.length != height) {
				throw new RuntimeException("data.length = " + data.length + " expected height = " + height);
			}
			byte[] target = null;
			for(int y = (height - 1); y >= 0; y--) {
				short[] row = data[y];
				if(row.length != width) {
					throw new RuntimeException("row.length  = " + row.length  + " expected width = " +width);
				}
				target = Serialisation.shortToDiffByteArrayBigEndian(row, target);
				zstdOut.write(target);
			}
		}
		out.write(byteOut.toByteArray());
	}
}
