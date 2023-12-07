package pointdb.process;

@Tag("chm")
public class Fun_chm_height {
	
	@Description("Lowest surface above ground (based on CHM raster pixels)")
	static class Fun_chm_height_min extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getCHM().aggregateMin();
		}
	}
	
	@Description("Highest surface above ground (based on CHM raster pixels)")
	static class Fun_chm_height_max extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getCHM().aggregateMax();
		}
	}
	
	@Description("Mean of surface above ground (based on CHM raster pixels)")
	static class Fun_chm_height_mean extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getCHM().aggregateMean();
		}
	}
	
	@Description("Standard deviation of surface above ground (based on CHM raster pixels)")
	static class Fun_chm_height_sd extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getCHM().aggregateSD();
		}
	}

}
