package pointcloud;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.UserIdentity;

import com.googlecode.javaewah.datastructure.BitSet;

import broker.Associated;
import broker.Informal;
import broker.acl.ACL;
import broker.acl.EmptyACL;
import griddb.Attribute;
import griddb.Cell;
import griddb.Encoding;
import griddb.GridDB;
import griddb.GridDB.ExtendedMeta;
import pointcloud.CellTable.ChainedFilterFunc;
import pointcloud.CellTable.FilterFunc;
import rasterunit.BandKey;
import rasterunit.Tile;
import rasterunit.TileCollection;
import rasterunit.TileKey;
import util.Range2d;
import util.collections.ReadonlyNavigableSetView;
import util.yaml.YamlMap;

public class PointCloud implements AutoCloseable {
	private static final Logger log = LogManager.getLogger();

	private static final int CURRENT_VERSION_MAJOR = 1;
	private static final int CURRENT_VERSION_MINOR = 0;
	private static final int CURRENT_VERSION_PATCH = 1;

	private static final AttributeSelector ATTRIBUTE_SELECTOR_ALL = new AttributeSelector().all();

	private final GridDB griddb;
	private Attribute attr_x;
	private Attribute attr_y;
	private Attribute attr_z;
	private Attribute attr_intensity;
	private Attribute attr_returnNumber;
	private Attribute attr_returns;
	private Attribute attr_scanDirectionFlag;
	private Attribute attr_edgeOfFlightLine;
	private Attribute attr_classification;
	private Attribute attr_scanAngleRank;
	private Attribute attr_gpsTime;
	private Attribute attr_red;
	private Attribute attr_green;
	private Attribute attr_blue;

	private double cellscale = 100;
	private double cellsize = 100;
	private DoublePoint celloffset = null;
	private String code = "";
	private String proj4 = "";
	private ACL acl = EmptyACL.ADMIN;
	private ACL acl_mod = EmptyACL.ADMIN;
	private Associated associated = new Associated();
	private Informal informal = Informal.EMPTY;
	private int version_major = CURRENT_VERSION_MAJOR;
	private int version_minor = CURRENT_VERSION_MINOR;
	private int version_patch = CURRENT_VERSION_PATCH;

	public final PointCloudConfig config;

	public PointCloud(PointCloudConfig config) {
		this.config = config;
		griddb = new GridDB(config.path, "pointcloud", new ExtendedMetaHook(), config.preferredStorageType, config.transaction);
		griddb.readMeta();
		loadAttributes();
		griddb.writeMeta();
	}

	private void loadAttributes() {
		attr_x = griddb.getAttribute("x");
		attr_y = griddb.getAttribute("y");
		attr_z = griddb.getAttribute("z");
		attr_intensity = griddb.getAttribute("intensity");
		attr_returnNumber = griddb.getAttribute("returnNumber");
		attr_returns = griddb.getAttribute("returns");
		attr_scanDirectionFlag = griddb.getAttribute("scanDirectionFlag");
		attr_edgeOfFlightLine = griddb.getAttribute("edgeOfFlightLine");
		attr_classification = griddb.getAttribute("classification");
		attr_scanAngleRank = griddb.getAttribute("scanAngleRank");
		attr_gpsTime = griddb.getAttribute("gpsTime");
		attr_red = griddb.getAttribute("red");
		attr_green = griddb.getAttribute("green");
		attr_blue = griddb.getAttribute("blue");
	}

