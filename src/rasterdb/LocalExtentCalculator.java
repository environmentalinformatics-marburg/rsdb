package rasterdb;

import java.io.IOException;

import rasterdb.tile.TileFloat;
import rasterdb.tile.TilePixel;
import rasterdb.tile.TileShort;
import rasterunit.RasterUnitStorage;
import rasterunit.TileKey;
import util.Range2d;

public class LocalExtentCalculator {

	public static Range2d calc(RasterDB rasterdb) {
		try {
		int bandMaxIndex = rasterdb.bandMap.keySet().stream().mapToInt(i->i).max().getAsInt();
		int[] bandTypes = new int[bandMaxIndex + 1];
		short[] bandShortNAs = new short[bandMaxIndex + 1];
		for(Band band : rasterdb.bandMap.values()) {
			bandTypes[band.index] = band.type;
			bandShortNAs[band.index] = band.getShortNA();
		}
		
		RasterUnitStorage rasterUnit = rasterdb.rasterUnit();
		Range2d tileRange = rasterUnit.getTileRange();
		int lxmin = TilePixel.PIXELS_PER_ROW_1;
		int lymin = TilePixel.PIXELS_PER_ROW_1;
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
			default:
				throw new RuntimeException("unknown type " + bandTypes[tileKey.b]);
			}
		}		
		return tileRange.mul(TilePixel.PIXELS_PER_ROW).add(lxmin, lymin, lxmax, lymax);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static int getXmin(short[][] pixels, short na) { // no check for all na
		int xmin = TilePixel.PIXELS_PER_ROW_1;
		for(int y = 0; y < TilePixel.PIXELS_PER_ROW; y++) {
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
		for(int y = 0; y < TilePixel.PIXELS_PER_ROW_1; y++) {
			short[] row = pixels[y];				
			for(int x = 0; x < TilePixel.PIXELS_PER_ROW; x++) {
				if(row[x] != na) {
					return y;
				}
			}
		}
		return TilePixel.PIXELS_PER_ROW_1;
	}
	
	public static int getXmax(short[][] pixels, short na) { // no check for all na
		int xmax = 0;
		for(int y = 0; y < TilePixel.PIXELS_PER_ROW; y++) {
			short[] row = pixels[y];				
			for(int x = TilePixel.PIXELS_PER_ROW_1; x > xmax ; x--) {
				if(row[x] != na) {
					xmax = x;
					break;
				}
			}
		}
		return xmax;
	}
	
	public static int getYmax(short[][] pixels, short na) { // no check for all na
		for(int y = TilePixel.PIXELS_PER_ROW_1; y > 0 ; y--) {
			short[] row = pixels[y];				
			for(int x = 0; x < TilePixel.PIXELS_PER_ROW; x++) {
				if(row[x] != na) {
					return y;
				}
			}
		}
		return 0;
	}
	
	public static int getXmin(float[][] pixels) { // no check for all na
		int xmin = TilePixel.PIXELS_PER_ROW_1;
		for(int y = 0; y < TilePixel.PIXELS_PER_ROW; y++) {
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
		for(int y = 0; y < TilePixel.PIXELS_PER_ROW_1; y++) {
			float[] row = pixels[y];				
			for(int x = 0; x < TilePixel.PIXELS_PER_ROW; x++) {
				if(Float.isFinite(row[x])) {
					return y;
				}
			}
		}
		return TilePixel.PIXELS_PER_ROW_1;
	}
	
	public static int getXmax(float[][] pixels) { // no check for all na
		int xmax = 0;
		for(int y = 0; y < TilePixel.PIXELS_PER_ROW; y++) {
			float[] row = pixels[y];				
			for(int x = TilePixel.PIXELS_PER_ROW_1; x > xmax ; x--) {
				if(Float.isFinite(row[x])) {
					xmax = x;
					break;
				}
			}
		}
		return xmax;
	}
	
	public static int getYmax(float[][] pixels) { // no check for all na
		for(int y = TilePixel.PIXELS_PER_ROW_1; y > 0 ; y--) {
			float[] row = pixels[y];				
			for(int x = 0; x < TilePixel.PIXELS_PER_ROW; x++) {
				if(Float.isFinite(row[x])) {
					return y;
				}
			}
		}
		return 0;
	}	

}
