package remotetask.voxeldb;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import broker.Broker;
import broker.acl.EmptyACL;
import pointcloud.DoubleRect;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import voxeldb.Importer;
import voxeldb.VoxelDB;

@task_voxeldb("import")
@Description("Import all *.las and *.laz files at a folder (and subfolders) on the server into a new VoxelDB layer.")
@Param(name="voxeldb", type="layer_id", desc="ID of new VoxelDB layer (target).", example="voxeldb1")
@Param(name="source", format="path", desc="Folder located on server with *.las / *.laz files to import including subfolders.", example="las/folder1")
@Param(name="epsg", desc="EPSG projection code. If epsg is left empty and proj4 parameter is set a automatic EPSG search will be tried, multiple EPSG codes may refer to one proj4.", format="number", example="25832", required=false)
@Param(name="proj4", desc="PROJ4 projection. If proj4 is left empty and epsg parameter is set a automatic proj4 generation will be tried.", format="text", example="+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs ", required=false)
@Param(name="rect", type="number_rect", desc="Only points inside of rect are imported - prevents import of points with erroneous x,y coordinates.", format="list of coordinates: xmin, ymin, xmax, ymax", example="609000.1, 5530100.7, 609094.1, 5530200.9", required=false)
@Param(name="cell_size", type="number", desc="Size of cells. (default: 100 -> 100 voxels edge length)", example="10", required=false)
@Param(name="voxel_size", type="number", desc="Resolution of voxels. (default: 1 -> voxels of 1 meter edge length)", example="0.2", required=false)
public class Task_import extends RemoteTask {
	private static final Logger log = LogManager.getLogger();
	private static final CRSFactory CRS_FACTORY = new CRSFactory();

	private final Broker broker;
	private final JSONObject task;

	public Task_import(Context ctx) {
		this.broker = ctx.broker;
		this.task = ctx.task;
		EmptyACL.ADMIN.check(ctx.userIdentity);
	}

	@Override
	public void process() throws IOException {

		DoubleRect filterRect = null;
		JSONArray rect_Text = task.optJSONArray("rect");
		if(rect_Text != null) {
			double rect_xmin = rect_Text.getDouble(0);
			double rect_ymin = rect_Text.getDouble(1);
			double rect_xmax = rect_Text.getDouble(2);
			double rect_ymax = rect_Text.getDouble(3);
			filterRect = new DoubleRect(rect_xmin, rect_ymin, rect_xmax, rect_ymax);
		}
		
		String voxeldb_name = task.getString("voxeldb");
		String storage_type = task.optString("storage_type", "TileStorage");
		boolean transactions = task.optBoolean("transactions", false);
		int cell_size = task.optNumber("cell_size", 100).intValue();
		double voxel_size = task.optNumber("voxel_size", 1).doubleValue();
		broker.deleteVoxeldb(voxeldb_name);
		VoxelDB voxeldb = broker.createNewVoxeldb(voxeldb_name, storage_type, transactions);
		voxeldb.trySetVoxelsize(voxel_size);
		voxeldb.trySetCellsize(cell_size);
		
		int epsg = task.optInt("epsg", 0);
		String proj4 = task.optString("proj4", null);
		if(epsg > 0) { // set EPSG
			voxeldb.setEpsg(epsg);
			if(proj4 != null) { // set EPSG, set PROJ4
				voxeldb.setProj4(proj4);
			} else { // set EPSG, no PROJ4
				try {
					CoordinateReferenceSystem crs = CRS_FACTORY.createFromName("EPSG:" + epsg);
					String crsParams = crs.getParameterString();
					if(!crsParams.isEmpty()) {
						voxeldb.setProj4(crsParams);
					}
				} catch(Exception e) {
					log.warn(e);
				}
			}			
		} else if(proj4 != null) {  // no EPSG, set PROJ4
			voxeldb.setProj4(proj4);
			String epsgTry = CRS_FACTORY.readEpsgFromParameters(proj4);
			if(epsgTry != null) {
				voxeldb.setEpsg(Integer.parseInt(epsgTry));
			}
		} else { // no EPSG, no PROJ4
			// nothing
		}

		Importer imprter = new Importer(voxeldb, filterRect, true);
		String source = task.getString("source");
		Path root = Paths.get(source);

		setMessage("start import");
		imprter.importDirectory(root);
		voxeldb.getGriddb().storage().flush();
		setMessage("finished import");
	}
}
