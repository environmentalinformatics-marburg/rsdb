package rasterunit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import griddb.Encoding;
import util.Range2d;
import util.Serialisation;
import util.collections.ReadonlyNavigableSetView;

public class TileStorage implements RasterUnitStorage {
	private static final Logger log = LogManager.getLogger();

	private final TileStorageConfig config;
	private final FileChannel tileFileChannel;
	private final ConcurrentSkipListMap<TileKey, TileSlot> map;
	private final AtomicLong fileLimit;
	private final ConcurrentSkipListSet<FreeSlot> freeSet;
	private boolean dirty; // only read/write inside of flushLock
	private final ReentrantReadWriteLock flushLock = new ReentrantReadWriteLock(true);

	public final ReadonlyNavigableSetView<TileKey> tileKeysReadonly;
	public final ReadonlyNavigableSetView<BandKey> bandKeysReadonly;
	public final ReadonlyNavigableSetView<Integer> timeKeysReadonly;
	private NavigableSet<RowKey> rowKeys;
	private NavigableSet<BandKey> bandKeys;
	private NavigableSet<Integer> timeKeys;

	private void addKey(TileKey key) {
		rowKeys.add(new RowKey(key.t, key.b, key.y));
		bandKeys.add(new BandKey(key.t, key.b));
		timeKeys.add(key.t);
	}

	public TileStorage(TileStorageConfig config) throws IOException {	
		this.config = config;
		boolean createIndexFile = false;
		if(config.create) {
			config.path.toFile().mkdirs();
			config.indexPath.toFile().delete();
			config.storagePath.toFile().delete();
			config.dirtyFile.delete();
			createIndexFile = true;
		}
		if(!config.create && !config.storagePath.toFile().exists()) {
			if(config.indexPath.toFile().exists()) {
				throw new RuntimeException("no tile file but existing index file");
			}
			if(config.dirtyFile.exists()) {
				throw new RuntimeException("no tile file but existing dirty file");
			}
			config.path.toFile().mkdirs();
			createIndexFile = true;
		}
		loadDirty();
		checkNotDirty();
		OpenOption[] tileFileOptions = new  OpenOption[] {StandardOpenOption.READ, StandardOpenOption.WRITE};
		if(createIndexFile) {
			tileFileOptions = new  OpenOption[] {StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW};
		}
		log.info("options " + Arrays.toString(tileFileOptions));
		tileFileChannel = FileChannel.open(config.storagePath , tileFileOptions);
		map = new ConcurrentSkipListMap<TileKey, TileSlot>(TileKey.COMPARATOR);
		tileKeysReadonly = new ReadonlyNavigableSetView<TileKey>(map.keySet());
		rowKeys = new ConcurrentSkipListSet<RowKey>(RowKey.COMPARATOR);
		bandKeys = new ConcurrentSkipListSet<BandKey>(BandKey.COMPARATOR);
		bandKeysReadonly = new ReadonlyNavigableSetView<BandKey>(bandKeys);
		timeKeys = new ConcurrentSkipListSet<Integer>();
		timeKeysReadonly = new ReadonlyNavigableSetView<Integer>(timeKeys);
		fileLimit = new AtomicLong(Long.MIN_VALUE);
		freeSet = new ConcurrentSkipListSet<FreeSlot>(FreeSlot.LEN_POS_COMPARATOR);
		open();
	}

	private void loadDirty() {
		dirty = config.dirtyFile.exists();
	}

	private void checkNotDirty() {
		if(dirty) {
			throw new RuntimeException("file not closed correctly last time");
		}
	}

	private void setDirty() throws IOException {
		if(!dirty) {
			if(!config.dirtyFile.createNewFile()) {
				throw new RuntimeException("could not create dirty file");
			}
			dirty = true;
		}
	}

	private void unsetDirty() throws IOException {
		if(dirty) {
			if(!config.dirtyFile.delete()) {
				throw new RuntimeException("could not delete dirty file");
			}
			dirty = false;
		}
	}

