package pointdb;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;


import org.tinylog.Logger;

import pointdb.las.Las;
import pointdb.las.Laz;
import pointdb.base.PdbConst;
import pointdb.base.Point;
import pointdb.base.TileKey;

public class Loader {
	

	/**
	 * block size in points
	 */
	//private final static int MAX_BLOCK_SIZE = 10_000_000; //old db
	//private final static int MAX_BLOCK_SIZE = 20_000_000; // new db
	//private final static int MAX_BLOCK_SIZE = 5_000_000; // safe?
	private final static int MAX_BLOCK_SIZE = 1_000_000;

	private final PointDB pointdb;

	public Loader(PointDB pointdb) {
		this.pointdb = pointdb;
	}


	public long load3(Path filename) throws IOException {		
		Las las = null;
		Laz laz = null;
		boolean isLas = true;
		if(filename.toString().toLowerCase().endsWith("las")) {
			las = new Las(filename);
			Logger.info(las);
		} else if(filename.toString().toLowerCase().endsWith("laz")) {
			laz = new Laz(filename);
			isLas = false;
			Logger.info(laz);
		} else {
			throw new RuntimeException("unknown extension");
		}
		
		//Logger.info("utm offset "+las.offset[0]+"  "+las.offset[1]);
		//Logger.info("utm min "+las.min[0]+"  "+las.min[1]);
		
		double[] las_scale_factor = isLas ? las.scale_factor : laz.scale_factor;
		double[] las_offset = isLas ? las.offset : laz.offset;
		long las_number_of_point_records = isLas ? las.number_of_point_records : laz.number_of_point_records;
		double las_min_x = isLas ? las.min[0] : laz.min[0];
		double las_min_y = isLas ? las.min[1] : laz.min[1];
		double las_max_x = isLas ? las.max[0] : laz.max[0];
		double las_max_y = isLas ? las.max[1] : laz.max[1];

		/*  x center 500_000
			x range equator 167_000 to 833_000
			x db range 0 to 1_000_000


			y northern hemisphere 0 (equator) to 9_328_000 (pole)
			y southern hemisphere 0 (?) to 10_000_000
			y db range 0 to 10_000_000 */

		final double UTM_LIMIT_X_MIN = 0;
		final double UTM_LIMIT_Y_MIN = 0;
		final double UTM_LIMIT_X_MAX = 1_000_000;
		final double UTM_LIMIT_Y_MAX = 10_000_000;

		if(las_min_x<UTM_LIMIT_X_MIN) {
			Logger.warn("las_min_x out of UTM range "+las_min_x+" -> changed to "+UTM_LIMIT_X_MIN);
			las_min_x = UTM_LIMIT_X_MIN;
		}
		if(las_min_y<UTM_LIMIT_Y_MIN) {
			Logger.warn("las_min_y out of UTM range "+las_min_y+" -> changed to "+UTM_LIMIT_Y_MIN);
			las_min_y = UTM_LIMIT_Y_MIN;
		}
		if(UTM_LIMIT_X_MAX<las_max_x) {
			Logger.warn("las_max_x out of UTM range "+las_max_x+" -> changed to "+UTM_LIMIT_X_MAX);
			las_max_x = UTM_LIMIT_X_MAX;
		}
		if(UTM_LIMIT_Y_MAX<las_max_y) {
			Logger.warn("las_max_y out of UTM range "+las_max_y+" -> changed to "+UTM_LIMIT_Y_MAX);
			las_max_y = UTM_LIMIT_Y_MAX;
		}		

		int tile_min_x = (int) (las_min_x / PdbConst.UTM_TILE_SIZE);
		int tile_min_y = (int) (las_min_y / PdbConst.UTM_TILE_SIZE);
		int utm_min_x = tile_min_x * PdbConst.UTM_TILE_SIZE;
		int utm_min_y = tile_min_y * PdbConst.UTM_TILE_SIZE;
		double utm_diff_x = utm_min_x-las_offset[0];
		double utm_diff_y = utm_min_y-las_offset[1];
		double utm_diff_z = 0-las_offset[2]; // base zero


		int las_factor_x = calcScaleCorrection(las_scale_factor[0]); // las_factor    las coordinates * las_factor == db coordinates 
		int las_factor_y = calcScaleCorrection(las_scale_factor[1]);
		int las_factor_z = calcScaleCorrection(las_scale_factor[2]);

		int las_scale_x = PdbConst.LOCAL_SCALE_FACTOR / las_factor_x; // las_scale    las coordinates * las_scale == UTM coordinates
		int las_scale_y = PdbConst.LOCAL_SCALE_FACTOR / las_factor_y;
		int las_scale_z = PdbConst.LOCAL_SCALE_FACTOR / las_factor_z;

		int las_diff_x = (int) (utm_diff_x*las_scale_x);
		int las_diff_y = (int) (utm_diff_y*las_scale_y);
		int las_diff_z = (int) (utm_diff_z*las_scale_z);
		int tiles_x = ((int) ((las_max_x-utm_min_x)/PdbConst.UTM_TILE_SIZE))+1+1;
		int tiles_y = ((int) ((las_max_y-utm_min_y)/PdbConst.UTM_TILE_SIZE))+1+1;
		int loader_max_x = tiles_x*PdbConst.LOCAL_TILE_SIZE;
		int loader_max_y = tiles_y*PdbConst.LOCAL_TILE_SIZE;

		//Logger.info("las diff "+las_diff_x+"  "+las_diff_y);
		//Logger.info("tiles "+tiles_x+"  "+tiles_y);
		//Logger.info("loader max "+loader_max_x+"  "+loader_max_y);



		final long filePointCount = las_number_of_point_records;
		long curr_block_pos = 0;
		while(curr_block_pos<filePointCount) {
			int curr_block_size = (curr_block_pos+MAX_BLOCK_SIZE<=filePointCount)?MAX_BLOCK_SIZE:(int)(filePointCount-curr_block_pos);
			int[] intDiffs = new int[]{las_diff_x, las_diff_y, las_diff_z};
			int[] intFactors = new int[]{las_factor_x, las_factor_y, las_factor_z};
			Point[] block_points = isLas ? las.read(curr_block_pos, curr_block_size, intDiffs, intFactors) : laz.read(curr_block_pos, curr_block_size, intDiffs, intFactors);

			//Logger.info("points "+points.length);
			//Logger.info("point "+points[0]);

			filter(block_points, 0, 0, loader_max_x-1, loader_max_y-1);

			int block_tile_min_x = tile_min_x;
			int block_tile_min_y = tile_min_y;
			int block_tiles_x = tiles_x;
			int block_tiles_y = tiles_y;

			/*if(false) { // recalculate tile ranges
				int block_point_min_x = Integer.MAX_VALUE;
				int block_point_min_y = Integer.MAX_VALUE;
				int block_point_max_x = Integer.MIN_VALUE;
				int block_point_max_y = Integer.MIN_VALUE;
				for(Point p:block_points) {
					if(p!=null) {
						int x = p.x;
						int y = p.y;
						if(x<block_point_min_x) {
							block_point_min_x = x;
						}
						if(block_point_max_x<x) {
							block_point_max_x = x;
						}
						if(y<block_point_min_y) {
							block_point_min_y = y;
						}
						if(block_point_max_y<y) {
							block_point_max_y = y;
						}
					}
				}
				Logger.info("block "+block_point_min_x/1000+" "+block_point_min_y/1000+" "+block_point_max_x/1000+" "+block_point_max_y/1000);
			}*/

			loadBlock2(block_points,block_tile_min_x, block_tile_min_y, block_tiles_x, block_tiles_y);
			//loadBlock(points, offset_x, offset_y, local_offset_x, local_offset_y, local_offset_z);
			pointdb.commit();
			//System.gc();
			curr_block_pos+=curr_block_size;
		}

		return filePointCount;
	}


