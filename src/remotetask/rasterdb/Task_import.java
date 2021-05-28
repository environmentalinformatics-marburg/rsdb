package remotetask.rasterdb;

import java.io.CharArrayWriter;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONWriter;

import broker.acl.EmptyACL;
import rasterdb.RasterDB;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteProxyTask;
import server.api.main.APIHandler_inspect;
import server.api.main.APIHandler_inspect.Strategy;
import util.JsonUtil;
import util.raster.GdalReader;

@task_rasterdb("import")
@Description("Import raster file data into rasterdb layer.")
@Param(name="rasterdb", type="layer_id", desc="ID of RasterDB layer. (new or existing)", example="rasterdb1")
@Param(name="file", format="path", desc="Raster file to import. (located on server)", example="data/raster.tiff")
@Param(name="bands", type="integer_array", desc="Array of integer band numbers at target layer. 0 is placeholder to not import that band. ( e.g. [2,0,7] leads to  file-band1 -> layer-band2, file-band3 -> layer-band7). If parameter is missing all bands are imported starting with layer-band1", example="1, 2, 3", required=false)
@Param(name="timeslices", type="string_array", desc="Array of timeslice names at target layer. ( e.g. [2020 spring, 2020 autumn] ). Array needs to be same length as bands parameter array. If parameter is missing all bands are imported into default timeslice.", example="2018, 2019, 2020", required=false)
@Param(name="update_pyramid", type="boolean", desc="Build pyramid of scaled down raster including imported data. Needed for visualisations. (default true. Use 'false' if you import multiple rasters for import speed up. After import, run task rebuild_pyramid once.)", example="false", required=false)
@Param(name="update_catalog", type="boolean", desc="Update extent with new imported data. (default true. Use 'false' if you import multiple rasters for import speed up. After import, run task rebuild_pyramid once.)", example="false", required=false)
public class Task_import extends RemoteProxyTask {
	private static final Logger log = LogManager.getLogger();

	public Task_import(Context ctx) {
		super(ctx);
		EmptyACL.ADMIN.check(ctx.userIdentity);
	}

	@Override
	public void process() throws IOException {
		String rasterdbID = ctx.task.getString("rasterdb");
		
		boolean update_pyramid = ctx.task.optBoolean("update_pyramid", true);
		boolean update_catalog = ctx.task.optBoolean("update_catalog", true);
		
		RasterDB rasterdb = null;
		Strategy strategy = Strategy.EXISTING_MERGE;
		if(ctx.broker.hasRasterdb(rasterdbID)) {
			rasterdb =  ctx.broker.getRasterdb(rasterdbID);	
		} else {
			strategy = Strategy.CREATE;			
		}

		String filename = ctx.task.getString("file");		
		GdalReader gdalreader = new GdalReader(filename);
		
		CharArrayWriter writer = new CharArrayWriter();
		JSONWriter json = new JSONWriter(writer);
		String fileID = null;
		boolean guessTimestamp = false;
		
		int[] layerBandIndices = null;
		if(ctx.task.has("bands")) {
			layerBandIndices = JsonUtil.getIntArray(ctx.task, "bands");
		}
		
		String[] timesliceNames = null;
		if(ctx.task.has("timeslices")) {
			timesliceNames = JsonUtil.getStringArray(ctx.task, "timeslices");
		}
		
		ImportSpec spec = APIHandler_inspect.createSpec(gdalreader, strategy, fileID, rasterdbID, rasterdb, guessTimestamp, layerBandIndices, timesliceNames);		
		spec.update_pyramid = update_pyramid;
		spec.update_catalog = update_catalog;
		log.info(spec);
		
		
		ImportProcessor importProcessor = ImportBySpec.importPerpare(ctx.broker, gdalreader, rasterdbID, spec);
		setRemoteProxy(importProcessor);
		importProcessor.process();
	}
}
