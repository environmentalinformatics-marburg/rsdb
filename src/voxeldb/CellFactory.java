package voxeldb;

import java.io.IOException;
import java.util.NavigableSet;
import java.util.stream.Stream;

import broker.TimeSlice;
import griddb.Attribute;
import griddb.Cell;
import griddb.Encoding;
import griddb.GridDB;
import rasterunit.KeyRange;
import rasterunit.Tile;
import rasterunit.TileKey;
import util.Range2d;
import util.Range3d;
import util.collections.vec.Vec;

public class CellFactory {

	private final VoxelDB voxeldb;
	private final GridDB griddb;
	private final int cellsize;

	private Attribute attr_count = null;
	private Attribute attr_red = null;
	private Attribute attr_green = null;
	private Attribute attr_blue = null;

	public CellFactory(VoxelDB voxeldb) {
		this.voxeldb = voxeldb;
		this.griddb = voxeldb.getGriddb();
		this.cellsize = voxeldb.getCellsize();

	}

	public static CellFactory ofAll(VoxelDB voxeldb) {
		CellFactory cellFactory = new CellFactory(voxeldb);
		cellFactory.setCount();
		cellFactory.setRed();
		cellFactory.setGreen();
		cellFactory.setBlue();
		return cellFactory;
	}

	public static CellFactory ofCount(VoxelDB voxeldb) {
		CellFactory cellFactory = new CellFactory(voxeldb);
		cellFactory.setCount();
		return cellFactory;
	}

	public CellFactory setCount() {
		attr_count = voxeldb.getGriddb().getAttribute("count");
		return this;
	}

	public CellFactory setRed() {
		attr_red = voxeldb.getGriddb().getAttribute("red");	
		return this;
	}

	public CellFactory setGreen() {
		attr_green = voxeldb.getGriddb().getAttribute("green");	
		return this;
	}

	public CellFactory setBlue() {
		attr_blue = voxeldb.getGriddb().getAttribute("blue");	
		return this;
	}

	public VoxelCell getVoxelCell(int x, int y, int z, int t) throws IOException {		
		Cell cell = griddb.getCell(z, x, y, t);
		return cellToVoxelCell(cell);
	}