	private void loadBlock2(Point[] points, int tile_min_x, int tile_min_y, int tiles_x, int tiles_y) {
		Logger.info("loadBlock2 with "+tiles_x+" x "+tiles_y+" tiles");
		int[][] point_count = new int[tiles_y][tiles_x];

		for(Point p:points) {
			if(p!=null) {
				int pos_x = p.x/PdbConst.LOCAL_TILE_SIZE;
				int pos_y = p.y/PdbConst.LOCAL_TILE_SIZE;
				point_count[pos_y][pos_x]++;
			}
		}

		Point[][][] tile = new Point[tiles_y][tiles_x][];
		for(int y=0;y<tiles_y;y++) {
			for(int x=0;x<tiles_x;x++) {
				if(point_count[y][x]>0) {
					tile[y][x] = new Point[point_count[y][x]];
					//Logger.info(point_count[y][x]);
					point_count[y][x] = 0;
				}
			}
		}

		for(Point p:points) {
			if(p!=null) {
				int pos_x = p.x / PdbConst.LOCAL_TILE_SIZE;
				int pos_y = p.y / PdbConst.LOCAL_TILE_SIZE;
				int lx = p.x % PdbConst.LOCAL_TILE_SIZE;
				int ly = p.y % PdbConst.LOCAL_TILE_SIZE;
				Point db_point = Point.of(lx, ly, p);				
				tile[pos_y][pos_x][point_count[pos_y][pos_x]++] = db_point;
			}
		}

		for(int y=0;y<tiles_y;y++) {
			for(int x=0;x<tiles_x;x++) {
				Point[] tile_points = tile[y][x];
				if(tile_points!=null) {
					int key_x = (tile_min_x+x)*PdbConst.UTM_TILE_SIZE;
					int key_y = (tile_min_y+y)*PdbConst.UTM_TILE_SIZE;
					TileKey tileKey = new TileKey(key_x, key_y);
					//Logger.info("insert "+t.length+"  at "+tileKey);
					pointdb.insertPoints(tileKey, tile_points);					
				}
			}
		}

	}