	private void creatMissingAttributes(AttributeSelector selector) {
		boolean changed = false;

		if(selector.x && attr_x == null) {
			attr_x = griddb.getOrAddAttribute("x", Encoding.ENCODING_INT32_DELTA_ZIGZAG_PFOR);
			changed = true;
		}
		if(selector.y && attr_y == null) {
			attr_y = griddb.getOrAddAttribute("y", Encoding.ENCODING_INT32_DELTA_ZIGZAG_PFOR);
			changed = true;
		}
		if(selector.z && attr_z == null) {
			attr_z = griddb.getOrAddAttribute("z", Encoding.ENCODING_INT32_DELTA_ZIGZAG_PFOR);
			changed = true;
		}
		if(selector.intensity && attr_intensity == null) {
			attr_intensity = griddb.getOrAddAttribute("intensity", Encoding.ENCODING_UINT16);
			changed = true;
		}
		if(selector.returnNumber && attr_returnNumber == null) {
			attr_returnNumber = griddb.getOrAddAttribute("returnNumber", Encoding.ENCODING_UINT8);
			changed = true;
		}
		if(selector.returns && attr_returns == null) {
			attr_returns = griddb.getOrAddAttribute("returns", Encoding.ENCODING_UINT8);
			changed = true;
		}
		if(selector.scanDirectionFlag && attr_scanDirectionFlag == null) {
			attr_scanDirectionFlag = griddb.getOrAddAttribute("scanDirectionFlag", Encoding.ENCODING_BITSET);
			changed = true;
		}
		if(selector.edgeOfFlightLine && attr_edgeOfFlightLine == null) {
			attr_edgeOfFlightLine = griddb.getOrAddAttribute("edgeOfFlightLine", Encoding.ENCODING_BITSET);
			changed = true;
		}
		if(selector.classification && attr_classification == null) {
			attr_classification = griddb.getOrAddAttribute("classification", Encoding.ENCODING_UINT8);
			changed = true;
		}
		if(selector.scanAngleRank && attr_scanAngleRank == null) {
			attr_scanAngleRank = griddb.getOrAddAttribute("scanAngleRank", Encoding.ENCODING_INT8);
			changed = true;
		}
		if(selector.gpsTime && attr_gpsTime == null) {
			attr_gpsTime = griddb.getOrAddAttribute("gpsTime", Encoding.ENCODING_INT64_DELTA_ZIGZAG_SPLIT_SPLIT);
			changed = true;
		}
		if(selector.red && attr_red == null) {
			attr_red = griddb.getOrAddAttribute("red", Encoding.ENCODING_UINT16_SPLIT);
			changed = true;
		}
		if(selector.green && attr_green == null) {
			attr_green = griddb.getOrAddAttribute("green", Encoding.ENCODING_UINT16_SPLIT);
			changed = true;
		}
		if(selector.blue && attr_blue == null) {
			attr_blue = griddb.getOrAddAttribute("blue", Encoding.ENCODING_UINT16_SPLIT);
			changed = true;
		}

		if(changed) {
			griddb.writeMeta();
		}
	}

