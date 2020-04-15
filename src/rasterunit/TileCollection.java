package rasterunit;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Spliterator;

class TileCollection extends AbstractCollection<Tile> {
	private final RasterUnitStorage rasterUnit;
	private final int xmin;
	private final int xmax;
	private final Collection<RowKey> rowsKeys;
	private int calculatedSize = -1;

	public TileCollection(RasterUnitStorage rasterUnit, Collection<RowKey> rowsKeys, int xmin, int xmax) {
		this.rasterUnit = rasterUnit;
		this.xmin = xmin;
		this.xmax = xmax;
		this.rowsKeys = rowsKeys;
	}

	@Override
	public Iterator<Tile> iterator() {
		return new TileIterator(this.rasterUnit, rowsKeys.iterator(), xmin, xmax);
	}
	
	public Iterator<TileKey> keyIterator() {
		return new TileKeyIterator(this.rasterUnit, rowsKeys.iterator(), xmin, xmax);
	}

	@Override
	public int size() {
		if(this.calculatedSize == -1) {
			int size = 0;
			for(RowKey rowKey:rowsKeys) {
				NavigableSet<TileKey> rowtileKeys = this.rasterUnit.getTileKeys(rowKey.t, rowKey.b, rowKey.y, xmin, xmax);
				size += rowtileKeys.size();
			}
			this.calculatedSize = size;
		}
		return this.calculatedSize;
	}
	
	public int rowCount() {
		return rowsKeys.size();
	}

	@Override
	public Spliterator<Tile> spliterator() {
		return new TileSpliterator(this.rasterUnit, rowsKeys.iterator(), xmin, xmax, size());
	}
	
	@Override
	public String toString() {
        Iterator<TileKey> it = keyIterator();
        if (! it.hasNext())
            return "[]";

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (;;) {
        	TileKey e = it.next();
            sb.append(e);
            if (! it.hasNext())
                return sb.append(']').toString();
            sb.append(',').append(' ');
        }
    }
}