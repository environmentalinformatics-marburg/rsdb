package rasterunit;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DB.BTreeMapMaker;
import org.mapdb.DBMaker;
import org.mapdb.DBMaker.Maker;
import org.mapdb.Store;
import org.xerial.snappy.Snappy;

import me.lemire.integercompression.FastPFOR;
import me.lemire.integercompression.IntCompressor;
import me.lemire.integercompression.SkippableComposition;
import me.lemire.integercompression.VariableByte;
import util.Range2d;
import util.Serialisation;
import util.Timer;
import util.collections.ReadonlyNavigableSetView;


public class RasterUnit implements RasterUnitStorage {
	private static final Logger log = LogManager.getLogger();

	private BTreeMap<TileKey,Tile> tileMap;

	private NavigableSet<TileKey> tileKeys;
	private NavigableSet<RowKey> rowKeys;
	private NavigableSet<BandKey> bandKeys;
	private NavigableSet<Integer> timeKeys;

	private DB tileMapDb;
	private final Path cachePath;
	private org.mapdb.Atomic.Integer databaseCacheTimestamp;

	public final ReadonlyNavigableSetView<TileKey> tileKeysReadonly;
	public final ReadonlyNavigableSetView<BandKey> bandKeysReadonly;
	public final ReadonlyNavigableSetView<Integer> timeKeysReadonly;

	/**
	 * some tiles written since last commit
	 */
	private boolean tilesWritten = false;

	/**
	 * some tiles written and committed ==> cache file is outdated
	 */
	private boolean cacheFileOutdated = false;

	public RasterUnit(Path root, String name, boolean fast_unsafe_import) {
		this(root, name, name + ".cache", fast_unsafe_import);
	}

	public RasterUnit(Path root, String name, String cacheName, boolean fast_unsafe_import) {
		cachePath = root.resolve(cacheName);
		Maker tileMapDbMaker = DBMaker.fileDB(root.resolve(name).toFile());
		if(fast_unsafe_import) {
			tileMapDbMaker.transactionDisable();
			tileMapDbMaker.asyncWriteEnable();
			tileMapDbMaker.asyncWriteFlushDelay(2000);
			tileMapDbMaker.storeExecutorEnable();
			tileMapDbMaker.executorEnable();
			tileMapDbMaker.asyncWriteQueueSize(1024);
			tileMapDbMaker.fileChannelEnable();
		}
		tileMapDb = tileMapDbMaker.make();
		org.mapdb.Atomic.String fileType = tileMapDb.atomicString("type");
		if(fileType.get().isEmpty()) {
			fileType.set("RasterUnit");
			tileMapDb.commit();
		} else {
			if(!fileType.get().equals("RasterUnit")) {
				throw new RuntimeException("wrong file type: "+fileType.get());
			}
		}
		org.mapdb.Atomic.String version = tileMapDb.atomicString("version");
		if(version.get().isEmpty()) {
			log.info("set version");
			version.set("1");
			tileMapDb.commit();
		} else {
			if(!version.get().equals("1")) {
				throw new RuntimeException("unknown version: "+version.get());
			}
		}

		int cacheTimestamp;
		if(tileMapDb.exists(DB_NAME_CACHE_TIMESTAMP)) {
			databaseCacheTimestamp = tileMapDb.atomicInteger(DB_NAME_CACHE_TIMESTAMP);	
		} else {
			cacheTimestamp = ThreadLocalRandom.current().nextInt();
			databaseCacheTimestamp = tileMapDb.atomicIntegerCreate(DB_NAME_CACHE_TIMESTAMP, cacheTimestamp);
		}
		BTreeMapMaker tileMapMaker = tileMapDb.treeMapCreate(name);
		tileMapMaker.keySerializer(TileKey.SERIALIZER, TileKey.COMPARATOR);
		tileMapMaker.valueSerializer(Tile.SERIALIZER);
		tileMap = tileMapMaker.makeOrGet();

		tileKeys = new ConcurrentSkipListSet<TileKey>(TileKey.COMPARATOR);
		tileKeysReadonly = new ReadonlyNavigableSetView<TileKey>(tileKeys);
		rowKeys = new ConcurrentSkipListSet<RowKey>(RowKey.COMPARATOR);
		bandKeys = new ConcurrentSkipListSet<BandKey>(BandKey.COMPARATOR);
		bandKeysReadonly = new ReadonlyNavigableSetView<BandKey>(bandKeys);
		timeKeys = new ConcurrentSkipListSet<Integer>();
		timeKeysReadonly = new ReadonlyNavigableSetView<Integer>(timeKeys);
		if(!loadCache()) {
			cacheFileOutdated = true;
			refreshKeys();
		}		
	}

