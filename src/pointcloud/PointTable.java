package pointcloud;

import java.util.function.Function;

import com.googlecode.javaewah.datastructure.BitSet;

import pointdb.base.Point2d;
import pointdb.base.PolygonUtil;

public class PointTable {
	//private static final Logger log = LogManager.getLogger();

	/**
	 * rows denotes valid rows, higher rows are not valid. So x.length may be higher than rows.
	 */
	public int rows;
	public double[] x;
	public double[] y;
	public double[] z;
	public char[] intensity; // Uint16
	public byte[] returnNumber; // Uint8
	public byte[] returns; // Uint8
	public java.util.BitSet scanDirectionFlag;
	public java.util.BitSet edgeOfFlightLine;
	
	/**
	 * classifications:
	 * <br> 1: Unclassified
	 * <br> 2: Ground
	 * <br> 3: Low Vegetation
	 * <br> 4: Medium Vegetation
	 * <br> 5: High Vegetation
	 * <br> 6: Building
	 * <br> 7: Low Point (noise)
	 * <br> 8: Model Key-point (mass point)
	 * <br> 9: Water
	 * <br>13: unofficial classification in layer "hessen" Vegetation 
	 * <br>Reserved for ASPRS Definition (13-31) 
	 */
	public byte[] classification; // Uint8
	public byte[] scanAngleRank; // Int8
	public long[] gpsTime; // Uint64
	public char[] red;
	public char[] green;
	public char[] blue;

	public PointTable(int rows) {
		this.rows = rows;
	}

