package server.api.voxeldbs;

import java.io.IOException;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Response;

import broker.TimeSlice;
import util.Range3d;
import voxeldb.CellFactory;
import voxeldb.VoxelCell;
import voxeldb.VoxelDB;
import voxeldb.VoxelGeoRef;
import voxeldb.aggregatedprocessor.AggProcBool8ofInt32;
import voxeldb.aggregatedprocessor.AggProcFloat32ofInt32;
import voxeldb.aggregatedprocessor.AggProcInt32ofInt32;
import voxeldb.aggregatedprocessor.AggProcUint8ofInt32;
import voxeldb.aggregatedprocessor.base.AggProc;
import voxeldb.aggregatedprocessor.base.AggProcBool8;
import voxeldb.aggregatedprocessor.base.AggProcFloat32;
import voxeldb.aggregatedprocessor.base.AggProcFloat32Delegate2Div;
import voxeldb.aggregator.AggBool8ofInt32Exist;
import voxeldb.aggregator.AggFloat32ofInt32Sum;
import voxeldb.aggregator.AggInt32ofInt32Count;
import voxeldb.aggregator.AggInt32ofInt32Exist;
import voxeldb.aggregator.AggInt32ofInt32Sum;
import voxeldb.aggregator.AggUint8ofInt32Sum;
import voxeldb.aggregator.base.AggBool8ofInt32;
import voxeldb.aggregator.base.AggFloat32ofInt32;
import voxeldb.aggregator.base.AggInt32ofInt32;
import voxeldb.aggregator.base.AggUint8ofInt32;
import voxeldb.voxelmapper.VoxelMapperInt32;

public abstract class AggregatedProcessing implements Consumer<VoxelCell>{
	private static final Logger log = LogManager.getLogger();