	public Tile createTile(CellTable cellTable, int cx, int cy, int cz) throws IOException {
		AttributeSelector selector = cellTable.toSelector();
		creatMissingAttributes(selector);
		int column_count = selector.count();
		byte[][] columns = new byte[column_count][];
		Attribute[] attributes = new Attribute[column_count];
		int column_index = 0;
		if(selector.x) {
			columns[column_index] = Encoding.createIntData(attr_x.encoding, cellTable.x, cellTable.rows);
			attributes[column_index++] = attr_x;
		}
		if(selector.y) {
			columns[column_index] = Encoding.createIntData(attr_y.encoding, cellTable.y, cellTable.rows);
			attributes[column_index++] = attr_y;
		}
		if(selector.z) {
			columns[column_index] = Encoding.createIntData(attr_z.encoding, cellTable.z, cellTable.rows);
			attributes[column_index++] = attr_z;
		}
		if(selector.intensity) {
			columns[column_index] = Encoding.createCharData(attr_intensity.encoding, cellTable.intensity, cellTable.rows);
			attributes[column_index++] = attr_intensity;
		}
		if(selector.returnNumber) {
			columns[column_index] = Encoding.createByteData(attr_returnNumber.encoding, cellTable.returnNumber, cellTable.rows);
			attributes[column_index++] = attr_returnNumber;
		}
		if(selector.returns) {
			columns[column_index] = Encoding.createByteData(attr_returns.encoding, cellTable.returns, cellTable.rows);
			attributes[column_index++] = attr_returns;
		}
		if(selector.scanDirectionFlag) {
			columns[column_index] = Encoding.createBitSetData(attr_scanDirectionFlag.encoding, cellTable.scanDirectionFlag, cellTable.rows);
			attributes[column_index++] = attr_scanDirectionFlag;
		}
		if(selector.edgeOfFlightLine) {
			columns[column_index] = Encoding.createBitSetData(attr_edgeOfFlightLine.encoding, cellTable.edgeOfFlightLine, cellTable.rows);
			attributes[column_index++] = attr_edgeOfFlightLine;
		}
		if(selector.classification) {
			columns[column_index] = Encoding.createByteData(attr_classification.encoding, cellTable.classification, cellTable.rows);
			attributes[column_index++] = attr_classification;
		}
		if(selector.scanAngleRank) {
			columns[column_index] = Encoding.createByteData(attr_scanAngleRank.encoding, cellTable.scanAngleRank, cellTable.rows);
			attributes[column_index++] = attr_scanAngleRank;
		}
		if(selector.gpsTime) {
			columns[column_index] = Encoding.createLongData(attr_gpsTime.encoding, cellTable.gpsTime, cellTable.rows);
			attributes[column_index++] = attr_gpsTime;
		}
		if(selector.red) {
			columns[column_index] = Encoding.createCharData(attr_red.encoding, cellTable.red, cellTable.rows);
			attributes[column_index++] = attr_red;
		}
		if(selector.green) {
			columns[column_index] = Encoding.createCharData(attr_green.encoding, cellTable.green, cellTable.rows);
			attributes[column_index++] = attr_green;
		}
		if(selector.blue) {
			columns[column_index] = Encoding.createCharData(attr_blue.encoding, cellTable.blue, cellTable.rows);
			attributes[column_index++] = attr_blue;
		}
		byte[] cellData = Cell.createData(attributes, columns, column_count);
		Tile tile = griddb.createTile(cx, cy, cz, cellData);
		//log.info("create cell cx: " + cx + " cy: " + cy + " columns: " + column_count + " rows: " + cellTable.rows + " compressed: " + cellData.length);
		return tile;
	}

	public GridDB getGriddb() {
		return griddb;
	}

	private class ExtendedMetaHook implements ExtendedMeta {

		private static final String TYPE = "pointcloud";

		@Override
		public void read(YamlMap yamlMap) {
			synchronized (griddb) {
				String type = yamlMap.getString("type");
				if (!type.equals(TYPE)) {
					throw new RuntimeException("wrong type: " + type);
				}
				String versionText = yamlMap.getString("version");
				String[] versionParts = versionText.split("\\.");
				if(versionParts.length < 1 || versionParts.length > 3) {
					throw new RuntimeException("version error");
				} else if(versionParts.length == 1) {
					version_major = Integer.parseInt(versionParts[0]);
					version_minor = 0;
					version_patch = 0;
				} else if(versionParts.length == 2) {
					version_major = Integer.parseInt(versionParts[0]);
					version_minor = Integer.parseInt(versionParts[1]);
					version_patch = 0;
				} else if(versionParts.length == 3) {
					version_major = Integer.parseInt(versionParts[0]);
					version_minor = Integer.parseInt(versionParts[1]);
					version_patch = Integer.parseInt(versionParts[2]);
				}

				cellscale = yamlMap.getDouble("cellscale");
				cellsize = yamlMap.getDouble("cellsize");
				if(yamlMap.contains("celloffset")) {
					celloffset = DoublePoint.ofYaml(yamlMap.getMap("celloffset"));
				}
				code = yamlMap.optString("code", "");
				proj4 = yamlMap.optString("proj4", "");
				acl = ACL.of(yamlMap.optList("acl").asStrings());
				acl_mod = ACL.of(yamlMap.optList("acl_mod").asStrings());
				associated = new Associated();
				if (yamlMap.contains("associated")) {
					associated = Associated.ofYaml(yamlMap.getMap("associated"));
				}
				informal = Informal.ofYaml(yamlMap);
			}
		}	

