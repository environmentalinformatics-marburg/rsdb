package griddb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import rasterunit.BandKey;
import rasterunit.RasterUnit;
import rasterunit.RasterUnitStorage;
import rasterunit.Tile;
import rasterunit.TileCollection;
import rasterunit.TileKey;
import rasterunit.TileStorage;
import rasterunit.TileStorageConfig;
import util.Range2d;
import util.collections.ReadonlyNavigableSetView;
import util.collections.vec.ReadonlyVecView;
import util.collections.vec.Vec;
import util.yaml.YamlMap;

public class GridDB implements AutoCloseable {
	private static final Logger log = LogManager.getLogger();

	private static final int TILE_TYPE_CELL = 0;

	private final Vec<Attribute> attributes = new Vec<>();
	private String storageType = null;

	private RasterUnitStorage storage = null;

	private final String name;
	private final Path path;
	private final Path metaPath;
	public final File metaFile;
	private final Path metaPathTemp;
	private final File metaFileTemp;

	private ExtendedMeta extendedMeta = null;

	private final String fileDataName;
	private final String fileDataCache;
	private final boolean transaction;
	
	private volatile boolean unsavedMetaChanges = false;

	public interface ExtendedMeta {
		public void read(YamlMap map);
		public void write(LinkedHashMap<String, Object> map);
	}

	public GridDB(Path path, String name, ExtendedMeta extendedMeta, String preferredStorageType, boolean transaction) {		
		this.path = path;
		this.name = name;
		this.storageType = preferredStorageType; // will be overwritten if yaml file exists
		//log.info("preferredStorageType (" + path + "   "+ name + ") " + preferredStorageType);
		this.transaction =  transaction;
		path.toFile().mkdirs();
		this.extendedMeta = extendedMeta;
		this.fileDataName = name + ".dat";
		this.fileDataCache = name + ".idx"; 
		String fileMetaName = name + ".yml";
		metaPath = path.resolve(fileMetaName);
		metaFile = metaPath.toFile();
		metaPathTemp = Paths.get(metaPath.toString()+"_temp");
		metaFileTemp = metaPathTemp.toFile();
	}

	public RasterUnitStorage storage() {
		RasterUnitStorage s = storage;
		return s == null ? loadStorage() : s;
	}

