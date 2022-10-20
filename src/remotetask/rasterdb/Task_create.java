package remotetask.rasterdb;

import org.json.JSONArray;
import org.json.JSONObject;
import org.tinylog.Logger;

import broker.Broker;
import broker.acl.ACL;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.RasterdbConfig;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import util.JsonUtil;

@task_rasterdb("create")
@Description("Create new rasterdb layer. Creator of the task will be owner of the layer which has read/modify access rigths of the created rasterdb layer including change of ACLs.")
@Param(name="rasterdb", type="layer_id", desc="ID of new RasterDB layer.", example="raster1")
@Param(name="pixel_size", type="number_array" , desc="Size of pixels in projection units.", format="size or x_size, y_size", example="10, 15.1", required=false)
@Param(name="offset", type="number_array", desc="Offset to projection origin.", format="x_offset, y_offset", example="0.5, -0.5", required=false)
@Param(name="code", desc="Projection code.", format="EPSG:code", example="EPSG:32632", required=false)
@Param(name="proj4", desc="Projection.", format="PROJ4", example="+proj=utm +zone=32 +ellps=WGS84 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs ", required=false)
@Param(name="storage_type", desc="Storage type of new RasterDB. (default: TileStorage)", format="RasterUnit or TileStorage", example="TileStorage", required=false)
@Param(name="tile_pixel_len", type="integer", desc="Tile width and height in pixels, defaults to 256. Note for band type 1=tile_int16 and 2=tile_float32 size 256 is supported only", example="256", required=false)
@Param(name="acl", type="string_array" , desc="'read' roles of the new rasterdb laye' (default: empty, admin role only)", format="group1, my_group2", example="my_read_group", required=false)
@Param(name="acl_mod", type="string_array" , desc="'modfiy' roles of the new rasterdb laye' (default: empty, admin role only)", format="my_group1, group2", example="my_write_group", required=false)
public class Task_create extends RemoteTask {

	private final Broker broker;
	private final JSONObject task;
	private final String rasterdbName;
	private final ACL acl;
	private final ACL acl_mod;

	public Task_create(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		this.rasterdbName = task.getString("rasterdb");
		//EmptyACL.ADMIN.check(ctx.userIdentity); // acl and acl_mod is checked, this is, if empty, admin only
		if(broker.hasRasterdb(rasterdbName)) {
			RasterDB rasterdb = broker.getRasterdb(rasterdbName);
			rasterdb.checkMod(ctx.userIdentity, "task rasterdb create of existing name");
			rasterdb.close();
		}		  
		this.acl = ACL.ofRoles(JsonUtil.optStringTrimmedList(task, "acl"));
		//acl.check(ctx.userIdentity, "acl parameter of task rasterdb create"); // acl and acl_mod check not needed: user will be owner of new rasterdb layer
		this.acl_mod = ACL.ofRoles(JsonUtil.optStringTrimmedList(task, "acl_mod"));
		//acl_mod.check(ctx.userIdentity, "acl_mod parameter of task rasterdb create"); // acl and acl_mod check not needed: user will be owner of new rasterdb layer
	}

	@Override
	public void process() {

		double pixel_size_x = GeoReference.NO_PIXEL_SIZE;
		double pixel_size_y = GeoReference.NO_PIXEL_SIZE;
		double offset_x = GeoReference.NO_OFFSET;
		double offset_y = GeoReference.NO_OFFSET;

		double pixel_size = task.optDouble("pixel_size");
		if(Double.isFinite(pixel_size)) {
			pixel_size_x = pixel_size;
			pixel_size_y = pixel_size;
		}
		JSONArray pixel_size_array = task.optJSONArray("pixel_size");
		if(pixel_size_array != null) {
			if(pixel_size_array.isEmpty()) {
				// nothing
			} else if(pixel_size_array.length() == 1) {
				double v = pixel_size_array.optDouble(0);
				pixel_size_x = v;
				pixel_size_y = v;
			} else if(pixel_size_array.length() == 2) {
				pixel_size_x = pixel_size_array.optDouble(0);
				pixel_size_y = pixel_size_array.optDouble(1);
			} else {
				throw new RuntimeException("invalid value for parameter 'pixel_size'");
			}
		}
		JSONObject pixel_size_obj = task.optJSONObject("pixel_size");
		if(pixel_size_obj != null) {
			double x = pixel_size_obj.optDouble("x");
			if(Double.isFinite(x)) {
				pixel_size_x = x;
			}
			double y = pixel_size_obj.optDouble("y");
			if(Double.isFinite(y)) {
				pixel_size_y = y;
			}
		}

		JSONArray offset_array = task.optJSONArray("offset");
		if(offset_array != null) {
			if(offset_array.length() == 2) {
				offset_x = offset_array.optDouble(0);
				offset_y = offset_array.optDouble(1);
			} else {
				throw new RuntimeException("invalid value for parameter 'offset'");
			}
		}
		JSONObject offset_obj = task.optJSONObject("offset");
		if(offset_obj != null) {
			double x = offset_obj.optDouble("x");
			if(Double.isFinite(x)) {
				offset_x = x;
			}
			double y = offset_obj.optDouble("y");
			if(Double.isFinite(y)) {
				offset_y = y;
			}
		}

		RasterdbConfig config = broker.createRasterdbConfig(rasterdbName);
		if(task.has("storage_type")) {
			String storage_type = task.getString("storage_type");
			config.preferredStorageType = storage_type;
		}
		if(task.has("tile_pixel_len")) {
			int tile_pixel_len = task.getInt("tile_pixel_len");
			Logger.info("tile_pixel_len " + tile_pixel_len);
			config.preferredTilePixelLen = tile_pixel_len;
		}		

		RasterDB rasterdb = broker.createNewRasterdb(config);

		Logger.info(ctx.userIdentity);
		if(ctx.userIdentity != null) {
			String username = ctx.userIdentity.getUserPrincipal().getName();
			rasterdb.setACL_owner(ACL.ofRole(username));
		}
		rasterdb.setACL(acl);
		rasterdb.setACL_mod(acl_mod);

		rasterdb.setPixelSize(pixel_size_x, pixel_size_y, offset_x, offset_y);

		String code = task.optString("code");
		if(code != null) {
			rasterdb.setCode(code);
		}

		String proj4 = task.optString("proj4");
		if(proj4 != null) {
			rasterdb.setProj4(proj4);
		}		
	}
}
