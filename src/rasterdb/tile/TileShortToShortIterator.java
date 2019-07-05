package rasterdb.tile;

import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rasterunit.Tile;

public class TileShortToShortIterator implements Iterator<short[][]> {
	private static final Logger log = LogManager.getLogger();
	
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