	public void writeTile(Tile tile) throws IOException {
		flushLock.readLock().lock();
		try {
			setDirty();
			TileKey key = tile.toTileKey();
			TileSlot prevValue = null;
			while(true) {
				prevValue = map.get(key);
				if(prevValue == null) {
					if(map.putIfAbsent(key, TileSlot.CONCURRENT_UPDATE) == null) {
						break;
					}
				}
				if(prevValue != null) {
					if(map.replace(key, prevValue, TileSlot.CONCURRENT_UPDATE)) {
						break;
					}
				}
			}		
			int len = tile.data.length;
			FreeSlot freeSlotFull = null;
			FreeSlot freeSlotPart = null;
			long pos = Long.MIN_VALUE;
			if(prevValue == null) {
				FreeSlot writeSlot = pollFreeSlot(len);
				if(writeSlot == null) {
					pos = fileLimit.getAndAdd(len);
				} else {
					pos = writeSlot.pos;
					int lenDiff = writeSlot.len - len;
					if(lenDiff > 0) {
						freeSlotPart = new FreeSlot(pos + len, lenDiff);
					}
				}
			} else {
				if(prevValue.len < len) {
					freeSlotFull = new FreeSlot(prevValue.pos, prevValue.len);
					FreeSlot writeSlot = pollFreeSlot(len);
					if(writeSlot == null) {
						pos = fileLimit.getAndAdd(len);
					} else {
						pos = writeSlot.pos;
						int lenDiff = writeSlot.len - len;
						if(lenDiff > 0) {
							freeSlotPart = new FreeSlot(pos + len, lenDiff);
						}
					}
				} else {
					pos = prevValue.pos;
					int lenDiff = prevValue.len - len;
					if(lenDiff > 0) {
						freeSlotPart = new FreeSlot(pos + len, lenDiff);
					}
				}
			}

			int rev = prevValue == null ? ThreadLocalRandom.current().nextInt() : prevValue.rev + 1;
			writeTile(tile.data, pos, len);
			TileSlot value = new TileSlot(pos, len, tile.type, rev);
			TileSlot oldValue = map.put(key, value);
			addKey(key);
			if(oldValue == null || !oldValue.isConcurrentUpdate()) {
				throw new RuntimeException("concurrent tile write error");
			}
			if(freeSlotFull != null) {
				freeSet.add(freeSlotFull);
			}
			if(freeSlotPart != null) {
				freeSet.add(freeSlotPart);
			}
		} finally {
			flushLock.readLock().unlock();
		}
	}

	private FreeSlot pollFreeSlot(int len) {
		flushLock.readLock().lock();
		try {
			FreeSlot fromElement = new FreeSlot(0, len);
			FreeSlot toElement = new FreeSlot(Long.MAX_VALUE, Integer.MAX_VALUE);
			return freeSet.subSet(fromElement, true, toElement, true).pollFirst();
		} finally {
			flushLock.readLock().unlock();
		}
	}

	private void writeTile(byte[] data, long pos, int len) throws IOException {
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(len);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put(data);
		byteBuffer.flip();
		writeTile(data, pos, len, byteBuffer);
	}

	private void writeTile(byte[] data, long pos, int len, ByteBuffer byteBuffer) throws IOException {
		int bufferPos = byteBuffer.position();
		int bufferLimit = byteBuffer.limit();
		if(bufferLimit - bufferPos != len) {
			throw new RuntimeException("bufferLimit - bufferPos != len   " + bufferLimit + "  " + bufferPos + "  " + len);
		}
		int written = 0;
		while(written < len) {
			written += tileFileChannel.write(byteBuffer, pos + written);
		}
		if(written != len) {
			throw new RuntimeException("write error");
		}
	}

	public Tile readTile(int t, int b, int y, int x) throws IOException {
		return readTile(new TileKey(t, b, y, x));
	}

