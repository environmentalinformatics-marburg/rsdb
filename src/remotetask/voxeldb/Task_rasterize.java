package remotetask.voxeldb;

import java.io.IOException;

import org.json.JSONObject;

import broker.Broker;
import broker.TimeSlice;
import broker.acl.ACL;
import rasterdb.Band;
import rasterdb.BandProcessor;
import rasterdb.RasterDB;
import rasterdb.tile.ProcessingFloat;
import rasterdb.tile.TilePixel;
import rasterunit.RasterUnitStorage;
import remotetask.CancelableRemoteTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import util.Range2d;
import util.frame.FloatFrame;
import voxeldb.CellFactory;
import voxeldb.VoxelDB;

@task_voxeldb("rasterize")
@Description("Convert voxels to raster.")
@Param(name="voxeldb", type="voxeldb", desc="VoxelDB layer. (source)", example="voxeldb1")
@Param(name="rasterdb", type="layer_id", desc="ID of new RasterDB layer. (if layer exists, delete) (target, default: [voxeldb]_rasterized)", example="rasterdb1", required=false)
public class Task_rasterize extends CancelableRemoteTask {
	
	private final Broker broker;
	private final JSONObject task;
	private final VoxelDB voxeldb;
	private final String rasterdb_name;

	public Task_rasterize(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		String voxeldb_name = task.getString("voxeldb");
		this.voxeldb = broker.getVoxeldb(voxeldb_name);
		voxeldb.check(ctx.userIdentity, "task voxeldb rasterize");
		voxeldb.checkMod(ctx.userIdentity, "task voxeldb rasterize"); // check needed as same ACLs are assigned to target rasterdb
		this.rasterdb_name = task.optString("rasterdb", voxeldb.getName() + "_rasterized");
		if(broker.hasRasterdb(rasterdb_name)) {
			RasterDB rasterdb = broker.getRasterdb(rasterdb_name);
			rasterdb.checkMod(ctx.userIdentity, "task voxeldb rasteriz of existing name");
			rasterdb.close();
		}
	}

