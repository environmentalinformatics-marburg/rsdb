package rasterdb.tile;

import java.util.Iterator;

import rasterunit.Tile;
import util.frame.ByteFrame;

public class TileFloatToByteIterator implements Iterator<byte[][]> {	
	
	private final Iterator<Tile> it;
	private final byte[][] empty;
	private final byte na_dst;

	public TileFloatToByteIterator(Iterator<Tile> it, byte[][] empty, byte na_dst) {
		this.it = it;
		this.empty = empty;
		this.na_dst = na_dst;
	}

	@Override
	public boolean hasNext() {
		return it.hasNext();
	}

	@Override
	public byte[][] next() {
		Tile tile = it.next();
		if(tile == null) {
			return empty;
		} else {
			float[][] src = TileFloat.decode(tile.data);
			byte[][] dst = new byte[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
			ByteFrame.floatToByte(src, dst, TilePixel.PIXELS_PER_ROW, TilePixel.PIXELS_PER_ROW, na_dst);
			return dst;
		}
	}
}