	public void commit() {
		if(tilesWritten) {
			databaseCacheTimestamp.incrementAndGet();
			tilesWritten = false;
			cacheFileOutdated = true;
		}
		tileMapDb.commit();
	}

	@Override
	public void close() {
		commit();
		if(cacheFileOutdated) {
			saveCache();
			cacheFileOutdated = false;
		}
		tileMapDb.close();
	}

	private void refreshKeys() {
		tileKeys.clear();
		for(TileKey key:tileMap.keySet()) {
			tileKeys.add(key);
		}
		refreshDerivedKeys();
	}

	private void refreshDerivedKeys() {
		rowKeys.clear();
		bandKeys.clear();
		timeKeys.clear();
		for(TileKey key:tileKeys) {
			rowKeys.add(new RowKey(key.t, key.b, key.y));
		}
		for(RowKey key:rowKeys) {
			bandKeys.add(new BandKey(key.t, key.b));
		}
		for(BandKey key:bandKeys) {
			timeKeys.add(key.t);
		}
	}

	private void addKey(TileKey key) {
		tileKeys.add(key);
		rowKeys.add(new RowKey(key.t, key.b, key.y));
		bandKeys.add(new BandKey(key.t, key.b));
		timeKeys.add(key.t);
	}

	/**
	 * sole method to write tiles
	 * @param tileKey
	 * @param tile
	 */
	public void write(TileKey tileKey, Tile tile) {
		//log.info("write tile " + tileKey);
		tilesWritten = true;
		tileMap.put(tileKey, tile);
		addKey(tileKey);
	}

	public void writeTile(Tile tile) {
		write(new TileKey(tile.t, tile.b, tile.y, tile.x), tile);
	}

	public NavigableSet<TileKey> getTileKeys(int t, int b, int y, int xmin, int xmax) {
		TileKey keyXmin = new TileKey(t, b, y, xmin);
		TileKey keyXmax = new TileKey(t, b, y, xmax);
		return tileKeys.subSet(keyXmin, true, keyXmax, true);
	}

	public Collection<Tile> getTiles(int t, int b, int y, int xmin, int xmax) {
		TileKey keyXmin = new TileKey(t, b, y, xmin);
		TileKey keyXmax = new TileKey(t, b, y, xmax);
		return getTiles(keyXmin, keyXmax);
	}

	public Collection<Tile> getTiles(TileKey keyXmin, TileKey keyXmax) {
		return tileMap.subMap(keyXmin, true, keyXmax, true).values();
	}

	public NavigableSet<RowKey> getRowKeys(int t, int b, int ymin, int ymax) {
		RowKey rowKeyYmin = new RowKey(t, b, ymin);
		RowKey rowKeyYmax = new RowKey(t, b, ymax);
		return rowKeys.subSet(rowKeyYmin, true, rowKeyYmax, true);
	}	

	public TileCollection readTiles(int t, int b, int ymin, int ymax, int xmin, int xmax) {
		Collection<RowKey> rows = getRowKeys(t, b, ymin, ymax);
		return new TileCollection(this, rows, xmin, xmax);
	}
	
	public Range2d getTileRange() {
		int xmin = Integer.MAX_VALUE;
		int ymin = Integer.MAX_VALUE;
		int xmax = Integer.MIN_VALUE;
		int ymax = Integer.MIN_VALUE;
		for(TileKey tileKey:tileKeys) {
			int x = tileKey.x;
			int y = tileKey.y;
			if(x<xmin) {
				xmin = x;
			}
			if(y<ymin) {
				ymin = y;
			}
			if(xmax<x) {
				xmax = x;
			}
			if(ymax<y) {
				ymax = y;
			}			
		}
		Range2d tileRange = new Range2d(xmin, ymin, xmax, ymax);
		return tileRange.isEmptyMarker() ? null : tileRange;
	}

	public Range2d getTileRange(int t) {
		int xmin = Integer.MAX_VALUE;
		int ymin = Integer.MAX_VALUE;
		int xmax = Integer.MIN_VALUE;
		int ymax = Integer.MIN_VALUE;
		for(TileKey tileKey:tileKeys.subSet(BandKey.toBandKeyMin(t).toTileKeyMin(), true, BandKey.toBandKeyMax(t).toTileKeyMax(), true)) {
			int x = tileKey.x;
			int y = tileKey.y;
			if(x<xmin) {
				xmin = x;
			}
			if(y<ymin) {
				ymin = y;
			}
			if(xmax<x) {
				xmax = x;
			}
			if(ymax<y) {
				ymax = y;
			}				
		}
		Range2d tileRange = new Range2d(xmin, ymin, xmax, ymax);
		return tileRange.isEmptyMarker() ? null : tileRange;
	}

