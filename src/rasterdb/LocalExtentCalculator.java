package rasterdb;

import java.io.IOException;
import java.util.Set;

import rasterdb.cell.CellInt16;
import rasterdb.cell.CellType;
import rasterdb.tile.TileFloat;
import rasterdb.tile.TilePixel;
import rasterdb.tile.TileShort;
import rasterunit.RasterUnitStorage;
import rasterunit.TileKey;
import util.Range2d;

public class LocalExtentCalculator {

	/**
	 * 
	 * @param rasterdb
	 * @return local range of all rasterdb bands, null if there are no tiles in rasterdb
	 */
	public static Range2d calc(RasterDB rasterdb) {
		try {
			Set<Integer> keys = rasterdb.bandMapReadonly.keySet();
			if(keys.isEmpty()) {
				return null;
			}
			int bandMaxIndex = keys.stream().mapToInt(i->i).max().getAsInt();
			int[] bandTypes = new int[bandMaxIndex + 1];
			short[] bandShortNAs = new short[bandMaxIndex + 1];
			for(Band band : rasterdb.bandMapReadonly.values()) {
				bandTypes[band.index] = band.type;
				bandShortNAs[band.index] = band.getInt16NA();
			}

			RasterUnitStorage rasterUnit = rasterdb.rasterUnit();
			Range2d tileRange = rasterUnit.getTileRange2d();
			if(tileRange == null) {
				return null;
			}
			int lxmin = rasterdb.getTilePixelLen() - 1;
			int lymin = rasterdb.getTilePixelLen() - 1;
			int lxmax = 0;
			int lymax = 0;

			for(TileKey tileKey:rasterUnit.tileKeysReadonly()) {
				switch(bandTypes[tileKey.b]) {
				case TilePixel.TYPE_SHORT: {
					short na = (short) bandShortNAs[tileKey.b];
					short[][] pixels = null;
					if(lxmin > 0 && tileRange.xmin == tileKey.x) {
						if(pixels == null) {
							pixels = TileShort.decode(rasterUnit.readTile(tileKey).data);
						}
						int cxmin = getXmin(pixels, na);
						if(cxmin < lxmin) {
							lxmin = cxmin;
						}
					}
					if(lymin > 0 && tileRange.ymin == tileKey.y) {
						if(pixels == null) {
							pixels = TileShort.decode(rasterUnit.readTile(tileKey).data);
						}
						int cymin = getYmin(pixels, na);
						if(cymin < lymin) {
							lymin = cymin;
						}
					}
					if(lxmax < TilePixel.PIXELS_PER_ROW_1 && tileRange.xmax == tileKey.x) {
						if(pixels == null) {
							pixels = TileShort.decode(rasterUnit.readTile(tileKey).data);
						}
						int cxmax = getXmax(pixels, na);
						if(cxmax > lxmax) {
							lxmax = cxmax;
						}
					}
					if(lymax < TilePixel.PIXELS_PER_ROW_1 && tileRange.ymax == tileKey.y) {
						if(pixels == null) {
							pixels = TileShort.decode(rasterUnit.readTile(tileKey).data);
						}
						int cymax = getYmax(pixels, na);
						if(cymax > lymax) {
							lymax = cymax;
						}
					}
					break;
				}
				case TilePixel.TYPE_FLOAT: {
					float[][] pixels = null;
					if(lxmin > 0 && tileRange.xmin == tileKey.x) {
						if(pixels == null) {
							pixels = TileFloat.decode(rasterUnit.readTile(tileKey).data);
						}
						int cxmin = getXmin(pixels);
						if(cxmin < lxmin) {
							lxmin = cxmin;
						}
					}
					if(lymin > 0 && tileRange.ymin == tileKey.y) {
						if(pixels == null) {
							pixels = TileFloat.decode(rasterUnit.readTile(tileKey).data);
						}
						int cymin = getYmin(pixels);
						if(cymin < lymin) {
							lymin = cymin;
						}
					}
					if(lxmax < TilePixel.PIXELS_PER_ROW_1 && tileRange.xmax == tileKey.x) {
						if(pixels == null) {
							pixels = TileFloat.decode(rasterUnit.readTile(tileKey).data);
						}
						int cxmax = getXmax(pixels);
						if(cxmax > lxmax) {
							lxmax = cxmax;
						}
					}
					if(lymax < TilePixel.PIXELS_PER_ROW_1 && tileRange.ymax == tileKey.y) {
						if(pixels == null) {
							pixels = TileFloat.decode(rasterUnit.readTile(tileKey).data);
						}
						int cymax = getYmax(pixels);
						if(cymax > lymax) {
							lymax = cymax;
						}
					}
					break;
				}
				case CellType.INT16: {
					CellInt16 cellInt16 = new CellInt16(rasterdb.getTilePixelLen());
					short na = (short) bandShortNAs[tileKey.b];
					short[][] pixels = null;
					if(lxmin > 0 && tileRange.xmin == tileKey.x) {
						if(pixels == null) {
							pixels = cellInt16.decodeCell(rasterUnit.readTile(tileKey));
						}
						int cxmin = getXmin(pixels, na);
						if(cxmin < lxmin) {
							lxmin = cxmin;
						}
					}
					if(lymin > 0 && tileRange.ymin == tileKey.y) {
						if(pixels == null) {
							pixels = cellInt16.decodeCell(rasterUnit.readTile(tileKey));
						}
						int cymin = getYmin(pixels, na);
						if(cymin < lymin) {
							lymin = cymin;
						}
					}
					int PIXELS_PER_ROW_1 = cellInt16.pixel_len - 1;
					if(lxmax < PIXELS_PER_ROW_1 && tileRange.xmax == tileKey.x) {
						if(pixels == null) {
							pixels = cellInt16.decodeCell(rasterUnit.readTile(tileKey));
						}
						int cxmax = getXmax(pixels, na);
						if(cxmax > lxmax) {
							lxmax = cxmax;
						}
					}
					if(lymax < PIXELS_PER_ROW_1 && tileRange.ymax == tileKey.y) {
						if(pixels == null) {
							pixels = cellInt16.decodeCell(rasterUnit.readTile(tileKey));
						}
						int cymax = getYmax(pixels, na);
						if(cymax > lymax) {
							lymax = cymax;
						}
					}
					break;
				}
				default:
					throw new RuntimeException("unknown type " + bandTypes[tileKey.b]);
				}
			}		
			return tileRange.mul(rasterdb.getTilePixelLen()).add(lxmin, lymin, lxmax, lymax);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static int getXmin(short[][] pixels, short na) { // no check for all na
		int w1 = pixels[0].length - 1;
		int h = pixels.length;
		int xmin = w1;
		for(int y = 0; y < h; y++) {
			short[] row = pixels[y];				
			for(int x = 0; x < xmin; x++) {
				if(row[x] != na) {
					xmin = x;
					break;
				}
			}
		}
		return xmin;
	}

	public static int getYmin(short[][] pixels, short na) { // no check for all na
		int w = pixels[0].length;
		int h1 = pixels.length - 1;
		for(int y = 0; y < h1; y++) {
			short[] row = pixels[y];				
			for(int x = 0; x < w; x++) {
				if(row[x] != na) {
					return y;
				}
			}
		}
		return h1;
	}

	public static int getXmax(short[][] pixels, short na) { // no check for all na
		int w1 = pixels[0].length - 1;
		int h = pixels.length;
		int xmax = 0;
		for(int y = 0; y < h; y++) {
			short[] row = pixels[y];				
			for(int x = w1; x > xmax ; x--) {
				if(row[x] != na) {
					xmax = x;
					break;
				}
			}
		}
		return xmax;
	}

	public static int getYmax(short[][] pixels, short na) { // no check for all na
		int w = pixels[0].length;
		int h1 = pixels.length - 1;
		for(int y = h1; y > 0 ; y--) {
			short[] row = pixels[y];				
			for(int x = 0; x < w; x++) {
				if(row[x] != na) {
					return y;
				}
			}
		}
		return 0;
	}

	public static int getXmin(float[][] pixels) { // no check for all na
		int w1 = pixels[0].length - 1;
		int h = pixels.length;
		int xmin = w1;
		for(int y = 0; y < h; y++) {
			float[] row = pixels[y];				
			for(int x = 0; x < xmin; x++) {
				if(Float.isFinite(row[x])) {
					xmin = x;
					break;
				}
			}
		}
		return xmin;
	}

	public static int getYmin(float[][] pixels) { // no check for all na
		int w = pixels[0].length;
		int h1 = pixels.length - 1;
		for(int y = 0; y < h1; y++) {
			float[] row = pixels[y];				
			for(int x = 0; x < w; x++) {
				if(Float.isFinite(row[x])) {
					return y;
				}
			}
		}
		return h1;
	}

	public static int getXmax(float[][] pixels) { // no check for all na
		int w1 = pixels[0].length - 1;
		int h = pixels.length;
		int xmax = 0;
		for(int y = 0; y < h; y++) {
			float[] row = pixels[y];				
			for(int x = w1; x > xmax ; x--) {
				if(Float.isFinite(row[x])) {
					xmax = x;
					break;
				}
			}
		}
		return xmax;
	}

	public static int getYmax(float[][] pixels) { // no check for all na
		int w = pixels[0].length;
		int h1 = pixels.length - 1;
		for(int y = h1; y > 0 ; y--) {
			float[] row = pixels[y];				
			for(int x = 0; x < w; x++) {
				if(Float.isFinite(row[x])) {
					return y;
				}
			}
		}
		return 0;
	}
}
