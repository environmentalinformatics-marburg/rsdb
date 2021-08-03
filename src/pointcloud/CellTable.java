package pointcloud;

import java.util.Arrays;

import com.googlecode.javaewah.datastructure.BitSet;

public class CellTable {

	/**
	 * rows denotes valid rows, higher rows are not valid. So x.length may be higher than rows.
	 */
	public int rows;
	public int cx;
	public int cy;
	public int cz;

	public int[] x;
	public int[] y;
	public int[] z;

	public char[] intensity; // Uint16
	public byte[] returnNumber; // Uint8
	public byte[] returns; // Uint8
	public java.util.BitSet scanDirectionFlag;
	public java.util.BitSet edgeOfFlightLine;
	public byte[] classification; // Uint8
	public byte[] scanAngleRank; // Int8
	public long[] gpsTime; // Uint64
	public char[] red;
	public char[] green;
	public char[] blue;

	@FunctionalInterface
	public static interface FilterFunc {
		BitSet apply(CellTable cellTable);
	}

	@FunctionalInterface
	public static interface ChainedFilterFunc {
		BitSet apply(CellTable cellTable, BitSet bitset);

		public static ChainedFilterFuncAnd and(FilterFunc filterFunc) {
			return filterFunc == null ? null : new ChainedFilterFuncAnd(filterFunc);
		}

		public static ChainedFilterFunc and(ChainedFilterFunc chainedFilterFunc, FilterFunc filterFunc) {
			return chainedFilterFunc == null ? and(filterFunc) : (filterFunc == null ? chainedFilterFunc : new ChainedFilterFuncAnd2(chainedFilterFunc, filterFunc));
		}
	}

	public static class ChainedFilterFuncAnd implements ChainedFilterFunc {
		private final FilterFunc filterFunc;

		public ChainedFilterFuncAnd(FilterFunc filterFunc) {
			this.filterFunc = filterFunc;
		}

		@Override
		public BitSet apply(CellTable cellTable, BitSet bitset) {			
			BitSet resultBitset = filterFunc.apply(cellTable);
			if(bitset != null) {
				resultBitset.and(bitset);
			}
			return resultBitset;
		}		
	}

	public static class ChainedFilterFuncAnd2 implements ChainedFilterFunc {
		private final ChainedFilterFunc chainedFilterFunc;
		private final FilterFunc filterFunc;

		public ChainedFilterFuncAnd2(ChainedFilterFunc chainedFilterFunc, FilterFunc filterFunc) {
			this.chainedFilterFunc = chainedFilterFunc;
			this.filterFunc = filterFunc;
		}

		@Override
		public BitSet apply(CellTable cellTable, BitSet bitset) {
			BitSet resultBitset = chainedFilterFunc.apply(cellTable, bitset);
			BitSet classFilter = filterFunc.apply(cellTable);
			resultBitset.and(classFilter);
			return resultBitset;
		}		
	}

	public CellTable(int cx, int cy, int cz, int rows) {
		this.cx = cx;
		this.cy = cy;
		this.cz = cz;
		this.rows = rows;
	}

