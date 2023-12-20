package pointdb.process;

import pointdb.base.GeoPoint;
import pointdb.processing.geopoint.RasterSubGrid;
import util.collections.vec.Vec;

class Fun_vegetation_coverage {

	private static double get_vegetation_coverage_CHM(DataProvider2 provider, double min) {
		RasterSubGrid chm = provider.getCHM();
		int total_count = 0;
		int vegetation_count = 0;
		for (int y = chm.start_y; y < chm.border_y; y++) {
			double[] row = chm.data[y];
			for (int x = chm.start_x; x < chm.border_x; x++) {
				double v = row[x];
				if (Double.isFinite(v)) {
					total_count++;
					if (min <= v) {
						vegetation_count++;
					}
				}
			}
		}
		return total_count == 0 ? Double.NaN : ((double) vegetation_count) / ((double) total_count);
	}

	private static double get_vegetation_coverage_points(DataProvider2 provider, double min) {
		Vec<GeoPoint> ps = provider.get_sortedRegionHeightPoints();
		int total_count = 0;
		int vegetation_count = 0;
		for(GeoPoint p:ps) {
			if(Double.isFinite(p.z)) {
				total_count++;
				if (min <= p.z) {
					vegetation_count++;
				}
			}
		}
		return total_count == 0 ? Double.NaN : ((double) vegetation_count) / ((double) total_count);
	}

	private static double get_vegetation_coverage_pulses(DataProvider2 provider, double min) {
		Vec<GeoPoint> ps = provider.get_sortedRegionHeightPoints();
		int total_count = 0;
		int vegetation_count = 0;
		for(GeoPoint p:ps) {
			if(Double.isFinite(p.z) && p.returnNumber == 1) {
				total_count++;
				if (min <= p.z) {
					vegetation_count++;
				}
			}
		}
		return total_count == 0 ? Double.NaN : ((double) vegetation_count) / ((double) total_count);
	}

	@Description("Vegetation coverage in 1 meter height (based on CHM raster pixels) [bbox based]")
	public static class Fun_vegetation_coverage_01m_CHM extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_vegetation_coverage_CHM(provider, 1);
		}
	}

	@Description("Vegetation coverage in 2 meter height (based on CHM raster pixels) [bbox based]")
	public static class Fun_vegetation_coverage_02m_CHM extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_vegetation_coverage_CHM(provider, 2);
		}
	}

	@Description("Vegetation coverage in 2 meter height (point based, all returns)")
	public static class Fun_vegetation_coverage_02m_points extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_vegetation_coverage_points(provider, 2);
		}
	}
	
	@Description("Vegetation coverage in 2 meter height (laser pulse based, all first return points)")
	public static class Fun_vegetation_coverage_02m_pulses extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_vegetation_coverage_pulses(provider, 2);
		}
	}

	@Description("Vegetation coverage in 5 meter height (based on CHM raster pixels) [bbox based]")
	public static class Fun_vegetation_coverage_05m_CHM extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_vegetation_coverage_CHM(provider, 5);
		}
	}

	@Description("Vegetation coverage in 10 meter height (based on CHM raster pixels) [bbox based]")
	public static class Fun_vegetation_coverage_10m_CHM extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_vegetation_coverage_CHM(provider, 10);
		}
	}

}