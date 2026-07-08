package pointdb.process;

import java.util.HashSet;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import pointcloud.Rect3d;
import pointdb.base.GeoPoint;
import util.collections.vec.Vec;

@Tag("point")
@Tag("forest_structure")
@Description("Box Dimension")
public class Fun_Db extends ProcessingFun {

	public static double calculate_box_dimension(double[] box_sizes, int[] voxel_counts, int box_sizes_count, int points_count) {
		SimpleRegression simpleRegression = new SimpleRegression();

		for (int i = 0; i < box_sizes_count; i++) {
			int voxel_count = voxel_counts[i];
			if(voxel_count > 1 && voxel_count < points_count) { // not all points in one box, not all points in separate boxes
				double log_inv_r = Math.log(1 / box_sizes[i]);
				double log_N = Math.log(voxel_count);

				simpleRegression.addData(log_inv_r, log_N);
			}
		}

		if(simpleRegression.getN() < 2) {
			return Double.NaN;
		}

		return simpleRegression.getSlope();
	}

	public static double calc(Vec<GeoPoint> rawPoints) {
		final double MIN_SIZE = 0.2;
		final int N_HALVINGS = 14;
		final double MIN_HEIGHT_FILTER = 0.5;

		Vec<GeoPoint> points = rawPoints.filter(p -> MIN_HEIGHT_FILTER < p.z);

		if(points.size() < 2) {
			return Double.NaN;
		}

		Rect3d rect3d = GeoPoint.getRect3d(points);

		double box_size_max = Math.round(rect3d.maxDimRange() * 100.0) / 100.0;

		int box_sizes_count = 0;
		double[] box_sizes = new double[N_HALVINGS + 1];
		for(int halving = 0; halving <= N_HALVINGS; halving++) {
			double box_size = box_size_max / Math.pow(2, halving);
			if(box_size < MIN_SIZE) {
				break;
			}
			box_sizes[box_sizes_count++] = box_size;
		}
		if(box_sizes_count < 2) {
			return Double.NaN;
		}

		int[] voxel_counts = new int[box_sizes_count];

		for(int i = 0; i < box_sizes_count; i++) {
			double box_size = box_sizes[i];
			double half_box = box_size / 2.0;

			//HashSet<List<Integer>> voxelized_boxes = new HashSet<>();
			HashSet<Long> voxelized_boxes = new HashSet<>();
			for(GeoPoint p : points) {				
				int ix = (int) Math.round((p.x - rect3d.xmin + half_box) / box_size);
				int iy = (int) Math.round((p.y - rect3d.ymin + half_box) / box_size);
				int iz = (int) Math.round((p.z - rect3d.zmin + half_box) / box_size);

				//voxelized_boxes.add(Arrays.asList(ix, iy, iz));
				long packedVoxel = ((long) ix << 42) | ((long) iy << 21) | iz;
				voxelized_boxes.add(packedVoxel);
			}
			voxel_counts[i] = voxelized_boxes.size();			
		}

		return calculate_box_dimension(box_sizes, voxel_counts, box_sizes_count, points.size());
	}

	@Override
	public double process(DataProvider2 provider) {
		Vec<GeoPoint> rawPoints = provider.get_sortedRegionHeightPoints();		
		return calc(rawPoints);
	}	
}