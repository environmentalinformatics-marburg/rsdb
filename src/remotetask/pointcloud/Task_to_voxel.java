package remotetask.pointcloud;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.tinylog.Logger;

import broker.Broker;
import broker.TimeSlice;
import broker.acl.ACL;
import broker.acl.EmptyACL;
import pointcloud.AttributeSelector;
import pointcloud.PointCloud;
import pointcloud.PointTable;
import rasterdb.RasterDB;
import remotetask.CancelableRemoteTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import voxeldb.CellFactory;
import voxeldb.VoxelCell;
import voxeldb.VoxelDB;

@task_pointcloud("to_voxel")
@Description("Convert pointcloud to voxels.")
@Param(name="pointcloud", type="pointcloud", desc="ID of PointDB layer.", example="pointcloud1")
@Param(name="time_slice", type="string", desc="Name of the new voxeldb time slice to create. Only used if pointcloud does not contain time slice entries. (default: untitled)", example="January", required=false)
@Param(name="voxeldb", type="layer_id", desc="ID of new VoxelDB layer. (target, default: [pointcloud]_voxels) ", example="pointcloud1_voxels", required=false)

public class Task_to_voxel extends CancelableRemoteTask {

	private final Broker broker;
	private final JSONObject task;
	private final PointCloud pointcloud;
	private final String voxeldb_name;

	public Task_to_voxel(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String name = task.getString("pointcloud");
		pointcloud = broker.getPointCloud(name);		
		pointcloud.check(ctx.userIdentity, "task pointcloud to_voxel");

		this.voxeldb_name = task.optString("voxeldb", pointcloud.getName() + "_voxels");
		if(broker.hasVoxeldb(voxeldb_name)) {
			VoxelDB voxeldb = broker.getVoxeldb(voxeldb_name);
			voxeldb.checkMod(ctx.userIdentity, "task pointcloud to_voxel of existing name");
			voxeldb.close();
		}		
	}

	@Override
	public void process() {
		String storage_type = task.optString("storage_type", "TileStorage");
		boolean transactions = task.optBoolean("transactions", false);
		double voxel_size = task.optNumber("voxel_size", 1).doubleValue();		

		broker.deleteVoxeldb(voxeldb_name);
		VoxelDB voxeldb = broker.createNewVoxeldb(voxeldb_name, storage_type, transactions);
		voxeldb.setACL(pointcloud.getACL());
		voxeldb.setACL_mod(pointcloud.getACL_mod());
		if(ctx.userIdentity != null) {
			String username = ctx.userIdentity.getUserPrincipal().getName();
			voxeldb.setACL_owner(ACL.ofRole(username));
		}
		voxeldb.setProj4(pointcloud.getProj4());
		voxeldb.setEpsg(pointcloud.getEPSGcode());
		voxeldb.trySetVoxelsize(voxel_size);		
		voxeldb.trySetCellsize(50);
		setMessage("query count of tiles");		
		long total = pointcloud.getTileKeys().size();
		setMessage("start processing " + total + " tiles");
		long counter = 0;		
		
		if(pointcloud.timeMapReadonly.isEmpty()) {
			String time_slice = task.optString("time_slice", "untitled");
			if(time_slice.isEmpty()) {
				time_slice = "";
			}
			TimeSlice timeSlice = new TimeSlice(0, time_slice);
			voxeldb.setTimeSlice(timeSlice);
			counter += process(voxeldb, timeSlice, counter, total);
		} else {
			for(TimeSlice timeSlice : pointcloud.timeMapReadonly.values()) {
				voxeldb.setTimeSlice(timeSlice);
				counter += process(voxeldb, timeSlice, counter, total);
			}
		}
		
		voxeldb.close();

		if(counter == total) {
			setMessage("all " + total + " tiles processed");
		} else {
			throw new RuntimeException("stopped (" + counter + " of " + total + " tiles processed)");
		}
	}

	public long process(VoxelDB voxeldb, TimeSlice timeSlice, long counter, long total) {		
		Stream<PointTable> pointTables = pointcloud.getPointTables(timeSlice.id, -Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, new AttributeSelector().setXYZ());
		StreamConsumer consumer = new StreamConsumer(counter, total, voxeldb, timeSlice);
		pointTables.sequential().forEachOrdered(consumer);
		return consumer.getCnt();
	}

	private final class StreamConsumer implements Consumer<PointTable> {

		private final long startCount;
		private final long total;
		private final VoxelDB voxeldb;
		private final int t;

		private LongAdder cnt = new LongAdder();

		public StreamConsumer(long startCount, long total, VoxelDB voxeldb, TimeSlice timeSlice) {
			this.startCount = startCount;
			this.total = total;
			this.voxeldb = voxeldb;
			this.t = timeSlice.id;
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
				setMessage((startCount + getCnt()) + " of " + total + " tiles processed");
				if(isCanceled()) {
					throw new RuntimeException("canceled");
				}
			}			
		}

		public long getCnt() {
			return cnt.sum();
		}

