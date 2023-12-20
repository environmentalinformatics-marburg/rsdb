package pointdb.process;

import pointdb.base.GeoPoint;

@Tag("point")
@Description("Density of LiDAR vegetation points (vegetation points per m² in bounding box, without non vegetation points like ground) [bbox based]")
class Fun_vegetation_point_density extends ProcessingFun {
	@Override
	public double process(DataProvider2 provider) {		
		if(provider.classified_vegetation) {
			int cnt = 0;
			for(GeoPoint p : provider.get_bboxPoints()) {
				if(p.isVegetaion()) {
					cnt++;
				}
			}
			return cnt / provider.bbox_rect.getArea();
		} else {				
			return Double.NaN; // not implemented for not vegetation classified pointclouds
		}
	}
}