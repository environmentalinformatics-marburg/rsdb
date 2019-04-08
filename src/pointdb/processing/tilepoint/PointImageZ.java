package pointdb.processing.tilepoint;

import pointdb.base.Point;
import pointdb.processing.tilemeta.StatisticsCreator.Statistics;
import util.image.MonoColor;

class PointImageZ extends PointImageAbstract {

	public PointImageZ(long utmm_min_x, long utmm_min_y, int screen_width, int screen_height, int TILE_LOCAL_TO_SCREEN_DIV, ImageType imageType, Statistics tileMinMaxCalc) {
		super(utmm_min_x, utmm_min_y, screen_width, screen_height, TILE_LOCAL_TO_SCREEN_DIV, tileMinMaxCalc);
	}

	@Override
	public void nextPoint(Point point) {
		if(point.returnNumber!=1 || point.intensity==0) {
			return;
		}
		int x = tile_screen_x + point.x/TILE_LOCAL_TO_SCREEN_DIV;
		int y = tile_screen_y_flip - point.y/TILE_LOCAL_TO_SCREEN_DIV; // flip y

		int c = point.z;		
		if(c!=0) {
			//imageBuffer[y*width+x] = MonoColor.getSafeRGBA(MonoColor.pow((c-z_min) * z_rangeInv, ginv_z));
			imageBuffer[y*width+x] = MonoColor.getSafeRGBA256((int) ((c-z_min) * z_rangeInv255));
		}
	}
}