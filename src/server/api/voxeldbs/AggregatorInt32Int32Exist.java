package server.api.voxeldbs;

public class AggregatorInt32Int32Exist extends AggregatorInt32Int32 {	
	public void process(int[][][] src, int xSrcStart, int ySrcStart, int zSrcStart, int xSrcEnd, int ySrcEnd, int zSrcEnd, int[][][] dst, int xSrcDstOffeset, int ySrcDstOffeset, int zSrcDstOffeset, int factor) {
		for(int z = zSrcStart; z <= zSrcEnd; z++) {
			int[][] srcZ = src[z];
			int[][] dstZ = dst[(z + zSrcDstOffeset) / factor];
			for(int y = ySrcStart; y <= ySrcEnd; y++) {
				int[] srcZY = srcZ[y];
				int[] dstZY = dstZ[(y + ySrcDstOffeset) / factor];
				for(int x = xSrcStart; x <= xSrcEnd; x++) {
					if(srcZY[x] != 0) {
						dstZY[(x + xSrcDstOffeset) / factor] = 1;
					}
				}
			}
		}		
	}
}
