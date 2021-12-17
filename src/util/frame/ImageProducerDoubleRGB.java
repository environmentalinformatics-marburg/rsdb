package util.frame;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Phaser;


import org.tinylog.Logger;

import util.Timer;
import util.Util;
import util.image.ImageRGBA;

public class ImageProducerDoubleRGB extends ImageRGBA {
	

	private final DoubleFrame frameR;
	private final DoubleFrame frameG;
	private final DoubleFrame frameB;

	public final int width;
	public final int height;
	public final int len;

	public ImageProducerDoubleRGB(DoubleFrame frameR, DoubleFrame frameG, DoubleFrame frameB) {
		super(frameR.width, frameR.height, true);
		this.frameR = frameR;
		this.frameG = frameG;
		this.frameB = frameB;

		this.width = frameR.width;
		this.height = frameR.height;
		this.len = width*height;
	}

	public ImageProducerDoubleRGB produceParallel() {
		produceParallel(2f, null);
		return this;
	}

	/**
	 * 
	 * @param gamma
	 * @param range nullable
	 */
	public void produceParallel(float gamma, double[] range) {
		Timer.start("render");
		final double ginv = 1.0 / gamma;

		if(imageBuffer==null||imageBuffer.length!=len) {
			throw new RuntimeException();
		}

		ForkJoinPool exe = ForkJoinPool.commonPool();
		Phaser phaser = new Phaser();
		phaser.register();

		phaser.register(); 
		exe.execute(()->{
			clearImage();
			phaser.arrive();
		});

		double[][][] dataHolder = new double[3][][];

		phaser.register(); 
		exe.execute(()->{
			dataHolder[0] = Util.flipRows(frameR.data);
			phaser.arrive();
		});

		phaser.register(); 
		exe.execute(()->{
			dataHolder[1] = Util.flipRows(frameG.data);
			phaser.arrive();
		});

		phaser.register(); 
		exe.execute(()->{
			dataHolder[2] = Util.flipRows(frameB.data);
			phaser.arrive();
		});

		double minR = 0;
		double maxR = 0;
		double minG = 0;
		double maxG = 0;
		double minB = 0;
		double maxB = 0;
		
		if(range != null) {
			minR = range[0];
			maxR = range[1];
			minG = minR;
			maxG = maxR;
			minB = minR;
			maxB = maxR;
			phaser.arriveAndAwaitAdvance();
		} else {

			double[][] minmaxHolder = new double[3][];

			phaser.register(); 
			exe.execute(()->{
				minmaxHolder[0] = frameR.getMinMax();
				phaser.arrive();
			});

			phaser.register(); 
			exe.execute(()->{
				minmaxHolder[1] = frameG.getMinMax();
				phaser.arrive();
			});

			phaser.register(); 
			exe.execute(()->{
				minmaxHolder[2] = frameB.getMinMax();
				phaser.arrive();
			});

			phaser.arriveAndAwaitAdvance();
			
			minR = minmaxHolder[0][0];
			maxR = minmaxHolder[0][1];
			minG = minmaxHolder[1][0];
			maxG = minmaxHolder[1][1];
			minB = minmaxHolder[2][0];
			maxB = minmaxHolder[2][1];
		}
		
		double rangeR = maxR-minR;
		double rangeG = maxG-minG;
		double rangeB = maxB-minB;
		
		if(rangeR==0) {
			rangeR=1;
		}		
		if(rangeG==0) {
			rangeG=1;
		}		
		if(rangeB==0) {
			rangeB=1;
		}

		draw(dataHolder[0], dataHolder[1], dataHolder[2], minR, rangeR, minG, rangeG, minB, rangeB, ginv);

		Logger.info(Timer.stop("render"));
	}

	private void draw(double[][] dataR, double[][] dataG, double[][] dataB, double minR, double rangeR, double minG, double rangeG, double minB, double rangeB, double ginv) {
		int div = ForkJoinPool.commonPool().getParallelism();
		int part = height/div;

		Phaser phaser = new Phaser();
		phaser.register();
		ForkJoinPool exe = ForkJoinPool.commonPool();

		for (int i = 0; i < div; i++) {
			int ystart = i*part;
			int yborder = i+1==div?height:ystart+part;
			phaser.register();
			exe.execute(()->{
				draw(dataR, dataG, dataB, minR, rangeR, minG, rangeG, minB, rangeB, ginv, ystart, yborder);
				phaser.arrive();
			});
		}
		phaser.arriveAndAwaitAdvance();
	}

	private void draw(double[][] dataR, double[][] dataG, double[][] dataB, double minR, double rangeR, double minG, double rangeG, double minB, double rangeB, double ginv, int ystart, int yborder) {
		for(int y=ystart;y<yborder;y++) {
			double[] rowR = dataR[y];
			double[] rowG = dataG[y];
			double[] rowB = dataB[y];
			int offset = y*width;
			for(int x=0;x<width;x++) {
				double r = rowR[x];
				if(r!=0) {
					r = (int) Math.round(255d*Math.pow((float)(r-minR) / rangeR, ginv));
					if(r>255) {
						r=255;
					}
				}
				double g = rowG[x];
				if(g!=0) {
					g = (int) Math.round(255d*Math.pow((float)(g-minG) / rangeG, ginv));
					if(g>255) {
						g=255;
					}
				}
				double b = rowB[x];
				if(b!=0) {
					b = (int) Math.round(255d*Math.pow((float)(b-minB) / rangeB, ginv));
					if(b>255) {
						b=255;
					}
				}
				imageBuffer[offset+x] = 0xff000000 | (((int)r)<<16) | (((int)g)<<8) | ((int)b);
			}
		}		
	}
}
