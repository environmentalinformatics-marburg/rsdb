package rasterunit;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

class TileSpliterator extends TileIterator implements Spliterator<Tile> {
	private final RasterUnitStorage rasterUnit;
	static final int BATCH_UNIT = 16;
	static final int MAX_BATCH = 16;
	static final int CHARACTERISTICS = Spliterator.SIZED | Spliterator.SUBSIZED;
	private int size;
	private int batch;

	public TileSpliterator(RasterUnitStorage rasterUnit, Iterator<RowKey> rowKeyIt, int xmin, int xmax, int size) {
		super(rasterUnit, rowKeyIt, xmin, xmax);
		this.rasterUnit = rasterUnit;
		this.size = size;
	}

	@Override
	public int characteristics() {
		return CHARACTERISTICS;
	}

	@Override
	public long estimateSize() {
		return size;
	}

	@Override
	public boolean tryAdvance(Consumer<? super Tile> action) {
		if (hasNext()) {
			action.accept(next());
			return true;
		}
		return false;
	}

	@Override
	public Spliterator<Tile> trySplit() {
		int len = size;
		if (len > 1 && hasNext()) {
			int n = batch + BATCH_UNIT;
			if (n > len) {
				n = len;
			}
			if (n > MAX_BATCH) {
				n = MAX_BATCH;
			}
			Object[] a = new Object[n];
			int j = 0;
			do { 
				a[j] = next(); 
			} while (++j < n && hasNext());
			batch = j;
			size -= j;
			return Spliterators.spliterator(a, 0, j, CHARACTERISTICS);
		}
		return null;
	}

}