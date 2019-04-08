package pointdb.processing.tilepoint;

import pointdb.base.PdbConst;
import pointdb.base.Tile;
import pointdb.processing.tilemeta.StatisticsCreator.Statistics;

abstract class PointImageAbstract extends PointImage {
	//private static final float gamma_z = 1.2f;
	//private static final float gamma_z = 1.01f; //significant
	private static final float gamma_z = 1.0f;
	//private static final float gamma_itensity = 2.5f; //KiLi
	//private static final float gamma_itensity = 3.5f; //Kellerwald
	//private static final float gamma_itensity = 1.01f; //significant
	private static final float gamma_itensity = 1.0f;
	protected static final float ginv_z = 1f / gamma_z;
	protected static final float ginv_itensity = 1f / gamma_itensity;

	private final long utmm_min_x;
	private final long utmm_min_y;
	protected final int TILE_LOCAL_TO_SCREEN_DIV;

	protected final int intensity_min;
	protected final int intensity_range;
	protected double intensity_rangeInv;
	protected float intensity_rangeInv255;
	protected final int z_min;
	protected final int z_range;
	protected final double z_rangeInv;
	protected final float z_rangeInv255;

	protected int tile_screen_x;
	//private int tile_screen_y;
	protected int tile_screen_y_flip; // flip y

	public PointImageAbstract(long utmm_min_x, long utmm_min_y, int screen_width, int screen_height, int TILE_LOCAL_TO_SCREEN_DIV, Statistics tileMinMaxCalc) {
		super(screen_width, screen_height);
		this.utmm_min_x = utmm_min_x;
		this.utmm_min_y = utmm_min_y;
		this.TILE_LOCAL_TO_SCREEN_DIV = TILE_LOCAL_TO_SCREEN_DIV;

		int[] clipIntensity = clipRange(tileMinMaxCalc.tile_significant_intensity_avg_min, tileMinMaxCalc.tile_significant_intensity_avg_max, tileMinMaxCalc.intensity_min, tileMinMaxCalc.intensity_max, 1);
		int[] clipZ = clipRange(tileMinMaxCalc.tile_significant_z_avg_min, tileMinMaxCalc.tile_significant_z_avg_max, tileMinMaxCalc.local_z_min, tileMinMaxCalc.local_z_max, 1);
		this.intensity_min = clipIntensity[0];
		this.intensity_range = clipIntensity[1] - clipIntensity[0];
		this.intensity_rangeInv = 1d/intensity_range;
		this.intensity_rangeInv255 = (float) (255d/intensity_range);
		this.z_min = clipZ[0];
		this.z_range = clipZ[1] - clipZ[0];
		this.z_rangeInv = 1d/z_range;
		this.z_rangeInv255 = (float) (255d/z_range);

		clearImage();			
	}

	@Override
	public void nextTile(Tile tile) {
		long utmm_start_x = (((long)tile.meta.x)*PdbConst.LOCAL_SCALE_FACTOR) - utmm_min_x;
		long utmm_start_y = (((long)tile.meta.y)*PdbConst.LOCAL_SCALE_FACTOR) - utmm_min_y;		
		tile_screen_x = (int) (utmm_start_x/TILE_LOCAL_TO_SCREEN_DIV);
		//tile_screen_y = (int) (utmm_start_y/TILE_LOCAL_TO_SCREEN_DIV);
		tile_screen_y_flip = height-1 - (int) (utmm_start_y/TILE_LOCAL_TO_SCREEN_DIV); // flip y
	}

	private static int[] clipRange(int reliableMin, int reliableMax, int globalMin, int globalMax, int factor) {
		int reliableRange = reliableMax - reliableMin;
		int stretch = reliableRange*factor;
		int min = reliableMin - stretch;
		if(min>reliableMin) min = Integer.MIN_VALUE;
		if(min<globalMin) min = globalMin;
		int max = reliableMax + stretch;
		if(max<reliableMax) max = Integer.MAX_VALUE;
		if(max>globalMax) max = globalMax;
		return new int[]{min,max};
	}

}