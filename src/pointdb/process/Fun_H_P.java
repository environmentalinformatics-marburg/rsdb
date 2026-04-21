package pointdb.process;

import java.util.HashMap;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import pointdb.indexfuncdsl.IndexFuncDSLParser.ValueContext;

@Tag("parameterized")
@Tag("canopy_height")
@Tag("canopy_height_percentile")
@Description("Percentile of Canopy Heights. Parameter: percentile  (based on point height above ground)")
public class Fun_H_P extends ParamProcessingFun {

	@Override
	public ProcessingFun instantiate(HashMap<String, ValueContext> paramMap) {
		return new Fun(this, paramMap);
	}

	public static class Fun extends ProcessingFun {

		private double percentile = -1;

		public Fun(Fun_H_P that, HashMap<String, ValueContext> paramMap) {
			super(that.getClass());
			ValueContext p = paramMap.get("percentile");
			if(p != null) {
				percentile = Double.parseDouble(p.number().getText());
			}			
		}

		@Override
		public double process(DataProvider2 provider) {
			double[] hs = provider.get_sortedCanopyHeights();
			return hs.length==0 ? 0 : new DescriptiveStatistics(provider.get_sortedCanopyHeights()).getPercentile(percentile);			
		}
	}
}
