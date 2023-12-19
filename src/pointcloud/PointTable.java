package pointcloud;

import java.util.function.Function;

import com.googlecode.javaewah.datastructure.BitSet;

import pointdb.base.Point2d;
import pointdb.base.PolygonUtil;
import pointdb.base.PolygonUtil.PolygonWithHoles;

public class PointTable {
	//

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

		public FilterByPolygonFunc(Point2d[] polygonPoints) {
			int len = polygonPoints.length;
			double[] vx = new double[len];
			double[] vy = new double[len];
			for (int i = 0; i < len; i++) {
				vx[i] = polygonPoints[i].x;
				vy[i] = polygonPoints[i].y;
			}
			this.cvx = vx;
			this.cvy = vy;
		}

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

	public static class FilterByPolygonWithHoleFunc implements Function<PointTable, BitSet> {
		private final double[] cvx;
		private final double[] cvy;
		private final double[] hvx;
		private final double[] hvy;

		public FilterByPolygonWithHoleFunc(Point2d[] polygonPoints, Point2d[] holePoints) {

			{
				int len = polygonPoints.length;
				double[] vx = new double[len];
				double[] vy = new double[len];
				for (int i = 0; i < len; i++) {
					vx[i] = polygonPoints[i].x;
					vy[i] = polygonPoints[i].y;
				}
				this.cvx = vx;
				this.cvy = vy;
			}

			{
				int len = holePoints.length;
				double[] hx = new double[len];
				double[] hy = new double[len];
				for (int i = 0; i < len; i++) {
					hx[i] = holePoints[i].x;
					hy[i] = holePoints[i].y;
				}
				this.hvx = hx;
				this.hvy = hy;
			}
		}

		@Override
		public BitSet apply(PointTable pointTable) {
			int len = pointTable.rows;
			BitSet bitset = new BitSet(len);
			double[] x = pointTable.x;
			double[] y = pointTable.y;
			double[] vx = cvx;
			double[] vy = cvy;
			double[] hx = hvx;
			double[] hy = hvy;
			for (int i = 0; i < len; i++) {
				if(PolygonUtil.wn_PnPolyDirectV2(x[i], y[i], vx, vy) != 0 && PolygonUtil.wn_PnPolyDirectV2(x[i], y[i], hx, hy) == 0) {
					bitset.set(i);
				}
			}
			return bitset;				
		}
	}

	public static class FilterByPolygonWithHolesFunc implements Function<PointTable, BitSet> {
		private final double[] cvx;
		private final double[] cvy;
		private double[][] hvx;
		private double[][] hvy;

		/**
		 * 
		 * @param polygonWithHoles  holes in polygonWithHoles need to be not null
		 */
		public FilterByPolygonWithHolesFunc(PolygonWithHoles polygonWithHoles) {
			Point2d[] polygonPoints = polygonWithHoles.polygon;
			int polygonPointsLen = polygonPoints.length;
			double[] vx = new double[polygonPointsLen];
			double[] vy = new double[polygonPointsLen];
			for (int i = 0; i < polygonPointsLen; i++) {
				vx[i] = polygonPoints[i].x;
				vy[i] = polygonPoints[i].y;
			}
			this.cvx = vx;
			this.cvy = vy;

			Point2d[][] holesPoints = polygonWithHoles.holes;
			int holesPointsLen = holesPoints.length;
			this.hvx = new double[holesPointsLen][];
			this.hvy = new double[holesPointsLen][];
			for (int ringIndex = 0; ringIndex < holesPointsLen; ringIndex++) {
				Point2d[] ringPoints = holesPoints[ringIndex];
				int len = ringPoints.length;
				double[] hx = new double[len];
				double[] hy = new double[len];
				for (int i = 0; i < len; i++) {
					hx[i] = ringPoints[i].x;
					hy[i] = ringPoints[i].y;
				}
				this.hvx[ringIndex] = hx;
				this.hvy[ringIndex] = hy;
			}
		}

