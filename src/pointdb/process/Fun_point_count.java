package pointdb.process;

@Tag("point")
@Description("Number of LiDAR points")
class Fun_point_count extends ProcessingFun {	
	@Override
	public double process(DataProvider2 provider) {
		return provider.get_regionPoints().size();
	}		
}