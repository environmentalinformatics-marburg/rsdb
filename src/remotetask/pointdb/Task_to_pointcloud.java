package remotetask.pointdb;

import java.util.TreeSet;

import org.json.JSONObject;
import org.tinylog.Logger;

import broker.Broker;
import broker.acl.ACL;
import pointcloud.CellTable;
import pointcloud.DoublePoint;
import pointcloud.PointCloud;
import pointdb.DBTileKeyProducer;
import pointdb.PointDB;
import pointdb.base.PdbConst;
import pointdb.base.Point;
import pointdb.base.Rect;
import pointdb.base.Tile;
import pointdb.base.TileKey;
import pointdb.processing.tilekey.TileKeyIsEmptyCollector;
import pointdb.processing.tilemeta.StatisticsCreator.Statistics;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import util.Timer;
import util.collections.vec.Vec;

@task_pointdb("to_pointcloud")
@Description("Create pointcloud layer from pointdb layer.")
@Param(name="pointdb", type="pointdb", desc="ID of PointDB layer. (source)", example="pointdb1")
@Param(name="pointcloud", type="layer_id", desc="ID of new PointCloud layer. (target) (if layer exists, delete)", example="pointcloud1")
@Param(name="storage_type", desc="Storage type of new PointCloud. (default: TileStorage)", format="RasterUnit or TileStorage", example="TileStorage", required=false)
@Param(name="transactions", type="boolean", desc="Use power failer safe (and slow) PointCloud operation mode. (RasterUnit only, default false)", example="false", required=false)
public class Task_to_pointcloud extends RemoteTask{

	private final Broker broker;
	private final JSONObject task;
	private final PointDB pointdb;
	private final String pointcloud_name;

	public static class Commiter implements AutoCloseable {
		public static final int maxTileWriteCount = 512;
		private final PointCloud pointcloud;
		private int tileWriteCount = 0;
		private long totalWriteCount = 0;

		public Commiter(PointCloud pointcloud) {
			this.pointcloud = pointcloud;
		}

		public void add(int tilesWrittenInBand) {
			tileWriteCount += tilesWrittenInBand;
			checkCommit();
		}

		public void addOne() {
			tileWriteCount++;
			checkCommit();
		}

		private void checkCommit() {
			if(maxTileWriteCount <= tileWriteCount) {
				commit();
				totalWriteCount += tileWriteCount;
				tileWriteCount = 0;
			}
		}

		public void checkFinishCommit() {
			if(0 < tileWriteCount) {
				commit();
				totalWriteCount += tileWriteCount;
				tileWriteCount = 0;
			}
		}
		
		private void commit() {
			Timer.start("to_pointdb commit");
			Logger.info(Timer.stop("to_pointdb commit between"));
			pointcloud.commit();
			Logger.info(Timer.stop("to_pointdb commit"));
			Timer.start("to_pointdb commit between");
		}

		public long getTotalWriteCount() {
			return totalWriteCount;
		}

		@Override
		public void close() throws Exception {
			checkFinishCommit();			
			pointcloud.getGriddb().storage().flush();
		}
	}

	public Task_to_pointcloud(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		if(!task.has("pointdb")) {
			throw new RuntimeException("missing parameter 'pointdb'");
		}
		String pointdb_name = task.getString("pointdb");
		pointdb = broker.getPointdb(pointdb_name);		
		pointdb.check(ctx.userIdentity, "task pointcloud to_pointcloud"); // check needed as same ACLs are assigned to target pointcloud, no ACL_mod in PointDB
		
		if(!task.has("pointcloud")) {
			throw new RuntimeException("missing parameter 'pointcloud'");
		}
		this.pointcloud_name = task.getString("pointcloud");
		if(broker.hasPointCloud(pointcloud_name)) {
			PointCloud pointcloud = broker.getPointCloud(pointcloud_name);
			pointcloud.checkMod(ctx.userIdentity, "task pointcloud to_pointcloud of existing name");
			pointcloud.close();
		}
	}

