package server.api.pointdb;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONWriter;

import broker.Broker;
import broker.acl.EmptyACL;
import pointdb.PointDB;
import pointdb.base.PdbConst;
import pointdb.processing.tilemeta.StatisticsCreator.Statistics;
import util.JsonUtil;
import util.Web;

public class APIHandler_info extends PointdbAPIHandler {
	private static final Logger log = LogManager.getLogger();	

	private static final String MIME_JSON = "application/json";

	public APIHandler_info(Broker broker) {
		super(broker, "info");		
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		request.setHandled(true);

		PointDB db = getPointdb(request);
		Statistics stat = Web.getBoolean(request, "statistics", true) ? db.tileMetaProducer(null).toStatistics() : null;

		String proj4 = db.config.getProj4();
		String projectionText = proj4;
		try {
			if(proj4.contains("+proj=utm")) {
				String prefix = "+zone=";
				int i = proj4.indexOf(prefix);
				if(i>=0) {
					int pos = i+prefix.length();
					String zoneName = proj4.substring(pos, pos+3).trim();
					projectionText = "UTM-"+zoneName;
				}
			}
		} catch(Exception e) {
			log.warn(e);
		}

		if(projectionText.isEmpty()) {
			int epsg = db.config.getEPSG(); 
			if(epsg>0) {
				projectionText = "EPSG:"+db.config.getEPSG();
			} else {
				projectionText = "unknown";
			}
		}

		//if(MIME_JSON.equals(request.getHeader("Accept"))) { // JSON
		response.setContentType(MIME_JSON);
		log.info(response.getWriter().getClass());
		JSONWriter json = new JSONWriter(response.getWriter());
		//log.info("JSON!");
		json.object();
		json.key("db");
		json.value(db.config.name);
		db.informal().writeJson(json);
		json.key("tile_size");
		json.value(PdbConst.UTM_TILE_SIZE);		
		json.key("proj4");
		json.value(db.config.getProj4());
		json.key("epsg");
		json.value(db.config.getEPSG());
		json.key("projectionText");
		json.value(projectionText);		
		json.key("classified");		
		json.value(db.config.isClassified_ground());
		json.key("classified_vegetation");		
		json.value(db.config.isClassified_vegetation());
		json.key("raster_processing_types");		
		RasterQueryProcessor.writeProcessingTypesJSON(json);
		if(EmptyACL.ADMIN.isAllowed(Web.getUserIdentity(request))) {
			json.key("acl");
			db.config.getAcl().writeJSON(json);
		}
		json.key("associated");
		json.object();
		JsonUtil.writeOptList(json, "poi_groups", db.config.getPoiGroupNames());
		JsonUtil.writeOptList(json, "roi_groups", db.config.getRoiGroupNames());
		if(db.config.hasRasterDB()) {
			json.key("rasterdb");
			json.value(db.config.getRasterDB());
		}
		json.endObject();
		if(stat != null) {
			json.key("tiles");
			json.value(stat.tile_sum);
			json.key("points");
			json.value(stat.point_count_sum);
			json.key("tile_x_min");			
			json.value(stat.tile_x_min);
			json.key("tile_x_max");			
			json.value(stat.tile_x_max);
			json.key("tile_y_min");
			json.value(stat.tile_y_min);				
			json.key("tile_y_max");
			json.value(stat.tile_y_max);
			json.key("local_z_min");
			json.value(stat.local_z_min);
			json.key("local_z_max");
			json.value(stat.local_z_max);
			json.key("local_z_avg");
			json.value(stat.local_z_avg);
			json.key("local_significant_z_avg");
			json.value(stat.local_significant_z_avg);
			json.key("tile_significant_z_avg_min");
			json.value(stat.tile_significant_z_avg_min);
			json.key("tile_significant_z_avg_max");
			json.value(stat.tile_significant_z_avg_max);
			json.key("intensity_min");
			json.value(stat.intensity_min);
			json.key("intensity_max");
			json.value(stat.intensity_max);
			json.key("intensity_avg");
			json.value(stat.intensity_avg);
			json.key("intensity_significant_avg");
			json.value(stat.intensity_significant_avg);
			json.key("tile_significant_intensity_avg_min");
			json.value(stat.tile_significant_intensity_avg_min);
			json.key("tile_significant_intensity_avg_max");
			json.value(stat.tile_significant_intensity_avg_max);
			json.key("x_range");			
			json.value(stat.tile_x_max + PdbConst.LOCAL_TILE_SIZE - stat.tile_x_min);
			json.key("y_range");		
			json.value(stat.tile_y_max + PdbConst.LOCAL_TILE_SIZE - stat.tile_y_min);
		}
		json.endObject();

		/*} else { // Plain Text
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("text/plain;charset=utf-8");
			PrintWriter out = response.getWriter();
			out.println("PointDB");			

			out.println("tiles "+stat.tile_sum);
			out.println("points "+stat.point_count_sum);
			out.println("tile size "+PdbConst.UTM_TILE_SIZE+" UTM units (meter)");
			out.println("points per tile "+stat.point_count_min+" - "+stat.point_count_max+" (avg "+stat.point_count_avg+")");
			out.println("points per significant tile "+stat.point_count_significant_min+" - "+stat.point_count_significant_max+" (avg "+stat.point_count_significant_avg+")");
			out.println("point geo range "+PdbConst.utmm_to_double(stat.utmm_x_min)+", "+PdbConst.utmm_to_double(stat.utmm_y_min)+" - "+PdbConst.utmm_to_double(stat.utmm_x_max)+", "+PdbConst.utmm_to_double(stat.utmm_y_max));
			out.println("tile geo range "+stat.tile_x_min+", "+stat.tile_y_min+" - "+(stat.tile_x_max+PdbConst.UTM_TILE_SIZE)+", "+(stat.tile_y_max+PdbConst.UTM_TILE_SIZE));
			out.println("tile local range "+PdbConst.localToUTM(stat.local_x_min)+", "+PdbConst.localToUTM(stat.local_y_min)+" - "+PdbConst.localToUTM(stat.local_x_max)+", "+PdbConst.localToUTM(stat.local_y_max));
			out.println("intensity "+stat.intensity_min+" - "+stat.intensity_max+" (avg "+stat.intensity_avg+")");
			out.println("significant intensity "+stat.tile_significant_intensity_avg_min+" - "+stat.tile_significant_intensity_avg_max);
			out.println("z "+PdbConst.localToUTM(stat.local_z_min)+" - "+PdbConst.localToUTM(stat.local_z_max)+" (avg "+PdbConst.localToUTM(stat.local_z_avg)+")");
			out.println("significant z "+PdbConst.localToUTM(stat.tile_significant_z_avg_min)+" - "+PdbConst.localToUTM(stat.tile_significant_z_avg_max));			
			out.println("returnNumber "+stat.returnNumber_min+" - "+stat.returnNumber_max);
			out.println("returns "+stat.returns_min+" - "+stat.returns_max);
			out.println("scanAngleRank "+stat.scanAngleRank_min+" - "+stat.scanAngleRank_max);
		}*/		
	}
}