	private synchronized RasterUnitStorage loadStorage() {
		if (storage == null) {
			switch(storageType) {
			case "RasterUnit":
				storage = new RasterUnit(path, fileDataName, fileDataCache, !transaction);
				break;
			case "TileStorage":
				TileStorageConfig config = new TileStorageConfig(path, name);
				try {
					storage = new TileStorage(config);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				break;
			default:
				throw new RuntimeException("unknown storage_type");
			}

		}
		return storage;
	}

	public void setExtendedMeta(ExtendedMeta extendedMeta) {
		this.extendedMeta = extendedMeta;
	}

	public static Cell tileToCell(Tile tile) {
		try {
			return new Cell(tile.x, tile.y, tile.b, tile.data);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Tile createTile(int cx, int cy, int cz, byte[] cellData) {		
		return new Tile(0, cz, cy, cx, TILE_TYPE_CELL, cellData);		
	}
	
	public Tile createTile(int cx, int cy, int cz, int t, byte[] cellData) {		
		return new Tile(t, cz, cy, cx, TILE_TYPE_CELL, cellData);		
	}

	public void writeTile(Tile tile) throws IOException {
		storage().writeTile(tile);
	}

	public synchronized void commit() {
		if (storage != null) {
			storage.commit();
		}
	}

	@Override
	public synchronized void close() throws IOException {
		if (storage != null) {
			storage.close();
			storage = null;
		}
		writeMetaIfNeeded();
	}

	private Tile getTile(int tx, int ty, int tz) throws IOException {
		return storage().readTile(0, tz, ty, tx);		
	}
	
	private Tile getTile(int tx, int ty, int tz, int t) throws IOException {
		return storage().readTile(t, tz, ty, tx);		
	}

	public Cell getCell(int x, int y, int z) throws IOException {
		Tile tile = getTile(x, y, z);
		return tile == null ? null : tileToCell(tile);
	}
	
	public Cell getCell(int x, int y, int z, int t) throws IOException {
		Tile tile = getTile(x, y, z, t);
		return tile == null ? null : tileToCell(tile);
	}

	public TileCollection getTiles(int xcellmin, int ycellmin, int xcellmax, int ycellmax) {
		return storage().readTiles(0, 0, ycellmin, ycellmax, xcellmin, xcellmax);
	}

	public Stream<Cell> getCells(int xcellmin, int ycellmin, int xcellmax, int ycellmax) {
		Collection<Tile> tiles = getTiles(xcellmin, ycellmin, xcellmax, ycellmax);
		/*Spliterator<Tile> spliterator = Spliterators.spliterator(tiles, 0);  // spliterator.trySplit() may allocate very big arrays TODO check
		Stream<Cell> stream = StreamSupport.stream(spliterator, true).map(tile -> tileToCell(tile));*/
		/*Spliterator<Tile> spliterator = new TileSpliterator<Tile>(tiles.iterator(), tiles.size(), 0);
		Stream<Cell> stream = StreamSupport.stream(spliterator, true).map(tile -> tileToCell(tile));*/
		Stream<Cell> stream = tiles.parallelStream().map(tile -> tileToCell(tile));
		//Stream<Cell> stream = tiles.stream().map(tile -> tileToCell(tile));
		return stream;
	}

	public Attribute getAttribute(String name) {
		for(Attribute attr:attributes) {
			if(attr.name.equals(name)) {
				return attr;
			}
		}
		return null;
	}

	public synchronized void addAttribute(String name, int encoding) {
		Attribute prev = getAttribute(name);
		if(prev != null) {
			log.warn("attribute already inserted "+name);
		} else {
			int id = attributes.size();
			if(id>127) {
				throw new RuntimeException("id overflow");
			}
			attributes.add(new Attribute((byte)id, encoding, name));
			setUnsavedMetaChanges();
		}
	}
	
	public synchronized void setUnsavedMetaChanges() {
		unsavedMetaChanges = true;		
	}
	
	private synchronized void unsetUnsavedMetaChanges() {
		unsavedMetaChanges = false;
	}
	
	public synchronized boolean hasUnsavedMetaChanges() {
		return unsavedMetaChanges;		
	}

	public synchronized Attribute getOrAddAttribute(String name, int encoding) {
		Attribute attr = getAttribute(name);
		if(attr == null) {
			addAttribute(name, encoding);
			attr = getAttribute(name);
		}
		return attr;
	}

	public synchronized LinkedHashMap<String, Object> metaToYaml() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		LinkedHashMap<Integer, Object> attrMap = new LinkedHashMap<Integer, Object>();
		for(Attribute attr:attributes) {
			attrMap.put((int) attr.id, attr.toYamlWithoutId());
		}
		map.put("attributes", attrMap);
		map.put("storage_type", storageType);
		if(extendedMeta != null) {
			extendedMeta.write(map);
		}
		return map;
	}

	public synchronized void writeMeta() {
		try {
			log.info("write meta " + metaPath);
			LinkedHashMap<String, Object> map = metaToYaml();
			Yaml yaml = new Yaml();
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(metaFileTemp)));
			yaml.dump(map, out);
			out.close();
			Files.move(metaPathTemp, metaPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			unsetUnsavedMetaChanges();
		} catch (Exception e) {
			log.warn(e);
		}
	}
	
	public synchronized void writeMetaIfNeeded() {
		if(hasUnsavedMetaChanges()) {
			writeMeta();
		}
	}

	public synchronized void yamlToMeta(YamlMap map) {
		attributes.clear();
		if (map.contains("attributes")) {
			Map<Number, Object> m = (Map<Number, Object>) map.getObject("attributes");
			for (Entry<Number, Object> entry : m.entrySet()) {
				int id = entry.getKey().intValue();
				YamlMap em = YamlMap.ofObject(entry.getValue());
				Attribute attribute = Attribute.ofYaml(em, id);
				attributes.add(attribute);
			}
		}
		storageType = map.optString("storage_type", "RasterUnit");
		if(extendedMeta != null) {
			extendedMeta.read(map);
		}
	}

	public synchronized void readMeta() { // throws if read error
		try {
			if (metaPath.toFile().exists()) {
				//log.info("read meta exists " + metaPath);
				YamlMap map;
				try(InputStream in = new FileInputStream(metaFile)) {
					map = YamlMap.ofObject(new Yaml().load(in));
				}
				yamlToMeta(map);
			} else {
				setUnsavedMetaChanges(); // mark for initial meta write
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.warn(e);
			throw new RuntimeException(e);
		}
	}

	public boolean isEmpty() {
		return storage().isEmpty();
	}

	/**
	 * 
	 * @return range or null
	 */
	public Range2d getTileRange2d() {
		return storage().getTileRange2d();
	}
	
	public Range2d getTileRange2dOfSubset(BandKey bandKey, Range2d subsetTileRange) {
		return storage().getTileRange2dOfSubset(bandKey, subsetTileRange);
	}

	public ReadonlyNavigableSetView<TileKey> getTileKeys() {
		return storage().tileKeysReadonly();
	}
	
	public NavigableSet<TileKey> getTileKeysOfT(int t) {
		TileKey fromElement = new TileKey(t, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
		TileKey toElement = new TileKey(t, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
		return storage().tileKeysReadonly().subSet(fromElement, true, toElement, true);
	}

	public ReadonlyVecView<Attribute> getAttributes() {
		return attributes.readonlyView();
	}
	
	public String getStorageType() {
		return storageType;
	}
}
