package util.indexedstorage;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.DataIO.DataInputByteArray;
import org.mapdb.DataIO.DataOutputByteArray;
import org.mapdb.Fun.Pair;
import org.mapdb.Serializer;

import util.Util;
import util.collections.vec.Vec;
import util.indexedstorage.MultiCache.TypedCache;

public class IndexedStorage<K, V> implements Closeable {
	private static final Logger log = LogManager.getLogger();

	private static final int CURRENT_VERSION = 1;
	private File fileIndex;
	public final ConcurrentSkipListMap<K, IndexEntry> indexMap;

	public final String name;
	private File fileData;
	private final RandomAccessFile raf_data;
	private final AtomicLong fileAppendPos;
	private int version;
	private final BulkSerializer<K> keySerializer;
	private final Serializer<V> dataSerializer;

	private TypedCache<K, V> cache;

	public IndexedStorage(String name, String filenamePrefix, BulkSerializer<K> keySerializer, Comparator<K> keyComparator, Serializer<V> dataSerializer, boolean clear) throws IOException {
		this.name = name;
		Util.createPathOfFile(filenamePrefix);		
		this.fileIndex = getFileIndex(filenamePrefix);
		this.fileData = getFileData(filenamePrefix);
		if(clear) {
			fileIndex.delete();
			fileData.delete();
		}		
		this.indexMap = new ConcurrentSkipListMap<K, IndexEntry>(keyComparator);
		this.raf_data = new RandomAccessFile(fileData,"rw");
		this.fileAppendPos = new AtomicLong();
		this.version = CURRENT_VERSION;
		this.keySerializer = keySerializer;
		this.dataSerializer = dataSerializer;
		this.cache = new MultiCache.TypedCache<K,V>(name);
		readIndex();
	}

	public static File getFileIndex(String filenamePrefix) {
		return new File(filenamePrefix+".idx");
	}

	public static File getFileData(String filenamePrefix) {
		return new File(filenamePrefix+".dat");
	}

	public static boolean checkExist(String filenamePrefix) {
		return getFileIndex(filenamePrefix).exists() &&  getFileData(filenamePrefix).exists();
	}

	private synchronized void readIndex() throws IOException {
		if(fileIndex.exists()) {
			try(RandomAccessFile raf_index = new RandomAccessFile(fileIndex,"r")) {
				version = raf_index.readInt();
				fileAppendPos.set(raf_index.readLong());
				K[] keys = keySerializer.deserializeBulk(raf_index);
				IndexEntry[] ies = IndexEntry.deserialize(raf_index);
				final int SIZE = keys.length;
				if(ies.length!=SIZE) {
					throw new RuntimeException("read error "+SIZE+" "+ies.length);
				}
				indexMap.clear();
				for (int i = 0; i < SIZE; i++) {
					indexMap.put(keys[i], ies[i]);
				}
			}
		}
	}

	public synchronized  void commit() throws IOException {
		try {
			raf_data.getFD().sync();
		} catch(Exception e) {
			log.warn("no sync for raf_data "+e);
		}
		try(RandomAccessFile raf_index = new RandomAccessFile(fileIndex,"rw")) {
			raf_index.writeInt(version);
			raf_index.writeLong(fileAppendPos.get());
			keySerializer.serializeBulk(raf_index, indexMap.keySet());
			IndexEntry.serialize(raf_index, indexMap.values());
			try {
				raf_index.getFD().sync();
			} catch(Exception e) {
				log.warn("no sync for raf_index "+e);
			}
		}
	}

	@Override
	public synchronized  void close() throws IOException {
		commit();
		raf_data.close();
	}

	/**
	 * 
	 * @param tile
	 * @param out  temp buffer (sets pos to zero before write)
	 * @throws IOException
	 */
	public synchronized void put(K key, V value, DataOutputByteArray out) throws IOException {
		out.pos = 0;
		dataSerializer.serialize(out, value);
		putRaw(key, out.buf, out.pos);
	}

	public synchronized void putRaw(K key, byte[] value, int len) throws IOException {
		long prefPos = fileAppendPos.getAndAdd(len);

		synchronized (raf_data) {
			raf_data.seek(prefPos);
			raf_data.write(value, 0, len);				
		}

		indexMap.put(key, new IndexEntry(prefPos,len));
	}

	public void getRawBox(long pos, int len, Box box) throws IOException {	
		if(box.buf==null || box.buf.length<len) {
			box.buf = new byte[len];
		}
		synchronized (raf_data) {
			raf_data.seek(pos);
			raf_data.readFully(box.buf, 0, len);			
		}
		box.len = len;
	}

	public void getRaw(K key, Box box) throws IOException {			
		box.len = 0;
		IndexEntry ie = indexMap.get(key);
		if(ie!=null) {
			getRawBox(ie.pos, ie.len, box);
		}		
	}

