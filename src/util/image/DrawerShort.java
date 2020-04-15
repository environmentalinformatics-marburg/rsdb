package util.image;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Phaser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DrawerShort {
	static final Logger log = LogManager.getLogger();

	public static void drawGrey(ImageBufferARGB image, short[][] data, int na, int min, int max, double gamma) {
		int yStart = 0;
		int yEnd = image.height - 1;
		drawGrey(image, data, na, min, max, gamma, yStart, yEnd);
	}
	
	public static void drawGrey3(ImageBufferARGB image, short[][] data, short na, short[] lut) {
		int yStart = 0;
		int yEnd = image.height - 1;
		drawGrey3(image, data, na, lut, yStart, yEnd);
	}

	public static void drawRGB(ImageBufferARGB image, short[][] rdata, short[][] gdata, short[][] bdata, int na, int rmin, int rmax, int gmin, int gmax, int bmin, int bmax, double gamma) {
		int yStart = 0;
		int yEnd = image.height - 1;
		drawRGB(image, rdata, gdata, bdata, na, rmin, rmax, gmin, gmax, bmin, bmax, gamma, yStart, yEnd);
	}

	public static void drawRGB3(ImageBufferARGB image, short[][] rdata, short[][] gdata, short[][] bdata, short naR, short naG, short naB, short[] rlut, short[] glut, short[] blut) {
		int yStart = 0;
		int yEnd = image.height - 1;
		drawRGB3(image, rdata, gdata, bdata, naR, naG, naB, rlut, glut, blut, yStart, yEnd);
	}

	public static void drawRB(ImageBufferARGB image, short[][] rdata, short[][] bdata, int na, int rmin, int rmax, int bmin, int bmax, double gamma) {
		int yStart = 0;
		int yEnd = image.height - 1;
		drawRB(image, rdata, bdata, na, rmin, rmax, bmin, bmax, gamma, yStart, yEnd);
	}
	
	public static void drawRB3(ImageBufferARGB image, short[][] rdata, short[][] bdata, short naR, short naB, short[] rlut, short[] blut) {
		int yStart = 0;
		int yEnd = image.height - 1;
		drawRB3(image, rdata, bdata, naR, naB, rlut, blut, yStart, yEnd);
	}

	public static void drawGreyParallel(ImageBufferARGB image, short[][] data, int na, int min, int max, double gamma) {
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
				drawGrey(image, data, na, min, max, gamma, yStartLocal, yEndLocal);
				phaser.arrive();
			});
			yStart = yEnd + 1;
			partNr++;
		} while(yEnd < end);
		phaser.arriveAndAwaitAdvance();		
	}
	
	public static void drawGrey3Parallel(ImageBufferARGB image, short[][] data, short na, short[] lut) {
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
			//log.info("draw "+partNr+"/"+parts+"   "+yStart+"  "+yEnd+"  of "+end);
			phaser.register();
			final int yStartLocal = yStart;
			final int yEndLocal = yEnd;
			exe.execute(()->{
				drawGrey3(image, data, na, lut, yStartLocal, yEndLocal);
				phaser.arrive();
			});
			yStart = yEnd + 1;
			partNr++;
		} while(yEnd < end);
		phaser.arriveAndAwaitAdvance();		
	}
	
	public static void drawPalette3Parallel(ImageBufferARGB image, short[][] data, short na, short[] lut, int[] palette) {
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
			//log.info("draw "+partNr+"/"+parts+"   "+yStart+"  "+yEnd+"  of "+end);
			phaser.register();
			final int yStartLocal = yStart;
			final int yEndLocal = yEnd;
			exe.execute(()->{
				drawPalette3(image, data, na, lut, palette, yStartLocal, yEndLocal);
				phaser.arrive();
			});
			yStart = yEnd + 1;
			partNr++;
		} while(yEnd < end);
		phaser.arriveAndAwaitAdvance();		
	}

	public static void drawRGBParallel(ImageBufferARGB image, short[][] rdata, short[][] gdata, short[][] bdata, int na, int rmin, int rmax, int gmin, int gmax, int bmin, int bmax, double gamma) {
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
				drawRGB(image, rdata, gdata, bdata, na, rmin, rmax, gmin, gmax, bmin, bmax, gamma, yStartLocal, yEndLocal);
				phaser.arrive();
			});
			yStart = yEnd + 1;
			partNr++;
		} while(yEnd < end);
		phaser.arriveAndAwaitAdvance();		
	}

	public static void drawRGB3Parallel(ImageBufferARGB image, short[][] rdata, short[][] gdata, short[][] bdata, short naR, short naG, short naB, short[] rlut, short[] glut, short[] blut) {
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
			//log.info("draw "+partNr+"/"+parts+"   "+yStart+"  "+yEnd+"  of "+end);
			phaser.register();
			final int yStartLocal = yStart;
			final int yEndLocal = yEnd;
			exe.execute(()->{
				drawRGB3(image, rdata, gdata, bdata, naR, naG, naB, rlut, glut, blut, yStartLocal, yEndLocal);
				phaser.arrive();
			});
			yStart = yEnd + 1;
			partNr++;
		} while(yEnd < end);
		phaser.arriveAndAwaitAdvance();		
	}

	public static void drawRBParallel(ImageBufferARGB image, short[][] rdata, short[][] bdata, int na, int rmin, int rmax, int bmin, int bmax, double gamma) {
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
				drawRB(image, rdata, bdata, na, rmin, rmax, bmin, bmax, gamma, yStartLocal, yEndLocal);
				phaser.arrive();
			});
			yStart = yEnd + 1;
			partNr++;
		} while(yEnd < end);
		phaser.arriveAndAwaitAdvance();		
	}
	
	public static void drawRB3Parallel(ImageBufferARGB image, short[][] rdata, short[][] bdata, short naR, short naB, short[] rlut, short[] blut) {
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
				drawRB3(image, rdata, bdata, naR, naB, rlut, blut, yStartLocal, yEndLocal);
				phaser.arrive();
			});
			yStart = yEnd + 1;
			partNr++;
		} while(yEnd < end);
		phaser.arriveAndAwaitAdvance();		
	}

	public static void drawGrey(ImageBufferARGB image, short[][] data, int na, int min, int max, double gamma, int yStart, int yEnd) {
		double range = max - min; 
		double ginv = 1f / gamma;
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			short[] row = data[y];
			for (int x = 0; x < width; x++) {				
				int v = row[x];
				if(v == na) {
					target[pos] = 0; // transparent
				} else {
					int r = 0;
					if(v<=min) {
						r = 0;
					} else if(max<=v) {
						r = 255;
					} else {						
						int sub = (v-min);
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
	
	
	public static void drawGrey3(ImageBufferARGB image, short[][] data, short na, short[] lut, int yStart, int yEnd) {
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			short[] row = data[y];
			for (int x = 0; x < width; x++) {				
				short v = row[x];
				if(v != na) {
					int p = Lut.match256s(lut, v);
					target[pos++] = 0xff000000 | (p << 16) | (p << 8) | p;
				} else {
					target[pos++] = 0; // transparent
				}				
			}
		}
	}
	
	public static void drawPalette3(ImageBufferARGB image, short[][] data, short na, short[] lut, int[] palette, int yStart, int yEnd) {
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			short[] row = data[y];
			for (int x = 0; x < width; x++) {				
				short v = row[x];
				if(v != na) {
					int p = Lut.match256s(lut, v);
					target[pos++] = palette[p];
				} else {
					target[pos++] = 0; // transparent
				}				
			}
		}
	}
	

	public static void drawRGB(ImageBufferARGB image, short[][] rdata, short[][] gdata, short[][] bdata, int na, int rmin, int rmax, int gmin, int gmax, int bmin, int bmax, double gamma, int yStart, int yEnd) {
		double rrange = rmax - rmin;
		double grange = gmax - gmin; 
		double brange = bmax - bmin; 
		double gammainv = 1f / gamma;
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			short[] rrow = rdata[y];
			short[] grow = gdata[y];
			short[] brow = bdata[y];
			for (int x = 0; x < width; x++) {				
				int rv = rrow[x];
				int gv = grow[x];
				int bv = brow[x];
				if(rv == na && gv == na && bv == na) {
					target[pos] = 0; // transparent
				} else {
					int rp = 0;
					if(rv<=rmin) {
						rp = 0;
					} else if(rmax<=rv) {
						rp = 255;
					} else {						
						int sub = (rv-rmin);
						double div = sub / rrange;
						double gammav = Math.pow(div, gammainv);
						double scaled = 255d * gammav;
						rp = (int) (scaled+0.5d);						
					}

					int gp = 0;
					if(gv<=gmin) {
						gp = 0;
					} else if(gmax<=gv) {
						gp = 255;
					} else {						
						int sub = (gv-gmin);
						double div = sub / grange;
						double gammav = Math.pow(div, gammainv);
						double scaled = 255d * gammav;
						gp = (int) (scaled+0.5d);						
					}

					int bp = 0;
					if(bv<=bmin) {
						bp = 0;
					} else if(bmax<=bv) {
						bp = 255;
					} else {						
						int sub = (bv-bmin);
						double div = sub / brange;
						double gammav = Math.pow(div, gammainv);
						double scaled = 255d * gammav;
						bp = (int) (scaled+0.5d);						
					}					

					target[pos] = 0xff000000 | (rp<<16) | (gp<<8) | bp;
				}
				pos++;
			}
		}
	}

	public static void drawRGB3(ImageBufferARGB image, short[][] rdata, short[][] gdata, short[][] bdata, short naR, short naG, short naB, short[] rlut, short[] glut, short[] blut, int yStart, int yEnd) {
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			short[] rrow = rdata[y];
			short[] grow = gdata[y];
			short[] brow = bdata[y];
			for (int x = 0; x < width; x++) {				
				short rv = rrow[x];
				short gv = grow[x];
				short bv = brow[x];				
				if(rv != naR || gv != naG || bv != naB) {
					target[pos++] = 0xff000000 | (Lut.match256s(rlut, rv) << 16) | (Lut.match256s(glut, gv) << 8) | Lut.match256s(blut, bv);
				} else {
					target[pos++] = 0; // transparent
				}				
			}
		}
	}

	public static void drawRB(ImageBufferARGB image, short[][] rdata, short[][] bdata, int na, int rmin, int rmax, int bmin, int bmax, double gamma, int yStart, int yEnd) {
		double rrange = rmax - rmin;
		double brange = bmax - bmin; 
		double gammainv = 1f / gamma;
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			short[] rrow = rdata[y];
			short[] brow = bdata[y];
			for (int x = 0; x < width; x++) {				
				int rv = rrow[x];
				int bv = brow[x];
				if(rv == na && bv == na) {
					target[pos] = 0; // transparent
				} else {
					int rp = 0;
					if(rv<=rmin) {
						rp = 0;
					} else if(rmax<=rv) {
						rp = 255;
					} else {						
						int sub = (rv-rmin);
						double div = sub / rrange;
						double gammav = Math.pow(div, gammainv);
						double scaled = 255d * gammav;
						rp = (int) (scaled+0.5d);						
					}

					int bp = 0;
					if(bv<=bmin) {
						bp = 0;
					} else if(bmax<=bv) {
						bp = 255;
					} else {						
						int sub = (bv-bmin);
						double div = sub / brange;
						double gammav = Math.pow(div, gammainv);
						double scaled = 255d * gammav;
						bp = (int) (scaled+0.5d);						
					}					

					target[pos] = 0xff000000 | (rp << 16) | (((rp + bp) >> 1) << 8) | bp;
				}
				pos++;
			}
		}
	}

	public static void drawRB3(ImageBufferARGB image, short[][] rdata, short[][] bdata, short naR, short naB, short[] rlut, short[] blut, int yStart, int yEnd) {
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			short[] rrow = rdata[y];
			short[] brow = bdata[y];
			for (int x = 0; x < width; x++) {				
				short rv = rrow[x];
				short bv = brow[x];				
				if(rv != naR || bv != naB) {
					int rp = Lut.match256s(rlut, rv);
					int bp = Lut.match256s(blut, bv);
					target[pos++] = 0xff000000 | (rp << 16) | (((rp + bp) >> 1) << 8) | bp;
				} else {
					target[pos++] = 0; // transparent
				}				
			}
		}
	}
}
