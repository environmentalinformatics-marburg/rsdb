package util.image;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Phaser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DrawerDouble {
	static final Logger log = LogManager.getLogger();
	
	public static void drawGrey(ImageBufferARGB image, double[][] data, double min, double max, double gamma) {
		int yStart = 0;
		int yEnd = image.height - 1;
		drawGrey(image, data, min, max, gamma, yStart, yEnd);
	}
	
	public static void drawGrey3(ImageBufferARGB image, double[][] data, double[] lut) {
		int yStart = 0;
		int yEnd = image.height - 1;
		drawGrey3(image, data, lut, yStart, yEnd);
	}
	
	public static void drawRGB(ImageBufferARGB image, double[][] rdata, double[][] gdata, double[][] bdata, double rmin, double rmax, double gmin, double gmax, double bmin, double bmax, double gamma) {
		int yStart = 0;
		int yEnd = image.height - 1;
		drawRGB(image, rdata, gdata, bdata, rmin, rmax, gmin, gmax, bmin, bmax, gamma, yStart, yEnd);
	}
	
	public static void drawRGB2(ImageBufferARGB image, double[][] rdata, double[][] gdata, double[][] bdata, float[] rlut, float[] glut, float[] blut) {
		int yStart = 0;
		int yEnd = image.height - 1;
		drawRGB2(image, rdata, gdata, bdata, rlut, glut, blut, yStart, yEnd);
	}
	
	public static void drawRGB3(ImageBufferARGB image, double[][] rdata, double[][] gdata, double[][] bdata, double[] rlut, double[] glut, double[] blut) {
		int yStart = 0;
		int yEnd = image.height - 1;
		drawRGB3(image, rdata, gdata, bdata, rlut, glut, blut, yStart, yEnd);
	}
	
	public static void drawRB(ImageBufferARGB image, double[][] rdata, double[][] bdata, double rmin, double rmax, double bmin, double bmax, double gamma) {
		int yStart = 0;
		int yEnd = image.height - 1;
		drawRB(image, rdata, bdata, rmin, rmax, bmin, bmax, gamma, yStart, yEnd);
	}
	
	public static void drawRB3(ImageBufferARGB image, double[][] rdata, double[][] bdata, double[] rlut, double[] blut) {
		int yStart = 0;
		int yEnd = image.height - 1;
		drawRB3(image, rdata, bdata, rlut, blut, yStart, yEnd);
	}
	
	public static void drawGreyParallel(ImageBufferARGB image, double[][] data, double min, double max, double gamma) {
		int parts = ForkJoinPool.commonPool().getParallelism();
		int part = image.height / parts;
		int end = image.height - 1;
		int yStart = 0;
		int yEnd = 0;
		int partNr = 1;
		Phaser phaser = new Phaser();
		phaser.register();
		ForkJoinPool exe = ForkJoinPool.commonPool();
		do {
			yEnd = yStart + part - 1;
			if(partNr == parts) {
				yEnd = end;
			}
			log.info("draw "+partNr+"/"+parts+"   "+yStart+"  "+yEnd+"  of "+end);
			phaser.register();
			final int yStartLocal = yStart;
			final int yEndLocal = yEnd;
			exe.execute(()->{
				drawGrey(image, data, min, max, gamma, yStartLocal, yEndLocal);
				phaser.arrive();
			});
			yStart = yEnd + 1;
			partNr++;
		} while(yEnd < end);
		phaser.arriveAndAwaitAdvance();		
	}
	
	public static void drawGrey3Parallel(ImageBufferARGB image, double[][] data, double[] lut) {
		int parts = ForkJoinPool.commonPool().getParallelism();
		int part = image.height / parts;
		int end = image.height - 1;
		int yStart = 0;
		int yEnd = 0;
		int partNr = 1;
		Phaser phaser = new Phaser();
		phaser.register();
		ForkJoinPool exe = ForkJoinPool.commonPool();
		do {
			yEnd = yStart + part - 1;
			if(partNr == parts) {
				yEnd = end;
			}
			log.info("draw "+partNr+"/"+parts+"   "+yStart+"  "+yEnd+"  of "+end);
			phaser.register();
			final int yStartLocal = yStart;
			final int yEndLocal = yEnd;
			exe.execute(()->{
				drawGrey3(image, data, lut, yStartLocal, yEndLocal);
				phaser.arrive();
			});
			yStart = yEnd + 1;
			partNr++;
		} while(yEnd < end);
		phaser.arriveAndAwaitAdvance();		
	}
	
	public static void drawPalette3Parallel(ImageBufferARGB image, double[][] data, double[] lut, int[] palette) {
		int parts = ForkJoinPool.commonPool().getParallelism();
		int part = image.height / parts;
		int end = image.height - 1;
		int yStart = 0;
		int yEnd = 0;
		int partNr = 1;
		Phaser phaser = new Phaser();
		phaser.register();
		ForkJoinPool exe = ForkJoinPool.commonPool();
		do {
			yEnd = yStart + part - 1;
			if(partNr == parts) {
				yEnd = end;
			}
			log.info("draw "+partNr+"/"+parts+"   "+yStart+"  "+yEnd+"  of "+end);
			phaser.register();
			final int yStartLocal = yStart;
			final int yEndLocal = yEnd;
			exe.execute(()->{
				drawPalette3(image, data, lut, palette, yStartLocal, yEndLocal);
				phaser.arrive();
			});
			yStart = yEnd + 1;
			partNr++;
		} while(yEnd < end);
		phaser.arriveAndAwaitAdvance();		
	}
	
	public static void drawRGBParallel(ImageBufferARGB image, double[][] rdata, double[][] gdata, double[][] bdata, double rmin, double rmax, double gmin, double gmax, double bmin, double bmax, double gamma) {
		int parts = ForkJoinPool.commonPool().getParallelism();
		int part = image.height / parts;
		int end = image.height - 1;
		int yStart = 0;
		int yEnd = 0;
		int partNr = 1;
		Phaser phaser = new Phaser();
		phaser.register();
		ForkJoinPool exe = ForkJoinPool.commonPool();
		do {
			yEnd = yStart + part - 1;
			if(partNr == parts) {
				yEnd = end;
			}
			log.info("draw "+partNr+"/"+parts+"   "+yStart+"  "+yEnd+"  of "+end);
			phaser.register();
			final int yStartLocal = yStart;
			final int yEndLocal = yEnd;
			exe.execute(()->{
				drawRGB(image, rdata, gdata, bdata, rmin, rmax, gmin, gmax, bmin, bmax, gamma, yStartLocal, yEndLocal);
				phaser.arrive();
			});
			yStart = yEnd + 1;
			partNr++;
		} while(yEnd < end);
		phaser.arriveAndAwaitAdvance();		
	}
	
	public static void drawRGB3Parallel(ImageBufferARGB image, double[][] rdata, double[][] gdata, double[][] bdata, double[] rlut, double[] glut, double[] blut) {
		int parts = ForkJoinPool.commonPool().getParallelism();
		int part = image.height / parts;
		int end = image.height - 1;
		int yStart = 0;
		int yEnd = 0;
		int partNr = 1;
		Phaser phaser = new Phaser();
		phaser.register();
		ForkJoinPool exe = ForkJoinPool.commonPool();
		do {
			yEnd = yStart + part - 1;
			if(partNr == parts) {
				yEnd = end;
			}
			log.info("draw "+partNr+"/"+parts+"   "+yStart+"  "+yEnd+"  of "+end);
			phaser.register();
			final int yStartLocal = yStart;
			final int yEndLocal = yEnd;
			exe.execute(()->{
				drawRGB3(image, rdata, gdata, bdata, rlut, glut, blut, yStartLocal, yEndLocal);
				phaser.arrive();
			});
			yStart = yEnd + 1;
			partNr++;
		} while(yEnd < end);
		phaser.arriveAndAwaitAdvance();		
	}
	
	public static void drawRBParallel(ImageBufferARGB image, double[][] rdata, double[][] bdata, double rmin, double rmax, double bmin, double bmax, double gamma) {
		int parts = ForkJoinPool.commonPool().getParallelism();
		int part = image.height / parts;
		int end = image.height - 1;
		int yStart = 0;
		int yEnd = 0;
		int partNr = 1;
		Phaser phaser = new Phaser();
		phaser.register();
		ForkJoinPool exe = ForkJoinPool.commonPool();
		do {
			yEnd = yStart + part - 1;
			if(partNr == parts) {
				yEnd = end;
			}
			log.info("draw "+partNr+"/"+parts+"   "+yStart+"  "+yEnd+"  of "+end);
			phaser.register();
			final int yStartLocal = yStart;
			final int yEndLocal = yEnd;
			exe.execute(()->{
				drawRB(image, rdata, bdata, rmin, rmax, bmin, bmax, gamma, yStartLocal, yEndLocal);
				phaser.arrive();
			});
			yStart = yEnd + 1;
			partNr++;
		} while(yEnd < end);
		phaser.arriveAndAwaitAdvance();		
	}
	
	public static void drawRB3Parallel(ImageBufferARGB image, double[][] rdata, double[][] bdata, double[] rlut, double[] blut) {
		int parts = ForkJoinPool.commonPool().getParallelism();
		int part = image.height / parts;
		int end = image.height - 1;
		int yStart = 0;
		int yEnd = 0;
		int partNr = 1;
		Phaser phaser = new Phaser();
		phaser.register();
		ForkJoinPool exe = ForkJoinPool.commonPool();
		do {
			yEnd = yStart + part - 1;
			if(partNr == parts) {
				yEnd = end;
			}
			log.info("draw "+partNr+"/"+parts+"   "+yStart+"  "+yEnd+"  of "+end);
			phaser.register();
			final int yStartLocal = yStart;
			final int yEndLocal = yEnd;
			exe.execute(()->{
				drawRB3(image, rdata, bdata, rlut, blut, yStartLocal, yEndLocal);
				phaser.arrive();
			});
			yStart = yEnd + 1;
			partNr++;
		} while(yEnd < end);
		phaser.arriveAndAwaitAdvance();		
	}
	
	public static void drawGrey(ImageBufferARGB image, double[][] data, double min, double max, double gamma, int yStart, int yEnd) {
		double range = max - min; 
		double ginv = 1f / gamma;
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			double[] row = data[y];
			for (int x = 0; x < width; x++) {				
				double v = row[x];
				if(!Double.isFinite(v)) {
					target[pos] = 0; // transparent
				} else {
					int r = 0;
					if(v<=min) {
						r = 0;
					} else if(max<=v) {
						r = 255;
					} else {						
						double sub = (v-min);
						double d = sub / range;
						double g = Math.pow(d, ginv);
						double f = 255d * g;
						r = (int) (f+0.5d);						
					}
					target[pos] = 0xff000000 | (r<<16) | (r<<8) | r;
				}
				pos++;
			}
		}
	}
	
	public static void drawGrey3(ImageBufferARGB image, double[][] data, double[] lut, int yStart, int yEnd) {
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			double[] row = data[y];
			for (int x = 0; x < width; x++) {				
				double v = row[x];
				if(Double.isFinite(v)) {
					int p = Lut.match256d(lut, v);
					target[pos++] = 0xff000000 | (p << 16) | (p << 8) | p;
				} else {
					target[pos++] = 0; // transparent
				}
			}
		}		
	}
	
	public static void drawPalette3(ImageBufferARGB image, double[][] data, double[] lut, int[] palette, int yStart, int yEnd) {
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			double[] row = data[y];
			for (int x = 0; x < width; x++) {				
				double v = row[x];
				if(Double.isFinite(v)) {
					int p = Lut.match256d(lut, v);
					target[pos++] = palette[p];
				} else {
					target[pos++] = 0; // transparent
				}
			}
		}		
	}
	
	public static void drawRGB(ImageBufferARGB image, double[][] rdata, double[][] gdata, double[][] bdata, double rmin, double rmax, double gmin, double gmax, double bmin, double bmax, double gamma, int yStart, int yEnd) {
		double rrange = rmax - rmin;
		double grange = gmax - gmin; 
		double brange = bmax - bmin; 
		double ginv = 1f / gamma;
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			double[] rrow = rdata[y];
			double[] grow = gdata[y];
			double[] brow = bdata[y];
			for (int x = 0; x < width; x++) {				
				double rv = rrow[x];
				double bv = brow[x];
				double gv = grow[x];
				if(!(Double.isFinite(rv) || Double.isFinite(gv) || Double.isFinite(bv))) {
					target[pos] = 0; // transparent
				} else {
					int rp = 0;
					if(rv<=rmin) {
						rp = 0;
					} else if(rmax<=rv) {
						rp = 255;
					} else {						
						double sub = (rv-rmin);
						double d = sub / rrange;
						double g = Math.pow(d, ginv);
						double f = 255d * g;
						rp = (int) (f+0.5d);						
					}
					
					int gp = 0;
					if(gv<=gmin) {
						gp = 0;
					} else if(gmax<=gv) {
						gp = 255;
					} else {						
						double sub = (gv-gmin);
						double d = sub / grange;
						double g = Math.pow(d, ginv);
						double f = 255d * g;
						gp = (int) (f+0.5d);						
					}
					
					int bp = 0;
					if(bv<=bmin) {
						bp = 0;
					} else if(bmax<=bv) {
						bp = 255;
					} else {						
						double sub = (bv-bmin);
						double d = sub / brange;
						double g = Math.pow(d, ginv);
						double f = 255d * g;
						bp = (int) (f+0.5d);						
					}
					
					target[pos] = 0xff000000 | (rp<<16) | (gp<<8) | bp;
				}
				pos++;
			}
		}		
	}
	
	public static void drawRGB2(ImageBufferARGB image, double[][] rdata, double[][] gdata, double[][] bdata, float[] rlut, float[] glut, float[] blut, int yStart, int yEnd) {
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			double[] rrow = rdata[y];
			double[] grow = gdata[y];
			double[] brow = bdata[y];
			for (int x = 0; x < width; x++) {				
				double rv = rrow[x];
				double bv = brow[x];
				double gv = grow[x];
				if(Double.isFinite(rv) || Double.isFinite(gv) || Double.isFinite(bv)) {
					target[pos++] = 0xff000000 | (Lut.match256f(rlut, (float) rv) << 16) | (Lut.match256f(glut, (float) gv) << 8) | Lut.match256f(blut, (float) bv);
				} else {
					target[pos++] = 0; // transparent
				}
			}
		}		
	}
	
	public static void drawRGB3(ImageBufferARGB image, double[][] rdata, double[][] gdata, double[][] bdata, double[] rlut, double[] glut, double[] blut, int yStart, int yEnd) {
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			double[] rrow = rdata[y];
			double[] grow = gdata[y];
			double[] brow = bdata[y];
			for (int x = 0; x < width; x++) {				
				double rv = rrow[x];
				double bv = brow[x];
				double gv = grow[x];
				if(Double.isFinite(rv) || Double.isFinite(gv) || Double.isFinite(bv)) {
					target[pos++] = 0xff000000 | (Lut.match256d(rlut, rv) << 16) | (Lut.match256d(glut, gv) << 8) | Lut.match256d(blut, bv);
				} else {
					target[pos++] = 0; // transparent
				}
			}
		}		
	}
	
	public static void drawRB(ImageBufferARGB image, double[][] rdata, double[][] bdata, double rmin, double rmax, double bmin, double bmax, double gamma, int yStart, int yEnd) {
		double rrange = rmax - rmin;
		double brange = bmax - bmin; 
		double ginv = 1f / gamma;
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			double[] rrow = rdata[y];
			double[] brow = bdata[y];
			for (int x = 0; x < width; x++) {				
				double rv = rrow[x];
				double bv = brow[x];
				if(!(Double.isFinite(rv) || Double.isFinite(bv))) {
					target[pos] = 0; // transparent
				} else {
					int rp = 0;
					if(rv<=rmin) {
						rp = 0;
					} else if(rmax<=rv) {
						rp = 255;
					} else {						
						double sub = (rv-rmin);
						double d = sub / rrange;
						double g = Math.pow(d, ginv);
						double f = 255d * g;
						rp = (int) (f+0.5d);						
					}
					
					int bp = 0;
					if(bv<=bmin) {
						bp = 0;
					} else if(bmax<=bv) {
						bp = 255;
					} else {						
						double sub = (bv-bmin);
						double d = sub / brange;
						double g = Math.pow(d, ginv);
						double f = 255d * g;
						bp = (int) (f+0.5d);						
					}
					
					target[pos] = 0xff000000 | (rp<<16) | (((rp + bp) >> 1)<<8) | bp;
				}
				pos++;
			}
		}		
	}
	
	public static void drawRB3(ImageBufferARGB image, double[][] rdata, double[][] bdata, double[] rlut, double[] blut, int yStart, int yEnd) {
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			double[] rrow = rdata[y];
			double[] brow = bdata[y];
			for (int x = 0; x < width; x++) {				
				double rv = rrow[x];
				double bv = brow[x];
				if(Double.isFinite(rv) || Double.isFinite(bv)) {
					int rp = Lut.match256d(rlut, rv);
					int bp = Lut.match256d(blut, bv);
					target[pos++] = 0xff000000 | (rp << 16) | (((rp + bp) >> 1) << 8) | bp;
				} else {
					target[pos++] = 0; // transparent
				}
			}
		}		
	}

}
