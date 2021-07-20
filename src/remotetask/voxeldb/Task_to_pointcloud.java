package remotetask.voxeldb;

import java.util.Collection;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import broker.Broker;
import broker.TimeSlice;
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
@Description("Convert voxels to pointcloud. One (filled) voxel leads to one pointcloud point. Currently first timeslice only.")
@Param(name="voxeldb", type="voxeldb", desc="VoxelDB layer. (source)", example="voxeldb1")
@Param(name="pointcloud", type="layer_id", desc="ID of new PointCloud layer. (target) (if layer exists, delete)", example="pointcloud1", required=false)
public class Task_to_pointcloud extends RemoteTask {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;

	public Task_to_pointcloud(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
	}

	@Override
	public void process() {
		String voxeldb_name = task.getString("voxeldb");
		VoxelDB voxeldb = broker.getVoxeldb(voxeldb_name);

		int cellsize = voxeldb.getCellsize();

		String pointcloud_name = task.optString("pointcloud", voxeldb.getName() + "_pointcloud");			
		String storage_type = task.optString("storage_type", "TileStorage");
		boolean transactions = task.optBoolean("transactions", false);
		setMessage("prepare pointcloud layer");
		broker.deletePointCloud(pointcloud_name);
		PointCloud pointcloud = broker.createNewPointCloud(pointcloud_name, storage_type, transactions);
		
		pointcloud.setACL(voxeldb.getACL());
		pointcloud.setACL_mod(voxeldb.getACL_mod());
		
		pointcloud.setProj4(voxeldb.geoRef().proj4);
		pointcloud.setCodeEPSG(voxeldb.geoRef().epsg);
		double cellscale = voxeldb.geoRef().voxelSizeX;
		pointcloud.trySetCellscale(1d / cellscale);
		pointcloud.trySetCellsize(cellsize * cellscale);
		pointcloud.getOrSetCelloffset(0, 0);

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
						int z = voxelCell.z * cellsize + zi;  
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

					log.info("VoxelCell " + xmin + " " + ymin + " " + zmin + "   " + xmax + " "+ ymax + " " + zmax);

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

					log.info(voxelCell + "  " + sum);	
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}

		pointcloud.close();
		voxeldb.close();


	}


}
