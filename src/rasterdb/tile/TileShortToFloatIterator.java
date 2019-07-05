package rasterdb.tile;

import java.util.Iterator;

import rasterunit.Tile;
import util.frame.FloatFrame;

public class TileShortToFloatIterator implements Iterator<float[][]> {
	private final Iterator<Tile> it;
	private final float[][] empty;
	private final short na;

	public TileShortToFloatIterator(Iterator<Tile> it, float[][] empty, short na) {
		this.it = it;
		this.empty = empty;
		this.na = na;
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
			short[][] src = TileShort.decode(tile.data);
			float[][] dst = new float[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
			FloatFrame.shortToFloat(src, dst, TilePixel.PIXELS_PER_ROW, TilePixel.PIXELS_PER_ROW, na);
			return dst;
		}
	}
}