	@Override
	public void process() {
		try {			
			String storage_type = task.optString("storage_type", "TileStorage");
			boolean transactions = task.optBoolean("transactions", false);
			setMessage("prepare pointcloud layer");
			broker.deletePointCloud(pointcloud_name);
			PointCloud pointcloud = broker.createNewPointCloud(pointcloud_name, storage_type, transactions);
			pointcloud.setACL(pointdb.getACL());
			pointcloud.setACL_mod(pointdb.getACL()); // no ACL_mod in PointDB
			if(ctx.userIdentity != null) {
				String username = ctx.userIdentity.getUserPrincipal().getName();
				pointcloud.setACL_owner(ACL.ofRole(username));
			}

			setMessage("verify pointdb layer");
			Statistics stat = pointdb.tileMetaProducer(null).toStatistics();
			Logger.info(stat.tile_x_min+"  "+stat.tile_x_max+"          "+(stat.tile_x_max - stat.tile_x_min + PdbConst.UTM_TILE_SIZE));
			Logger.info(stat.tile_y_min+"  "+stat.tile_y_max+"          "+(stat.tile_y_max - stat.tile_y_min + PdbConst.UTM_TILE_SIZE));
			Logger.info("utmm tile size " + PdbConst.LOCAL_TILE_SIZE);
			Logger.info("utm tile size " + PdbConst.UTM_TILE_SIZE);
			Logger.info("tiles " + stat.tile_sum);


			setMessage("set pointcloud parameters");
			pointcloud.trySetCellsize(100);
			double cellsize = pointcloud.getCellsize();
			long utmm_cellsize = PdbConst.to_utmm(cellsize);
			long utmm_cellsize_minus_one = utmm_cellsize - 1;
			DoublePoint celloffset = pointcloud.getOrSetCelloffset(Math.floor(stat.tile_x_min / cellsize), Math.floor(stat.tile_y_min / cellsize));
			long utmm_pointdb_min_x = PdbConst.to_utmm(stat.tile_x_min);
			long utmm_pointdb_min_y = PdbConst.to_utmm(stat.tile_y_min);
			long utmm_pointdb_max_x = PdbConst.to_utmm(stat.tile_x_max) + PdbConst.LOCAL_TILE_SIZE_MINUS_ONE;
			long utmm_pointdb_max_y = PdbConst.to_utmm(stat.tile_y_max) + PdbConst.LOCAL_TILE_SIZE_MINUS_ONE;
			long utmm_min_x = PdbConst.to_utmm(celloffset.x * cellsize);
			long utmm_min_y = PdbConst.to_utmm(celloffset.y * cellsize);
			if(utmm_pointdb_min_x < utmm_min_x || utmm_pointdb_min_y < utmm_min_y) {
				throw new RuntimeException("init error");
			}
			pointcloud.setCodeEPSG(pointdb.config.getEPSG());
			pointcloud.setProj4(pointdb.config.getProj4());
			double cellScale = pointcloud.getCellscale();

			double LOCAL_SCALE = PdbConst.LOCAL_SCALE_FACTOR;

			setMessage("load pointdb tile keys");
			TreeSet<TileKey> cachedKeySet = new TreeSet<TileKey>(pointdb.tileMetaMap.keySet());

			setMessage("start transfer points");
			long sourceTileCount = 0;
			long targetTileCount = 0;
			try(Commiter commiter = new Commiter(pointcloud)) { 

				long utmm_pos_y = utmm_min_y;		
				while(utmm_pos_y <= utmm_pointdb_max_y) {
					long utmm_pos_x = utmm_min_x;
					while(utmm_pos_x <= utmm_pointdb_max_x) {
						long utmm_cell_min_x = utmm_pos_x;
						long utmm_cell_min_y = utmm_pos_y;
						long utmm_cell_max_x = utmm_cell_min_x + utmm_cellsize_minus_one;
						long utmm_cell_max_y = utmm_cell_min_y + utmm_cellsize_minus_one;
						Rect rect = Rect.of_UTMM(utmm_cell_min_x, utmm_cell_min_y, utmm_cell_max_x, utmm_cell_max_y);
						//Logger.info("rect "+rect);

						DBTileKeyProducer tileKeyProducer = DBTileKeyProducer.of(pointdb, rect);
						TileKeyIsEmptyCollector.Processor tileKeyConsumer = new TileKeyIsEmptyCollector.Processor();
						tileKeyProducer.produce(cachedKeySet, tileKeyConsumer);					
						if(!tileKeyConsumer.isEmpty) {
							Timer.start("to_pointdb tile");
							double utm_cell_min_x = PdbConst.utmmToDouble(utmm_cell_min_x);
							double utm_cell_min_y = PdbConst.utmmToDouble(utmm_cell_min_y);
							Timer.start("to_pointdb read");
							Vec<Tile> tiles = pointdb.tileProducer(rect).toVec();
							sourceTileCount += tiles.size();
							Logger.info(Timer.stop("to_pointdb read"));
							int len = tiles.elementSum(Tile::size);
							Logger.info("tiles " + tiles.size()+"   "+len);
							int[] xs = new int[len];
							int[] ys = new int[len];
							int[] zs = new int[len];
							char[] intensity = new char[len];
							byte[] returnNumber = new byte[len];
							byte[] returns = new byte[len];
							byte[] scanAngleRank = new byte[len];
							byte[] classification = new byte[len];
							int cnt = 0;
							for(Tile tile:tiles) {
								double tile_x = tile.meta.x;
								double tile_y = tile.meta.y;
								for(Point point:tile.points) {
									double x = tile_x+point.x/LOCAL_SCALE;
									double y = tile_y+point.y/LOCAL_SCALE;
									double z = point.z/LOCAL_SCALE;
									double xx = (x - utm_cell_min_x) * cellScale;
									double yy = (y - utm_cell_min_y) * cellScale;
									double zz = z * cellScale;
									//Logger.info(x+"  "+xx+"     "+y+"  "+yy+"     "+z+"  "+zz);
									xs[cnt] = (int) xx;
									ys[cnt] = (int) yy;
									zs[cnt] = (int) zz;
									intensity[cnt] = point.intensity;
									returnNumber[cnt] = point.returnNumber;
									returns[cnt] = point.returns;
									scanAngleRank[cnt] = point.scanAngleRank;
									classification[cnt] = point.classification;
									cnt++;
								}
							}
							if(cnt != len) {
								throw new RuntimeException("processing error");
							}
							int cell_pos_x = (int) (Math.floor(utm_cell_min_x / cellsize) - celloffset.x);
							int cell_pos_y = (int) (Math.floor(utm_cell_min_y / cellsize) - celloffset.y);
							int cell_pos_z = 0;
							Logger.info("cellpos "+cell_pos_x+"  "+cell_pos_y);
							CellTable cellTable = new CellTable(cell_pos_x, cell_pos_y, cell_pos_z, len, xs, ys, zs);
							cellTable.intensity = intensity;
							cellTable.returnNumber = returnNumber;
							cellTable.returns = returns;
							cellTable.scanAngleRank = scanAngleRank;
							cellTable.classification = classification;
							cellTable.cleanup();
							CellTable oldCellTable = pointcloud.getCellTable(cellTable.cx, cellTable.cy, cellTable.cz);
							if(oldCellTable != null) {
								Logger.warn("merge with existing cell");
								cellTable = CellTable.merge(oldCellTable, cellTable);
							}
							Timer.start("to_pointdb create tile");
							int t = 0;
							rasterunit.Tile tile = pointcloud.createTile(cellTable, cellTable.cx, cellTable.cy, cellTable.cz, t, Integer.MIN_VALUE);
							Logger.info(Timer.stop("to_pointdb create tile"));
							Timer.start("to_pointdb write");
							pointcloud.writeTile(tile);
							targetTileCount++;
							Logger.info(Timer.stop("to_pointdb write"));
							//pointcloud.commit();
							commiter.addOne();
							Logger.info(Timer.stop("to_pointdb tile"));
							if(isMessageTime()) {
								setMessage(sourceTileCount + " of " + stat.tile_sum + " source tiles processed -> " + targetTileCount + " target tiles");
							}
						}
						utmm_pos_x += utmm_cellsize;	
					}
					utmm_pos_y += utmm_cellsize;
				}
			}
			setMessage("finished transfer " + sourceTileCount + " of " + stat.tile_sum + " source tiles -> " + targetTileCount + " target tiles");

		} catch(RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
