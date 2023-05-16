package remotetask.pointdb;

import pointcloud.Rect2d;
import pointdb.base.Rect;
import pointdb.process.DataProvider2;

public abstract class DataProvider2Factory {	
	public abstract DataProvider2 get(Rect rect);
	public abstract Rect2d getExtent();
}