	public Tile readTile(TileKey tileKey) throws IOException {
		flushLock.readLock().lock();
		try {
			TileSlot tileSlotPre = null;
			while(true) {
				while(true) {
					tileSlotPre = map.get(tileKey);
					if(tileSlotPre == null) {
						return null;
					} else {
						if(!tileSlotPre.isConcurrentUpdate()) {
							break;
						}
					}
				}
				byte[] data = readTile(tileSlotPre.pos, tileSlotPre.len);
				TileSlot tileSlotPost = map.get(tileKey);
				if(tileSlotPost != null && !tileSlotPost.isConcurrentUpdate() && tileSlotPre.equals(tileSlotPost)) {
					return new Tile(tileKey.t, tileKey.b, tileKey.y, tileKey.x, tileSlotPre.type, data);
				}
			}
		} finally {
			flushLock.readLock().unlock();
		}
	}

	private byte[] readTile(long pos, int len) throws IOException {
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(len);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		readTile(pos, len, byteBuffer);
		byteBuffer.flip();
		byte[] data = new byte[len];
		byteBuffer.get(data);
		return data;
	}

	private void readTile(long pos, int len, ByteBuffer byteBuffer) throws IOException {
		int bufferPos = byteBuffer.position();
		int bufferLimit = byteBuffer.limit();
		if(bufferLimit - bufferPos != len) {
			throw new RuntimeException("bufferLimit - bufferPos != len   " + bufferLimit + "  " + bufferPos + "  " + len);
		}
		int readLen = 0;
		while(readLen < len) {
			readLen += tileFileChannel.read(byteBuffer, pos + readLen);
		}
		if(readLen != len) {
			throw new RuntimeException("read error");
		}
	}

	public void flush() throws IOException {
		flush(false);
	}

	private void flush(boolean close) throws IOException {
		flushLock.writeLock().lock();
		try {
			if(dirty) {
				tileFileChannel.force(true);
				//writeIndexVersion1(map,config.indexPath);
				writeIndexVersion2(map,config.indexPath);	
				unsetDirty();
			}
			if(close) {
				tileFileChannel.close();
				fileLimit.set(Long.MIN_VALUE);
				freeSet.clear();
				map.clear();
			}
		} finally {
			flushLock.writeLock().unlock();
		}
	}

	public static void writeIndexVersion1(ConcurrentSkipListMap<TileKey, TileSlot> map, Path path) throws IOException {
		int mapLen = map.size();
		final int ENTRY_LEN = 6 * 4 + 8;
		long fileLen = 4 + 4 + 4 + mapLen * ENTRY_LEN;
		if(fileLen > Integer.MAX_VALUE) {
			throw new RuntimeException("index file too large");
		}
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect((int) fileLen);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.putInt(INDEX_FILE_HEADER);
		byteBuffer.putInt(INDEX_FILE_VERSION_1);
		byteBuffer.putInt(mapLen);
		for(Entry<TileKey, TileSlot> e:map.entrySet()) {
			TileKey k = e.getKey();
			TileSlot v = e.getValue();
			byteBuffer.putInt(k.t);
			byteBuffer.putInt(k.b);
			byteBuffer.putInt(k.y);
			byteBuffer.putInt(k.x);
			byteBuffer.putLong(v.pos);
			byteBuffer.putInt(v.len);
			byteBuffer.putInt(v.type);
		}
		byteBuffer.flip();
		try(FileChannel indexFileChannel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			int fileWritten = 0;
			while(fileWritten < fileLen) {
				fileWritten += indexFileChannel.write(byteBuffer);
			}
			if(fileWritten != fileLen) {
				throw new RuntimeException("write error");
			}
		}
	}

