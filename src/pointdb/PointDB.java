package pointdb;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.DBMaker.Maker;

import broker.Informal;
import pointdb.base.Point;
import pointdb.base.PointdbConfig;
import pointdb.base.Rect;
import pointdb.base.Tile;
import pointdb.base.TileKey;
import pointdb.base.TileMeta;
import pointdb.processing.tile.TileProducer;
import pointdb.processing.tilekey.TileKeyProducer;
import pointdb.processing.tilemeta.TileMetaProducer;
import pointdb.processing.tilepoint.TilePointProducer;
import util.Util;

/**
 * PointDB contains one planar point cloud of lidar data
 * @author woellauer
 *
 */
public class PointDB {
	private static final Logger log = LogManager.getLogger();	

	private final DB db;
	private final DB dbMeta;
	public final BTreeMap<TileKey, Tile> tileMap;
	public final BTreeMap<TileKey, TileMeta> tileMetaMap;	
	public final PointdbConfig config;

	/**
	 * opens one existing PointDB
	 * @param config
	 */
	public PointDB(PointdbConfig config) {
		this(config, false);
	}

	/**
	 * opens one PointDB
	 * @param config
	 * @param createIfmissing create DB if missing
	 */
	public PointDB(PointdbConfig config, boolean createIfmissing) {
		if(!createIfmissing) {
			checkExistDB(config);
		}
		this.config = config;
		File file = new File(config.getTileDbFullPath());
		File fileMeta = new File(config.getTileMetaDbFullPath());
		Util.checkFileNotLocked(file);
		Util.checkFileNotLocked(fileMeta);
		log.info("init PointDB...");		
		Util.createPathOfFile(file);
		Util.createPathOfFile(fileMeta);
		log.info("init PointDB open tile file ...");
		db = createTileDB(file,config.isTransactions());
		log.info("init PointDB open tile map ...");
		tileMap = createTileMap(db, config.getStorageType());
		log.info("init PointDB open meta file ...");
		dbMeta = createTileMetaDB(fileMeta, config.isTransactions());
		log.info("init PointDB open meta map ...");
		tileMetaMap = createTileMetaMap(dbMeta);

		log.info("init PointDB commit ...");
		commit(); // workaround for empty db close reopen error
		log.info("init PointDB done");
	}

	public static void checkExistDB(PointdbConfig config) {
		File dbDir = config.getDbPath().toFile();
		if(!dbDir.exists()) {
			throw new PdbException("path to db does not exist "+dbDir);
		}
		if(!dbDir.isDirectory()) {
			throw new PdbException("path to db is no directory "+dbDir);
		}
		File tileFile = new File(config.getTileDbFullPath());
		if(!tileFile.exists()) {
			throw new PdbException("db tile file does not exist "+tileFile);
		}
		File metaFile = new File(config.getTileMetaDbFullPath());
		if(!metaFile.exists()) {
			throw new PdbException("db meta file does not exist "+metaFile);
		}
	}

	public void commit() {
		dbMeta.commit();
		db.commit();		
	}

	public void compact() {
		log.info("DB commit...");
		dbMeta.commit();
		db.commit();
		log.info("DB compact...");
		dbMeta.compact();
		db.compact();

	}

	public void close() {
		if(!db.isClosed()) {
			log.info("DB commit...");
			dbMeta.commit();
			db.commit();
			log.info("DB close...");
			dbMeta.close();
			db.close();
		}
	}

	public void insertPoints(TileKey tileKey,Point[] points) {
		Tile oldTile = tileMap.get(tileKey);
		if(oldTile==null) {
			TileMeta meta = TileMeta.of(tileKey,points);
			Tile tile = new Tile(meta,points);
			tileMap.put(tileKey, tile);
			tileMetaMap.put(tileKey, meta);
		} else {
			Point[] merged_points = Util.concatenate(oldTile.points, points);
			TileMeta merged_meta = TileMeta.of(tileKey,merged_points);
			Tile merged_tile = new Tile(merged_meta,merged_points);
			tileMap.put(tileKey, merged_tile);
			tileMetaMap.put(tileKey, merged_meta);
		}
	}	

	public TileIterator getTiles(int utm_min_x, int utm_min_y, int utm_max_x, int utm_max_y) {
		return new TileIterator(this, utm_min_x, utm_min_y, utm_max_x, utm_max_y);
	}