	@Override	
	public Range2d getTileRange(BandKey bandKey) {
		int xmin = Integer.MAX_VALUE;
		int ymin = Integer.MAX_VALUE;
		int xmax = Integer.MIN_VALUE;
		int ymax = Integer.MIN_VALUE;
		NavigableSet<TileKey> subset = tileKeys.subSet(bandKey.toTileKeyMin(), true, bandKey.toTileKeyMax(), true);
		if(subset.isEmpty()) {
			return null;
		}
		for(TileKey tileKey:subset) {
			int x = tileKey.x;
			int y = tileKey.y;
			if(x<xmin) {
				xmin = x;
			}
			if(y<ymin) {
				ymin = y;
			}
			if(xmax<x) {
				xmax = x;
			}
			if(ymax<y) {
				ymax = y;
			}				
		}
		Range2d tileRange = new Range2d(xmin, ymin, xmax, ymax);
		return tileRange.isEmptyMarker() ? null : tileRange;
	}
	
	@Override
	public Range2d getTileRangeOfSubset(BandKey bandKey, Range2d subsetTileRange) {
		int xmin = Integer.MAX_VALUE;
		int xmax = Integer.MIN_VALUE;
		NavigableSet<RowKey> subsetRowKeys = getRowKeys(bandKey.t, bandKey.b, subsetTileRange.ymin, subsetTileRange.ymax);
		if(subsetRowKeys.isEmpty()) {
			return null;
		}
		for(RowKey rowKey : subsetRowKeys) {			
			NavigableSet<TileKey> row = tileKeys.subSet(rowKey.toTileKey(subsetTileRange.xmin), true, rowKey.toTileKey(subsetTileRange.xmax), true);
			if(!row.isEmpty()) {
				TileKey minKey = row.first();
				if(minKey.x < xmin) {
					xmin = minKey.x;
				}
				TileKey maxKey = row.last();
				if(xmax < maxKey.x) {
					xmax = maxKey.x;
				}
			}
		}
		return xmin == Integer.MAX_VALUE || xmax == Integer.MIN_VALUE ? null : new Range2d(xmin, subsetRowKeys.first().y, xmax, subsetRowKeys.last().y);
	}


	private static final int cacheHeader = 0xcaac;
	private static final int cacheVersion = 2;
	private static final String DB_NAME_CACHE_TIMESTAMP = "cacheTimestamp";
	private static final int CACHE_KEY_COUNT_MIN = 4096;

	private static ThreadLocal<IntCompressor> threadLocal_ic = new ThreadLocal<IntCompressor>() {
		@Override
		protected IntCompressor initialValue() {
			return new IntCompressor(new SkippableComposition(new FastPFOR(), new VariableByte()));
		}		
	};

