package pointdb.processing.geopoint;

public class AbstractRaster {
	
	public final int local_min_x;
	public final int local_min_y;
	public final int local_max_x;
	public final int local_max_y;
	public final int range_x;
	public final int range_y;
	public final int cell_count;
	
	protected AbstractRaster(int local_min_x, int local_min_y, int local_max_x, int  local_max_y) {
		this.local_min_x = local_min_x;
		this.local_min_y = local_min_y;
		this.local_max_x = local_max_x;
		this.local_max_y = local_max_y;
		this.range_x = local_max_x-local_min_x+1;
		this.range_y = local_max_y-local_min_y+1;
		this.cell_count = range_x*range_y;
	}

	public AbstractRaster(AbstractRaster r) {
		this.local_min_x = r.local_min_x;
		this.local_min_y = r.local_min_y;
		this.local_max_x = r.local_max_x;
		this.local_max_y = r.local_max_y;
		this.range_x = r.range_x;
		this.range_y = r.range_y;
		this.cell_count = r.cell_count;
	}

}
