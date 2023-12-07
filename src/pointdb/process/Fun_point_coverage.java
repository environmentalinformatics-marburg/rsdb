package pointdb.process;

import pointdb.base.GeoPoint;
import pointdb.base.Rect;
import util.collections.vec.Vec;

@Tag("point")
@Description("Ratio of area that is covered by LiDAR points (based on bounding box with aligned 1x1 meter grid, if no point is in one grid cell, cell is not covered)")
class Fun_point_coverage extends ProcessingFun {
	
	@Override
	public double process(DataProvider2 provider) {
		Rect bbox = provider.bbox_rect;
		Vec<GeoPoint> points = provider.get_bordered_bboxPoints();
		
		int xmin = bbox.getInteger_UTM_min_x();
		int xmax = bbox.getInteger_UTM_max_x();
		int xlen = xmax - xmin + 1;
		int ymin = bbox.getInteger_UTM_min_y();
		int ymax = bbox.getInteger_UTM_max_y();
		int ylen = ymax - ymin + 1;
		boolean[][] mask = new boolean[ylen][xlen];
		for(GeoPoint p:points) {
			int x = (int) p.x;
			int y = (int) p.y;
			if(xmin <= x && x <= xmax && ymin <= y && y <= ymax) {
				mask[y - ymin][x - xmin] = true;
			}
		}
		long cnt = 0;
		for(int y=0; y<ylen; y++) {
			boolean[] ymask = mask[y];
			for(int x=0; x<xlen; x++) {
				if(ymask[x]) {
					cnt++;
				}
			}
		}		
		return ((double) cnt) / (((double) xlen) * ((double) ylen));
		
	}
	
	/*@Override
	public double process(DataProvider2 provider) {
		Rect rect = provider.old.transformed_rect;
		int min_x = rect.getInteger_UTM_min_x();
		int min_y = rect.getInteger_UTM_min_y();
		int max_x = rect.getInteger_UTM_max_x();
		int max_y = rect.getInteger_UTM_max_y();
		int[][] counter = new int[max_y-min_y+1][max_x-min_x+1];
		for(GeoPoint p:provider.old.getTransformedPoints()) {
			int x = (int) p.x;
			int y = (int) p.y;
			counter[y-min_y][x-min_x]++;
		}
		int xBegin = rect.utmm_min_x%PdbConst.LOCAL_SCALE_FACTOR==0 ? 0 : 1;
		int yBegin = rect.utmm_min_y%PdbConst.LOCAL_SCALE_FACTOR==0 ? 0 : 1;
		int xEnd = (rect.utmm_max_x%PdbConst.LOCAL_SCALE_FACTOR==PdbConst.LOCAL_SCALE_FACTOR-1 ? max_x : max_x-1) - min_x;
		int yEnd = (rect.utmm_max_y%PdbConst.LOCAL_SCALE_FACTOR==PdbConst.LOCAL_SCALE_FACTOR-1 ? max_y : max_y-1) - min_y;

		int c = 0;
		for (int y = yBegin; y <= yEnd; y++) {
			int[] row = counter[y];
			for (int x = xBegin; x <= xEnd; x++) {
				if(row[x]>0) {
					c++;
				}
			}
		}
		return ((double) c)/( (xEnd-xBegin+1) * (yEnd-yBegin+1) );
	}*/
}