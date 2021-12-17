package server.api.pointdb;

import java.util.Arrays;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import org.tinylog.Logger;

import pointdb.base.GeoPoint;
import pointdb.base.Rect;
import pointdb.processing.geopoint.RasterGrid;
import util.Robust;
import util.collections.vec.Vec;

/**
 * Grid with 0.5 x 0.5 units cells
 *
 */
public class HalfGrid {
	

	private int xmin;
	private int xmax;
	private int ymin;
	private int ymax;
	private int xrange;
	private int yrange;
	private GeoPoint[][] grid;

	public HalfGrid(Rect rect) {
		rect = rect.outerMeterRect();
		this.xmin = (int) (rect.utmm_min_x/500d);
		this.xmax = (int) (rect.utmm_max_x/500d);
		this.xrange = xmax-xmin+1;
		this.ymin = (int) (rect.utmm_min_y/500d);
		this.ymax = (int) (rect.utmm_max_y/500d);
		this.yrange = ymax-ymin+1;

		this.grid = new GeoPoint[yrange][xrange];
	}

	public void insertMax(Vec<GeoPoint> points) {
		for(GeoPoint p:points) {
			int x = ((int) (p.x*2d))-xmin;
			int y = ((int) (p.y*2d))-ymin;

			if(grid[y][x]==null || grid[y][x].z<p.z) {
				grid[y][x] = p;
			}
		}		
	}

	public void insertMin(Vec<GeoPoint> points) {
		for(GeoPoint p:points) {
			int x = ((int) (p.x*2d))-xmin;
			int y = ((int) (p.y*2d))-ymin;

			if(grid[y][x]==null || grid[y][x].z>p.z) {
				grid[y][x] = p;
			}
		}		
	}

	public void retainMostLikely() {

		class Entry implements Comparable<Entry>{
			int x;
			int y;
			double v;
			@Override
			public int compareTo(Entry o) {
				return Double.compare(v, o.v);
			}
			public void set(int x, int y, double v) {
				this.x = x;
				this.y = y;
				this.v = v;
			}
		}

		Entry[] entries = new Entry[4];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = new Entry();
		}

