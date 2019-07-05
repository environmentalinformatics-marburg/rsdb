package rasterdb.tile;

import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rasterunit.Tile;
import util.frame.ShortFrame;

public class TileFloatToShortIterator implements Iterator<short[][]> {
	private static final Logger log = LogManager.getLogger();
	
	private final Iterator<Tile> it;
	private final short[][] empty;
	private final short na_target;

	public TileFloatToShortIterator(Iterator<Tile> it, short[][] empty, short na_target) {
		this.it = it;
		this.empty = empty;
		this.na_target = na_target;
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
			float[][] src = TileFloat.decode(tile.data);
			short[][] dst = new short[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
			ShortFrame.floatToShort(src, dst, TilePixel.PIXELS_PER_ROW, TilePixel.PIXELS_PER_ROW, na_target);
			return dst;
		}
	}
}