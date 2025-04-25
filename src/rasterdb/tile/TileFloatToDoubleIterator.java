package rasterdb.tile;

import java.util.Iterator;

import rasterunit.Tile;
import util.frame.DoubleFrame;

public class TileFloatToDoubleIterator implements Iterator<double[][]> {
	private final Iterator<Tile> it;
	private final double[][] empty;

	public TileFloatToDoubleIterator(Iterator<Tile> it, double[][] empty) {
		this.it = it;
		this.empty = empty;
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
			float[][] src = TileFloat.decode(tile.data);
			double[][] dst = new double[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
			DoubleFrame.floatToDouble(src, dst, TilePixel.PIXELS_PER_ROW, TilePixel.PIXELS_PER_ROW);
			return dst;
		}
	}
}