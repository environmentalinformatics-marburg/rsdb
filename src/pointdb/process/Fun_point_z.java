package pointdb.process;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import pointdb.base.GeoPoint;

public class Fun_point_z {
	
	static double[] getValues(DataProvider2 provider)  {
		return provider.get_regionPoints().mapDoubleArray(GeoPoint::getZ);
	}
	
	@Tag("point")
	@Tag("z")
	@Description("Mean point z-coordinate")	
	static class Fun_point_z_MEAN extends ProcessingFun {
		public static final Fun_point_z_MEAN DEFAULT = new Fun_point_z_MEAN();
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
	@Tag("z")
	@Description("Median point z-coordinate")
	public
	static class Fun_point_z_MEDIAN extends ProcessingFun {
		public static final Fun_point_z_MEDIAN DEFAULT = new Fun_point_z_MEDIAN(); 
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
	@Tag("z")
	@Description("Standard Deviation of point z-coordinate")
	static class Fun_point_z_SD extends ProcessingFun {
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
	@Tag("z")
	@Description("Variance of point z-coordinate")
	static class Fun_point_z_VAR extends ProcessingFun {
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
	@Tag("z")
	@Description("Coefficient of Variation of point z-coordinate")
	static class Fun_point_z_VAR_COEF extends ProcessingFun {
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
	@Tag("z")
	@Description("Maximum point z-coordinate")
	static public class Fun_point_z_MAX extends ProcessingFun {
		public static final Fun_point_z_MAX DEFAULT = new Fun_point_z_MAX(); 
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
	@Tag("z")
	@Description("Skewness of point z-coordinate")
	static class Fun_point_z_SKEW extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] hs = getValues(provider);
			return hs.length==0 ? 0 : new DescriptiveStatistics(hs).getSkewness();
		}
	}

	@Tag("point")
	@Tag("z")
	@Description("Excess Kurtosis of point z-coordinate")
	static class Fun_point_z_KURTOSIS extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] hs = getValues(provider);
			return hs.length==0 ? 0 : new DescriptiveStatistics(hs).getKurtosis();
		}
	}

	static double get_BE_H_Percentile(DataProvider2 provider, double percentile) {
		double[] hs = getValues(provider);
		return hs.length==0 ? 0 : new DescriptiveStatistics(hs).getPercentile(percentile);
	}
	
	@Tag("point")
	@Tag("z")
	@Description("5% Percentile of point z-coordinate")
	static class Fun_point_z_P5 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_BE_H_Percentile(provider, 5);
		}
	}


	@Tag("point")
	@Tag("z")
	@Description("10% Percentile of point z-coordinate")
	static class Fun_point_z_P10 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_BE_H_Percentile(provider, 10);
		}
	}

	@Tag("point")
	@Tag("z")
	@Description("20% Percentile of point z-coordinate")
	static class Fun_point_z_P20 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_BE_H_Percentile(provider, 20);
		}
	}
	
	@Tag("point")
	@Tag("z")
	@Description("25% Percentile of point z-coordinate")
	static class Fun_point_z_P25 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_BE_H_Percentile(provider, 25);
		}
	}


	@Tag("point")
	@Tag("z")
	@Description("30% Percentile of point z-coordinate")
	static class Fun_point_z_P30 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_BE_H_Percentile(provider, 30);
		}
	}

	@Tag("point")
	@Tag("z")
	@Description("40% Percentile of point z-coordinate")
	static class Fun_point_z_P40 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_BE_H_Percentile(provider, 40);
		}
	}

	@Tag("point")
	@Tag("z")
	@Description("50% Percentile of point z-coordinate")
	static class Fun_point_z_P50 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_BE_H_Percentile(provider, 50);
		}
	}

	@Tag("point")
	@Tag("z")
	@Description("60% Percentile of point z-coordinate")
	static class Fun_point_z_P60 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_BE_H_Percentile(provider, 60);
		}
	}

	@Tag("point")
	@Tag("z")
	@Description("70% Percentile of point z-coordinate")
	static class Fun_point_z_P70 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_BE_H_Percentile(provider, 70);
		}
	}
	
	@Tag("point")
	@Tag("z")
	@Description("75% Percentile of point z-coordinate")
	static class Fun_point_z_P75 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_BE_H_Percentile(provider, 75);
		}
	}


	@Tag("point")
	@Tag("z")
	@Description("80% Percentile of point z-coordinate")
	static class Fun_point_z_P80 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_BE_H_Percentile(provider, 80);
		}
	}

	@Tag("point")
	@Tag("z")
	@Description("90% Percentile of point z-coordinate")
	static class Fun_point_z_P90 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_BE_H_Percentile(provider, 90);
		}
	}
	
	@Tag("point")
	@Tag("z")
	@Description("95% Percentile of point z-coordinate")
	static class Fun_point_z_P95 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_BE_H_Percentile(provider, 95);
		}
	}
	
	@Tag("point")
	@Tag("z")
	@Description("99% Percentile of point z-coordinate")
	static class Fun_point_z_P99 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_BE_H_Percentile(provider, 99);
		}
	}

	@Tag("point")
	@Tag("z")
	@Description("100% Percentile of point z-coordinate")
	static class Fun_point_z_P100 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return get_BE_H_Percentile(provider, 100);
		}
	}

}
