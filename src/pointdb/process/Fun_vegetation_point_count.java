package pointdb.process;

import pointdb.base.GeoPoint;

@Tag("point")
@Description("count of LiDAR vegetation points (without non vegetation points like ground)")
class Fun_vegetation_point_count extends ProcessingFun {
	@Override
	public double process(DataProvider2 provider) {		
		if(provider.classified_vegetation) {
			int cnt = 0;
			for(GeoPoint p : provider.get_regionPoints()) {
				if(p.isVegetaion()) {
					cnt++;
				}
			}
			return cnt;
		} else {				
			return Double.NaN; // not implemented for not vegetation classified pointclouds
		}
	}
}