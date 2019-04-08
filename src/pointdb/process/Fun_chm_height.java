package pointdb.process;

@Tag("chm")
public class Fun_chm_height {
	
	@Description("lowest surface above ground (based on CHM raster pixels)")
	static class Fun_chm_height_min extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getCHM().aggregateMin();
		}
	}
	
	@Description("highest surface above ground (based on CHM raster pixels)")
	static class Fun_chm_height_max extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getCHM().aggregateMax();
		}
	}
	
	@Description("mean of surface above ground (based on CHM raster pixels)")
	static class Fun_chm_height_mean extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getCHM().aggregateMean();
		}
	}
	
	@Description("standard deviation of surface above ground (based on CHM raster pixels)")
	static class Fun_chm_height_sd extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getCHM().aggregateSD();
		}
	}

}
