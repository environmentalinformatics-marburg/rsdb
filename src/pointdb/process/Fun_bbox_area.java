package pointdb.process;

@Tag("abstract")
@Description("Bounding box area of subset (m²)  (independent of contained data) [bbox based]")
class Fun_bbox_area extends ProcessingFun {
	@Override
	public double process(DataProvider2 provider) {
		return provider.region.bbox.getArea();
	}
}