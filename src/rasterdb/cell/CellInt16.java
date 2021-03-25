package rasterdb.cell;

import java.nio.ByteOrder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.lemire.integercompression.IntWrapper;
import rasterdb.Band;
import rasterunit.Tile;
import util.Serialisation;

public class CellInt16 extends Cell<short[][]>{
	private static final Logger log = LogManager.getLogger();

	public CellInt16(int pixel_len) {
		super(pixel_len);
	}

	@Override
	public boolean isNotAllNaCellPixels(short[][] cellPixels, Band band) {
		short na = band.getInt16NA();
		for(int i = 0; i < pixel_len; i++) {
			short[] src = cellPixels[i];				
			for(int c = 0; c < pixel_len; c++) {
				if(src[c] != na) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public byte[] encodeCell(short[][] pixels) {		
		int[] raw = new int[cell_pixel_count];
		int destPos = 0;
		for(int i = 0; i < pixel_len; i++) {
			short[] src = pixels[i];				
			for(int c=0; c < pixel_len; c++) {
				raw[destPos++] = src[c];
			}
		}
		return enc(raw);
	}

	@Override
	public short[][] decodeCell(Tile tile) {
		int[] raw = dec(tile.data);
		short[][] cellPixels = new short[pixel_len][pixel_len];
		int srcPos = 0;
		for(int i=0; i < pixel_len; i++) {
			short[] dst = cellPixels[i];				
			for(int c = 0; c < pixel_len; c++) {
				dst[c] = (short) raw[srcPos++]; 
			}
		}
		return cellPixels;
	}

	@Override
	public void decodeCellDiv(Tile tile, int xCellStart, int yCellStart, int div, short[][] target, Band band, int xTargetStart, int yTargetStart, int xTargetEnd, int yTargetEnd) {
		short na = band.getInt16NA();
		int[] raw = dec(tile.data);
		int posTargetRow = yCellStart * pixel_len + xCellStart;
		for(int y = yTargetStart; y <= yTargetEnd; y++) {
			int posTargetPixel = posTargetRow;
			short[] dst = target[y];
			for(int x = xTargetStart; x <= xTargetEnd; x++) {
				int sum = 0;
				int cnt = 0;
				int posTileRow = posTargetPixel;
				for(int py = 0; py < div; py++) {
					int posTilePixel = posTileRow;
					for(int px = 0; px < div; px++) {
						int v = raw[posTilePixel++];
						if(v != na) {
							sum += v;
							cnt++;
						}
					}
					posTileRow += pixel_len;
				}
				dst[x] = cnt == 0 ? na : (short)(sum / cnt);
				posTargetPixel += div;
			}
			posTargetRow += pixel_len * div;
		}		
	}

	@Override	
	public void decodeCellMerge(Tile tile, short[][] cellPixels, Band band) {
		short na = band.getInt16NA();
		int[] raw = dec(tile.data);
		int srcPos = 0;
		for(int i=0; i < pixel_len; i++) {
			short[] dst = cellPixels[i];				
			for(int c = 0; c < pixel_len; c++) {
				if(dst[c] == na) {
					dst[c] = (short) raw[srcPos]; 
				}
				srcPos++;
			}
		}
	}
	
	@Override
	public void decodeCell(Tile tile, int xCellStart, int yCellStart, int xCellEnd, short[][] target, int xTargetStart, int yTargetStart, int xTargetEnd, int yTargetEnd) {
		int[] raw = dec(tile.data);
		int xCellSkip = xCellStart + ((pixel_len - 1) - xCellEnd);
		int cellPos = (yCellStart * pixel_len) + xCellStart;
		for(int y = yTargetStart; y <= yTargetEnd; y++) {
			short[] dst = target[y];				
			for(int x = xTargetStart; x <= xTargetEnd; x++) {
				dst[x] = (short) raw[cellPos++];
			}
			cellPos += xCellSkip;
		}
	}

	/*public static byte[] enc(short[] data) {
		//Serialisation.encodeDelta(data);
		Serialisation.encodeDeltaZigZag(data);
		byte[] transformed = Serialisation.shortToByteArray(data);
		//byte[] transformed = encInt16_split(data);
		//byte[] result = Zstd.compress(transformed, 1); // default
		byte[] result = Zstd.compress(transformed, 1);
		return result;
	}

	public static short[] dec(byte[] data) {
		int len = (int) Zstd.decompressedSize(data);
		byte[] inter = Zstd.decompress(data, len);
		//short[] result = decInt16_split(inter);
		short[] result = Serialisation.byteToShortArray(inter);
		//Serialisation.decodeDelta(result);
		Serialisation.decodeDeltaZigZag(result);
		return result;
	}*/

	public byte[] enc(int[] data) {
		//int[] data2 = Serialisation.castShortToInt(data);		
		Serialisation.encodeDeltaZigZag(data);
		IntWrapper inpos = new IntWrapper();
		int[] compressed_raw = new int[cell_pixel_count + 256];
		IntWrapper outpos = new IntWrapper();
		threadLocal_fastPFOR.get().headlessCompress(data, inpos, cell_pixel_count, compressed_raw, outpos);
		byte[] transformed = Serialisation.intToByteArray(compressed_raw, outpos.get());
		return transformed;
		//byte[] result = Zstd.compress(transformed, 1);
		/*try {
			byte[] result = Snappy.rawCompress(compressed_raw, outpos.get() * 4);
			return result;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		//return null;
	}

	public int[] dec(byte[] data) {
		//int len = (int) Zstd.decompressedSize(data);
		//byte[] inter = Zstd.decompress(data, len);
		//int[] inter2 = Serialisation.byteToIntArray(inter);
		//int[] inter2 = Serialisation.byteToIntArray(data);
		int SIZE_INTS = data.length/4;
		int[] inter2 = new int[SIZE_INTS];
		java.nio.ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(inter2);
		IntWrapper inpos = new IntWrapper();
		int[] result = new int[cell_pixel_count];
		IntWrapper outpos = new IntWrapper();
		threadLocal_fastPFOR.get().headlessUncompress(inter2, inpos, inter2.length, result, outpos, cell_pixel_count);
		Serialisation.decodeDeltaZigZag(result);
		return result;
		//short[] result2 = Serialisation.castIntToShort(result);
		//return result2;
	}

	@Override
	public short[][] createEmptyUninitialized(int width, int height) {
		return new short[height][width];
	}

	@Override
	public short[][] createEmptyNA(int width, int height, Band band) {
		short na = band.getInt16NA();
		short[][] data = new short[height][width];
		if(na != 0) {
			for (int y = 0; y < height; y++) {
				short[] row = data[y];
				for (int x = 0; x < width; x++) {
					row[x] = na;
				}
			}
		}
		return data;
	}

	@Override
	public void copy(short[][] pixels, int tx, int ty, short[][] tilePixels, int xStart, int txlen, int yStart, int yEnd) {
		for(int y = yStart; y <= yEnd; y++) {
			short[] targetRow = pixels[ty++];
			short[] pixelRow = tilePixels[y];
			System.arraycopy(targetRow, tx, pixelRow, xStart, txlen);
		}		
	}

	/*@Override
	public void copyDiv(short[][] data, int div, Band band, short[][] target, int xTargetMin, int yTargetMin) {
		short na = band.getInt16NA();
		int w = data[0].length;
		int h = data.length;

		int yTarget = yTargetMin;
		for(int py = 0; py < h; py += div) {
			short[] targetRow = target[yTarget++];
			int xTarget = xTargetMin;
			for(int px = 0; px < w; px += div) {
				int sum = 0;
				int cnt = 0;
				for (int by = 0; by < div; by++) {
					short[] row = data[py + by];
					for (int bx = 0; bx < div; bx++) {
						short v = row[px + bx];
						if(v != na) {
							sum += v;
							cnt++;
						}
					}
				}
				targetRow[xTarget++] = cnt == 0 ? na : (short)(sum / cnt);
			}
		}
	}*/
}