		private void process(PointTable pointTable) throws IOException {

			CellFactory cellFactory = CellFactory.ofCount(voxeldb);

			int cellsize = voxeldb.getCellsize();
			double xorigin = voxeldb.geoRef().originX;
			double yorigin = voxeldb.geoRef().originY;
			double zorigin = voxeldb.geoRef().originZ;
			double xvoxelsize = voxeldb.geoRef().voxelSizeX;
			double yvoxelsize = voxeldb.geoRef().voxelSizeY;
			double zvoxelsize = voxeldb.geoRef().voxelSizeZ;			

			int len = pointTable.rows;
			double[] xs = pointTable.x;
			double[] ys = pointTable.y;
			double[] zs = pointTable.z;			

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
			Logger.info("PointTable " + xmin + " " + ymin + " " + zmin + "   " + xmax + " "+ ymax + " " + zmax + "     " + len);

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
				int ixmin = Integer.MAX_VALUE;
				int iymin = Integer.MAX_VALUE;
				int izmin = Integer.MAX_VALUE;
				int ixmax = Integer.MIN_VALUE;
				int iymax = Integer.MIN_VALUE;
				int izmax = Integer.MIN_VALUE;

				int[][][] cnt = new int[(int) vzrange][(int) vyrange][(int) vxrange];
				int counter = 0;
				for(int i = 0; i < len; i++) {
					int x = (int) (Math.floor((xs[i] - xorigin) / xvoxelsize) - vxmin);
					int y = (int) (Math.floor((ys[i] - yorigin) / yvoxelsize) - vymin);
					int z = (int) (Math.floor((zs[i] - zorigin) / zvoxelsize) - vzmin);
					//Logger.info("  " + xs[i] + " " + ys[i] + " " + zs[i] +" -> " + x + " " + y + " " + z + "     " + xorigin +  "  " + xvoxelsize + " " + vxmin);
					cnt[z][y][x]++;

					if(x < ixmin) {
						ixmin = x;
					}
					if(ixmax < x) {
						ixmax = x;
					}
					if(y < iymin) {
						iymin = y;
					}
					if(iymax < y) {
						iymax = y;
					}
					if(z < izmin) {
						izmin = z;
					}
					if(izmax < z) {
						izmax = z;
					}
					counter++;
				}
				Logger.info("i " + ixmin + " " + iymin + " " + izmin + "   " + ixmax + " "+ iymax + " " + izmax + "    " + counter);

				int cxmin = (int) Math.floorDiv(vxmin, cellsize);
				int cymin = (int) Math.floorDiv(vymin, cellsize);
				int czmin = (int) Math.floorDiv(vzmin, cellsize);
				int cxmax = (int) Math.floorDiv(vxmax, cellsize);
				int cymax = (int) Math.floorDiv(vymax, cellsize);
				int czmax = (int) Math.floorDiv(vzmax, cellsize);
				for(int cz = czmin; cz <= czmax; cz++) {
					for(int cy = cymin; cy <= cymax; cy++) {
						for(int cx = cxmin; cx <= cxmax; cx++) {
							Logger.info("cx " + cx + "  cy " + cy + "  cz " + cz);
							VoxelCell oldVoxelCell = cellFactory.getVoxelCell(cx, cy, cz, t);							
							int[][][] ccnt = oldVoxelCell == null ? new int[cellsize][cellsize][cellsize] : oldVoxelCell.cnt;

							/*{
								int sxmin = Integer.MAX_VALUE;
								int symin = Integer.MAX_VALUE;
								int szmin = Integer.MAX_VALUE;
								int sxmax = Integer.MIN_VALUE;
								int symax = Integer.MIN_VALUE;
								int szmax = Integer.MIN_VALUE;
								int sum = 0;
								for(int z = 0; z < cellsize; z++) {
									for(int y = 0; y < cellsize; y++) {
										for(int x = 0; x < cellsize; x++) {
											int v = ccnt[z][y][x];
											if(v > 0) {
												sum++;
												if(x < sxmin) {
													sxmin = x;
												}
												if(sxmax < x) {
													sxmax = x;
												}
												if(y < symin) {
													symin = y;
												}
												if(symax < y) {
													symax = y;
												}
												if(z < szmin) {
													szmin = z;
												}
												if(szmax < z) {
													szmax = z;
												}
											}
										}
									}
								}
								Logger.info("vc " + sxmin + " " + symin + " " + szmin + "   " + sxmax + " "+ symax + " " + szmax + "     " + sum);
							}*/

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
							Logger.info("lvxmin " + lvxmin + "  lvymin " + lvymin + "  lvzmin " + lvzmin + "  lvxmax " + lvxmax + "  lvymax " + lvymax + "  lvzmax " + lvzmax +  "  xr " + (lvxmax - lvxmin + 1) +  "  yr " + (lvymax - lvymin + 1) +  "  zr " + (lvzmax - lvzmin + 1));
							for(int z = lvzmin; z <= lvzmax; z++) {
								for(int y = lvymin; y <= lvymax; y++) {
									for(int x = lvxmin; x <= lvxmax; x++) {
										ccnt[z - cvzmin][y - cvymin][x - cvxmin] = cnt[z - (int)vzmin][y - (int)vymin][x - (int)vxmin];
									}
								}
							}

							/*{
								int sxmin = Integer.MAX_VALUE;
								int symin = Integer.MAX_VALUE;
								int szmin = Integer.MAX_VALUE;
								int sxmax = Integer.MIN_VALUE;
								int symax = Integer.MIN_VALUE;
								int szmax = Integer.MIN_VALUE;
								int sum = 0;
								for(int z = 0; z < cellsize; z++) {
									for(int y = 0; y < cellsize; y++) {
										for(int x = 0; x < cellsize; x++) {
											int v = ccnt[z][y][x];
											if(v > 0) {
												sum++;
												if(x < sxmin) {
													sxmin = x;
												}
												if(sxmax < x) {
													sxmax = x;
												}
												if(y < symin) {
													symin = y;
												}
												if(symax < y) {
													symax = y;
												}
												if(z < szmin) {
													szmin = z;
												}
												if(szmax < z) {
													szmax = z;
												}
											}
										}
									}
								}
								Logger.info("vc " + sxmin + " " + symin + " " + szmin + "   " + sxmax + " "+ symax + " " + szmax + "     " + sum);
							}*/

							VoxelCell voxelCell = new VoxelCell(cx, cy, cz, ccnt);
							cellFactory.writeVoxelCell(voxelCell, t);
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
