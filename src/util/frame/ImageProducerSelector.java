package util.frame;

import util.image.PureImage;

public class ImageProducerSelector {
	
	public static PureImage produce(ShortFrame frame, VisualMapping mapping) {
		return produce(frame, mapping, null);
	}
	
	public static PureImage produce(ShortFrame frame, VisualMapping mapping, int[] valueRange) {
		switch(mapping) {
		case GREY:
			return new  ImageProducerGrey(frame, valueRange).produce();
		case COLOR:
			return new ImageProducerMono(frame, valueRange).produce();
		default:
			throw new RuntimeException("unknown mapping "+mapping);
		}
	}
}
