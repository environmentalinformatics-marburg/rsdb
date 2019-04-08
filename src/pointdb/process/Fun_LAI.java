package pointdb.process;

@Tag("forest_structure")
@Description("Leaf-area index, from Getzin et al., with k=0.3, h.bin=1, GR.threshold=5 (based on CHM raster pixels)")
public class Fun_LAI extends ProcessingFun {
	//private static final Logger log = LogManager.getLogger();
	
	/*private static int[] getProfile_CHM_based(DataProvider2 provider) {
		RasterSubGrid chm = provider.getCHM();
		double[][] data = chm.data;
		int max = 0;
		for (int y = chm.start_y; y < chm.border_y; y++) {
			double[] row = data[y];
			for (int x = chm.start_x; x < chm.border_x; x++) {
				double v = row[x];
				if (Double.isFinite(v)) {					
					int i = (int) v;
					if(max < i) {
						max = i;
					}
				}
			}
		}
		if(max > 50) {
			max = 50;
		}
		int[] profile = new int[max + 1];

		for (int y = chm.start_y; y < chm.border_y; y++) {
			double[] row = data[y];
			for (int x = chm.start_x; x < chm.border_x; x++) {
				double v = row[x];
				if (Double.isFinite(v)) {					
					int i = (int) v;
					if(0 <= i && i <= max) {
						profile[i]++;
					}
				}
			}
		}
		return profile;
	}*/

	public static int[] getProfile(double[] zs) {
		int max = 0;
		for(double v:zs) {
			if (Double.isFinite(v)) {					
				int i = (int) v;
				if(max < i) {
					max = i;
				}
			}
		}
		if(max > 50) {
			max = 50;
		}
		int[] profile = new int[max + 2];

		for(double v:zs) {
			if (Double.isFinite(v)) {					
				int i = (int) v;
				if(0 <= i && i <= max) {
					profile[i]++;
				}
			}

		}
		return profile;
	}

	@Override
	public double process(DataProvider2 provider) {		
		//int[] profile = getProfile_CHM_based(provider);
		//log.info("LAI pre points " + provider.get_sortedRegionHeightPoints().size());
		//double[] points = provider.get_sortedCanopyHeights();
		double[] points = provider.get_sortedRegionHeights();
		//log.info("LAI points " + points.length);
		int[] profile = getProfile(points);
		//log.info("profile "+Arrays.toString(profile));
		if(profile.length == 0 || profile.length == 1) {
			return 0;
		}
		double[] vfp = calcVFP(profile);
		double lai = calcLAI(vfp);
		return lai;
	}

	public static double[] calcVFP(int[] profile) {
		int sum = 0;
		for(Integer cnt:profile) {
			sum += cnt;
		}
		//log.info("sum " + sum);
		double[] cumProfile = new double[profile.length];
		double cumsum = 0;
		for(int i = profile.length - 1; i >= 0; i--) {
			cumsum += profile[i];
			cumProfile[i] = 1d - cumsum / sum;
		}
		/*log.info("cumProfile ");
		for (int i = 0; i < cumProfile.length; i++) {
			log.info(i+" "+cumProfile[i]);
		}*/
		
		double[] cumProfileNext = new double[profile.length];
		for (int i = 0; i < cumProfile.length - 1; i++) {
			cumProfileNext[i] = cumProfile[i + 1];
		}
		cumProfileNext[profile.length - 1] = 1d;
		/*log.info("cumProfileNext ");
		for (int i = 0; i < cumProfileNext.length; i++) {
			log.info(i+" "+cumProfileNext[i]);
		}*/
		
		double[] vfp = new double[profile.length];
		double k = 0.3d;
		double h_bin = 1d;
		double factor = 1d / (k * h_bin);
		//log.info("factor " + factor);
		for (int i = 1; i < profile.length; i++) {
			vfp[i] = (cumProfileNext[i] == 0 || cumProfile[i] == 0) ? 0 : ( factor * (Math.log(cumProfileNext[i]) - Math.log(cumProfile[i])) );
		}
		
		/*log.info("vfp ");
		for (int i = 0; i < vfp.length; i++) {
			log.info(i+" "+vfp[i]);
		}*/
		
		int gr_threshold = profile.length < 5 ? profile.length : 5;
		for (int i = 0; i < gr_threshold; i++) {
			vfp[i] = 0;
		}
		
		/*log.info("vfp ");
		for (int i = 0; i < vfp.length; i++) {
			log.info(i+" "+vfp[i]);
		}*/
		
		return vfp;
	}
	
	public static double calcLAI(double[] vfp) {
		double sum = 0d;
		for(double v:vfp) {
			sum += v;
		}
		return sum;
	}
}