package pointdb.process;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import pointdb.base.GeoPoint;

public class Fun_point_veg_h {
	
	static double[] getValues(DataProvider2 provider)  {		
		return provider.get_sortedVegetationHeightPoints().mapDoubleArray(GeoPoint::getZ);
	}
	
	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("Mean vegetation point height above ground")	
	static class Fun_point_veg_h_MEAN extends ProcessingFun {
		public static final Fun_point_veg_h_MEAN DEFAULT = new Fun_point_veg_h_MEAN();
		@Override
		public double process(DataProvider2 provider) {
			double[] zs = getValues(provider);
			if(zs.length==0) {
				return 0; // !!!
			}
			double sum = 0;
			double cnt = 0;
			for(double z:zs) {
				sum += z;
				cnt++;
			}
			return sum / cnt;
		}
	}

	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("Median vegetation point height above ground")
	public
	static class Fun_point_veg_h_MEDIAN extends ProcessingFun {
		public static final Fun_point_veg_h_MEDIAN DEFAULT = new Fun_point_veg_h_MEDIAN(); 
		@Override
		public double process(DataProvider2 provider) {
			double[] zs = getValues(provider);
			int len = zs.length;
			if(len==0) {
				return 0;
			}
			if(len%2==0) {
				int pos = len/2;
				return (zs[pos-1] + zs[pos]) / 2d;
			} else {
				return zs[len/2];
			}
		}
	}

	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("Standard Deviation of vegetation point height above ground")
	static class Fun_point_veg_h_SD extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double cnt = 0;
			double sum = 0;
			double qsum = 0;
			for(double h:getValues(provider)) {
				cnt++;
				sum += h;
				qsum += h*h;					
			}
			return cnt==0? 0 : Math.sqrt( (cnt*qsum - sum*sum) / (cnt*(cnt-1)) );
		}
	}

	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("Variance of vegetation point height above ground")
	static class Fun_point_veg_h_VAR extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double cnt = 0;
			double sum = 0;
			double qsum = 0;
			for(double h:getValues(provider)) {
				cnt++;
				sum += h;
				qsum += h*h;					
			}
			return cnt==0? 0 : (cnt*qsum - sum*sum) / (cnt*(cnt-1));
		}
	}

	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("Coefficient of Variation of vegetation point height above ground")
	static class Fun_point_veg_h_VAR_COEF extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double cnt = 0;
			double sum = 0;
			double qsum = 0;
			for(double h:getValues(provider)) {
				cnt++;
				sum += h;
				qsum += h*h;					
			}
			return cnt==0? 0 : Math.sqrt( (cnt*qsum - sum*sum) / (cnt*(cnt-1)) ) / (sum / cnt);
		}
	}

	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("Maximum vegetation point height above ground")
	static public class Fun_point_veg_h_MAX extends ProcessingFun {
		public static final Fun_point_veg_h_MAX DEFAULT = new Fun_point_veg_h_MAX(); 
		@Override
		public double process(DataProvider2 provider) {
			double[] zs = getValues(provider);
			if(zs.length==0) {
				return 0; // !!
			}
			return zs[zs.length-1];			
		}
	}

	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("Skewness of vegetation point height above ground")
	static class Fun_point_veg_h_SKEW extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] hs = getValues(provider);
			return hs.length==0 ? 0 : new DescriptiveStatistics(hs).getSkewness();
		}
	}

	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("Excess Kurtosis of vegetation point height above ground")
	static class Fun_point_veg_h_KURTOSIS extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] hs = getValues(provider);
			return hs.length==0 ? 0 : new DescriptiveStatistics(hs).getKurtosis();
		}
	}

	static double get_percentile(DataProvider2 provider, double percentile) {
		double[] hs = getValues(provider);
		return hs.length==0 ? 0 : new DescriptiveStatistics(hs).getPercentile(percentile);
	}
	
	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("5% Percentile of vegetation point height above ground")
	static class Fun_point_veg_h_P5 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_percentile(provider, 5);
		}
	}


	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("10% Percentile of vegetation point height above ground")
	static class Fun_point_veg_h_P10 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_percentile(provider, 10);
		}
	}

	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("20% Percentile of vegetation point height above ground")
	static class Fun_point_veg_h_P20 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_percentile(provider, 20);
		}
	}
	
	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("25% Percentile of vegetation point height above ground")
	static class Fun_point_veg_h_P25 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_percentile(provider, 25);
		}
	}


	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("30% Percentile of vegetation point height above ground")
	static class Fun_point_veg_h_P30 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_percentile(provider, 30);
		}
	}

	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("40% Percentile of vegetation point height above ground")
	static class Fun_point_veg_h_P40 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_percentile(provider, 40);
		}
	}

	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("50% Percentile of vegetation point height above ground")
	static class Fun_point_veg_h_P50 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_percentile(provider, 50);
		}
	}

	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("60% Percentile of vegetation point height above ground")
	static class Fun_point_veg_h_P60 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_percentile(provider, 60);
		}
	}

	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("70% Percentile of vegetation point height above ground")
	static class Fun_point_veg_h_P70 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_percentile(provider, 70);
		}
	}
	
	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("75% Percentile of vegetation point height above ground")
	static class Fun_point_veg_h_P75 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_percentile(provider, 75);
		}
	}


	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("80% Percentile of vegetation point height above ground")
	static class Fun_point_veg_h_P80 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_percentile(provider, 80);
		}
	}

	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("90% Percentile of vegetation point height above ground")
	static class Fun_point_veg_h_P90 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_percentile(provider, 90);
		}
	}
	
	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("95% Percentile of vegetation point height above ground")
	static class Fun_point_veg_h_P95 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_percentile(provider, 95);
		}
	}
	
	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("99% Percentile of vegetation point height above ground")
	static class Fun_point_veg_h_P99 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_percentile(provider, 99);
		}
	}

	@Tag("point")
	@Tag("height")
	@Tag("vegetation")
	@Description("100% Percentile of vegetation point height above ground")
	static class Fun_point_veg_h_P100 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_percentile(provider, 100);
		}
	}

}
