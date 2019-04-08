package util.frame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.Util;
import util.image.ImageGrey;

public class ImageProducerGrey extends ImageGrey {
	@SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger();	
	
	private final ShortFrame frame;
	private final int len;
	private final int[] valueRange; //nullable

	public ImageProducerGrey(ShortFrame frame) {
		this(frame, null);
	}

	public ImageProducerGrey(ShortFrame frame, int[] valueRange) {
		super(frame.width, frame.height);
		this.frame = frame;
		this.len = width*height;
		this.valueRange = valueRange;
	}

	public ImageProducerGrey produce() {
		produce(2f);
		return this;
	}

	public void produce(float gamma) {
		final float ginv = 1f / gamma;

		if(imageBuffer==null||imageBuffer.length!=len) {
			throw new RuntimeException();
		}

		short[][] data = Util.flipRows(frame.data);		

		int minR = 0;
		int maxR = 0;
		if(valueRange!=null) {
			minR = valueRange[0];
			maxR = valueRange[1];
		} else {		
			int[] minmaxR = frame.getMinMax0();
			minR = minmaxR[0];
			maxR = minmaxR[1];
			//log.info("minmax "+Arrays.toString(minmaxR));
		}

		int rangeR = maxR-minR;
		//rangeR = rangeR*7/8;
		if(rangeR==0) {
			rangeR=1;
		}

		int pos=0;
		for(int y=0;y<height;y++) {
			short[] row = data[y];
			for(int x=0;x<width;x++) {
				int c = row[x];
				if(c!=0) {
					c = (int) Math.round(255d*Math.pow((float)(c-minR) / rangeR, ginv));
					if(c>255) {
						c=255;
					}
				}
				imageBuffer[pos++] = (byte) c;
			}
		}
	}
}
