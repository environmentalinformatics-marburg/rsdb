package rasterunit;

import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.function.Consumer;

class TileKeyIterator implements Iterator<TileKey> {
	
	private static final Iterator<TileKey> EMPTY_TILE_KEY_ITERATOR = new Iterator<TileKey>() {
		@Override
		public boolean hasNext() {
			return false;
		}
		@Override
		public TileKey next() {
			return null;
		}			
	};

	private final RasterUnitStorage rasterUnit;
	private final int xmin;
	private final int xmax;
	private final Iterator<RowKey> rowKeyIt;
	private Iterator<TileKey> it = EMPTY_TILE_KEY_ITERATOR;

	public TileKeyIterator(RasterUnitStorage rasterUnit, Iterator<RowKey> rowKeyIt, int xmin, int xmax) {
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
			it = this.rasterUnit.getTileKeys(rowKey.t, rowKey.b, rowKey.y, xmin, xmax).iterator();			
		}
		return true;
	}

	@Override
	public TileKey next() {
		return it.next();
	}

	@Override
	public void forEachRemaining(Consumer<? super TileKey> action) {
		while (hasNext()) {
			action.accept(next());
		}
	}
}