package rasterdb.tile;

import java.io.IOException;

public class TileFloat {
	
	public static byte[] encode(float[][] pixels) {		
		try {			
			int[] raw = new int[TilePixel.PIXELS_PER_TILE];
			int destPos = 0;
			for(int i=0;i<TilePixel.PIXELS_PER_ROW;i++) {
				float[] src = pixels[i];				
				for(int c=0;c<TilePixel.PIXELS_PER_ROW;c++) {
					raw[destPos++] = Float.floatToIntBits(src[c]);
				}
			}
			byte[] bytes = TileShort.encode_raw(raw);
			return bytes;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static float[][] decode(byte[] data) {
		try {
			int[] raw = TileShort.decode_raw(data);
			float[][] pixels = new float[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
			int srcPos = 0;
			for(int i=0;i<TilePixel.PIXELS_PER_ROW;i++) {
				float[] dst = pixels[i];				
				for(int c=0;c<TilePixel.PIXELS_PER_ROW;c++) {
					dst[c] = Float.intBitsToFloat(raw[srcPos++]);
				}
			}
			return pixels;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void decode_raw(int[] raw, float[] dst) {
		for(int i=0;i<TilePixel.PIXELS_PER_TILE;i++) {
			dst[i] = Float.intBitsToFloat(raw[i]);
		}
	}
	
	public static void decode(byte[] data, float[][] target, int xmin, int ymin) {
		try {
			int[] raw = TileShort.decode_raw(data);
			int srcPos = 0;
			int xStart = xmin;
			int xEnd = xmin + TilePixel.PIXELS_PER_ROW - 1;
			int yStart = ymin;
			int yEnd = ymin + TilePixel.PIXELS_PER_ROW - 1;
			for(int y = yStart; y <= yEnd; y++) {
				float[] dst = target[y];				
				for(int x = xStart; x <= xEnd; x++) {
					dst[x] = Float.intBitsToFloat(raw[srcPos++]);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void decodeMerge(byte[] data, float[][] pixels) {
		try {
			int[] raw = TileShort.decode_raw(data);
			int srcPos = 0;
			for(int i=0;i<TilePixel.PIXELS_PER_ROW;i++) {
				float[] dst = pixels[i];				
				for(int c=0;c<TilePixel.PIXELS_PER_ROW;c++) {
					if(!Float.isFinite(dst[c])) {
						dst[c] = Float.intBitsToFloat(raw[srcPos]); 
					}
					srcPos++;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static boolean isNotAllNa(float[][] pixels) {
		for(int i=0;i<TilePixel.PIXELS_PER_ROW;i++) {
			float[] src = pixels[i];				
			for(int c=0;c<TilePixel.PIXELS_PER_ROW;c++) {
				if(Float.isFinite(src[c])) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static int countNotNa(float[][] pixels) {
		int cnt = 0;
		for(int i=0;i<TilePixel.PIXELS_PER_ROW;i++) {
			float[] src = pixels[i];				
			for(int c=0;c<TilePixel.PIXELS_PER_ROW;c++) {
				if(Float.isFinite(src[c])) {
					cnt++;
				}
			}
		}
		return cnt;
	}
	
	public static int countNotNa_raw(float[] raw) {
		int cnt = 0;
		for(int i=0;i<TilePixel.PIXELS_PER_TILE;i++) {
			if(Float.isFinite(raw[i])) {
				cnt++;
			}
		}
		return cnt;
	}
	
	public static short[][] floatToShort(float[][] sourceFloat, short[][] targetShort, short targetNA) {
		int w = sourceFloat[0].length;
		int h = sourceFloat.length;
		if(targetShort == null || targetShort.length != h || targetShort[0].length != w) {
			targetShort = new short[h][w];
		}
		for (int y = 0; y < h; y++) {
			float[] a = sourceFloat[y];
			short[] b = targetShort[y];
			for (int x = 0; x < w; x++) {
				float v = a[x];
				b[x] = Float.isFinite(v) ? (short) v : targetNA;
			}
		}
		return targetShort;
	}

	public static short[][] floatToShort(float[][] sourceFloat, short[][] targetShort, float sourceNA, short targetNA) {
		int w = sourceFloat[0].length;
		int h = sourceFloat.length;
		if(targetShort == null || targetShort.length != h || targetShort[0].length != w) {
			targetShort = new short[h][w];
		}
		for (int y = 0; y < h; y++) {
			float[] a = sourceFloat[y];
			short[] b = targetShort[y];
			for (int x = 0; x < w; x++) {
				float v = a[x];
				b[x] = Float.isFinite(v) && v != sourceNA ? (short) v : targetNA;
			}
		}
		return targetShort;
	}
	
	public static float[][] shortToFloat(short[][] sourceShort, float[][] targetFloat, short sourceNA) {
		int w = sourceShort[0].length;
		int h = sourceShort.length;
		if(targetFloat == null || targetFloat.length != h || targetFloat[0].length != w) {
			targetFloat = new float[h][w];
		}
		for (int y = 0; y < h; y++) {
			short[] a = sourceShort[y];
			float[] b = targetFloat[y];
			for (int x = 0; x < w; x++) {
				short v = a[x];
				b[x] = v != sourceNA ? (float) v : Float.NaN;
			}
		}
		return targetFloat;
	}	
	
	public static float[][] shortToFloat(short[][] sourceShort, float[][] targetFloat, short sourceNA, float targetNA) {
		int w = sourceShort[0].length;
		int h = sourceShort.length;
		if(targetFloat == null || targetFloat.length != h || targetFloat[0].length != w) {
			targetFloat = new float[h][w];
		}
		for (int y = 0; y < h; y++) {
			short[] a = sourceShort[y];
			float[] b = targetFloat[y];
			for (int x = 0; x < w; x++) {
				short v = a[x];
				b[x] = v != sourceNA ? (float) v : targetNA;
			}
		}
		return targetFloat;
	}	
}
