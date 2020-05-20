package remotetask.pointcloud;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import pointcloud.DoubleRect;
import pointcloud.Importer;
import pointcloud.PointCloud;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;

@task_pointcloud("import")
@Description("Import all *.las and *.laz files at a folder (and subfolders) on the server into a new PointCloud layer.")
@Param(name="pointcloud", type="layer_id", desc="ID of new PointCloud layer (target).", example="pointcloud1")
@Param(name="source", format="path", desc="Folder located on server with *.las / *.laz files to import including subfolders.", example="las/folder1")
@Param(name="epsg", desc="EPSG projection code. If epsg is left empty and proj4 parameter is set a automatic EPSG search will be tried, multiple EPSG codes may refer to one proj4.", format="number", example="25832", required=false)
@Param(name="proj4", desc="PROJ4 projection. If proj4 is left empty and epsg parameter is set a automatic proj4 generation will be tried.", format="text", example="+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs ", required=false)
@Param(name="rect", type="number_rect", desc="Only points inside of rect are imported - prevents import of points with erroneous x,y coordinates.", format="list of coordinates: xmin, ymin, xmax, ymax", example="609000.1, 5530100.7, 609094.1, 5530200.9", required=false)
@Param(name="cellsize", type="number", desc="Size of cells. (default: 100 -> 100 meter)", example="10", required=false)
@Param(name="cellscale", type="number", desc="Resolution of points. (default: 100 -> 1/100 = 0.01 meter precision)", example="1000", required=false)
@Param(name="storage_type", desc="Storage type of new PointCloud. (default: TileStorage)", format="RasterUnit or TileStorage", example="TileStorage", required=false)
@Param(name="transactions", type="boolean", desc="Use power failer safe (and slow) PointCloud operation mode. (RasterUnit only, default false)", example="false", required=false)
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
		
		String name = task.getString("pointcloud");
		String storage_type = task.optString("storage_type", "TileStorage");
		boolean transactions = task.optBoolean("transactions", false);
		PointCloud pointcloud = broker.createNewPointCloud(name, storage_type, transactions);
		double cellsize = task.optDouble("cellsize", 100);
		pointcloud.trySetCellsize(cellsize);
		double cellscale = task.optDouble("cellscale", 100);
		pointcloud.trySetCellscale(cellscale);

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
					log.warn(e);
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


		Importer imprter = new Importer(pointcloud, filterRect);
		String source = task.getString("source");
		Path root = Paths.get(source);

		setMessage("start import");
		imprter.importDirectory(root);
		pointcloud.getGriddb().storage().flush();
		setMessage("finished import");
	}
}
