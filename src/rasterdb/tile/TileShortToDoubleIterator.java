package rasterdb.tile;

import java.util.Iterator;

import rasterunit.Tile;
import util.frame.DoubleFrame;

public class TileShortToDoubleIterator implements Iterator<double[][]> {
	private final Iterator<Tile> it;
	private final double[][] empty;
	private final short na;

	public TileShortToDoubleIterator(Iterator<Tile> it, double[][] empty, short na) {
		this.it = it;
		this.empty = empty;
		this.na = na;
	}

	@Override
	public boolean hasNext() {
		return it.hasNext();
	}

	@Override
	public double[][] next() {
		Tile tile = it.next();
		if(tile == null) {
			return empty;
		} else {
			short[][] src = TileShort.decode(tile.data);
			double[][] dst = new double[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
			DoubleFrame.shortToDouble(src, dst, TilePixel.PIXELS_PER_ROW, TilePixel.PIXELS_PER_ROW, na);
			return dst;
		}
	}
}