	public int calcScaleCorrection(double lasScale) {
		final double DEFAULT_SCALE_1000 = 0.001d;
		final double SCALE_100 = 0.01d;
		final double SCALE_10 = 0.1d;
		final double SCALE_1 = 1d;
		if(lasScale==DEFAULT_SCALE_1000) {
			return 1;
		}
		if(lasScale==SCALE_100) {
			return 10;
		}
		if(lasScale==SCALE_10) {
			return 100;
		}
		if(lasScale==SCALE_1) {
			return 1000;
		} else {
			throw new RuntimeException("scale not implemented "+lasScale);
		}
	}


	public long load2(Path filename) throws IOException {
		Las las = null;
		Laz laz = null;
		boolean isLas = true;
		if(filename.toString().toLowerCase().endsWith("las")) {
			las = new Las(filename);
		} else if(filename.toString().toLowerCase().endsWith("laz")) {
			laz = new Laz(filename);
			isLas = false;
		} else {
			throw new RuntimeException("unknown extension");
		}

		double[] las_scale_factor = isLas ? las.scale_factor : laz.scale_factor;
		double[] las_offset = isLas ? las.offset : laz.offset;
		double[] las_min = isLas ? las.min : laz.min;
		double[] las_max = isLas ? las.max : laz.max;
		long las_number_of_point_records = isLas ? las.number_of_point_records : laz.number_of_point_records;

		Logger.info("scale "+Arrays.toString(las_scale_factor));
		final double DEFAULT_SCALE_1000 = 0.001d;
		int[] intScaleCorrection = null;
		if(las_scale_factor[0]!=DEFAULT_SCALE_1000 || las_scale_factor[1]!=DEFAULT_SCALE_1000 || las_scale_factor[2]!=DEFAULT_SCALE_1000) {
			try {
				intScaleCorrection = new int[]{calcScaleCorrection(las_scale_factor[0]), calcScaleCorrection(las_scale_factor[1]), calcScaleCorrection(las_scale_factor[2])};
			} catch(Exception e) {
				Logger.error("scale not implemented "+Arrays.toString(las_scale_factor));
				return -2;
			}
		}

		//offset in whole tiles
		final int offset_x = (int) (((long)(las_offset[0]*PdbConst.LOCAL_SCALE_FACTOR))/PdbConst.LOCAL_TILE_SIZE);
		final int offset_y = (int) (((long)(las_offset[1]*PdbConst.LOCAL_SCALE_FACTOR))/PdbConst.LOCAL_TILE_SIZE);

		//offset inside tile
		final int local_offset_x = (int) (((long)(las_offset[0]*PdbConst.LOCAL_SCALE_FACTOR))%PdbConst.LOCAL_TILE_SIZE);
		final int local_offset_y = (int) (((long)(las_offset[1]*PdbConst.LOCAL_SCALE_FACTOR))%PdbConst.LOCAL_TILE_SIZE);

		//no tiles in z
		final int local_offset_z = (int) (las_offset[2]*PdbConst.LOCAL_SCALE_FACTOR);

		final long filePointCount = las_number_of_point_records;
		long curr_block_pos = 0;
		while(curr_block_pos<filePointCount) {
			int curr_block_size = (curr_block_pos+MAX_BLOCK_SIZE<=filePointCount)?MAX_BLOCK_SIZE:(int)(filePointCount-curr_block_pos);
			Point[] points = isLas ? las.read(curr_block_pos, curr_block_size, null, intScaleCorrection) : laz.read(curr_block_pos, curr_block_size, null, intScaleCorrection);

			int local_min_x = (int) ((las_min[0]-las_offset[0]-1)*PdbConst.LOCAL_SCALE_FACTOR);
			int local_min_y = (int) ((las_min[1]-las_offset[1]-1)*PdbConst.LOCAL_SCALE_FACTOR);
			int local_max_x = (int) ((las_max[0]-las_offset[0]+1)*PdbConst.LOCAL_SCALE_FACTOR);
			int local_max_y = (int) ((las_max[1]-las_offset[1]+1)*PdbConst.LOCAL_SCALE_FACTOR);

			Logger.info("projection offset "+las_offset[0]+" "+las_offset[1]);
			Logger.info("projection range  "+las_min[0]+" "+las_min[1]+"  -  "+las_max[0]+" "+las_max[1]);


			filter(points, local_min_x, local_min_y, local_max_x, local_max_y);

			loadBlock(points, offset_x, offset_y, local_offset_x, local_offset_y, local_offset_z);
			pointdb.commit();
			//System.gc();
			curr_block_pos+=curr_block_size;
		}

		return filePointCount;
	}

