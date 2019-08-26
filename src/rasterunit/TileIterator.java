package rasterunit;

import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.function.Consumer;

class TileIterator implements Iterator<Tile> {
	
	private static final Iterator<Tile> EMPTY_TILE_ITERATOR = new Iterator<Tile>() {
		@Override
		public boolean hasNext() {
			return false;
		}
		@Override
		public Tile next() {
			return null;
		}			
	};

	private final RasterUnitStorage rasterUnit;
	private final int xmin;
	private final int xmax;
	private final Iterator<RowKey> rowKeyIt;
	private Iterator<Tile> it = EMPTY_TILE_ITERATOR;

	public TileIterator(RasterUnitStorage rasterUnit, Iterator<RowKey> rowKeyIt, int xmin, int xmax) {
		this.rasterUnit = rasterUnit;
		this.xmin = xmin;
		this.xmax = xmax;
		this.rowKeyIt = rowKeyIt;
	}

	@Override
	public boolean hasNext() {			
		while(!it.hasNext()) {
			if(!rowKeyIt.hasNext()) {
				return false;
			}
			RowKey rowKey = rowKeyIt.next();
			NavigableSet<TileKey> rowTileKeys = this.rasterUnit.getTileKeys(rowKey.t, rowKey.b, rowKey.y, xmin, xmax);
			if(rowTileKeys.isEmpty()) {
				it = EMPTY_TILE_ITERATOR;
			} else {					
				Collection<Tile> tiles = rasterUnit.getTiles(rowTileKeys.first(), rowTileKeys.last());
				it = tiles.iterator();
			}
		}
		return true;
	}

	@Override
	public Tile next() {
		return it.next();
	}

	@Override
	public void forEachRemaining(Consumer<? super Tile> action) {
		while (hasNext()) {
			action.accept(next());
		}
	}
}