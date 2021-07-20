package remotetask.pointcloud;

import pointcloud.DoubleRect;
import pointcloud.PointCloud;
import pointdb.base.Rect;
import pointdb.process.DataProvider2;
import pointdb.subsetdsl.Region;
import remotetask.pointdb.DataProvider2Factory;

public class DataProvider2FactoryPointcloud extends DataProvider2Factory {
	private final int t;
	private final PointCloud pointcloud;
	
	public DataProvider2FactoryPointcloud(int t, PointCloud pointcloud) {
		this.t = t;
		this.pointcloud = pointcloud;
	}

	@Override
	public DataProvider2 get(Rect rect) {
		DataProvider2 dp = new DataProvider2(pointcloud, t, Region.ofRect(rect));
		return dp;
	}
	
	@Override
	public DoubleRect getExtent() {
		return pointcloud.getRange();
	}
}
