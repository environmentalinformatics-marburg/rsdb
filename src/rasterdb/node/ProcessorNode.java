package rasterdb.node;

import rasterdb.BandProcessor;
import util.frame.DoubleFrame;

public abstract class ProcessorNode {
	
	public abstract DoubleFrame[] process(BandProcessor processor);

}
