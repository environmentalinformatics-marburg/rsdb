package remotetask.pointdb;

import pointdb.base.Rect;
import pointdb.process.DataProvider2;

public abstract class DataProvider2Factory {	
	public abstract DataProvider2 get(Rect rect);
}