	private void filter(Point[] points, int local_min_x, int local_min_y, int local_max_x, int local_max_y) {
		//Logger.info("filter range "+local_min_x+" "+local_min_y+"  -  "+local_max_x+" "+local_max_y);
		final int len = points.length;
		int removeCounter = 0;
		for (int i = 0; i < len; i++) {
			Point p = points[i];
			int px = p.x;
			int py = p.y;
			if(px<local_min_x || py<local_min_y || local_max_x<px || local_max_y<py) {
				//Logger.info("remove "+p);
				points[i] = null; // remove point
				removeCounter++;
			}
		}
		if(removeCounter>0) {
			Logger.info("removed points out of range: "+removeCounter);
		}
	}

	private static class Range {
		public final int min_x;
		public final int max_x;
		public final int min_y;
		public final int max_y;
		private Range(int min_x, int max_x, int min_y, int max_y) {
			this.min_x = min_x;
			this.max_x = max_x;
			this.min_y = min_y;
			this.max_y = max_y;
		}
		public static Range of(Point[] points) {
			int block_file_min_x = Integer.MAX_VALUE;
			int block_file_max_x = Integer.MIN_VALUE;
			int block_file_min_y = Integer.MAX_VALUE;
			int block_file_max_y = Integer.MIN_VALUE;
			for(int i=0;i<points.length;i++) {        //calc block file local min max x y
				Point p = points[i];
				if(p!=null) {
					if(p.x<block_file_min_x) {
						block_file_min_x = p.x;
					}
					if(block_file_max_x<p.x) {
						block_file_max_x = p.x;
					}
					if(p.y<block_file_min_y) {
						block_file_min_y = p.y;
					}
					if(block_file_max_y<p.y) {
						block_file_max_y = p.y;
					}
				}
			}
			return new Range(block_file_min_x, block_file_max_x, block_file_min_y, block_file_max_y);
		}
		public Range add(int x, int y) {
			return new Range(min_x+x, max_x+x, min_y+y, max_y+y);
		}
		public Range divCorr(int d) {
			final int c = d-1;
			return new Range((min_x<0?(min_x-c):min_x)/d, (max_x<0?(max_x-c):max_x)/d, (min_y<0?(min_y-c):min_y)/d, (max_y<0?(max_y-c):max_y)/d);
		}
		public int range_x() {
			return max_x+1-min_x;
		}
		public int range_y() {
			return max_y+1-min_y;
		}
		@Override
		public String toString() {
			return "Range [min_x=" + min_x + ", max_x=" + max_x + ", min_y=" + min_y + ", max_y=" + max_y + "]";
		}		
	}




