package remotetask.rasterdb;

import java.io.CharArrayWriter;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONWriter;

import broker.Broker;
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
@Param(name="bands", type="integer_array", desc="Array of integer band numbers of target layer. 0 is placeholder to not import that band. ( e.g. [2,0,7] leads to  file-band1 -> layer-band2, file-band3 -> layer-band7). If parameter is missing alle bands are imported starting with layer-band1", example="1, 2, 3", required=false)
@Param(name="update_pyramid", type="boolean", desc="Build pyramid of scaled down raster including imported data. Needed for visualisations. (default true. Use 'false' if you import multiple rasters for import speed up. After import, run task rebuild_pyramid once.)", example="false", required=false)
@Param(name="update_catalog", type="boolean", desc="Update extent with new imported data. (default true. Use 'false' if you import multiple rasters for import speed up. After import, run task rebuild_pyramid once.)", example="false", required=false)
public class Task_import extends RemoteProxyTask {
	private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;
	
	public Task_import(Context ctx) {
		this.broker = ctx.broker;
		this.task = ctx.task;
		EmptyACL.ADMIN.check(ctx.userIdentity);
	}

	@Override
	public void process() throws IOException {
		String rasterdbID = task.getString("rasterdb");
		
		boolean update_pyramid = task.optBoolean("update_pyramid", true);
		boolean update_catalog = task.optBoolean("update_catalog", true);
		
		RasterDB rasterdb = null;
		Strategy strategy = Strategy.EXISTING_MERGE;
		if(broker.hasRasterdb(rasterdbID)) {
			rasterdb =  broker.getRasterdb(rasterdbID);	
		} else {
			strategy = Strategy.CREATE;			
		}

		String filename = task.getString("file");		
		GdalReader gdalreader = new GdalReader(filename);
		
		CharArrayWriter writer = new CharArrayWriter();
		JSONWriter json = new JSONWriter(writer);
		String fileID = null;
		boolean guessTimestamp = false;
		
		int[] layerBandIndices = null;
		if(task.has("bands")) {
			layerBandIndices = JsonUtil.getIntArray(task, "bands");
		}
		ImportSpec spec = APIHandler_inspect.createSpec(gdalreader, strategy, fileID, rasterdbID, rasterdb, guessTimestamp, layerBandIndices);		
		spec.update_pyramid = update_pyramid;
		spec.update_catalog = update_catalog;
		log.info(spec);
		
		
		ImportProcessor importProcessor = ImportBySpec.importPerpare(broker, gdalreader, rasterdbID, spec);
		setRemoteProxy(importProcessor);
		importProcessor.process();
	}
}
