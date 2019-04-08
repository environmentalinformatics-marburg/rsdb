package pointdb;

import java.util.Collection;

import pointdb.base.PdbConst;
import pointdb.base.Rect;
import pointdb.base.TileKey;
import pointdb.base.TileMeta;
import pointdb.processing.tilemeta.TileMetaConsumer;
import pointdb.processing.tilemeta.TileMetaProducer;

public class DBTileMetaProducer implements TileMetaProducer {
	private final PointDB pointdb;
	protected final int tile_utm_min_x;
	protected final int tile_utm_min_y;
	protected final int tile_utm_max_x;
	protected final int tile_utm_max_y;
	
	public static DBTileMetaProducer of(PointDB pointdb, Rect rect) {
		return of_UTMM(pointdb, rect.utmm_min_x, rect.utmm_min_y, rect.utmm_max_x, rect.utmm_max_y);
	}
	
	public static DBTileMetaProducer of_UTMM(PointDB pointdb, long utmm_min_x, long utmm_min_y, long utmm_max_x, long utmm_max_y) {
		int utm_min_x = (int)(utmm_min_x/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_min_y = (int)(utmm_min_y/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_max_x = (int)(utmm_max_x/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_max_y = (int)(utmm_max_y/PdbConst.LOCAL_SCALE_FACTOR);
		return of_UTM(pointdb, utm_min_x, utm_min_y, utm_max_x, utm_max_y);
	}
	
	public static DBTileMetaProducer of_UTM(PointDB pointdb, int utm_min_x, int utm_min_y, int utm_max_x, int utm_max_y) {
		int tile_utm_min_x = utm_min_x - (utm_min_x % PdbConst.UTM_TILE_SIZE);
		int tile_utm_min_y = utm_min_y - (utm_min_y % PdbConst.UTM_TILE_SIZE);
		int tile_utm_max_x = utm_max_x - (utm_max_x % PdbConst.UTM_TILE_SIZE);
		int tile_utm_max_y = utm_max_y - (utm_max_y % PdbConst.UTM_TILE_SIZE);
		return new DBTileMetaProducer(pointdb, tile_utm_min_x, tile_utm_min_y, tile_utm_max_x, tile_utm_max_y);
	}
	
	private DBTileMetaProducer(PointDB pointdb, int tile_utm_min_x, int tile_utm_min_y, int tile_utm_max_x, int tile_utm_max_y) {
		this.pointdb = pointdb;
		this.tile_utm_min_x = tile_utm_min_x;
		this.tile_utm_min_y = tile_utm_min_y;
		this.tile_utm_max_x = tile_utm_max_x;
		this.tile_utm_max_y = tile_utm_max_y;
	}
	
	@Override
	public void produce(TileMetaConsumer tileMetaConsumer) {
		for(int y=tile_utm_min_y;y<=tile_utm_max_y;y+=PdbConst.UTM_TILE_SIZE) {
			TileKey fromKey = new TileKey(tile_utm_min_x, y);
			TileKey toKey = new TileKey(tile_utm_max_x, y);
			Collection<TileMeta> rowMetaTiles = pointdb.tileMetaMap.subMap(fromKey, true, toKey, true).values();
			for(TileMeta tileMeta:rowMetaTiles) {
				tileMetaConsumer.nextTileMeta(tileMeta);
			}
		}
	}
}