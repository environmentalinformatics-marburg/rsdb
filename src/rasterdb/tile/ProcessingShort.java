package rasterdb.tile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import rasterdb.Band;
import rasterdb.tile.Processing.Commiter;
import rasterunit.BandKey;
import rasterunit.RasterUnit;
import rasterunit.RasterUnitStorage;
import rasterunit.Tile;
import rasterunit.TileKey;
import util.Range2d;

public class ProcessingShort {
	//private static final Logger log = LogManager.getLogger();

	public static short[][] readPixels(int div, RasterUnitStorage pyramid_rasterUnit, int t, Band band, Range2d pixelRange) {
		switch(div) {
		case 1:
			return readPixels(pyramid_rasterUnit, t, band, pixelRange);
		case 2:
			return readPixelsDiv2(pyramid_rasterUnit, t, band, pixelRange);
		case 4:
			return readPixelsDiv4(pyramid_rasterUnit, t, band, pixelRange);
		case 8:
		case 16:
		case 32:
		case 64:
		case 128:
		case 256:
			return readPixelsDiv(pyramid_rasterUnit, t, band, pixelRange, div);
			default:
				throw new RuntimeException("unknown div " + div);
		}
	}

	public static short[][] readPixels(RasterUnitStorage pyramid_rasterUnit, int t, Band band, Range2d pixelRange) {
		return readPixels(pyramid_rasterUnit, t, band, pixelRange.ymin, pixelRange.ymax, pixelRange.xmin, pixelRange.xmax);
	}

	public static short[][] readPixels(RasterUnitStorage pyramid_rasterUnit, int t, Band band, int pymin, int pymax, int pxmin, int pxmax) {
		int ymin = TilePixel.pixelToTile(pymin);
		int ymax = TilePixel.pixelToTile(pymax);
		int xmin = TilePixel.pixelToTile(pxmin); 
		int xmax = TilePixel.pixelToTile(pxmax);
		short[][] data = readTiles(pyramid_rasterUnit, t, band, ymin, ymax, xmin, xmax);
		int yloff = TilePixel.pixelToTileOffset(pymin);
		int xloff = TilePixel.pixelToTileOffset(pxmin);
		int yroff = TilePixel.tileToPixel(ymax - ymin) + TilePixel.pixelToTileOffset(pymax);
		int xroff = TilePixel.tileToPixel(xmax - xmin) + TilePixel.pixelToTileOffset(pxmax);
		return copy(data, yloff, yroff, xloff, xroff);
	}

	public static short[][] readPixelsDiv2(RasterUnitStorage pyramid_rasterUnit, int t, Band band, Range2d pixelRange) {
		return readPixelsDiv2(pyramid_rasterUnit, t, band, pixelRange.ymin, pixelRange.ymax, pixelRange.xmin, pixelRange.xmax);
	}

	public static short[][] readPixelsDiv2(RasterUnitStorage pyramid_rasterUnit, int t, Band band, int pymin, int pymax, int pxmin, int pxmax) {
		int ymin = TilePixel.pixelToTile(pymin);
		int ymax = TilePixel.pixelToTile(pymax);
		int xmin = TilePixel.pixelToTile(pxmin); 
		int xmax = TilePixel.pixelToTile(pxmax);
		short[][] data = readTilesDiv2(pyramid_rasterUnit, t, band, ymin, ymax, xmin, xmax);
		int yloff = TilePixel.pixelToTileDiv2Offset(pymin);
		int xloff = TilePixel.pixelToTileDiv2Offset(pxmin);
		int yroff = TilePixel.tileDiv2ToPixel(ymax - ymin) + TilePixel.pixelToTileDiv2Offset(pymax);
		int xroff = TilePixel.tileDiv2ToPixel(xmax - xmin) + TilePixel.pixelToTileDiv2Offset(pxmax);
		return copy(data, yloff, yroff, xloff, xroff);
	}

