package pointdb;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import pointdb.base.PdbConst;
import pointdb.base.Rect;
import pointdb.base.Tile;
import pointdb.base.TileKey;
import pointdb.processing.tile.TileConsumer;
import pointdb.processing.tile.TileProducer;

public class DBAsyncTileProducer implements TileProducer {

	private final static int MAX_QUEUE_SIZE = 256;
	private static final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final PointDB pointdb;
	private volatile boolean requestedStop = false;
	private final int tile_utm_min_x;
	private final int tile_utm_min_y;
	private final int tile_utm_max_x;
	private final int tile_utm_max_y;

	public static DBAsyncTileProducer of(PointDB pointdb, Rect rect) {
		return of_UTMM(pointdb, rect.utmm_min_x, rect.utmm_min_y, rect.utmm_max_x, rect.utmm_max_y);
	}

	public static DBAsyncTileProducer of_UTMM(PointDB pointdb, long utmm_min_x, long utmm_min_y, long utmm_max_x, long utmm_max_y) {
		int utm_min_x = (int)(utmm_min_x/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_min_y = (int)(utmm_min_y/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_max_x = (int)(utmm_max_x/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_max_y = (int)(utmm_max_y/PdbConst.LOCAL_SCALE_FACTOR);
		return of_UTM(pointdb, utm_min_x, utm_min_y, utm_max_x, utm_max_y);
	}

	public static DBAsyncTileProducer of_UTM(PointDB pointdb, int utm_min_x, int utm_min_y, int utm_max_x, int utm_max_y) {
		int tile_utm_min_x = utm_min_x - (utm_min_x % PdbConst.UTM_TILE_SIZE);
		int tile_utm_min_y = utm_min_y - (utm_min_y % PdbConst.UTM_TILE_SIZE);
		int tile_utm_max_x = utm_max_x - (utm_max_x % PdbConst.UTM_TILE_SIZE);
		int tile_utm_max_y = utm_max_y - (utm_max_y % PdbConst.UTM_TILE_SIZE);
		return new DBAsyncTileProducer(pointdb, tile_utm_min_x, tile_utm_min_y, tile_utm_max_x, tile_utm_max_y);
	}	

	private DBAsyncTileProducer(PointDB pointdb, int tile_utm_min_x, int tile_utm_min_y, int tile_utm_max_x, int tile_utm_max_y) {		
		this.pointdb = pointdb;
		this.tile_utm_min_x = tile_utm_min_x;
		this.tile_utm_min_y = tile_utm_min_y;
		this.tile_utm_max_x = tile_utm_max_x;
		this.tile_utm_max_y = tile_utm_max_y;
	}

	private void generate(BlockingQueue<Tile> queue) {
		try {
			for(int y=tile_utm_min_y;y<=tile_utm_max_y;y+=PdbConst.UTM_TILE_SIZE) {
				TileKey fromKey = new TileKey(tile_utm_min_x, y);
				TileKey toKey = new TileKey(tile_utm_max_x, y);
				Collection<Tile> rowTiles = pointdb.tileMap.subMap(fromKey, true, toKey, true).values();
				for(Tile tile:rowTiles) { //rowTiles.forEach is much slower!			
					queue.put(tile);
				}
				if(requestedStop) {
					break;
				}
			}
			queue.put(Tile.SENTINEL);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void produce(TileConsumer tileConsumer) {
		LinkedBlockingQueue<Tile> queue = new LinkedBlockingQueue<Tile>(MAX_QUEUE_SIZE); // A bit faster than LinkedTransferQueue
		executor.execute(()->generate(queue));
		try {
			for(;;) {
				Tile tile = queue.take();
				if(tile==Tile.SENTINEL) {
					break;
				}
				tileConsumer.nextTile(tile);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void requestStop() {
		requestedStop = true;
	}
}
