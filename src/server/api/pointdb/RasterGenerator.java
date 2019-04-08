package server.api.pointdb;

import java.util.Collection;

import org.apache.commons.math3.linear.SingularMatrixException;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pointdb.base.GeoPoint;
import pointdb.base.Rect;
import pointdb.processing.geopoint.PointGrid;
import pointdb.processing.geopoint.RasterGrid;
import util.collections.vec.Vec;

public class RasterGenerator {
	private static final Logger log = LogManager.getLogger();

	public PointGrid pointGrid;
	public RasterGrid rasterGrid;
	Vec<GeoPoint>[][] grid;
	double[][] raster;
	int xrange;
	int yrange;

	public RasterGenerator(Rect rect, Collection<GeoPoint> points) {
		this(rect, PointGrid.of(rect, points));
	}

	public RasterGenerator(Rect rect, PointGrid pointGrid) {
		this.pointGrid = pointGrid;
		this.rasterGrid = RasterGrid.ofExtent(pointGrid);
		grid = pointGrid.grid;
		raster = rasterGrid.data;
		xrange = pointGrid.range_x;
		yrange = pointGrid.range_y;

	}

	public void run() {
		for(int y=0; y<yrange; y++) {
			for(int x=0; x<xrange; x++) {				
				//raster[y][x] = fillPixel(x, y);
				//raster[y][x] = fillPixelNN(x, y);
				raster[y][x] = fillPixelWeightedNN(x, y);
			}			
		}
		//rasterGrid = rasterGrid.toSmoothed();
	}

	public double fillPixelWeightedNN(int x, int y) {
		double xPos = x + 0.5d;
		double yPos = y + 0.5d;

		int wxbegin = x;
		int wybegin = y;
		int wxend = x;
		int wyend = y;

		double zSum = 0;
		double zCount = 0;
		double z = Double.NaN;
		double minDistance = Double.MAX_VALUE;
		int expansionCount = 0;
		if(wxbegin>0) wxbegin--;
		if(wybegin>0) wybegin--;
		if(wxend<xrange-1) wxend++;
		if(wyend<yrange-1) wyend++;
		while(zCount == 0d) {
			//log.info("round "+expansionCount);
			for (int wy = wybegin; wy <= wyend; wy++) {
				Vec<GeoPoint>[] wrow = grid[wy];
				for (int wx = wxbegin; wx <= wxend; wx++) {
					Vec<GeoPoint> points = wrow[wx];
					for(GeoPoint p:points) {
						double dx = xPos - p.x;
						double dy = yPos - p.y;						
						double distance = Math.sqrt(dx*dx + dy*dy);
						double weight = 1 / distance;
						zSum += p.z * weight;
						zCount += weight;
					}

				}			
			}
			if(expansionCount > 18) {
				break;
			}
			expansionCount++;
			if(wxbegin>0) wxbegin--;
			if(wybegin>0) wybegin--;
			if(wxend<xrange-1) wxend++;
			if(wyend<yrange-1) wyend++;			
		}
		if(zCount == 0d) {
			return Double.NaN;
		} else {
			return zSum / zCount;  
		}
	}

	public double fillPixelNN(int x, int y) {
		double xPos = x + 0.5d;
		double yPos = y + 0.5d;

		int wxbegin = x;
		int wybegin = y;
		int wxend = x;
		int wyend = y;

		double z = Double.NaN;
		double minDistance = Double.MAX_VALUE;
		int expansionCount = 0;
		if(wxbegin>0) wxbegin--;
		if(wybegin>0) wybegin--;
		if(wxend<xrange-1) wxend++;
		if(wyend<yrange-1) wyend++;
		while(Double.isNaN(z)) {
			//log.info("round "+expansionCount);
			for (int wy = wybegin; wy <= wyend; wy++) {
				Vec<GeoPoint>[] wrow = grid[wy];
				for (int wx = wxbegin; wx <= wxend; wx++) {
					Vec<GeoPoint> points = wrow[wx];
					for(GeoPoint p:points) {
						double dx = xPos - p.x;
						double dy = yPos - p.y;						
						double distance = Math.sqrt(dx*dx + dy*dy);
						if(distance<minDistance) {
							z = p.z;
							//log.info("OK");
						}
					}

				}			
			}
			if(expansionCount>10) {
				break;
			}
			expansionCount++;
			if(wxbegin>0) wxbegin--;
			if(wybegin>0) wybegin--;
			if(wxend<xrange-1) wxend++;
			if(wyend<yrange-1) wyend++;			
		}
		return z;
	}

	public double fillPixel(int x, int y) {	
		if(!grid[y][x].isEmpty()) {
			double sum=0;
			for(GeoPoint p:grid[y][x]) {
				sum += p.z;
			}
			return sum / grid[y][x].size();
		}

		int wxbegin = x;
		int wybegin = y;
		int wxend = x;
		int wyend = y;
		/*if(wxbegin>0) wxbegin--;
		if(wybegin>0) wybegin--;
		if(wxend<xrange-1) wxend++;
		if(wyend<yrange-1) wyend++;*/

		//int singularMatrixExceptionCount = 0;
		int expansionCount = 0;
		while(true) {
			expansionCount++;
			if(wxbegin>0) wxbegin--;
			if(wybegin>0) wybegin--;
			if(wxend<xrange-1) wxend++;
			if(wyend<yrange-1) wyend++;
			int cnt = pointGrid.countWindow(wxbegin, wybegin, wxend, wyend);
			if(cnt>0) {
				//log.info(cnt);
				if(cnt<7) {
					double sum = 0;
					for (int wy = wybegin; wy <= wyend; wy++) {
						Vec<GeoPoint>[] wrow = grid[wy];
						for (int wx = wxbegin; wx <= wxend; wx++) {
							/*for(GeoPoint p:wrow[wx]) {
								sum += p.z;
							}*/
							if(!wrow[wx].isEmpty()) {
								sum += wrow[wx].get(0).z;
							}
						}
					}
					return sum / cnt;
				} else {
					try {
						double[] ry = new double[cnt];
						double[][] rx = new double[cnt][2];
						int i = 0;
						for (int wy = wybegin; wy <= wyend; wy++) {
							Vec<GeoPoint>[] wrow = grid[wy];
							for (int wx = wxbegin; wx <= wxend; wx++) {
								/*for(GeoPoint p:wrow[wx]) {							
								double[] rxt = rx[i];
								rxt[0] = p.x;
								rxt[1] = p.y;
								ry[i] = p.z;
								i++;
							}*/
								if(!wrow[wx].isEmpty()) {
									double[] rxt = rx[i];
									GeoPoint p = wrow[wx].get(0);
									rxt[0] = p.x;
									rxt[1] = p.y;
									ry[i] = p.z;
									i++;
								}
							}
							OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
							regression.newSampleData(ry, rx);
							double[] reg = regression.estimateRegressionParameters();
							return reg[0]+reg[1]*rasterGrid.get_cell_x(x)+reg[2]*rasterGrid.get_cell_y(y);
						}
					}
					catch(SingularMatrixException e) {
						//singularMatrixExceptionCount++;
					} catch(Exception e) {
						log.warn(e);						
					}
				}
			}
			if(expansionCount>10) {
				return Double.NaN;
			}
		}		
		//return Double.NaN;
	}
}
