package remotetask.pointcloud;

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
import broker.TimeSlice.TimeSliceBuilder;
import broker.acl.ACL;
import pointcloud.AttributeSelector;
import pointcloud.Importer;
import pointcloud.PointCloud;
import pointcloud.Rect2d;
import remotetask.CancelableRemoteProxyTask;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import util.JsonUtil;

@task_pointcloud("import")
@Description("Import all *.las and *.laz files at a folder (and subfolders) on the server into a new PointCloud layer.")
@Param(name="pointcloud", type="layer_id", desc="ID of new PointCloud layer (target).", example="pointcloud1")
@Param(name="source", format="path", desc="Folder located on server with *.las / *.laz files to import including subfolders. OR one .las / .laz file located on the server.", example="las/folder1")
@Param(name="epsg", desc="EPSG projection code. If epsg is left empty and proj4 parameter is set a automatic EPSG search will be tried, multiple EPSG codes may refer to one proj4.", format="number", example="25832", required=false)
@Param(name="proj4", desc="PROJ4 projection. If proj4 is left empty and epsg parameter is set a automatic proj4 generation will be tried.", format="text", example="+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs ", required=false)
@Param(name="time_slice", type="string", desc="Name of time slice. (default: untitled)", example="January", required=false)
@Param(name="rect", type="number_rect", desc="Only points inside of rect are imported - prevents import of points with erroneous x,y coordinates.", format="list of coordinates: xmin, ymin, xmax, ymax", example="609000.1, 5530100.7, 609094.1, 5530200.9", required=false)
@Param(name="omit_attributes", type="string_array", desc="List of attributes that should NOT be imported. Possible case sensitive values: x, y, z, intensity, returnNumber, returns, scanDirectionFlag, edgeOfFlightLine, classification, scanAngleRank, gpsTime, red, green, blue (default no attribute --> all attributes are imported)", example="red, green, blue", required=false)
@Param(name="cellsize", type="number", desc="Size of cells. (default: 100 -> 100 meter)", example="10", required=false)
@Param(name="cellscale", type="number", desc="Resolution of points. (default: 100 -> 1/100 = 0.01 meter precision)", example="1000", required=false)
@Param(name="compression_level", type="integer", desc="Compression level, higher values are slower with better compression ratio, decompression speed is not impacted. Value of range -99 to 22 (default: 1)", example="22", required=false)
//@Param(name="storage_type", desc="Storage type of new PointCloud. (default: TileStorage)", format="RasterUnit or TileStorage", example="TileStorage", required=false)
//@Param(name="transactions", type="boolean", desc="Use power failer safe (and slow) PointCloud operation mode. (RasterUnit only, default false)", example="false", required=false)
@Param(name="acl", type="string_array" , desc="'read' roles of the new rasterdb laye' (default: empty, admin role only)", format="group1, my_group2", example="my_read_group", required=false)
@Param(name="acl_mod", type="string_array" , desc="'modfiy' roles of the new rasterdb laye' (default: empty, admin role only)", format="my_group1, group2", example="my_write_group", required=false)
public class Task_import extends CancelableRemoteProxyTask {

	private static final CRSFactory CRS_FACTORY = new CRSFactory();

	private final Broker broker;
	private final JSONObject task;

	public Task_import(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		// any user can import into new pointclouds
		//AclUtil.check(ctx.userIdentity, "task pointcloud import");
	}

