package rasterdb.tile;

import java.util.Iterator;

import rasterunit.Tile;

public class TileShortToShortIterator implements Iterator<short[][]> {	
	
	private final Iterator<Tile> it;
	private final short[][] empty;

	public TileShortToShortIterator(Iterator<Tile> it, short[][] empty) {
		this.it = it;
		this.empty = empty;
	}

	@Override
	public boolean hasNext() {
		return it.hasNext();
	}

	@Override
	public short[][] next() {
		Tile tile = it.next();
		if(tile == null) {
			return empty;
		} else {
			return TileShort.decode(tile.data);
		}
	}
}