package pointdb.processing.tilepoint;

import pointdb.base.Point;
import pointdb.processing.tilemeta.StatisticsCreator.Statistics;

class PointImageIntensityZ extends PointImageAbstract {

	public PointImageIntensityZ(long utmm_min_x, long utmm_min_y, int screen_width, int screen_height, int TILE_LOCAL_TO_SCREEN_DIV, ImageType imageType, Statistics tileMinMaxCalc) {
		super(utmm_min_x, utmm_min_y, screen_width, screen_height, TILE_LOCAL_TO_SCREEN_DIV, tileMinMaxCalc);
	}

	@Override
	public void nextPoint(Point point) {
		if(point.returnNumber!=1 || point.intensity==0) {
			return;
		}
		int x = tile_screen_x + point.x/TILE_LOCAL_TO_SCREEN_DIV;
		int y = tile_screen_y_flip - point.y/TILE_LOCAL_TO_SCREEN_DIV; // flip y
		
		int c_i = point.intensity;
		if(c_i!=0) {
			//c_i = (int) Math.round(255d*Math.pow((float)(c_i-intensity_min) / intensity_range, ginv_itensity));
			c_i = (int) ((c_i-intensity_min) * intensity_rangeInv255);
			if(c_i>255) {
				c_i=255;
			}
		}

		int c_z = point.z;		
		if(c_z!=0) {
			//c_z = (int) Math.round(255d*Math.pow((float)(c_z-z_min) / z_range, ginv_z));
			c_z = (int) ((c_z-z_min) * z_rangeInv255);
			if(c_z>255) {
				c_z=255;
			}
			if(c_z<0) {
				c_z=0;
			}
		}
		
		int rg = ((255-c_z)*c_i)/255;
		int b = (c_z*c_i)/255;

		imageBuffer[y*width+x] = (rg<<16) | (rg<<8) | b;
	}
}