	@Override
	public void process() throws IOException {		
		String name = task.getString("pointcloud");
		
		if(broker.hasPointCloud(name)) {
			throw new RuntimeException("Pointcloud already exists. Currently, in not already existing pointclods can be imported only.");
		}

		int compression_level = task.optInt("compression_level", Integer.MIN_VALUE);

		AttributeSelector selector = new AttributeSelector().all();
		JSONArray omit_attributes = task.optJSONArray("omit_attributes");
		if(omit_attributes != null) {
			int omit_attributes_len = omit_attributes.length();
			for (int i = 0; i < omit_attributes_len; i++) {
				String omit_attribute = omit_attributes.getString(i);			
				selector.set(omit_attribute, false);
			}
		}

		Rect2d filterRect = null;
		JSONArray rect_Text = task.optJSONArray("rect");
		if(rect_Text != null) {
			double rect_xmin = rect_Text.getDouble(0);
			double rect_ymin = rect_Text.getDouble(1);
			double rect_xmax = rect_Text.getDouble(2);
			double rect_ymax = rect_Text.getDouble(3);
			filterRect = new Rect2d(rect_xmin, rect_ymin, rect_xmax, rect_ymax);
		}

		String storage_type = task.optString("storage_type", "TileStorage");
		boolean transactions = task.optBoolean("transactions", false);
		PointCloud pointcloud = broker.createNewPointCloud(name, storage_type, transactions);
		double cellsize = task.optDouble("cellsize", 100);
		pointcloud.trySetCellsize(cellsize);
		double cellscale = task.optDouble("cellscale", 100);
		pointcloud.trySetCellscale(cellscale);		

		String timeSliceName = task.optString("time_slice", null);
		TimeSlice timeSlice;
		if(timeSliceName == null || timeSliceName.isBlank()) {
			timeSlice = pointcloud.timeMapReadonly.get(0);
			if(timeSlice == null) {
				timeSlice = new TimeSlice(0, "untitled");
				pointcloud.setTimeSlice(timeSlice);
			}
		} else {
			timeSlice = pointcloud.addTimeSlice(new TimeSliceBuilder(timeSliceName));
		}		

		int epsg = task.optInt("epsg", 0);
		String proj4 = task.optString("proj4", null);
		if(epsg > 0) { // set EPSG
			pointcloud.setCodeEPSG(epsg);
			if(proj4 != null) { // set EPSG, set PROJ4
				pointcloud.setProj4(proj4);
			} else { // set EPSG, no PROJ4
				try {
					CoordinateReferenceSystem crs = CRS_FACTORY.createFromName("EPSG:" + epsg);
					String crsParams = crs.getParameterString();
					if(!crsParams.isEmpty()) {
						pointcloud.setProj4(crsParams);
					}
				} catch(Exception e) {
					Logger.warn(e);
				}
			}			
		} else if(proj4 != null) {  // no EPSG, set PROJ4
			pointcloud.setProj4(proj4);
			String epsgTry = CRS_FACTORY.readEpsgFromParameters(proj4);
			if(epsgTry != null) {
				pointcloud.setCode("EPSG:" + epsgTry);
			}
		} else { // no EPSG, no PROJ4
			// nothing
		}

		if(proj4 != null) {
			pointcloud.setProj4(proj4);
		}

		Logger.info(ctx.userIdentity);
		if(ctx.userIdentity != null) {
			String username = ctx.userIdentity.getUserPrincipal().getName();
			pointcloud.setACL_owner(ACL.ofRole(username));
		}
		ACL acl = ACL.ofRoles(JsonUtil.optStringTrimmedList(task, "acl"));
		ACL acl_mod = ACL.ofRoles(JsonUtil.optStringTrimmedList(task, "acl_mod"));
		pointcloud.setACL(acl);
		pointcloud.setACL_mod(acl_mod);

		Importer importer = new Importer(pointcloud, filterRect, selector, compression_level);
		setRemoteProxy(importer);

		String source = task.getString("source");
		Path root = Paths.get(source);
		setMessage("start import");
		importer.importDirectory(root, timeSlice );
		setMessage("finished. imported files " + importer.file_counter + ",   erroneous files " + importer.file_error_counter);
		pointcloud.getGriddb().storage().flush();
		setMessage("finished import");
		boolean update_catalog = true;
		if(update_catalog) {
			setMessage("update catalog");
			broker.catalog.updateCatalog();
		}
		setMessage("finished");
	}
}
