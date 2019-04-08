package pointdb.process;

@Tag("dtm")
public class Fun_dtm_aspect {
	//private static final Logger log = LogManager.getLogger();
	
	@Description("circular mean of aspect of ground a.s.l. without weighting slopes (based on DTM raster pixels) small scale direction")
	static class Fun_dtm_aspect_unweighted_mean extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDTM_aspect().aggregateCircularMean();
		}
	}
	
	@Description("circular mean aspect of ground a.s.l. with weighting slopes (based on DTM raster pixels) large scale direction")
	static class Fun_dtm_aspect_mean extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return provider.old.getDTM().aggregateAspectCircularMean();
		}
	}
	
	@Description("aspect of ground a.s.l. (based on DTM raster pixels bilinear regression)")
	@Exculde("base class")
	static class Fun_dtm_aspect_reg extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] reg = provider.old.getDTM().getRegressionParameters();			
			//log.info("TESTING reg "+reg[0]+" "+reg[1]+" "+reg[2]);
			double dx = reg[1];
			double dy = reg[2];
			return Math.atan2(dy, dx);
		}
	}
}