	public void loadBlock(Point[] points, int offset_x, int offset_y, int local_offset_x, int local_offset_y, int local_offset_z) {
		final Range range = Range.of(points).add(local_offset_x, local_offset_y).divCorr(PdbConst.LOCAL_TILE_SIZE).add(offset_x, offset_y);
		//Logger.info(range);

		final int rx = range.range_x();
		final int ry = range.range_y();
		int[][] point_count = new int[rx][ry];
		for(Point point:points) {
			if(point!=null) {
				int dx = point.x+local_offset_x;
				int dy = point.y+local_offset_y;
				int x = (dx<0?(dx-PdbConst.LOCAL_TILE_SIZE_MINUS_ONE)/PdbConst.LOCAL_TILE_SIZE:dx/PdbConst.LOCAL_TILE_SIZE) + offset_x - range.min_x;
				int y = (dy<0?(dy-PdbConst.LOCAL_TILE_SIZE_MINUS_ONE)/PdbConst.LOCAL_TILE_SIZE:dy/PdbConst.LOCAL_TILE_SIZE) + offset_y - range.min_y;
				point_count[x][y]++;
			}
		}
		//Logger.info(Arrays.toString(point_count));
		Point[][][] tile = new Point[rx][ry][];
		for(int x=0;x<rx;x++) {
			for(int y=0;y<ry;y++) {
				tile[x][y] = new Point[point_count[x][y]];
				point_count[x][y] = 0;
			}
		}
		//Logger.info(Arrays.toString(tile));
		for(Point point:points) {
			if(point!=null) {
				int dx = point.x+local_offset_x;
				int dy = point.y+local_offset_y;
				int x = (dx<0?(dx-PdbConst.LOCAL_TILE_SIZE_MINUS_ONE)/PdbConst.LOCAL_TILE_SIZE:dx/PdbConst.LOCAL_TILE_SIZE) + offset_x - range.min_x;
				int y = (dy<0?(dy-PdbConst.LOCAL_TILE_SIZE_MINUS_ONE)/PdbConst.LOCAL_TILE_SIZE:dy/PdbConst.LOCAL_TILE_SIZE) + offset_y - range.min_y;
				int lx = dx<0?dx%PdbConst.LOCAL_TILE_SIZE + PdbConst.LOCAL_TILE_SIZE:dx%PdbConst.LOCAL_TILE_SIZE;
				int ly = dy<0?dy%PdbConst.LOCAL_TILE_SIZE + PdbConst.LOCAL_TILE_SIZE:dy%PdbConst.LOCAL_TILE_SIZE;
				int lz = local_offset_z+point.z;
				Point p = Point.of(lx, ly, lz, point);			
				tile[x][y][point_count[x][y]++] = p;
			}
		}
		//Logger.info(Arrays.toString(tile));
		for(int x=0;x<rx;x++) {
			for(int y=0;y<ry;y++) {
				Point[] t = tile[x][y];
				if(t.length>0) {
					int tx = x+range.min_x;
					int ty = y+range.min_y;
					int key_x = tx*PdbConst.UTM_TILE_SIZE;
					int key_y = ty*PdbConst.UTM_TILE_SIZE;
					TileKey tileKey = new TileKey(key_x, key_y);
					//Logger.info("insert "+t.length+"  at "+tileKey);
					pointdb.insertPoints(tileKey, t);
				}
			}
		}
	}



