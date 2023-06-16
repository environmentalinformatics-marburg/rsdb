package pointdb.process;

import com.googlecode.javaewah.datastructure.BitSet;

import pointcloud.Rect3d;
import pointdb.base.GeoPoint;

@Tag("forest_structure")
public class Fun_ENL {

	@Description("Effective number of layers (ENL) with Hill-Number 0D.")
	static class Fun_ENL0 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {		
			GeoPoint[] points = provider.get_sortedCanopyHeightPoints();
			if(points.length < 1) {
				return Double.NaN;
			}
			Rect3d rect3d = GeoPoint.getRect3d(points);		
			int zmin = (int) rect3d.zmin;
			int zmax = (int) rect3d.zmax;
			int zrange = zmax - zmin + 1;
			return zrange;
		}
	}
	
	@Description("Effective number of layers (ENL) with Hill-Number 1D.")
	static class Fun_ENL1 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {		
			GeoPoint[] points = provider.get_sortedCanopyHeightPoints();
			if(points.length < 1) {
				return Double.NaN;
			}
			Rect3d rect3d = GeoPoint.getRect3d(points);		
			int xmin = (int) (rect3d.xmin / 0.2d);
			int xmax = (int) (rect3d.xmax / 0.2d);
			int xrange = xmax - xmin + 1;
			int ymin = (int) (rect3d.ymin / 0.2d);
			int ymax = (int) (rect3d.ymax / 0.2d);
			int yrange = ymax - ymin + 1;
			int xyrange = xrange * yrange;
			int zmin = (int) rect3d.zmin;
			int zmax = (int) rect3d.zmax;
			int zrange = zmax - zmin + 1;
			double xyzrangeD = xyrange * zrange;

			BitSet[] layer = new BitSet[zrange];
			for (int z = 0; z < zrange; z++) {
				layer[z] = new BitSet(xyrange);
			}

			for(GeoPoint p : points) {
				int x = ((int) (p.x / 0.2d)) - xmin;
				int y = ((int) (p.y / 0.2d)) - ymin;
				int z = ((int) p.z) - zmin;
				layer[z].set(y * xrange + x);
			}

			double[] pval = new double[zrange];
			for (int z = 0; z < zrange; z++) {
				pval[z] = layer[z].cardinality() / xyzrangeD;
				//pval[z] = layer[z].cardinality();
			}

			double v = 0d; 
			for (int z = 0; z < zrange; z++) {
				double p = pval[z];
				v += p * Math.log(p);
				//v += p;
			}

			return Math.exp(-v);
			//return v;
		}
	}
	
	@Description("Effective number of layers (ENL) with Hill-Number 2D.")
	static class Fun_ENL2 extends ProcessingFun {
		@Override
		public double process(DataProvider2 provider) {		
			GeoPoint[] points = provider.get_sortedCanopyHeightPoints();
			if(points.length < 1) {
				return Double.NaN;
			}
			Rect3d rect3d = GeoPoint.getRect3d(points);		
			int xmin = (int) (rect3d.xmin / 0.2d);
			int xmax = (int) (rect3d.xmax / 0.2d);
			int xrange = xmax - xmin + 1;
			int ymin = (int) (rect3d.ymin / 0.2d);
			int ymax = (int) (rect3d.ymax / 0.2d);
			int yrange = ymax - ymin + 1;
			int xyrange = xrange * yrange;
			int zmin = (int) rect3d.zmin;
			int zmax = (int) rect3d.zmax;
			int zrange = zmax - zmin + 1;
			double xyzrangeD = xyrange * zrange;

			BitSet[] layer = new BitSet[zrange];
			for (int z = 0; z < zrange; z++) {
				layer[z] = new BitSet(xyrange);
			}

			for(GeoPoint p : points) {
				int x = ((int) (p.x / 0.2d)) - xmin;
				int y = ((int) (p.y / 0.2d)) - ymin;
				int z = ((int) p.z) - zmin;
				layer[z].set(y * xrange + x);
			}

			double[] pval = new double[zrange];
			for (int z = 0; z < zrange; z++) {
				pval[z] = layer[z].cardinality() / xyzrangeD;
			}

			double v = 0d; 
			for (int z = 0; z < zrange; z++) {
				double p = pval[z];
				v += p * p;
			}

			return 1d / v;
		}
	}
}