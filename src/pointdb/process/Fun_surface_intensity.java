package pointdb.process;

import pointdb.base.GeoPoint;
import util.collections.vec.Vec;

public class Fun_surface_intensity {

	@Description("Lowest intensity of first return point of LiDAR laser pulses")
	static class Fun_surface_intensity_min extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double min = Double.MAX_VALUE;
			for(GeoPoint p:provider.old.getExtremesNormalisedPoints()) {
				double a = p.intensity;
				if(p.returnNumber==1 && a!=0) {
					if(a<min) {
						min = a;
					}
				}
			}
			return min==Double.MAX_VALUE ? Double.NaN : min;
		}		
	}

	@Description("Highest intensity of first return point of LiDAR laser pulses")
	static class Fun_surface_intensity_max extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double max = -Double.MAX_VALUE;
			for(GeoPoint p:provider.old.getExtremesNormalisedPoints()) {
				double a = p.intensity;
				if(p.returnNumber==1  && a!=0) {
					if(max<a) {
						max = a;
					}
				}
			}
			return max== -Double.MAX_VALUE ? Double.NaN : max;
		}		
	}

	@Description("Mean intensity of first return point of LiDAR laser pulses")
	static class Fun_surface_intensity_mean extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double cnt=0;
			double sum=0;
			for(GeoPoint p:provider.old.getExtremesNormalisedPoints()) {
				double a = p.intensity;
				if(p.returnNumber==1  && a!=0) {
					cnt++;
					sum += p.intensity;
				}
			}		
			return cnt==0 ? Double.NaN : (sum/cnt);
		}		
	}

	@Description("Standard deviation of intensity of first return point of LiDAR laser pulses")
	static class Fun_surface_intensity_sd extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double sum = 0;
			double qsum = 0;
			Vec<GeoPoint> ps = provider.old.getExtremesNormalisedPoints();
			for(GeoPoint p:ps) {
				double a = p.intensity;
				if(p.returnNumber==1  && a!=0) {
					sum += a;
					qsum += a*a;
				}
			}
			double n = ps.size();
			return Math.sqrt( (n*qsum - sum*sum) / (n*(n-1)) );		
		}		
	}
}
