package pointdb;

import java.util.NavigableSet;

import pointdb.base.PdbConst;
import pointdb.base.Rect;
import pointdb.base.TileKey;
import pointdb.processing.tilekey.TileKeyConsumer;
import pointdb.processing.tilekey.TileKeyProducer;

public class DBTileKeyProducer implements TileKeyProducer {
	private final PointDB pointdb;
	protected final int tile_utm_min_x;
	protected final int tile_utm_min_y;
	protected final int tile_utm_max_x;
	protected final int tile_utm_max_y;
	
	public static DBTileKeyProducer of(PointDB pointdb, Rect rect) {
		return of_UTMM(pointdb, rect.utmm_min_x, rect.utmm_min_y, rect.utmm_max_x, rect.utmm_max_y);
	}
	
	public static DBTileKeyProducer of_UTMM(PointDB pointdb, long utmm_min_x, long utmm_min_y, long utmm_max_x, long utmm_max_y) {
		int utm_min_x = (int)(utmm_min_x/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_min_y = (int)(utmm_min_y/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_max_x = (int)(utmm_max_x/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_max_y = (int)(utmm_max_y/PdbConst.LOCAL_SCALE_FACTOR);
		return of_UTM(pointdb, utm_min_x, utm_min_y, utm_max_x, utm_max_y);
	}
	
	public static DBTileKeyProducer of_UTM(PointDB pointdb, int utm_min_x, int utm_min_y, int utm_max_x, int utm_max_y) {
		int tile_utm_min_x = utm_min_x - (utm_min_x % PdbConst.UTM_TILE_SIZE);
		int tile_utm_min_y = utm_min_y - (utm_min_y % PdbConst.UTM_TILE_SIZE);
		int tile_utm_max_x = utm_max_x - (utm_max_x % PdbConst.UTM_TILE_SIZE);
		int tile_utm_max_y = utm_max_y - (utm_max_y % PdbConst.UTM_TILE_SIZE);
		return new DBTileKeyProducer(pointdb, tile_utm_min_x, tile_utm_min_y, tile_utm_max_x, tile_utm_max_y);
	}
	
	private DBTileKeyProducer(PointDB pointdb, int tile_utm_min_x, int tile_utm_min_y, int tile_utm_max_x, int tile_utm_max_y) {
		this.pointdb = pointdb;
		this.tile_utm_min_x = tile_utm_min_x;
		this.tile_utm_min_y = tile_utm_min_y;
		this.tile_utm_max_x = tile_utm_max_x;
		this.tile_utm_max_y = tile_utm_max_y;
	}
	
	@Override
	public void produce(TileKeyConsumer tileKeyConsumer) {
		NavigableSet<TileKey> fullKeySet = pointdb.isReadyIndexedStorage() ? pointdb.indexedStorage.indexMap.keySet() : pointdb.tileMetaMap.keySet();
		produce(fullKeySet, tileKeyConsumer);
	}
	
	/**
	 * use keySet instead of pointdb meta
	 * @param fullKeySet
	 * @param tileKeyConsumer
	 */
	public void produce(NavigableSet<TileKey> fullKeySet, TileKeyConsumer tileKeyConsumer) {
		TileKey firstKey = fullKeySet.first();
		TileKey lastKey = fullKeySet.last();
		int ymin = tile_utm_min_y < firstKey.y ? firstKey.y : tile_utm_min_y;
		int ymax = tile_utm_max_y > lastKey.y ? lastKey.y : tile_utm_max_y;
		for(int y = ymin; y <= ymax; y+=PdbConst.UTM_TILE_SIZE) {
			TileKey fromKey = new TileKey(tile_utm_min_x, y);
			TileKey toKey = new TileKey(tile_utm_max_x, y);
			NavigableSet<TileKey> keys = fullKeySet.subSet(fromKey, true, toKey, true);
			for(TileKey key:keys) {
				tileKeyConsumer.nextTileKey(key);
			}
		}	
	}
}