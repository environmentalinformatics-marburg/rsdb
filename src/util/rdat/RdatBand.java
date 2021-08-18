package util.rdat;

import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Supplier;

public abstract class RdatBand {

	public final int width;
	public final int height;
	private final RdatList meta; // nullable

	/**
	 * 
	 * @param width
	 * @param height
	 * @param meta nullable
	 */
	public RdatBand(int width, int height, RdatList meta) {
		this.width = width;
		this.height = height;
		this.meta = meta;
	}

	public abstract int getType();
	public abstract int getBytesPerSample();

	public void writeMeta(DataOutput out) throws IOException {
		RdatList m = meta;
		if(m == null) {
			m = new RdatList();	
		}
		m.addInt32("flipped", 1);
		m.write(out);
	}

	public abstract void writeData(DataOutput out) throws IOException;

	public static RdatBandInt16 ofInt16(int width, int height, RdatList meta, short[][] data) {
		return new RdatBandInt16(width, height, meta) {			
			@Override
			protected short[][] getData() {
				return data;
			}
		};
	}
	
	public static RdatBandInt32 ofInt32(int width, int height, RdatList meta, int[][] data) {
		return new RdatBandInt32(width, height, meta) {			
			@Override
			protected int[][] getData() {
				return data;
			}
		};
	}
	
	public static RdatBandBool8 ofBool8(int width, int height, RdatList meta, boolean[][] data) {
		return new RdatBandBool8(width, height, meta) {			
			@Override
			protected boolean[][] getData() {
				return data;
			}
		};
	}
	
	public static RdatBandFloat32 ofFloat32(int width, int height, RdatList meta, float[][] data) {
		return new RdatBandFloat32(width, height, meta) {			
			@Override
			protected float[][] getData() {
				return data;
			}
		};
	}
	
	public static RdatBandFloat64 ofFloat64(int width, int height, RdatList meta, double[][] data) {
		return new RdatBandFloat64(width, height, meta) {			
			@Override
			protected double[][] getData() {
				return data;
			}
		};
	}
	
	
	public static RdatBandInt16 ofInt16(int width, int height, RdatList meta, Supplier<short[][]> supplier) {
		return new RdatBandInt16(width, height, meta) {			
			@Override
			protected short[][] getData() {
				return supplier.get();
			}
		};
	}
	
	public static RdatBandInt32 ofInt32(int width, int height, RdatList meta, Supplier<int[][]> supplier) {
		return new RdatBandInt32(width, height, meta) {			
			@Override
			protected int[][] getData() {
				return supplier.get();
			}
		};
	}
	
	public static RdatBandFloat32 ofFloat32(int width, int height, RdatList meta, Supplier<float[][]> supplier) {
		return new RdatBandFloat32(width, height, meta) {			
			@Override
			protected float[][] getData() {
				return supplier.get();
			}
		};
	}
	
	public static RdatBandFloat64 ofFloat64(int width, int height, RdatList meta, Supplier<double[][]> supplier) {
		return new RdatBandFloat64(width, height, meta) {			
			@Override
			protected double[][] getData() {
				return supplier.get();
			}
		};
	}



}
