package pointdb.process;

import pointdb.base.GeoPoint;

@Tag("point")
@Description("Count of LiDAR ground points (without non ground points like vegetation)")
class Fun_ground_point_count extends ProcessingFun {
	@Override
	public double process(DataProvider2 provider) {		
		if(provider.classified_ground) {
			int cnt = 0;
			for(GeoPoint p : provider.get_regionPoints()) {
				if(p.isGround()) {
					cnt++;
				}
			}
			return cnt;
		} else {				
			return Double.NaN; // not implemented for not ground classified pointclouds
		}
	}
}