	public static void process(VoxelDB voxeldb, Range3d range, TimeSlice timeSlice, int aggregation_factor_x, int aggregation_factor_y, int aggregation_factor_z, String product, boolean crop, Response response, String format) throws IOException {
		VoxelGeoRef ref = voxeldb.geoRef();
		double aggOriginX = ref.voxelXtoGeo(range.xmin);
		double aggOriginY = ref.voxelYtoGeo(range.ymin);
		double aggOriginZ = ref.voxelZtoGeo(range.zmin);
		double aggVoxelSizeX = ref.voxelSizeX * aggregation_factor_x;
		double aggVoxelSizeY = ref.voxelSizeY * aggregation_factor_y;
		double aggVoxelSizeZ = ref.voxelSizeZ * aggregation_factor_z;
		VoxelGeoRef aggRef = ref.with(aggOriginX, aggOriginY, aggOriginZ, aggVoxelSizeX, aggVoxelSizeY, aggVoxelSizeZ);
		log.info(aggRef);

		CellFactory cellFactory = new CellFactory(voxeldb);
		AggProc aggProc = null;
		switch(product) {
		case "exist": {
			VoxelMapperInt32 mapper = cellFactory.registerMapper("count");
			AggBool8ofInt32 agg = new AggBool8ofInt32Exist();
			aggProc = new AggProcBool8ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, mapper, agg);
			break;
		}
		case "count": {
			VoxelMapperInt32 mapper = cellFactory.registerMapper("count");
			AggInt32ofInt32 agg = new AggInt32ofInt32Count();
			aggProc = new AggProcInt32ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, mapper, agg);
			break;
		}
		case "sum": {
			VoxelMapperInt32 mapper = cellFactory.registerMapper("count");
			AggInt32ofInt32 agg = new AggInt32ofInt32Sum();
			aggProc = new AggProcInt32ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, mapper, agg);
			break;
		}
		case "logSum": {
			VoxelMapperInt32 mapper = cellFactory.registerMapper("count");
			AggFloat32ofInt32 agg = new AggFloat32ofInt32Sum();
			aggProc = new AggProcFloat32ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, mapper, agg) {
				@Override
				public void finish() {
					for (int z = 0; z < zAggLen; z++) {
						float[][] dstZ = dst[z];
						for (int y = 0; y < yAggLen; y++) {
							float[] dstZY = dstZ[y];
							for (int x = 0; x < xAggLen; x++) {
								dstZY[x] = (float) Math.log(dstZY[x]);
							}
						}
					}
				}
			};
			break;
		}
		default: {
			throw new RuntimeException("unknown product");
		}
		}
		cellFactory.getVoxelCells(timeSlice, range).forEach(aggProc);
		aggProc.finish();
		aggProc.write(response, format, crop);		
	}	

	public static void process(VoxelDB voxeldb, Range3d range, TimeSlice timeSlice, String product, boolean crop, Response response, String format) throws IOException {
		int aggregation_factor_x = 1;
		int aggregation_factor_y = 1;
		int aggregation_factor_z = 1;
		VoxelGeoRef ref = voxeldb.geoRef();
		double aggOriginX = ref.voxelXtoGeo(range.xmin);
		double aggOriginY = ref.voxelYtoGeo(range.ymin);
		double aggOriginZ = ref.voxelZtoGeo(range.zmin);
		double aggVoxelSizeX = ref.voxelSizeX * aggregation_factor_x;
		double aggVoxelSizeY = ref.voxelSizeY * aggregation_factor_y;
		double aggVoxelSizeZ = ref.voxelSizeZ * aggregation_factor_z;
		VoxelGeoRef aggRef = ref.with(aggOriginX, aggOriginY, aggOriginZ, aggVoxelSizeX, aggVoxelSizeY, aggVoxelSizeZ);
		log.info(aggRef);

		CellFactory cellFactory = new CellFactory(voxeldb);
		AggProc aggProc = null;
		switch(product) {
		case "count":
		case "count:int32": {
			VoxelMapperInt32 mapper = cellFactory.registerMapper("count");
			AggInt32ofInt32 agg = new AggInt32ofInt32Sum();
			aggProc = new AggProcInt32ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, mapper, agg);
			break;
		}
		case "count:uint8": {
			VoxelMapperInt32 mapper = cellFactory.registerMapper("count");
			AggUint8ofInt32 agg = new AggUint8ofInt32Sum();
			aggProc = new AggProcUint8ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, mapper, agg);
			break;
		}
		case "count_log":
		case "count_log:int32": {
			VoxelMapperInt32 mapper = cellFactory.registerMapper("count");
			AggFloat32ofInt32Sum agg = new AggFloat32ofInt32Sum();
			aggProc = new AggProcFloat32ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, mapper, agg) {
				@Override
				public void finish() {
					for (int z = 0; z < zAggLen; z++) {
						float[][] dstZ = dst[z];
						for (int y = 0; y < yAggLen; y++) {
							float[] dstZY = dstZ[y];
							for (int x = 0; x < xAggLen; x++) {
								dstZY[x] = (float) Math.log(dstZY[x]);
							}
						}
					}
				}
			};
			break;
		}
		case "red":
		case "red:int32":{
			VoxelMapperInt32 mapper = cellFactory.registerMapper("red");
			AggInt32ofInt32 agg = new AggInt32ofInt32Sum();
			aggProc = new AggProcInt32ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, mapper, agg);
			break;
		}
		case "green":
		case "green:int32": {
			VoxelMapperInt32 mapper = cellFactory.registerMapper("green");
			AggInt32ofInt32 agg = new AggInt32ofInt32Sum();
			aggProc = new AggProcInt32ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, mapper, agg);
			break;
		}
		case "blue": 
		case "blue:int32": {
			VoxelMapperInt32 mapper = cellFactory.registerMapper("blue");
			AggInt32ofInt32 agg = new AggInt32ofInt32Sum();
			aggProc = new AggProcInt32ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, mapper, agg);
			break;
		}
		case "red_mean": 
		case "red_mean:float32": {
			VoxelMapperInt32 mapperA = cellFactory.registerMapper("red");
			VoxelMapperInt32 mapperB = cellFactory.registerMapper("count");
			AggFloat32ofInt32 aggA = new AggFloat32ofInt32Sum();
			AggFloat32ofInt32 aggB = new AggFloat32ofInt32Sum();
			AggProcFloat32 aggProcA = new AggProcFloat32ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, mapperA, aggA);
			AggProcFloat32 aggProcB = new AggProcFloat32ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, mapperB, aggB);
			aggProc = new AggProcFloat32Delegate2Div(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, aggProcA, aggProcB);
			break;
		}
		case "green_mean": 
		case "green_mean:float32": {
			VoxelMapperInt32 mapperA = cellFactory.registerMapper("green");
			VoxelMapperInt32 mapperB = cellFactory.registerMapper("count");
			AggFloat32ofInt32 aggA = new AggFloat32ofInt32Sum();
			AggFloat32ofInt32 aggB = new AggFloat32ofInt32Sum();
			AggProcFloat32 aggProcA = new AggProcFloat32ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, mapperA, aggA);
			AggProcFloat32 aggProcB = new AggProcFloat32ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, mapperB, aggB);
			aggProc = new AggProcFloat32Delegate2Div(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, aggProcA, aggProcB);
			break;
		}
		case "blue_mean": 
		case "blue_mean:float32": {
			VoxelMapperInt32 mapperA = cellFactory.registerMapper("blue");
			VoxelMapperInt32 mapperB = cellFactory.registerMapper("count");
			AggFloat32ofInt32 aggA = new AggFloat32ofInt32Sum();
			AggFloat32ofInt32 aggB = new AggFloat32ofInt32Sum();
			AggProcFloat32 aggProcA = new AggProcFloat32ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, mapperA, aggA);
			AggProcFloat32 aggProcB = new AggProcFloat32ofInt32(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, mapperB, aggB);
			aggProc = new AggProcFloat32Delegate2Div(cellFactory, range, aggregation_factor_x, aggregation_factor_y, aggregation_factor_z, aggRef, aggProcA, aggProcB);
			break;
		}
		default: {
			throw new RuntimeException("unknown product");
		}
		}
		cellFactory.getVoxelCells(timeSlice, range).forEach(aggProc);
		aggProc.finish();
		aggProc.write(response, format, crop);		
	}	
}
