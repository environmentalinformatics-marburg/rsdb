package server.api.rasterdb;

public class ScaleDownMax2 {
	public static boolean validScaleDownMax2(int widthSrc, int heightSrc, int widthDst, int heightDst) {
		if(widthSrc == widthDst && heightSrc == heightDst) {
			return true;
		}
		if(widthSrc <= widthDst || heightSrc <= heightDst) {
			return false;
		}
		if(widthSrc < 2 || heightSrc < 2) {
			return false;
		}
		if(widthSrc / 2 >= widthDst || heightSrc / 2 >= heightDst) {
			return false;
		}
		return true;
	}
	
	public static float[][] scaleDownMax2(float[][] src, int widthSrc, int heightSrc, int widthDst, int heightDst) {
		if(widthSrc == widthDst && heightSrc == heightDst) {
			return src;
		}
		if(widthSrc <= widthDst || heightSrc <= heightDst) {
			throw new RuntimeException();
		}
		if(widthSrc < 2 || heightSrc < 2) {
			throw new RuntimeException();
		}
		if(widthSrc / 2 >= widthDst || heightSrc / 2 >= heightDst) {
			throw new RuntimeException();
		}		

		float[][] dst = new float[heightDst][widthDst];
		float[] xFracs = new float[widthDst];
		float[] yFracs = new float[heightDst];
		int[] xDstSrcPoss = new int[widthDst];
		int[] yDstSrcPoss = new int[heightDst];		

		{
			double widthSrcD = widthSrc;
			for (int xDst = 0; xDst < widthDst; xDst++) {
				double f1 = (xDst * widthSrcD) / widthDst;
				double f2 = ((xDst + 1) * widthSrcD) / widthDst;
				int pos = (int) Math.floor(f1);
				xDstSrcPoss[xDst] = pos;				
				double frac1 = pos + 1 - f1; 
				double frac2 = f2 - pos - 1;
				double frac = frac2 / (frac1 + frac2);				
				xFracs[xDst] = (float) frac;				
				//Logger.info(xDst + "  " + pos + "  " + frac1 + "  " + frac2 + "   " + frac);
			}
		}

		{
			double heightSrcD = heightSrc;
			for (int yDst = 0; yDst < heightDst; yDst++) {
				double f1 = (yDst * heightSrcD) / heightDst;
				double f2 = ((yDst + 1) * heightSrcD) / heightDst;
				int pos = (int) Math.floor(f1);
				yDstSrcPoss[yDst] = pos;				
				double frac1 = pos + 1 - f1; 
				double frac2 = f2 - pos - 1;
				double frac = frac2 / (frac1 + frac2);				
				yFracs[yDst] = (float) frac;				
				//Logger.info(yDst + "  " + pos + "  " + frac1 + "  " + frac2 + "   " + frac);
			}
		}

		for (int yDst = 0; yDst < heightDst; yDst++) {
			int yDstSrcPos = yDstSrcPoss[yDst];
			float[] srcY0 = src[yDstSrcPos];
			float[] srcY1 = src[yDstSrcPos + 1];
			float yFrac = yFracs[yDst];
			float[] dstY = dst[yDst];
			for (int xDst = 0; xDst < widthDst; xDst++) {
				float xFrac = xFracs[xDst];
				int xDstSrcPos = xDstSrcPoss[xDst];
				//Logger.info(xDst + "  " + xDstSrcPos + "  " + xFrac);
				float s00 = srcY0[xDstSrcPos];
				float s01 = srcY0[xDstSrcPos + 1];
				float s0 = s00 + (s01 - s00) * xFrac;
				float s10 = srcY1[xDstSrcPos];
				float s11 = srcY1[xDstSrcPos + 1];
				float s1 = s10 + (s11 - s10) * xFrac;
				float s = s0 + (s1 - s0) * yFrac;
				dstY[xDst] = s;
			}
		}
		return dst;
	}
	
	public static short[][] scaleDownMax2(short[][] src, int widthSrc, int heightSrc, int widthDst, int heightDst, short na) {
		if(widthSrc == widthDst && heightSrc == heightDst) {
			return src;
		}
		if(widthSrc <= widthDst || heightSrc <= heightDst) {
			throw new RuntimeException();
		}
		if(widthSrc < 2 || heightSrc < 2) {
			throw new RuntimeException();
		}
		if(widthSrc / 2 >= widthDst || heightSrc / 2 >= heightDst) {
			throw new RuntimeException();
		}		

		short[][] dst = new short[heightDst][widthDst];
		float[] xFracs = new float[widthDst];
		float[] yFracs = new float[heightDst];
		int[] xDstSrcPoss = new int[widthDst];
		int[] yDstSrcPoss = new int[heightDst];		

		{
			double widthSrcD = widthSrc;
			for (int xDst = 0; xDst < widthDst; xDst++) {
				double f1 = (xDst * widthSrcD) / widthDst;
				double f2 = ((xDst + 1) * widthSrcD) / widthDst;
				int pos = (int) Math.floor(f1);
				xDstSrcPoss[xDst] = pos;				
				double frac1 = pos + 1 - f1; 
				double frac2 = f2 - pos - 1;
				double frac = frac2 / (frac1 + frac2);				
				xFracs[xDst] = (float) frac;				
				//Logger.info(xDst + "  " + pos + "  " + frac1 + "  " + frac2 + "   " + frac);
			}
		}

		{
			double heightSrcD = heightSrc;
			for (int yDst = 0; yDst < heightDst; yDst++) {
				double f1 = (yDst * heightSrcD) / heightDst;
				double f2 = ((yDst + 1) * heightSrcD) / heightDst;
				int pos = (int) Math.floor(f1);
				yDstSrcPoss[yDst] = pos;				
				double frac1 = pos + 1 - f1; 
				double frac2 = f2 - pos - 1;
				double frac = frac2 / (frac1 + frac2);				
				yFracs[yDst] = (float) frac;				
				//Logger.info(yDst + "  " + pos + "  " + frac1 + "  " + frac2 + "   " + frac);
			}
		}

		for (int yDst = 0; yDst < heightDst; yDst++) {
			int yDstSrcPos = yDstSrcPoss[yDst];
			short[] srcY0 = src[yDstSrcPos];
			short[] srcY1 = src[yDstSrcPos + 1];
			float yFrac = yFracs[yDst];
			short[] dstY = dst[yDst];
			for (int xDst = 0; xDst < widthDst; xDst++) {
				float xFrac = xFracs[xDst];
				int xDstSrcPos = xDstSrcPoss[xDst];
				//Logger.info(xDst + "  " + xDstSrcPos + "  " + xFrac);
				float s00 = srcY0[xDstSrcPos];
				float s01 = srcY0[xDstSrcPos + 1];
				float s0 = s00 + (s01 - s00) * xFrac;
				float s10 = srcY1[xDstSrcPos];
				float s11 = srcY1[xDstSrcPos + 1];
				float s1 = s10 + (s11 - s10) * xFrac;
				float s = s0 + (s1 - s0) * yFrac;
				dstY[xDst] = s00 == na || s01 == na || s10 == na || s11 == na ? na : (short) s;
			}
		}
		return dst;
	}
}
