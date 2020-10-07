package pointdb.processing.geopoint;

import java.util.Arrays;

import pointdb.base.GeoPoint;
import util.Util;
import util.collections.vec.Vec;

public class Normalise {
	public boolean normalise_origin = false;
	public boolean normalise_ground = false;
	public boolean normalise_extremes = false;

	/**
	 * parses normalise parameter
	 * @param normaliseText null for no parameter
	 * @return 
	 */
	public static Normalise parse(String normaliseText) {
		Normalise normalise = new Normalise();
		if(normaliseText==null) {
			return normalise;
		}
		for(String n:Util.columnTextToColumns(normaliseText, false)) {
			switch(n.trim().toLowerCase()) {
			case "": //empty
				break;
			case "origin":
				normalise.normalise_origin = true;
				break;
			case "ground":
				normalise.normalise_ground = true;
				break;
			case "extremes":
				normalise.normalise_extremes = true;
				break;
			default:
				throw new RuntimeException("normalise type unknown: "+n);
			}
		}
		return normalise;
	}

	@Deprecated
	public Vec<GeoPoint> optional_normalise(Vec<GeoPoint> result) {
		result = optional_normaliseExtremes(result);
		result = optional_normaliseGround(result);
		return result;
	}

	@Deprecated
	private Vec<GeoPoint> optional_normaliseGround(Vec<GeoPoint> result) {
		if(!normalise_ground) {
			return result;
		}
		double xd_min = Double.MAX_VALUE;
		double yd_min = Double.MAX_VALUE;
		double xd_max = -Double.MAX_VALUE;
		double yd_max = -Double.MAX_VALUE;
		for(GeoPoint p:result) {
			if(p.x<xd_min) xd_min = p.x;
			if(p.y<yd_min) yd_min = p.y;
			if(p.x>xd_max) xd_max = p.x;
			if(p.y>yd_max) yd_max = p.y;
		}
		int x_min = (int) xd_min;
		int y_min = (int) yd_min;
		int x_max = (int) xd_max;
		int y_max = (int) yd_max;
		int x_range = x_max-x_min+1;
		int y_range = y_max-y_min+1;
		double[][] ground = new double[x_range][y_range];
		for (int ix = 0; ix < x_range; ix++) {
			for (int iy = 0; iy < y_range; iy++) {
				ground[ix][iy] = Double.MAX_VALUE;
			}
		}
		for(GeoPoint p:result) {
			if(ground[((int) p.x)-x_min][((int) p.y)-y_min]>p.z) {
				ground[((int) p.x)-x_min][((int) p.y)-y_min] = p.z;
			}
		}
		Vec<GeoPoint> corrected = new Vec<GeoPoint>(result.size());
		for(GeoPoint p:result) {
			corrected.add(GeoPoint.of(p.x, p.y, p.z-ground[((int) p.x)-x_min][((int) p.y)-y_min], p));
		}
		return corrected;		
	}

	public Vec<GeoPoint> optional_normaliseExtremes(Vec<GeoPoint> result) {
		if(!normalise_extremes) {
			return result;
		}
		final int size = result.size();
		int extreme_part = size/100;
		if(extreme_part<1) {
			return result;
		}
		double[] zs = new double[size];
		for (int i = 0; i < size; i++) {
			zs[i] = result.get(i).z;
		}
		Arrays.sort(zs);
		double min = zs[extreme_part-1];
		double max = zs[size-extreme_part];
		Vec<GeoPoint> corrected = new Vec<GeoPoint>(size);
		for(GeoPoint p:result) {
			if(p.z>min && p.z<max) {
				corrected.add(p);
			}
		}
		return corrected;
	}
	
	public static Vec<GeoPoint> no_extermes_of_sorted(Vec<GeoPoint> result) {
		final int size = result.size();
		int extreme_part = size/100;
		if(extreme_part<1) {
			return result;
		}
		double[] zs = new double[size];
		for (int i = 0; i < size; i++) {
			zs[i] = result.get(i).z;
		}
		double min = zs[extreme_part-1];
		double max = zs[size-extreme_part];
		Vec<GeoPoint> corrected = new Vec<GeoPoint>(size);
		for(GeoPoint p:result) {
			if(p.z>min && p.z<max) {
				corrected.add(p);
			}
		}
		return corrected;
	}
	
	public static Vec<GeoPoint> translate(Vec<GeoPoint> result, double tx, double ty) {
		return result.map(p->GeoPoint.of(p.x + tx, p.y + ty, p));
	}
}
