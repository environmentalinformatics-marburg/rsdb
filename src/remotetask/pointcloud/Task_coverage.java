package remotetask.pointcloud;

import java.io.IOException;
import java.util.Iterator;

import org.json.JSONObject;
import org.tinylog.Logger;

import broker.Broker;
import broker.Informal.Builder;
import broker.acl.ACL;
import broker.TimeSlice;
import pointcloud.P2d;
import pointcloud.PointCloud;
import rasterdb.Band;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.tile.ProcessingShort;
import rasterdb.tile.TilePixel;
import rasterunit.RasterUnitStorage;
import rasterunit.TileCollection;
import rasterunit.TileKey;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import util.Range2d;

@task_pointcloud("coverage")
@Description("Create a coverage raster of areas that are covered by the pointcloud in pointcloud cell resolution.")
@Param(name="pointcloud", type="pointcloud", desc="ID of PointCloud layer. (source)", example="pointcloud1")
@Param(name="rasterdb", type="layer_id", desc="ID of new RasterDB layer. (target, default: [pointcloud]_coverage) ", example="pointcloud1_coverage", required=false)
@Param(name="associate", type="boolean", desc="Set the created raster layer as map visualisation for this point cloud layer. (default: true)", example="false", required=false)
@Param(name="storage_type", desc="Storage type of new RasterDB. (default: TileStorage)", format="RasterUnit or TileStorage", example="TileStorage", required=false)
@Param(name="transactions", type="boolean", desc="Use power failer safe (and slow) RasterDB operation mode. (RasterUnit only, default false)", example="false", required=false)
public class Task_coverage extends RemoteTask {
	

	private final Broker broker;
	private final JSONObject task;
	private final PointCloud pointcloud;
	private final String rasterdb_name;

	public Task_coverage(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("pointcloud");
		pointcloud = broker.getPointCloud(name);
		pointcloud.check(ctx.userIdentity, "task pointcloud coverage");
		pointcloud.checkMod(ctx.userIdentity, "task pointcloud coverage"); // check needed as same ACLs are assigned to target rasterdb
		this.rasterdb_name = task.optString("rasterdb", pointcloud.getName() + "_coverage");
		if(broker.hasRasterdb(rasterdb_name)) {
			RasterDB rasterdb = broker.getRasterdb(rasterdb_name);
			rasterdb.checkMod(ctx.userIdentity, "task pointcloud coverage of existing name");
			rasterdb.close();
		}
	}

	@Override
	public void process() throws IOException {
		boolean transactions = true;
		if(task.has("transactions")) {
			transactions = task.getBoolean("transactions");
		}
		boolean associate = true;
		if(task.has("associate")) {
			associate = task.getBoolean("associate");
		}
		RasterDB rasterdb;
		if(task.has("storage_type")) {
			String storage_type = task.getString("storage_type");
			rasterdb = broker.createNewRasterdb(rasterdb_name, transactions, storage_type);
		} else {
			rasterdb = broker.createNewRasterdb(rasterdb_name, transactions);	
		}
		if(ctx.userIdentity != null) {
			String username = ctx.userIdentity.getUserPrincipal().getName();
			rasterdb.setACL_owner(ACL.ofRole(username));
		}
		rasterdb.setACL(pointcloud.getACL());
		rasterdb.setACL_mod(pointcloud.getACL_mod());
		startCoverage(rasterdb);
		rasterdb.rebuildPyramid(true, this);
		if(associate) {
			pointcloud.setAssociatedRasterDB(rasterdb_name);
		}
	}

	public void startCoverage(RasterDB rasterdb) throws IOException {
		if(!rasterdb.rasterUnit().isEmpty()) {
			throw new RuntimeException("target RasterDB not empty: " + rasterdb.config.getName());
		}
		rasterdb.setCode(pointcloud.getCode());
		rasterdb.setProj4(pointcloud.getProj4());
		rasterdb.associated.setPointCloud(pointcloud.getName());
		Builder informal = rasterdb.informal().toBuilder();
		informal.description = "cell coverage of poindcloud " + pointcloud.getName();
		rasterdb.setInformal(informal.build());
		rasterdb.writeMeta();

		pointcloud.setAssociatedRasterDB(rasterdb.config.getName());

		Range2d cellrange = pointcloud.getCellRange();
		P2d celloffset = pointcloud.getCelloffset();
		double cellsize = pointcloud.getCellsize();
		double pointcloud_xmin = (celloffset.x + cellrange.xmin) * cellsize;
		double pointcloud_ymin = (celloffset.y + cellrange.ymin) * cellsize;		
		Logger.info("set coverage raster to pointcloud_xmin " + pointcloud_xmin + " pointcloud_ymin " + pointcloud_ymin);
		double raster_pixel_size = cellsize;
		rasterdb.setPixelSize(raster_pixel_size, raster_pixel_size, pointcloud_xmin, pointcloud_ymin);

		Band band = rasterdb.createBand(TilePixel.TYPE_SHORT, "coverage", null);

		if(pointcloud.timeMapReadonly.isEmpty()) {
			int t = 0;
			runCoverage(rasterdb.rasterUnit(), rasterdb.ref(), t, band);
		} else {
			for(TimeSlice timeSlice : pointcloud.timeMapReadonly.values()) {
				rasterdb.setTimeSlice(timeSlice);
				int t = timeSlice.id;
				runCoverage(rasterdb.rasterUnit(), rasterdb.ref(), t, band);				
			}
		}
	}

