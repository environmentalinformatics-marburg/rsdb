package remotetask.voxeldb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import broker.Broker;
import pointcloud.CellTable;
import pointcloud.PointCloud;
import rasterunit.Tile;
import remotetask.CancelableRemoteTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import voxeldb.VoxelDB;

@task_voxeldb("voxel_to_pointcloud")
@Description("Convert voxels to pointcloud.")
@Param(name="voxeldb", type="layer_id", desc="VoxelDB layer. (source)", example="voxeldb1")
@Param(name="pointcloud", type="layer_id", desc="ID of new PointCloud layer. (target) (if layer exists, delete)", example="pointcloud1")
public class Task_voxel_to_pointcloud extends CancelableRemoteTask {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;

	public Task_voxel_to_pointcloud(Context ctx) {
		this.broker = ctx.broker;
		this.task = ctx.task;
	}

	@Override
	public void process() {
		String voxeldb_name = task.getString("voxeldb");
		VoxelDB voxeldb = broker.getVoxeldb(voxeldb_name);
		
		int cellsize = voxeldb.getCellsize();

		if(!task.has("pointcloud")) {
			throw new RuntimeException("missing parameter 'pointcloud'");
		}
		String pointcloud_name = task.getString("pointcloud");
		String storage_type = task.optString("storage_type", "TileStorage");
		boolean transactions = task.optBoolean("transactions", false);
		setMessage("prepare pointcloud layer");
		broker.deletePointCloud(pointcloud_name);
		PointCloud pointcloud = broker.createNewPointCloud(pointcloud_name, storage_type, transactions);
		pointcloud.setProj4(voxeldb.geoRef().proj4);
		pointcloud.setCodeEPSG(voxeldb.geoRef().epsg);
		double cellscale = voxeldb.geoRef().voxelSizeX;
		pointcloud.trySetCellscale(1d / cellscale);
		pointcloud.trySetCellsize(cellsize * cellscale);
		pointcloud.getOrSetCelloffset(0, 0);


		voxeldb.getVoxelCells().sequential().forEach(voxelCell -> {
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
			
			CellTable cellTable = new CellTable(voxelCell.x, voxelCell.y, 0, pos);
			cellTable.x = xs;
			cellTable.y = ys;
			cellTable.z = zs;
			cellTable.intensity = is;
			
			CellTable oldCellTable = pointcloud.getCellTable(cellTable.cx, cellTable.cy, cellTable.cz);
			if(oldCellTable != null) {
				cellTable = CellTable.merge(oldCellTable, cellTable);
			}
			Tile tile = pointcloud.createTile(cellTable, cellTable.cx, cellTable.cy, cellTable.cz);
			pointcloud.writeTile(tile);			

			log.info(voxelCell + "  " + sum);	
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});


		pointcloud.close();
		voxeldb.close();


	}


}
