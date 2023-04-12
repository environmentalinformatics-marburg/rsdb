package rasterdb;

import java.io.IOException;

public class Rect2i {
	public final int xmin;
	public final int ymin;
	public final int xmax;
	public final int ymax;	

	public Rect2i(int xmin, int ymin, int xmax, int ymax) {
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
	}
	
	@FunctionalInterface
	public static interface TileRectConsumer {
		void accept(int xtile, int ytile, int xtilemax, int ytilemax, int xtmin, int ytmin, int xtmax, int ytmax);
	}
	
	@FunctionalInterface
	public static interface TileRectConsumerIO {
		void accept(int xtile, int ytile, int xtilemax, int ytilemax, int xtmin, int ytmin, int xtmax, int ytmax) throws IOException;
	}
	
	@FunctionalInterface
	public static interface TileRectConsumerThrows {
		void accept(int xtile, int ytile, int xtilemax, int ytilemax, int xtmin, int ytmin, int xtmax, int ytmax) throws Exception;
	}
	
	public void tiled(int xsize, int ysize, TileRectConsumer consumer) {
		int fxmax = xmax - xmin;
		int fymax = ymax - ymin;
		int xtilemax = fxmax / xsize;
		int ytilemax = fymax / ysize;
		for(int ytile = 0; ytile <= ytilemax;  ytile++) {
			for(int xtile = 0; xtile <= xtilemax;  xtile++) {
				int xtmin = xmin + xtile * xsize;
				int ytmin = ymin + ytile * ysize;
				int xtmax = xtmin + xsize - 1; 
				int ytmax = ytmin + ysize - 1;
				xtmax = xmax < xtmax ? xmax : xtmax;
				ytmax = ymax < ytmax ? ymax : ytmax;
				consumer.accept(xtile, ytile, xtilemax, ytilemax, xtmin, ytmin, xtmax, ytmax);
			}
		}
	}
	
	public void tiledIO(int xsize, int ysize, TileRectConsumerIO consumer) throws IOException {
		int fxmax = xmax - xmin;
		int fymax = ymax - ymin;
		int xtilemax = fxmax / xsize;
		int ytilemax = fymax / ysize;
		for(int ytile = 0; ytile <= ytilemax;  ytile++) {
			for(int xtile = 0; xtile <= xtilemax;  xtile++) {
				int xtmin = xmin + xtile * xsize;
				int ytmin = ymin + ytile * ysize;
				int xtmax = xtmin + xsize - 1; 
				int ytmax = ytmin + ysize - 1;
				xtmax = xmax < xtmax ? xmax : xtmax;
				ytmax = ymax < ytmax ? ymax : ytmax;
				consumer.accept(xtile, ytile, xtilemax, ytilemax, xtmin, ytmin, xtmax, ytmax);
			}
		}
	}
	
	public void tiledThrows(int xsize, int ysize, TileRectConsumerThrows consumer) throws Exception {
		int fxmax = xmax - xmin;
		int fymax = ymax - ymin;
		int xtilemax = fxmax / xsize;
		int ytilemax = fymax / ysize;
		for(int ytile = 0; ytile <= ytilemax;  ytile++) {
			for(int xtile = 0; xtile <= xtilemax;  xtile++) {
				int xtmin = xmin + xtile * xsize;
				int ytmin = ymin + ytile * ysize;
				int xtmax = xtmin + xsize - 1; 
				int ytmax = ytmin + ysize - 1;
				xtmax = xmax < xtmax ? xmax : xtmax;
				ytmax = ymax < ytmax ? ymax : ytmax;
				consumer.accept(xtile, ytile, xtilemax, ytilemax, xtmin, ytmin, xtmax, ytmax);
			}
		}
	}
}
