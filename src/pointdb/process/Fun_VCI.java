package pointdb.process;

import pointdb.base.GeoPoint;
import util.collections.vec.Vec;

@Tag("point")
@Tag("forest_structure")
@Description("Vertical complexity Index")
class Fun_VCI extends ProcessingFun {
	@Override
	public double process(DataProvider2 provider) {
		Vec<GeoPoint> points = provider.get_sortedRegionHeightPoints();
		if(points.isEmpty()) {
			return Double.NaN;
		}
		int h_max = 100;
		int[] h_count = new int[h_max + 1];
		int count = 0;
		for(GeoPoint p:points) {
			int h = (int) p.z;
			if(h <= h_max) {
				count++;
				h_count[h < 0 ? 0 : h]++;
			}
		}
		if(count > 0) {
			double sum = 0;
			for(int c:h_count) {
				double p = (double) c / (double) count;
				double s = p * Math.log(p);
				sum += Double.isFinite(s) ? s : 0;
			}
			return -sum;
		} else {
			return Double.NaN;
		}
	}
}