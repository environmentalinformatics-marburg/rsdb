package rasterdb.cell;

import java.nio.ByteOrder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.lemire.integercompression.IntWrapper;
import rasterdb.Band;
import rasterunit.Tile;
import util.Serialisation;

public class CellInt8 extends Cell<byte[][]>{
	private static final Logger log = LogManager.getLogger();

	public CellInt8(int pixel_len) {
		super(pixel_len);
	}

	@Override
	public boolean isNotAllNaCellPixels(byte[][] cellPixels, Band band) {
		byte na = band.getInt8NA();
		for(int i = 0; i < pixel_len; i++) {
			byte[] src = cellPixels[i];				
			for(int c = 0; c < pixel_len; c++) {
				if(src[c] != na) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public byte[] encodeCell(byte[][] pixels) {		
		int[] raw = new int[cell_pixel_count];
		int destPos = 0;
		for(int i = 0; i < pixel_len; i++) {
			byte[] src = pixels[i];				
			for(int c=0; c < pixel_len; c++) {
				raw[destPos++] = src[c];
			}
		}
		return enc(raw);
	}

	@Override
	public byte[][] decodeCell(Tile tile) {
		int[] raw = dec(tile.data);
		byte[][] cellPixels = new byte[pixel_len][pixel_len];
		int srcPos = 0;
		for(int i=0; i < pixel_len; i++) {
			byte[] dst = cellPixels[i];				
			for(int c = 0; c < pixel_len; c++) {
				dst[c] = (byte) raw[srcPos++]; 
			}
		}
		return cellPixels;
	}

	@Override
	public void decodeCellDiv(Tile tile, int xCellStart, int yCellStart, int div, byte[][] target, Band band, int xTargetStart, int yTargetStart, int xTargetEnd, int yTargetEnd) {
		byte na = band.getInt8NA();
		int[] raw = dec(tile.data);
		int posTargetRow = yCellStart * pixel_len + xCellStart;
		for(int y = yTargetStart; y <= yTargetEnd; y++) {
			int posTargetPixel = posTargetRow;
			byte[] dst = target[y];
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
				dst[x] = cnt == 0 ? na : (byte)(sum / cnt);
				posTargetPixel += div;
			}
			posTargetRow += pixel_len * div;
		}		
	}

	@Override	
	public void decodeCellMerge(Tile tile, byte[][] cellPixels, Band band) {
		byte na = band.getInt8NA();
		int[] raw = dec(tile.data);
		int srcPos = 0;
		for(int i=0; i < pixel_len; i++) {
			byte[] dst = cellPixels[i];				
			for(int c = 0; c < pixel_len; c++) {
				if(dst[c] == na) {
					dst[c] = (byte) raw[srcPos]; 
				}
				srcPos++;
			}
		}
	}
	
	@Override
	public void decodeCell(Tile tile, int xCellStart, int yCellStart, int xCellEnd, byte[][] target, int xTargetStart, int yTargetStart, int xTargetEnd, int yTargetEnd) {
		int[] raw = dec(tile.data);
		int xCellSkip = xCellStart + ((pixel_len - 1) - xCellEnd);
		int cellPos = (yCellStart * pixel_len) + xCellStart;
		for(int y = yTargetStart; y <= yTargetEnd; y++) {
			byte[] dst = target[y];				
			for(int x = xTargetStart; x <= xTargetEnd; x++) {
				dst[x] = (byte) raw[cellPos++];
			}
			cellPos += xCellSkip;
		}
	}

	/*public static byte[] enc(byte[] data) {
		//Serialisation.encodeDelta(data);
		Serialisation.encodeDeltaZigZag(data);
		byte[] transformed = Serialisation.byteToByteArray(data);
		//byte[] transformed = encInt16_split(data);
		//byte[] result = Zstd.compress(transformed, 1); // default
		byte[] result = Zstd.compress(transformed, 1);
		return result;
	}

	public static byte[] dec(byte[] data) {
		int len = (int) Zstd.decompressedSize(data);
		byte[] inter = Zstd.decompress(data, len);
		//byte[] result = decInt16_split(inter);
		byte[] result = Serialisation.byteToByteArray(inter);
		//Serialisation.decodeDelta(result);
		Serialisation.decodeDeltaZigZag(result);
		return result;
	}*/

	public byte[] enc(int[] data) {
		//int[] data2 = Serialisation.castByteToInt(data);		
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
		/*int[] inter2 = null;
		try {
			inter2 = Snappy.uncompressIntArray(data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		IntWrapper inpos = new IntWrapper();
		int[] result = new int[cell_pixel_count];
		IntWrapper outpos = new IntWrapper();
		threadLocal_fastPFOR.get().headlessUncompress(inter2, inpos, inter2.length, result, outpos, cell_pixel_count);
		Serialisation.decodeDeltaZigZag(result);
		return result;
		//byte[] result2 = Serialisation.castIntToByte(result);
		//return result2;
	}

	@Override
	public byte[][] createEmptyUninitialized(int width, int height) {
		return new byte[height][width];
	}

	@Override
	public byte[][] createEmptyNA(int width, int height, Band band) {
		byte na = band.getInt8NA();
		byte[][] data = new byte[height][width];
		if(na != 0) {
			for (int y = 0; y < height; y++) {
				byte[] row = data[y];
				for (int x = 0; x < width; x++) {
					row[x] = na;
				}
			}
		}
		return data;
	}

	@Override
	public void copy(byte[][] pixels, int tx, int ty, byte[][] tilePixels, int xStart, int txlen, int yStart, int yEnd) {
		for(int y = yStart; y <= yEnd; y++) {
			byte[] targetRow = pixels[ty++];
			byte[] pixelRow = tilePixels[y];
			System.arraycopy(targetRow, tx, pixelRow, xStart, txlen);
		}		
	}
}
