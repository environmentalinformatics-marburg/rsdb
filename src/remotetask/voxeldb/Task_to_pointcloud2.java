package remotetask.voxeldb;

import org.json.JSONObject;
import org.tinylog.Logger;

import broker.Broker;
import broker.TimeSlice;
import broker.acl.ACL;
import pointcloud.CellTable;
import pointcloud.P2d;
import pointcloud.PointCloud;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import voxeldb.CellFactory;
import voxeldb.VoxelDB;
import voxeldb.VoxelGeoRef;

@task_voxeldb("to_pointcloud2")
@Description("Convert voxels to pointcloud. One (filled) voxel leads to one pointcloud point")
@Param(name="voxeldb", type="voxeldb", desc="VoxelDB layer. (source)", example="voxeldb1")
@Param(name="pointcloud", type="layer_id", desc="ID of new PointCloud layer. (target) (if layer exists, delete)", example="pointcloud1", required=false)
public class Task_to_pointcloud2 extends RemoteTask {

	private final Broker broker;
	private final JSONObject task;
	private final VoxelDB voxeldb;	
	private final String pointcloud_name;

	public Task_to_pointcloud2(Context ctx) {
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

		int voxelCellSize = voxeldb.getCellsize();
		int voxelCellSize_1 = voxelCellSize - 1;

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

		//pointcloud.trySetCellsize(10);
		double pointCellSize = pointcloud.getCellsize();
		Logger.info("point cell size " + pointCellSize);

		int pointCellLocalMax = pointcloud.getCellLocalMax();
		Logger.info("point cell local max " + pointCellLocalMax);

		double pointCellOffsetX_ = Math.floor(voxelRef.originX / pointCellSize);
		double pointCellOffsetY_ = Math.floor(voxelRef.originY / pointCellSize);
		P2d off = pointcloud.getOrSetCelloffset(pointCellOffsetX_, pointCellOffsetY_);
		double pointCellOffsetX = off.x;
		double pointCellOffsetY = off.y;
		Logger.info("point cell offset " + pointCellOffsetX + " " + pointCellOffsetY);

		CellFactory cf = CellFactory.ofCount(voxeldb);
		for(TimeSlice timeSlice : voxeldb.timeMapReadonly.values()) {
			pointcloud.setTimeSlice(timeSlice);
			cf.getVoxelCells(timeSlice).sequential().forEach(voxelCell -> {
				try {
					long voxelMinX = pointOriginX + voxelCell.x * voxelCellSize;
					long voxelMinY = pointOriginY + voxelCell.y * voxelCellSize;
					long voxelMaxX = voxelMinX + voxelCellSize_1;
					long voxelMaxY = voxelMinY + voxelCellSize_1;
					Logger.info("voxel range " + voxelMinX + " " + voxelMinY + " - " + voxelMaxX + " " + voxelMaxY);
					int pointCellMinX = (int) (Math.floor(voxelMinX / pointCellSize) - pointCellOffsetX);
					int pointCellMinY = (int) (Math.floor(voxelMinY / pointCellSize) - pointCellOffsetY);
					int pointCellMaxX = (int) (Math.floor(voxelMaxX / pointCellSize) - pointCellOffsetX);
					int pointCellMaxY = (int) (Math.floor(voxelMaxY / pointCellSize) - pointCellOffsetY);
					Logger.info("point cell min " + pointCellMinX + " " + pointCellMinY);
					Logger.info("point cell max " + pointCellMaxX + " " + pointCellMaxY);

					for(int cy = pointCellMinY; cy <= pointCellMaxY; cy++) {
						for(int cx = pointCellMinX; cx <= pointCellMaxX; cx++) {
							Logger.info("cell " + cx + " " + cy);
							long subsetMinX = (long) ((cx + pointCellOffsetX) * pointCellSize);
							long subsetMinY = (long) ((cy + pointCellOffsetY) * pointCellSize);
							long subsetMaxX = subsetMinX + pointCellLocalMax;
							long subsetMaxY = subsetMinY + pointCellLocalMax;
							Logger.info("sub  range " + subsetMinX + " " + subsetMinY + " - " + subsetMaxX + " " + subsetMaxY);
							subsetMinX = Math.max(subsetMinX, voxelMinX);
							subsetMinY = Math.max(subsetMinY, voxelMinY);
							subsetMaxX = Math.min(subsetMaxX, voxelMaxX);
							subsetMaxY = Math.min(subsetMaxY, voxelMaxY);
							Logger.info("sub+ range " + subsetMinX + " " + subsetMinY + " - " + subsetMaxX + " " + subsetMaxY);
							int voxelLocalMinX = (int) (subsetMinX - voxelMinX);
							int voxelLocalMinY = (int) (subsetMinY - voxelMinY);
							int voxelLocalMaxX = (int) (subsetMaxX - voxelMinX);
							int voxelLocalMaxY = (int) (subsetMaxY - voxelMinY);
							Logger.info("sub+ local " + voxelLocalMinX + " " + voxelLocalMinY + " - " + voxelLocalMaxX + " " + voxelLocalMaxY);

							int maxPointCount = (voxelLocalMaxX - voxelLocalMinX + 1) * (voxelLocalMaxY - voxelLocalMinY + 1) * voxelCellSize;
							int[] xs = new int[maxPointCount];
							int[] ys = new int[maxPointCount];
							int[] zs = new int[maxPointCount];
							char[] is = new char[maxPointCount];
							int pos = 0;

							int[][][] cnt = voxelCell.cnt;

							for(int zi = 0; zi < voxelCellSize; zi++) {
								int[][] cntZ = cnt[zi];
								for(int yi = voxelLocalMinY; yi <= voxelLocalMaxY; yi++) {
									int[] cntZY = cntZ[yi];
									for(int xi = voxelLocalMinX; xi <= voxelLocalMaxX; xi++) {
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
							rasterunit.Tile tile;
							tile = pointcloud.createTile(cellTable, cellTable.cx, cellTable.cy, cellTable.cz, timeSlice.id, Integer.MIN_VALUE);
							pointcloud.writeTile(tile);							
						}
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}

		pointcloud.close();
		voxeldb.close();

		/*double voxelSizeX = voxeldb.geoRef().voxelSizeX;
		double voxelSizeY = voxeldb.geoRef().voxelSizeY;
		double voxelSizeZ = voxeldb.geoRef().voxelSizeZ;
		Logger.info("voxel size " + voxelSizeX + " " + voxelSizeY + " " + voxelSizeZ);
		if(voxelSizeX != voxelSizeY) {
			throw new RuntimeException("voxel to pointcloud transformation implemented for voxelSizeX = voxelSizeY only.   " + voxelSizeX + "   " + voxelSizeY);
		}

		double pointcloudCellScale = 1d / voxelSizeX;
		Logger.info("pointcloud cell scale " + pointcloudCellScale);
		pointcloud.trySetCellscale(pointcloudCellScale);

		double voxelCellGeoSizeX = voxelCellsize * voxelSizeX;
		double voxelCellGeoSizeY = voxelCellsize * voxelSizeY;
		double voxelCellGeoSizeZ = voxelCellsize * voxelSizeZ;

		double pointcloudCellSize = voxelCellsize * voxelSizeX;
		Logger.info("pointcloud cell size " + pointcloudCellSize);
		pointcloud.trySetCellsize(pointcloudCellSize);

		double voxelOriginX = voxeldb.geoRef().originX;
		double voxelOriginY = voxeldb.geoRef().originY;
		double voxelOriginZ = voxeldb.geoRef().originZ;
		//Logger.info("voxel origin " + originX + " " + originY + " " + originZ);
		//pointcloud.getOrSetCelloffset(originX, originY);
		int integerOriginZ = (int) Math.round(voxelOriginZ / voxelSizeZ);
		Logger.info("pointcloud integer origin z " + integerOriginZ);

		CellFactory cf = CellFactory.ofCount(voxeldb);
		for(TimeSlice timeSlice : voxeldb.timeMapReadonly.values()) {
			pointcloud.setTimeSlice(timeSlice);
			cf.getVoxelCells(timeSlice).sequential().forEach(voxelCell -> {

				double voxelGeoOffsetX = voxelOriginX + voxelCell.x * voxelCellGeoSizeX;
				double voxelGeoOffsetY = voxelOriginY + voxelCell.y * voxelCellGeoSizeY;
				double voxelGeoOffsetZ = voxelOriginZ + voxelCell.z * voxelCellGeoSizeZ;
				int integerOffsetX = (int) Math.floor(voxelGeoOffsetX / pointcloudCellSize);

				try {
					int sum = 0;
					int[][][] cnt = voxelCell.cnt;

					double[] xs = new double[cellLen];
					double[] ys = new double[cellLen];
					double[] zs = new double[cellLen];
					char[] is = new char[cellLen];
					int pos = 0;

					int xmin = Integer.MAX_VALUE;
					int ymin = Integer.MAX_VALUE;
					int zmin = Integer.MAX_VALUE;
					int xmax = Integer.MIN_VALUE;
					int ymax = Integer.MIN_VALUE;
					int zmax = Integer.MIN_VALUE;			

					for(int zi = 0; zi < voxelCellsize; zi++) {
						double z = voxelGeoOffsetZ + zi * voxelCellGeoSizeX;  
						for(int yi = 0; yi < voxelCellsize; yi++) {
							double y = voxelGeoOffsetY + yi * voxelCellGeoSizeY;
							for(int xi = 0; xi < voxelCellsize; xi++) {
								int v = cnt[zi][yi][xi];
								if(v > 0) {
									xs[pos] = voxelGeoOffsetX + xi * voxelCellGeoSizeX;
									ys[pos] = y;
									zs[pos] = z;
									is[pos] = Character.MAX_VALUE < v ? Character.MAX_VALUE : (char) v;
									pos++;
									sum += v;

									if(xi < xmin) {
										xmin = xi;
									}
									if(xmax < xi) {
										xmax = xi;
									}
									if(yi < ymin) {
										ymin = yi;
									}
									if(ymax < yi) {
										ymax = yi;
									}
									if(z < zmin) {
										zmin = zi;
									}
									if(zmax < z) {
										zmax = zi;
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
		voxeldb.close();*/
	}
}
