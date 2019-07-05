package rasterdb.tile;

import java.util.Iterator;

import rasterunit.Tile;

public class TileFloatToFloatIterator implements Iterator<float[][]> {
	private final Iterator<Tile> it;
	private final float[][] empty;

	public TileFloatToFloatIterator(Iterator<Tile> it, float[][] empty) {
		this.it = it;
		this.empty = empty;
	}

	@Override
	public boolean hasNext() {
		return it.hasNext();
	}

	@Override
	public float[][] next() {
		Tile tile = it.next();
		if(tile == null) {
			return empty;
		} else {
			return TileFloat.decode(tile.data);
		}
	}
}