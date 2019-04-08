package util.raster;

public class Raster {
	public final int width;
	public final int height;	
	public final int xmin;
	public final int xmax;
	public final int ymin;
	public final int ymax;
	public final int xoffset;
	public final int yoffset;
	
	public Raster(int xmin, int xmax, int ymin, int ymax, int xoffset, int yoffset) {
		this.width = xmax-xmin+1;
		this.height = ymax-ymin+1;
		this.xmin = xmin;
		this.xmax = xmax;
		this.ymin = ymin;
		this.ymax = ymax;
		this.xoffset = xoffset;
		this.yoffset = yoffset;
	}

}
