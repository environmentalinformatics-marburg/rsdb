package rasterdb.node;

import rasterdb.FrameProducer;
import util.frame.DoubleFrame;

public class ProcessorNode_black_point_compensation extends ProcessorNode {
	
	private final ProcessorNode node;

	public ProcessorNode_black_point_compensation(ProcessorNode node) {
		this.node = node;
	}

	@Override
	public DoubleFrame[] process(FrameProducer processor) {
		DoubleFrame[] frames = node.process(processor);
		for (int i = 0; i < frames.length; i++) {
			DoubleFrame frame = frames[i];
			double[] minmax = frame.getMinMax();			
			frame.substractThis(minmax[0]);			
		}
		return frames;
	}

}