	public static void writeIndexVersion2(ConcurrentSkipListMap<TileKey, TileSlot> map, Path path) throws IOException {
		final int MAP_LEN = map.size();
		final int ENTRY_LEN = 6 * 4 + 8;
		final int COMPRESSORS = 8;
		final int MAX_OVERHEAD = COMPRESSORS * 8; // estimation
		long maxFileLen = 4 + 4 + 4 + MAP_LEN * ENTRY_LEN + MAX_OVERHEAD;
		if(maxFileLen > Integer.MAX_VALUE) {
			throw new RuntimeException("index file too large");
		}
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect((int) maxFileLen);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.putInt(TileStorage.INDEX_FILE_HEADER);
		byteBuffer.putInt(TileStorage.INDEX_FILE_VERSION_2);
		byteBuffer.putInt(MAP_LEN);
		Set<Entry<TileKey, TileSlot>> set = map.entrySet();
		{
			int i = 0;
			int[] data = new int[MAP_LEN];
			for(Entry<TileKey, TileSlot> e:set) {
				TileKey k = e.getKey();
				data[i++] = k.t;
			}
			Serialisation.encodeDelta(data); // delta >= 0 (except first if below zero)
			int[] compressed = Encoding.encInt32_pfor_internal(data);
			Serialisation.writeIntsWithSize(compressed, byteBuffer);
		}
		{
			int i = 0;
			int[] data = new int[MAP_LEN];
			for(Entry<TileKey, TileSlot> e:set) {
				TileKey k = e.getKey();
				data[i++] = k.b;
			}
			Serialisation.encodeDeltaZigZag(data);
			int[] compressed = Encoding.encInt32_pfor_internal(data);
			Serialisation.writeIntsWithSize(compressed, byteBuffer);
		}
		{
			int i = 0;
			int[] data = new int[MAP_LEN];
			for(Entry<TileKey, TileSlot> e:set) {
				TileKey k = e.getKey();
				data[i++] = k.y;
			}
			Serialisation.encodeDeltaZigZag(data);
			int[] compressed = Encoding.encInt32_pfor_internal(data);
			Serialisation.writeIntsWithSize(compressed, byteBuffer);
		}
		{
			int i = 0;
			int[] data = new int[MAP_LEN];
			for(Entry<TileKey, TileSlot> e:set) {
				TileKey k = e.getKey();
				data[i++] = k.x;
			}
			Serialisation.encodeDeltaZigZag(data);
			int[] compressed = Encoding.encInt32_pfor_internal(data);
			Serialisation.writeIntsWithSize(compressed, byteBuffer);
		}		
		{
			int i = 0;
			int[] upper = new int[MAP_LEN];
			int[] lower = new int[MAP_LEN];
			long prev = 0;
			for(Entry<TileKey, TileSlot> e:set) {
				TileSlot v = e.getValue();
				long curr = v.pos;
				long value = Serialisation.encodeZigZag(curr - prev);
				upper[i] = (int) (value >> 32);
				lower[i++] = (int) value;
				prev = curr;
			}
			int[] compressed_upper = Encoding.encInt32_pfor_internal(upper);
			Serialisation.writeIntsWithSize(compressed_upper, byteBuffer);
			int[] compressed_lower = Encoding.encInt32_pfor_internal(lower);
			Serialisation.writeIntsWithSize(compressed_lower, byteBuffer);
		}		
		{
			int i = 0;
			int[] data = new int[MAP_LEN];
			for(Entry<TileKey, TileSlot> e:set) {
				TileSlot v = e.getValue();
				data[i++] = v.len;
			}
			Serialisation.encodeDeltaZigZag(data);
			int[] compressed = Encoding.encInt32_pfor_internal(data);
			Serialisation.writeIntsWithSize(compressed, byteBuffer);
		}		
		{
			int i = 0;
			int[] data = new int[MAP_LEN];
			for(Entry<TileKey, TileSlot> e:set) {
				TileSlot v = e.getValue();
				data[i++] = v.type;
			}
			int[] compressed = Encoding.encInt32_pfor_internal(data); // no delta
			Serialisation.writeIntsWithSize(compressed, byteBuffer);
		}

		byteBuffer.flip();
		int dataSize = byteBuffer.limit();
		log.info("dataSize " + dataSize);

		try(FileChannel indexFileChannel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			int fileWritten = 0;
			while(fileWritten < dataSize) {
				fileWritten += indexFileChannel.write(byteBuffer);
			}
			if(fileWritten != dataSize) {
				throw new RuntimeException("write error");
			}
		}
	}

