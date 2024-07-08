package pointdb.process;

import java.util.HashMap;

import pointdb.base.GeoPoint;
import pointdb.indexfuncdsl.IndexFuncDSLParser.ValueContext;
import util.collections.vec.Vec;

@Tag("parameterized")
@Description("Penetration rate (pass through rate) at specific height layer. Parameters: height_lower, height_upper  (based on point height above ground)")
public class Fun_PR extends ParamProcessingFun {

	@Override
	public ProcessingFun instantiate(HashMap<String, ValueContext> paramMap) {
		return new Fun(this, paramMap);
	}

	public static class Fun extends ProcessingFun {

		private double height_upper = Double.POSITIVE_INFINITY;  // meter above ground
		private double height_lower = Double.NEGATIVE_INFINITY; // meter above ground

		public Fun(Fun_PR that, HashMap<String, ValueContext> paramMap) {
			super(that.getClass());
			ValueContext hl = paramMap.get("height_lower");
			if(hl != null) {
				height_lower = Double.parseDouble(hl.number().getText());
			}
			ValueContext hu = paramMap.get("height_upper");
			if(hu != null) {
				height_upper = Double.parseDouble(hu.number().getText());
			}
		}

		@Override
		public double process(DataProvider2 provider) {
			double cnt = 0;
			double reach = 0;
			Vec<GeoPoint> points = provider.get_sortedRegionHeightPoints();
			if(points.isEmpty()) {
				return Double.NaN;
			}

			for(GeoPoint p:points) {
				if(p.z <= height_upper) {
					reach++;
					if(height_lower < p.z) {
						cnt++;
					}
				}
			}				

			if(reach == 0) {
				return Double.NaN;
			}
			return 1d - cnt/reach;
		}
	}
}
