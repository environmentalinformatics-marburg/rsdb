package server.api.rasterdb;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.proj.Projection;
import org.locationtech.proj4j.units.Unit;

import broker.Broker;
import rasterdb.Band;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterunit.TileKey;
import server.api.rasterdb.WmsCapabilities.WmsStyle;
import util.Range2d;
import util.TimeUtil;

public class RasterdbMethod_meta_json extends RasterdbMethod {
	private static final Logger log = LogManager.getLogger();

	public RasterdbMethod_meta_json(Broker broker) {
		super(broker, "meta.json");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		//log.info(request);
		request.setHandled(true);
		try {
			boolean requestTileCount = request.getParameter("tile_count") != null;
			boolean requestTileSizeStats = request.getParameter("tile_size_stats") != null;
			boolean requestStorageSize = request.getParameter("storage_size") != null;
			boolean requestInternalStorageInternalFreeSize = request.getParameter("storage_internal_free_size") != null;			

			GeoReference ref = rasterdb.ref();

			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());

			json.object();
			json.key("type");
			json.value("RasterDB");
			json.key("name");
			json.value(rasterdb.config.getName());
			rasterdb.informal().writeJson(json);
			json.key("ref");
			json.object();
			if(ref.has_pixel_size()) {
				json.key("pixel_size");
				json.object();
				json.key("x");
				json.value(ref.pixel_size_x);
				json.key("y");
				json.value(ref.pixel_size_y);
				json.endObject();
				Range2d localRange = rasterdb.getLocalRange(false);
				if(localRange != null) {
					json.key("extent");				
					json.array();
					json.value(ref.pixelXToGeo(localRange.xmin));
					json.value(ref.pixelYToGeo(localRange.ymin));
					json.value(ref.pixelXToGeo(localRange.xmax));
					json.value(ref.pixelYToGeo(localRange.ymax));
					json.endArray();
					json.key("internal_rasterdb_extent");
					json.array();
					json.value(localRange.xmin);
					json.value(localRange.ymin);
					json.value(localRange.xmax);
					json.value(localRange.ymax);
					json.endArray();
				}
				if(Double.isFinite(ref.offset_x) && Double.isFinite(ref.offset_y)) {
					json.key("internal_rasterdb_offset");
					json.object();
					json.key("x");
					json.value(ref.offset_x);
					json.key("y");
					json.value(ref.offset_y);
					json.endObject();
				}
			}
			Unit unit = null;
			if(ref.has_proj4()) {
				json.key("proj4");
				json.value(ref.proj4);
				try {
					CoordinateReferenceSystem crs = new CRSFactory().createFromParameters(null, ref.proj4);
					if(crs != null) {
						Projection projection = crs.getProjection();
						if(projection != null) {
							unit = projection.getUnits();
						}
					}
				} catch(Exception e) {
					log.warn(e);
				}
			}
			if(ref.has_code()) {
				json.key("code");
				json.value(ref.code);
				if(unit == null) {
					try {
						CoordinateReferenceSystem crs = new CRSFactory().createFromName(ref.code);
						if(crs != null) {
							Projection projection = crs.getProjection();
							if(projection != null) {
								unit = projection.getUnits();
							}
						}
					} catch(Exception e) {
						log.warn(e);
					}
				}
			}
			if(unit != null) {
				json.key("unit");
				json.object();
				json.key("name");
				json.value(unit.name);
				json.endObject();
			}
			String projectionTitle = ref.getProjectionTitle();
			if(projectionTitle != null) {
				json.key("projectionTitle");
				json.value(projectionTitle);
			}
			json.endObject();
			json.key("timestamps");
			json.array();
			for(int timestamp:rasterdb.rasterUnit().timeKeysReadonly()) {
				json.object();
				json.key("timestamp");
				json.value(timestamp);
				json.key("datetime");
				json.value(TimeUtil.toText(timestamp));
				json.endObject();
			}
			json.endArray();
			json.key("bands");
			json.array();
			for(Band band:rasterdb.bandMap.values()) {
				json.object();
				json.key("index");
				json.value(band.index);
				if(band.has_wavelength()) {
					json.key("wavelength");
					json.value(band.wavelength);
				}
				if(band.has_fwhm()) {
					json.key("fwhm");
					json.value(band.fwhm);
				}
				json.key("title");
				json.value(band.has_title() ? band.title : "band"+band.index);
				json.key("datatype");
				json.value(band.getPixelTypeName());
				if(band.has_visualisation()) {
					json.key("visualisation");
					json.value(band.visualisation);
				}
				if(band.has_vis_min()) {
					json.key("vis_min");
					json.value(band.vis_min);
				}
				if(band.has_vis_max()) {
					json.key("vis_max");
					json.value(band.vis_max);
				}
				json.endObject();
			}
			json.endArray();
			json.key("wms");			
			json.object();
			json.key("styles");
			json.array();
			for(WmsStyle style:WmsCapabilities.getWmsStyles(rasterdb)) {
				json.object();
				json.key("name");
				json.value(style.name);
				json.key("title");
				json.value(style.title);
				json.key("description");
				json.value(style.description);
				json.endObject();
			}
			json.endArray(); // end array style
			json.endObject(); // end object wms
			json.key("associated");
			rasterdb.associated.writeJSON(json);			
			json.key("modify");
			json.value(rasterdb.isAllowedMod(userIdentity));
			//if(EmptyACL.ADMIN.isAllowed(userIdentity)) {
			json.key("acl");
			rasterdb.getACL().writeJSON(json);
			json.key("acl_mod");
			rasterdb.getACL_mod().writeJSON(json);
			//}			
			if(request.getParameter("tilekeys") != null) {
				json.key("tilekeys");
				json.array();
				for(TileKey key:rasterdb.rasterUnit().tileKeysReadonly()) {
					json.value(key.t + ", " + key.b + ", " + key.y + ", " + key.x);
				}
				json.endArray();
			}
			if(requestTileCount) {
				json.key("tile_count");
				int tile_count = rasterdb.hasRasterUnit() ? rasterdb.rasterUnit().calculateTileCount() : 0;
				json.value(tile_count);
			}
			if(requestTileSizeStats && rasterdb.hasRasterUnit()) {
				long[] stats = rasterdb.rasterUnit().calculateTileSizeStats();
				if(stats != null) {
					json.key("tile_size_stats");
					json.object();
					json.key("min");
					json.value(stats[0]);		
					json.key("mean");
					json.value(stats[1]);		
					json.key("max");
					json.value(stats[2]);		
					json.endObject();		
				}
			}
			if(requestStorageSize && rasterdb.hasRasterUnit()) {
				try {
					long storage_size = rasterdb.rasterUnit().calculateStorageSize();
					storage_size += rasterdb.rasterPyr1Unit().calculateStorageSize();
					storage_size += rasterdb.rasterPyr2Unit().calculateStorageSize();
					storage_size += rasterdb.rasterPyr3Unit().calculateStorageSize();
					storage_size += rasterdb.rasterPyr4Unit().calculateStorageSize();
					json.key("storage_size");
					json.value(storage_size);
				} catch(Exception e) {
					log.warn(e);
				}
			}
			if(requestInternalStorageInternalFreeSize) {
				try {
					long storage_internal_free_size = rasterdb.hasRasterUnit() ? rasterdb.rasterUnit().calculateInternalFreeSize() : 0;
					storage_internal_free_size += rasterdb.rasterPyr1Unit().calculateInternalFreeSize();
					storage_internal_free_size += rasterdb.rasterPyr2Unit().calculateInternalFreeSize();
					storage_internal_free_size += rasterdb.rasterPyr3Unit().calculateInternalFreeSize();
					storage_internal_free_size += rasterdb.rasterPyr4Unit().calculateInternalFreeSize();
					json.key("storage_internal_free_size");
					json.value(storage_internal_free_size);
				} catch(Exception e) {
					log.warn(e);
				}
			}
			json.endObject(); // end full object
		} catch(Exception e) {
			e.printStackTrace();
			log.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}		
	}

}