	private boolean saveCache() {
		try {
			commit();
			if(tileKeys.size() < CACHE_KEY_COUNT_MIN) {
				return false;
			}
			int cacheTimestamp = databaseCacheTimestamp.get();
			int key_count = tileKeys.size();

			int[] ints = new int[key_count*4];
			int pos = 0;
			for(TileKey tilekey:tileKeys) {
				ints[pos++] = tilekey.t;
			}
			for(TileKey tilekey:tileKeys) {
				ints[pos++] = tilekey.b;
			}
			for(TileKey tilekey:tileKeys) {
				ints[pos++] = tilekey.y;
			}
			for(TileKey tilekey:tileKeys) {
				ints[pos++] = tilekey.x;
			}
			Serialisation.encodeDeltaZigZag(ints);
			int[] cints = threadLocal_ic.get().compress(ints);
			byte[] bytes = Snappy.compress(cints);
			int byte_count = bytes.length;

			int cache_size = 4 + 4 + 4 + 4 + 4 + byte_count;
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(cache_size);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

			byteBuffer.putInt(cacheHeader);
			byteBuffer.putInt(cacheVersion);
			byteBuffer.putInt(cacheTimestamp);
			byteBuffer.putInt(key_count);
			byteBuffer.putInt(byte_count);
			byteBuffer.put(bytes);			

			((Buffer) byteBuffer).flip(); // fix compatibility with older versions than JDK9
			try(FileChannel fileChannel = FileChannel.open(cachePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
				fileChannel.write(byteBuffer);
				return true;
			} catch(Exception e) {
				log.warn(e);
				return false;
			}
		} catch(Exception e) {
			log.warn(e);
			return false;
		}
	}

	private boolean loadCache() {
		try {
			if(!cachePath.toFile().exists()) {
				return false;
			}
			Timer.start("cache");
			//log.info("load cache ...");
			try(FileChannel fileChannel = FileChannel.open(cachePath, StandardOpenOption.READ)) {
				int cache_size = (int) fileChannel.size();
				ByteBuffer byteBuffer = ByteBuffer.allocateDirect(cache_size);
				byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
				fileChannel.read(byteBuffer);
				fileChannel.close();
				((Buffer) byteBuffer).flip(); // fix compatibility with older versions than JDK9
				int fileCacheHeader = byteBuffer.getInt();
				if(fileCacheHeader != cacheHeader) {
					log.warn("cache fileformat error");
					return false;
				}

				int fileCacheVersion = byteBuffer.getInt();
				if(fileCacheVersion != cacheVersion) {
					log.warn("cache version error");
					return false;
				}
				int fileCacheTimestamp = byteBuffer.getInt();
				if(fileCacheTimestamp != databaseCacheTimestamp.get()) {
					log.warn("invalid cache file");
					return false;
				}
				int key_count = byteBuffer.getInt();
				//log.info("key_count "+key_count);
				int byte_count = byteBuffer.getInt();
				byte[] bytes = new byte[byte_count];
				byteBuffer.get(bytes);
				int[] cints = Snappy.uncompressIntArray(bytes);
				int[] ints = threadLocal_ic.get().uncompress(cints);
				Serialisation.decodeDeltaZigZag(ints);
				if(ints.length != 4*key_count) {
					throw new RuntimeException("read errror");
				}
				int post = 0;
				int posb = key_count;
				int posy = 2*key_count;
				int posx = 3*key_count;				
				TileKey[] tileKeyArray = new TileKey[key_count];
				for (int i = 0; i < key_count; i++) {
					int t = ints[post++];
					int b = ints[posb++];
					int y = ints[posy++];
					int x = ints[posx++];
					tileKeyArray[i] = new TileKey(t, b, y, x);
				}
				tileKeys.clear();
				tileKeys.addAll(Arrays.asList(tileKeyArray));
				refreshDerivedKeys();
				if(fileCacheTimestamp != databaseCacheTimestamp.get()) {
					log.warn("invalid cache file");
					tileKeys.clear();
					refreshDerivedKeys();
					return false;
				}
				log.info("cache loaded  "+Timer.stop("cache")+"  "+key_count+" keys"/*+tileKeys.size()*/);			
				return true;
			} catch(Exception e) {
				log.warn(e);
			}
		} catch(Exception e) {
			log.warn(e);
		}
		return false;
	}

	public Tile readTile(int t, int b, int y, int x) {
		return readTile(new TileKey(t, b, y, x));
	}

	public Tile readTile(TileKey tileKey) {
		return tileMap.get(tileKey);
	}

	@Override
	public int getTileCount() {
		return tileKeys.size();
	}

	public boolean isEmpty() {
		return tileMap.isEmpty();
	}

	@Override
	public long removeAllTilesOfTimestamp(int t) {
		try {
			long cnt = 0;
			TileKey min = new TileKey(t, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
			TileKey max = new TileKey(t, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
			log.info("generate keys");
			TreeSet<TileKey> keys = new TreeSet<TileKey>(tileKeys.subSet(min, true, max, true));
			log.info("remove tiles");
			for(TileKey key:keys) {
				if(tileMap.remove(key) != null) {
					cnt++;
				}
			}
			return cnt;
		} finally {
			log.info("refresh keys");
			refreshKeys();
			tilesWritten = true;
			log.info("commit");
			commit();
		}
	}
	
	@Override
	public long removeAllTilesOfBand(int b) {
		try {
			long cnt = 0;
			for(int t:this.timeKeys) {				
				TileKey min = new TileKey(t, b, Integer.MIN_VALUE, Integer.MIN_VALUE);
				TileKey max = new TileKey(t, b, Integer.MAX_VALUE, Integer.MAX_VALUE);
				log.info("generate keys");
				TreeSet<TileKey> keys = new TreeSet<TileKey>(tileKeys.subSet(min, true, max, true));
				log.info("remove tiles");
				for(TileKey key:keys) {
					if(tileMap.remove(key) != null) {
						cnt++;
					}
				}				
			}			
			return cnt;
		} finally {
			log.info("refresh keys");
			refreshKeys();
			tilesWritten = true;
			log.info("commit");
			commit();
		}
	}

	@Override
	public void flush() throws IOException {
		commit();		
	}
	
	public ReadonlyNavigableSetView<TileKey> tileKeysReadonly() {
		return tileKeysReadonly;
	}

	@Override
	public ReadonlyNavigableSetView<BandKey> bandKeysReadonly() {
		return bandKeysReadonly;
	}

	@Override
	public ReadonlyNavigableSetView<Integer> timeKeysReadonly() {
		return timeKeysReadonly;
	}

	@Override
	public long calculateInternalFreeSize() {		
		return Store.forDB(tileMapDb).getFreeSize();		
	}

	@Override
	public long calculateStorageSize() {
		return Store.forDB(tileMapDb).getCurrSize();
	}

	@Override
	public int calculateTileCount() {
		return tileKeys.size();
	}

	@Override
	public long[] calculateTileSizeStats() {
		return null;
	}
}
