package rasterdb.tile;

public class ProcessingChar {

 public static void convertNA(char[][] pixels, char srcNA, char dstNA) {
		int w = pixels[0].length;
		int h = pixels.length;
		for(int i=0;i<h;i++) {
			char[] src = pixels[i];				
			for(int c=0;c<w;c++) {
				//Logger.info(src[c]);
				if(src[c] == srcNA) {
					src[c] = dstNA;
				}
				/*if(src[c] != 0) {
					System.out.println(src[c]);
				}*/
			}
		}
	}
}
