package pointdb.process;

@Tag("point")
@Description("Density of LiDAR points (points per m² in bounding box) [bbox based]")
class Fun_point_density extends ProcessingFun {
	@Override
	public double process(DataProvider2 provider) {
		return provider.get_bboxPoints().size() / provider.bbox_rect.getArea();
	}
}