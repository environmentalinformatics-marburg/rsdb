package pointdb.process;

@Tag("forest_structure")
public class Fun_forest_structure {
	
	@Description("Mean top-of-canopy height (redirects to chm_height_mean) (based on CHM raster pixels)")
	public static class Fun_TCH extends Fun_chm_height.Fun_chm_height_mean {
		public static final Fun_TCH  DEFAULT = new Fun_TCH ();
	}
	
	@Description("Aboveground biomass carbon (6.85 * TCH ^ 0.952) (based on CHM raster pixels) (Stephan Getzin et al.)")
	public static class Fun_AGB_carbon extends ProcessingFun {
		public static final Fun_AGB_carbon  DEFAULT = new Fun_AGB_carbon();
		@Override
		public double process(DataProvider2 provider) {			
			double tch = Fun_TCH.DEFAULT.process(provider);	
			//double tch = Fun_BE.Fun_BE_H_MEAN.DEFAULT.process(provider);
			return 6.85 * Math.pow(tch, 0.952);
		}
	}
	
	@Description("Aboveground biomass (6.85 * TCH ^ 0.952) / 0.48 (based on AGB_carbon) (Stephan Getzin, et al.)")
	public static class Fun_AGB extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {			
			double agb_carbon = Fun_AGB_carbon.DEFAULT.process(provider);	
			return agb_carbon / 0.48;
		}
	}

}
