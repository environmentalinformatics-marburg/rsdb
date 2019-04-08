package server.api.pointdb;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pointdb.processing.geopoint.RasterSubGrid;
import util.collections.vec.Vec;

public class Watershed {

	private static final Logger log = LogManager.getLogger();

	public static void main(String[] args) {

	}

	static class Pixel implements Comparable<Pixel>{

		public static final Pixel SENTINEL = new Pixel(Integer.MIN_VALUE,Integer.MIN_VALUE,Integer.MIN_VALUE);

		public static final int MASK = -2;
		public static final int INIT = -1;
		public static final int WSHED = 0;

		public final int x;
		public final int y;
		public final int v;
		public int label;
		public int distance;
		public Vec<Pixel> neighbors;

		public Pixel(int x, int y, int v) {
			this.x = x;
			this.y = y;
			this.v = v;
			this.label = INIT;
			this.distance = 0;
			this.neighbors = new Vec<Pixel>();
		}

		@Override
		public int compareTo(Pixel o) {			
			return Double.compare(this.v, o.v);
		}

		@Override
		public String toString() {
			return x+","+y+" h:" + v + " l:" + label;
		}		
	}

	private final RasterSubGrid chm;
	private final Pixel[][] pixels;
	private int hmin;
	private int hmax;
	private Map<Integer, Vec<Pixel>> pixelListMap;


	public Watershed(RasterSubGrid chm) {
		this.chm = chm;

		double[][] data = chm.data;
		//int xrange = chm.range_x;
		//int yrange = chm.range_y;
		int xstart = chm.start_x;
		int ystart = chm.start_y;
		int xborder = chm.border_x;
		int yborder = chm.border_y;
		this.pixels = new Pixel[yborder][xborder];

		hmin = Integer.MAX_VALUE;
		hmax = Integer.MIN_VALUE;
		for (int y = ystart; y < yborder ; y++) { // begin calc hmin hmax
			double[] data_row = data[y];
			for (int x = xstart; x < xborder; x++) {
				double raw = data_row[x];
				int v = Double.isNaN(raw)?Integer.MIN_VALUE:(int)raw;
				if(v!=Integer.MIN_VALUE && v<hmin) {
					hmin = v;
				}
				if(v!=Integer.MIN_VALUE && hmax<v) {
					hmax = v;
				}				
			}
		} // end calc hmin hmax

		//this.pixelList = new Vec<Pixel>(chm.cell_count);
		this.pixelListMap = new HashMap<Integer,Vec<Pixel>>();

		for (int y = ystart; y < yborder ; y++) { // begin create grid of pixels and list of pixels
			double[] data_row = data[y];
			for (int x = xstart; x < xborder; x++) {
				double raw = data_row[x];
				//int v = Double.isNaN(raw)?hmin:(int)raw;
				int v = Double.isNaN(raw)?hmax:hmax-((int)raw); // swap low and high				
				Pixel p = new Pixel(x, y, v);
				pixels[y][x] = p;
				Vec<Pixel> list = pixelListMap.get(v);
				if(list==null) {
					list = new Vec<Pixel>();
					pixelListMap.put(v, list);
				}
				list.add(p);
			}
		} // end create grid of pixels and list of pixels

		for (int y = ystart; y < yborder ; y++) { // begin set neighbours of pixel
			Pixel[] row_prev = y<=ystart?null:pixels[y-1];
			Pixel[] row = pixels[y];
			Pixel[] row_next = y==yborder-1?null:pixels[y+1];
			for (int x = xstart; x < xborder; x++) {
				Vec<Pixel> neighbours = row[x].neighbors;
				if(row_prev!=null) {
					if(x>xstart) {
						neighbours.add(row_prev[x-1]);
					}
					neighbours.add(row_prev[x]);
					if(x<xborder-1) {
						neighbours.add(row_prev[x+1]);
					}
				}
				if(x>xstart) {
					neighbours.add(row[x-1]);
				}
				if(x<xborder-1) {
					neighbours.add(row[x+1]);
				}
				if(row_next!=null) {
					if(x>xstart) {
						neighbours.add(row_next[x-1]);
					}
					neighbours.add(row_next[x]);
					if(x<xborder-1) {
						neighbours.add(row_next[x+1]);
					}
				}
			}
		} // end set neighbours of pixel


		//pixelList.sortThis(); // sort pixel list
		//log.info(pixelList);
	}

