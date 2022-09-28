package remotetask.voxeldb;

import org.json.JSONObject;
import org.tinylog.Logger;

import broker.Broker;
import broker.TimeSlice;
import broker.acl.ACL;
import pointcloud.CellTable;
import pointcloud.DoublePoint;
import pointcloud.PointCloud;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import util.Range2d;
import util.Range3d;
import voxeldb.CellFactory;
import voxeldb.VoxelDB;
import voxeldb.VoxelGeoRef;
import voxeldb.aggregatedprocessor.AggProcInt32ofInt32;
import voxeldb.aggregator.AggInt32ofInt32Sum;
import voxeldb.aggregator.base.AggInt32ofInt32;
import voxeldb.voxelmapper.VoxelMapperInt32;

@task_voxeldb("to_pointcloud3")
@Description("Convert voxels to pointcloud. One (filled) voxel leads to one pointcloud point.")
@Param(name="voxeldb", type="voxeldb", desc="VoxelDB layer. (source)", example="voxeldb1")
@Param(name="pointcloud", type="layer_id", desc="ID of new PointCloud layer. (target) (if layer exists, delete)", example="pointcloud1", required=false)
public class Task_to_pointcloud3 extends RemoteTask {

	private final Broker broker;
	private final JSONObject task;
	private final VoxelDB voxeldb;	
	private final String pointcloud_name;

