package pointdb.process;

@Tag("dtm")
public class Fun_dtm_elevation {
	
	@Description("Lowest ground a.s.l. (based on DTM raster pixels)")
	static class Fun_dtm_elevation_min extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDTM().aggregateMin();
		}
	}
	
	@Description("Highest ground a.s.l. (based on DTM raster pixels)")
	static class Fun_dtm_elevation_max extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDTM().aggregateMax();
		}
	}
	
	@Description("Mean of ground a.s.l. (based on DTM raster pixels)")
	@Exculde("base class")
	static class Fun_dtm_elevation_mean extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDTM().aggregateMean();
		}
	}
	
	@Description("Standard deviation of ground a.s.l. (based on DTM raster pixels)")
	static class Fun_dtm_elevation_sd extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDTM().aggregateSD();
		}
	}

}
