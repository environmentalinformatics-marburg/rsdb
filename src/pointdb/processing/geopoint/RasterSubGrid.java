package pointdb.processing.geopoint;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import org.tinylog.Logger;

import pointdb.base.Rect;
import util.Serialisation;

public class RasterSubGrid extends AbstractRaster {
	@SuppressWarnings("unused")
	

	public final int start_x;
	public final int start_y;
	public final int border_x;
	public final int border_y;

	public final double[][] data;

	public final Map<String, Object> meta = new TreeMap<String, Object>();


	/**
	 * Create raster with parameters of g without copy of data.
	 * @param g
	 */
	public RasterSubGrid(RasterSubGrid g) {
		super(g);
		this.start_x = g.start_x;
		this.start_y = g.start_y;
		this.border_x = g.border_x;
		this.border_y = g.border_y;
		this.data = new double[range_y][range_x];
	}

	public RasterSubGrid(AbstractRaster r, double[][] data, int start_x, int start_y, int border_x, int border_y) {
		super(r);
		this.start_x = start_x;
		this.start_y = start_y;
		this.border_x = border_x;
		this.border_y = border_y;
		this.data = data;
	}

	/**
	 * Create sub grid of g.
	 * @param g
	 * @param min_x
	 * @param min_y
	 * @param max_x
	 * @param max_y
	 */
	public RasterSubGrid(RasterGrid g, int min_x, int min_y, int max_x, int  max_y) {
		super(min_x, min_y, max_x, max_y);
		if(min_x<g.local_min_x || min_y<g.local_min_y || max_x>g.local_max_x || max_y>g.local_max_y ) {
			throw new RuntimeException("rect is larger than RasterGrid source");
		}
		this.start_x = this.local_min_x-g.local_min_x;
		this.start_y = this.local_min_y-g.local_min_y;
		this.border_x = start_x+range_x;
		this.border_y = start_y+range_y;
		this.data = g.data;
	}

	/**
	 * Create full grid (for RasterGrid)
	 * @param min_x
	 * @param min_y
	 * @param max_x
	 * @param max_y
	 */
	public RasterSubGrid(int min_x, int min_y, int max_x, int  max_y) {
		super(min_x, min_y, max_x, max_y);
		this.start_x = 0;
		this.start_y = 0;
		this.border_x = start_x+range_x;
		this.border_y = start_y+range_y;
		this.data = new double[range_y][range_x];
	}



	/**
	 * Create subgrid of r wiht rect.
	 * @param r
	 * @param rect
	 * @return
	 */
	public static RasterSubGrid of(RasterGrid r, Rect rect) {
		int min_x = rect.getInteger_UTM_min_x();
		int min_y = rect.getInteger_UTM_min_y();
		int max_x = rect.getInteger_UTM_max_x();
		int max_y = rect.getInteger_UTM_max_y();
		if(min_x<r.local_min_x || min_y<r.local_min_y || max_x>r.local_max_x || max_y>r.local_max_y ) {
			throw new RuntimeException("rect is larger than RasterGrid source "+rect);
		}
		return new RasterSubGrid(r, min_x, min_y, max_x, max_y);
	}

	public void writeRawGrid(DataOutput out) throws IOException {
		Serialisation.writeSubArrayArrayBE(out, data, start_y, border_y, start_x, border_x);
	}

	@Override
	public String toString() {
		return "RasterSubGrid [start_x=" + start_x + ", start_y=" + start_y + ", border_x=" + border_x + ", border_y="
				+ border_y + ", data=" + ", meta=" + meta + ", min_x=" + local_min_x + ", min_y="
				+ local_min_y + ", max_x=" + local_max_x + ", max_y=" + local_max_y + ", range_x=" + range_x + ", range_y=" + range_y
				+ ", cell_count=" + cell_count + "]";
	}



	/**
	 * get slope of raster
	 * @return
	 */
	public RasterSubGrid toSlope() {
		int sizeX = data[0].length;
		int sizeY = data.length;
		int beginX = 1;
		int beginY = 1;
		int endX = sizeX - 2;
		int endY = sizeY - 2;
		double[][] slope = new double[sizeY][sizeX];
		for (int y = beginY; y<endY; y++) {
			double[] prow = data[y-1];
			double[] row = data[y];
			double[] nrow = data[y+1];
			double[] srow = slope[y];
			for (int x = beginX; x<=endX; x++) {
				//http://www.usna.edu/Users/oceano/pguth/md_help/html/demb1f3n.htm
				//Four neighbors to N, S, E, and W (excluding point itself)
				double dy = (prow[x]-nrow[x]);
				double dx = (row[x-1]-row[x+1]);
				srow[x] = Math.atan(Math.sqrt(dx*dx+dy*dy)/2);
			}
		}
		return new RasterSubGrid(this, slope, start_x, start_y, border_x, border_y);
	}
	
