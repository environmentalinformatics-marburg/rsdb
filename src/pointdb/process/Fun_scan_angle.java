package pointdb.process;

import pointdb.base.GeoPoint;
import util.collections.vec.Vec;

@Tag("scan_angle")
public class Fun_scan_angle {
	
	@Description("Highest angle of LiDAR laser pulses to horizon (lowest angle to perpendicular line in degrees)")
	static class Fun_scan_angle_min extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {		
			double min = Double.MAX_VALUE;
			for(GeoPoint p:provider.old.getExtremesNormalisedPoints()) {
				double a = Math.abs(p.scanAngleRank);
				if(a<min) {
					min = a;
				}
			}
			return min==Double.MAX_VALUE ? Double.NaN : min;
		}		
	}
	
	@Description("Most shallow angle of LiDAR laser pulses to horizon (highest angle to perpendicular line in degrees)")
	static class Fun_scan_angle_max extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {		
			double max = 0;
			for(GeoPoint p:provider.old.getExtremesNormalisedPoints()) {
				double a = Math.abs(p.scanAngleRank);
				if(max<a) {
					max = a;
				}
			}
			return max;
		}		
	}
	
	@Description("Mean angle of LiDAR laser pulses (angle to perpendicular line in degrees)")
	static class Fun_scan_angle_mean extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {		
			double sum = 0;
			Vec<GeoPoint> ps = provider.old.getExtremesNormalisedPoints();
			for(GeoPoint p:ps) {
				sum += Math.abs(p.scanAngleRank);
			}
			double n = ps.size();
			return n==0 ? Double.NaN : (sum/n);		
		}		
	}
	
	@Description("Standard deviation of angles of LiDAR laser pulses (angle to perpendicular line in degrees)")
	static class Fun_scan_angle_sd extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {		
			double sum = 0;
			double qsum = 0;
			Vec<GeoPoint> ps = provider.old.getExtremesNormalisedPoints();
			for(GeoPoint p:ps) {
				double a = Math.abs(p.scanAngleRank);
				sum += a;
				qsum += a*a;
			}
			double n = ps.size();
			return Math.sqrt( (n*qsum - sum*sum) / (n*(n-1)) );		
			
		}		
	}
}
