package util.tiff;

import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Supplier;

import util.image.ImageBufferARGB;

public abstract class TiffBand {
	
	public final int width;
	public final int height;
	
	public TiffBand(int width, int height) {
		this.width = width;
		this.height = height;
	}	
	
	public abstract short getBitsPerSample();
	public abstract short getSampleFormat();
	public abstract void writeData(DataOutput out) throws IOException;
	
	public final long getDataSize() {
		long w = width;
		long h = height;
		long b = getBitsPerSample() / 8;
		return w * h * b;	
	}
	
	public static TiffBandInt16 ofInt16(short[][] data) {
		return ofInt16(data[0].length, data.length, data);
	}
	
	public static TiffBandInt32 ofInt32(int[][] data) {
		return ofInt32(data[0].length, data.length, data);
	}
	
	public static TiffBandFloat32 ofFloat32(float[][] data) {
		return ofFloat32(data[0].length, data.length, data);
	}
	
	public static TiffBandFloat64 ofFloat64(double[][] data) {
		return ofFloat64(data[0].length, data.length, data);
	}
	
	public static TiffBandInt16 ofInt16(int width, int height, short[][] data) {
		return new TiffBandInt16(width, height) {			
			@Override
			protected short[][] getData() {
				return data;
			}
		};
	}
	
	public static TiffBandInt32 ofInt32(int width, int height, int[][] data) {
		return new TiffBandInt32(width, height) {			
			@Override
			protected int[][] getData() {
				return data;
			}
		};
	}
	
	public static TiffBandUint8 ofUint8(int width, int height, byte[][] data) {
		return new TiffBandUint8(width, height) {			
			@Override
			protected byte[][] getData() {
				return data;
			}
		};
	}
	
	public static TiffBandUint8ofInt32Byte0 ofUint8ofInt32Byte0(int width, int height, int[] data) {
		return new TiffBandUint8ofInt32Byte0(width, height) {			
			@Override
			protected int[] getData() {
				return data;
			}
		};
	}
	
	public static TiffBandUint8ofInt32Byte1 ofUint8ofInt32Byte1(int width, int height, int[] data) {
		return new TiffBandUint8ofInt32Byte1(width, height) {			
			@Override
			protected int[] getData() {
				return data;
			}
		};
	}
	
	public static TiffBandUint8ofInt32Byte2 ofUint8ofInt32Byte2(int width, int height, int[] data) {
		return new TiffBandUint8ofInt32Byte2(width, height) {			
			@Override
			protected int[] getData() {
				return data;
			}
		};
	}
	
	public static TiffBandUint8ofInt32Byte3 ofUint8ofInt32Byte3(int width, int height, int[] data) {
		return new TiffBandUint8ofInt32Byte3(width, height) {			
			@Override
			protected int[] getData() {
				return data;
			}
		};
	}
	
	public static TiffBandUint8ofInt32Byte3 ofImageBufferARGB_Alpha(ImageBufferARGB imageBufferARGB) {
		return ofUint8ofInt32Byte3(imageBufferARGB.width, imageBufferARGB.height, imageBufferARGB.data);
	}
	
	public static TiffBandUint8ofInt32Byte2 ofImageBufferARGB_Red(ImageBufferARGB imageBufferARGB) {
		return ofUint8ofInt32Byte2(imageBufferARGB.width, imageBufferARGB.height, imageBufferARGB.data);
	}
	
	public static TiffBandUint8ofInt32Byte1 ofImageBufferARGB_Green(ImageBufferARGB imageBufferARGB) {
		return ofUint8ofInt32Byte1(imageBufferARGB.width, imageBufferARGB.height, imageBufferARGB.data);
	}
	
	public static TiffBandUint8ofInt32Byte0 ofImageBufferARGB_Blue(ImageBufferARGB imageBufferARGB) {
		return ofUint8ofInt32Byte0(imageBufferARGB.width, imageBufferARGB.height, imageBufferARGB.data);
	}
	
	public static TiffBandFloat32 ofFloat32(int width, int height, float[][] data) {
		return new TiffBandFloat32(width, height) {			
			@Override
			protected float[][] getData() {
				return data;
			}
		};
	}
	
	public static TiffBandFloat64 ofFloat64(int width, int height, double[][] data) {
		return new TiffBandFloat64(data[0].length, data.length) {			
			@Override
			protected double[][] getData() {
				return data;
			}
		};
	}
	
	public static TiffBandFloat64 ofFloat64(int xoff, int yoff, int xlen, int ylen, double[][] data) {
		return new TiffBandFloat64Slice(xoff, yoff, xlen, ylen) {			
			@Override
			protected double[][] getData() {
				return data;
			}
		};
	}
	
	public static TiffBandInt16 ofInt16(int width, int height, Supplier<short[][]> supplier) {
		return new TiffBandInt16(width, height) {			
			@Override
			protected short[][] getData() {
				return supplier.get();
			}
		};
	}
	
	public static TiffBandInt32 ofInt32(int width, int height, Supplier<int[][]> supplier) {
		return new TiffBandInt32(width, height) {			
			@Override
			protected int[][] getData() {
				return supplier.get();
			}
		};
	}
	
	public static TiffBandUint8 ofUint8(int width, int height, Supplier<byte[][]> supplier) {
		return new TiffBandUint8(width, height) {			
			@Override
			protected byte[][] getData() {
				return supplier.get();
			}
		};
	}
	
	public static TiffBandUint8ofInt32Byte0 ofUint8ofInt32Byte0(int width, int height, Supplier<int[]> supplier) {
		return new TiffBandUint8ofInt32Byte0(width, height) {			
			@Override
			protected int[] getData() {
				return supplier.get();
			}
		};
	}
	
	public static TiffBandUint8ofInt32Byte1 ofUint8ofInt32Byte1(int width, int height, Supplier<int[]> supplier) {
		return new TiffBandUint8ofInt32Byte1(width, height) {			
			@Override
			protected int[] getData() {
				return supplier.get();
			}
		};
	}
	
	public static TiffBandUint8ofInt32Byte2 ofUint8ofInt32Byte2(int width, int height, Supplier<int[]> supplier) {
		return new TiffBandUint8ofInt32Byte2(width, height) {			
			@Override
			protected int[] getData() {
				return supplier.get();
			}
		};
	}
	
	public static TiffBandUint8ofInt32Byte3 ofUint8ofInt32Byte3(int width, int height, Supplier<int[]> supplier) {
		return new TiffBandUint8ofInt32Byte3(width, height) {			
			@Override
			protected int[] getData() {
				return supplier.get();
			}
		};
	}
	
	public static TiffBandFloat32 ofFloat32(int width, int height, Supplier<float[][]> supplier) {
		return new TiffBandFloat32(width, height) {			
			@Override
			protected float[][] getData() {
				return supplier.get();
			}
		};
	}
	
	public static TiffBandFloat64 ofFloat64(int width, int height, Supplier<double[][]> supplier) {
		return new TiffBandFloat64(width, height) {			
			@Override
			protected double[][] getData() {
				return supplier.get();
			}
		};
	}
	
	public static TiffBandFloat64 ofFloat64(int xoff, int yoff, int xlen, int ylen, Supplier<double[][]> supplier) {
		return new TiffBandFloat64Slice(xoff, yoff, xlen, ylen) {			
			@Override
			protected double[][] getData() {
				return supplier.get();
			}
		};
	}
}