		@Override
		public BitSet apply(PointTable pointTable) {
			int len = pointTable.rows;
			BitSet bitset = new BitSet(len);
			double[] x = pointTable.x;
			double[] y = pointTable.y;
			double[] vx = cvx;
			double[] vy = cvy;
			double[][] hx = hvx;
			double[][] hy = hvy;
			int holesPointsLen = hx.length;
			pointTablePointsLoop: for (int i = 0; i < len; i++) {
				if(PolygonUtil.wn_PnPolyDirectV2(x[i], y[i], vx, vy) != 0) {
					for (int ringIndex = 0; ringIndex < holesPointsLen; ringIndex++) {
						if(PolygonUtil.wn_PnPolyDirectV2(x[i], y[i], hx[ringIndex], hy[ringIndex]) != 0) {
							continue pointTablePointsLoop;
						}
					}
					bitset.set(i);
				}
			}
			return bitset;
		}
	}

	public static class FilterByPolygonsFunc implements Function<PointTable, BitSet> {
		private final double[][] cvx;
		private final double[][] cvy;

		public FilterByPolygonsFunc(Point2d[][] polygonsPoints) {
			int polgygonsLen = polygonsPoints.length;
			this.cvx = new double[polgygonsLen][];
			this.cvy = new double[polgygonsLen][];
			for (int polygonIndex = 0; polygonIndex < polgygonsLen; polygonIndex++) {
				Point2d[] polygonPoints = polygonsPoints[polygonIndex];
				int len = polygonPoints.length;
				double[] vx = new double[len];
				double[] vy = new double[len];
				for (int i = 0; i < len; i++) {
					vx[i] = polygonPoints[i].x;
					vy[i] = polygonPoints[i].y;
				}
				this.cvx[polygonIndex] = vx;
				this.cvy[polygonIndex] = vy;
			}	
		}

		public FilterByPolygonsFunc(double[][] vx, double[][] vy) {
			this.cvx = vx;
			this.cvy = vy;
		}

		@Override
		public BitSet apply(PointTable pointTable) {
			int len = pointTable.rows;
			BitSet bitset = new BitSet(len);
			double[] x = pointTable.x;
			double[] y = pointTable.y;
			double[][] mvx = cvx;
			double[][] mvy = cvy;
			int polgygonsLen = mvx.length;
			pointTablePointsLoop: for (int i = 0; i < len; i++) {				
				for (int polygonIndex = 0; polygonIndex < polgygonsLen; polygonIndex++) {
					if(PolygonUtil.wn_PnPolyDirectV2(x[i], y[i], mvx[polygonIndex], mvy[polygonIndex]) != 0) {
						bitset.set(i);
						continue pointTablePointsLoop;
					}
				}
			}
			return bitset;				
		}
	}

	public static class FilterByPolygonsWithHoleFunc implements Function<PointTable, BitSet> {
		private final double[][] cvx;
		private final double[][] cvy;
		private final double[][] hvx;
		private final double[][] hvy;

		/**
		 * 
		 * @param polygonsPoints not null
		 * @param polygonsHolePoints not null, but entry null if no hole
		 */
		public FilterByPolygonsWithHoleFunc(Point2d[][] polygonsPoints, Point2d[][] polygonsHolePoints) {

			{
				int polgygonsLen = polygonsPoints.length;
				this.cvx = new double[polgygonsLen][];
				this.cvy = new double[polgygonsLen][];
				for (int polygonIndex = 0; polygonIndex < polgygonsLen; polygonIndex++) {
					Point2d[] polygonPoints = polygonsPoints[polygonIndex];
					int len = polygonPoints.length;
					double[] vx = new double[len];
					double[] vy = new double[len];
					for (int i = 0; i < len; i++) {
						vx[i] = polygonPoints[i].x;
						vy[i] = polygonPoints[i].y;
					}
					this.cvx[polygonIndex] = vx;
					this.cvy[polygonIndex] = vy;
				}
			}

			{
				int polygonsHolePointsLen = polygonsHolePoints.length;
				this.hvx = new double[polygonsHolePointsLen][];
				this.hvy = new double[polygonsHolePointsLen][];
				for (int polygonHoleIndex = 0; polygonHoleIndex < polygonsHolePointsLen; polygonHoleIndex++) {
					Point2d[] polygonHolePoints = polygonsPoints[polygonHoleIndex];
					if(polygonHolePoints != null) {
						int len = polygonHolePoints.length;
						double[] hx = new double[len];
						double[] hy = new double[len];
						for (int i = 0; i < len; i++) {
							hx[i] = polygonHolePoints[i].x;
							hy[i] = polygonHolePoints[i].y;
						}
						this.hvx[polygonHoleIndex] = hx;
						this.hvy[polygonHoleIndex] = hy;
					}
				}
			}
		}