	public static final int INDEX_FILE_HEADER = 0xe6699667;
	static final int INDEX_FILE_VERSION_1 = 0x01_00_00_00;
	public static final int INDEX_FILE_VERSION_2 = 0x02_00_00_00;

	public static TreeSet<TileSlot> readIndex(Path path, ConcurrentSkipListMap<TileKey, TileSlot> map) throws IOException {
		try(FileChannel indexFileChannel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.CREATE)) {
			long fileLen = indexFileChannel.size();
			log.info("fileLen " + fileLen);
			if(fileLen > Integer.MAX_VALUE) {
				throw new RuntimeException("index file too large");
			}
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect((int) fileLen);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);					
			int fileRead = 0;
			while(fileRead < fileLen) {
				fileRead += indexFileChannel.read(byteBuffer);
			}
			if(fileRead != fileLen) {
				throw new RuntimeException("read error");
			}
			byteBuffer.flip();
			int fileHeader = byteBuffer.getInt();
			if(fileHeader == INDEX_FILE_HEADER) {
				int fileVersion = byteBuffer.getInt();
				switch(fileVersion) {
				case INDEX_FILE_VERSION_1:
					return readIndexVersion1(byteBuffer, map);
				case INDEX_FILE_VERSION_2:
					return readIndexVersion2(byteBuffer, map);					
				default:
					throw new RuntimeException("unknown index version");
				}

			} else {
				byteBuffer.rewind(); // legacy version
				return readIndexVersion1(byteBuffer, map);
			}
		}
	}

	public static TreeSet<TileSlot> readIndexVersion1(ByteBuffer byteBuffer, ConcurrentSkipListMap<TileKey, TileSlot> map) {
		int mapLen = byteBuffer.getInt();
		log.info("mapLen " + mapLen);
		ThreadLocalRandom random = ThreadLocalRandom.current();
		TreeSet<TileSlot> slotSet = new TreeSet<TileSlot>(TileSlot.POS_LEN_REV_COMPARATOR);
		for (int i = 0; i < mapLen; i++) {
			int t = byteBuffer.getInt();
			int b = byteBuffer.getInt();
			int y = byteBuffer.getInt();
			int x = byteBuffer.getInt();
			long pos = byteBuffer.getLong();
			int len = byteBuffer.getInt();
			int type = byteBuffer.getInt(); 
			TileSlot tileSlot = new TileSlot(pos, len, type, random.nextInt());
			map.put(new TileKey(t, b, y, x), tileSlot);
			slotSet.add(tileSlot);
		}
		return slotSet;		
	}

	public static TreeSet<TileSlot> readIndexVersion2(ByteBuffer byteBuffer, ConcurrentSkipListMap<TileKey, TileSlot> map) {
		int mapLen = byteBuffer.getInt();
		log.info("mapLen " + mapLen);

		int[] ts = Encoding.decInt32_pfor_internal(Serialisation.readIntsWithSize(byteBuffer));
		Serialisation.decodeDelta(ts);  // delta, no zigzag

		int[] bs = Encoding.decInt32_pfor_internal(Serialisation.readIntsWithSize(byteBuffer));
		Serialisation.decodeDeltaZigZag(bs);

		int[] ys = Encoding.decInt32_pfor_internal(Serialisation.readIntsWithSize(byteBuffer));
		Serialisation.decodeDeltaZigZag(ys);

		int[] xs = Encoding.decInt32_pfor_internal(Serialisation.readIntsWithSize(byteBuffer));
		Serialisation.decodeDeltaZigZag(xs);

		int[] pos_upper = Encoding.decInt32_pfor_internal(Serialisation.readIntsWithSize(byteBuffer));
		int[] pos_lower = Encoding.decInt32_pfor_internal(Serialisation.readIntsWithSize(byteBuffer));
		long[] poss = new long[mapLen];
		for (int i = 0; i < mapLen; i++) {
			poss[i] = (long)pos_lower[i] | ((long)pos_upper[i]<<32);
		}
		Serialisation.decodeDeltaZigZag(poss);

		int[] lens = Encoding.decInt32_pfor_internal(Serialisation.readIntsWithSize(byteBuffer));
		Serialisation.decodeDeltaZigZag(lens);

		int[] types = Encoding.decInt32_pfor_internal(Serialisation.readIntsWithSize(byteBuffer)); // no delta

		ThreadLocalRandom random = ThreadLocalRandom.current();
		TreeSet<TileSlot> slotSet = new TreeSet<TileSlot>(TileSlot.POS_LEN_REV_COMPARATOR);
		for (int i = 0; i < mapLen; i++) {
			TileSlot tileSlot = new TileSlot(poss[i], lens[i], types[i], random.nextInt());
			map.put(new TileKey(ts[i], bs[i], ys[i], xs[i]), tileSlot);
			slotSet.add(tileSlot);
		}
		return slotSet;
	}

	public static long refreshFreeSet(TreeSet<TileSlot> slotSet, ConcurrentSkipListSet<FreeSlot> freeSet) {
		long pos = 0;
		for(TileSlot tileSlot:slotSet) {
			long slotPos = tileSlot.pos;
			if(slotPos < pos) {
				throw new RuntimeException("internal error");
			}
			long lenDiff = slotPos - pos;
			while(lenDiff > 0) {
				int freeSlotLen = lenDiff > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) lenDiff;
				FreeSlot freeSlot = new FreeSlot(pos, freeSlotLen);
				freeSet.add(freeSlot);
				pos += freeSlotLen;
				lenDiff = slotPos - pos;
			}
			pos = tileSlot.pos + tileSlot.len;
		}
		return pos;
	}

	private void open() throws IOException {
		flushLock.writeLock().lock();
		try {
			map.clear();
			fileLimit.set(Long.MIN_VALUE);
			freeSet.clear();
			if(config.indexPath.toFile().exists()) {
				TreeSet<TileSlot> slotSet = readIndex(config.indexPath, map);
				refreshDerivedKeys();
				long pos = refreshFreeSet(slotSet, freeSet);
				fileLimit.set(pos);
				long tileFileLen = tileFileChannel.size();
				if(tileFileLen < pos) {
					throw new RuntimeException("tile file error");
				}
				if(pos < tileFileLen) {
					log.info("truncate " + tileFileLen + " to " + pos);
					tileFileChannel.truncate(pos);
				}
			} else {
				fileLimit.set(0);
			}
		} finally {
			flushLock.writeLock().unlock();
		}
	}

	@Override
	public void close() throws IOException {
		flush(true);
	}

	@Override
	public void commit() {
		// nothing		
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Range2d getTileRange() {
		int xmin = Integer.MAX_VALUE;
		int ymin = Integer.MAX_VALUE;
		int xmax = Integer.MIN_VALUE;
		int ymax = Integer.MIN_VALUE;
		for(TileKey tileKey:map.keySet()) {
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
	public Collection<Tile> readTiles(int t, int b, int ymin, int ymax, int xmin, int xmax) {
		Collection<RowKey> rows = getRowKeys(t, b, ymin, ymax);
		return new TileCollection(this, rows, xmin, xmax);
	}

	public NavigableSet<TileKey> getTileKeys(int t, int b, int y, int xmin, int xmax) {
		TileKey keyXmin = new TileKey(t, b, y, xmin);
		TileKey keyXmax = new TileKey(t, b, y, xmax);
		return map.keySet().subSet(keyXmin, true, keyXmax, true);
	}

	@Override
	public Collection<Tile> getTiles(TileKey keyXmin, TileKey keyXmax) {
		NavigableSet<TileKey> keys = map.keySet().subSet(keyXmin, true, keyXmax, true);
		return new TileCollectionInternal(keys);
	}

	private class TileCollectionInternal extends AbstractCollection<Tile> {
		private final NavigableSet<TileKey> keys;

		public TileCollectionInternal(NavigableSet<TileKey> keys) {
			this.keys = keys;
		}

		@Override
		public Iterator<Tile> iterator() {
			return new TileIterator(keys);
		}

		@Override
		public int size() {
			return keys.size();
		}
	}

	private class TileIterator implements Iterator<Tile> {		
		private final Iterator<TileKey> it;

		public TileIterator(NavigableSet<TileKey> keys) {
			it = keys.iterator();
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public Tile next() {
			TileKey tileKey = it.next();
			try {
				return readTile(tileKey);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

	public NavigableSet<RowKey> getRowKeys(int t, int b, int ymin, int ymax) {
		RowKey rowKeyYmin = new RowKey(t, b, ymin);
		RowKey rowKeyYmax = new RowKey(t, b, ymax);
		return rowKeys.subSet(rowKeyYmin, true, rowKeyYmax, true);
	}

	private void refreshDerivedKeys() {
		rowKeys.clear();
		bandKeys.clear();
		timeKeys.clear();
		for(TileKey key:map.keySet()) {
			rowKeys.add(new RowKey(key.t, key.b, key.y));
		}
		for(RowKey key:rowKeys) {
			bandKeys.add(new BandKey(key.t, key.b));
		}
		for(BandKey key:bandKeys) {
			timeKeys.add(key.t);
		}
	}

	@Override
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
	public Range2d getTileRange(BandKey bandKey) {
		int xmin = Integer.MAX_VALUE;
		int ymin = Integer.MAX_VALUE;
		int xmax = Integer.MIN_VALUE;
		int ymax = Integer.MIN_VALUE;
		NavigableSet<TileKey> subset = map.keySet().subSet(bandKey.toTileKeyMin(), true, bandKey.toTileKeyMax(), true);
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
	public int getTileCount() {
		return map.size();
	}

	@Override
	public long removeAllTilesOfTimestamp(int t) throws IOException {
		flushLock.writeLock().lock();
		try {
			TileKey min = new TileKey(t, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
			TileKey max = new TileKey(t, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
			ConcurrentNavigableMap<TileKey, TileSlot> tmap = map.subMap(min, true, max, true);
			long cnt = tmap.size();
			if(cnt > 0) {
				tmap.clear(); // remove all tile entries of timestamp
				flush(); // write removed entries to file
				open(); // regenerate free slot list
			}
			return cnt;
		} finally {
			refreshDerivedKeys();
			flushLock.writeLock().unlock();
		}	
	}

	@Override
	public long removeAllTilesOfBand(int b) throws IOException {
		flushLock.writeLock().lock();
		try {
			long cnt = 0;
			for(int t:this.timeKeys) {	
				TileKey min = new TileKey(t, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
				TileKey max = new TileKey(t, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
				ConcurrentNavigableMap<TileKey, TileSlot> tmap = map.subMap(min, true, max, true);
				int subSize = tmap.size();				
				if(subSize > 0) {
					tmap.clear(); // remove all tile entries of timestamp
					cnt += subSize;
				}
			}
			if(cnt > 0) {
				flush(); // write removed entries to file
				open(); // regenerate free slot list
			}
			return cnt;
		} finally {
			refreshDerivedKeys();
			flushLock.writeLock().unlock();
		}	
	}	
}
