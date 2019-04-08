package rasterdb.node;

import rasterdb.BandProcessor;
import util.frame.DoubleFrame;

public class ProcessorNode_normalised_difference extends ProcessorNode {
	
	private final ProcessorNode referenceNode;
	private final ProcessorNode targetNode;

	public ProcessorNode_normalised_difference(ProcessorNode referenceNode, ProcessorNode targetNode) {
		this.referenceNode = referenceNode;
		this.targetNode = targetNode;
	}

	@Override
	public DoubleFrame[] process(BandProcessor processor) {
		DoubleFrame refFrame = referenceNode.process(processor)[0];
		DoubleFrame[] targetFrames = targetNode.process(processor);
		DoubleFrame[] resultFrames = new DoubleFrame[targetFrames.length];
		for (int i = 0; i < resultFrames.length; i++) {
			DoubleFrame targetFrame = targetFrames[i];
			DoubleFrame nd = DoubleFrame.normalised_difference(refFrame, targetFrame);
			nd.meta.putAll(targetFrame.meta);
			resultFrames[i] = nd;
		}
		return resultFrames;
	}

}