		@Override
		public BitSet apply(PointTable pointTable) {
			int len = pointTable.rows;
			BitSet bitset = new BitSet(len);
			double[] x = pointTable.x;
			double[] y = pointTable.y;
			double[][] mvx = cvx;
			double[][] mvy = cvy;
			int polgygonsLen = mvx.length;
			double[][] mhx = hvx;
			double[][] mhy = hvy;
			pointTablePointsLoop: for (int i = 0; i < len; i++) {				
				for (int polygonIndex = 0; polygonIndex < polgygonsLen; polygonIndex++) {
					if(PolygonUtil.wn_PnPolyDirectV2(x[i], y[i], mvx[polygonIndex], mvy[polygonIndex]) != 0 
							&& (mhx == null || PolygonUtil.wn_PnPolyDirectV2(x[i], y[i], mhx[polygonIndex], mhy[polygonIndex]) == 0)) {
						bitset.set(i);
						continue pointTablePointsLoop;
					}
				}
			}
			return bitset;				
		}
	}

	public static class FilterByPolygonsWithHolesFunc implements Function<PointTable, BitSet> {
		private final double[][] cvx;
		private final double[][] cvy;
		private final double[][][] hvx;
		private final double[][][] hvy;

		public FilterByPolygonsWithHolesFunc(PolygonWithHoles[] polygonsWithHoles) {
			int polygonsWithHolesLen = polygonsWithHoles.length;

			this.cvx = new double[polygonsWithHolesLen][];
			this.cvy = new double[polygonsWithHolesLen][];			
			for (int polygonIndex = 0; polygonIndex < polygonsWithHolesLen; polygonIndex++) {
				Point2d[] polygonPoints = polygonsWithHoles[polygonIndex].polygon;
				int polygonPointsLen = polygonPoints.length;
				double[] vx = new double[polygonPointsLen];
				double[] vy = new double[polygonPointsLen];
				for (int i = 0; i < polygonPointsLen; i++) {
					vx[i] = polygonPoints[i].x;
					vy[i] = polygonPoints[i].y;
				}
				this.cvx[polygonIndex] = vx;
				this.cvy[polygonIndex] = vy;
			}

			this.hvx = new double[polygonsWithHolesLen][][];
			this.hvy = new double[polygonsWithHolesLen][][];
			for (int polygonIndex = 0; polygonIndex < polygonsWithHolesLen; polygonIndex++) {
				Point2d[][] holesPoints = polygonsWithHoles[polygonIndex].holes;
				if(holesPoints != null) {
					int holesPointsLen = holesPoints.length;
					double[][] phx = new double[holesPointsLen][];
					double[][] phy = new double[holesPointsLen][];
					for (int ringIndex = 0; ringIndex < holesPointsLen; ringIndex++) {
						Point2d[] ringPoints = holesPoints[ringIndex];
						int len = ringPoints.length;
						double[] hx = new double[len];
						double[] hy = new double[len];
						for (int i = 0; i < len; i++) {
							hx[i] = ringPoints[i].x;
							hy[i] = ringPoints[i].y;
						}
						phx[ringIndex] = hx;
						phy[ringIndex] = hy;
					}
					hvx[polygonIndex] = phx;
					hvy[polygonIndex] = phy;
				}
			}
		}

		@Override
		public BitSet apply(PointTable pointTable) {
			int len = pointTable.rows;
			BitSet bitset = new BitSet(len);
			double[] x = pointTable.x;
			double[] y = pointTable.y;
			double[][] pvx = cvx;
			double[][] pvy = cvy;
			double[][][] phx = hvx;
			double[][][] phy = hvy;
			int polygonsWithHolesLen = pvx.length;
			pointTablePointsLoop: for (int i = 0; i < len; i++) {
				polygonsLoop: for (int polygonIndex = 0; polygonIndex < polygonsWithHolesLen; polygonIndex++) {
					double[] vx = pvx[polygonIndex];
					double[] vy = pvy[polygonIndex];
					if(PolygonUtil.wn_PnPolyDirectV2(x[i], y[i], vx, vy) != 0) {
						double[][] hx = phx[polygonIndex];
						double[][] hy = phy[polygonIndex];
						if(hx != null) {
							int holesPointsLen = hx.length;
							for (int ringIndex = 0; ringIndex < holesPointsLen; ringIndex++) {
								if(PolygonUtil.wn_PnPolyDirectV2(x[i], y[i], hx[ringIndex], hy[ringIndex]) != 0) {
									continue polygonsLoop;
								}
							}
							bitset.set(i);
							continue pointTablePointsLoop;
						}
					}
				}
			}
			return bitset;			
		}
	}

