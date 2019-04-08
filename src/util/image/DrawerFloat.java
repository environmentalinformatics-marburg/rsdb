package util.image;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Phaser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DrawerFloat {
	static final Logger log = LogManager.getLogger();
	
	public static void drawGrey3(ImageBufferARGB image, float[][] data, float[] lut) {
		int yStart = 0;
		int yEnd = image.height - 1;
		drawGrey3(image, data, lut, yStart, yEnd);
	}
	
	public static void drawRGB3(ImageBufferARGB image, float[][] rdata, float[][] gdata, float[][] bdata, float[] rlut, float[] glut, float[] blut) {
		int yStart = 0;
		int yEnd = image.height - 1;
		drawRGB3(image, rdata, gdata, bdata, rlut, glut, blut, yStart, yEnd);
	}
	
	public static void drawRB3(ImageBufferARGB image, float[][] rdata, float[][] bdata, float[] rlut, float[] blut) {
		int yStart = 0;
		int yEnd = image.height - 1;
		drawRB3(image, rdata, bdata, rlut, blut, yStart, yEnd);
	}
	
	public static void drawGrey3Parallel(ImageBufferARGB image, float[][] data, float[] lut) {
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
	
	public static void drawPalette3Parallel(ImageBufferARGB image, float[][] data, float[] lut, int[] palette) {
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
	
	public static void drawRGB3Parallel(ImageBufferARGB image, float[][] rdata, float[][] gdata, float[][] bdata, float[] rlut, float[] glut, float[] blut) {
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
	
	public static void drawRB3Parallel(ImageBufferARGB image, float[][] rdata, float[][] bdata, float[] rlut, float[] blut) {
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
				drawRB3(image, rdata, bdata, rlut, blut, yStartLocal, yEndLocal);
				phaser.arrive();
			});
			yStart = yEnd + 1;
			partNr++;
		} while(yEnd < end);
		phaser.arriveAndAwaitAdvance();		
	}
	
	public static void drawGrey3(ImageBufferARGB image, float[][] data, float[] lut, int yStart, int yEnd) {
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			float[] row = data[y];
			for (int x = 0; x < width; x++) {				
				float v = row[x];
				if(Float.isFinite(v)) {
					int p = Lut.match256f(lut, v);
					target[pos++] = 0xff000000 | (p << 16) | (p << 8) | p;
				} else {
					target[pos++] = 0; // transparent
				}
			}
		}		
	}
	
	public static void drawPalette3(ImageBufferARGB image, float[][] data, float[] lut, int[] palette, int yStart, int yEnd) {
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			float[] row = data[y];
			for (int x = 0; x < width; x++) {				
				float v = row[x];
				if(Float.isFinite(v)) {
					int p = Lut.match256f(lut, v);
					target[pos++] = palette[p];
				} else {
					target[pos++] = 0; // transparent
				}
			}
		}		
	}
	
	public static void drawRGB3(ImageBufferARGB image, float[][] rdata, float[][] gdata, float[][] bdata, float[] rlut, float[] glut, float[] blut, int yStart, int yEnd) {
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			float[] rrow = rdata[y];
			float[] grow = gdata[y];
			float[] brow = bdata[y];
			for (int x = 0; x < width; x++) {				
				float rv = rrow[x];
				float bv = brow[x];
				float gv = grow[x];
				if(Float.isFinite(rv) || Float.isFinite(gv) || Float.isFinite(bv)) {
					target[pos++] = 0xff000000 | (Lut.match256f(rlut, rv) << 16) | (Lut.match256f(glut, gv) << 8) | Lut.match256f(blut, bv);
				} else {
					target[pos++] = 0; // transparent
				}
			}
		}		
	}
	
	public static void drawRB3(ImageBufferARGB image, float[][] rdata, float[][] bdata, float[] rlut, float[] blut, int yStart, int yEnd) {
		int width = image.width;
		int yMax = image.height - 1;
		int[] target = image.data;
		int pos = (yMax - yEnd) * width;
		for (int y = yEnd; y >= yStart; y--) {
			float[] rrow = rdata[y];
			float[] brow = bdata[y];
			for (int x = 0; x < width; x++) {				
				float rv = rrow[x];
				float bv = brow[x];
				if(Float.isFinite(rv) || Float.isFinite(bv)) {
					int rp = Lut.match256f(rlut, rv);
					int bp = Lut.match256f(blut, bv);
					target[pos++] = 0xff000000 | (rp << 16) | (((rp + bp) >> 1) << 8) | bp;
				} else {
					target[pos++] = 0; // transparent
				}
			}
		}		
	}
}