	/**
	 * Import one file into db.
	 * @param filename
	 * @return number of loaded points
	 * @throws IOException
	 */
	/*public long load(Path filename) throws IOException {
		Las las = new Las(filename);

		final double DEFAULT_SCALE = 0.001d;
		if(las.scale_factor[0]!=DEFAULT_SCALE || las.scale_factor[0]!=DEFAULT_SCALE || las.scale_factor[0]!=DEFAULT_SCALE) {
			Logger.error("scale not implemented "+Arrays.toString(las.scale_factor));
			return -2;
		}

		int file_tile_local_offset_x = (int) (((long)(las.offset[0]*PdbConst.LOCAL_SCALE_FACTOR))%PdbConst.LOCAL_TILE_SIZE);
		int file_tile_local_offset_y = (int) (((long)(las.offset[1]*PdbConst.LOCAL_SCALE_FACTOR))%PdbConst.LOCAL_TILE_SIZE);

		int file_local_offset_z = (int)(las.offset[2]*PdbConst.LOCAL_SCALE_FACTOR);

		int file_tile_offset_x = (int) (((long)las.offset[0])/PdbConst.UTM_TILE_SIZE);
		int file_tile_offset_y = (int) (((long)las.offset[1])/PdbConst.UTM_TILE_SIZE);

		//int zone_offset_x = (int) (((long)(las.offset[0]*LOCALE_SCALE_FACTOR))/LOCAL_TILE_SIZE);
		//int zone_offset_y = (int) (((long)(las.offset[1]*LOCALE_SCALE_FACTOR))/LOCAL_TILE_SIZE);
		//System.out.println("localoffset "+file_tile_local_offset_x+" "+file_tile_local_offset_y);
		//System.out.println("zone_offset "+file_tile_offset_x+" "+file_tile_offset_y);

		//long start = System.currentTimeMillis();
		//System.out.println("fill...");

		long curr_block_pos = 0;
		long filePointCount = las.number_of_point_records;
		//Logger.info("points "+filePointCount);
		while(curr_block_pos<filePointCount) {

			//Logger.info("next block..."+curr_block_pos);

			int curr_block_size = (curr_block_pos+MAX_BLOCK_SIZE<=filePointCount)?MAX_BLOCK_SIZE:(int)(filePointCount-curr_block_pos);
			Point[] points = las.read(curr_block_pos, curr_block_size);

			int block_file_min_x = Integer.MAX_VALUE;
			int block_file_max_x = Integer.MIN_VALUE;
			int block_file_min_y = Integer.MAX_VALUE;
			int block_file_max_y = Integer.MIN_VALUE;
			for(int i=0;i<points.length;i++) {        //calc block file local min max x y
				if(points[i].x<block_file_min_x) {
					block_file_min_x = points[i].x;
				}
				if(block_file_max_x<points[i].x) {
					block_file_max_x = points[i].x;
				}
				if(points[i].y<block_file_min_y) {
					block_file_min_y = points[i].y;
				}
				if(block_file_max_y<points[i].y) {
					block_file_max_y = points[i].y;
				}
			}
			//System.out.println("block file range: "+block_file_min_x+" "+block_file_max_x+" "+block_file_min_y+" "+block_file_max_y);

			int global_tile_min_x = file_tile_offset_x + (block_file_min_x+file_tile_local_offset_x)/PdbConst.LOCAL_TILE_SIZE;
			//global_tile_min_x--; //TODO check !!!
			int global_tile_max_x = file_tile_offset_x + (block_file_max_x+file_tile_local_offset_x)/PdbConst.LOCAL_TILE_SIZE;
			int global_tile_min_y = file_tile_offset_y + (block_file_min_y+file_tile_local_offset_y)/PdbConst.LOCAL_TILE_SIZE;
			//global_tile_min_y--; //TODO check !!!
			int global_tile_max_y = file_tile_offset_y + (block_file_max_y+file_tile_local_offset_y)/PdbConst.LOCAL_TILE_SIZE;
			int global_tile_range_x = global_tile_max_x-global_tile_min_x+1;
			int global_tile_range_y = global_tile_max_y-global_tile_min_y+1;
			//System.out.println("global tile range "+global_tile_min_x+" "+global_tile_max_x +" "+global_tile_min_y+" "+global_tile_max_y);			

			int[][] tile_point_counter = new int [global_tile_range_x][global_tile_range_y];

			for(int i=0;i<points.length;i++) {
				int global_tile_x = file_tile_offset_x + (points[i].x+file_tile_local_offset_x)/PdbConst.LOCAL_TILE_SIZE;
				int global_tile_y = file_tile_offset_y + (points[i].y+file_tile_local_offset_y)/PdbConst.LOCAL_TILE_SIZE;
				int tile_index_x = global_tile_x-global_tile_min_x;
				int tile_index_y = global_tile_y-global_tile_min_y;
				tile_point_counter[tile_index_x][tile_index_y]++;
			}

			int[][] tile_point_count = new int [global_tile_range_x][global_tile_range_y];
			Point[][][] tile = new Point[global_tile_range_x][global_tile_range_y][];
			for(int tile_index_x=0;tile_index_x<global_tile_range_x;tile_index_x++) {
				for(int tile_index_y=0;tile_index_y<global_tile_range_y;tile_index_y++) {
					tile[tile_index_x][tile_index_y] = new Point[tile_point_counter[tile_index_x][tile_index_y]];
				}
			}

			for(int i=0;i<points.length;i++) {
				int global_tile_x = file_tile_offset_x + (points[i].x+file_tile_local_offset_x)/PdbConst.LOCAL_TILE_SIZE;
				int global_tile_y = file_tile_offset_y + (points[i].y+file_tile_local_offset_y)/PdbConst.LOCAL_TILE_SIZE;
				int tile_index_x = global_tile_x-global_tile_min_x;
				int tile_index_y = global_tile_y-global_tile_min_y;
				int mod_x = (points[i].x+file_tile_local_offset_x)%PdbConst.LOCAL_TILE_SIZE;
				int mod_y = (points[i].y+file_tile_local_offset_y)%PdbConst.LOCAL_TILE_SIZE;
				int tile_local_x = mod_x>=0?mod_x:PdbConst.LOCAL_TILE_SIZE+mod_x;
				int tile_local_y = mod_y>=0?mod_y:PdbConst.LOCAL_TILE_SIZE+mod_y;

//				if(mod_x<0) {  // TODO check !!!
//					tile_index_x--;
//				}
//
//				if(mod_y<0) {  // TODO check !!!
//					tile_index_y--;
//				}

				Point tile_local_point = Point.of(tile_local_x, tile_local_y, file_local_offset_z+points[i].z, points[i]);
				tile[tile_index_x][tile_index_y][tile_point_count[tile_index_x][tile_index_y]++] = tile_local_point;


				if(PdbConst.DEBUG) {
					final double file_point_utm_x =  las.offset[0] + (((double)points[i].x)/PdbConst.LOCAL_SCALE_FACTOR);

					final int db_zoneID_x = global_tile_min_x+tile_index_x;
					final double db_offset_x = db_zoneID_x*PdbConst.UTM_TILE_SIZE;
					final double db_point_utm_x = db_offset_x  + (((double)tile_local_point.x)/PdbConst.LOCAL_SCALE_FACTOR);

					if( (db_point_utm_x != file_point_utm_x) ) {
						Logger.error("wrong point "+file_point_utm_x+"  ->  "+db_point_utm_x+"   file offset "+las.offset[0]+"  db offset "+db_offset_x+"   file raw point/1000 "+points[i].x/1000d+"   db raw point "+tile_local_point.x/1000d);
					}
				}
			}

//			for(int i=0;i<points.length;i++) {
//				int local_zone_x = (points[i].x+file_tile_local_offset_x)/LOCAL_TILE_SIZE;
//				int local_zone_y = (points[i].y+file_tile_local_offset_y)/LOCAL_TILE_SIZE;
//				int zone_index_x = local_zone_x-global_tile_min_x;
//				int zone_index_y = local_zone_y-global_tile_min_y;
//				Point localPoint = new Point((points[i].x+file_tile_local_offset_x)%LOCAL_TILE_SIZE,(points[i].y+file_tile_local_offset_y)%LOCAL_TILE_SIZE,points[i].z,points[i].intensity);
//				tile[zone_index_x][zone_index_y][tile_point_count[zone_index_x][zone_index_y]++] = localPoint;				
//			}

			for(int tile_index_x=0;tile_index_x<global_tile_range_x;tile_index_x++) {
				for(int tile_index_y=0;tile_index_y<global_tile_range_y;tile_index_y++) {
					if(tile[tile_index_x][tile_index_y].length>0) {
						int zoneID_x = global_tile_min_x+tile_index_x;
						int zoneID_y = global_tile_min_y+tile_index_y;
						int key_x = zoneID_x*PdbConst.UTM_TILE_SIZE;
						int key_y = zoneID_y*PdbConst.UTM_TILE_SIZE;
						//String zoneID = "UTM_37M_"+key_x+"_"+key_y;
						TileKey tileKey = new TileKey(key_x, key_y);
						pointdb.insertPoints(tileKey, tile[tile_index_x][tile_index_y]);

						//System.out.println("zoneID "+zoneID);


					}
				}
			}

			curr_block_pos+=curr_block_size;

			//Logger.info("commit...");
			pointdb.commit();
		}		

		//long end = System.currentTimeMillis();
		//System.out.println(Util.msToText(start,end)+" fill");
		return filePointCount;
	}*/

}
