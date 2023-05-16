package rasterdb.node;

import rasterdb.FrameProducer;
import util.frame.DoubleFrame;

public abstract class ProcessorNode {
	
	public abstract DoubleFrame[] process(FrameProducer processor);

}
