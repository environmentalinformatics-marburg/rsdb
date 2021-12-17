package server.api.main;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONWriter;

import broker.Broker;
import rasterdb.RasterDB;
import server.api.APIHandler;
import util.Web;

public class APIHandler_create_raster extends APIHandler {
	

	public APIHandler_create_raster(Broker broker) {
		super(broker, "create_raster");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {		
		String name = request.getParameter("name");
		if(name==null) {
			throw new RuntimeException("missing name parameter");
		}
		boolean transaction = true;
		String storage_type = "TileStorage";
		String storage_type_parameter = request.getParameter("storage_type");
		if(storage_type_parameter != null && !storage_type_parameter.isEmpty()) {
			Logger.info("set storage_type" + storage_type_parameter);
			storage_type = storage_type_parameter;
		}
		String proj4 = request.getParameter("proj4");
		
		double xres = Double.NaN;
		double yres = Double.NaN;
		
		String xresText = request.getParameter("xres");
		if(xresText != null) {
			xres = Double.parseDouble(xresText);
		}
		
		String yresText = request.getParameter("yres");
		if(yresText != null) {
			yres = Double.parseDouble(yresText);
		}
		
		boolean create_new = Web.getBoolean(request, "create_new", true);
		
		RasterDB rasterdb = create_new ? broker.createNewRasterdb(name, transaction, storage_type): broker.createOrGetRasterdb(name, transaction, storage_type);
		
		if(proj4 != null && !proj4.isEmpty()) {
			rasterdb.setProj4(proj4);
		}
		if(Double.isFinite(xres) && Double.isFinite(yres)) {
			rasterdb.setPixelSize(xres, yres, 0, 0);
		} else if(Double.isFinite(xres) ^ Double.isFinite(yres)) {
			throw new RuntimeException("both parameter xres and yres need to be present, or none of them");
		}
		
		rasterdb.close();

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("result");
		json.value("created raster "+name);
		json.endObject();
	}
}
