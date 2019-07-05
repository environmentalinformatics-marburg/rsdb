package rasterdb.tile;

import java.util.Iterator;

import rasterunit.Tile;

public class NullFillTileYReverseIterator implements Iterator<Tile> {
	private final Iterator<Tile> it;
	private final int tymin;
	private final int txmin;
	private final int txmax;

	int cx;
	int cy;
	Tile cur;

	public NullFillTileYReverseIterator(Iterator<Tile> yReverseIterator, int tymin, int tymax, int txmin, int txmax) {
		this.it = yReverseIterator;
		this.tymin = tymin;
		this.txmin = txmin;			
		this.txmax = txmax;
		cx = txmin;
		cy = tymax;
		cur = it.hasNext() ? it.next() : null;
	}

	@Override
	public boolean hasNext() {
		return cy >= tymin && cx <= txmax;
	}

	@Override
	public Tile next() {
		if(cur != null && cur.y == cy && cur.x == cx) {
			cx++;
			if(cx > txmax) {
				cx = txmin;
				cy--;
			}
			Tile ret = cur;
			cur = it.hasNext() ? it.next() : null;
			return ret;
		} else {
			cx++;
			if(cx > txmax) {
				cx = txmin;
				cy--;
			}
			return null;
		}
	}
}