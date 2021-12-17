package pointdb.processing.geopoint;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import org.tinylog.Logger;

import pointdb.base.GeoPoint;
import pointdb.base.Rect;
import util.Serialisation;
import util.collections.vec.Vec;

public class RasterGrid extends RasterSubGrid {
	@SuppressWarnings("unused")
	

	public static final int window_size = PointGrid.window_size;

	public final Map<String, Object> meta = new TreeMap<String, Object>();

	private RasterGrid(PointGrid g) {
		super(g.min_x, g.min_y, g.max_x, g.max_y);
	}

	public RasterGrid(RasterGrid g) {
		super(g.local_min_x, g.local_min_y, g.local_max_x, g.local_max_y);
	}

	public RasterGrid(int xmin, int xmax, int ymin, int ymax) {
		super(xmin, ymin, xmax, ymax);
	}

	public static RasterGrid ofExtent(PointGrid pointGrid) {
		RasterGrid rasterGrid = new RasterGrid(pointGrid);
		return rasterGrid;
	}

	public int get_cell_x(int local_x) {
		return local_min_x+local_x;
	}

	public int get_cell_y(int local_y) {
		return local_min_y+local_y;
	}

	public void first_z(PointGrid pointGrid) {
		Vec<GeoPoint>[][] grid = pointGrid.grid;

		for (int y = start_y; y < border_y; y++) {
			Vec<GeoPoint>[] row = grid[y];
			double[] data_row = data[y];
			for (int x = start_x; x < border_x; x++) {
				double z = Double.NaN;

				Vec<GeoPoint> cell = row[x];
				if(cell.size()>0) {
					z = cell.get(0).z;
				} else {
					PointSubGrid wgrid = pointGrid.dynamicWindowSubGrid(x, y, 1, 16);
					if(wgrid!=null) {
						z = 0;
						int cnt=0;
						for(GeoPoint p:wgrid) {
							z += p.z;
							cnt++;
						}
						z/=cnt;
					}
				}

				data_row[x] = z;

			}
		}		
	}

	public void window_z(PointGrid pointGrid, int pointsMin, int borderMax) {
		int start_x = window_size-1;
		int start_y = window_size-1;
		int border_x = range_x-window_size+1;
		int border_y = range_y-window_size+1;
		for (int y = start_y; y < border_y; y++) {
			double[] data_row = data[y];
			for (int x = start_x; x < border_x; x++) {

				int collected_size = 0;
				int border = 0; // border minimum is border+1 !!!
				while(collected_size<pointsMin && border<=borderMax) {
					border++;  // inc before count call !!!
					collected_size = pointGrid.countWindow(x, y, border);					
				}

				if(collected_size>=pointsMin) {
					OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
					double[] ry = new double[collected_size];
					double[][] rx = new double[collected_size][2];
					int i = 0;
					for(GeoPoint p:pointGrid.windowSubGrid(x, y, border)) {
						double[] rxt = rx[i];
						rxt[0] = p.x;
						rxt[1] = p.y;
						ry[i] = p.z;
						i++;
					}
					regression.newSampleData(ry, rx);
					double[] reg = regression.estimateRegressionParameters();
					data_row[x] = reg[0]+reg[1]*get_cell_x(x)+reg[2]*get_cell_y(y);
				} else {
					data_row[x] = Double.NaN;
				}
			}
		}
	}

	public Vec<GeoPoint> toGeoPoints() {
		Vec<GeoPoint> result = new Vec<GeoPoint>(cell_count);
		int border_x = range_x-window_size+1;
		int border_y = range_y-window_size+1;
		for (int y = window_size-1; y < border_y; y++) {
			double[] data_row = data[y];
			for (int x = window_size-1; x < border_x; x++) {
				double v = data_row[x];
				if(!Double.isNaN(v)) {
					result.add(GeoPoint.of((double)x, (double)y, v));
				}
			}
		}
		return result;
	}

	public void minus_zero(RasterGrid rasterGrid_DTM) {
		int border_x = range_x-window_size+1;
		int border_y = range_y-window_size+1;
		for (int y = window_size-1; y < border_y; y++) {
			double[] data_row = data[y];
			double[] ref_row = rasterGrid_DTM.data[y];
			for (int x = window_size-1; x < border_x; x++) {
				if(Double.isFinite(data_row[x]) && Double.isFinite(ref_row[x]) && ref_row[x]>0d) {
					data_row[x] -= ref_row[x];
					if(data_row[x]<0d) {
						data_row[x] = 0d;
					}
				} else {
					data_row[x] = 0d;
				}
			}
		}		
	}

	@Override
	public void writeRawGrid(DataOutput out) throws IOException {
		Serialisation.writeArrayArrayBE(out, data);
	}

	public RasterSubGrid subGrid(Rect rect) {
		return RasterSubGrid.of(this,rect);
	}

	public static RasterGrid ofSlope(RasterGrid r) {
		RasterGrid s = new RasterGrid(r);
		ofSlope(r,s);
		return s;
	}	
	
	public RasterGrid toSmoothed() {
		RasterGrid r = new RasterGrid(this);
		int w = data[0].length-1;
		int h = data.length-1;
		for (int y = 1; y < h; y++) {
			double[] row0 = data[y - 1];
			double[] row1 = data[y];
			double[] row2 = data[y + 1];
			double[] rrow = r.data[y];
			for (int x = 1; x < w; x++) {
				double mean = (row0[x-1]+row0[x]+row0[x+1]+row1[x-1]+row1[x]+row1[x+1]+row2[x-1]+row2[x]+row2[x+1])/9d;
				rrow[x] += mean; 
			}
		}
		return r;
	}	
}
