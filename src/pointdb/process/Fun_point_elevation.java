package pointdb.process;

import pointdb.base.GeoPoint;
import util.collections.vec.Vec;

@Tag("point")
public class Fun_point_elevation {
	
	@Description("lowest point a.s.l.")
	static class Fun_point_elevation_min extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double min_z=11_000;
			for(GeoPoint p:provider.old.getExtremesNormalisedPoints()) {
				if(p.z<min_z) {
					min_z = p.z;
				}
			}
			return ((-1_000<min_z) && (min_z<10_000))?min_z:Double.NaN;
		}
	}
	
	@Description("highest point a.s.l.")
	static class Fun_point_elevation_max extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double max_z=0;
			for(GeoPoint p:provider.old.getExtremesNormalisedPoints()) {
				if(max_z<p.z) {
					max_z = p.z;
				}
			}
			return ((-1_000<max_z) && (max_z<10_000))?max_z:Double.NaN;
		}
	}
	
	@Description("mean of points a.s.l.")
	static class Fun_point_elevation_mean extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {		
			double sum = 0;
			Vec<GeoPoint> ps = provider.old.getExtremesNormalisedPoints();
			for(GeoPoint p:ps) {
				sum += p.z;
			}
			double n = ps.size();
			return n==0 ? Double.NaN : (sum/n);		
		}
	}
	
	@Description("standard deviation of points a.s.l.")
	static class Fun_point_elevation_sd extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {		
			double sum = 0;
			double qsum = 0;
			Vec<GeoPoint> ps = provider.old.getExtremesNormalisedPoints();
			for(GeoPoint p:ps) {
				double z = p.z;
				sum += z;
				qsum += z*z;
			}
			double n = ps.size();
			return Math.sqrt( (n*qsum - sum*sum) / (n*(n-1)) );		
		}
	}
}
