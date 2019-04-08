package pointdb.process;

import pointdb.processing.geopoint.RasterSubGrid;

class Fun_vegetation_coverage {

	private static double get_vegetation_coverage(DataProvider2 provider, double min) {
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

	@Description("vegetation coverage in 1 meter height (based on CHM raster pixels)")
	public static class Fun_vegetation_coverage_01m extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_vegetation_coverage(provider, 1);
		}
	}

	@Description("vegetation coverage in 2 meter height (based on CHM raster pixels)")
	public static class Fun_vegetation_coverage_02m extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_vegetation_coverage(provider, 2);
		}
	}
	
	@Description("vegetation coverage in 5 meter height (based on CHM raster pixels)")
	public static class Fun_vegetation_coverage_05m extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_vegetation_coverage(provider, 5);
		}
	}

	@Description("vegetation coverage in 10 meter height (based on CHM raster pixels)")
	public static class Fun_vegetation_coverage_10m extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_vegetation_coverage(provider, 10);
		}
	}

}