	private static DB createTileMetaDB(File fileMeta, boolean transactions) {
		Maker dbMetaMaker = DBMaker.fileDB(fileMeta);
		//.closeOnJvmShutdown() // use own handler
		if(!transactions) {
			dbMetaMaker.transactionDisable();
		}
		dbMetaMaker.compressionEnable();
		return dbMetaMaker.make();		
	}

	private static BTreeMap<TileKey, TileMeta> createTileMetaMap(DB dbMeta) {
		return dbMeta.treeMapCreate("tileMetaMap")
				.keySerializer(TileKey.SERIALIZER, TileKey.COMPARATOR)
				.valueSerializer(TileMeta.SERIALIZER)
				//.nodeSize(1024) // !!
				.makeOrGet();		
	}



	private static DB createTileDB(File file, boolean transactions) {
		Maker dbmaker = DBMaker.fileDB(file);
		dbmaker.closeOnJvmShutdown();
		//.fileLockDisable() // safe for read only ??
		//.compressionEnable() //slow
		//.asyncWriteEnable() //slow
		//dbmaker.fileMmapEnable(); //crashes (with HTreeMap ??) // buggy?
		dbmaker.fileChannelEnable(); //maybe problem with Thread.interrupt situations
		dbmaker.storeExecutorEnable();
		dbmaker.executorEnable();
		dbmaker.asyncWriteEnable().asyncWriteFlushDelay(1000).asyncWriteQueueSize(10000);
		//.cacheWeakRefEnable()		        
		//.cacheSoftRefEnable()
		//.cacheSize(1000000)
		//.cacheHardRefEnable()
		//.cacheSize(1000000000)
		//.cacheLRUEnable()
		//.cacheSize(1024*1024)
		dbmaker.cacheExecutorEnable();
		dbmaker.allocateIncrement(134217728).allocateStartSize(134217728);
		if(!transactions) {
			dbmaker.transactionDisable();
		}
		return dbmaker.make();		
	}

	@SuppressWarnings("static-access")
	private static BTreeMap<TileKey, Tile> createTileMap(DB db, String storageType) {
		if(storageType.isEmpty()) { // current version
			return db.treeMapCreate("tileMap")
					.keySerializer(TileKey.SERIALIZER, TileKey.COMPARATOR)
					.valueSerializer(Tile.SERIALIZER)
					.valuesOutsideNodesEnable() // !!
					//.nodeSize(1024) // !!
					.makeOrGet();		
		} else if(Tile.SERIALIZER_OLD_VERSION.version.equals(storageType)){ // old Version
			return db.treeMapCreate("tileMap")
					.keySerializer(TileKey.SERIALIZER, TileKey.COMPARATOR)
					.valueSerializer(Tile.SERIALIZER_OLD_VERSION)
					.valuesOutsideNodesEnable() // !!
					//.nodeSize(1024) // !!
					.makeOrGet();			
		} else {
			throw new RuntimeException("unknown storage type in pointdb: " + storageType);
		}
	}

	public TileKeyProducer tileKeyProducer(Rect rect) {
		if(rect==null) {
			return DBFullTileKeyProducer.of(this);
		}
		return DBTileKeyProducer.of(this, rect);
	}

	/**
	 * Producer for TileMeta
	 * @param rect null => all data
	 * @return
	 */
	public TileMetaProducer tileMetaProducer(Rect rect) {
		if(rect==null) {
			return DBFullTileMetaProducer.of(this);
		}
		return DBTileMetaProducer.of(this, rect);
	}

	/**
	 * Producer of tile points
	 * @param rect
	 * @return rect null => all data
	 */
	public TilePointProducer tilePointProducer(Rect rect) {
		TileProducer tileProducer = tileProducer(rect);
		if(rect==null) {
			return tileProducer.toFullTilePointProducer();
		}
		return tileProducer.toTilePointProducer(rect);
	}

	public TileProducer tileProducer(Rect rect) {
		if(rect==null) {
			return DBFullTileProducer.of(this);
		}
		//return DBAsyncTileProducer.of(this, rect); // bugs in async ???
		return DBTileProducer.of(this, rect);
	}

	public Informal informal() {
		return config.informal();
	}
}