	public static FilterByPolygonFunc getFilterByPolygonFunc(Point2d[] polygon) {
		return new FilterByPolygonFunc(polygon);
	}

	/**
	 * polygons without holes
	 * @param polygons
	 * @return
	 */
	public static FilterByPolygonsFunc getFilterByPolygonsFunc(Point2d[][] polygons) {
		return new FilterByPolygonsFunc(polygons);
	}

	public static Function<PointTable, BitSet> getFilterByPolygonWithHolesFunc(PolygonWithHoles polygonWithHoles) {
		if(polygonWithHoles.hasNoHoles()) {
			return getFilterByPolygonFunc(polygonWithHoles.polygon);
		} else if(polygonWithHoles.holes.length == 1) { 
			return new FilterByPolygonWithHoleFunc(polygonWithHoles.polygon, polygonWithHoles.holes[0]);
		} else {
			return new FilterByPolygonWithHolesFunc(polygonWithHoles);
		}
	}

	public static Function<PointTable, BitSet> getFilterByPolygonsWithHolesFunc(PolygonWithHoles[]  polygonsWithHoles) {
		if(polygonsWithHoles.length == 1) {
			return getFilterByPolygonWithHolesFunc(polygonsWithHoles[0]);
		} else {
			int maxHoles = 0;
			for(PolygonWithHoles polygonWithHoles : polygonsWithHoles) {
				if(polygonWithHoles.hasHoles()) {
					if(maxHoles < polygonWithHoles.holes.length) {
						maxHoles = polygonWithHoles.holes.length;
					}
				}
			}
			if(maxHoles == 0) {
				Point2d[][] polygons = new Point2d[polygonsWithHoles.length][];
				for (int i = 0; i < polygonsWithHoles.length; i++) {
					polygons[i] = polygonsWithHoles[i].polygon;
					if(polygonsWithHoles[i].holes != null) {
						throw new RuntimeException("hole error");
					}
				}
				return getFilterByPolygonsFunc(polygons);
			} else if(maxHoles == 1) {
				Point2d[][] polygons = new Point2d[polygonsWithHoles.length][];
				Point2d[][] polygonsHole = new Point2d[polygonsWithHoles.length][];
				for (int i = 0; i < polygonsWithHoles.length; i++) {
					polygons[i] = polygonsWithHoles[i].polygon;
					if(polygonsWithHoles[i].hasHoles()) {
						if(polygonsWithHoles[i].holes.length != 1) {
							throw new RuntimeException("hole error");
						}
						polygonsHole[i] = polygonsWithHoles[i].holes[0];
					}
				}
				return new FilterByPolygonsWithHoleFunc(polygons, polygonsHole);			
			} else {
				return new FilterByPolygonsWithHolesFunc(polygonsWithHoles);
			}
		}
	}

	public double[] get_x() {
		return x;
	}
	public double[] get_y() {
		return y;
	}
	public double[] get_z() {
		return z;
	}

	public char[] get_intensity() {
		return intensity;
	}

	public byte[] get_returnNumber() {
		return returnNumber;
	}

	public byte[] get_returns() {
		return returns;
	}

	public java.util.BitSet get_scanDirectionFlag() {
		return scanDirectionFlag;
	}

	public java.util.BitSet get_edgeOfFlightLine() {
		return edgeOfFlightLine;
	}

	public byte[] get_classification() {
		return classification;
	}

	public byte[] get_scanAngleRank() {
		return scanAngleRank;
	}

	public long[] get_gpsTime() { // Uint64
		return gpsTime;
	}

	public char[] get_red() {
		return red;
	}

	public char[] get_green() {
		return green;
	}

	public char[] get_blue() {
		return blue;
	}
}
