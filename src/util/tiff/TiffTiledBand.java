package util.tiff;

import java.util.Iterator;
import java.util.function.Supplier;

public abstract class TiffTiledBand extends TiffBand {
	
	public final int tileWidth;
	public final int tileHeight;

	public TiffTiledBand(int width, int height, int tileWidth, int tileHeight) {
		super(width, height);
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
	
	public static TiffTiledBandInt16 ofInt16Iterator(int width, int height, int tileWidth, int tileHeight, Supplier<Iterator<short[][]>> supplier) {
		return new TiffTiledBandInt16(width, height, tileWidth, tileHeight) {
			@Override
			protected Iterator<short[][]> getTiles() {
				return supplier.get();
			}			
		};
	}
	
	public static TiffTiledBandFloat32 ofFloat32Iterator(int width, int height, int tileWidth, int tileHeight, Supplier<Iterator<float[][]>> supplier) {
		return new TiffTiledBandFloat32(width, height, tileWidth, tileHeight) {
			@Override
			protected Iterator<float[][]> getTiles() {
				return supplier.get();
			}			
		};
	}
}
