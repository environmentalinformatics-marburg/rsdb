package remotetask.pointdb;

import pointcloud.Rect2d;
import pointdb.PointDB;
import pointdb.base.Rect;
import pointdb.process.DataProvider2;
import pointdb.processing.tilemeta.StatisticsCreator.Statistics;
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

	@Override
	public Rect2d getExtent() {
		Statistics stat = pointdb.tileMetaProducer(null).toStatistics();
		Rect rect = Rect.of_UTMM(stat.utmm_x_min, stat.utmm_y_min, stat.utmm_x_max, stat.utmm_y_max);
		return rect.toRect2d();
	}
}
