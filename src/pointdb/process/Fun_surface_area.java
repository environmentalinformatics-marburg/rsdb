package pointdb.process;

import pointdb.processing.geopoint.RasterSubGrid;

class Fun_surface_area {
	
	@Description("Surface area of DTM raster pixels [bbox based]")
	static class Fun_dtm_surface_area extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			RasterSubGrid r = provider.getDTM().toSurfaceArea();
			return r.sum(); 
		}	
	}
	
	@Description("Surface area of DSM raster pixels [bbox based]")
	static class Fun_dsm_surface_area extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			RasterSubGrid r = provider.getDSM().toSurfaceArea();
			return r.sum(); 
		}	
	}
	
	@Description("Surface area of CHM raster pixels [bbox based]")
	static class Fun_chm_surface_area extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			RasterSubGrid r = provider.getCHM().toSurfaceArea();
			return r.sum(); 
		}	
	}
	
	@Description("Surface area ratio of DTM raster pixels [bbox based]")
	static class Fun_dtm_surface_ratio extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			RasterSubGrid r = provider.getDTM().toSurfaceArea();
			return r.sum() / r.count(); 
		}	
	}
	
	@Description("Surface area ratio of DSM raster pixels [bbox based]")
	static class Fun_dsm_surface_ratio extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			RasterSubGrid r = provider.getDSM().toSurfaceArea();
			return r.sum() / r.count(); 
		}	
	}
	
	@Description("Surface area ratio of CHM raster pixels [bbox based]")
	static class Fun_chm_surface_ratio extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			RasterSubGrid r = provider.getCHM().toSurfaceArea();
			return r.sum() / r.count(); 
		}	
	}
	
}