	//derived from http://www.gdal.org/gdaldem.html#gdaldem_roughness
	public RasterSubGrid toRoughness() {
		int w = data[0].length-1;
		int h = data.length-1;
		double[][] roughness = new double[data.length][data[0].length];
		double[] check = new double[8]; 
		for (int y = 1; y < h; y++) {
			double[] row0 = data[y - 1];
			double[] row1 = data[y];
			double[] row2 = data[y + 1];
			double[] rrow = roughness[y];
			for (int x = 1; x < w; x++) {
				double v = row1[x];
				check[0] = Math.abs(row0[x-1] - v);
				check[1] = Math.abs(row0[x] - v);
				check[2] = Math.abs(row0[x+1] - v);
				check[3] = Math.abs(row1[x-1] - v);
				check[4] = Math.abs(row1[x+1] - v);
				check[5] = Math.abs(row2[x-1] - v);
				check[6] = Math.abs(row2[x] - v);
				check[7] = Math.abs(row2[x+1] - v);
				double rv = Double.NEGATIVE_INFINITY;
				for (int i = 0; i < 8; i++) {
					if(rv < check[i]) {
						rv = check[i];
					}
				}
				rrow[x] += Double.isFinite(rv) ? rv : Double.NaN; 
			}
		}
		return new RasterSubGrid(this, roughness, start_x, start_y, border_x, border_y);
	}
	
	public static double sqr(double x) {
		return x * x;		
	}
	
	//Riley, Shawn & Degloria, Stephen & Elliot, S.D.. (1999). A Terrain Ruggedness Index that Quantifies Topographic Heterogeneity. Internation Journal of Science. 5. 23-27. 
	public RasterSubGrid toTerrainRuggednessIndex() {
		int w = data[0].length-1;
		int h = data.length-1;
		double[][] r = new double[data.length][data[0].length];
		for (int y = 1; y < h; y++) {
			double[] row0 = data[y - 1];
			double[] row1 = data[y];
			double[] row2 = data[y + 1];
			double[] rrow = r[y];
			for (int x = 1; x < w; x++) {
				double v = row1[x];
				double rv = sqr(v - row0[x-1]) + sqr(v - row0[x]) + sqr(v - row0[x+1]) + sqr(v - row1[x-1]) + sqr(v - row1[x+1]) + sqr(v - row2[x-1]) + sqr(v - row2[x]) + sqr(v - row2[x+1]);
				rrow[x] += Math.sqrt(rv); 
			}
		}
		return new RasterSubGrid(this, r, start_x, start_y, border_x, border_y);
	}
	
	//http://www.jennessent.com/downloads/tpi-poster-tnc_18x22.pdf
	public RasterSubGrid toTopographicPositionIndex() {
		int w = data[0].length-1;
		int h = data.length-1;
		double[][] r = new double[data.length][data[0].length];
		for (int y = 1; y < h; y++) {
			double[] row0 = data[y - 1];
			double[] row1 = data[y];
			double[] row2 = data[y + 1];
			double[] rrow = r[y];
			for (int x = 1; x < w; x++) {
				double mean = (row0[x-1]+row0[x]+row0[x+1]+row1[x-1]+row1[x+1]+row2[x-1]+row2[x]+row2[x+1]) / 8d;
				double v = row1[x];
				rrow[x] += v - mean; 
			}
		}
		return new RasterSubGrid(this, r, start_x, start_y, border_x, border_y);
	}	

	public RasterSubGrid toAspect() {
		int sizeX = data[0].length;
		int sizeY = data.length;
		int beginX = 1;
		int beginY = 1;
		int endX = sizeX - 2;
		int endY = sizeY - 2;
		double[][] slope = new double[sizeY][sizeX];
		for (int y = beginY; y<endY; y++) {
			double[] prow = data[y-1];
			double[] row = data[y];
			double[] nrow = data[y+1];
			double[] srow = slope[y];
			for (int x = beginX; x<=endX; x++) {
				//http://www.usna.edu/Users/oceano/pguth/md_help/html/demb1f3n.htm
				//Four neighbors to N, S, E, and W (excluding point itself)
				double dy = (prow[x]-nrow[x]);
				double dx = (row[x-1]-row[x+1]);
				srow[x] = Math.atan2(dx, dy);
			}
		}
		return new RasterSubGrid(this, slope, start_x, start_y, border_x, border_y);
	}



	public static RasterSubGrid ofSlope(RasterSubGrid r) {
		RasterSubGrid s = new RasterSubGrid(r);
		ofSlope(r,s);
		return s;
	}

