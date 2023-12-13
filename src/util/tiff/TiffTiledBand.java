package util.tiff;

import java.util.Iterator;
import java.util.function.Supplier;

public abstract class TiffTiledBand extends TiffBand {
	
	public final int tileWidth;
	public final int tileHeight;

	public TiffTiledBand(int width, int height, int tileWidth, int tileHeight, String description) {
		super(width, height, description);
		if(width % tileWidth != 0) {
			throw new RuntimeException("width is no multiple of tileWidth: " + width + "  " + tileWidth);
		}
		if(height % tileHeight != 0) {
			throw new RuntimeException("height is no multiple of tileHeight: " + height + "  " + tileHeight);
		}
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
	}
	
	public long getSizePerTile() {
		long w = tileWidth;
		long h = tileHeight;
		long b = getBitsPerSample() / 8;
		return w * h * b;	
	}
	
	public int getTileCountWidth() {
		return width / tileWidth;
	}
	
	public int getTileCountHeight() {
		return height / tileHeight;
	}
	
	public int getTileCount() {
		return  Math.multiplyExact(getTileCountWidth(), getTileCountHeight());
	}
	
	public long[] getTileSizes() {
		long sizePerTile = getSizePerTile();
		int tileCount = getTileCount();
		long[] tileSizes = new long[tileCount];
		for (int i = 0; i < tileCount; i++) {
			tileSizes[i] = sizePerTile;
		}
		return tileSizes;
	}
	
	/**
	 * Not usable, because GDAL interprets tiff int8 as uint 8.
	 *
	 */
	public static TiffTiledBandInt8 ofInt8Iterator(int width, int height, int tileWidth, int tileHeight, Supplier<Iterator<byte[][]>> supplier, String description) {
		return new TiffTiledBandInt8(width, height, tileWidth, tileHeight, description) {
			@Override
			protected Iterator<byte[][]> getTiles() {
				return supplier.get();
			}			
		};
	}
	
	public static TiffTiledBandUint8 ofUint8Iterator(int width, int height, int tileWidth, int tileHeight, Supplier<Iterator<byte[][]>> supplier, String description) {
		return new TiffTiledBandUint8(width, height, tileWidth, tileHeight, description) {
			@Override
			protected Iterator<byte[][]> getTiles() {
				return supplier.get();
			}			
		};
	}
	
	public static TiffTiledBandInt16 ofInt16Iterator(int width, int height, int tileWidth, int tileHeight, Supplier<Iterator<short[][]>> supplier, String description) {
		return new TiffTiledBandInt16(width, height, tileWidth, tileHeight, description) {
			@Override
			protected Iterator<short[][]> getTiles() {
				return supplier.get();
			}			
		};
	}
	
	public static TiffTiledBandInt32 ofInt32Iterator(int width, int height, int tileWidth, int tileHeight, Supplier<Iterator<int[][]>> supplier, String description) {
		return new TiffTiledBandInt32(width, height, tileWidth, tileHeight, description) {
			@Override
			protected Iterator<int[][]> getTiles() {
				return supplier.get();
			}			
		};
	}
	
	public static TiffTiledBandFloat32 ofFloat32Iterator(int width, int height, int tileWidth, int tileHeight, Supplier<Iterator<float[][]>> supplier, String description) {
		return new TiffTiledBandFloat32(width, height, tileWidth, tileHeight, description) {
			@Override
			protected Iterator<float[][]> getTiles() {
				return supplier.get();
			}			
		};
	}
	
	public static TiffTiledBandFloat64 ofFloat64Iterator(int width, int height, int tileWidth, int tileHeight, Supplier<Iterator<double[][]>> supplier, String description) {
		return new TiffTiledBandFloat64(width, height, tileWidth, tileHeight, description) {
			@Override
			protected Iterator<double[][]> getTiles() {
				return supplier.get();
			}			
		};
	}
}
