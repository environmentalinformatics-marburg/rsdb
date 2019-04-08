package util.frame;

public class BooleanFrame {
	public final boolean[][] data;
	public final int width;
	public final int height;

	public final int local_min_x;
	public final int local_min_y;
	public final int local_max_x;
	public final int local_max_y;
	
	public BooleanFrame(boolean[][] data, int local_min_x, int local_min_y, int local_max_x, int local_max_y) {
		this.data = data;		
		this.width = data[0].length;
		this.height = data.length;

		this.local_min_x = local_min_x;
		this.local_min_y = local_min_y;
		this.local_max_x = local_max_x;
		this.local_max_y = local_max_y;
	}
}
