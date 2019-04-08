package pointdb;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;

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

public class DBAsyncIndexedStorageTileProducer implements TileProducer {
	static final Logger log = LogManager.getLogger();

	private final static int MAX_QUEUE_SIZE = 256;

	private final PointDB pointdb;
	private volatile boolean requestedStop = false;
	private final int tile_utm_min_x;
	private final int tile_utm_min_y;
	private final int tile_utm_max_x;
	private final int tile_utm_max_y;

	public static DBAsyncIndexedStorageTileProducer of(PointDB pointdb, Rect rect) {
		return of_UTMM(pointdb, rect.utmm_min_x, rect.utmm_min_y, rect.utmm_max_x, rect.utmm_max_y);
	}

	public static DBAsyncIndexedStorageTileProducer of_UTMM(PointDB pointdb, long utmm_min_x, long utmm_min_y, long utmm_max_x, long utmm_max_y) {
		int utm_min_x = (int)(utmm_min_x/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_min_y = (int)(utmm_min_y/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_max_x = (int)(utmm_max_x/PdbConst.LOCAL_SCALE_FACTOR);
		int utm_max_y = (int)(utmm_max_y/PdbConst.LOCAL_SCALE_FACTOR);
		return of_UTM(pointdb, utm_min_x, utm_min_y, utm_max_x, utm_max_y);
	}

	public static DBAsyncIndexedStorageTileProducer of_UTM(PointDB pointdb, int utm_min_x, int utm_min_y, int utm_max_x, int utm_max_y) {
		int tile_utm_min_x = utm_min_x - (utm_min_x % PdbConst.UTM_TILE_SIZE);
		int tile_utm_min_y = utm_min_y - (utm_min_y % PdbConst.UTM_TILE_SIZE);
		int tile_utm_max_x = utm_max_x - (utm_max_x % PdbConst.UTM_TILE_SIZE);
		int tile_utm_max_y = utm_max_y - (utm_max_y % PdbConst.UTM_TILE_SIZE);
		return new DBAsyncIndexedStorageTileProducer(pointdb, tile_utm_min_x, tile_utm_min_y, tile_utm_max_x, tile_utm_max_y);
	}	

	private DBAsyncIndexedStorageTileProducer(PointDB pointdb, int tile_utm_min_x, int tile_utm_min_y, int tile_utm_max_x, int tile_utm_max_y) {		
		this.pointdb = pointdb;
		this.tile_utm_min_x = tile_utm_min_x;
		this.tile_utm_min_y = tile_utm_min_y;
		this.tile_utm_max_x = tile_utm_max_x;
		this.tile_utm_max_y = tile_utm_max_y;
	}

	private void enqueue(BlockingQueue<Tile> queue, int emptyTileCount) {
		try {
			Box box = new Box(null);
			IndexedStorage<TileKey, Tile> indexedStorage = pointdb.indexedStorage;
			ConcurrentSkipListMap<TileKey, IndexEntry> map = indexedStorage.indexMap;

			for(int y=tile_utm_min_y;y<=tile_utm_max_y;y+=PdbConst.UTM_TILE_SIZE) {
				TileKey fromKey = new TileKey(tile_utm_min_x, y);
				TileKey toKey = new TileKey(tile_utm_max_x, y);
				Collection<TileKey> keys = map.subMap(fromKey, true, toKey, true).keySet();
				/*for(TileKey key:keys) {
					Tile tile = indexedStorage.get(key, box);					
					queue.put(tile);
				}*/
				indexedStorage.foreach(keys, box, tile->{
					try {
						queue.put(tile);				
					} catch (Exception e) {
						Util.rethrow(e);
					}
				});
				if(requestedStop) {
					break;
				}
			}
			for (int i = 0; i < emptyTileCount; i++) {
				queue.put(Tile.SENTINEL);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Util.rethrow(e);
		}
	}

	@Override
	public void produce(TileConsumer tileConsumer) {
		LinkedBlockingQueue<Tile> queue = new LinkedBlockingQueue<Tile>(MAX_QUEUE_SIZE); // A bit faster than LinkedTransferQueue
		ForkJoinPool.commonPool().execute(()->enqueue(queue, 1));
		try {
			for(;;) {
				Tile tile = queue.take();
				if(tile==Tile.SENTINEL) {
					break;
				}
				tileConsumer.nextTile(tile);
			}
		} catch (InterruptedException e) {
			Util.rethrow(e);
		}
	}

	/*@Override
	public void produce(TileConsumer tileConsumer) { // not thread safe if tileConsumer is not thread safe !!!
		LinkedBlockingQueue<Tile> queue = new LinkedBlockingQueue<Tile>(MAX_QUEUE_SIZE); // A bit faster than LinkedTransferQueue
		int div = 16;
		Phaser phaser = new Phaser();
		phaser.register();
		ForkJoinPool exe = ForkJoinPool.commonPool();
		for (int i = 0; i < div; i++) {
			int id = i;
			phaser.register();
			exe.execute(()-> {
				try {
					for(;;) {
						Tile tile = queue.take();
						if(tile==Tile.SENTINEL) {
							break;
						}
						tileConsumer.nextTile(tile);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
					Util.rethrow(e);
				} finally {
					phaser.arrive();
					log.info("worker finished "+id);
				}
			});
		}
		enqueue(queue, div);
		phaser.arriveAndAwaitAdvance();
		log.info("queue "+queue.size());
	}*/

	@Override
	public void requestStop() {
		requestedStop = true;
	}
}