	public static short[][] readPixelsDiv4(RasterUnitStorage pyramid_rasterUnit, int t, Band band, Range2d pixelRange) {
		return readPixelsDiv4(pyramid_rasterUnit, t, band, pixelRange.ymin, pixelRange.ymax, pixelRange.xmin, pixelRange.xmax);
	}

	public static short[][] readPixelsDiv(RasterUnitStorage pyramid_rasterUnit, int t, Band band, Range2d pixelRange, int div) {
		return readPixelsDiv(pyramid_rasterUnit, t, band, pixelRange.ymin, pixelRange.ymax, pixelRange.xmin, pixelRange.xmax, div);
	}

	public static short[][] readPixelsDiv4(RasterUnitStorage pyramid_rasterUnit, int t, Band band, int pymin, int pymax, int pxmin, int pxmax) {
		int ymin = TilePixel.pixelToTile(pymin);
		int ymax = TilePixel.pixelToTile(pymax);
		int xmin = TilePixel.pixelToTile(pxmin); 
		int xmax = TilePixel.pixelToTile(pxmax);
		short[][] data = readTilesDiv4(pyramid_rasterUnit, t, band, ymin, ymax, xmin, xmax);
		int yloff = TilePixel.pixelToTileDiv4Offset(pymin);
		int xloff = TilePixel.pixelToTileDiv4Offset(pxmin);
		int yroff = TilePixel.tileDiv4ToPixel(ymax - ymin) + TilePixel.pixelToTileDiv4Offset(pymax);
		int xroff = TilePixel.tileDiv4ToPixel(xmax - xmin) + TilePixel.pixelToTileDiv4Offset(pxmax);
		return copy(data, yloff, yroff, xloff, xroff);
	}

	public static short[][] readPixelsDiv(RasterUnitStorage pyramid_rasterUnit, int t, Band band, int pymin, int pymax, int pxmin, int pxmax, int div) {
		int ymin = TilePixel.pixelToTile(pymin);
		int ymax = TilePixel.pixelToTile(pymax);
		int xmin = TilePixel.pixelToTile(pxmin); 
		int xmax = TilePixel.pixelToTile(pxmax);
		short[][] data = readTilesDiv(pyramid_rasterUnit, t, band, ymin, ymax, xmin, xmax, div);
		int yloff = TilePixel.pixelToTileDivOffset(pymin, div);
		int xloff = TilePixel.pixelToTileDivOffset(pxmin, div);
		int yroff = TilePixel.tileDivToPixel(ymax - ymin, div) + TilePixel.pixelToTileDivOffset(pymax, div);
		int xroff = TilePixel.tileDivToPixel(xmax - xmin, div) + TilePixel.pixelToTileDivOffset(pxmax, div);
		return copy(data, yloff, yroff, xloff, xroff);
	}

	public static short[][] copy(short[][] data, int ymin, int ymax, int xmin, int xmax) {
		int xlen = xmax - xmin + 1;
		int ylen = ymax - ymin + 1;
		short[][] pixels = new short[ylen][xlen]; //not na fill: all pixels are written
		for (int y = 0; y < ylen; y++) {
			System.arraycopy(data[ymin + y], xmin, pixels[y], 0, xlen);
		}
		return pixels;
	}

	/**
	 * no NA value check
	 * @param tilePixels
	 * @param target
	 * @param ymin
	 * @param xmin
	 */
	public static void copyFastTileDiv2(short[][] tilePixels, short[][] target, int ymin, int xmin) {
		int yTarget = ymin;
		for(int py = 0; py < TilePixel.PIXELS_PER_ROW; py += 2) {
			short[] row1 = tilePixels[py];
			short[] row2 = tilePixels[py+1];
			short[] targetRow = target[yTarget++];
			int xTarget = xmin;
			for(int px = 0; px < TilePixel.PIXELS_PER_ROW; px += 2) {
				targetRow[xTarget++] = (short) ((row1[px]+row1[px+1]+row2[px]+row2[px+1])>>2);
			}
		}
	}

