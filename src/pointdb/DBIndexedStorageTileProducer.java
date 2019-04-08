package pointdb;

import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pointdb.base.PdbConst;
import pointdb.base.Rect;
import pointdb.base.Tile;
import pointdb.base.TileKey;
import pointdb.processing.tile.TileConsumer;
import pointdb.processing.tile.TileProducer;
import util.Util;
import util.indexedstorage.Box;
import util.indexedstorage.IndexEntry;
import util.indexedstorage.IndexedStorage;

public class DBIndexedStorageTileProducer implements TileProducer {
	static final Logger log = LogManager.getLogger();

	private final PointDB pointdb;
	private volatile boolean requestedStop = false;
	private final int tile_utm_min_x;
	private final int tile_utm_min_y;
	private final int tile_utm_max_x;
	private final int tile_utm_max_y;

	public static DBIndexedStorageTileProducer of(PointDB pointdb, Rect rect) {
		return of_UTMM(pointdb, rect.utmm_min_x, rect.utmm_min_y, rect.utmm_max_x, rect.utmm_max_y);
	}

	public static DBIndexedStorageTileProducer of_UTMM(PointDB pointdb, long utmm_min_x, long utmm_min_y, long utmm_max_x, long utmm_max_y) {
		int utm_min_x = (int)(utmm_min_x/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_min_y = (int)(utmm_min_y/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_max_x = (int)(utmm_max_x/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_max_y = (int)(utmm_max_y/PdbConst.LOCAL_SCALE_FACTOR);
		return of_UTM(pointdb, utm_min_x, utm_min_y, utm_max_x, utm_max_y);
	}

	public static DBIndexedStorageTileProducer of_UTM(PointDB pointdb, int utm_min_x, int utm_min_y, int utm_max_x, int utm_max_y) {
		int tile_utm_min_x = utm_min_x - (utm_min_x % PdbConst.UTM_TILE_SIZE);
		int tile_utm_min_y = utm_min_y - (utm_min_y % PdbConst.UTM_TILE_SIZE);
		int tile_utm_max_x = utm_max_x - (utm_max_x % PdbConst.UTM_TILE_SIZE);
		int tile_utm_max_y = utm_max_y - (utm_max_y % PdbConst.UTM_TILE_SIZE);
		return new DBIndexedStorageTileProducer(pointdb, tile_utm_min_x, tile_utm_min_y, tile_utm_max_x, tile_utm_max_y);
	}	

	private DBIndexedStorageTileProducer(PointDB pointdb, int tile_utm_min_x, int tile_utm_min_y, int tile_utm_max_x, int tile_utm_max_y) {		
		this.pointdb = pointdb;
		this.tile_utm_min_x = tile_utm_min_x;
		this.tile_utm_min_y = tile_utm_min_y;
		this.tile_utm_max_x = tile_utm_max_x;
		this.tile_utm_max_y = tile_utm_max_y;
	}

	@Override
	public void produce(TileConsumer tileConsumer) {
		try {
			Box box = new Box(null);
			IndexedStorage<TileKey, Tile> indexedStorage = pointdb.indexedStorage;
			ConcurrentSkipListMap<TileKey, IndexEntry> map = indexedStorage.indexMap;
			for(int y = tile_utm_min_y; y <= tile_utm_max_y; y += PdbConst.UTM_TILE_SIZE) {
				TileKey fromKey = new TileKey(tile_utm_min_x, y);
				TileKey toKey = new TileKey(tile_utm_max_x, y);
				Collection<TileKey> keys = map.subMap(fromKey, true, toKey, true).keySet();
				for(TileKey key:keys) {
					Tile tile = indexedStorage.get(key, box);					
					tileConsumer.nextTile(tile);
				}
				if(requestedStop) {
					break;
				}
			}
		} catch (Exception e) {
			Util.rethrow(e);
		}		
	}	

	@Override
	public void requestStop() {
		requestedStop = true;
	}
}