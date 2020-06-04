package rasterunit;

import java.io.IOException;
import java.util.Collection;
import java.util.NavigableSet;

import util.Range2d;
import util.collections.ReadonlyNavigableSetView;

public interface RasterUnitStorage extends AutoCloseable {
	ReadonlyNavigableSetView<TileKey> tileKeysReadonly();
	ReadonlyNavigableSetView<BandKey> bandKeysReadonly();
	ReadonlyNavigableSetView<Integer> timeKeysReadonly();
	boolean isEmpty();
	
	Tile readTile(TileKey tileKey) throws IOException;
	Tile readTile(int t, int b, int y, int x) throws IOException;
	TileCollection readTiles(int t, int b, int ymin, int ymax, int xmin, int xmax);
	NavigableSet<TileKey> getTileKeys(int t, int b, int y, int xmin, int xmax);
	Collection<Tile> getTiles(TileKey keyXmin, TileKey keyXmax);
	NavigableSet<RowKey> getRowKeys(int t, int b, int ymin, int ymax);

	/**
	 * 
	 * @return range or null
	 */
	Range2d getTileRange();
	
	/**
	 * 
	 * @return range or null
	 */	
	Range2d getTileRange(BandKey bandKey);
	
	/**
	 * 
	 * @return range or null
	 */
	Range2d getTileRangeOfSubset(BandKey bandKey, Range2d subsetTileRange);
	
	void writeTile(Tile tile) throws IOException;
	
	void commit();
	void flush() throws IOException;
	void close() throws IOException;
	int getTileCount();
	
	default NavigableSet<RowKey> getRowKeysReverse(int t, int b, int ymin, int ymax) {
		return getRowKeys(t, b, ymin, ymax).descendingSet();
	}
	
	default Collection<Tile> getTilesYReverse(int t, int b, int ymin, int ymax, int xmin, int xmax) {
		Collection<RowKey> rows = getRowKeysReverse(t, b, ymin, ymax);
		return new TileCollection(this, rows, xmin, xmax);
	}
	long removeAllTilesOfTimestamp(int t) throws IOException;
	long removeAllTilesOfBand(int b) throws IOException;
	
	long calculateInternalFreeSize();
	long calculateStorageSize();
	int calculateTileCount();
	
	/**
	 * stats or null
	 * @return
	 */
	long[] calculateTileSizeStats();
	
	default int getTilePixelLen() {
		return 256;
	}
}
