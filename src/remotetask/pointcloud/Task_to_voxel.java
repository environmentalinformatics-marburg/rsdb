package remotetask.pointcloud;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import pointcloud.AttributeSelector;
import pointcloud.PointCloud;
import pointcloud.PointTable;
import remotetask.CancelableRemoteTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import voxeldb.VoxelCell;
import voxeldb.VoxelDB;
import voxeldb.VoxeldbConfig;

@task_pointcloud("to_voxel")
@Description("Convert pointcloud to voxels.")
@Param(name="pointcloud", type="pointcloud", desc="ID of PointDB layer.", example="pointcloud1")
public class Task_to_voxel extends CancelableRemoteTask {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	private final PointCloud pointcloud;

	public Task_to_voxel(Context ctx) {
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("pointcloud");
		pointcloud = broker.getPointCloud(name);
		pointcloud.check(ctx.userIdentity);
		EmptyACL.ADMIN.check(ctx.userIdentity);
	}

	@Override
	public void process() {
		
		VoxeldbConfig config = new VoxeldbConfig("voxeldb", Paths.get("c:/temp_TLS/voxeldb"), "TileStorage", false);
		VoxelDB voxeldb = new VoxelDB(config);
		
		setMessage("query count of tiles");		
		long total = pointcloud.getTileKeys().size();
		setMessage("start processing " + total + " tiles");
		Stream<PointTable> pointTables = pointcloud.getPointTables(-Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, new AttributeSelector().setXYZ());

		StreamConsumer consumer = new StreamConsumer(total, voxeldb);
		pointTables.forEach(consumer);
		
		voxeldb.close();
		
		if(consumer.getCnt() == total) {
			setMessage("all " + total + " tiles processed");
		} else {
			throw new RuntimeException("stopped (" + consumer.cnt + " of " + total + " tiles processed)");
		}
	}

	private final class StreamConsumer implements Consumer<PointTable> {

		private final long total;
		private final VoxelDB voxeldb;

		private LongAdder cnt = new LongAdder();

		public StreamConsumer(long total, VoxelDB voxeldb) {
			this.total = total;
			this.voxeldb = voxeldb;
		}

		@Override
		public void accept(PointTable t) {
			try {
				process(t);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			cnt.increment();
			if(isMessageTime()) {
				setMessage(getCnt() + " of " + total + " tiles processed");
				if(isCanceled()) {
					throw new RuntimeException("canceled");
				}
			}			
		}

		public long getCnt() {
			return cnt.sum();
		}

		private void process(PointTable t) throws IOException {
			int len = t.rows;
			double[] xs = t.x;
			double[] ys = t.y;
			double[] zs = t.z;			

			double xmin = Double.MAX_VALUE;
			double ymin = Double.MAX_VALUE;
			double zmin = Double.MAX_VALUE;
			double xmax = - Double.MAX_VALUE;
			double ymax = - Double.MAX_VALUE;
			double zmax = - Double.MAX_VALUE;			

			for(int i = 0; i < len; i++) {
				double x = xs[i];
				if(x < xmin) {
					xmin = x;
				}
				if(xmax < x) {
					xmax = x;
				}
				double y = ys[i];
				if(y < ymin) {
					ymin = y;
				}
				if(ymax < y) {
					ymax = y;
				}
				double z = zs[i];
				if(z < zmin) {
					zmin = z;
				}
				if(zmax < z) {
					zmax = z;
				}
			}

			double xorigin = 0;
			double yorigin = 0;
			double zorigin = 0;
			double xvoxelsize = 1;
			double yvoxelsize = 1;
			double zvoxelsize = 1;
			long vxmin = (long) Math.floor((xmin - xorigin) / xvoxelsize); 
			long vymin = (long) Math.floor((ymin - yorigin) / yvoxelsize); 
			long vzmin = (long) Math.floor((zmin - zorigin) / zvoxelsize);			
			long vxmax = (long) Math.floor((xmax - xorigin) / xvoxelsize); 
			long vymax = (long) Math.floor((ymax - yorigin) / yvoxelsize); 
			long vzmax = (long) Math.floor((zmax - zorigin) / zvoxelsize);
			long vxrange = vxmax - vxmin + 1;
			long vyrange = vymax - vymin + 1;
			long vzrange = vzmax - vzmin + 1;
			long vcount = vxrange * vyrange * vzrange;

			if(vcount <= MAX_VOXEL_COUNT) {
				int[][][] cnt = new int[(int) vzrange][(int) vyrange][(int) vxrange];
				for(int i = 0; i < len; i++) {
					int x = (int) Math.floor((xs[i] - xorigin) / xvoxelsize) - (int)vxmin;
					int y = (int) Math.floor((ys[i] - yorigin) / yvoxelsize) - (int)vymin;
					int z = (int) Math.floor((zs[i] - zorigin) / zvoxelsize) - (int)vzmin;
					cnt[z][y][x]++;
				}

				int cellsize = 100;
				int cxmin = (int) Math.floorDiv(vxmin, cellsize);
				int cymin = (int) Math.floorDiv(vymin, cellsize);
				int czmin = (int) Math.floorDiv(vzmin, cellsize);
				int cxmax = (int) Math.floorDiv(vxmax, cellsize);
				int cymax = (int) Math.floorDiv(vymax, cellsize);
				int czmax = (int) Math.floorDiv(vzmax, cellsize);
				for(int cz = czmin; cz <= czmax; cz++) {
					for(int cy = cymin; cy <= cymax; cy++) {
						for(int cx = cxmin; cx <= cxmax; cx++) {							
							VoxelCell oldVoxelCell = voxeldb.getVoxelCell(cx, cy, cz);							
							int[][][] ccnt = oldVoxelCell == null ? new int[cellsize][cellsize][cellsize] : oldVoxelCell.cnt;
							int cvxmin = (cx * cellsize);
							int cvymin = (cy * cellsize);
							int cvzmin = (cz * cellsize);
							int cvxmax = cvxmin + cellsize - 1;
							int cvymax = cvymin + cellsize - 1;
							int cvzmax = cvzmin + cellsize - 1;
							int lvxmin = cvxmin <= vxmin ? (int) vxmin : cvxmin;
							int lvymin = cvymin <= vymin ? (int) vymin : cvymin;
							int lvzmin = cvzmin <= vzmin ? (int) vzmin : cvzmin;							
							int lvxmax = cvxmax >= vxmax ? (int) vxmax : cvxmax;
							int lvymax = cvymax >= vymax ? (int) vymax : cvymax;
							int lvzmax = cvzmax >= vzmax ? (int) vzmax : cvzmax;
							for(int z = lvzmin; z <= lvzmax; z++) {
								for(int y = lvymin; y <= lvymax; y++) {
									for(int x = lvxmin; x <= lvxmax; x++) {
										ccnt[z - cvzmin][y - cvymin][x - cvxmin] = cnt[z - (int)vzmin][y - (int)vymin][x - (int)vxmin];
									}
								}
							}
							VoxelCell voxelCell = new VoxelCell(cx, cy, cz, ccnt);
							voxeldb.writeVoxelCell(voxelCell);
						}
					}
				}
			} else {
				throw new RuntimeException("high range voxel processing not implemented");
			}

		}

		private static final long MAX_VOXEL_COUNT = 300 * 300 * 300;
	}
}