		@Override
		public synchronized void write(LinkedHashMap<String, Object> map) {
			synchronized (griddb) {
				map.put("type", TYPE);
				String version_text = version_major + "." + version_minor + "." + version_patch;
				map.put("version", version_text);
				map.put("cellscale", cellscale);
				map.put("cellsize", cellsize);
				if(celloffset != null) {
					map.put("celloffset", celloffset.toYaml());
				}
				if(hasCode()) {
					map.put("code", code);
				}
				if(hasProj4()) {
					map.put("proj4", proj4);
				}
				map.put("acl", acl.toYaml());
				map.put("acl_mod", acl_mod.toYaml());
				map.put("associated", associated.toYaml());
				informal.writeYaml(map);
			}
		}	
	}

	public void writeTile(Tile tile) throws IOException {
		griddb.writeTile(tile);		
	}

	public void commit() {
		griddb.commit();
	}

	@Override
	public void close() {
		try {
			griddb.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	public double getCellscale() {
		return cellscale;
	}

	public double getCellsize() {
		return cellsize;
	}

	public DoublePoint getCelloffset() {
		return celloffset;
	}

	public DoublePoint getOrSetCelloffset(double xcelloffset, double ycelloffset) {
		synchronized (griddb) {
			if(celloffset == null) {
				celloffset = new DoublePoint(xcelloffset, ycelloffset);
				log.info("set celloffset " + celloffset);
				griddb.writeMeta();
			}
		}
		return celloffset;
	}

	public boolean trySetCellscale(double cellscale) {
		synchronized (griddb) {
			if(griddb.isEmpty()) {
				this.cellscale = cellscale;
				griddb.writeMeta();
				return true;
			} else {
				return false;
			}
		}		
	}

	public boolean trySetCellsize(double cellsize) {
		synchronized (griddb) {
			if(griddb.isEmpty()) {
				this.cellsize = cellsize;
				griddb.writeMeta();
				return true;
			} else {
				return false;
			}
		}		
	}

	public Stream<Cell> getCells(double xmin, double ymin, double xmax, double ymax) {
		if(celloffset == null) {
			log.warn("no cell offset in PointCloud " + config.name);
			return Stream.empty();
		} else {
			double xcelloffset = celloffset.x;
			double ycelloffset = celloffset.y;
			int xcellmin = (int) (Math.floor(xmin / cellsize) - xcelloffset);
			int xcellmax = (int) (Math.floor(xmax / cellsize) - xcelloffset);
			int ycellmin = (int) (Math.floor(ymin / cellsize) - ycelloffset);
			int ycellmax = (int) (Math.floor(ymax / cellsize) - ycelloffset);
			//log.info(xcellmin + " " + ycellmin + " " + xcellmax + " " + ycellmax);
			Stream<Cell> cells = griddb.getCells(xcellmin, ycellmin, xcellmax, ycellmax);		
			return cells;
		}
	}

	public TileCollection getTiles(double xmin, double ymin, double xmax, double ymax) {
		if(celloffset == null) {
			log.warn("no cell offset in PointCloud " + config.name);
			return null;
		} else {
			double xcelloffset = celloffset.x;
			double ycelloffset = celloffset.y;
			int xcellmin = (int) (Math.floor(xmin / cellsize) - xcelloffset);
			int xcellmax = (int) (Math.floor(xmax / cellsize) - xcelloffset);
			int ycellmin = (int) (Math.floor(ymin / cellsize) - ycelloffset);
			int ycellmax = (int) (Math.floor(ymax / cellsize) - ycelloffset);
			//log.info(xcellmin + " " + ycellmin + " " + xcellmax + " " + ycellmax);
			return griddb.getTiles(xcellmin, ycellmin, xcellmax, ycellmax);		
		}
	}

	public int countCells(double xmin, double ymin, double xmax, double ymax) {
		if(celloffset == null) {
			log.warn("no cell offset in PointCloud " + config.name);
			return 0;
		} else {
			double xcelloffset = celloffset.x;
			double ycelloffset = celloffset.y;
			int xcellmin = (int) (Math.floor(xmin / cellsize) - xcelloffset);
			int xcellmax = (int) (Math.floor(xmax / cellsize) - xcelloffset);
			int ycellmin = (int) (Math.floor(ymin / cellsize) - ycelloffset);
			int ycellmax = (int) (Math.floor(ymax / cellsize) - ycelloffset);
			int size = griddb.getTiles(xcellmin, ycellmin, xcellmax, ycellmax).size();
			return size;
		}
	}


	public CellTable getCellTable(int cx, int cy, int cz) throws IOException {
		Cell cell = griddb.getCell(cx, cy, cz);
		if(cell == null) {
			return null;
		} else {
			return getCellTable(cell, ATTRIBUTE_SELECTOR_ALL);
		}
	}

	public CellTable getCellTable(int cx, int cy, int cz, AttributeSelector selector) throws IOException {
		Cell cell = griddb.getCell(cx, cy, cz);
		if(cell == null) {
			return null;
		} else {
			return getCellTable(cell, selector);
		}
	}

	public CellTable getCellTable(Cell cell, AttributeSelector selector) {
		//log.info("getCellTable " + selector);
		int[] x = selector.x ? cell.getInt(attr_x) : null;
		int[] y = selector.y ? cell.getInt(attr_y) : null;
		int[] z = selector.z ? cell.getInt(attr_z) : null;
		CellTable cellTable = new CellTable(cell.x, cell.y, cell.b, x.length, x, y, z);
		if(selector.intensity) {
			cellTable.intensity = cell.getChar(attr_intensity);
		}
		if(selector.returnNumber) {
			cellTable.returnNumber = cell.getByte(attr_returnNumber);
			//log.info("cellTable.returnNumber " + cellTable.returnNumber + "   " + attr_returnNumber);
		}
		if(selector.returns) {
			cellTable.returns = cell.getByte(attr_returns);
		}
		if(selector.scanDirectionFlag) {
			cellTable.scanDirectionFlag = cell.getBitSet(attr_scanDirectionFlag);
		}
		if(selector.edgeOfFlightLine) {
			cellTable.edgeOfFlightLine = cell.getBitSet(attr_edgeOfFlightLine);
		}
		if(selector.classification) {
			cellTable.classification = cell.getByte(attr_classification);
		}
		if(selector.scanAngleRank) {
			cellTable.scanAngleRank = cell.getByte(attr_scanAngleRank);
		}
		if(selector.gpsTime) {
			cellTable.gpsTime = cell.getLong(attr_gpsTime);
		}
		if(selector.red) {
			cellTable.red = cell.getChar(attr_red);
		}
		if(selector.green) {
			cellTable.green = cell.getChar(attr_green);
		}
		if(selector.blue) {
			cellTable.blue = cell.getChar(attr_blue);
		}
		return cellTable;
	}

	public Stream<CellTable> getCellTables(double xmin, double ymin, double xmax, double ymax, AttributeSelector selector) {
		Stream<Cell> cells = getCells(xmin, ymin, xmax, ymax);
		Stream<CellTable> cellTables = cells.map(cell -> getCellTable(cell, selector));
		return cellTables;
	}

	public Stream<PointTable> getPointTables(double xmin, double ymin, double xmax, double ymax, AttributeSelector selector) {
		return getPointTables(xmin, ymin, xmax, ymax, selector, (ChainedFilterFunc) null);
	}

	public Stream<PointTable> getPointTables(double xmin, double ymin, double xmax, double ymax, AttributeSelector selector, FilterFunc filterFunc) {
		return getPointTables(xmin, ymin, xmax, ymax, selector, ChainedFilterFunc.and(filterFunc));
	}

	public Stream<PointTable> getPointTables(double xmin, double ymin, double xmax, double ymax, AttributeSelector selector, ChainedFilterFunc filterFunc) {
		AttributeSelector loadSelector = selector.hasXY() ? selector : selector.copy().setXY();
		//log.info("selector " + selector); 
		//log.info("loadSelector " + loadSelector); 
		Stream<CellTable> cellTables = getCellTables(xmin, ymin, xmax, ymax, loadSelector);
		Stream<PointTable> pointTables = cellTables.map(cellTable -> {

			/*if(cellTable.returnNumber != null && cellTable.returnNumber.length > 0) {
				log.info("cellTable.returnNumber" + cellTable.returnNumber[0]);
			} else {
				log.info("cellTable.returnNumber MISSING");
			}*/

			/*log.info("CellTable " + cellTable.cx + "  " + cellTable.cy + "  offset " + celloffset.x + " " + celloffset.y + "  cellsize " + cellsize + " cellscale " + cellscale);
			if(cellTable.x != null && cellTable.x.length > 0 && cellTable.y != null && cellTable.y.length > 0 ) {
				log.info("cell point "+cellTable.x[0]+" "+cellTable.y[0]);
			}*/
			//Timer.resume("create mask");

			BitSet filter;
			if(isVersion(1, 0, 0)) { //workaround for bug in import with points on line of pxymax	
				filter = maskExtent(cellTable, xmin, ymin, xmax, ymax); // always mask extent for buggy import with point on line of pxymax
			} else {
				double pxmin = (celloffset.x + cellTable.cx) * cellsize;
				double pymin = (celloffset.y + cellTable.cy) * cellsize;
				double pxmax = (celloffset.x + cellTable.cx + 1) * cellsize - 1 / cellscale;
				double pymax = (celloffset.y + cellTable.cy + 1) * cellsize - 1 / cellscale;
				//log.info("cell range   " + pxmin + " " + pymin + " " + pxmax + " " + pymax);
				filter = xmin <= pxmin && ymin <= pymin && xmax >= pxmax && ymax >= pymax ? null : maskExtent(cellTable, xmin, ymin, xmax, ymax);
				//log.info("filter range " + xmin + " " + ymin + " " + xmax + " " + ymax + "   " + filter);
			}

			//Timer.stop("create mask");
			if(!selector.x) {
				cellTable.x = null;
			}
			if(!selector.y) {
				cellTable.y = null;
			}
			if(filterFunc != null) {
				filter = filterFunc.apply(cellTable, filter);
			}
			int size = filter == null ? cellTable.rows : filter.cardinality();
			/*if(size < cellTable.rows) {
				log.info("filter "+Timer.get("create mask"));
			}*/
			/*if(cellTable.x != null) {
				for(int x:cellTable.x) {
					log.info("px " + x);
				}
			}*/
			PointTable pointTable = size < cellTable.rows ? cellTableToPointTable(cellTable, filter, size) : cellTableToPointTable(cellTable);
			return pointTable;
		});
		return pointTables;
	}

	public BitSet maskExtent(CellTable cellTable, double xmin, double ymin, double xmax, double ymax) {
		int len = cellTable.rows;
		int[] x = cellTable.x;
		int[] y = cellTable.y;
		double scale = cellscale;
		double xoff = (celloffset.x + cellTable.cx) * cellsize;
		double yoff = (celloffset.y + cellTable.cy) * cellsize;
		BitSet bitSet = new BitSet(len);
		for (int i = 0; i < len; i++) {
			double px = (x[i] / scale) + xoff;
			double py = (y[i] / scale) + yoff;
			if(xmin <= px && px <= xmax && ymin <= py && py <= ymax) {
				bitSet.set(i);
			}
		}
		return bitSet;
	}	

	public PointTable cellTableToPointTable(CellTable cellTable) {
		int len = cellTable.rows;
		double[] x = cellTable.x == null ? null : ColumnsUtil.transform(cellTable.x, len, cellscale, (celloffset.x + cellTable.cx) * cellsize);
		double[] y = cellTable.y == null ? null : ColumnsUtil.transform(cellTable.y, len, cellscale, (celloffset.y + cellTable.cy) * cellsize);
		double[] z = cellTable.z == null ? null : ColumnsUtil.transform(cellTable.z, len, cellscale);
		PointTable pointTable = new PointTable(len, x, y, z);
		pointTable.intensity = cellTable.intensity;
		pointTable.returnNumber = cellTable.returnNumber;
		pointTable.returns = cellTable.returns;
		pointTable.scanDirectionFlag = cellTable.scanDirectionFlag;
		pointTable.edgeOfFlightLine = cellTable.edgeOfFlightLine;
		pointTable.classification = cellTable.classification;
		pointTable.scanAngleRank = cellTable.scanAngleRank;
		pointTable.gpsTime = cellTable.gpsTime;
		pointTable.red = cellTable.red;
		pointTable.green = cellTable.green;
		pointTable.blue = cellTable.blue;
		return pointTable;
	}

	public PointTable cellTableToPointTable(CellTable cellTable, BitSet mask, int size) {
		int len = cellTable.rows;
		//log.info("cellTableToPointTable " + size +" / " + len +" -> " + (((double)size) / len));
		double[] x = cellTable.x == null ? null : ColumnsUtil.filterTransform(cellTable.x, len, mask, size, cellscale, (celloffset.x + cellTable.cx) * cellsize);
		double[] y = cellTable.y == null ? null : ColumnsUtil.filterTransform(cellTable.y, len, mask, size, cellscale, (celloffset.y + cellTable.cy) * cellsize);
		double[] z = cellTable.z == null ? null : ColumnsUtil.filterTransform(cellTable.z, len, mask, size, cellscale);
		PointTable pointTable = new PointTable(size, x, y, z);
		if(cellTable.intensity != null) {
			pointTable.intensity = ColumnsUtil.filter(cellTable.intensity, len, mask, size);
		}
		if(cellTable.returnNumber != null) {
			pointTable.returnNumber = ColumnsUtil.filter(cellTable.returnNumber, len, mask, size);
		}
		if(cellTable.returns != null) {
			pointTable.returns = ColumnsUtil.filter(cellTable.returns, len, mask, size);
		}
		if(cellTable.scanDirectionFlag != null) {
			pointTable.scanDirectionFlag = ColumnsUtil.filter(cellTable.scanDirectionFlag, len, mask, size);
		}
		if(cellTable.edgeOfFlightLine != null) {
			pointTable.edgeOfFlightLine = ColumnsUtil.filter(cellTable.edgeOfFlightLine, len, mask, size);
		}
		if(cellTable.classification != null) {
			pointTable.classification = ColumnsUtil.filter(cellTable.classification, len, mask, size);
		}
		if(cellTable.scanAngleRank != null) {
			pointTable.scanAngleRank = ColumnsUtil.filter(cellTable.scanAngleRank, len, mask, size);
		}
		if(cellTable.gpsTime != null) {
			pointTable.gpsTime = ColumnsUtil.filter(cellTable.gpsTime, len, mask, size);
		}
		if(cellTable.red != null) {
			pointTable.red = ColumnsUtil.filter(cellTable.red, len, mask, size);
		}
		if(cellTable.green != null) {
			pointTable.green = ColumnsUtil.filter(cellTable.green, len, mask, size);
		}
		if(cellTable.blue != null) {
			pointTable.blue = ColumnsUtil.filter(cellTable.blue, len, mask, size);
		}
		return pointTable;
	}

	public DoubleRect getRange() {
		Range2d range2d = griddb.getTileRange2d();
		if(range2d == null) {
			return null;
		} else {
			double xmin = (celloffset.x + range2d.xmin) * cellsize;
			double ymin = (celloffset.y + range2d.ymin) * cellsize;
			double xmax = (celloffset.x + range2d.xmax + 1) * cellsize - (1 / cellscale);
			double ymax = (celloffset.y + range2d.ymax + 1) * cellsize - (1 / cellscale);
			return new DoubleRect(xmin, ymin, xmax, ymax);
		}
	}

	/**
	 * 
	 * @return range or null
	 */
	public Range2d getCellRange() {
		return griddb.getTileRange2d();
	}

	public Range2d getCellRange2dOfSubset(Range2d subsetCellRange) {
		return griddb.getTileRange2dOfSubset(new BandKey(0, 0), subsetCellRange);
	}

	public String getCode() {
		return code;
	}

	public short getEPSGcode() {
		String c = getCode();		
		return c.startsWith("EPSG:") ? Short.parseShort(c.substring(5)) : 0;
	}

	public boolean hasCode() {
		return !code.isEmpty();
	}

	public void setCode(String code) {
		this.code = code;
		griddb.writeMeta();
	}

	public void setCodeEPSG(int epsg) {
		this.code = "EPSG:" + epsg;
		griddb.writeMeta();
	}

	public String getProj4() {
		return proj4;
	}

	public boolean hasProj4() {
		return !proj4.isEmpty();
	}

	public void setProj4(String proj4) {
		this.proj4 = proj4;
		griddb.writeMeta();
	}

	public File getMetaFile() {
		return griddb.metaFile;
	}

	public String getName() {
		return config.name;
	}

	public ACL getACL() {
		return acl;
	}

	public ACL getACL_mod() {
		return acl_mod;
	}

	public ReadonlyNavigableSetView<TileKey> getTileKeys() {
		return griddb.getTileKeys();
	}

	public boolean isEmpty() {
		return griddb.isEmpty();
	}

	public boolean isAllowed(UserIdentity userIdentity) {
		return acl.isAllowed(userIdentity);
	}

	public void check(UserIdentity userIdentity) {
		acl.check(userIdentity);
	}

	public boolean isAllowedMod(UserIdentity userIdentity) {
		return acl_mod.isAllowed(userIdentity);
	}

	public void checkMod(UserIdentity userIdentity) {
		acl_mod.check(userIdentity);
	}

	public void setACL(ACL acl) {
		this.acl = acl;
		griddb.writeMeta();		
	}

	public void setACL_mod(ACL acl_mod) {
		this.acl_mod = acl_mod;
		griddb.writeMeta();		
	}

	public boolean isClassified_ground() {
		return true;
	}

	public boolean isClassified_vegetation() {
		return true;
	}

	public AttributeSelector getSelector() {
		AttributeSelector selector = new AttributeSelector();
		for(Attribute attribute : griddb.getAttributes()) {
			selector.set(attribute.name);
		}
		return selector;
	}

	public Associated getAssociated() {
		return associated;
	}

	public Informal informal() {
		return informal;
	}

	public void setInformal(Informal informal) {
		this.informal = informal;
		griddb.writeMeta();
	}

	public void setAssociatedRasterDB(String name) {
		associated.setRasterDB(name);
		griddb.writeMeta();
	}

	public void setAssociatedPoiGroups(List<String> poi_groups) {
		associated.setPoi_groups(poi_groups);
		griddb.writeMeta();
	}

	public void setAssociatedRoiGroups(List<String> roi_groups) {
		associated.setRoi_groups(roi_groups);
		griddb.writeMeta();
	}

	public void commitMeta() {
		griddb.writeMeta();
	}

	public boolean isVersion(int version_major, int version_minor, int version_patch) {
		return this.version_major == version_major && this.version_minor == version_minor && this.version_patch == version_patch;
	}

	public boolean isVersionOrNewer(int version_major, int version_minor, int version_patch) {
		if(this.version_major < version_major) {
			return false;
		}
		if(this.version_minor < version_minor) {
			return false;
		}
		if(this.version_patch < version_patch) {
			return false;
		}
		return true;
	}
}
