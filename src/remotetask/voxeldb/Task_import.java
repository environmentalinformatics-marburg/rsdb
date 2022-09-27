package remotetask.voxeldb;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.tinylog.Logger;

import broker.Broker;
import broker.TimeSlice;
import broker.acl.ACL;
import broker.acl.AclUtil;
import pointcloud.DoubleRect;
import remotetask.CancelableRemoteProxyTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import util.JsonUtil;
import voxeldb.Importer;
import voxeldb.VoxelDB;

@task_voxeldb("import")
@Description("Import all *.las and *.laz files at a folder (and subfolders) on the server into a new VoxelDB layer.")
@Param(name="voxeldb", type="layer_id", desc="ID of new VoxelDB layer (target).", example="voxeldb1")
@Param(name="source", format="path", desc="Folder located on server with *.las / *.laz files to import including subfolders.", example="las/folder1")
@Param(name="epsg", desc="EPSG projection code. If epsg is left empty and proj4 parameter is set a automatic EPSG search will be tried, multiple EPSG codes may refer to one proj4.", format="number", example="25832", required=false)
@Param(name="proj4", desc="PROJ4 projection. If proj4 is left empty and epsg parameter is set a automatic proj4 generation will be tried.", format="text", example="+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs ", required=false)
@Param(name="rect", type="number_rect", desc="Only points inside of rect are imported - prevents import of points with erroneous x,y coordinates.", format="list of coordinates: xmin, ymin, xmax, ymax", example="609000.1, 5530100.7, 609094.1, 5530200.9", required=false)
@Param(name="zmin", type="number", desc="Only points above are imported - prevents import of points with erroneous z coordinates.", example="0", required=false)
@Param(name="zmax", type="number", desc="Only points below are imported - prevents import of points with erroneous z coordinates.", example="20", required=false)
@Param(name="cell_size", type="number", desc="Size of cells. (default: 100 -> 100 voxels edge length)", example="10", required=false)
@Param(name="voxel_size", type="number", desc="Resolution of voxels. (default: 1 -> voxels of 1 meter edge length)", example="0.2", required=false)
@Param(name="time_slice", type="string", desc="Name of time slice. (default: untitled)", example="January", required=false)
@Param(name="clear", type="boolean", desc="Delete existing VoxelDB of that ID. (default: false)", example="true", required=false)
@Param(name="access_roles", type="string_array", desc="List of access control read roles", example="role1, role2, role3", required=false)
@Param(name="modify_roles", type="string_array", desc="List of access control write roles", example="role1, role2, role3", required=false)
public class Task_import extends CancelableRemoteProxyTask {
	
	private static final CRSFactory CRS_FACTORY = new CRSFactory();

	private final Broker broker;
	private final JSONObject task;

	public Task_import(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		AclUtil.check(ctx.userIdentity, "task voxeldb import");
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
		
		double filterZmin = task.optNumber("zmin", Double.NaN).doubleValue();
		double filterZmax = task.optNumber("zmax", Double.NaN).doubleValue();
		
		String voxeldb_name = task.getString("voxeldb");
		String storage_type = task.optString("storage_type", "TileStorage");
		boolean transactions = task.optBoolean("transactions", false);
		int cell_size = task.optNumber("cell_size", 100).intValue();
		double voxel_size = task.optNumber("voxel_size", 1).doubleValue();
		
		boolean clearVoxelDB = task.optBoolean("clear", false);
		
		String[] access_roles = JsonUtil.optStringTrimmedArray(task, "access_roles");
		String[] modify_roles = JsonUtil.optStringTrimmedArray(task, "modify_roles");
		
		VoxelDB voxeldb;
		if(clearVoxelDB) {
			broker.deleteVoxeldb(voxeldb_name);
			voxeldb = broker.createNewVoxeldb(voxeldb_name, storage_type, transactions);			
		} else {
			voxeldb = broker.getOrCreateVoxeldb(voxeldb_name, storage_type, transactions);
		}
		
		if(access_roles.length > 0) {
			voxeldb.setACL(ACL.ofRoles(access_roles));
		}
		
		if(modify_roles.length > 0) {
			voxeldb.setACL_mod(ACL.ofRoles(modify_roles));
		}		

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
					Logger.warn(e);
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
		
		String time_slice = task.optString("time_slice", "untitled");
		if(time_slice.isEmpty()) {
			time_slice = "";
		}
		TimeSlice timeSlice = voxeldb.addTimeSlice(new TimeSlice.TimeSliceBuilder(time_slice));

		String source = task.getString("source");
		Path root = Paths.get(source);
		
		try(Importer importer = new Importer(voxeldb, filterRect, filterZmin, filterZmax, true, timeSlice)) {
			this.setRemoteProxy(importer);
			setMessage("start import");
			importer.importDirectory(root);
			voxeldb.getGriddb().storage().flush();
			setMessage("finished import");
		}		
	}
}
