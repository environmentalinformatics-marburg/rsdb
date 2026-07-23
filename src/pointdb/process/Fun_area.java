package pointdb.process;

@Tag("geometry")
@Description("Area of subset (m²) (polygon independent of contained data)")
class Fun_area extends ProcessingFun {
	@Override
	public double process(DataProvider2 provider) {
		return provider.region.getArea();
	}
}