package util.frame;

import util.Util;
import util.image.ImageRGBA;
import util.image.MonoColor;

public class ImageProducerMono extends ImageRGBA {
	
	private final ShortFrame frame;

	private final int width;
	private final int height;
	private final int len;
	private final int[] valueRange; //nullable
	
	public ImageProducerMono(ShortFrame frame) {
		this(frame, null);
	}

	public ImageProducerMono(ShortFrame frame, int[] valueRange) {
		super(frame.width, frame.height);
		this.frame = frame;

		this.width = frame.width;
		this.height = frame.height;
		this.len = width*height;
		this.valueRange = valueRange;
	}

	public ImageProducerMono produce() {
		produce(2f);
		return this;
	}
	
	public void produce(float gamma) {
		final float ginv = 1f / gamma;
		
		if(imageBuffer==null||imageBuffer.length!=len) {
			throw new RuntimeException();
		}

		short[][] data = Util.flipRows(frame.data);
		
		clearImage();
		
		
		int min = 0;
		int max = 0;
		if(valueRange!=null) {
			min = valueRange[0];
			max = valueRange[1];
		} else {		
			int[] minmax = frame.getMinMax0();
			min = 0;
			//min = minmax[0];
			max = minmax[1];
		}

		int range = max-min;
		//range = range*7/8;
		if(range==0) {
			range=1;
		}		

		for(int y=0;y<height;y++) {
			short[] row = data[y];
			int offset = y*width;
			for(int x=0;x<width;x++) {
				int c = row[x];
				/*if(r!=0) {
					r = (r-minR)*255/rangeR;
				}
				if(r>255) {
					r=255;
				}*/
				if(c!=0) {
					c = (int) Math.round(255d*Math.pow((float)(c-min) / range, ginv));
					if(c>255) {
						c=255;
					}
				}

				/*int r = colR[c];
				int g = colG[c];
				int b = colB[c];
				imageBuffer[offset+x] = 0xff000000 | (r<<16) | (g<<8) | b;*/
				
				imageBuffer[offset+x] = MonoColor.colInferno[c];
			}
		}
	}
}