	@Override
	public void process() throws IOException {
		voxeldb.getLocalRange(true); // refresh local range

		CellFactory cf = CellFactory.ofCount(voxeldb);

		boolean transactions = true;
		if(task.has("transactions")) {
			transactions = task.getBoolean("transactions");
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
			rasterdb.setACL_owner(ACL.of(username));
		}
		rasterdb.setProj4(voxeldb.geoRef().proj4);
		rasterdb.setCode("EPSG:" + voxeldb.geoRef().epsg);
		rasterdb.setPixelSize(voxeldb.geoRef().voxelSizeX, voxeldb.geoRef().voxelSizeY, voxeldb.geoRef().originX, voxeldb.geoRef().originY);

		voxeldb.setAssociatedRasterDB(rasterdb.config.getName());		
		rasterdb.associated.setVoxelDB(voxeldb.getName());

		rasterdb.setACL(voxeldb.getACL());
		rasterdb.setACL_mod(voxeldb.getACL_mod());

		rasterdb.writeMeta();

		Band bandSum = rasterdb.createBand(TilePixel.TYPE_FLOAT, "sum", null);
		Band bandCount = rasterdb.createBand(TilePixel.TYPE_FLOAT, "count", null);
		Band bandElevation = rasterdb.createBand(TilePixel.TYPE_FLOAT, "z_sum", null);
		//Band bandMaxCount = rasterdb.createBand(TilePixel.TYPE_FLOAT, "maxCount", null);
		Band bandDensity = rasterdb.createBand(TilePixel.TYPE_FLOAT, "density_mean", null);

		int cellsize = voxeldb.getCellsize();
		double originZ = voxeldb.geoRef().originZ;
		double voxelSizeZ = voxeldb.geoRef().voxelSizeZ;

		for(TimeSlice timeSlice:voxeldb.timeMapReadonly.values()) {
			if(isCanceled()) {
				throw new RuntimeException("canceled");
			}
			setMessage("process time slice: " + timeSlice.id +" : " + timeSlice.name);
			cf.getVoxelCells(timeSlice).sequential().forEach(voxelCell -> {
				if(isCanceled()) {
					throw new RuntimeException("canceled");
				}
				try {
					rasterdb.setTimeSlice(timeSlice);

					RasterUnitStorage storage = rasterdb.rasterUnit();
					int rx = voxelCell.x * cellsize;
					int ry = voxelCell.y * cellsize;
					Range2d range2d = Range2d.ofCorner(rx, ry, cellsize, cellsize);
					int timestamp = timeSlice.id;
					BandProcessor bandProcessor = new BandProcessor(rasterdb, range2d, timestamp);
					int[][][] cnt = voxelCell.cnt;

					{
						FloatFrame frame = bandProcessor.getFloatFrame(bandSum);
						float[][]  pixels = frame.data;

						for(int z = 0; z < cellsize; z++) {
							int[][] cntz = cnt[z];
							for(int y = 0; y < cellsize; y++) {
								int[] cntzy = cntz[y];
								float[] pixelsy = pixels[y];
								for(int x = 0; x < cellsize; x++) {
									int v = cntzy[x];
									if(v != 0) {
										pixelsy[x] = Float.isFinite(pixelsy[x]) ? pixelsy[x] + v : v; // point sum
									}
								}
							}
						}
						ProcessingFloat.writeMerge(storage, bandProcessor.timestamp, bandSum, pixels, range2d.ymin, range2d.xmin);
					}

					{
						FloatFrame frame = bandProcessor.getFloatFrame(bandCount);
						float[][]  pixels = frame.data;

						for(int z = 0; z < cellsize; z++) {
							int[][] cntz = cnt[z];
							for(int y = 0; y < cellsize; y++) {
								int[] cntzy = cntz[y];
								float[] pixelsy = pixels[y];
								for(int x = 0; x < cellsize; x++) {
									int v = cntzy[x];
									if(v != 0) {
										pixelsy[x] = Float.isFinite(pixelsy[x]) ? pixelsy[x] + 1 : 1; // voxel count
									}
								}
							}
						}
						ProcessingFloat.writeMerge(storage, bandProcessor.timestamp, bandCount, pixels, range2d.ymin, range2d.xmin);
					}

					/*{
						FloatFrame frame = bandProcessor.getFloatFrame(bandElevation);
						float[][]  pixels = frame.data;

						for(int z = 0; z < cellsize; z++) {
							int[][] cntz = cnt[z];
							float zr = (float) (originZ + voxelSizeZ * (voxelCell.z * cellsize + z));
							for(int y = 0; y < cellsize; y++) {
								int[] cntzy = cntz[y];
								float[] pixelsy = pixels[y];
								for(int x = 0; x < cellsize; x++) {
									int v = cntzy[x];
									if(v != 0) {
										pixelsy[x] = zr;
									}
								}
							}
						}
						ProcessingFloat.writeMerge(storage, bandProcessor.timestamp, bandElevation, pixels, range2d.ymin, range2d.xmin);
					}*/

					{
						FloatFrame frame = bandProcessor.getFloatFrame(bandElevation);
						float[][]  pixels = frame.data;

						for(int z = 0; z < cellsize; z++) {
							int[][] cntz = cnt[z];
							float zr = (float) (originZ + voxelSizeZ * (voxelCell.z * cellsize + z));
							for(int y = 0; y < cellsize; y++) {
								int[] cntzy = cntz[y];
								float[] pixelsy = pixels[y];
								for(int x = 0; x < cellsize; x++) {
									int v = cntzy[x];
									if(v != 0) {
										pixelsy[x] = Float.isFinite(pixelsy[x]) ? pixelsy[x] + zr : zr;
									}
								}
							}
						}
						ProcessingFloat.writeMerge(storage, bandProcessor.timestamp, bandElevation, pixels, range2d.ymin, range2d.xmin);
					}
					
					{
						FloatFrame frame = bandProcessor.getFloatFrame(bandDensity);
						float[][]  pixels = frame.data;
						int[][] counts = new int[pixels.length][pixels[0].length];

						for(int z = 0; z < cellsize; z++) {
							int[][] cntz = cnt[z];
							for(int y = 0; y < cellsize; y++) {
								int[] cntzy = cntz[y];
								float[] pixelsy = pixels[y];
								int[] countsy = counts[y];
								for(int x = 0; x < cellsize; x++) {									
									int v = cntzy[x];
									if(v != 0) {
										pixelsy[x] = Float.isFinite(pixelsy[x]) ? pixelsy[x] + v : v; // point sum
										countsy[x]++;
									}
								}
							}
						}
						for(int y = 0; y < cellsize; y++) {
							float[] pixelsy = pixels[y];
							int[] countsy = counts[y];
							for(int x = 0; x < cellsize; x++) {
								pixelsy[x] /= countsy[x];
							}
						}
						ProcessingFloat.writeMerge(storage, bandProcessor.timestamp, bandDensity, pixels, range2d.ymin, range2d.xmin);
					}
					
					/*{
						FloatFrame frame = bandProcessor.getFloatFrame(bandElevation);
						float[][]  pixels = frame.data;
						int[][] counts = new int[pixels.length][pixels[0].length];

						for(int z = 0; z < cellsize; z++) {
							int[][] cntz = cnt[z];
							float zr = (float) (originZ + voxelSizeZ * (voxelCell.z * cellsize + z));
							for(int y = 0; y < cellsize; y++) {
								int[] cntzy = cntz[y];
								float[] pixelsy = pixels[y];
								int[] countsy = counts[y];
								for(int x = 0; x < cellsize; x++) {
									int v = cntzy[x];
									if(v != 0) {
										pixelsy[x] = Float.isFinite(pixelsy[x]) ? pixelsy[x] + zr : zr;
										countsy[x]++;
									}
								}
							}
						}
						for(int y = 0; y < cellsize; y++) {
							float[] pixelsy = pixels[y];
							int[] countsy = counts[y];
							for(int x = 0; x < cellsize; x++) {
								pixelsy[x] /= countsy[x];
							}
						}
						ProcessingFloat.writeMerge(storage, bandProcessor.timestamp, bandElevation, pixels, range2d.ymin, range2d.xmin);
					}*/

					/*{
					FloatFrame frame = bandProcessor.getFloatFrame(bandMaxCount);
					float[][]  pixels = frame.data;

					for(int z = 0; z < cellsize; z++) {
						int[][] cntz = cnt[z];
						float zr = (float) (originZ + voxelSizeZ * (voxelCell.z * cellsize + z));
						for(int y = 0; y < cellsize; y++) {
							int[] cntzy = cntz[y];
							float[] pixelsy = pixels[y];
							for(int x = 0; x < cellsize; x++) {
								int v = cntzy[x];
								if(v != 0) {
									float prev = pixelsy[x];
									if(Float.isFinite(prev)) {
										if(prev < v) {
											pixelsy[x] = v;
										}
									} else {
										pixelsy[x] = v;
									}
								}
							}
						}
					}
					ProcessingFloat.writeMerge(storage, bandProcessor.timestamp, bandMaxCount, pixels, range2d.ymin, range2d.xmin);
				}*/


					storage.commit();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
		if(isCanceled()) {
			rasterdb.close();
			voxeldb.close();
			throw new RuntimeException("canceled");
		}
		rasterdb.rebuildPyramid(true);
		rasterdb.close();
		voxeldb.close();
	}


}
