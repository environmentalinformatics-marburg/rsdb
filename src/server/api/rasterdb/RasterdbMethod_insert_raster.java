package server.api.rasterdb;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

import broker.Broker;
import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.tile.ProcessingShort;
import rasterdb.tile.TilePixel;
import rasterunit.RasterUnit;
import rasterunit.RasterUnitStorage;
import util.Serialisation;

public class RasterdbMethod_insert_raster extends RasterdbMethod {
	private static final Logger log = LogManager.getLogger();

	public RasterdbMethod_insert_raster(Broker broker) {
		super(broker, "insert_raster");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		log.info("!!! inser_raster !!!"+request);
		request.setHandled(true);
		RasterUnitStorage rasterUnit = rasterdb.rasterUnit();
		try {

			boolean flush = true;
			boolean rebuild_pyramid = true;

			String[] extText = (request.getParameter("ext").split(" "));
			log.info("ext "+Arrays.toString(extText));

			double geoxmin = Double.parseDouble(extText[0]);
			double geoymin = Double.parseDouble(extText[1]);
			double geoxmax = Double.parseDouble(extText[2]);
			double geoymax = Double.parseDouble(extText[3]);

			log.info("geoxmin "+geoxmin);
			log.info("geoymin "+geoymin);
			log.info("geoxmax "+geoxmax);
			log.info("geoymax "+geoymax);
			log.info("geoxrange " + (geoxmax - geoxmin));
			log.info("geoyrange " + (geoymax - geoymin));

			int width = Integer.parseInt(request.getParameter("width"));
			int height = Integer.parseInt(request.getParameter("height"));
			log.info("width " + width);
			log.info("heigth " + height);

			double pixel_size_x = (geoxmax - geoxmin) / width;
			double pixel_size_y = (geoymax - geoymin) / height;
			log.info("pixel_size_x " + pixel_size_x);
			log.info("pixel_size_y " + pixel_size_y);

			if(!rasterdb.ref().has_pixel_size()) {
				double rasterdb_geo_offset_x = geoxmin;
				double rasterdb_geo_offset_y = geoymin;
				rasterdb.setPixelSize(pixel_size_x, pixel_size_y, rasterdb_geo_offset_x, rasterdb_geo_offset_y);
			}

			String proj4 = request.getParameter("proj4");

			if(!proj4.isEmpty() && !rasterdb.ref().has_proj4()) {
				rasterdb.setProj4(proj4+' '); // space at end of proj4
			}

			ServletInputStream in = request.getInputStream();
			long reqest_size = request.getContentLengthLong();
			log.info("reqest_size "+reqest_size);
			int raw_size = width * height * 2;
			log.info("raw_size "+raw_size);
			byte[] raw = new byte[raw_size];
			int pos = 0;
			while(pos < raw_size) {
				log.info("read at "+pos+" of "+raw_size);
				int read_size = in.read(raw, pos, raw_size - pos);
				if(read_size < 1) {
					throw new RuntimeException("not all bytes read "+pos+"  of  "+raw_size);
				}
				pos += read_size;
			}
			if(pos != raw_size) {
				throw new RuntimeException("not all bytes read "+pos+"  of  "+raw_size);
			}

			int t = 0;
			String timestampText = request.getParameter("timestamp");
			if(timestampText != null) {
				try{
					t = Integer.parseInt(timestampText);
				}catch(Exception e) {
					throw new RuntimeException("could not parse timestamp: "+timestampText);
				}
			}


			String flip_y = request.getParameter("flip_y") == null ? "FALSE" : request.getParameter("flip_y");
			boolean flipY = flip_y.trim().toUpperCase().equals("TRUE");
			short[][] pixels = flipY ? Serialisation.byteToShortArrayArrayFlipY(raw, width, height) : Serialisation.byteToShortArrayArray(raw, width, height);
			int pixelXmin = rasterdb.ref().geoXToPixel(geoxmin);
			int pixelYmin = rasterdb.ref().geoYToPixel(geoymin);


			int bandIndex = Integer.parseInt(request.getParameter("band"));
			Band band = rasterdb.bandMap.get(bandIndex);
			if(band == null) {
				String band_title = request.getParameter("band_title"); // nullable
				band = Band.of(TilePixel.TYPE_SHORT, bandIndex, band_title, null);
				rasterdb.setBand(band);
			}			
			ProcessingShort.writeMerge(rasterUnit, t, band, pixels, pixelYmin, pixelXmin);
			if(rebuild_pyramid) {
				rasterdb.rebuildPyramid(true);
			}
			if(flush && !rebuild_pyramid) {
				rasterdb.flush();
			}			
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("result");
			json.value("inserted data");
			json.endObject();
		} catch(Exception e) {
			e.printStackTrace();
			log.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}		
	}

}