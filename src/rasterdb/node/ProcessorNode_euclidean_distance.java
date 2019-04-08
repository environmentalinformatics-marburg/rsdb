package rasterdb.node;

import rasterdb.BandProcessor;
import util.frame.DoubleFrame;

public class ProcessorNode_euclidean_distance extends ProcessorNode {
	
	private final ProcessorNode targetNode;

	public ProcessorNode_euclidean_distance(ProcessorNode targetNode) {
		this.targetNode = targetNode;
	}

	@Override
	public DoubleFrame[] process(BandProcessor processor) {
		DoubleFrame[] targetFrames = targetNode.process(processor);
		DoubleFrame resultFrame = DoubleFrame.euclidean_distance(targetFrames);
		resultFrame.meta.put("name", "spectral_distance");
		return new DoubleFrame[]{resultFrame};
	}

}
