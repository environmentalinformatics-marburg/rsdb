package rasterdb.cell;

import java.io.IOException;
import java.util.Collection;

import me.lemire.integercompression.FastPFOR;
import rasterdb.Band;
import rasterdb.tile.Processing.Commiter;
import rasterunit.BandKey;
import rasterunit.RasterUnitStorage;
import rasterunit.Tile;
import rasterunit.TileKey;
import util.Range2d;

public abstract class Cell<T> {
	

	public final int pixel_len;
	public final int cell_pixel_count;

	public Cell(int pixel_len) {
		this.pixel_len = pixel_len;
		this.cell_pixel_count = pixel_len * pixel_len;
	}

	public abstract byte[] encodeCell(T pixels);
	public abstract T decodeCell(Tile tile);
	public abstract void decodeCell(Tile tile, int xCellStart, int yCellStart, int xCellEnd, T target, int xTargetStart, int yTargetStart, int xTargetEnd, int yTargetEnd);
	public abstract void decodeCellMerge(Tile tile, T cellPixels, Band band);
	public abstract void decodeCellDiv(Tile tile, int xCellStart, int yCellStart, int div, T target, Band band, int xTargetStart, int yTargetStart, int xTargetEnd, int yTargetEnd);
	
	public abstract T createEmptyUninitialized(int width, int height);
	public abstract T createEmptyNA(int width, int height, Band band);
	public abstract void copy(T pixels, int tx, int ty, T tilePixels, int xStart, int txlen, int yStart, int yEnd);	
	
	public abstract boolean isNotAllNaCellPixels(T cellPixels, Band band);
	
	public void decodeCellDiv(Tile tile, int div,Band band) {
		int div_len = pixel_len / div;
		T target = createEmptyUninitialized(div_len, div_len);
		decodeCellDiv(tile, 0, 0, div, target, band, 0, 0, div_len - 1, div_len - 1);
	}
	
	public boolean writeCellMerge(RasterUnitStorage storage, int t, Band band, int y, int x, T cellPixels) throws IOException {
		if(isNotAllNaCellPixels(cellPixels, band)) {
			TileKey tileKey = new TileKey(t, band.index, y, x);
			if(storage.tileKeysReadonly().contains(tileKey)) {
				Tile tile = storage.readTile(tileKey);
				if(tile != null) {
					decodeCellMerge(tile, cellPixels, band);
				}
			}
			byte[] data = encodeCell(cellPixels);
			storage.writeTile(new Tile(tileKey, band.type, data));
			return true;
		} else {
			return false;
		}
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
	
	public int cellDivToPixelMin(int t, int div) {
		int div_len = pixel_len / div;
		return t * div_len;
	}

	public int cellDivToPixelMax(int t, int div) {
		int div_len = pixel_len / div;
		return t * div_len + div_len - 1;
	}

	public static ThreadLocal<FastPFOR> threadLocal_fastPFOR = new ThreadLocal<FastPFOR>() {
		@Override
		protected FastPFOR initialValue() {
			return new FastPFOR();
		}		
	};

	public T read(RasterUnitStorage storage, int t, Band band, Range2d pixelRange, int div) {
		//Logger.info("read " + pixelRange);
		if(div < 1 || pixel_len / div < 1 || pixel_len % div != 0) {
			throw new RuntimeException("invalid div for pixel_len   " + pixel_len + " / " + div);
		}
		if(div == 1) {
			return read(storage, t, band, pixelRange);
		} else {
			return readDiv(storage, t, band, pixelRange, div);
			//T data = read(storage, t, band, pixelRange);			
			//return copyDiv(data, pixelRange.getWidth(), pixelRange.getHeight(), div, band);
		}
	}

	public T read(RasterUnitStorage storage, int t, Band band, Range2d pixelRange) {
		T result = createEmptyNA(pixelRange.getWidth(), pixelRange.getHeight(), band);
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
	
	public T readDiv(RasterUnitStorage storage, int t, Band band, Range2d pixelRange, int div) {
		int tymin = pixelToCell(pixelRange.ymin);
		int tymax = pixelToCell(pixelRange.ymax);
		int txmin = pixelToCell(pixelRange.xmin); 
		int txmax = pixelToCell(pixelRange.xmax);
		Collection<Tile> tiles = storage.readTiles(t, band.index, tymin, tymax, txmin, txmax);
		
		Range2d pixelRangeDiv = pixelRange.floorDiv(div);
		T result = createEmptyNA(pixelRangeDiv.getWidth(), pixelRangeDiv.getHeight(), band);
		
		for(Tile tile:tiles) {
			decodeCellDiv(tile, div, result, pixelRangeDiv, band);
		}
		return result;	
	}

	public T createEmptyCell(Band band) {
		return createEmptyNA(pixel_len, pixel_len, band);
	}

	public int writeMerge(RasterUnitStorage rasterUnit, int t, Band band, T pixels, int pixelYmin, int pixelXmin, int xlen, int ylen) throws IOException {
		int tileWriteCount = 0;
		int pixelXmax = pixelXmin + xlen - 1;
		int pixelYmax = pixelYmin + ylen - 1;
		int tileXmin = pixelToCell(pixelXmin);
		int tileYmin = pixelToCell(pixelYmin);
		int tileXmax = pixelToCell(pixelXmax);
		int tileYmax = pixelToCell(pixelYmax);
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
				T tilePixels = createEmptyCell(band); // na fill: not all pixels may be written
				int ty = yMin + yStart;
				int tx = xMin + xStart;
				int txlen = xEnd - xStart + 1;
				copy(pixels, tx, ty, tilePixels, xStart, txlen, yStart, yEnd);				
				if(writeCellMerge(rasterUnit, t, band, tileY, tileX, tilePixels)) {
					tileWriteCount++;
				}
			}
		}
		return tileWriteCount;
	}

