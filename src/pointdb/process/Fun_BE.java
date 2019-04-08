package pointdb.process;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import pointdb.base.GeoPoint;
import util.collections.vec.Vec;

@Tag("BE")
public class Fun_BE {
	//private static final Logger log = LogManager.getLogger();

	@Tag("canopy_height")
	@Description("Mean Canopy Height (based on point height above ground)")	
	static class Fun_BE_H_MEAN extends ProcessingFun {
		public static final Fun_BE_H_MEAN DEFAULT = new Fun_BE_H_MEAN();
		@Override
		public double process(DataProvider2 provider) {
			double[] zs = provider.get_sortedCanopyHeights();
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

	@Tag("canopy_height")
	@Description("Median Canopy Height (based on point height above ground)")
	public
	static class Fun_BE_H_MEDIAN extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] zs = provider.get_sortedCanopyHeights();
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

	@Tag("canopy_height")
	@Description("Standard Deviation of Canopy Height (based on point height above ground)")
	static class Fun_BE_H_SD extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double cnt = 0;
			double sum = 0;
			double qsum = 0;
			for(double h:provider.get_sortedCanopyHeights()) {
				cnt++;
				sum += h;
				qsum += h*h;					
			}
			return cnt==0? 0 : Math.sqrt( (cnt*qsum - sum*sum) / (cnt*(cnt-1)) );
		}
	}
	
	@Tag("canopy_height")
	@Description("Variance of Canopy Height (based on point height above ground)")
	static class Fun_BE_H_VAR extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double cnt = 0;
			double sum = 0;
			double qsum = 0;
			for(double h:provider.get_sortedCanopyHeights()) {
				cnt++;
				sum += h;
				qsum += h*h;					
			}
			return cnt==0? 0 : (cnt*qsum - sum*sum) / (cnt*(cnt-1));
		}
	}
	
	@Tag("canopy_height")
	@Description("Coefficient of Variation of Canopy Height (based on point height above ground)")
	static class Fun_BE_H_VAR_COEF extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double cnt = 0;
			double sum = 0;
			double qsum = 0;
			for(double h:provider.get_sortedCanopyHeights()) {
				cnt++;
				sum += h;
				qsum += h*h;					
			}
			return cnt==0? 0 : Math.sqrt( (cnt*qsum - sum*sum) / (cnt*(cnt-1)) ) / (sum / cnt);
		}
	}

	@Tag("canopy_height")
	@Description("Maximum Canopy Height (based on point height above ground)")
	static public class Fun_BE_H_MAX extends ProcessingFun {
		public static final Fun_BE_H_MAX DEFAULT = new Fun_BE_H_MAX(); 
		@Override
		public double process(DataProvider2 provider) {
			double[] zs = provider.get_sortedCanopyHeights();
			if(zs.length==0) {
				return 0; // !!
			}
			return zs[zs.length-1];			
		}
	}

	@Tag("canopy_height")
	@Description("Skewness of Canopy Height Distribution (based on point height above ground)")
	static class Fun_BE_H_SKEW extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] hs = provider.get_sortedCanopyHeights();
			return hs.length==0 ? 0 : new DescriptiveStatistics(provider.get_sortedCanopyHeights()).getSkewness();
		}
	}

	@Tag("canopy_height")
	@Description("Excess Kurtosis of Canopy Height Distribution (based on point height above ground)")
	static class Fun_BE_H_KURTOSIS extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] hs = provider.get_sortedCanopyHeights();
			return hs.length==0 ? 0 : new DescriptiveStatistics(hs).getKurtosis();
		}
	}

	@Tag("canopy_height")
	@Tag("canopy_height_percentile")
	@Description("10% Percentile of Canopy Heights (based on point height above ground)")
	static class Fun_BE_H_P10 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] hs = provider.get_sortedCanopyHeights();
			return hs.length==0 ? 0 : new DescriptiveStatistics(provider.get_sortedCanopyHeights()).getPercentile(10);
		}
	}

	@Tag("canopy_height")
	@Tag("canopy_height_percentile")
	@Description("20% Percentile of Canopy Heights (based on point height above ground)")
	static class Fun_BE_H_P20 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] hs = provider.get_sortedCanopyHeights();
			return hs.length==0 ? 0 : new DescriptiveStatistics(provider.get_sortedCanopyHeights()).getPercentile(20);
		}
	}

	@Tag("canopy_height")
	@Tag("canopy_height_percentile")
	@Description("30% Percentile of Canopy Heights (based on point height above ground)")
	static class Fun_BE_H_P30 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] hs = provider.get_sortedCanopyHeights();
			return hs.length==0 ? 0 : new DescriptiveStatistics(provider.get_sortedCanopyHeights()).getPercentile(30);
		}
	}

	@Tag("canopy_height")
	@Tag("canopy_height_percentile")
	@Description("40% Percentile of Canopy Heights (based on point height above ground)")
	static class Fun_BE_H_P40 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] hs = provider.get_sortedCanopyHeights();
			return hs.length==0 ? 0 : new DescriptiveStatistics(provider.get_sortedCanopyHeights()).getPercentile(40);
		}
	}

	@Tag("canopy_height")
	@Tag("canopy_height_percentile")
	@Description("50% Percentile of Canopy Heights (based on point height above ground)")
	static class Fun_BE_H_P50 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] hs = provider.get_sortedCanopyHeights();
			return hs.length==0 ? 0 : new DescriptiveStatistics(provider.get_sortedCanopyHeights()).getPercentile(50);
		}
	}

	@Tag("canopy_height")
	@Tag("canopy_height_percentile")
	@Description("60% Percentile of Canopy Heights (based on point height above ground)")
	static class Fun_BE_H_P60 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] hs = provider.get_sortedCanopyHeights();
			return hs.length==0 ? 0 : new DescriptiveStatistics(provider.get_sortedCanopyHeights()).getPercentile(60);
		}
	}

	@Tag("canopy_height")
	@Tag("canopy_height_percentile")
	@Description("70% Percentile of Canopy Heights (based on point height above ground)")
	static class Fun_BE_H_P70 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] hs = provider.get_sortedCanopyHeights();
			return hs.length==0 ? 0 : new DescriptiveStatistics(provider.get_sortedCanopyHeights()).getPercentile(70);
		}
	}

	@Tag("canopy_height")
	@Tag("canopy_height_percentile")
	@Description("80% Percentile of Canopy Heights (based on point height above ground)")
	static class Fun_BE_H_P80 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] hs = provider.get_sortedCanopyHeights();
			return hs.length==0 ? 0 : new DescriptiveStatistics(provider.get_sortedCanopyHeights()).getPercentile(80);
		}
	}
	
	@Tag("canopy_height")
	@Tag("canopy_height_percentile")
	@Description("90% Percentile of Canopy Heights (based on point height above ground)")
	static class Fun_BE_H_P90 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] hs = provider.get_sortedCanopyHeights();
			return hs.length==0 ? 0 : new DescriptiveStatistics(provider.get_sortedCanopyHeights()).getPercentile(90);
		}
	}
	
	@Tag("canopy_height")
	@Tag("canopy_height_percentile")
	@Description("100% Percentile of Canopy Heights (based on point height above ground)")
	static class Fun_BE_H_P100 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double[] hs = provider.get_sortedCanopyHeights();
			return hs.length==0 ? 0 : new DescriptiveStatistics(provider.get_sortedCanopyHeights()).getPercentile(100);
		}
	}

	private static final double understory_layer_height_max = 5.0d;
	private static final double regeneration_layer_height_max = 2.0d;  // meter above ground
	private static final double ground_layer_height_max = 0.3d; // meter above ground

	@Tag("RD_strata")
	@Description("Return density of canopy vegetation layer (based on point height above ground)")
	static class Fun_BE_RD_CAN extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double hmax = Fun_BE_H_MAX.DEFAULT.process(provider);
			if(Double.isNaN(hmax)) {
				return Double.NaN;
			}
			double hcan = understory_layer_height_max;
			if(hcan<=regeneration_layer_height_max) {
				return 0; // !!!
			}
			double cnt = 0;
			Vec<GeoPoint> points = provider.get_sortedRegionHeightPoints();
			if(provider.classified_vegetation) {
				for(GeoPoint p:points) {
					if(hcan<p.z && p.isVegetaion()) {
						cnt++;
					}
				}
			} else {
				for(GeoPoint p:points) {
					if(hcan<p.z) {
						cnt++;
					}
				}

			}
			return cnt/points.size();
		}
	}

	@Tag("RD_strata")
	@Description("Return density of understory vegetation layer (based on point height above ground)")
	static class Fun_BE_RD_UND extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double hmax = Fun_BE_H_MAX.DEFAULT.process(provider);
			if(Double.isNaN(hmax)) {
				return Double.NaN;
			}
			double hcan = understory_layer_height_max;
			if(hcan<=regeneration_layer_height_max) {
				return 0; // !!!
			}
			double cnt = 0;
			Vec<GeoPoint> points = provider.get_sortedRegionHeightPoints();
			if(provider.classified_vegetation) {
				for(GeoPoint p:points) {
					if(regeneration_layer_height_max<p.z && p.z<=hcan && p.isVegetaion()) {
						cnt++;
					}
				}
			} else {
				for(GeoPoint p:points) {
					if(regeneration_layer_height_max<p.z && p.z<=hcan) {
						cnt++;
					}
				}

			}
			return cnt/points.size();
		}
	}

	@Tag("RD_strata")
	@Description("Return density of regeneration vegetation layer (based on point height above ground)")
	static class Fun_BE_RD_REG extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double cnt = 0;
			Vec<GeoPoint> points = provider.get_sortedRegionHeightPoints();
			if(points.isEmpty()) {
				return Double.NaN;
			}
			if(provider.classified_vegetation) {
				for(GeoPoint p:points) {				
					if(p.z<=regeneration_layer_height_max && p.isVegetaion()) {
						cnt++;
					}
				}
			} else {
				for(GeoPoint p:points) {				
					if(ground_layer_height_max<p.z && p.z<=regeneration_layer_height_max) {
						cnt++;
					}
				}
			}
			return cnt/points.size();
		}
	}

	@Tag("RD_strata")
	@Description("Return density of ground layer (based on point height above ground)")
	static class Fun_BE_RD_GND extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double cnt = 0;
			Vec<GeoPoint> points = provider.get_sortedRegionHeightPoints();
			if(points.isEmpty()) {
				return Double.NaN;
			}
			if(provider.classified_vegetation) {
				for(GeoPoint p:points) {
					if(!p.isVegetaion()) {
						cnt++;
					}
				}
			} else {
				for(GeoPoint p:points) {
					if(p.z<=ground_layer_height_max) {
						cnt++;
					}									
				}				
			}
			return cnt/points.size();
		}
	}

	@Tag("PR_strata")
	@Description("Penetration rate of canopy vegetation layer (redirects to BE_RD_CAN) (based on point height above ground)")
	static class Fun_BE_PR_CAN extends Fun_BE_RD_CAN {
		public static final Fun_BE_PR_CAN DEFAULT = new Fun_BE_PR_CAN(); 
	}


	@Tag("PR_strata")
	@Description("Penetration rate of understory vegetation layer (based on point height above ground)")
	static class Fun_BE_PR_UND extends ProcessingFun {
		public static final Fun_BE_PR_UND DEFAULT = new Fun_BE_PR_UND(); 
		@Override
		public double process(DataProvider2 provider) {
			double hmax = Fun_BE_H_MAX.DEFAULT.process(provider);
			if(Double.isNaN(hmax)) {
				return Double.NaN;
			}
			double hcan = understory_layer_height_max;
			if(hcan<=regeneration_layer_height_max) {
				return 0;
			}
			double cnt = 0;
			double reach = 0;
			Vec<GeoPoint> points = provider.get_sortedRegionHeightPoints();
			if(points.isEmpty()) {
				return Double.NaN;
			}
			if(provider.classified_vegetation) {
				for(GeoPoint p:points) {
					if(p.z <= hcan) {
						reach++;
						if(regeneration_layer_height_max < p.z && p.isVegetaion()) {
							cnt++;
						}
					}
				}
			} else {
				for(GeoPoint p:points) {
					if(p. z<= hcan) {
						reach++;
						if(regeneration_layer_height_max < p.z) {
							cnt++;
						}
					}
				}				
			}
			if(reach==0) {
				return Double.NaN;
			}
			return cnt/reach;
		}
	}

	@Tag("PR_strata")
	@Description("Penetration rate of regeneration vegetation layer (based on point height above ground)")
	static class Fun_BE_PR_REG extends ProcessingFun {
		public static final Fun_BE_PR_REG DEFAULT = new Fun_BE_PR_REG(); 
		@Override
		public double process(DataProvider2 provider) {
			double cnt = 0;
			double reach = 0;
			Vec<GeoPoint> points = provider.get_sortedRegionHeightPoints();
			if(points.isEmpty()) {
				return Double.NaN;
			}
			if(provider.classified_vegetation) {
				for(GeoPoint p:points) {
					if(p.z <= regeneration_layer_height_max) {
						reach++;
						if(p.isVegetaion()) {
							cnt++;
						}
					}
				}
			} else {
				for(GeoPoint p:points) {
					if(p.z <= regeneration_layer_height_max) {
						reach++;
						if(ground_layer_height_max < p.z) {
							cnt++;
						}
					}
				}				
			}
			if(reach==0) {
				return Double.NaN;
			}
			return cnt/reach;
		}
	}

	@Description("Foliage Height Diversity (based on point height above ground)")
	static class Fun_BE_FHD extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			double can = Fun_BE_PR_CAN.DEFAULT.process(provider);
			double und = Fun_BE_PR_UND.DEFAULT.process(provider);
			double reg = Fun_BE_PR_REG.DEFAULT.process(provider);
			if(Double.isNaN(can) || Double.isNaN(und) || Double.isNaN(reg)) {
				return Double.NaN;
			}
			double scan = can*Math.log(can);
			double sund = und*Math.log(und);
			double sreg = reg*Math.log(reg);
			return -( (Double.isFinite(scan) ? scan : 0) + (Double.isFinite(sund) ? sund : 0) + (Double.isFinite(sreg) ? sreg : 0) );
		}
	}

	@Exculde
	@Description("Canopy Roughness (TODO)")
	static class Fun_BE_CAN_ROUGH extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {
			return 0;
		}
	}

	@Description("Mean Elevation (mean of ground a.s.l., based on DTM raster pixels)")
	public static class Fun_BE_ELEV_MEAN extends Fun_dtm_elevation.Fun_dtm_elevation_mean {
	}

	@Description("Mean Slope (slope of ground a.s.l., based on DTM raster pixels bilinear regression)")
	static class Fun_BE_ELEV_SLOPE extends Fun_dtm_slope.Fun_dtm_slope_reg {
	}

	@Description("Mean Aspect (aspect of ground a.s.l., based on DTM raster pixels bilinear regression)")
	static class Fun_BE_ELEV_ASPECT extends Fun_dtm_aspect.Fun_dtm_aspect_reg {
	}
	
	@Tag("PR_meter")
	@Exculde("base class")
	static class Fun_BE_PR_INTERVAL extends ProcessingFun {
		private final double hsetMin;
		private final double hsetMax;
		public Fun_BE_PR_INTERVAL(int hsetMax) {
			super("BE_PR_" + (hsetMax<=9 ? "0" : "") + hsetMax, "Penetration rate of "+hsetMax+" meter layer (based on point height above ground)");
			this.hsetMin = hsetMax - 1;
			this.hsetMax = hsetMax;
		}
		@Override
		public double process(DataProvider2 provider) {
			double cnt = 0;
			double reach = 0;
			Vec<GeoPoint> points = provider.get_sortedRegionHeightPoints();
			if(points.isEmpty()) {
				return Double.NaN;
			}
			if(provider.classified_vegetation) {
				for(GeoPoint p:points) {
					if(p.z <= hsetMax) {
						reach++;
						if(hsetMin < p.z && p.isVegetaion()) {
							cnt++;
						}
					}
				}
			} else {
				for(GeoPoint p:points) {
					if(p.z <= hsetMax) {
						reach++;
						if(hsetMin < p.z) {
							cnt++;
						}
					}
				}				
			}
			if(reach == 0) {
				return Double.NaN;
			}
			return cnt/reach;
		}
	}
	
	@Tag("RD_meter")
	@Exculde("base class")
	static class Fun_BE_RD_INTERVAL extends ProcessingFun {
		private final double hsetMin;
		private final double hsetMax;
		public Fun_BE_RD_INTERVAL(int hsetMax) {
			super("BE_RD_" + (hsetMax<=9 ? "0" : "") + hsetMax, "Return density of "+hsetMax+" meter layer (based on point height above ground)");
			this.hsetMin = hsetMax - 1;
			this.hsetMax = hsetMax;
		}
		@Override
		public double process(DataProvider2 provider) {
			double cnt = 0;
			Vec<GeoPoint> points = provider.get_sortedRegionHeightPoints();
			if(points.isEmpty()) {
				return Double.NaN;
			}
			for(GeoPoint p:points) {			
				if(hsetMin<p.z && p.z<=hsetMax) {
					cnt++;
				}
			}
			return cnt/points.size();
		}
	}
	


}
