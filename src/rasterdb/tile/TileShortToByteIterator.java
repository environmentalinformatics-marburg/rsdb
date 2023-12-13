package rasterdb.tile;

import java.util.Iterator;

import rasterunit.Tile;
import util.frame.ByteFrame;

public class TileShortToByteIterator implements Iterator<byte[][]> {	
	
	private final Iterator<Tile> it;
	private final byte[][] empty;
	private final short na_src;
	private final byte na_dst;

	public TileShortToByteIterator(Iterator<Tile> it, byte[][] empty, short na_src, byte na_dst) {
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
	public byte[][] next() {
		Tile tile = it.next();
		if(tile == null) {
			return empty;
		} else {
			short[][] src = TileShort.decode(tile.data);
			byte[][] dst = new byte[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
			ByteFrame.shortToByte(src, dst, TilePixel.PIXELS_PER_ROW, TilePixel.PIXELS_PER_ROW, na_src, na_dst);
			return dst;
		}
	}
}