	public static void copyTileDiv2(short[][] tilePixels, short na, short[][] target, int ymin, int xmin) {
		int yTarget = ymin;
		for(int py = 0; py < TilePixel.PIXELS_PER_ROW; py += 2) {
			short[] targetRow = target[yTarget++];
			int xTarget = xmin;
			for(int px = 0; px < TilePixel.PIXELS_PER_ROW; px += 2) {
				int sum = 0;
				int cnt = 0;
				for (int by = 0; by < 2; by++) {
					short[] row = tilePixels[py + by];
					for (int bx = 0; bx < 2; bx++) {
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

	/**
	 * no NA value check
	 * @param tilePixels
	 * @param target
	 * @param ymin
	 * @param xmin
	 */
	public static void copyFastTileDiv4(short[][] tilePixels, short[][] target, int ymin, int xmin) {
		int yTarget = ymin;
		for(int py = 0; py < TilePixel.PIXELS_PER_ROW; py += 4) {
			short[] row1 = tilePixels[py];
			short[] row2 = tilePixels[py+1];
			short[] row3 = tilePixels[py+2];
			short[] row4 = tilePixels[py+3];
			short[] targetRow = target[yTarget++];
			int xTarget = xmin;
			for(int px = 0; px < TilePixel.PIXELS_PER_ROW; px += 4) {
				targetRow[xTarget++] = (short) ((row1[px]+row1[px+1]+row1[px+2]+row1[px+3]
						+row2[px]+row2[px+1]+row2[px+2]+row2[px+3]
								+row3[px]+row3[px+1]+row3[px+2]+row3[px+3]
										+row4[px]+row4[px+1]+row4[px+2]+row4[px+3])>>4);
			}
		}
	}

	public static void copyTileDiv4(short[][] tilePixels, short na, short[][] target, int ymin, int xmin) {
		int yTarget = ymin;
		for(int py = 0; py < TilePixel.PIXELS_PER_ROW; py += 4) {
			short[] targetRow = target[yTarget++];
			int xTarget = xmin;
			for(int px = 0; px < TilePixel.PIXELS_PER_ROW; px += 4) {
				int sum = 0;
				int cnt = 0;
				for (int by = 0; by < 4; by++) {
					short[] row = tilePixels[py + by];
					for (int bx = 0; bx < 4; bx++) {
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

	public static void copyTileDiv(short[][] tilePixels, short na, short[][] target, int ymin, int xmin, int div) {
		int yTarget = ymin;
		for(int py = 0; py < TilePixel.PIXELS_PER_ROW; py += div) {
			short[] targetRow = target[yTarget++];
			int xTarget = xmin;
			for(int px = 0; px < TilePixel.PIXELS_PER_ROW; px += div) {
				int sum = 0;
				int cnt = 0;
				for (int by = 0; by < div; by++) {
					short[] row = tilePixels[py + by];
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
	
	public static void copyTileDivSelect(short[][] tilePixels, short na, short[][] target, int ymin, int xmin, int div) {
		switch(div) {
		case 2:
			copyTileDiv2(tilePixels, na, target, ymin, xmin);
			break;
		case 4:
			copyTileDiv4(tilePixels, na, target, ymin, xmin);
			break;
		default:
			copyTileDiv(tilePixels, na, target, ymin, xmin, div);
			break;
		}
	}

	private static final boolean parallel = true;

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

	public static short[][] readTiles(RasterUnit rasterUnit, int t, Band band, Range2d range2d) {
		return readTiles(rasterUnit, t, band, range2d.ymin, range2d.ymax, range2d.xmin, range2d.xmax);
	}

	public static short[][] readTiles(RasterUnitStorage pyramid_rasterUnit, int t, Band band, int ymin, int ymax, int xmin, int xmax) {
		int rxlen = TilePixel.tileToPixel(xmax - xmin + 1);
		int rylen = TilePixel.tileToPixel(ymax - ymin + 1);
		short na = band.getInt16NA();
		short[][] data = createEmpty(rxlen, rylen, na); // na fill: not all pixels may be written
		Collection<Tile> tiles = pyramid_rasterUnit.readTiles(t, band.index, ymin, ymax, xmin, xmax);
		if(!parallel) {
			for(Tile tile:tiles) {
				int x = TilePixel.tileToPixel(tile.x - xmin);
				int y = TilePixel.tileToPixel(tile.y - ymin);
				TileShort.decode(tile.data, data, x, y);
			}
		} else {
			Tile[] resultTiles = tiles.toArray(new Tile[0]);
			Arrays.stream(resultTiles).parallel().forEach(tile -> {
				int x = TilePixel.tileToPixel(tile.x - xmin);
				int y = TilePixel.tileToPixel(tile.y - ymin);
				TileShort.decode(tile.data, data, x, y);
			});
		}
		return data;
	}

	public static short[][] readTilesDiv2(RasterUnit rasterUnit, int t, Band band, Range2d range2d) {
		return readTilesDiv2(rasterUnit, t, band, range2d.ymin, range2d.ymax, range2d.xmin, range2d.xmax);
	}

	public static short[][] readTilesDiv2(RasterUnitStorage pyramid_rasterUnit, int t, Band band, int ymin, int ymax, int xmin, int xmax) {
		int rxlen = TilePixel.tileDiv2ToPixel(xmax - xmin + 1);
		int rylen = TilePixel.tileDiv2ToPixel(ymax - ymin + 1);
		short na = band.getInt16NA();
		short[][] data = createEmpty(rxlen, rylen, na); // na fill: not all pixels may be written
		Collection<Tile> tiles = pyramid_rasterUnit.readTiles(t, band.index, ymin, ymax, xmin, xmax);
		if(!parallel) {
			for(Tile tile:tiles) {
				int x = TilePixel.tileDiv2ToPixel(tile.x - xmin);
				int y = TilePixel.tileDiv2ToPixel(tile.y - ymin);
				short[][] pixels = TileShort.decode(tile.data);
				copyTileDiv2(pixels, na, data, y, x);
			}
		}else {
			Tile[] resultTiles = tiles.toArray(new Tile[0]);
			Arrays.stream(resultTiles).parallel().forEach(tile -> {
				int x = TilePixel.tileDiv2ToPixel(tile.x - xmin);
				int y = TilePixel.tileDiv2ToPixel(tile.y - ymin);
				short[][] pixels = TileShort.decode(tile.data);
				copyTileDiv2(pixels, na, data, y, x);
			});
		}
		return data;
	}

	public static short[][] readTilesDiv4(RasterUnit rasterUnit, int t, Band band, Range2d range2d) {
		return readTilesDiv4(rasterUnit, t, band, range2d.ymin, range2d.ymax, range2d.xmin, range2d.xmax);
	}

	public static short[][] readTilesDiv(RasterUnit rasterUnit, int t, Band band, Range2d range2d, int div) {
		return readTilesDiv(rasterUnit, t, band, range2d.ymin, range2d.ymax, range2d.xmin, range2d.xmax, div);
	}

	public static short[][] readTilesDiv4(RasterUnitStorage pyramid_rasterUnit, int t, Band band, int ymin, int ymax, int xmin, int xmax) {
		int rxlen = TilePixel.tileDiv4ToPixel(xmax - xmin + 1);
		int rylen = TilePixel.tileDiv4ToPixel(ymax - ymin + 1);
		short na = band.getInt16NA();
		short[][] data = createEmpty(rxlen, rylen, na); // na fill: not all pixels may be written
		Collection<Tile> tiles = pyramid_rasterUnit.readTiles(t, band.index, ymin, ymax, xmin, xmax);
		if(!parallel) {
			for(Tile tile:tiles) {
				int x = TilePixel.tileDiv4ToPixel(tile.x - xmin);
				int y = TilePixel.tileDiv4ToPixel(tile.y - ymin);
				short[][] pixels = TileShort.decode(tile.data);
				copyTileDiv4(pixels, na, data, y, x);
			}
		}else {
			Tile[] resultTiles = tiles.toArray(new Tile[0]);
			Arrays.stream(resultTiles).parallel().forEach(tile -> {
				int x = TilePixel.tileDiv4ToPixel(tile.x - xmin);
				int y = TilePixel.tileDiv4ToPixel(tile.y - ymin);
				short[][] pixels = TileShort.decode(tile.data);
				copyTileDiv4(pixels, na, data, y, x);
			});
		}
		return data;
	}

	public static short[][] readTilesDiv(RasterUnitStorage pyramid_rasterUnit, int t, Band band, int ymin, int ymax, int xmin, int xmax, int div) {
		int rxlen = TilePixel.tileDivToPixel(xmax - xmin + 1, div);
		int rylen = TilePixel.tileDivToPixel(ymax - ymin + 1, div);
		short na = band.getInt16NA();
		short[][] data = createEmpty(rxlen, rylen, na); // na fill: not all pixels may be written
		Collection<Tile> tiles = pyramid_rasterUnit.readTiles(t, band.index, ymin, ymax, xmin, xmax);
		if(!parallel) {
			for(Tile tile:tiles) {
				int x = TilePixel.tileDivToPixel(tile.x - xmin, div);
				int y = TilePixel.tileDivToPixel(tile.y - ymin, div);
				short[][] pixels = TileShort.decode(tile.data);
				copyTileDiv(pixels, na, data, y, x, div);
			}
		}else {
			Tile[] resultTiles = tiles.toArray(new Tile[0]);
			Arrays.stream(resultTiles).parallel().forEach(tile -> {
				int x = TilePixel.tileDivToPixel(tile.x - xmin, div);
				int y = TilePixel.tileDivToPixel(tile.y - ymin, div);
				short[][] pixels = TileShort.decode(tile.data);
				copyTileDiv(pixels, na, data, y, x, div);
			});
		}
		return data;
	}

	public static int writeMerge(RasterUnitStorage rasterUnit, int t, Band band, short[][] pixels, int pixelYmin, int pixelXmin) throws IOException {
		int tileWriteCount = 0;
		int xlen = pixels[0].length;
		int ylen = pixels.length;
		int pixelXmax = pixelXmin + xlen - 1;
		int pixelYmax = pixelYmin + ylen - 1;
		int tileXmin = TilePixel.pixelToTile(pixelXmin);
		int tileYmin = TilePixel.pixelToTile(pixelYmin);
		int tileXmax = TilePixel.pixelToTile(pixelXmax);
		int tileYmax = TilePixel.pixelToTile(pixelYmax);
		short na = band.getInt16NA();
		for(int tileY = tileYmin; tileY <= tileYmax; tileY++) {
			for(int tileX = tileXmin; tileX <= tileXmax; tileX++) {
				int xMin = TilePixel.tileToPixel(tileX) - pixelXmin;
				int yMin = TilePixel.tileToPixel(tileY) - pixelYmin;
				int xStart = 0;
				if(xMin < 0) {
					xStart -= xMin;
				}
				int xEnd = TilePixel.PIXELS_PER_ROW - 1;
				if(xMin + TilePixel.PIXELS_PER_ROW - 1 > xlen - 1) {
					xEnd = xlen - 1 - xMin;
				}
				int yStart = 0;
				if(yMin < 0) {
					yStart -= yMin;
				}
				int yEnd = TilePixel.PIXELS_PER_ROW - 1;
				if(yMin + TilePixel.PIXELS_PER_ROW - 1 > ylen - 1) {
					yEnd = ylen - 1 - yMin;
				}
				short[][] tilePixels = createEmpty(TilePixel.PIXELS_PER_ROW, TilePixel.PIXELS_PER_ROW, na); // na fill: not all pixels may be written
				int ty = yMin + yStart;
				int tx = xMin + xStart;
				int txlen = xEnd - xStart + 1;
				for(int y = yStart; y <= yEnd; y++) {
					short[] targetRow = pixels[ty++];
					short[] pixelRow = tilePixels[y];
					System.arraycopy(targetRow, tx, pixelRow, xStart, txlen);
				}
				if(writeTileMerge(rasterUnit, t, band, tileY, tileX, tilePixels)) {
					tileWriteCount++;
				}
			}
		}
		return tileWriteCount;
	}

	public static boolean writeTileMerge(RasterUnitStorage rasterUnit, int t, Band band, int y, int x, short[][] tilePixels) throws IOException {
		short na = band.getInt16NA();
		if(TileShort.isNotAllNa(tilePixels, na)) {
			TileKey tileKey = new TileKey(t, band.index, y, x);
			Tile tile = rasterUnit.readTile(tileKey);
			if(tile != null) {
				TileShort.decodeMerge(tile.data, tilePixels, na);
			}		
			byte[] data = TileShort.encode(tilePixels);
			rasterUnit.writeTile(new Tile(tileKey, TilePixel.TYPE_SHORT, data));
			return true;
		} else {
			return false;
		}
	}

	public static void writeStorageBandDiv(Band band, int div, RasterUnitStorage srcStorage, BandKey srcBandKey, RasterUnitStorage dstStorage, BandKey dstBandKey, Commiter counter) throws IOException {
		int pixel_len = TilePixel.PIXELS_PER_ROW;
		if(div < 2 ) {
			throw new RuntimeException("invalid div: " + div);
		}
		if(pixel_len % div != 0) {
			throw new RuntimeException("invalid div: " + div + "  for 256");
		}
		Range2d range = srcStorage.getTileRange2d(srcBandKey);
		if(range == null) {
			return;
		}
		
		int divm1 = div - 1;

		int xmin = Math.floorDiv(range.xmin, div);
		int ymin = Math.floorDiv(range.ymin, div);
		int xmax = Math.floorDiv(range.xmax, div);
		int ymax = Math.floorDiv(range.ymax, div);

		short na = band.getInt16NA();
		
		int pixel_len_div = pixel_len / div;

		for(int y=ymin;y<=ymax;y++) {
			int tymin = div * y;
			int tymax = tymin + divm1;
			int x = xmin;
			int tilesWrittenInRow = 0;
			while(x<=xmax) {
				final int BATCH_SIZE_MAX = 256;
				int remaining_tiles = xmax - x + 1;
				int batch_size = BATCH_SIZE_MAX <= remaining_tiles ? BATCH_SIZE_MAX : remaining_tiles;
				short[][][] target = new short[batch_size][][];
				int txmin = div * x;
				int txmax = txmin + div * batch_size - 1;
				Collection<Tile> tiles = srcStorage.readTiles(srcBandKey.t, srcBandKey.b, tymin, tymax, txmin, txmax);
				for(Tile tile:tiles) {
					int targetIndex = Math.floorDiv(tile.x - txmin, div);
					if(target[targetIndex] == null) {
						target[targetIndex] = createEmpty(TilePixel.PIXELS_PER_ROW, TilePixel.PIXELS_PER_ROW, na); // na fill: not all pixels may be written
					}
					short[][] pixels = TileShort.decode(tile.data);
					int iy = tile.y - tymin;
					int ix = tile.x - txmin - targetIndex * div;
					copyTileDivSelect(pixels, na, target[targetIndex], iy * pixel_len_div, ix * pixel_len_div, div);
				}				
				for (int targetIndex = 0; targetIndex < target.length; targetIndex++) {
					if(target[targetIndex] != null) {
						TileKey tileKey = dstBandKey.toTileKey(y, x + targetIndex);
						byte[] data = TileShort.encode(target[targetIndex]);
						dstStorage.writeTile(new Tile(tileKey, TilePixel.TYPE_SHORT, data));
						tilesWrittenInRow++;
					}
				}
				x += batch_size; 
			}
			counter.add(tilesWrittenInRow);
		}		
	}

	public static void convertNA(short[][] pixels, short srcNA, short dstNA) {
		int w = pixels[0].length;
		int h = pixels.length;
		for(int i=0;i<h;i++) {
			short[] src = pixels[i];				
			for(int c=0;c<w;c++) {
				//log.info(src[c]);
				if(src[c] == srcNA) {
					src[c] = dstNA;
				}
				/*if(src[c] != 0) {
					System.out.println(src[c]);
				}*/
			}
		}
	}
}
