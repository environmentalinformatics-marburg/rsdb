package rasterdb.cell;

import java.nio.ByteOrder;

import com.github.luben.zstd.Zstd;

import me.lemire.integercompression.IntWrapper;
import rasterdb.Band;
import rasterunit.Tile;
import util.Serialisation;

public class CellUint16 extends Cell<char[][]>{	

	public CellUint16(int pixel_len) {
		super(pixel_len);
	}

	@Override
	public boolean isNotAllNaCellPixels(char[][] cellPixels, Band band) {
		char na = band.getUint16NA();
		for(int i = 0; i < pixel_len; i++) {
			char[] src = cellPixels[i];				
			for(int c = 0; c < pixel_len; c++) {
				if(src[c] != na) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public byte[] encodeCell(char[][] pixels) {		
		int[] raw = new int[cell_pixel_count];
		int destPos = 0;
		for(int i = 0; i < pixel_len; i++) {
			char[] src = pixels[i];				
			for(int c=0; c < pixel_len; c++) {
				raw[destPos++] = src[c];
			}
		}
		return enc(raw);
	}

	@Override
	public char[][] decodeCell(Tile tile) {
		int[] raw = dec(tile.data);
		char[][] cellPixels = new char[pixel_len][pixel_len];
		int srcPos = 0;
		for(int i=0; i < pixel_len; i++) {
			char[] dst = cellPixels[i];				
			for(int c = 0; c < pixel_len; c++) {
				dst[c] = (char) raw[srcPos++]; 
			}
		}
		return cellPixels;
	}

	@Override
	public void decodeCellDiv(Tile tile, int xCellStart, int yCellStart, int div, char[][] target, Band band, int xTargetStart, int yTargetStart, int xTargetEnd, int yTargetEnd) {
		char na = band.getUint16NA();
		int[] raw = dec(tile.data);
		int posTargetRow = yCellStart * pixel_len + xCellStart;
		for(int y = yTargetStart; y <= yTargetEnd; y++) {
			int posTargetPixel = posTargetRow;
			char[] dst = target[y];
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
				dst[x] = cnt == 0 ? na : (char)(sum / cnt);
				posTargetPixel += div;
			}
			posTargetRow += pixel_len * div;
		}		
	}

	@Override	
	public void decodeCellMerge(Tile tile, char[][] cellPixels, Band band) {
		char na = band.getUint16NA();
		int[] raw = dec(tile.data);
		int srcPos = 0;
		for(int i=0; i < pixel_len; i++) {
			char[] dst = cellPixels[i];				
			for(int c = 0; c < pixel_len; c++) {
				if(dst[c] == na) {
					dst[c] = (char) raw[srcPos]; 
				}
				srcPos++;
			}
		}
	}
	
	@Override
	public void decodeCell(Tile tile, int xCellStart, int yCellStart, int xCellEnd, char[][] target, int xTargetStart, int yTargetStart, int xTargetEnd, int yTargetEnd) {
		int[] raw = dec(tile.data);
		int xCellSkip = xCellStart + ((pixel_len - 1) - xCellEnd);
		int cellPos = (yCellStart * pixel_len) + xCellStart;
		for(int y = yTargetStart; y <= yTargetEnd; y++) {
			char[] dst = target[y];				
			for(int x = xTargetStart; x <= xTargetEnd; x++) {
				dst[x] = (char) raw[cellPos++];
			}
			cellPos += xCellSkip;
		}
	}

	public byte[] enc(int[] data) {	
		Serialisation.encodeDeltaZigZag(data);
		IntWrapper inpos = new IntWrapper();
		int[] compressed_raw = new int[cell_pixel_count + 256];
		IntWrapper outpos = new IntWrapper();
		threadLocal_fastPFOR.get().headlessCompress(data, inpos, cell_pixel_count, compressed_raw, outpos);
		byte[] transformed = Serialisation.intToByteArray(compressed_raw, outpos.get());
		return transformed;
		/*return Zstd.compress(transformed);*/
	}

	public int[] dec(byte[] data) {
		/*int original_size = (int) Zstd.getFrameContentSize(data);
		if(original_size <= 0) {
			throw new RuntimeException("Decompress error: " + original_size);
		}
		byte[] transformed = Zstd.decompress(data, original_size);
		int[] compressed_raw = Serialisation.byteToIntArray(transformed);*/
		int[] compressed_raw = Serialisation.byteToIntArray(data);
		IntWrapper inpos = new IntWrapper();
		int[] result = new int[cell_pixel_count];
		IntWrapper outpos = new IntWrapper();
		threadLocal_fastPFOR.get().headlessUncompress(compressed_raw, inpos, compressed_raw.length, result, outpos, cell_pixel_count);
		if(outpos.intValue() != cell_pixel_count) {
			throw new RuntimeException("dec error");
		}
		Serialisation.decodeDeltaZigZag(result);
		return result;
	}

	@Override
	public char[][] createEmptyUninitialized(int width, int height) {
		return new char[height][width];
	}

	@Override
	public char[][] createEmptyNA(int width, int height, Band band) {
		char na = band.getUint16NA();
		char[][] data = new char[height][width];
		if(na != 0) {
			for (int y = 0; y < height; y++) {
				char[] row = data[y];
				for (int x = 0; x < width; x++) {
					row[x] = na;
				}
			}
		}
		return data;
	}

	@Override
	public void copy(char[][] pixels, int tx, int ty, char[][] tilePixels, int xStart, int txlen, int yStart, int yEnd) {
		for(int y = yStart; y <= yEnd; y++) {
			char[] targetRow = pixels[ty++];
			char[] pixelRow = tilePixels[y];
			System.arraycopy(targetRow, tx, pixelRow, xStart, txlen);
		}		
	}
}