	public CellTable(int cx, int cy, int cz, int rows, int[] x, int[] y, int[] z) {
		this.cx = cx;
		this.cy = cy;
		this.cz = cz;
		this.rows = rows;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static CellTable merge(CellTable a, CellTable b) {
		int alen = a.rows;
		int blen = b.rows;
		int clen = alen + blen;

		int[] x = Arrays.copyOf(a.x, clen);
		System.arraycopy(b.x, 0, x, alen, blen);

		int[] y = Arrays.copyOf(a.y, clen);
		System.arraycopy(b.y, 0, y, alen, blen);

		int[] z = Arrays.copyOf(a.z, clen);
		System.arraycopy(b.z, 0, z, alen, blen);

		CellTable c = new CellTable(a.cx, a.cy, a.cz, clen, x, y, z);

		if(a.intensity != null || b.intensity != null) {
			c.intensity = a.intensity != null ? Arrays.copyOf(a.intensity, clen) : new char[clen];
			if(b.intensity != null) {
				System.arraycopy(b.intensity, 0, c.intensity, alen, blen);
			}
		}

		if(a.returnNumber != null || b.returnNumber != null) {
			c.returnNumber = a.returnNumber != null ? Arrays.copyOf(a.returnNumber, clen) : new byte[clen];
			if(b.returnNumber != null) {
				System.arraycopy(b.returnNumber, 0, c.returnNumber, alen, blen);
			}
		}

		if(a.returns != null || b.returns != null) {
			c.returns = a.returns != null ? Arrays.copyOf(a.returns, clen) : new byte[clen];
			if(b.returns != null) {
				System.arraycopy(b.returns, 0, c.returns, alen, blen);
			}
		}

		if(a.scanDirectionFlag != null || b.scanDirectionFlag != null) {
			c.scanDirectionFlag = a.scanDirectionFlag != null ? copyBitSet(a.scanDirectionFlag, alen, clen) : new java.util.BitSet(clen);
			if(b.scanDirectionFlag != null) {
				insertBitSet(b.scanDirectionFlag, blen, c.scanDirectionFlag, alen);
			}
		}

		if(a.edgeOfFlightLine != null || b.edgeOfFlightLine != null) {
			c.edgeOfFlightLine = a.edgeOfFlightLine != null ? copyBitSet(a.edgeOfFlightLine, alen, clen) : new java.util.BitSet(clen);
			if(b.edgeOfFlightLine != null) {
				insertBitSet(b.edgeOfFlightLine, blen, c.edgeOfFlightLine, alen);
			}
		}

		if(a.classification != null || b.classification != null) {
			c.classification = a.classification != null ? Arrays.copyOf(a.classification, clen) : new byte[clen];
			if(b.classification != null) {
				System.arraycopy(b.classification, 0, c.classification, alen, blen);
			}
		}

		if(a.scanAngleRank != null || b.scanAngleRank != null) {
			c.scanAngleRank = a.scanAngleRank != null ? Arrays.copyOf(a.scanAngleRank, clen) : new byte[clen];
			if(b.scanAngleRank != null) {
				System.arraycopy(b.scanAngleRank, 0, c.scanAngleRank, alen, blen);
			}
		}

		if(a.gpsTime != null || b.gpsTime != null) {
			c.gpsTime = a.gpsTime != null ? Arrays.copyOf(a.gpsTime, clen) : new long[clen];
			if(b.gpsTime != null) {
				System.arraycopy(b.gpsTime, 0, c.gpsTime, alen, blen);
			}
		}

		if(a.red != null || b.red != null) {
			c.red = a.red != null ? Arrays.copyOf(a.red, clen) : new char[clen];
			if(b.red != null) {
				System.arraycopy(b.red, 0, c.red, alen, blen);
			}
		}

		if(a.green != null || b.green != null) {
			c.green = a.green != null ? Arrays.copyOf(a.green, clen) : new char[clen];
			if(b.green != null) {
				System.arraycopy(b.green, 0, c.green, alen, blen);
			}
		}

		if(a.blue != null || b.blue != null) {
			c.blue = a.blue != null ? Arrays.copyOf(a.blue, clen) : new char[clen];
			if(b.blue != null) {
				System.arraycopy(b.blue, 0, c.blue, alen, blen);
			}
		}

		return c;
	}

	@Override
	public String toString() {
		return "celltable[" + rows + "]";
	}

	private static java.util.BitSet copyBitSet(java.util.BitSet bitSet, int len, int newLen) {		
		java.util.BitSet result = new java.util.BitSet(newLen);		
		for(int pos = bitSet.nextSetBit(0); pos != -1 && pos < len; pos = bitSet.nextSetBit(pos + 1)) {
			result.set(pos);
		}
		return result;
	}

	private static void insertBitSet(java.util.BitSet src, int srcLen, java.util.BitSet dst, int dstOff) {
		for(int pos = src.nextSetBit(0); pos != -1 && pos < srcLen; pos = src.nextSetBit(pos + 1)) {
			dst.set(dstOff + pos);
		}
	}

	public void cleanup() {		
		if(intensity != null && isZero(intensity, rows)) {
			intensity = null;
		}

		if(returnNumber != null && isZero(returnNumber, rows)) {
			returnNumber = null;
		}

		if(returns != null && isZero(returns, rows)) {
			returns = null;
		}

		if(scanDirectionFlag != null && scanDirectionFlag.isEmpty()) {
			scanDirectionFlag = null;
		}

		if(edgeOfFlightLine != null && edgeOfFlightLine.isEmpty()) {
			edgeOfFlightLine = null;
		}

		if(classification != null && isZero(classification, rows)) {
			classification = null;
		}

		if(scanAngleRank != null && isZero(scanAngleRank, rows)) {
			scanAngleRank = null;
		}

		if(gpsTime != null && isZero(gpsTime, rows)) {
			gpsTime = null;
		}

		if(red != null && isZero(red, rows)) {
			red = null;
		}

		if(green != null && isZero(green, rows)) {
			green = null;
		}

		if(blue != null && isZero(blue, rows)) {
			blue = null;
		}
	}

	private static boolean isZero(long[] data, int len) {
		for (int i = 0; i < len; i++) {
			if(data[i] != 0) {
				return false;
			}
		}
		return true;
	}

	private static boolean isZero(char[] data, int len) {
		for (int i = 0; i < len; i++) {
			if(data[i] != 0) {
				return false;
			}
		}
		return true;
	}

	private static boolean isZero(byte[] data, int len) {
		for (int i = 0; i < len; i++) {
			if(data[i] != 0) {
				return false;
			}
		}
		return true;
	}

	public AttributeSelector toSelector() {
		AttributeSelector s = new AttributeSelector();

		if(x != null) {
			s.x = true;
		}
		if(y != null) {
			s.y = true;
		}
		if(z != null) {
			s.z = true;
		}		
		if(intensity != null) {
			s.intensity = true;
		}
		if(returnNumber != null) {
			s.returnNumber = true;
		}
		if(returns != null) {
			s.returns = true;
		}
		if(scanDirectionFlag != null) {
			s.scanDirectionFlag = true;
		}
		if(edgeOfFlightLine != null) {
			s.edgeOfFlightLine = true;
		}
		if(classification != null) {
			s.classification = true;
		}
		if(scanAngleRank != null) {
			s.scanAngleRank = true;
		}
		if(gpsTime != null) {
			s.gpsTime = true;
		}
		if(red != null) {
			s.red = true;
		}
		if(green != null) {
			s.green = true;
		}
		if(blue != null) {
			s.blue = true;
		}

		return s;
	}

	public static FilterFunc filterFuncClassification(byte classification) {
		return cellTable -> cellTable.filterClassification(classification);
	}

	public static ChainedFilterFunc parseFilter(String filterText) {
		ChainedFilterFunc chainedFilterFunc = null;
		if(filterText != null) {
			String[] elementTexts = filterText.split(";");
			for(String elementText:elementTexts) {
				FilterFunc elementFilter = parseFilterElement(elementText);
				chainedFilterFunc = ChainedFilterFunc.and(chainedFilterFunc, elementFilter);
			}
		}
		return chainedFilterFunc;
	}

	public static FilterFunc parseFilterElement(String text) {
		if(text == null) {
			return null;
		}				
		text = text.trim();
		if(text.isEmpty()) {
			return null;
		}
		String cEQ = "classification=";

		if(text.startsWith(cEQ)) {
			String r = text.substring(cEQ.length()).trim();
			int classNr = Integer.parseInt(r);
			return filterFuncClassification((byte)classNr);
		} else {
			throw new RuntimeException("filter unknown: "+text);
		}
	}

	public static boolean isVegetaion(byte classification) {
		return classification == 3 // low vegetation
				|| classification == 4  // medium vegetation
				|| classification == 5  // high vegetation 
				|| classification == 13  // wire - guard
				//|| classification == 1 // unassigned
				|| classification == 20; // (non standard) vegetation
	}

	public static boolean isGround(byte classification) {
		return classification == 2 // ground
				|| classification == 8; // model key/reserved
	}

	/**
	 * Point is classified as some valid entity, or not classified
	 * @return
	 */
	public boolean isEntity(byte classification) {
		return isGround(classification) 
				|| isVegetaion(classification) 
				|| classification == 6 // building
				|| classification == 9 // water
				|| classification == 0 // not classified
				|| classification == 1; // unassigned
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
}