	public void run() {
		//flood();
		testFlood();
		writeOutput();
	}

	private void writeOutput() {
		double[][] data = chm.data;
		int xstart = chm.start_x;
		int ystart = chm.start_y;
		int xborder = chm.border_x;
		int yborder = chm.border_y;

		for (int y = ystart; y < yborder ; y++) { // fill output grid
			double[] data_row = data[y];
			Pixel[] pixel_row = pixels[y];
			for (int x = xstart; x < xborder; x++) {
				data_row[x] = pixel_row[x].label;
				//data_row[x] = pixel_row[x].v;
			}
		}

	}

	private void maskPixels(List<Pixel> pixelList, ArrayDeque<Pixel> queue) {
		for(Pixel p:pixelList) { // begin loop over pixels with height h				
			p.label = Pixel.MASK;
			//log.info("neighbors "+p.neighbors);
			for(Pixel q:p.neighbors) { // loop over neighbors of p
				if(q.label>=0) { // neighbor is labeled
					p.distance = 1;
					queue.add(p);
					break;
				}
			}
		} // end loop over pixels with height h	

	}

	private void testFlood() {
		//ArrayDeque<Pixel> queue = new ArrayDeque<Pixel>();

		int labelNr = 1;

		for (int h=hmin; h<=hmax; h++) {// begin loop over h
			List<Pixel> pixelList = pixelListMap.get(h);
			if(pixelList==null) {
				continue;
			}

			log.info("current height "+h);	

			for(Pixel p:pixelList) {
				int bases = 0;
				for(Pixel q:p.neighbors) {
					if(q.label>0 /*&& q.v<h*/) {
						if(p.label>0 && p.label!=q.label) {
							if(q.label>p.label) {
								p.label = q.label;
							}
							bases = 2;
						} else {
							p.label = q.label;
							bases = 1;
						}
					}
				}
				if(bases==0) {
					if((hmax-p.v)>10) {
						p.label = labelNr++;
					} else {
						p.label = 0;
					}
				} /*else if(bases==2) {
					p.label = Pixel.WSHED;
				}*/
			}





			/*maskPixels(pixelList, queue);

			while(!queue.isEmpty()) {
				Pixel p = queue.remove();
				for(Pixel q:p.neighbors) {
					if(q.label>=0) {

					}
				}
			}*/

		}// end loop over h
	}


	private void flood() {
		log.info("start watershed");		

		int current_label = 0;		
		ArrayDeque<Pixel> queue = new ArrayDeque<Pixel>();		

		log.info("loop over height "+hmin+"  "+hmax);

		for (int h=hmin; h<=hmax; h++) {// begin loop over h

			List<Pixel> pixelList = pixelListMap.get(h);
			if(pixelList==null) {
				continue;
			}

			log.info("current height "+h);			
			maskPixels(pixelList, queue);


			int current_dist = 1;
			queue.add(Pixel.SENTINEL);
			while(true) { // begin loop indefinitely
				Pixel p = queue.remove();
				if(p==Pixel.SENTINEL) {
					if(queue.isEmpty()) {
						break;
					} else {
						queue.add(Pixel.SENTINEL);
						current_dist++;
						p = queue.remove();
					}
				}

				for(Pixel p1:p.neighbors) {
					if(p1.distance<current_dist && (p1.label>0 || p1.label==Pixel.WSHED)) { //i.e., p1 belongs to an already labeled basin or to the watersheds
						if(p1.distance>0) {
							if(p.label == Pixel.MASK || p.label == Pixel.WSHED) {
								p.label = p1.label;
							} else if(p.label!=p1.label) {
								p.label = Pixel.WSHED;
							}
						} else if(p.label==Pixel.MASK) {
							p.label = Pixel.WSHED;
						}
					} else if(p1.label==Pixel.MASK && p1.distance==0) {
						p1.distance = current_dist+1;
						queue.add(p1);
					}
				}

			} // end loop indefinitely

			for(Pixel p:pixelList) { // checks if new minima have been discovered
				p.distance = 0;
				if(p.label == Pixel.MASK) {
					current_label++;
					queue.add(p);
					p.label = current_label;
					while(!queue.isEmpty()) {
						Pixel p1 = queue.remove();
						for(Pixel p2:pixelList) {
							if(p2.label==Pixel.MASK) {
								queue.add(p2);
								p2.label = current_label;
							}
						}
					}
				}
			}

		} // end loop over h		

		log.info("end watershed");
	}
}
