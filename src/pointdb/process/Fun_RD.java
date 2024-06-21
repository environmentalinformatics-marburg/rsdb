package pointdb.process;

import java.util.HashMap;

import pointdb.base.GeoPoint;
import pointdb.indexfuncdsl.IndexFuncDSLParser.ValueContext;
import util.collections.vec.Vec;

@Tag("parameterized")
@Description("[parameterized] Return density of points at specifiec height. With height_lower < point <= height_upper  (based on point height above ground)")
public class Fun_RD extends ParamProcessingFun {

	@Override
	public ProcessingFun instantiate(HashMap<String, ValueContext> paramMap) {
		return new Fun(this, paramMap);
	}

	public static class Fun extends ProcessingFun {

		private double height_upper = Double.POSITIVE_INFINITY;  // meter above ground
		private double height_lower = Double.NEGATIVE_INFINITY; // meter above ground

		public Fun(Fun_RD that, HashMap<String, ValueContext> paramMap) {
			super(that.getClass());
			height_lower = Double.parseDouble(paramMap.get("height_lower").number().getText());
			height_upper = Double.parseDouble(paramMap.get("height_upper").number().getText());
		}

		@Override
		public double process(DataProvider2 provider) {
			double cnt = 0;
			Vec<GeoPoint> points = provider.get_sortedRegionHeightPoints();
			if(points.isEmpty()) {
				return Double.NaN;
			}
			for(GeoPoint p:points) {				
				if(height_lower < p.z && p.z <= height_upper) {
					cnt++;
				}
			}
			return cnt/points.size();
		}
	}
}