	public void writeStorageBandDiv(Band band, int div, RasterUnitStorage srcStorage, BandKey srcBandKey, RasterUnitStorage dstStorage, BandKey dstBandKey, Commiter counter) throws IOException {
		if(div < 2 ) {
			throw new RuntimeException("invalid div: " + div);
		}
		if(pixel_len % div != 0) {
			throw new RuntimeException("invalid div: " + div + "  for pixel_len");
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

		int pixel_len_div = pixel_len / div;

		for(int y=ymin;y<=ymax;y++) {
			int tymin = div * y;
			int tymax = tymin + divm1;
			int x = xmin;
			int tilesWrittenInRow = 0;
			while(x <= xmax) {
				final int BATCH_SIZE_MAX = 256;
				int remaining_tiles = xmax - x + 1;
				int batch_size = BATCH_SIZE_MAX <= remaining_tiles ? BATCH_SIZE_MAX : remaining_tiles;
				@SuppressWarnings("unchecked")
				T[] target = (T[]) new Object[batch_size];
				int txmin = div * x;
				int txmax = txmin + div * batch_size - 1;
				Collection<Tile> tiles = srcStorage.readTiles(srcBandKey.t, srcBandKey.b, tymin, tymax, txmin, txmax);
				for(Tile tile:tiles) {
					int targetIndex = Math.floorDiv(tile.x - txmin, div);
					if(target[targetIndex] == null) {
						target[targetIndex] = createEmptyCell(band); // na fill: not all pixels may be written
					}
					int iy = tile.y - tymin;
					int ix = tile.x - txmin - targetIndex * div;
					//T pixels = decodeCell(tile);
					//copyDiv(pixels, div, band, target[targetIndex], ix * pixel_len_div, iy * pixel_len_div);
					int xTargetStart = ix * pixel_len_div;
					int yTargetStart = iy * pixel_len_div;
					int xTargetEnd = xTargetStart + pixel_len_div - 1;
					int yTargetEnd = yTargetStart + pixel_len_div - 1; 
					decodeCellDiv(tile, 0, 0, div, target[targetIndex], band, xTargetStart, yTargetStart, xTargetEnd, yTargetEnd);
				}				
				for (int targetIndex = 0; targetIndex < target.length; targetIndex++) {
					if(target[targetIndex] != null) {
						TileKey tileKey = dstBandKey.toTileKey(y, x + targetIndex);
						byte[] data = encodeCell(target[targetIndex]);
						dstStorage.writeTile(new Tile(tileKey, CellType.INT16, data));
						tilesWrittenInRow++;
					}
				}
				x += batch_size; 
			}
			counter.add(tilesWrittenInRow);
		}		
	}
	
	/*public T copyDiv(T data, int w, int h, int div, Band band) {
		int wr = w / div;
		int hr = h / div;
		T target = createEmptyUninitialized(wr, hr);
		//Logger.info("w h " + w +"  " + h +  " div  " + div  +  "   " + wr + "  " + hr + "   " + w % div + "   " + h % div);		
		copyDiv(data, div, band, target, 0, 0);
		return target;
	}*/
	
	public void decodeCell(Tile tile, T target, Range2d targetRange) {

		//Logger.info("decodeCell target  " + targetRange.xmin + " " + targetRange.ymin + "   " + targetRange.xmax + " " + targetRange.ymax);

		int cxmin = cellToPixelMin(tile.x);
		int cymin = cellToPixelMin(tile.y);
		int cxmax = cellToPixelMax(tile.x);
		int cymax = cellToPixelMax(tile.y);

		//Logger.info("decodeCell cell    " + cxmin + " " + cymin + "   " + cxmax + " " + cymax);

		int xCoveredMin = cxmin < targetRange.xmin ? targetRange.xmin : cxmin;
		int yCoveredMin = cymin < targetRange.ymin ? targetRange.ymin : cymin;
		int xCoveredMax = cxmax > targetRange.xmax ? targetRange.xmax : cxmax;
		int yCoveredMax = cymax > targetRange.ymax ? targetRange.ymax : cymax;

		//Logger.info("decodeCell covered " + xCoveredMin + " " + yCoveredMin + "   " + xCoveredMax + " " + yCoveredMax);

		int xTargetStart = xCoveredMin - targetRange.xmin;
		int yTargetStart = yCoveredMin - targetRange.ymin;
		int xTargetEnd = xCoveredMax - targetRange.xmin;
		int yTargetEnd = yCoveredMax - targetRange.ymin;

		//Logger.info("decodeCell xTarget " + xTargetStart + " - " + xTargetEnd + " :  " + (xTargetEnd - xTargetStart + 1));
		//Logger.info("decodeCell yTarget " + yTargetStart + " - " + yTargetEnd + " :  " + (yTargetEnd - yTargetStart + 1));

		int xCellStart = xCoveredMin - cxmin;
		int yCellStart = yCoveredMin - cymin;
		int xCellEnd = xCoveredMax - cxmin;
		//int yCellEnd = yCoveredMax - cymin;

		//Logger.info("decodeCell xCell   " + xCellStart + " - " + xCellEnd + " :  " + (xCellEnd - xCellStart + 1));
		//Logger.info("decodeCell yCell   " + yCellStart + " - " + yCellEnd + " :  " + (yCellEnd - yCellStart + 1));

		decodeCell(tile, xCellStart, yCellStart, xCellEnd, target, xTargetStart, yTargetStart, xTargetEnd, yTargetEnd);
	}
	
	public void decodeCellDiv(Tile tile, int div, T target, Range2d targetRange, Band band) {
		int cxmin = cellDivToPixelMin(tile.x, div);
		int cymin = cellDivToPixelMin(tile.y, div);
		int cxmax = cellDivToPixelMax(tile.x, div);
		int cymax = cellDivToPixelMax(tile.y, div);

		//Logger.info("c " + cxmin + " " + cymin + " " + cxmax + " " + cymax);

		int xCoveredMin = cxmin < targetRange.xmin ? targetRange.xmin : cxmin;
		int yCoveredMin = cymin < targetRange.ymin ? targetRange.ymin : cymin;
		int xCoveredMax = cxmax > targetRange.xmax ? targetRange.xmax : cxmax;
		int yCoveredMax = cymax > targetRange.ymax ? targetRange.ymax : cymax;

		int xTargetStart = xCoveredMin - targetRange.xmin;
		int yTargetStart = yCoveredMin - targetRange.ymin;
		int xTargetEnd = xCoveredMax - targetRange.xmin;
		int yTargetEnd = yCoveredMax - targetRange.ymin;

		int xCellStart = (xCoveredMin - cxmin) * div;
		int yCellStart = (yCoveredMin - cymin) * div;
		//int xCellEnd = (xCoveredMax - cxmin) * div + (div - 1);
		//int yCellEnd = (yCoveredMax - cymin) * div + (div - 1);

		//Logger.info("t " + xTargetStart + " " + yTargetStart + " " + xTargetEnd + " " + yTargetEnd);

		decodeCellDiv(tile, xCellStart, yCellStart, div, target, band, xTargetStart, yTargetStart, xTargetEnd, yTargetEnd);
	}
}