	public V get(K key, Box box) throws IOException {
		V cachedTile = cache.get(key);
		if(cachedTile!=null) {
			//log.info("cached! "+key);
			return cachedTile;
		}
		//log.info("NOT cached! "+key);

		getRaw(key, box);
		try (DataInputByteArray in = new DataInputByteArray(box.buf)) {
			V tile = dataSerializer.deserialize(in, -1);
			if(in.getPos() != box.len) {
				throw new RuntimeException("read error pos:"+in.getPos()+" should be  "+box.len);
			}
			cache.put(key, tile);
			return tile;
		}
	}

	public void foreach(Collection<K> keys, Box box, Consumer<V> consumer) throws IOException {
		Iterator<K> it = keys.iterator();
		Vec<Pair<K,IndexEntry>> bufferedEntries = new Vec<Pair<K,IndexEntry>>();
		while(it.hasNext()) {
			K key = it.next();
			V cachedTile = cache.get(key);
			if(cachedTile!=null) {
				consumer.accept(cachedTile);
			} else {
				updateBuffer(bufferedEntries, key, box, consumer);
			}
		}
		readBuffer(bufferedEntries, box, consumer);
	}

	private static final long maxReadSize = 16*1024*1024;

	private void updateBuffer(Vec<Pair<K,IndexEntry>> bufferedEntries, K key, Box box, Consumer<V> consumer) throws IOException {
		IndexEntry ie = indexMap.get(key);
		if(ie!=null) {
			Pair<K, IndexEntry> prefIe = bufferedEntries.last();
			if(prefIe==null) { // first entry
				bufferedEntries.add(new Pair<K, IndexEntry>(key,ie));
			} else if(prefIe.b.getBorder()!=ie.pos) { // entry not next in index
				readBuffer(bufferedEntries, box, consumer);
				bufferedEntries.add(new Pair<K, IndexEntry>(key,ie));
			} else if( bufferedEntries.last().b.pos - bufferedEntries.first().b.pos + bufferedEntries.last().b.len > maxReadSize ) { // entry next in index, but size in buffer too large
				readBuffer(bufferedEntries, box, consumer);
				bufferedEntries.add(new Pair<K, IndexEntry>(key,ie));
			} else { // entry next in index
				bufferedEntries.add(new Pair<K, IndexEntry>(key,ie));
			}
		}
	}

	private void readBuffer(Vec<Pair<K,IndexEntry>> bufferedEntries, Box box, Consumer<V> consumer) throws IOException {
		if(bufferedEntries.isEmpty()) {
			return;
		}
		long pos = bufferedEntries.first().b.pos;
		int len = (int) (bufferedEntries.last().b.pos-pos+bufferedEntries.last().b.len);
		//log.info("batch read pos:"+pos+"  len:"+len+"  cnt:"+bufferedEntries.size());
		getRawBox(pos, len, box);
		
		/*try (DataInputByteArray in = new DataInputByteArray(box.buf)) {
			for(Pair<K, IndexEntry> ie:bufferedEntries) {
				int elementPos = (int) (ie.b.pos-pos);
				in.setPos(elementPos);
				V tile = dataSerializer.deserialize(in, -1);
				if(in.getPos() != elementPos+ie.b.len) {
					throw new RuntimeException("read error pos:"+in.getPos()+" should be "+(elementPos+ie.b.len)+"    start pos "+elementPos);
				}
				cache.put(ie.a, tile);
				consumer.accept(tile);
			}
		}*/

		Phaser phaser = new Phaser();
		phaser.register();
		ForkJoinPool exe = ForkJoinPool.commonPool();
		/*log.info("PoolSize "+exe.getPoolSize());
		log.info("ActiveThreadCount "+exe.getActiveThreadCount());
		log.info("RunningThreadCount "+exe.getRunningThreadCount());
		log.info("Parallelism "+exe.getParallelism());
		log.info("QueuedSubmissionCount "+exe.getQueuedSubmissionCount());
		log.info("StealCount "+exe.getStealCount());
		log.info("QueuedTaskCount "+exe.getQueuedTaskCount());
		log.info("IndexedStorage start load "+bufferedEntries.size());*/
		for(Pair<K, IndexEntry> ie:bufferedEntries) {
			phaser.register();
			exe.execute(()->{
				//log.info("IndexedStorage start exe");
				try (DataInputByteArray in = new DataInputByteArray(box.buf)) {
					int elementPos = (int) (ie.b.pos-pos);
					in.setPos(elementPos);
					V tile = dataSerializer.deserialize(in, -1);
					if(in.getPos() != elementPos+ie.b.len) {
						throw new RuntimeException("read error pos:"+in.getPos()+" should be "+(elementPos+ie.b.len)+"    start pos "+elementPos);
					}
					cache.put(ie.a, tile);
					consumer.accept(tile);
				} catch(Exception e) {
					Util.rethrow(e);
				} finally {
					//log.info("work done");
					phaser.arrive();
				}
			});
		}
		
		phaser.arriveAndAwaitAdvance();
		//log.info("IndexedStorage phaser done");

		bufferedEntries.clear();
	}
}
