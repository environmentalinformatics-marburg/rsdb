package pointdb.process;

@Tag("dsm")
public class Fun_dsm_elevation {
	
	@Description("Minimal surface a.s.l. (based on DSM raster pixels) [bbox based]")
	static class Fun_dsm_elevation_min extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDTM().aggregateMin();
		}
	}
	
	@Description("Highest surface a.s.l. (based on DSM raster pixels) [bbox based]")
	static class Fun_dsm_elevation_max extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDSM().aggregateMax();
		}
	}
	
	@Description("Mean surface a.s.l. (based on DSM raster pixels) [bbox based]")
	static class Fun_dsm_elevation_mean extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDSM().aggregateMean();
		}
	}
	
	@Description("Standard deviation of surface a.s.l. (based on DSM raster pixels) [bbox based]")
	static class Fun_dsm_elevation_sd extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDSM().aggregateSD();
		}
	}

}
