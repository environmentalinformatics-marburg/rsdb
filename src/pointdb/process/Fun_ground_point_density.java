package pointdb.process;

import pointdb.base.GeoPoint;

@Tag("point")
@Description("Density of LiDAR ground points (ground points per m² in bounding box, without non ground points like vegetation)")
class Fun_ground_point_density extends ProcessingFun {
	@Override
	public double process(DataProvider2 provider) {		
		if(provider.classified_ground) {
			int cnt = 0;
			for(GeoPoint p : provider.get_bboxPoints()) {
				if(p.isGround()) {
					cnt++;
				}
			}
			return cnt / provider.bbox_rect.getArea();
		} else {				
			return Double.NaN; // not implemented for not ground classified pointclouds
		}
	}
}