package pointdb;

import java.util.Iterator;

import pointdb.base.PdbConst;
import pointdb.base.Tile;
import pointdb.base.TileKey;

public class TileIterator implements Iterator<Tile> {

	private final PointDB pointdb;
	private final int tile_min_x;
	private final int tile_max_x;
	private final int tile_max_y;
	
	private int next_tile_y;
	private Iterator<Tile> currIt;

	public TileIterator(PointDB pointdb, int utm_min_x, int utm_min_y, int utm_max_x, int utm_max_y) {			
		this.pointdb = pointdb;
		this.tile_min_x = utm_min_x - (utm_min_x % PdbConst.UTM_TILE_SIZE);
		this.next_tile_y = utm_min_y - (utm_min_y % PdbConst.UTM_TILE_SIZE);
		this.tile_max_x = utm_max_x - (utm_max_x % PdbConst.UTM_TILE_SIZE);
		this.tile_max_y = utm_max_y - (utm_max_y % PdbConst.UTM_TILE_SIZE);
		nextRow();
	}

	private void nextRow() {
		while(next_tile_y<=tile_max_y) {
			TileKey fromKey = new TileKey(tile_min_x, next_tile_y);
			TileKey toKey = new TileKey(tile_max_x, next_tile_y);
			currIt = this.pointdb.tileMap.subMap(fromKey, true, toKey, true).values().iterator();
			next_tile_y += PdbConst.UTM_TILE_SIZE;
			if(currIt.hasNext()) {
				return;
			}
		}
	}

	@Override
	public boolean hasNext() {
		if(currIt.hasNext()) {
			return true;
		}
		nextRow();
		return currIt.hasNext();
	}
	@Override
	public Tile next() {
		if(currIt.hasNext()) {
			return currIt.next();
		}
		nextRow();
		return currIt.next();
	}
}