package pointdb.process;

import pointdb.base.GeoPoint;

@Deprecated
@Description("Canopy height")
class Fun_canopy_height extends ProcessingFun {
	@Override
	public double process(DataProvider2 provider) {
		double max_z=0;
		for(GeoPoint p:provider.old.getGroundNormalisedPoints()) {
			if(max_z<p.z) {
				max_z = p.z;
			}
		}
		return max_z<0?Double.NaN:max_z;
	}
}