package pointdb;

import java.util.Collection;

import pointdb.base.PdbConst;
import pointdb.base.Rect;
import pointdb.base.Tile;
import pointdb.base.TileKey;
import pointdb.processing.tile.TileConsumer;
import pointdb.processing.tile.TileProducer;

public class DBTileProducer implements TileProducer {
	
	private final PointDB pointdb;
	private volatile boolean requestedStop = false;
	private final int tile_utm_min_x;
	private final int tile_utm_min_y;
	private final int tile_utm_max_x;
	private final int tile_utm_max_y;
	
	public static DBTileProducer of(PointDB pointdb, Rect rect) {
		return of_UTMM(pointdb, rect.utmm_min_x, rect.utmm_min_y, rect.utmm_max_x, rect.utmm_max_y);
	}
	
	public static DBTileProducer of_UTMM(PointDB pointdb, long utmm_min_x, long utmm_min_y, long utmm_max_x, long utmm_max_y) {
		int utm_min_x = (int)(utmm_min_x/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_min_y = (int)(utmm_min_y/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_max_x = (int)(utmm_max_x/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_max_y = (int)(utmm_max_y/PdbConst.LOCAL_SCALE_FACTOR);
		return of_UTM(pointdb, utm_min_x, utm_min_y, utm_max_x, utm_max_y);
	}
	
	public static DBTileProducer of_UTM(PointDB pointdb, int utm_min_x, int utm_min_y, int utm_max_x, int utm_max_y) {
		int tile_utm_min_x = utm_min_x - (utm_min_x % PdbConst.UTM_TILE_SIZE);
		int tile_utm_min_y = utm_min_y - (utm_min_y % PdbConst.UTM_TILE_SIZE);
		int tile_utm_max_x = utm_max_x - (utm_max_x % PdbConst.UTM_TILE_SIZE);
		int tile_utm_max_y = utm_max_y - (utm_max_y % PdbConst.UTM_TILE_SIZE);
		return new DBTileProducer(pointdb, tile_utm_min_x, tile_utm_min_y, tile_utm_max_x, tile_utm_max_y);
	}	
	
	private DBTileProducer(PointDB pointdb, int tile_utm_min_x, int tile_utm_min_y, int tile_utm_max_x, int tile_utm_max_y) {		
		this.pointdb = pointdb;
		this.tile_utm_min_x = tile_utm_min_x;
		this.tile_utm_min_y = tile_utm_min_y;
		this.tile_utm_max_x = tile_utm_max_x;
		this.tile_utm_max_y = tile_utm_max_y;
	}
	
	@Override
	public void produce(TileConsumer tileConsumer) {
		for(int y=tile_utm_min_y;y<=tile_utm_max_y;y+=PdbConst.UTM_TILE_SIZE) {
			TileKey fromKey = new TileKey(tile_utm_min_x, y);
			TileKey toKey = new TileKey(tile_utm_max_x, y);
			Collection<Tile> rowTiles = pointdb.tileMap.subMap(fromKey, true, toKey, true).values();
			for(Tile tile:rowTiles) {
				tileConsumer.nextTile(tile);
			}
			if(requestedStop) {
				break;
			}
		}
	}
	
	@Override
	public void requestStop() {
		requestedStop = true;
	}
}