	private void runCoverage(RasterUnitStorage rasterUnitStorage, GeoReference geoReference, int t, Band band) throws IOException {
		Range2d cellrange = pointcloud.getCellRange();
		if(cellrange == null) {
			return;
		}
		Logger.info("cellRange: " + cellrange);
		runCoverage(rasterUnitStorage, geoReference, t, band, cellrange);
	}

	private void runCoverage(RasterUnitStorage rasterUnitStorage, GeoReference ref, int t, Band band, Range2d cellrange) throws IOException {
		if(cellrange == null) {
			return;
		}
		Logger.info("pre  " + cellrange);
		cellrange = pointcloud.getCellRange2dOfSubset(cellrange);
		Logger.info("post " + cellrange);
		if(cellrange == null) {
			return;
		}
		int cellrangeY = cellrange.getHeight();
		if(cellrangeY > 4096) {
			Logger.info("subdiv Y " + cellrangeY);
			int middleY = (int) ((((long)cellrange.ymin) + ((long)cellrange.ymax)) / 2);
			runCoverage(rasterUnitStorage, ref, t, band, new Range2d(cellrange.xmin, cellrange.ymin, cellrange.xmax, middleY));
			runCoverage(rasterUnitStorage, ref, t, band, new Range2d(cellrange.xmin, middleY + 1, cellrange.xmax, cellrange.ymax));
			return;
		}

		int cellrangeX = cellrange.getWidth();
		if(cellrangeX > 4096) {
			Logger.info("subdiv X " + cellrangeX);
			int middleX = (int) ((((long)cellrange.xmin) + ((long)cellrange.xmax)) / 2);
			runCoverage(rasterUnitStorage, ref, t, band, new Range2d(cellrange.xmin, cellrange.ymin, middleX, cellrange.ymax));
			runCoverage(rasterUnitStorage, ref, t, band, new Range2d(middleX + 1, cellrange.ymin, cellrange.xmax, cellrange.ymax));
			return;
		}		

		P2d celloffset = pointcloud.getCelloffset();
		double cellsize = pointcloud.getCellsize();
		double cellscale1d = 1 / pointcloud.getCellscale();
		double pointcloud_xmin = (celloffset.x + cellrange.xmin) * cellsize;
		double pointcloud_ymin = (celloffset.y + cellrange.ymin) * cellsize;
		double pointcloud_xmax = (celloffset.x + cellrange.xmax + 1) * cellsize - cellscale1d;
		double pointcloud_ymax = (celloffset.y + cellrange.ymax + 1) * cellsize - cellscale1d;		
		Logger.info("local pointcloud_xmin " + pointcloud_xmin + " pointcloud_ymin " + pointcloud_ymin+"   pointcloud_xmax " + pointcloud_xmax + " pointcloud_ymax " + pointcloud_ymax);
		int raster_xmin = ref.geoXToPixel(pointcloud_xmin);
		int raster_ymin = ref.geoYToPixel(pointcloud_ymin);
		int raster_xmax = ref.geoXToPixel(pointcloud_xmax);
		int raster_ymax = ref.geoYToPixel(pointcloud_ymax);
		Logger.info("raster_xmin " + raster_xmin + " raster_ymin " + raster_ymin+"   raster_xmax " + raster_xmax + " raster_ymax " + raster_ymax);


		int cellCount = pointcloud.countCells(t, pointcloud_xmin, pointcloud_ymin, pointcloud_xmax, pointcloud_ymax);
		Logger.info("cell count "+cellCount);
		if(cellCount > 0) {			
			TileCollection tileCollection = pointcloud.getTiles(t, pointcloud_xmin, pointcloud_ymin, pointcloud_xmax, pointcloud_ymax);
			if(tileCollection != null) {
				double xcelloffset = celloffset.x;
				double ycelloffset = celloffset.y;
				int xcellmin = (int) (Math.floor(pointcloud_xmin / cellsize) - xcelloffset);
				int ycellmin = (int) (Math.floor(pointcloud_ymin / cellsize) - ycelloffset);

				short na = band.getInt16NA();
				short[][] pixels = ProcessingShort.createEmpty(raster_xmax - raster_xmin + 1, raster_ymax - raster_ymin + 1, na);
				Iterator<TileKey> it = tileCollection.keyIterator();
				while(it.hasNext()) {
					TileKey key = it.next();
					int x = key.x - xcellmin;
					int y = key.y - ycellmin;
					pixels[y][x] = 1;
				}
				ProcessingShort.writeMerge(rasterUnitStorage, t, band, pixels, raster_ymin, raster_xmin);
				rasterUnitStorage.commit();
				Logger.info("committed");
			}			
		}
	}
}
