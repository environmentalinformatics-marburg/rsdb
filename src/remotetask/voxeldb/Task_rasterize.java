package remotetask.voxeldb;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import broker.Broker;
import broker.TimeSlice;
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
@Param(name="rasterdb", type="layer_id", desc="ID of new RasterDB layer. (target) (if layer exists, delete)", example="rasterdb1", required=false)
public class Task_rasterize extends CancelableRemoteTask {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;

	public Task_rasterize(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
	}

	@Override
	public void process() throws IOException {
		String voxeldb_name = task.getString("voxeldb");
		VoxelDB voxeldb = broker.getVoxeldb(voxeldb_name);
		
		CellFactory cf = CellFactory.ofCount(voxeldb);

		String rasterdb_name = task.optString("rasterdb", voxeldb.getName() + "_rasterized");
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
		rasterdb.setProj4(voxeldb.geoRef().proj4);
		rasterdb.setCode("EPSG:" + voxeldb.geoRef().epsg);
		rasterdb.setPixelSize(voxeldb.geoRef().voxelSizeX, voxeldb.geoRef().voxelSizeY, voxeldb.geoRef().originX, voxeldb.geoRef().originY);

		voxeldb.setAssociatedRasterDB(rasterdb.config.getName());		
		rasterdb.associated.setVoxelDB(voxeldb.getName());
		rasterdb.writeMeta();

		Band bandCount = rasterdb.createBand(TilePixel.TYPE_FLOAT, "count", null);
		Band bandElevation = rasterdb.createBand(TilePixel.TYPE_FLOAT, "elevation", null);
		//Band bandMaxCount = rasterdb.createBand(TilePixel.TYPE_FLOAT, "maxCount", null);

		int cellsize = voxeldb.getCellsize();
		double originZ = voxeldb.geoRef().originZ;
		double voxelSizeZ = voxeldb.geoRef().voxelSizeZ;

		for(TimeSlice timeSlice:voxeldb.timeMapReadonly.values()) {
			if(isCanceled()) {
				throw new RuntimeException("canceled");
			}
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
						FloatFrame frame = bandProcessor.getFloatFrame(bandCount);
						float[][]  pixels = frame.data;

						for(int z = 0; z < cellsize; z++) {
							for(int y = 0; y < cellsize; y++) {
								for(int x = 0; x < cellsize; x++) {
									int v = cnt[z][y][x];
									if(v != 0) {
										pixels[y][x] = Float.isFinite(pixels[y][x]) ? pixels[y][x] + v : v;
									}
								}
							}
						}
						ProcessingFloat.writeMerge(storage, bandProcessor.timestamp, bandCount, pixels, range2d.ymin, range2d.xmin);
					}

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
										pixelsy[x] = zr;
									}
								}
							}
						}
						ProcessingFloat.writeMerge(storage, bandProcessor.timestamp, bandElevation, pixels, range2d.ymin, range2d.xmin);
					}

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
