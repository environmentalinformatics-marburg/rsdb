package rasterdb.composite;

import java.util.function.Function;
import java.util.function.IntFunction;

import rasterdb.RasterDB;

public class CompositeRaster {
	//
	
	private RasterDB[] rasterdbs;
	
	public <T> T[] foreach(Function<RasterDB, T> mapper, IntFunction<T[]> gen) {
		int len = rasterdbs.length;
		T[] result = gen.apply(len);
		for(int i=0; i<len ;i++) {
			result[i] = mapper.apply(rasterdbs[i]);
		}	
		return result;
	}	
}
