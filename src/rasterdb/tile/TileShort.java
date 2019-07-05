package rasterdb.tile;

import java.io.IOException;

import org.xerial.snappy.Snappy;

import me.lemire.integercompression.FastPFOR;
import me.lemire.integercompression.IntWrapper;
import util.Serialisation;

public class TileShort {

	private static ThreadLocal<FastPFOR> threadLocal_fastPFOR = new ThreadLocal<FastPFOR>() {
		@Override
		protected FastPFOR initialValue() {
			return new FastPFOR();
		}		
	};	

	private static int DECODER_BUFFER_SIZE = TilePixel.PIXELS_PER_TILE + 256;

	public static byte[] encode_raw(int[] raw) throws IOException {
		Serialisation.encodeDeltaZigZag(raw);
		IntWrapper inpos = new IntWrapper();
		int[] compressed_raw = new int[DECODER_BUFFER_SIZE];
		IntWrapper outpos = new IntWrapper();
		threadLocal_fastPFOR.get().headlessCompress(raw, inpos, TilePixel.PIXELS_PER_TILE, compressed_raw, outpos);
		byte[] bytes = Snappy.rawCompress(compressed_raw, outpos.get() * 4);
		return bytes;
	}

	public static int[] decode_raw(byte[] data) throws IOException {
		int[] raw_compressed = Snappy.uncompressIntArray(data);
		IntWrapper inpos = new IntWrapper();
		int[] raw = new int[TilePixel.PIXELS_PER_TILE];
		IntWrapper outpos = new IntWrapper();
		threadLocal_fastPFOR.get().headlessUncompress(raw_compressed, inpos, raw_compressed.length, raw, outpos, TilePixel.PIXELS_PER_TILE);
		Serialisation.decodeDeltaZigZag(raw);
		return raw;
	}

	public static void decode_raw(byte[] data, int[] target) throws IOException {
		int[] raw_compressed = Snappy.uncompressIntArray(data);
		IntWrapper inpos = new IntWrapper();
		IntWrapper outpos = new IntWrapper();
		threadLocal_fastPFOR.get().headlessUncompress(raw_compressed, inpos, raw_compressed.length, target, outpos, TilePixel.PIXELS_PER_TILE);
		Serialisation.decodeDeltaZigZag(target);
	}

	public static byte[] encode(short[][] pixels) {		
		try {
			int[] raw = new int[TilePixel.PIXELS_PER_TILE];
			int destPos = 0;
			for(int i=0;i<TilePixel.PIXELS_PER_ROW;i++) {
				short[] src = pixels[i];				
				for(int c=0;c<TilePixel.PIXELS_PER_ROW;c++) {
					raw[destPos++] = src[c];
				}
			}
			return encode_raw(raw);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static short[][] decode(byte[] data) {
		try {
			int[] raw = decode_raw(data);		
			short[][] pixels = new short[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
			int srcPos = 0;
			for(int i=0;i<TilePixel.PIXELS_PER_ROW;i++) {
				short[] dst = pixels[i];				
				for(int c=0;c<TilePixel.PIXELS_PER_ROW;c++) {
					dst[c] = (short) raw[srcPos++];
				}
			}
			return pixels;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void decode(byte[] data, short[][] target, int xmin, int ymin) {
		try {
			int[] raw = decode_raw(data);
			int srcPos = 0;
			int xStart = xmin;
			int xEnd = xmin + TilePixel.PIXELS_PER_ROW - 1;
			int yStart = ymin;
			int yEnd = ymin + TilePixel.PIXELS_PER_ROW - 1;
			for(int y = yStart; y <= yEnd; y++) {
				short[] dst = target[y];				
				for(int x = xStart; x <= xEnd; x++) {
					dst[x] = (short) raw[srcPos++];
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void decodeMerge(byte[] data, short[][] pixels, short na) {
		try {
			int[] raw = decode_raw(data);
			int srcPos = 0;
			for(int i=0;i<TilePixel.PIXELS_PER_ROW;i++) {
				short[] dst = pixels[i];				
				for(int c=0;c<TilePixel.PIXELS_PER_ROW;c++) {
					if(dst[c] == na) {
						dst[c] = (short) raw[srcPos]; 
					}
					srcPos++;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isNotAllNa(short[][] pixels, short na) {
		for(int i=0;i<TilePixel.PIXELS_PER_ROW;i++) {
			short[] src = pixels[i];				
			for(int c=0;c<TilePixel.PIXELS_PER_ROW;c++) {
				if(src[c] != na) {
					return true;
				}
			}
		}
		return false;
	}

	public static int countNotNa(short[][] pixels, short na) {
		int cnt = 0;
		for(int i=0;i<TilePixel.PIXELS_PER_ROW;i++) {
			short[] src = pixels[i];				
			for(int c=0;c<TilePixel.PIXELS_PER_ROW;c++) {
				if(src[c] != na) {
					cnt++;
				}
			}
		}
		return cnt;
	}

	public static int countNotNa_raw(int[] raw, int na) {
		int cnt = 0;
		for(int i=0;i<TilePixel.PIXELS_PER_TILE;i++) {
			//System.out.println(raw[i]);
			if(raw[i] != na) {
				cnt++;
			}
		}
		return cnt;
	}

	public static void convertNA(short[][] pixels, short srcNA, short dstNA) {
		for(int i=0;i<TilePixel.PIXELS_PER_ROW;i++) {
			short[] src = pixels[i];				
			for(int c=0;c<TilePixel.PIXELS_PER_ROW;c++) {
				/*if(src[c] != 255) {
					System.out.println(src[c]);
				}*/
				if(src[c] == srcNA) {
					src[c] = dstNA;
				}
			}
		}
	}
}
