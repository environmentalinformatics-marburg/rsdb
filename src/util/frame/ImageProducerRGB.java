package util.frame;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Phaser;


import org.tinylog.Logger;

import util.Timer;
import util.Util;
import util.image.ImageRGBA;

public class ImageProducerRGB extends ImageRGBA {
	

	public final ShortFrame frameR;
	public final ShortFrame frameG;
	public final ShortFrame frameB;

	public final int width;
	public final int height;
	public final int len;

	public ImageProducerRGB(ShortFrame frameR, ShortFrame frameG, ShortFrame frameB) {
		super(frameR.width, frameR.height, true);
		this.frameR = frameR;
		this.frameG = frameG;
		this.frameB = frameB;

		this.width = frameR.width;
		this.height = frameR.height;
		this.len = width*height;
	}

	public ImageProducerRGB produceParallel() {
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
		final float ginv = 1f / gamma;

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

		short[][][] dataHolder = new short[3][][];

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

		int minR = 0;
		int maxR = 0;
		int minG = 0;
		int maxG = 0;
		int minB = 0;
		int maxB = 0;

		if(range != null) {
			minR = (int) range[0];
			maxR = (int) range[1];
			minG = minR;
			maxG = maxR;
			minB = minR;
			maxB = maxR;
			phaser.arriveAndAwaitAdvance();
		} else {		
			int[][] minmaxHolder = new int[3][];

			phaser.register(); 
			exe.execute(()->{
				minmaxHolder[0] = frameR.getMinMax0();
				phaser.arrive();
			});

			phaser.register(); 
			exe.execute(()->{
				minmaxHolder[1] = frameG.getMinMax0();
				phaser.arrive();
			});

			phaser.register(); 
			exe.execute(()->{
				minmaxHolder[2] = frameB.getMinMax0();
				phaser.arrive();
			});

			phaser.arriveAndAwaitAdvance();
			
			//minR = minmaxHolder[0][0];
			maxR = minmaxHolder[0][1];
			//minG = minmaxHolder[1][0];
			maxG = minmaxHolder[1][1];
			//minB = minmaxHolder[2][0];
			maxB = minmaxHolder[2][1];
		}

		int rangeR = maxR-minR;
		int rangeG = maxG-minG;
		int rangeB = maxB-minB;
		
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

	private void draw(short[][] dataR, short[][] dataG, short[][] dataB, int minR, int rangeR, int minG, int rangeG, int minB, int rangeB, float ginv) {
		//draw(dataR, dataG, dataB, minR, rangeR, minG, rangeG, minB, rangeB, ginv, 0, height); // run without parallel	

		int div = ForkJoinPool.commonPool().getParallelism();
		int part = height/div;

		/*for (int i = 0; i < div; i++) {
		int ystart = i*part;
		int yborder = i+1==div?height:ystart+part;
		draw(dataR, dataG, dataB, minR, rangeR, minG, rangeG, minB, rangeB, ginv, ystart, yborder);
	    }*/	

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

		/*Logger.info("RGB done "+exe.getPoolSize());
		Logger.info("PoolSize "+exe.getPoolSize());
		Logger.info("ActiveThreadCount "+exe.getActiveThreadCount());
		Logger.info("RunningThreadCount "+exe.getRunningThreadCount());
		Logger.info("Parallelism "+exe.getParallelism());
		Logger.info("QueuedSubmissionCount "+exe.getQueuedSubmissionCount());
		Logger.info("StealCount "+exe.getStealCount());
		Logger.info("QueuedTaskCount "+exe.getQueuedTaskCount());*/
	}

	private void draw(short[][] dataR, short[][] dataG, short[][] dataB, int minR, int rangeR, int minG, int rangeG, int minB, int rangeB, float ginv, int ystart, int yborder) {
		for(int y=ystart;y<yborder;y++) {
			short[] rowR = dataR[y];
			short[] rowG = dataG[y];
			short[] rowB = dataB[y];
			int offset = y*width;
			for(int x=0;x<width;x++) {
				int r = rowR[x];
				/*if(r!=0) {
					r = (r-minR)*255/rangeR;
				}
				if(r>255) {
					r=255;
				}*/
				if(r!=0) {
					r = (int) Math.round(255d*Math.pow((float)(r-minR) / rangeR, ginv));
					if(r>255) {
						r=255;
					}
				}


				int g = rowG[x];
				/*if(g!=0) {
					g = (g-minG)*255/rangeG;
				}
				if(g>255) {
					g=255;
				}*/
				if(g!=0) {
					g = (int) Math.round(255d*Math.pow((float)(g-minG) / rangeG, ginv));
					if(g>255) {
						g=255;
					}
				}

				int b = rowB[x];
				/*if(b!=0) {
					b = (b-minB)*255/rangeB;
				}
				if(b>255) {
					b=255;
				}*/
				if(b!=0) {
					b = (int) Math.round(255d*Math.pow((float)(b-minB) / rangeB, ginv));
					if(b>255) {
						b=255;
					}
				}

				imageBuffer[offset+x] = 0xff000000 | (r<<16) | (g<<8) | b;
			}
		}		
	}
	
	public void setValues(short[][] data, short source, int target) {
		for(int y=0;y<height;y++) {
			short[] row = data[y];
			int offset = y*width;
			for(int x=0;x<width;x++) {
				if(row[x] == source) {
					imageBuffer[offset+x] = target;
				}
			}
		}
	}
}