	public Stream<VoxelCell> getVoxelCells(TimeSlice timeSlice) {
		return griddb.getTileKeysOfT(timeSlice.id).stream().map(tileKey -> {
			try {
				return griddb.storage().readTile(tileKey);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		})
				.map(GridDB::tileToCell)
				.map(this::cellToVoxelCell);
	}


	public Stream<VoxelCell> getVoxelCells(TimeSlice timeSlice, Range3d range) {		
		Range3d cellRange = range.div(cellsize, cellsize, cellsize);
		TileKey fromElement = new TileKey(timeSlice.id, cellRange.ymin, cellRange.xmin, cellRange.zmin);
		TileKey toElement = new TileKey(timeSlice.id, cellRange.ymax, cellRange.xmax, cellRange.zmax);
		NavigableSet<TileKey> keys = griddb.getTileKeys().subSet(fromElement, true, toElement, true);
		int xmin = cellRange.zmin;
		int ymin = cellRange.xmin;
		int xmax = cellRange.zmax;
		int ymax = cellRange.xmax;
		Stream<VoxelCell> result = keys.stream()
				.filter(key -> ymin <= key.y && key.y <= ymax && xmin <= key.x && key.x <= xmax)
				.map(tileKey -> {
					try {
						return griddb.storage().readTile(tileKey);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				})
				.map(GridDB::tileToCell)
				.map(this::cellToVoxelCell);
		return result;
	}


	public Stream<VoxelCell> getVoxelCells() {
		return griddb.getTileKeys().stream().map(tileKey -> {
			try {
				return griddb.storage().readTile(tileKey);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		})
				.map(GridDB::tileToCell)
				.map(this::cellToVoxelCell);
	}

	/**
	 * convert 1d-array to 3d-array
	 * @param ints nullable
	 * @return 3d-array or null
	 */
	private int[][][] getIntCube(int[] ints) {
		if(ints == null) {
			return null;
		}
		if(ints.length != cellsize * cellsize * cellsize) {
			throw new RuntimeException("wrong element count "  + ints.length + "   " + (cellsize * cellsize * cellsize));
		}
		int[][][] r = new int[cellsize][cellsize][cellsize];
		int pos = 0;
		for(int z = 0; z < cellsize; z++) {
			for(int y = 0; y < cellsize; y++) {
				for(int x = 0; x < cellsize; x++) {
					r[z][y][x] = ints[pos++];
				}
			}
		}
		return r;
	}

	public VoxelCell cellToVoxelCell(Cell cell) {
		if(cell == null) {
			return null;
		}
		int[][][] cnt = getIntCube(cell.getInt(attr_count));
		int[][][] red = getIntCube(cell.getInt(attr_red));
		int[][][] green = getIntCube(cell.getInt(attr_green));
		int[][][] blue = getIntCube(cell.getInt(attr_blue));

		VoxelCell voxelCell = new VoxelCell(cell.y, cell.b, cell.x, cnt, red, green, blue);
		return voxelCell;
	}

	private int[] getInts(int[][][] cube) {
		if(cube == null) {
			return null;
		}
		int len = cellsize * cellsize * cellsize;
		int[] r = new int[len];
		int pos = 0;
		for(int z = 0; z < cellsize; z++) {
			for(int y = 0; y < cellsize; y++) {
				for(int x = 0; x < cellsize; x++) {
					r[pos++] = cube[z][y][x];
				}
			}
		}
		return r;
	}

	private Attribute ensure_attr_count() {
		if(attr_count == null) {
			attr_count = griddb.getOrAddAttribute("count", Encoding.ENCODING_INT32);
		}
		return attr_count;
	}

	private Attribute ensure_attr_red() {
		if(attr_red == null) {
			attr_red = griddb.getOrAddAttribute("red", Encoding.ENCODING_INT32);
		}
		return attr_red;
	}

	private Attribute ensure_attr_green() {
		if(attr_green == null) {
			attr_green = griddb.getOrAddAttribute("green", Encoding.ENCODING_INT32);
		}
		return attr_green;
	}

	private Attribute ensure_attr_blue() {
		if(attr_blue == null) {
			attr_blue = griddb.getOrAddAttribute("blue", Encoding.ENCODING_INT32);
		}
		return attr_blue;
	}

	private byte[] createIntData(Attribute attr, int[][][] cube) {
		int[] ints = getInts(cube);		
		byte[] data = Encoding.createIntData(attr.encoding, ints, ints.length);
		return data;
	}

	private static class AttributeData {
		public final Attribute attribue;
		public final byte[] data;

		public AttributeData(Attribute attribue, byte[] data) {
			this.attribue = attribue;
			this.data = data;
		}

		public byte[] data() {
			return data;
		}

		public Attribute attribue() {
			return attribue;
		}
	}

	private Tile voxelCellToTile(VoxelCell voxelCell, int t) {
		Vec<AttributeData> dataCollector = new Vec<AttributeData>();

		if(voxelCell.cnt != null) {
			Attribute attr = ensure_attr_count();
			byte[] data = createIntData(attr, voxelCell.cnt);
			dataCollector.add(new AttributeData(attr, data));			
		}
		if(voxelCell.red != null) {
			Attribute attr = ensure_attr_red();
			byte[] data = createIntData(attr, voxelCell.red);
			dataCollector.add(new AttributeData(attr, data));			
		}
		if(voxelCell.green != null) {
			Attribute attr = ensure_attr_green();
			byte[] data = createIntData(attr, voxelCell.green);
			dataCollector.add(new AttributeData(attr, data));			
		}
		if(voxelCell.blue != null) {
			Attribute attr = ensure_attr_blue();
			byte[] data = createIntData(attr, voxelCell.blue);
			dataCollector.add(new AttributeData(attr, data));			
		}

		Attribute[] attrColumns = dataCollector.mapArray(AttributeData::attribue, Attribute[]::new);
		byte[][] dataColumns = dataCollector.mapArray(AttributeData::data, byte[][]::new);
		byte[] cellData = Cell.createData(attrColumns, dataColumns, dataCollector.size());
		Tile tile = griddb.createTile(voxelCell.z, voxelCell.x, voxelCell.y, t, cellData);
		return tile;
	}

	public void writeVoxelCell(VoxelCell voxelCell, int t) throws IOException {
		Tile tile = voxelCellToTile(voxelCell, t);
		griddb.writeTile(tile);
	}

	public Range3d toRange(VoxelCell voxelCell) {
		int xmin = voxelCell.x * cellsize;
		int ymin = voxelCell.y * cellsize;
		int zmin = voxelCell.z * cellsize;
		int xmax = xmin + cellsize - 1;
		int ymax = ymin + cellsize - 1;
		int zmax = zmin + cellsize - 1;
		return new Range3d(xmin, ymin, zmin, xmax, ymax, zmax);
	}
	
	public Range3d getCellRange() {
		KeyRange keyRange = voxeldb.getGriddb().storage().getKeyRange();
		if(keyRange == null) {
			return null;
		}
		return new Range3d(keyRange.ymin, keyRange.bmin, keyRange.xmin, keyRange.ymax, keyRange.bmax, keyRange.xmax);		
	}
}
