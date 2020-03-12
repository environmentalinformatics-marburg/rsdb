package pointdb.process;

import pointdb.processing.geopoint.RasterSubGrid;

class Fun_surface_area {
	
	@Description("surface area of DTM raster pixels")
	static class Fun_dtm_surface_area extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			RasterSubGrid r = provider.getDTM().toSurfaceArea();
			return r.sum(); 
		}	
	}
	
	@Description("surface area of DSM raster pixels")
	static class Fun_dsm_surface_area extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			RasterSubGrid r = provider.getDSM().toSurfaceArea();
			return r.sum(); 
		}	
	}
	
	@Description("surface area of CHM raster pixels")
	static class Fun_chm_surface_area extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			RasterSubGrid r = provider.getCHM().toSurfaceArea();
			return r.sum(); 
		}	
	}
	
	@Description("surface area ratio of DTM raster pixels")
	static class Fun_dtm_surface_ratio extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			RasterSubGrid r = provider.getDTM().toSurfaceArea();
			return r.sum() / r.count(); 
		}	
	}
	
	@Description("surface area ratio of DSM raster pixels")
	static class Fun_dsm_surface_ratio extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			RasterSubGrid r = provider.getDSM().toSurfaceArea();
			return r.sum() / r.count(); 
		}	
	}
	
	@Description("surface area ratio of CHM raster pixels")
	static class Fun_chm_surface_ratio extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			RasterSubGrid r = provider.getCHM().toSurfaceArea();
			return r.sum() / r.count(); 
		}	
	}
	
}