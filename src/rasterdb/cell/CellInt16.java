package rasterdb.cell;

import java.io.IOException;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.luben.zstd.Zstd;

import rasterdb.Band;
import rasterdb.tile.TilePixel;
import rasterdb.tile.Processing.Commiter;
import rasterunit.BandKey;
import rasterunit.RasterUnitStorage;
import rasterunit.Tile;
import rasterunit.TileKey;
import util.Range2d;

public class CellInt16 {
	private static final Logger log = LogManager.getLogger();

	public final int pixel_len;
	public final int cell_pixel_count;

	public CellInt16(int pixel_len) {
		this.pixel_len = pixel_len;
		this.cell_pixel_count = pixel_len * pixel_len;
	}

	public short[][] read(RasterUnitStorage storage, int t, Band band, Range2d pixelRange, int div) {
		log.info("read " + pixelRange);
		if(div < 1 || pixel_len / div < 1 || pixel_len % div != 0) {
			throw new RuntimeException("invalid div for pixel_len   " + pixel_len + " / " + div);
		}
		if(div == 1) {
			return read(storage, t, band, pixelRange);
		} else {
			short[][] data = read(storage, t, band, pixelRange);
			short na = band.getInt16NA();
			return div(data, pixelRange, div, na);
		}
	}

	public static short[][] div(short[][] data, Range2d pixelRange, int div, short na) {
		int w = pixelRange.getWidth();
		int h = pixelRange.getHeight();
		int wr = w / div;
		int hr = h / div;
		short[][] target = new short[hr][wr];
		log.info("w h " + w +"  " + h +  " div  " + div  +  "   " + wr + "  " + hr + "   " + w % div + "   " + h % div);

		int yTarget = 0;
		for(int py = 0; py < h; py += div) {
			short[] targetRow = target[yTarget++];
			int xTarget = 0;
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
		return target;
	}	

	public static void copyDiv(short[][] data, int div, short na, short[][] target, int xTargetMin, int yTargetMin) {
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
	}

	public short[][] read(RasterUnitStorage storage, int t, Band band, Range2d pixelRange) {
		short na = band.getInt16NA();
		short[][] result = createEmpty(pixelRange.getWidth(), pixelRange.getHeight(), na);
		int tymin = pixelToCell(pixelRange.ymin);
		int tymax = pixelToCell(pixelRange.ymax);
		int txmin = pixelToCell(pixelRange.xmin); 
		int txmax = pixelToCell(pixelRange.xmax);
		Collection<Tile> tiles = storage.readTiles(t, band.index, tymin, tymax, txmin, txmax);
		for(Tile tile:tiles) {
			decodeCell(tile, result, pixelRange);
		}
		return result;	
	}


	public int pixelToCell(int p) {
		return Math.floorDiv(p, pixel_len);
	}

	public int cellToPixelMin(int t) {
		return t * pixel_len;
	}

	public int cellToPixelMax(int t) {
		return t * pixel_len + pixel_len - 1;
	}

	public int writeMerge(RasterUnitStorage rasterUnit, int t, Band band, short[][] pixels, int pixelYmin, int pixelXmin) throws IOException {
		int tileWriteCount = 0;
		int xlen = pixels[0].length;
		int ylen = pixels.length;
		int pixelXmax = pixelXmin + xlen - 1;
		int pixelYmax = pixelYmin + ylen - 1;
		int tileXmin = pixelToCell(pixelXmin);
		int tileYmin = pixelToCell(pixelYmin);
		int tileXmax = pixelToCell(pixelXmax);
		int tileYmax = pixelToCell(pixelYmax);
		short na = band.getInt16NA();
		for(int tileY = tileYmin; tileY <= tileYmax; tileY++) {
			for(int tileX = tileXmin; tileX <= tileXmax; tileX++) {
				int xMin = cellToPixelMin(tileX) - pixelXmin;
				int yMin = cellToPixelMin(tileY) - pixelYmin;
				int xStart = 0;
				if(xMin < 0) {
					xStart -= xMin;
				}
				int xEnd = pixel_len - 1;
				if(xMin + pixel_len - 1 > xlen - 1) {
					xEnd = xlen - 1 - xMin;
				}
				int yStart = 0;
				if(yMin < 0) {
					yStart -= yMin;
				}
				int yEnd = pixel_len - 1;
				if(yMin + pixel_len - 1 > ylen - 1) {
					yEnd = ylen - 1 - yMin;
				}
				short[][] tilePixels = createEmpty(pixel_len, pixel_len, na); // na fill: not all pixels may be written
				int ty = yMin + yStart;
				int tx = xMin + xStart;
				int txlen = xEnd - xStart + 1;
				for(int y = yStart; y <= yEnd; y++) {
					short[] targetRow = pixels[ty++];
					short[] pixelRow = tilePixels[y];
					System.arraycopy(targetRow, tx, pixelRow, xStart, txlen);
				}
				if(writeCellMerge(rasterUnit, t, band, tileY, tileX, tilePixels)) {
					tileWriteCount++;
				}
			}
		}
		return tileWriteCount;
	}

	public boolean writeCellMerge(RasterUnitStorage storage, int t, Band band, int y, int x, short[][] cellPixels) throws IOException {
		short na = band.getInt16NA();
		if(isNotAllNaCellPixels(cellPixels, na)) {
			TileKey tileKey = new TileKey(t, band.index, y, x);
			if(storage.tileKeysReadonly().contains(tileKey)) {
				Tile tile = storage.readTile(tileKey);
				if(tile != null) {
					decodeCellMerge(tile.data, cellPixels, na);
				}
			}
			byte[] data = encodeCell(cellPixels);
			storage.writeTile(new Tile(tileKey, band.type, data));
			return true;
		} else {
			return false;
		}
	}

	public boolean isNotAllNaCellPixels(short[][] cellPixels, short na) {
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

	public byte[] encodeCell(short[][] pixels) {		
		short[] raw = new short[cell_pixel_count];
		int destPos = 0;
		for(int i = 0; i < pixel_len; i++) {
			short[] src = pixels[i];				
			for(int c=0; c < pixel_len; c++) {
				raw[destPos++] = src[c];
			}
		}
		return enc(raw);
	}

	public static short[][] createEmpty(int width, int height, short na) {
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

	public short[][] createEmptyCell(short na) {
		return createEmpty(pixel_len, pixel_len, na);
	}

	private void decodeCell(Tile tile, short[][] target, Range2d targetRange) {
		short[] raw = dec(tile.data);

		//log.info("decodeCell target  " + targetRange.xmin + " " + targetRange.ymin + "   " + targetRange.xmax + " " + targetRange.ymax);

		int cxmin = cellToPixelMin(tile.x);
		int cymin = cellToPixelMin(tile.y);
		int cxmax = cellToPixelMax(tile.x);
		int cymax = cellToPixelMax(tile.y);

		if(targetRange.xmin <= cxmin && targetRange.ymin <= cymin && cxmax <= targetRange.xmax && cymax <= targetRange.ymax) {
			int xTargetStart = cxmin - targetRange.xmin;
			int yTargetStart = cymin - targetRange.ymin;
			//int xTargetEnd = cxmax - targetRange.xmin;
			//int yTargetEnd = cymax - targetRange.ymin;
			
			/*int cellPos = 0;
			for(int y = yTargetStart; y <= yTargetEnd; y++) {
				short[] dst = target[y];				
				for(int x = xTargetStart; x <= xTargetEnd; x++) {
					dst[x] = raw[cellPos++];
				}
			}*/
			for(int y = 0; y < pixel_len; y++) {
				System.arraycopy(raw, pixel_len * y, target[yTargetStart + y], xTargetStart, pixel_len);
			}
		} else {
			//log.info("decodeCell cell    " + cxmin + " " + cymin + "   " + cxmax + " " + cymax);

			int xCoveredMin = cxmin < targetRange.xmin ? targetRange.xmin : cxmin;
			int yCoveredMin = cymin < targetRange.ymin ? targetRange.ymin : cymin;
			int xCoveredMax = cxmax > targetRange.xmax ? targetRange.xmax : cxmax;
			int yCoveredMax = cymax > targetRange.ymax ? targetRange.ymax : cymax;

			//log.info("decodeCell covered " + xCoveredMin + " " + yCoveredMin + "   " + xCoveredMax + " " + yCoveredMax);

			int xTargetStart = xCoveredMin - targetRange.xmin;
			int yTargetStart = yCoveredMin - targetRange.ymin;
			int xTargetEnd = xCoveredMax - targetRange.xmin;
			int yTargetEnd = yCoveredMax - targetRange.ymin;

			//log.info("decodeCell xTarget " + xTargetStart + " - " + xTargetEnd + " :  " + (xTargetEnd - xTargetStart + 1));
			//log.info("decodeCell yTarget " + yTargetStart + " - " + yTargetEnd + " :  " + (yTargetEnd - yTargetStart + 1));

			int xCellStart = xCoveredMin - cxmin;
			int yCellStart = yCoveredMin - cymin;
			int xCellEnd = xCoveredMax - cxmin;
			//int yCellEnd = yCoveredMax - cymin;

			//log.info("decodeCell xCell   " + xCellStart + " - " + xCellEnd + " :  " + (xCellEnd - xCellStart + 1));
			//log.info("decodeCell yCell   " + yCellStart + " - " + yCellEnd + " :  " + (yCellEnd - yCellStart + 1));

			int xCellSkip = xCellStart + ((pixel_len - 1) - xCellEnd);

			int cellPos = (yCellStart * pixel_len) + xCellStart;
			for(int y = yTargetStart; y <= yTargetEnd; y++) {
				short[] dst = target[y];				
				for(int x = xTargetStart; x <= xTargetEnd; x++) {
					dst[x] = raw[cellPos++];
				}
				cellPos += xCellSkip;
			}
		}
	}


	public void decodeCellMerge(byte[] data, short[][] cellPixels, short na) {
		short[] raw = dec(data);
		int srcPos = 0;
		for(int i=0; i < pixel_len; i++) {
			short[] dst = cellPixels[i];				
			for(int c = 0; c < pixel_len; c++) {
				if(dst[c] == na) {
					dst[c] = raw[srcPos]; 
				}
				srcPos++;
			}
		}
	}


	public static byte[] enc(short[] data) {
		byte[] transformed = encInt16_split(data);
		//byte[] result = Zstd.compress(transformed, 1); // default
		byte[] result = Zstd.compress(transformed, 100); // default
		return result;
	}

	public static short[] dec(byte[] data) {
		int len = (int) Zstd.decompressedSize(data);
		byte[] inter = Zstd.decompress(data, len);
		short[] result = decInt16_split(inter);
		return result;
	}


	public static byte[] encInt16_split(short[] data) {
		int size = data.length;
		int len = size << 1;
		byte[] result = new byte[len];
		for (int i = 0; i < size; i++) {
			short v = data[i];
			result[i] = (byte) v;
			result[i + size] = (byte) (v>>8);
		}
		return result;
	}

	public static short[] decInt16_split(byte[] data) {	
		int len = data.length;
		int size = len >> 1;		
		short[] result = new short[size];
		for(int i = 0; i < size; i++) {
			result[i] = (short) ((data[i] & 0xFF) | ((data[i + size] & 0xFF)<<8));
		}
		return result;
	}

	public short[][] decodeCell(byte[] data) {
		short[] raw = dec(data);
		short[][] cellPixels = new short[pixel_len][pixel_len];
		int srcPos = 0;
		for(int i=0; i < pixel_len; i++) {
			short[] dst = cellPixels[i];				
			for(int c = 0; c < pixel_len; c++) {
				dst[c] = raw[srcPos++]; 
			}
		}
		return cellPixels;
	}

	public void writeRasterUnitBandDiv4(RasterUnitStorage rasterUnitStorage, RasterUnitStorage targetUnit, BandKey bandKey, Band band, Commiter counter) throws IOException {
		Range2d range = rasterUnitStorage.getTileRange(bandKey);
		if(range == null) {
			return;
		}

		int xmin = Math.floorDiv(range.xmin, 4);
		int ymin = Math.floorDiv(range.ymin, 4);
		int xmax = Math.floorDiv(range.xmax, 4);
		int ymax = Math.floorDiv(range.ymax, 4);

		short na = band.getInt16NA();

		int pixel_len_div4 = pixel_len / 4;

		for(int y=ymin;y<=ymax;y++) {
			int tymin = 4*y;
			int tymax = tymin + 3;
			int x = xmin;
			int tilesWrittenInRow = 0;
			while(x <= xmax) {
				final int BATCH_SIZE_MAX = 256;
				int remaining_tiles = xmax - x + 1;
				int batch_size = BATCH_SIZE_MAX <= remaining_tiles ? BATCH_SIZE_MAX : remaining_tiles;
				short[][][] target = new short[batch_size][][];
				int txmin = 4 * x;
				int txmax = txmin + 4 * batch_size - 1;
				Collection<Tile> tiles = rasterUnitStorage.readTiles(bandKey.t, bandKey.b, tymin, tymax, txmin, txmax);
				for(Tile tile:tiles) {
					int targetIndex = Math.floorDiv(tile.x - txmin, 4);
					if(target[targetIndex] == null) {
						target[targetIndex] = createEmptyCell(na); // na fill: not all pixels may be written
					}
					short[][] pixels = decodeCell(tile.data);
					int iy = tile.y - tymin;
					int ix = tile.x - txmin - targetIndex * 4;
					copyDiv(pixels, 4, na, target[targetIndex], ix * pixel_len_div4, iy * pixel_len_div4);
				}				
				for (int targetIndex = 0; targetIndex < target.length; targetIndex++) {
					if(target[targetIndex] != null) {
						TileKey tileKey = bandKey.toTileKey(y, x + targetIndex);
						byte[] data = encodeCell(target[targetIndex]);
						targetUnit.writeTile(new Tile(tileKey, CellType.INT16, data));
						tilesWrittenInRow++;
					}
				}
				x += batch_size; 
			}
			counter.add(tilesWrittenInRow);
		}		
	}

	public static int countNotNa_raw(short[] raw, short na) {
		int len = raw.length;
		int cnt = 0;
		for(int i = 0; i < len; i++) {
			if(raw[i] != na) {
				cnt++;
			}
		}
		return cnt;
	}
}