	public static void ofSlope(RasterSubGrid r, RasterSubGrid s) {
		/*int start_x = r.start_x+1;
		int start_y = r.start_y+1;
		int border_x = r.border_x-1;
		int border_y = r.border_y-1;*/
		
		int start_x = 1;
		int start_y = 1;
		int border_x = r.range_x - 1;
		int border_y = r.range_y - 1;
		
		/*for (int loop = 0; loop < 10; loop++) {
			Timer.start("tan");*/
		for (int y = start_y; y<border_y; y++) {
			double[] prow = r.data[y-1];
			double[] row = r.data[y];
			double[] nrow = r.data[y+1];
			double[] srow = s.data[y];
			for (int x = start_x; x<border_x; x++) {
				//http://www.usna.edu/Users/oceano/pguth/md_help/html/demb1f3n.htm
				double dy = (prow[x]-nrow[x]);
				double dx = (row[x-1]-row[x+1]);
				srow[x] = Math.atan(Math.sqrt(dx*dx+dy*dy)/2);
			}
		}
		/*Logger.info(Timer.stopToString("tan"));
		}*/
	}

	public double aggregateMean() {
		double sum = 0;
		double cnt = 0;
		for (int y = start_y; y<border_y; y++) {
			double[] row = data[y];
			for (int x = start_x; x<border_x; x++) {
				double v = row[x];
				if(Double.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}
		return cnt==0? Double.NaN : (sum/cnt);
	}

	/**
	 * input: raster of aspects
	 * !!only mean of aspects, missing weight of slopes
	 * @return
	 */
	public double aggregateCircularMean() {
		double cxsum = 0;
		double cysum = 0;
		double cnt = 0;
		for (int y = start_y; y<border_y; y++) {
			double[] row = data[y];
			for (int x = start_x; x<border_x; x++) {
				double v = row[x];
				if(Double.isFinite(v)) {
					cysum += Math.sin(v);
					cxsum += Math.cos(v);
					cnt++;
				}
			}
		}
		return Math.atan2(cysum/cnt, cxsum/cnt);
	}

	/**
	 * input: rater of elevation
	 * @return
	 */
	public double aggregateAspectCircularMean() {
		int sizeX = data[0].length;
		int sizeY = data.length;
		int beginX = 1;
		int beginY = 1;
		int endX = sizeX - 2;
		int endY = sizeY - 2;
		double dysum = 0;
		double dxsum = 0;
		double cnt = 0;
		for (int y = beginY; y<endY; y++) {
			double[] prow = data[y-1];
			double[] row = data[y];
			double[] nrow = data[y+1];
			for (int x = beginX; x<=endX; x++) {
				dxsum += (prow[x]-nrow[x]);
				dysum += (row[x-1]-row[x+1]);
				cnt++;
			}
		}
		return Math.atan2(dysum/cnt, dxsum/cnt);
	}

	public double aggregateMin() {
		double min = Double.MAX_VALUE;
		for (int y = start_y; y<border_y; y++) {
			double[] row = data[y];
			for (int x = start_x; x<border_x; x++) {
				double v = row[x];
				if(Double.isFinite(v) && v<min) {
					min = v;
				}
			}
		}
		return min==Double.MAX_VALUE?Double.NaN:min;
	}

	public double aggregateMax() {
		double max = -Double.MAX_VALUE;
		for (int y = start_y; y<border_y; y++) {
			double[] row = data[y];
			for (int x = start_x; x<border_x; x++) {
				double v = row[x];
				if(Double.isFinite(v) && max<v) {
					max = v;
				}
			}
		}
		return max==-Double.MAX_VALUE?Double.NaN:max;
	}

	public double aggregateSD() {		
		double cnt = 0;
		double sum = 0;
		double qsum = 0;
		for (int y = start_y; y<border_y; y++) {
			double[] row = data[y];
			for (int x = start_x; x<border_x; x++) {
				double v = row[x];
				if(Double.isFinite(v)) {
					cnt++;
					sum += v;
					qsum += v*v;
				}
			}
		}
		return cnt==0? Double.NaN : Math.sqrt( (cnt*qsum - sum*sum) / (cnt*(cnt-1)) );
	}

	public OLSMultipleLinearRegression getRegression() {
		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
		int n=0;
		for (int y = start_y; y<border_y; y++) {
			double[] row = data[y];
			for (int x = start_x; x<border_x; x++) {
				double v = row[x];
				if(Double.isFinite(v)) {						
					n++;
				}
			}
		}
		double[] ry = new double[n];
		double[][] rx = new double[n][2];
		int i=0;
		for (int y = start_y; y<border_y; y++) {
			double[] row = data[y];
			for (int x = start_x; x<border_x; x++) {
				double v = row[x];
				if(Double.isFinite(v)) {
					rx[i][0] = x;
					rx[i][1] = y;
					ry[i] = v;
					i++;
				}
			}
		}
		regression.newSampleData(ry, rx);		
		return regression;
	}
	
	public double[] getRegressionParameters() {
		return getRegression().estimateRegressionParameters();
	}
	
	/**
	 * this raster minus r.
	 * if result value < 0 then 0.
	 * @param r
	 */
	public void minus_zero(RasterSubGrid r) {
		int sizeX = data[0].length;
		int sizeY = data.length;
		for (int y = 0; y < sizeY; y++) {
			double[] row = data[y];
			double[] rrow = r.data[y];
			for (int x = 0; x < sizeX; x++) {
				double v = row[x] - rrow[x];
				row[x] = v<0 ? 0 : v;
			}
		}
	}

	public RasterSubGrid copy() {
		double[][] d = new double[this.data.length][this.data[0].length];
		for (int i = 0; i < d.length; i++) {
			System.arraycopy(this.data[i], 0, d[i], 0, d[i].length);
		}
		return new RasterSubGrid(this, d, this.start_x, this.start_y, this.border_x, this.border_y);		
	}
	
	public double[][] copySubData() {
		double[][] d = new double[this.range_y][this.range_x];
		for (int i = 0; i < this.range_y; i++) {
			System.arraycopy(this.data[i + this.start_y], this.start_x, d[i], 0, this.range_x); 
		}
		return d;
	}
	
	public RasterSubGrid toSurfaceArea() {
		int w = data[0].length-1;
		int h = data.length-1;
		double[][] r = new double[data.length][data[0].length];
		for (int y = 1; y < h; y++) {
			double[] row0 = data[y - 1];
			double[] row1 = data[y];
			double[] row2 = data[y + 1];
			double[] rrow = r[y];
			for (int x = 1; x < w; x++) {
				double c00 = row0[x - 1];
				double c01 = row0[x];
				double c02 = row0[x + 1];
				
				double c10 = row1[x - 1];
				double v = row1[x];
				double c12 = row1[x + 1];
				
				double c20 = row2[x - 1];
				double c21 = row2[x];
				double c22 = row2[x + 1];
				
				//double d00 = Math.sqrt(2 + sqr(c00 - v)) / 2;
				double d01 = Math.sqrt(1 + sqr(c01 - v)) / 2;
				//double d02 = Math.sqrt(2 + sqr(c02 - v)) / 2;
				
				double d10 = Math.sqrt(1 + sqr(c10 - v)) / 2;
				double d12 = Math.sqrt(1 + sqr(c12 - v)) / 2;
				
				//double d20 = Math.sqrt(2 + sqr(c20 - v)) / 2;
				double d21 = Math.sqrt(1 + sqr(c21 - v)) / 2;
				//double d22 = Math.sqrt(2 + sqr(c22 - v)) / 2;
				
				double d00_01 = Math.sqrt(1 + sqr(c00 - c01)) / 2;
				double d01_02 = Math.sqrt(1 + sqr(c01 - c02)) / 2;
				double d02_12 = Math.sqrt(1 + sqr(c02 - c12)) / 2;
				
				double d12_22 = Math.sqrt(1 + sqr(c12 - c22)) / 2;
				double d21_22 = Math.sqrt(1 + sqr(c21 - c22)) / 2;
				double d20_21 = Math.sqrt(1 + sqr(c20 - c21)) / 2;
				double d10_20 = Math.sqrt(1 + sqr(c10 - c20)) / 2;
				double d00_10 = Math.sqrt(1 + sqr(c00 - c10)) / 2;
				
				double a1 = (d00_01 * d01) / 2;
				double a2 = (d01_02 * d01) / 2;				
				double a3 = (d00_10 * d10) / 2;
				double a4 = (d02_12 * d12) / 2;
				double a5 = (d10_20 * d10) / 2;
				double a6 = (d12_22 * d12) / 2;
				double a7 = (d20_21 * d21) / 2;
				double a8 = (d21_22 * d21) / 2;
				
				rrow[x] = a1 + a2 + a3 + a4 + a5 + a6 + a7 + a8;
			}
		}
		return new RasterSubGrid(this, r, start_x, start_y, border_x, border_y);
	}
	
	public double sum() {
		double sum = 0d;
		for (int y = start_y; y<border_y; y++) {
			double[] row = data[y];
			for (int x = start_x; x<border_x; x++) {
				double v = row[x];
				if(Double.isFinite(v)) {
					sum += v;
				}
			}
		}
		return sum;
	}
	
	public int count() {
		int cnt = 0;
		for (int y = start_y; y<border_y; y++) {
			double[] row = data[y];
			for (int x = start_x; x<border_x; x++) {
				double v = row[x];
				if(Double.isFinite(v)) {
					cnt++;
				}
			}
		}
		return cnt;
	}
	
	
}