		for(int y=0;y<yrange;y+=2) {
			for(int x=0;x<xrange;x+=2) {				
				GeoPoint pa = grid[y][x];
				GeoPoint pb = grid[y][x+1];
				GeoPoint pc = grid[y+1][x];
				GeoPoint pd = grid[y+1][x+1];
				if(pa==null||pb==null||pc==null||pd==null) {
					grid[y][x] = null;
					grid[y][x+1] = null;
					grid[y+1][x] = null;
					grid[y+1][x+1] = null;
				} else {
					entries[0].set(x, y, pa.z);
					entries[1].set(x+1, y, pb.z);
					entries[2].set(x, y+1, pc.z);
					entries[3].set(x+1, y+1, pd.z);
					Arrays.sort(entries);
					double a = entries[0].v;
					double b = entries[1].v;
					double c = entries[2].v;
					double d = entries[3].v;					
					double ab = Math.abs(a-b);
					double bc = Math.abs(b-c);
					double cd = Math.abs(c-d);

					if(ab<=bc) {
						if(ab<=cd) {
							//return (a+b)/2;
							Entry e1 = entries[2];
							Entry e2 = entries[3];
							grid[e1.y][e1.x] = null;
							grid[e2.y][e2.x] = null;
						} else {
							//return (c+d)/2;
							Entry e1 = entries[0];
							Entry e2 = entries[1];
							grid[e1.y][e1.x] = null;
							grid[e2.y][e2.x] = null;
						}
					} else {
						if(bc<=cd) {
							//return (b+c)/2;
							Entry e1 = entries[0];
							Entry e2 = entries[3];
							grid[e1.y][e1.x] = null;
							grid[e2.y][e2.x] = null;
						} else {
							//return (c+d)/2;
							Entry e1 = entries[0];
							Entry e2 = entries[1];
							grid[e1.y][e1.x] = null;
							grid[e2.y][e2.x] = null;
						}
					}
				}
			}
		}
	}

	public void removeNoneGrounds() {

		//double max_slope = 0.6d;
		double max_slope = 0.8d;
		//double max_slope = 1d;

		int window_size = 9;
		int xborder = xrange-window_size+1;
		int yborder = yrange-window_size+1;
		for(int y=0;y<yborder;y+=window_size) {
			for(int x=0;x<xborder;x+=window_size) {

				int wxborder = x+window_size;
				int wyborder = y+window_size;
				GeoPoint minP = GeoPoint.MAX_Z;
				int cnt=0;
				for(int wy=y;wy<wyborder;wy++) {
					for(int wx=x;wx<wxborder;wx++) {
						GeoPoint p = grid[wy][wx];
						if(p!=null) {
							cnt++;
							if(p.z<minP.z) {
								minP = p;
							}
						}
					}
				}

				//Logger.info("cnt"+cnt);

				if(cnt>=25) {

					for(int wy=y;wy<wyborder;wy++) {
						for(int wx=x;wx<wxborder;wx++) {
							GeoPoint p = grid[wy][wx];
							if(p!=null && minP.slope(p)>max_slope) {
								grid[wy][wx] = null;
							}
						}
					}

				} else {
					for(int wy=y;wy<wyborder;wy++) {
						for(int wx=x;wx<wxborder;wx++) {
							grid[wy][wx] = null;
						}
					}
				}

			}
		}
	}

	public RasterGrid get() {
		int oxmin = xmin/2;
		int oxmax = xmax/2;
		int oymin = ymin/2;
		int oymax = ymax/2;
		RasterGrid rasterGrid = new RasterGrid(oxmin, oxmax, oymin, oymax);
		//Logger.info("halfGrid "+this);
		//Logger.info("rasterGrid "+rasterGrid);
		int oxrange = rasterGrid.range_x;
		int oyrange = rasterGrid.range_y;

		double[][] data = rasterGrid.data;
		boolean[][] interpolated = new boolean[oyrange][oxrange];

		int MAX_ACC_COUNT = 100;
		double[] accX = new double[MAX_ACC_COUNT];
		double[] accY = new double[MAX_ACC_COUNT];
		double[] accZ = new double[MAX_ACC_COUNT];

		for(int y=0;y<oyrange;y++) {
			for(int x=0;x<oxrange;x++) {
				int cnt=0;
				double sum=0;
				int lx = x*2;
				int ly = y*2;
				if(grid[ly][lx]!=null) {
					sum += grid[ly][lx].z;
					cnt++;
				}
				if(grid[ly][lx+1]!=null) {
					sum += grid[ly][lx+1].z;
					cnt++;
				}
				if(grid[ly+1][lx]!=null) {
					sum += grid[ly+1][lx].z;
					cnt++;
				}
				if(grid[ly+1][lx+1]!=null) {
					sum += grid[ly+1][lx+1].z;
					cnt++;
				}
				data[y][x] = (cnt==0) ? Double.NaN : (sum/cnt);
			}
		}

		for(int y=0;y<oyrange;y++) {
			for(int x=0;x<oxrange;x++) {

				if(!Double.isNaN(data[y][x])) {
					continue;
				}

				for (int ws = 4; ws <= 12; ws++) {
					int cnt=0;
					double sum=0;

					int xa = x-ws;
					if(xa<0) xa=0;

					int xb = x+ws+1;
					if(xb>oxrange) xb = oxrange;

					int ya = y-ws;
					if(ya<0) ya=0;

					int yb = y+ws+1;
					if(yb>oyrange) yb = oyrange;

					cnt=0;
					sum=0;
					for(int iy=ya;iy<yb;iy++) {
						for(int ix=xa;ix<xb;ix++) {
							if((!interpolated[iy][ix]) && (!Double.isNaN(data[iy][ix]))) {
								sum += data[iy][ix];
								accX[cnt] = ix;
								accY[cnt] = iy;
								accZ[cnt] = data[iy][ix];
								cnt++;
							}
						}
					}

					if(cnt<8) {
						continue;
					}

					if(cnt>0) {
						if(cnt<4) {
							data[y][x] = sum/cnt;						
						} else {
							try {
								OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
								double[] ry = new double[cnt];
								double[][] rx = new double[cnt][2];
								for (int i = 0; i < cnt; i++) {
									ry[i] = accZ[i];
									rx[i][0] = accX[i];
									rx[i][1] = accY[i];
								}
								regression.newSampleData(ry, rx);
								double[] reg = regression.estimateRegressionParameters();

								if(Math.abs(reg[1])<1 && Math.abs(reg[2])<1) {
									data[y][x] = reg[0]+reg[1]*x+reg[2]*y;
								} else {
									data[y][x] = sum/cnt;
								}
							} catch(Exception e) {
								//Logger.warn(e);
								data[y][x] = sum/cnt;
							}
						}

						interpolated[y][x] = true;
						//Logger.info("ws "+ws);
						break;
					}
				}



			}
		}


		return rasterGrid;
	}


	public RasterGrid getRobust() {
		int oxmin = xmin/2;
		int oxmax = xmax/2-1;
		int oymin = ymin/2;
		int oymax = ymax/2-1;
		RasterGrid rasterGrid = new RasterGrid(oxmin, oxmax, oymin, oymax);
		int oxrange = rasterGrid.range_x;
		int oyrange = rasterGrid.range_y;

		boolean[][] interpolated = new boolean[oyrange][oxrange];

		double[][] data = rasterGrid.data;

		//Logger.info("grid "+oxrange+" "+oyrange);

		double[] acc = new double[25];
		for(int y=0;y<oyrange;y++) {
			for(int x=0;x<oxrange;x++) {
				int cnt=0;
				int lx = x*2;
				int ly = y*2;
				if(grid[ly][lx]!=null) {
					acc[cnt++]=grid[ly][lx].z;
				}
				if(grid[ly][lx+1]!=null) {
					acc[cnt++]= grid[ly][lx+1].z;
				}
				if(grid[ly+1][lx]!=null) {
					acc[cnt++]= grid[ly+1][lx].z;
				}
				if(grid[ly+1][lx+1]!=null) {
					acc[cnt++]= grid[ly+1][lx+1].z;
				}
				data[y][x] = Robust.getMostLikely(acc, cnt);
			}
		}




		for(int y=1;y<oyrange-1;y++) {
			for(int x=1;x<oxrange-1;x++) {

				if(true) {
					continue;
				}

				if(!Double.isNaN(data[y][x])) {
					continue;
				}

				int cnt = 0;
				if(!interpolated[y-1][x-1] && data[y-1][x-1]!=0) acc[cnt++] = data[y-1][x-1];
				if(!interpolated[y-1][x] && data[y-1][x]!=0) acc[cnt++] = data[y-1][x];
				if(!interpolated[y-1][x+1] && data[y-1][x+1]!=0) acc[cnt++] = data[y-1][x+1];
				if(!interpolated[y][x-1] && data[y][x-1]!=0) acc[cnt++] = data[y][x-1];
				//if(!interpolated[y][x] && data[y][x]!=0) acc[cnt++] = data[y][x];
				if(!interpolated[y][x+1] && data[y][x+1]!=0) acc[cnt++] = data[y][x+1];
				if(!interpolated[y+1][x-1] && data[y+1][x-1]!=0) acc[cnt++] = data[y+1][x-1];
				if(!interpolated[y+1][x] && data[y+1][x]!=0) acc[cnt++] = data[y+1][x];
				if(!interpolated[y+1][x+1] && data[y+1][x+1]!=0) acc[cnt++] = data[y+1][x+1];
				data[y][x] = Robust.getMostLikely(acc, cnt);

				if(!Double.isNaN(data[y][x])) {
					interpolated[y][x] = true;
					continue;
				}

				int xa = x-2;
				if(xa<0) xa=0;

				int xb = x+3;
				if(xb>oxrange) xb = oxrange;

				int ya = y-2;
				if(ya<0) ya=0;

				int yb = y+3;
				if(yb>oyrange) yb = oyrange;

				cnt=0;
				for(int iy=ya;iy<yb;iy++) {
					for(int ix=xa;ix<xb;ix++) {
						if(!interpolated[iy][ix] && (!Double.isNaN(data[y][x]))) acc[cnt++] = data[iy][ix];
					}
				}

				data[y][x] = Robust.getMostLikely(acc, cnt);
				if(!Double.isNaN(data[y][x])) {
					interpolated[y][x] = true;
					continue;
				}

				xa = x-3;
				if(xa<0) xa=0;

				xb = x+4;
				if(xb>oxrange) xb = oxrange;

				ya = y-3;
				if(ya<0) ya=0;

				yb = y+4;
				if(yb>oyrange) yb = oyrange;

				cnt=0;
				for(int iy=ya;iy<yb;iy++) {
					for(int ix=xa;ix<xb;ix++) {
						if(!interpolated[iy][ix] && (!Double.isNaN(data[y][x]))) acc[cnt++] = data[iy][ix];
					}
				}

				data[y][x] = Robust.getMostLikely(acc, cnt);
				if(!Double.isNaN(data[y][x])) {
					interpolated[y][x] = true;
					continue;
				}

			}
		}

		return rasterGrid;
	}

	public void removePeaks() {
		double max_diff = 0.5d;
		int yborder = yrange-1;
		int xborder = xrange-1;
		for(int y=1;y<yborder;y+=1) {
			for(int x=1;x<xborder;x+=1) {
				GeoPoint pa = grid[y][x-1];
				GeoPoint pb = grid[y][x];
				GeoPoint pc = grid[y][x+1];
				if(pa!=null&&pb!=null&&pc!=null) {
					double a = pa.z;
					double b = pb.z;
					double c = pc.z;
					if(a+max_diff<b&&c+max_diff<b) {
						grid[y][x] = null;
					}
				}
			}
		}

	}

	public void regCheck() {
		int win_size = 16;

		int cxrange = xrange/win_size+1;
		int cyrange = yrange/win_size+1;
		GeoPoint[][] coarseGrid = new GeoPoint[cyrange][cxrange];


		int yborder = yrange-win_size+1;
		int xborder = xrange-win_size+1;
		for(int y=0;y<yborder;y+=win_size) { // create coarseGrid
			for(int x=0;x<xborder;x+=win_size) {
				GeoPoint p = getWinMin(x, y, win_size);
				if(p!=GeoPoint.MAX_Z) {
					coarseGrid[y/win_size][x/win_size] = p;
				} else {
				}
			}
		}

		int cyborder = cyrange-1;
		int cxborder = cxrange-1;

		for(int y=1;y<cyborder;y+=1) { // fill gaps coarseGrid
			for(int x=1;x<cxborder;x+=1) {
				if(coarseGrid[y][x]==null) {
					double sum=0;
					int cnt=0;
					for(int wy=y-1;wy<=y+1;wy++) {
						GeoPoint[] row = coarseGrid[wy];
						for(int wx=x-1;wx<=x+1;wx++) {
							if(row[x]!=null) {
								sum += row[x].z;
								cnt++;
							}
						}
					}
					if(cnt>0) {
						GeoPoint p = GeoPoint.of((xmin+x*win_size)/2, (ymin+y*win_size)/2, sum/cnt);
						coarseGrid[y][x] = p;
					}
				} else {
					//Logger.info(coarseGrid[y][x]+" at "+(xmin+x*win_size)/2+" "+(ymin+y*win_size)/2);
				}
			}
		}



		for(int y=1;y<cyborder;y+=1) {
			windowLoop: for(int x=1;x<cxborder;x+=1) {
				OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();

				double[] ry = new double[9];
				double[][] rx = new double[9][2];
				int cnt=0;
				try {
					for(int wy=y-1;wy<=y+1;wy++) {
						GeoPoint[] row = coarseGrid[wy];
						for(int wx=x-1;wx<=x+1;wx++) {
							GeoPoint p = row[wx];
							if(p!=null) {
								rx[cnt][0] = p.x;
								rx[cnt][1] = p.y;
								ry[cnt] = p.z;
								cnt++;
							} else {
								// fill nulls ;
								continue windowLoop;
							}
						}
					}
					regression.newSampleData(ry, rx);
					double[] reg = regression.estimateRegressionParameters();

					checkWindow(x*win_size,y*win_size,win_size,reg);
				} catch(Exception e) {
					Logger.warn(e+"   "+cnt+"  "+Arrays.toString(ry));
				}

			}
		}
	}

	private void checkWindow(int xstart, int ystart, int win_size, double[] reg) {
		double max_diff = 1d;
		int xborder = xstart+win_size;
		int yborder = ystart+win_size;
		for(int y=ystart;y<yborder;y+=1) {
			GeoPoint[] row = grid[y];
			for(int x=xstart;x<xborder;x+=1) {
				GeoPoint p = row[x];
				if(p!=null) {
					double z = reg[0]+reg[1]*p.x+reg[2]*p.y;
					//Logger.info("z  "+z+"  "+p.z);
					if(Math.abs(p.z-z)>max_diff) {
						row[x] = null;
					}
				}
			}
		}

	}

	public GeoPoint getWinMin(int x, int y, int win_size) {
		return getWinMin(x, x+win_size, y, y+win_size);
	}

	public GeoPoint getWinMin(int xstart, int xborder, int ystart, int yborder) {
		GeoPoint p = GeoPoint.MAX_Z;
		int cnt=0;
		for(int y=ystart;y<yborder;y+=1) {
			GeoPoint[] row = grid[y];
			for(int x=xstart;x<xborder;x+=1) {
				GeoPoint q = row[x];
				if(q!=null) {
					cnt++;
					if(q.z<p.z) {
						p = q;
					}
				}
			}
		}
		return cnt<32?GeoPoint.MAX_Z:p;
	}

	public GeoPoint setWin(int xstart, int xborder, int ystart, int yborder, GeoPoint p) {
		for(int y=ystart;y<yborder;y+=1) {
			GeoPoint[] row = grid[y];
			for(int x=xstart;x<xborder;x+=1) {
				row[x] = p;
			}
		}
		return p;
	}

	@Override
	public String toString() {
		return "HalfGrid [xmin=" + xmin + ", xmax=" + xmax + ", ymin=" + ymin + ", ymax=" + ymax + ", xrange=" + xrange
				+ ", yrange=" + yrange + "]";
	}

}
