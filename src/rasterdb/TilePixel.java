package rasterdb;

public class TilePixel {
	
	public static final int TYPE_SHORT = 1;
	public static final int TYPE_FLOAT = 2;
	public static final int PIXELS_PER_ROW = 256;
	public static final int PIXELS_PER_ROW_1 = PIXELS_PER_ROW - 1;
	public static final int PIXELS_PER_TILE = PIXELS_PER_ROW * PIXELS_PER_ROW;
	
	public static final int PIXELS_PER_ROW_DIV2 = PIXELS_PER_ROW >> 1;
	public static final int PIXELS_PER_ROW_DIV4 = PIXELS_PER_ROW >> 2;
	
	private TilePixel() {}
	
	public static int pixelToTile(int p) {
		return Math.floorDiv(p, PIXELS_PER_ROW);
	}
	
	public static int pixelToTileOffset(int p) {
		return Math.floorMod(p, PIXELS_PER_ROW);
	}
	
	public static int pixelDiv2ToTileDiv2Offset(int pDiv2) {
		return Math.floorMod(pDiv2, PIXELS_PER_ROW_DIV2);
	}
	
	public static int pixelDiv4ToTileDiv4Offset(int pDiv4) {
		return Math.floorMod(pDiv4, PIXELS_PER_ROW_DIV4);
	}
	
	public static int pixelDivToTileDivOffset(int pDiv, int div) {
		return Math.floorMod(pDiv, (PIXELS_PER_ROW / div));
	}
	
	public static int pixelToDiv2(int p) {
		return Math.floorDiv(p, 2); 
	}
	
	public static int pixelToTileDiv2Offset(int p) {
		int pDiv2 = pixelToDiv2(p); 
		return pixelDiv2ToTileDiv2Offset(pDiv2);		
	}
	
	public static int pixelToDiv4(int p) {
		return Math.floorDiv(p, 4); 
	}
	
	public static int pixelToDiv(int p, int div) {
		return Math.floorDiv(p, div); 
	}
	
	public static int pixelToTileDiv4Offset(int p) {
		int pDiv4 = pixelToDiv4(p); 
		return pixelDiv4ToTileDiv4Offset(pDiv4);		
	}
	
	public static int pixelToTileDivOffset(int p, int div) {
		int pDiv = pixelToDiv(p, div); 
		return pixelDivToTileDivOffset(pDiv, div);		
	}
	
	public static int tileToPixel(int t) {
		return t * PIXELS_PER_ROW;
	}
	
	public static int tileToPixelMax(int t) {
		return t * PIXELS_PER_ROW + PIXELS_PER_ROW_1;
	}
	
	public static int tileDiv2ToPixel(int t) {
		return t * PIXELS_PER_ROW_DIV2;
	}
	
	public static int tileDiv4ToPixel(int t) {
		return t * PIXELS_PER_ROW_DIV4;
	}
	
	public static int tileDivToPixel(int t, int div) {
		return t * (PIXELS_PER_ROW / div);
	}

}
