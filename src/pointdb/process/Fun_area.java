package pointdb.process;

@Tag("abstract")
@Description("(polygon) area of subset (m²) (independent of contained data)")
class Fun_area extends ProcessingFun {
	@Override
	public double process(DataProvider2 provider) {
		return provider.region.getArea();
	}
}