	public PointTable(int rows, double[] x, double[] y, double[] z) {
		this.rows = rows;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public String toString() {
		return "PointTable[" + rows + "]";
	}

	/**
	 * Set all valid attributes in selector, no change to null attributes
	 * no change to PointTable
	 * @param selector target
	 */
	public void addAttributes(AttributeSelector selector) {		
		selector.x = x == null ? selector.x : true;
		selector.y = y == null ? selector.y : true; 
		selector.z = z == null ? selector.z : true; 
		selector.intensity = intensity == null ? selector.intensity : true; 
		selector.returnNumber = returnNumber == null ? selector.returnNumber : true; 
		selector.returns = returns == null ? selector.returns : true; 
		selector.scanDirectionFlag = scanDirectionFlag == null ? selector.scanDirectionFlag : true; 
		selector.edgeOfFlightLine = edgeOfFlightLine == null ? selector.edgeOfFlightLine : true; 
		selector.classification = classification == null ? selector.classification : true; 
		selector.scanAngleRank = scanAngleRank == null ? selector.scanAngleRank : true; 
		selector.gpsTime = gpsTime == null ? selector.gpsTime : true;
		selector.red = red == null ? selector.red : true;
		selector.green = green == null ? selector.green : true;
		selector.blue = blue == null ? selector.blue : true;
	}

	public static AttributeSelector getSelector(PointTable[] pointTables) {
		AttributeSelector selector = new AttributeSelector();
		for (PointTable pointTable : pointTables) {
			pointTable.addAttributes(selector);
		}
		return selector;
	}
	
	public static PointTable applyMask(PointTable srcTable, BitSet mask) {
		int len = srcTable.rows;
		int size = mask.cardinality();
		if(size == len) {
			return srcTable;
		}		
		double[] x = ColumnsUtil.filter(srcTable.x, len, mask, size);
		double[] y = ColumnsUtil.filter(srcTable.y, len, mask, size);
		double[] z = ColumnsUtil.filter(srcTable.z, len, mask, size);
		PointTable dstTable = new PointTable(size, x, y, z);
		if(srcTable.intensity != null) {
			dstTable.intensity = ColumnsUtil.filter(srcTable.intensity, len, mask, size);
		}
		if(srcTable.returnNumber != null) {
			dstTable.returnNumber = ColumnsUtil.filter(srcTable.returnNumber, len, mask, size);
		}
		if(srcTable.returns != null) {
			dstTable.returns = ColumnsUtil.filter(srcTable.returns, len, mask, size);
		}
		if(srcTable.scanDirectionFlag != null) {
			dstTable.scanDirectionFlag = ColumnsUtil.filter(srcTable.scanDirectionFlag, len, mask, size);
		}
		if(srcTable.edgeOfFlightLine != null) {
			dstTable.edgeOfFlightLine = ColumnsUtil.filter(srcTable.edgeOfFlightLine, len, mask, size);
		}
		if(srcTable.classification != null) {
			dstTable.classification = ColumnsUtil.filter(srcTable.classification, len, mask, size);
		}
		if(srcTable.scanAngleRank != null) {
			dstTable.scanAngleRank = ColumnsUtil.filter(srcTable.scanAngleRank, len, mask, size);
		}
		if(srcTable.gpsTime != null) {
			dstTable.gpsTime = ColumnsUtil.filter(srcTable.gpsTime, len, mask, size);
		}
		if(srcTable.red != null) {
			dstTable.red = ColumnsUtil.filter(srcTable.red, len, mask, size);
		}
		if(srcTable.green != null) {
			dstTable.green = ColumnsUtil.filter(srcTable.green, len, mask, size);
		}
		if(srcTable.blue != null) {
			dstTable.blue = ColumnsUtil.filter(srcTable.blue, len, mask, size);
		}
		return dstTable;
	}

	public static boolean isVegetaion(byte classification) {
		return classification == 3 || classification == 4 || classification == 5 || classification == 13;
	}

	public static boolean isGround(byte classification) {
		return classification == 2 || classification == 8;
	}

	/**
	 * Point is classified as some valid entity, or not classified
	 * @return
	 */
	public static boolean isEntity(byte classification) {
		return isGround(classification) || isVegetaion(classification) || classification == 6 || classification == 9 || classification == 0;
	}

	public static boolean isLastReturn(byte returnNumber, byte returns) {
		return returnNumber == returns;
	}

	public BitSet filterEntity() {
		int len = rows;
		BitSet bitset = new BitSet(len);
		byte[] c = this.classification;
		if(c != null) {
			for (int i = 0; i < len; i++) {
				if(isEntity(c[i])) {
					bitset.set(i);
				}
			}
		}
		return bitset;
	}
	
	public BitSet filterGround() {
		int len = rows;
		BitSet bitset = new BitSet(len);
		byte[] c = this.classification;
		if(c != null) {
			for (int i = 0; i < len; i++) {
				if(isGround(c[i])) {
					bitset.set(i);
				}
			}
		}
		return bitset;
	}
	
	public BitSet filterFirstReturn() {
		int len = rows;
		byte[] r = this.returnNumber;
		BitSet bitset = new BitSet(len);
		if(r != null) {
			for (int i = 0; i < len; i++) {
				if(r[i] == 1) {
					bitset.set(i);
				}
			}
		}
		return bitset;
	}
	
	public BitSet filterLastReturn() {
		int len = rows;
		byte[] returnNumber = this.returnNumber;
		byte[] returns = this.returns;
		BitSet bitset = new BitSet(len);
		if(returnNumber != null && returns != null) {
			for (int i = 0; i < len; i++) {
				if(returnNumber[i] == returns[i]) {
					bitset.set(i);
				}
			}
		}
		return bitset;
	}
	
	public BitSet filterClassification(byte classification) {
		int len = rows;
		BitSet bitset = new BitSet(len);
		byte[] c = this.classification;
		if(c != null) {
			for (int i = 0; i < len; i++) {
				if(c[i] == classification) {
					bitset.set(i);
				}
			}
		}
		return bitset;
	}
	
	public static class FilterByPolygonFunc implements Function<PointTable, BitSet> {
		private final double[] cvx;
		private final double[] cvy;
		
		public FilterByPolygonFunc(double[] vx, double[] vy) {
			this.cvx = vx;
			this.cvy = vy;
		}
		
		@Override
		public BitSet apply(PointTable pointTable) {
			int len = pointTable.rows;
			BitSet bitset = new BitSet(len);
			double[] x = pointTable.x;
			double[] y = pointTable.y;
			double[] vx = cvx;
			double[] vy = cvy;
			for (int i = 0; i < len; i++) {
				if(PolygonUtil.wn_PnPolyDirectV2(x[i], y[i], vx, vy) != 0) {
					bitset.set(i);
				}
			}
			return bitset;				
		}
	}

	public static FilterByPolygonFunc getFilterByPolygonFunc(Point2d[] points) {
		int plen = points.length;
		double[] vx = new double[plen];
		double[] vy = new double[plen];
		for (int i = 0; i < plen; i++) {
			vx[i] = points[i].x;
			vy[i] = points[i].y;
		}
		return getFilterByPolygonFunc(vx, vy);
	}
	
	public static FilterByPolygonFunc getFilterByPolygonFunc(double[] vx, double[] vy) {
		return new FilterByPolygonFunc(vx, vy);
	}
}
