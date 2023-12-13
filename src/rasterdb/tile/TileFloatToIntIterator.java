package rasterdb.tile;

import java.util.Iterator;

import rasterunit.Tile;
import util.frame.IntFrame;

public class TileFloatToIntIterator implements Iterator<int[][]> {	
	
	private final Iterator<Tile> it;
	private final int[][] empty;
	private final int na_target;

	public TileFloatToIntIterator(Iterator<Tile> it, int[][] empty, int na_target) {
		this.it = it;
		this.empty = empty;
		this.na_target = na_target;
	}

	@Override
	public boolean hasNext() {
		return it.hasNext();
	}

	@Override
	public int[][] next() {
		Tile tile = it.next();
		if(tile == null) {
			return empty;
		} else {
			float[][] src = TileFloat.decode(tile.data);
			int[][] dst = new int[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
			IntFrame.floatToInt(src, dst, TilePixel.PIXELS_PER_ROW, TilePixel.PIXELS_PER_ROW, na_target);
			return dst;
		}
	}
}