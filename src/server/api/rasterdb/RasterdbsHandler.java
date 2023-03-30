package server.api.rasterdb;

import java.io.IOException;
import java.util.TreeSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONWriter;

import broker.Broker;
import rasterdb.Band;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import server.api.main.APIHandler_session;
import util.JsonUtil;
import util.Range2d;
import util.Web;

public class RasterdbsHandler extends AbstractHandler {

	private final Broker broker;

	public RasterdbsHandler(Broker broker) {
		this.broker = broker;
	}

	@Override
	public void handle(String target, Request request, HttpServletRequest internal, HttpServletResponse response)
			throws IOException, ServletException {

		boolean includeBands = request.getParameter("bands") != null;
		boolean includeCode = request.getParameter("code") != null;
		boolean includeRef = request.getParameter("ref") != null;
		boolean includeInfo = request.getParameter("info") != null;

		Logger.info(request + "    size: " + request.getContentLength());
		request.setHandled(true);
		try {
			TreeSet<String> rasterdbTags = new TreeSet<>();
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(Web.MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("rasterdbs");
			json.array();			
			for (String name : broker.getRasterdbNames()) {				
				RasterDB rasterdb = broker.getRasterdb(name);
				if (rasterdb.isAllowed(Web.getUserIdentity(request))) {
					json.object();
					JsonUtil.put(json, "name", name);

					String title = rasterdb.informal().title;
					if(title != null && !title.isEmpty()) {
						JsonUtil.put(json, "title", title);
					} else {
						JsonUtil.put(json, "title", name);
					}
					JsonUtil.writeOptList(json, "tags", rasterdb.informal().tags);
					for(String tag:rasterdb.informal().tags) {
						rasterdbTags.add(tag);
					}

					if(includeInfo) {
						JsonUtil.put(json, "description", rasterdb.informal().description);
						JsonUtil.optPut(json, "acquisition_date", rasterdb.informal().acquisition_date);
						JsonUtil.optPut(json, "corresponding_contact", rasterdb.informal().corresponding_contact);
					}

					if(includeBands) {
						json.key("bands");
						json.array();
						for(Band band:rasterdb.bandMapReadonly.values()) {
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
					}
					if(includeCode && rasterdb.ref().has_code()) {
						json.key("code");
						json.value(rasterdb.ref().code);
					}
					if(includeRef) {
						GeoReference ref = rasterdb.ref();
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
								json.value(ref.pixelXToGeo(localRange.xmax + 1));
								json.value(ref.pixelYToGeo(localRange.ymax + 1));
								json.endArray();
								json.key("internal_rasterdb_extent");
								json.array();
								json.value(localRange.xmin);
								json.value(localRange.ymin);
								json.value(localRange.xmax);
								json.value(localRange.ymax);
								json.endArray();
							}
						}
						json.endObject();
					}
					json.endObject();
				}
			}
			json.endArray();
			json.key("tags");
			json.array();
			for(String tag:rasterdbTags) {
				json.value(tag);				
			}
			json.endArray();
			json.key("session");
			json.value(APIHandler_session.createSession());
			json.endObject();

		} catch (Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}
	}

}
