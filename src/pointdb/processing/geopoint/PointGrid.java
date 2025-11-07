package pointdb.processing.geopoint;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;


import org.tinylog.Logger;

import pointdb.base.GeoPoint;
import pointdb.base.Rect;
import util.collections.vec.Vec;

public class PointGrid extends AbstractCollection<GeoPoint> implements GeoPointConsumer {
	

	public static final int window_size = 5;

	public final int min_x;
	public final int min_y;
	public final int max_x;
	public final int max_y;
	public final int range_x;
	public final int range_y;

	public final Vec<GeoPoint>[][] grid; // grid[y][x];

	private PointGrid(int min_x, int min_y, int max_x, int max_y, int range_x, int range_y, Vec<GeoPoint>[][] grid) {
		this.min_x = min_x;
		this.min_y = min_y;
		this.max_x = max_x;
		this.max_y = max_y;
		this.range_x = range_x;
		this.range_y = range_y;
		this.grid = grid;
	}

	@SuppressWarnings("unchecked")
	public PointGrid(Rect rect) {
		this.min_x = rect.getInteger_UTM_min_x();
		this.min_y = rect.getInteger_UTM_min_y();
		this.max_x = rect.getInteger_UTM_max_x();
		this.max_y = rect.getInteger_UTM_max_y();
		this.range_x = max_x-min_x+1;
		this.range_y = max_y-min_y+1;
		this.grid = new Vec[range_y][range_x];
		for(Vec<GeoPoint>[] row:grid) {
			for (int i = 0; i < row.length; i++) {
				row[i] = new Vec<GeoPoint>();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public PointGrid(Rect rect, int initialCellCapacity) {
		this.min_x = rect.getInteger_UTM_min_x();
		this.min_y = rect.getInteger_UTM_min_y();
		this.max_x = rect.getInteger_UTM_max_x();
		this.max_y = rect.getInteger_UTM_max_y();
		this.range_x = max_x-min_x+1;
		this.range_y = max_y-min_y+1;
		this.grid = new Vec[range_y][range_x];
		for(Vec<GeoPoint>[] row:grid) {
			for (int i = 0; i < row.length; i++) {
				row[i] = new Vec<GeoPoint>(initialCellCapacity);
			}
		}
	}

	public static PointGrid of(Rect rect, Collection<GeoPoint> points) {
		PointGrid pointGrid = new PointGrid(rect);
		pointGrid.insert(points);
		return pointGrid;		
	}

	public void insert(Collection<GeoPoint> points) {
		for(GeoPoint p:points) {
			insert(p);
		}
	}

	public void insert(GeoPoint p) {
		int x = ((int) p.x)-min_x;
		int y = ((int) p.y)-min_y;
		grid[y][x].add(p);
	}

	/**
	 * For internal use.
	 * inserts point into grid.
	 * Implementation of GeoPointConsumer
	 */
	@Override
	public void nextGeoPoint(GeoPoint p) {
		insert(p);		
	}

	public void groundingSimple() {
		for(Vec<GeoPoint>[] row:grid) {
			for(Vec<GeoPoint> cell:row) {
				if(cell.size()<2) {
					continue;
				}
				GeoPoint minp = cell.get(0);
				int size = cell.size();
				for (int i = 1; i < size; i++) {
					GeoPoint p = cell.get(i);
					if(p.z<minp.z) {
						minp = p;
					}
				}
				cell.clear();
				cell.add(minp);
			}
		}
	}

	public Vec<GeoPoint> toList() {
		int border_x = range_x-window_size+1;
		int border_y = range_y-window_size+1;
		Vec<GeoPoint> result = new Vec<GeoPoint>();
		for (int y = window_size-1; y < border_y; y++) {
			Vec<GeoPoint>[] row = grid[y];
			for (int x = window_size-1; x < border_x; x++) {
				Logger.info("points in cell "+row[x].size());
				result.addAll(row[x]);
			}
		}
		/*for(ArrayList<GeoPoint>[] row:grid) {
			for(ArrayList<GeoPoint> cell:row) {
				//Logger.info(cell.size());
				result.addAll(cell);
			}
		}*/
		return result;
	}

	public PointSubGrid dynamicWindowSubGrid(int cell_x, int cell_y, int minPoints, int maxBorder) {
		for(int border=1;border<=maxBorder;border++) {
			if(countWindow(cell_x,cell_y,border)>=minPoints) {
				return windowSubGrid(cell_x, cell_y, border);
			}
		}
		return null;
	}


	public PointSubGrid windowSubGrid(int cell_x, int cell_y, int border) {
		int start_x = cell_x-border;
		if(start_x<0) {
			start_x = 0;
		}
		int start_y = cell_y-border;
		if(start_y<0) {
			start_y = 0;
		}
		int border_x = cell_x+border+1;
		if(border_x>range_x) {
			border_x = range_x;
		}
		int border_y = cell_y+border+1;
		if(border_y>range_y) {
			border_y = range_y;
		}		
		return new PointSubGrid(grid, start_x, start_y, border_x, border_y);		
	}

	/**
	 * Count points in window
	 * @param cell_x
	 * @param cell_y
	 * @param border
	 * @param outList
	 * @return
	 */
	public int countWindow(int cell_x, int cell_y, int border) {
		int cnt=0;
		int start_x = cell_x-border;
		if(start_x<0) {
			start_x = 0;
		}
		int start_y = cell_y-border;
		if(start_y<0) {
			start_y = 0;
		}
		int border_x = cell_x+border+1;
		if(border_x>range_x) {
			border_x = range_x;
		}
		int border_y = cell_y+border+1;
		if(border_y>range_y) {
			border_y = range_y;
		}
		for (int y = start_y; y < border_y; y++) {
			Vec<GeoPoint>[] row = grid[y];
			for (int x = start_x; x < border_x; x++) {
				cnt += row[x].size();
			}
		}
		return cnt;
	}

	public int countWindow(int wxbegin, int wybegin, int wxend, int wyend) {
		int cnt=0;
		for (int y = wybegin; y <= wyend; y++) {
			Vec<GeoPoint>[] row = grid[y];
			for (int x = wxbegin; x <= wxend; x++) {
				//cnt += row[x].size();
				if(!row[x].isEmpty()) {
					cnt++;
				}
			}
		}
		return cnt;		
	}

	public void removePositiveOutliers() {
		for(Vec<GeoPoint>[] row:grid) {
			for(Vec<GeoPoint> cell:row) {
				cell.sort(GeoPoint.Z_COMPARATOR_SAFE);
				int size = cell.size();
				if(size>0) {
					cell.remove(cell.size()-1);
				}
			}
		}
	}



	public void removeNegativeOutliers() {
		for(Vec<GeoPoint>[] row:grid) {
			for(Vec<GeoPoint> cell:row) {
				cell.sort(GeoPoint.Z_REVERSE_COMPARATOR_SAFE);

				/*if(cell.size()<5) {
					cell.clear();
					continue;
				}

				cell.remove(cell.size()-1);
				cell.remove(cell.size()-1);
				cell.remove(cell.size()-1);

				while(cell.size()>1) {
					int i = cell.size()-1;
					GeoPoint a = cell.get(i);
					GeoPoint b = cell.get(i-1);
					if(a.z+0.5d>b.z) {
						break;
					}
					cell.remove(i);
				}

				if(cell.size()==1) {
					cell.clear();
				}*/

				/*int size = cell.size();
				if(size>3) {
					cell.remove(cell.size()-1);
					cell.remove(cell.size()-1);
					cell.remove(cell.size()-1);
				} else {
					cell.clear();
				}*/



				int size = cell.size();
				if(size>0) {
					cell.remove(cell.size()-1);
				}
			}
		}
	}


	/**
	 * based on "Repetitive interpolation: A robust algorithm for DTM generation from Aerial Laser Scanner Data in forested terrain"
	 * @param maxSlope
	 */
	public void grounding(double maxSlope) {

		int base_border_x = range_x-window_size;
		int base_border_y = range_y-window_size;
		for(int base_y=0;base_y<base_border_y;base_y++) {
			for(int base_x=0;base_x<base_border_x;base_x++) {
				GeoPoint minp = null;
				int local_border_x = base_x+window_size+1;
				int local_border_y = base_y+window_size+1;

				for(int local_y=base_y;local_y<local_border_y;local_y++) {  //get minp
					for(int local_x=base_x;local_x<local_border_x;local_x++) {
						Vec<GeoPoint> cell = grid[local_y][local_x];
						int size = cell.size();
						for (int i = 1; i < size; i++) {
							GeoPoint p = cell.get(i);
							if(minp==null||p.z<minp.z) {
								minp = p;
							}
						}
					}
				}
				if(minp==null) {
					continue;
				}

				for(int local_y=base_y;local_y<local_border_y;local_y++) {  //get minp
					for(int local_x=base_x;local_x<local_border_x;local_x++) {
						Vec<GeoPoint> cell = grid[local_y][local_x];

						Iterator<GeoPoint> it = cell.iterator();
						while(it.hasNext()) {
							GeoPoint p = it.next();
							double slope = minp.slope(p);
							if(slope>maxSlope) {							
								it.remove();
							}
						}
					}
				}

			}
		}
	}

	public void getMinP(int x, int y) {

	}

	public void retainMax() {
		for(Vec<GeoPoint>[] row:grid) {			
			for(Vec<GeoPoint> cell:row) {
				int SIZE = cell.size();
				if(SIZE<2) {
					continue;
				}
				GeoPoint maxP = cell.get(0);
				for (int i = 0; i < SIZE; i++) {
					GeoPoint p = cell.get(i);
					if(maxP.z<p.z) {
						maxP = p;
					}
				}
				cell.clear();
				cell.add(maxP);
			}
		}		
	}

	public int cell_size() {
		return range_x*range_y;
	}

	public int filled_cell_size() {
		int cnt=0;
		for (int y = 0; y < range_y; y++) {
			Vec<GeoPoint>[] row = grid[y];
			for (int x = 0; x < range_x; x++) {
				Vec<GeoPoint> cell = row[x];
				if(!cell.isEmpty()) {
					cnt++;
				}
			}
		}
		return cnt;
	}

	public int empty_cell_size() {
		int cnt=0;
		for (int y = 0; y < range_y; y++) {
			Vec<GeoPoint>[] row = grid[y];
			for (int x = 0; x < range_x; x++) {
				Vec<GeoPoint> cell = row[x];
				if(cell.isEmpty()) {
					cnt++;
				}
			}
		}
		return cnt;
	}

	public int average_cell_point_size() {
		return size()/cell_size();
	}

	public int average_filled_cell_point_size() {
		return size()/filled_cell_size();
	}

	public void retainTop() {
		for(Vec<GeoPoint>[] row:grid) {
			for(Vec<GeoPoint> cell:row) {
				if(cell.isEmpty()) {
					continue;
				}else if(cell.size()==1) {
					cell.clear();
					continue;
				}
				cell.sort(GeoPoint.Z_REVERSE_COMPARATOR_SAFE);
				Iterator<GeoPoint> it = cell.iterator();
				GeoPoint topP = null;
				GeoPoint currP = it.next();
				while(it.hasNext()) {
					GeoPoint nextP = it.next();
					if(currP.z-0.5d<nextP.z) {
						if(it.hasNext()) {
							GeoPoint nextPrevP = nextP;
							nextP = it.next();
							if(nextPrevP.z-0.5d<nextP.z) {
								topP = currP;
								break;
							}
						}						
					}
					currP = nextP;
				}
				if(topP==null) {
					cell.clear();
				} else {
					cell.clear();
					cell.add(topP);
				}
			}
		}

	}

	public void retainGround() {
		for(Vec<GeoPoint>[] row:grid) {
			for(Vec<GeoPoint> cell:row) {
				if(cell.isEmpty()) {
					continue;
				}else if(cell.size()==1) {
					cell.clear();
					continue;
				}
				cell.sort(GeoPoint.Z_COMPARATOR_SAFE);
				Iterator<GeoPoint> it = cell.iterator();
				GeoPoint topP = null;
				GeoPoint currP = it.next();
				while(it.hasNext()) {
					GeoPoint nextP = it.next();
					if(currP.z+0.5d>nextP.z) {
						if(it.hasNext()) {
							GeoPoint nextPrevP = nextP;
							nextP = it.next();
							if(nextPrevP.z+0.5d>nextP.z) {
								topP = currP;
								break;
							}
						}						
					}
					currP = nextP;
				}
				if(topP==null) {
					cell.clear();
				} else {
					cell.clear();
					cell.add(topP);
				}
			}
		}

	}

	public void sortReverseZ() {
		for(Vec<GeoPoint>[] row:grid) {
			for(Vec<GeoPoint> cell:row) {
				if(cell.isEmpty()) {
					continue;
				}else if(cell.size()==1) {
					cell.clear();
					continue;
				}
				cell.sort(GeoPoint.Z_REVERSE_COMPARATOR_SAFE);
			}
		}

	}

	public Iterator<Vec<GeoPoint>> cellIterator() {
		return new CellIterator(grid, range_x, range_y);
	}

	public Collection<Vec<GeoPoint>> cells() {		
		return new AbstractCollection<Vec<GeoPoint>>() {
			@Override
			public Iterator<Vec<GeoPoint>> iterator() {
				return new CellIterator(grid, range_x, range_y);
			}
			@Override
			public int size() {
				return range_x*range_y;
			}
		};
	}

	@Override
	public Iterator<GeoPoint> iterator() {
		return new PointIterator(grid, range_x, range_y);
	}	

	private static class CellIterator implements Iterator<Vec<GeoPoint>> {

		private final Vec<GeoPoint>[][] grid;
		private final int border_x;
		private final int border_y;
		private int curr_x;
		private int curr_y;

		public CellIterator(Vec<GeoPoint>[][] grid, int border_x, int border_y) {
			super();
			this.grid = grid;
			this.border_x = border_x;
			this.border_y = border_y;
			this.curr_x = 0;
			this.curr_y = 0;
		}
		@Override
		public boolean hasNext() {
			return curr_y<border_y;
		}

		@Override
		public Vec<GeoPoint> next() {
			Vec<GeoPoint> p = grid[curr_y][curr_x++];
			if(curr_x==border_x) {
				curr_x = 0;
				curr_y++;
			}
			return p;
		}			
	};

	private static class PointIterator implements Iterator<GeoPoint> {

		private final Vec<GeoPoint>[][] grid;
		private final int border_x;
		private final int border_y;
		private int curr_x;
		private int curr_y;
		private Iterator<GeoPoint> curr_it;

		public PointIterator(Vec<GeoPoint>[][] grid, int border_x, int border_y) {
			this.grid = grid;
			this.border_x = border_x;
			this.border_y = border_y;
			this.curr_x = 0;
			this.curr_y = 0;
			next_curr_it();
		}
		@Override
		public boolean hasNext() {
			for(;;) {
				if(curr_it.hasNext()) {
					return true;
				}
				if(curr_y==border_y) {
					return false;
				}
				next_curr_it();
			}
		}

		private void next_curr_it() {
			curr_it = grid[curr_y][curr_x++].iterator();
			if(curr_x==border_x) {
				curr_x = 0;
				curr_y++;
			}
		}

		@Override
		public GeoPoint next() {
			return curr_it.next();
		}	
	}

	@Override
	public int size() {
		int sum=0;
		for (int y = 0; y < range_y; y++) {
			Vec<GeoPoint>[] row = grid[y];
			for (int x = 0; x < range_x; x++) {
				Vec<GeoPoint> cell = row[x];
				sum += cell.size();
			}
		}
		return sum;
	}

	public PointGrid copy() {
		@SuppressWarnings("unchecked")
		Vec<GeoPoint>[][] g = new Vec[range_y][range_x];
		for(int y=0;y<range_y;y++) {
			Vec<GeoPoint>[] row = grid[y];
			Vec<GeoPoint>[] grow = g[y];
			for(int x=0;x<range_x;x++) {
				grow[x] = row[x].copy();
			}
		}
		return new PointGrid(min_x, min_y, max_x, max_y, range_x, range_y, g); 
	}
}