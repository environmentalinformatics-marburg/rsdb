package pointdb.process;

@Tag("dsm")
public class Fun_dsm_elevation {
	
	@Description("minimal surface a.s.l. (based on DSM raster pixels)")
	static class Fun_dsm_elevation_min extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDTM().aggregateMin();
		}
	}
	
	@Description("highest surface a.s.l. (based on DSM raster pixels)")
	static class Fun_dsm_elevation_max extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDSM().aggregateMax();
		}
	}
	
	@Description("mean surface a.s.l. (based on DSM raster pixels)")
	static class Fun_dsm_elevation_mean extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDSM().aggregateMean();
		}
	}
	
	@Description("standard deviation of surface a.s.l. (based on DSM raster pixels)")
	static class Fun_dsm_elevation_sd extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDSM().aggregateSD();
		}
	}

}
