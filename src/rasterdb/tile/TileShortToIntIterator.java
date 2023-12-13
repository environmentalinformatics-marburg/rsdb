package rasterdb.tile;

import java.util.Iterator;

import rasterunit.Tile;
import util.frame.FloatFrame;
import util.frame.IntFrame;

public class TileShortToIntIterator implements Iterator<int[][]> {
	
	private final Iterator<Tile> it;
	private final int[][] empty;
	private final short na_src;
	private final int na_dst;

	public TileShortToIntIterator(Iterator<Tile> it, int[][] empty, short na_src, int na_dst) {
		this.it = it;
		this.empty = empty;
		this.na_src = na_src;
		this.na_dst = na_dst;
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
			short[][] src = TileShort.decode(tile.data);
			int[][] dst = new int[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
			IntFrame.shortToInt(src, dst, TilePixel.PIXELS_PER_ROW, TilePixel.PIXELS_PER_ROW, na_src, na_dst);
			return dst;
		}
	}
}