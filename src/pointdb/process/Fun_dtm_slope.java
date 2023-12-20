package pointdb.process;

@Tag("dtm")
public class Fun_dtm_slope {
	//
	
	@Description("Lowest slope of ground a.s.l. (based on DTM raster pixels) [bbox based]")
	static class Fun_dtm_slope_min extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDTM_slope().aggregateMin();
		}
	}
	
	@Description("Lowest slope of ground a.s.l. (based on DTMraster pixels) [bbox based]")
	static class Fun_dtm_slope_max extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDTM_slope().aggregateMax();
		}
	}
	
	@Description("Mean slope of ground a.s.l. (based on DTM raster pixels)  [bbox based]")
	static class Fun_dtm_slope_mean extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDTM_slope().aggregateMean();
		}
	}
	
	@Description("Standard deviation of slope of ground a.s.l. (based on DTM raster pixels)  [bbox based]")
	static class Fun_dtm_slope_sd extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDTM_slope().aggregateSD();
		}
	}
	
	@Description("Slope of ground a.s.l. (based on DTM raster pixels bilinear regression)  [bbox based]")
	@Exculde("base class")
	static class Fun_dtm_slope_reg extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] reg = provider.old.getDTM().getRegressionParameters();			
			//Logger.info("TESTING reg "+reg[0]+" "+reg[1]+" "+reg[2]);
			double dx = reg[1];
			double dy = reg[2];
			return Math.atan(Math.sqrt(dx*dx+dy*dy));
		}
	}

}
