package pointdb.process;

import pointdb.base.GeoPoint;
import util.collections.vec.Vec;

@Tag("pulse")
public class Fun_pulse {
	
	@Description("number of LiDAR laser pulses")
	public static class Fun_pulse_count extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.getPulseCount();
		}		
	}
	
	@Description("density of LiDAR laser pulses (pulses per m² in bounding box)")
	static class Fun_pulse_density extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.getPulseCount() / provider.bbox_rect.getArea();
		}
	}
	
	@Description("post spacing of LiDAR laser pulses (distance between pulses in m in bounding box)")
	static class Fun_pulse_spacing extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return Math.sqrt(provider.bbox_rect.getArea() / provider.getPulseCount());
		}
	}
	
	@Description("area that is represented by one LiDAR laser pulse (m² per pulse in bounding box) (not to be confused with laser beam extent)")
	static class Fun_pulse_area extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.bbox_rect.getArea() / provider.getPulseCount();
		}
	}
	
	@Description("lowest number of return points per LiDAR laser pulse")
	static class Fun_pulse_returns_min extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {		
			double min = Double.MAX_VALUE;
			for(GeoPoint p:provider.get_regionPoints()) {			
				if(p.returnNumber==1) {
					double r = p.returns;
					if(r<min) {
						min = r;
					}
				}
			}
			return min==Double.MAX_VALUE ? Double.NaN : min;
		}		
	}
	
	@Description("highest number of return points per LiDAR laser pulse")
	static class Fun_pulse_returns_max extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {		
			double max = -Double.MAX_VALUE;
			for(GeoPoint p:provider.get_regionPoints()) {			
				if(p.returnNumber == 1) {
					double r = p.returns;
					if(max<r) {
						max = r;
					}
				}
			}
			return max == -Double.MAX_VALUE ? Double.NaN : max;
		}		
	}
	
	@Description("mean of return points per LiDAR laser pulse")
	static class Fun_pulse_returns_mean extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {		
			double sum = 0;
			double cnt = 0;
			for(GeoPoint p:provider.get_regionPoints()) {
				if(p.returnNumber == 1) {
					sum += p.returns;
					cnt++;
				}
			}
			return sum/cnt;
		}		
	}
	
	@Description("standard deviation of return points per LiDAR laser pulse")
	static class Fun_pulse_returns_sd extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {			
			double sum = 0;
			double qsum = 0;
			Vec<GeoPoint> ps = provider.get_regionPoints();
			for(GeoPoint p:ps) {
				double a = p.returns;
				sum += a;
				qsum += a*a;
			}
			double n = ps.size();
			return Math.sqrt( (n*qsum - sum*sum) / (n*(n-1)) );		
			
		}		
	}
}
