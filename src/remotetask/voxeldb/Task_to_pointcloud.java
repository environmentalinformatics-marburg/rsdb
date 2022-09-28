package remotetask.voxeldb;

import org.json.JSONObject;
import org.tinylog.Logger;

import broker.Broker;
import broker.TimeSlice;
import broker.acl.ACL;
import pointcloud.CellTable;
import pointcloud.PointCloud;
import rasterunit.Tile;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import voxeldb.CellFactory;
import voxeldb.VoxelDB;

@task_voxeldb("to_pointcloud")
@Description("Convert voxels to pointcloud. One (filled) voxel leads to one pointcloud point.")
@Param(name="voxeldb", type="voxeldb", desc="VoxelDB layer. (source)", example="voxeldb1")
@Param(name="pointcloud", type="layer_id", desc="ID of new PointCloud layer. (target) (if layer exists, delete)", example="pointcloud1", required=false)
public class Task_to_pointcloud extends RemoteTask {

	private final Broker broker;
	private final JSONObject task;
	private final VoxelDB voxeldb;	
	private final String pointcloud_name;

	public Task_to_pointcloud(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String voxeldb_name = task.getString("voxeldb");
		this.voxeldb = broker.getVoxeldb(voxeldb_name);
		voxeldb.check(ctx.userIdentity, "task voxeldb to_pointcloud");
		this.pointcloud_name = task.optString("pointcloud", voxeldb.getName() + "_pointcloud");
		if(broker.hasPointCloud(pointcloud_name)) {
			PointCloud pointcloud = broker.getPointCloud(pointcloud_name);
			pointcloud.checkMod(ctx.userIdentity, "task voxeldb to_pointcloud of existing name");
			pointcloud.close();
		}
	}

	@Override
	public void process() {
		int cellsize = voxeldb.getCellsize();
					
		String storage_type = task.optString("storage_type", "TileStorage");
		boolean transactions = task.optBoolean("transactions", false);
		setMessage("prepare pointcloud layer");
		broker.deletePointCloud(pointcloud_name);
		PointCloud pointcloud = broker.createNewPointCloud(pointcloud_name, storage_type, transactions);
		
		pointcloud.setACL(voxeldb.getACL());
		pointcloud.setACL_mod(voxeldb.getACL_mod());
		if(ctx.userIdentity != null) {
			String username = ctx.userIdentity.getUserPrincipal().getName();
			pointcloud.setACL_owner(ACL.ofRole(username));
		}
		
		pointcloud.setProj4(voxeldb.geoRef().proj4);
		pointcloud.setCodeEPSG(voxeldb.geoRef().epsg);
		double voxelSizeX = voxeldb.geoRef().voxelSizeX;
		double voxelSizeY = voxeldb.geoRef().voxelSizeY;
		double voxelSizeZ = voxeldb.geoRef().voxelSizeZ;
		Logger.info("voxel size " + voxelSizeX + " " + voxelSizeY + " " + voxelSizeZ);
		if(voxelSizeX != voxelSizeY) {
			throw new RuntimeException("voxel to pointcloud transformation implemented for voxelSizeX = voxelSizeY only.   " + voxelSizeX + "   " + voxelSizeY);
		}
		
		double pointcloudCellScale = 1d / voxelSizeX;
		Logger.info("pointcloud cell scale " + pointcloudCellScale);
		pointcloud.trySetCellscale(pointcloudCellScale);
		
		double pointcloudCellSize = cellsize * voxelSizeX;
		Logger.info("pointcloud cell size " + pointcloudCellSize);
		pointcloud.trySetCellsize(pointcloudCellSize);
		
		//double originX = voxeldb.geoRef().originX;
		//double originY = voxeldb.geoRef().originY;
		double originZ = voxeldb.geoRef().originZ;
		//Logger.info("voxel origin " + originX + " " + originY + " " + originZ);
		//pointcloud.getOrSetCelloffset(originX, originY);
		int integerOriginZ = (int) Math.round(originZ / voxelSizeZ);
		Logger.info("pointcloud integer origin z " + integerOriginZ);

		CellFactory cf = CellFactory.ofCount(voxeldb);
		for(TimeSlice timeSlice : voxeldb.timeMapReadonly.values()) {
			pointcloud.setTimeSlice(timeSlice);
			cf.getVoxelCells(timeSlice).sequential().forEach(voxelCell -> {
				try {
					int sum = 0;
					int[][][] cnt = voxelCell.cnt;

					int len = cellsize * cellsize * cellsize;
					int[] xs = new int[len];
					int[] ys = new int[len];
					int[] zs = new int[len];
					char[] is = new char[len];
					int pos = 0;

					int xmin = Integer.MAX_VALUE;
					int ymin = Integer.MAX_VALUE;
					int zmin = Integer.MAX_VALUE;
					int xmax = Integer.MIN_VALUE;
					int ymax = Integer.MIN_VALUE;
					int zmax = Integer.MIN_VALUE;			

					for(int i = 0; i < len; i++) {
						double x = xs[i];

					}

					for(int zi = 0; zi < cellsize; zi++) {
						int z = voxelCell.z * cellsize + zi + integerOriginZ;  
						for(int y = 0; y < cellsize; y++) {
							for(int x = 0; x < cellsize; x++) {
								int v = cnt[zi][y][x];
								if(v > 0) {
									xs[pos] = x;
									ys[pos] = y;
									zs[pos] = z;
									is[pos] = Character.MAX_VALUE < v ? Character.MAX_VALUE : (char) v;
									pos++;
									sum += v;

									if(x < xmin) {
										xmin = x;
									}
									if(xmax < x) {
										xmax = x;
									}
									if(y < ymin) {
										ymin = y;
									}
									if(ymax < y) {
										ymax = y;
									}
									if(z < zmin) {
										zmin = z;
									}
									if(zmax < z) {
										zmax = z;
									}
								}
							}
						}
					}

					Logger.info("VoxelCell " + xmin + " " + ymin + " " + zmin + "   " + xmax + " "+ ymax + " " + zmax);

					int z = 0;
					CellTable cellTable = new CellTable(voxelCell.x, voxelCell.y, z, pos);
					cellTable.x = xs;
					cellTable.y = ys;
					cellTable.z = zs;
					cellTable.intensity = is;

					CellTable oldCellTable = pointcloud.getCellTable(cellTable.cx, cellTable.cy, cellTable.cz);
					if(oldCellTable != null) {
						cellTable = CellTable.merge(oldCellTable, cellTable);
					}
					Tile tile = pointcloud.createTile(cellTable, cellTable.cx, cellTable.cy, cellTable.cz, timeSlice.id, Integer.MIN_VALUE);
					pointcloud.writeTile(tile);			

					Logger.info(voxelCell + "  " + sum);	
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}

		pointcloud.close();
		voxeldb.close();
	}
}