	public Task_to_pointcloud3(Context ctx) {
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
		VoxelGeoRef voxelRef = voxeldb.geoRef();

		double voxelSizeX = voxelRef.voxelSizeX;
		double voxelSizeY = voxelRef.voxelSizeY;
		double voxelSizeZ = voxelRef.voxelSizeZ;
		Logger.info("voxel size " + voxelSizeX + " " + voxelSizeY + " " + voxelSizeZ);
		if(voxelSizeX != voxelSizeY) {
			throw new RuntimeException("voxel to pointcloud transformation implemented for voxelSizeX = voxelSizeY only.   " + voxelSizeX + "   " + voxelSizeY);
		}

		//int voxelCellSize = voxeldb.getCellsize();
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

		double pointcloudCellScale = 1d / voxelSizeX;
		Logger.info("pointcloud cell scale " + pointcloudCellScale);
		pointcloud.trySetCellscale(pointcloudCellScale);

		long pointOriginX = (long) Math.floor(voxelRef.originX * pointcloudCellScale);
		long pointOriginY = (long) Math.floor(voxelRef.originY * pointcloudCellScale);
		Logger.info("point origin " + pointOriginX + " " + pointOriginY);

		pointcloud.trySetCellsize(250 / pointcloudCellScale);
		double pointCellSize = pointcloud.getCellsize();
		Logger.info("point cell size " + pointCellSize);

		int pointCellLocalMax = pointcloud.getCellLocalMax();
		Logger.info("point cell local max " + pointCellLocalMax);

		double pointCellOffsetX_ = Math.floor(voxelRef.originX / pointCellSize);
		double pointCellOffsetY_ = Math.floor(voxelRef.originY / pointCellSize);
		DoublePoint off = pointcloud.getOrSetCelloffset(pointCellOffsetX_, pointCellOffsetY_);
		long pointCellOffsetX = (long) off.x;
		long pointCellOffsetY = (long) off.y;
		Logger.info("point cell offset " + pointCellOffsetX + " " + pointCellOffsetY);

		Range3d range = voxeldb.getLocalRange(true);
		Logger.info("voxel " + range);
		VoxelGeoRef ref = voxeldb.geoRef();
		double geoMinX = ref.voxelXtoGeo(range.xmin);
		double geoMinY = ref.voxelYtoGeo(range.ymin);
		double geoMaxX = ref.voxelXtoGeo(range.xmax);
		double geoMaxY = ref.voxelYtoGeo(range.ymax);
		int pointCellMinX = (int) (((long) Math.floor(geoMinX / pointCellSize)) - pointCellOffsetX);
		int pointCellMinY = (int) (((long) Math.floor(geoMinY / pointCellSize)) - pointCellOffsetY);
		int pointCellMaxX = (int) (((long) Math.floor(geoMaxX / pointCellSize)) - pointCellOffsetX);
		int pointCellMaxY = (int) (((long) Math.floor(geoMaxY / pointCellSize)) - pointCellOffsetY);
		Logger.info("point cell range " + pointCellMinX + " " + pointCellMinY + " - " + pointCellMaxX + " " + pointCellMaxY);
		
		CellFactory cellFactory = new CellFactory(voxeldb);
		VoxelMapperInt32 mapper = cellFactory.registerMapper("count");
		AggInt32ofInt32 agg = new AggInt32ofInt32Sum();			

		try {
			for(TimeSlice timeSlice : voxeldb.timeMapReadonly.values()) {
				Logger.info("timeslice " + timeSlice.name);
				pointcloud.setTimeSlice(timeSlice);
				log("timeslice " + timeSlice.name);
				for(int cy = pointCellMinY; cy <= pointCellMaxY; cy++) {
					double geoCellMinY = (cy + pointCellOffsetY) * pointCellSize;
					double geoCellMaxY = geoCellMinY + pointCellLocalMax / pointcloudCellScale;
					for(int cx = pointCellMinX; cx <= pointCellMaxX; cx++) {
						double geoCellMinX = (cx + pointCellOffsetX) * pointCellSize;
						double geoCellMaxX = geoCellMinX + pointCellLocalMax / pointcloudCellScale;
						Logger.info("geo cell " + geoCellMinX + " " + geoCellMinY + " - " + geoCellMaxX + " " + geoCellMaxY);
						Range2d cellRange2d = ref.geoToRange(geoCellMinX, geoCellMinY, geoCellMaxX, geoCellMaxY);
						Range3d cellRange3d = Range3d.of(cellRange2d, range.zmin, range.zmax);
						Logger.info("cell voxel " + cellRange3d);

						double aggOriginX = ref.voxelXtoGeo(cellRange3d.xmin);
						double aggOriginY = ref.voxelYtoGeo(cellRange3d.ymin);
						double aggOriginZ = ref.voxelZtoGeo(cellRange3d.zmin);
						double aggVoxelSizeX = ref.voxelSizeX * 1;
						double aggVoxelSizeY = ref.voxelSizeY * 1;
						double aggVoxelSizeZ = ref.voxelSizeZ * 1;
						VoxelGeoRef aggRef = ref.with(aggOriginX, aggOriginY, aggOriginZ, aggVoxelSizeX, aggVoxelSizeY, aggVoxelSizeZ);

						setMessage("timeslice " + timeSlice.name + "  cell " + cx + " " + cy);

						AggProcInt32ofInt32 aggProc = new AggProcInt32ofInt32(cellFactory, cellRange3d, 1, 1, 1, aggRef, mapper, agg);
						cellFactory.getVoxelCells(timeSlice, range).forEach(aggProc);
						aggProc.finish();
						int[][][] cnt = aggProc.getData();

						int maxPointCount = cellRange3d.xyzlen();
						int[] xs = new int[maxPointCount];
						int[] ys = new int[maxPointCount];
						int[] zs = new int[maxPointCount];
						char[] is = new char[maxPointCount];
						int pos = 0;
						int xlen = cellRange3d.xlen();
						int ylen = cellRange3d.ylen();
						int zlen = cellRange3d.zlen();

						for(int zi = 0; zi < zlen; zi++) {
							int[][] cntZ = cnt[zi];
							for(int yi = 0; yi < ylen; yi++) {
								int[] cntZY = cntZ[yi];
								for(int xi = 0; xi < xlen; xi++) {
									int v = cntZY[xi];
									if(v > 0) {
										xs[pos] = xi;
										ys[pos] = yi;
										zs[pos] = zi;
										is[pos] = Character.MAX_VALUE < v ? Character.MAX_VALUE : (char) v;
										pos++;
									}
								}
							}
						}
						
						Logger.info("point count " + pos);

						int z = 0;
						CellTable cellTable = new CellTable(cx, cy, z, pos);
						cellTable.x = xs;
						cellTable.y = ys;
						cellTable.z = zs;
						cellTable.intensity = is;

						CellTable oldCellTable = pointcloud.getCellTable(cellTable.cx, cellTable.cy, cellTable.cz);
						if(oldCellTable != null) {
							Logger.info("merge " + cellTable.cx + " " + cellTable.cy);
							cellTable = CellTable.merge(oldCellTable, cellTable);
						}
						rasterunit.Tile tile;
						tile = pointcloud.createTile(cellTable, cellTable.cx, cellTable.cy, cellTable.cz, timeSlice.id, Integer.MIN_VALUE);
						pointcloud.writeTile(tile);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		pointcloud.close();
		voxeldb.close();
	}
}
