package pointdb.processing.geopoint;

import java.util.Iterator;
import pointdb.base.GeoPoint;
import util.collections.vec.Vec;

public class PointSubGrid implements Iterable<GeoPoint> {
	
	private final Vec<GeoPoint>[][] grid;
	private final int start_x;
	private final int start_y;
	private final int border_x;
	private final int border_y;
	
	
	public PointSubGrid(Vec<GeoPoint>[][] grid, int start_x, int start_y, int border_x, int border_y) {
		this.grid = grid;
		this.start_x = start_x;
		this.start_y = start_y;
		this.border_x = border_x;
		this.border_y = border_y;
	}


	@Override
	public Iterator<GeoPoint> iterator() {
		return new PointIterator(grid, start_x, start_y, border_x, border_y);
	}
	
	public Iterator<Vec<GeoPoint>> cellIterator() {
		return new CellIterator(grid, start_x, start_y, border_x, border_y);
	}
	
	public Iterable<Vec<GeoPoint>> cells() {
		return ()->new CellIterator(grid, start_x, start_y, border_x, border_y);
	}
	
	/**
	 * Iterator over all points in window for correct function before each next hasNext needs to be called.
	 * @author woellauer
	 *
	 */
	private static class PointIterator implements Iterator<GeoPoint> {

		private final Vec<GeoPoint>[][] grid;
		private final int start_x;
		private final int border_x;
		private final int border_y;
		private int curr_x;
		private int curr_y;
		private Iterator<GeoPoint> curr_it;

		public PointIterator(Vec<GeoPoint>[][] grid, int start_x, int start_y, int border_x, int border_y) {
			this.grid = grid;
			this.start_x = start_x;
			this.border_x = border_x;
			this.border_y = border_y;
			this.curr_x = start_x;
			this.curr_y = start_y;
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
				curr_x = start_x;
				curr_y++;
			}
		}
		
		@Override
		public GeoPoint next() {
			return curr_it.next();
		}	
	}
	
	private static class CellIterator implements Iterator<Vec<GeoPoint>> {

		private final Vec<GeoPoint>[][] grid;
		private final int start_x;
		private final int border_x;
		private final int border_y;
		private int curr_x;
		private int curr_y;

		public CellIterator(Vec<GeoPoint>[][] grid, int start_x, int start_y, int border_x, int border_y) {
			super();
			this.grid = grid;
			this.start_x = start_x;
			this.border_x = border_x;
			this.border_y = border_y;
			this.curr_x = start_x;
			this.curr_y = start_y;
		}
		@Override
		public boolean hasNext() {
			return curr_y<border_y;
		}

		@Override
		public Vec<GeoPoint> next() {
			Vec<GeoPoint> p = grid[curr_y][curr_x++];
			if(curr_x==border_x) {
				curr_x = start_x;
				curr_y++;
			}
			return p;
		}			
	};	
}
