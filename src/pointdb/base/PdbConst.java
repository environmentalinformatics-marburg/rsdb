package pointdb.base;

public final class PdbConst {
	private PdbConst(){}
	
	//public final static int UTM_TILE_SIZE = 10; // old db
	/**
	 * Size of tiles in UTM (meter)
	 */
	public final static int UTM_TILE_SIZE = 20; // new db
	
	/**
	 * Factor to multiply local coordinates to get UTM (meter)
	 */
	public final static int LOCAL_SCALE_FACTOR = 1000;
	
	public final static double LOCAL_SCALE_FACTORd = LOCAL_SCALE_FACTOR;
	
	/**
	 * Size of tiles in local coordinates
	 */
	public final static int LOCAL_TILE_SIZE = UTM_TILE_SIZE*LOCAL_SCALE_FACTOR;
	
	public final static int LOCAL_TILE_SIZE_MINUS_ONE = LOCAL_TILE_SIZE - 1;
	
	/**
	 * enable some debug checking, logging, etc
	 */
	public static final boolean DEBUG = true;
	
	public static double localToUTM(int v) {
		return ((double)v)/LOCAL_SCALE_FACTOR;		
	}
	
	public static double utmmToDouble(long v) {
		return v/LOCAL_SCALE_FACTORd;
	}
	
	/**
	 * Align utm double value to tile. Only correct for positive values.
	 * @param v
	 * @return
	 */
	public static double utmd_alignToTile(double v) {
		return v - v%UTM_TILE_SIZE;
	}
	
	public static final int MIN_SIGNIFICANT_POINT_COUNT_PER_UTM = 2;
	
	public static final int MIN_SIGNIFICANT_TILE_POINT_COUNT = (MIN_SIGNIFICANT_POINT_COUNT_PER_UTM*UTM_TILE_SIZE)*(MIN_SIGNIFICANT_POINT_COUNT_PER_UTM*UTM_TILE_SIZE);
	
	//public int gMin_intensity = 4704;//kili
	//public int gMax_intensity = 29034;//kili
	//public static int gMin_intensity = 2;//kellerwald
	//public static int gMax_intensity = 228;//kellerwald
	public static int gMin_intensity = Integer.MIN_VALUE;//open
	public static int gMax_intensity = Integer.MAX_VALUE;//open
	
    //public int gMin_z = 560_359;//kili
	//public int gMax_z = 2159_0_27;//kili
	//public static int gMin_z = 178_135;//kellerwald
	//public static int gMax_z = 578_745;//kellerwald
	public static int gMin_z = Integer.MIN_VALUE;//open
	public static int gMax_z = Integer.MAX_VALUE;//open
	
	public static long to_utmm(double x) {
		return (long) (x*PdbConst.LOCAL_SCALE_FACTOR);
	}
	
	public static int to_utm(double x) {
		return (int) x;
	}
	
	public static double utmm_to_double(long x) {
		return ((double)x)/PdbConst.LOCAL_SCALE_FACTOR;
	}
}
