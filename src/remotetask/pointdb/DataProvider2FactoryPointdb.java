package remotetask.pointdb;

import pointdb.PointDB;
import pointdb.base.Rect;
import pointdb.process.DataProvider2;
import pointdb.subsetdsl.Region;

public class DataProvider2FactoryPointdb extends DataProvider2Factory {	
	private final PointDB pointdb;
	
	public DataProvider2FactoryPointdb(PointDB pointdb) {
		this.pointdb = pointdb;
	}

	@Override
	public DataProvider2 get(Rect rect) {
		DataProvider2 dp = new DataProvider2(pointdb, Region.ofRect(rect));
		return dp;
	}
}
