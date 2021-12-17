package server.api.rasterdb;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

import broker.Broker;
import broker.TimeSlice;
import broker.TimeSlice.TimeSliceBuilder;
import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.tile.ProcessingFloat;
import rasterdb.tile.ProcessingShort;
import rasterdb.tile.TileFloat;
import rasterdb.tile.TilePixel;
import rasterdb.tile.TileShort;
import rasterunit.RasterUnitStorage;
import util.Serialisation;
import util.TimeUtil;
import util.Web;
import util.collections.vec.Vec;

public class RasterdbMethod_insert_raster extends RasterdbMethod {
	

	public RasterdbMethod_insert_raster(Broker broker) {
		super(broker, "insert_raster");	
	}

	public int getDataTypeSize(String dataTypeText) {
		switch(dataTypeText) {
		case "int16": return 2;
		case "float32": return 4;
		default:
			throw new RuntimeException("unknown data_type: " + dataTypeText);
		}			
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		Logger.info("!!! inser_raster !!!"+request);
		request.setHandled(true);
		RasterUnitStorage rasterUnit = rasterdb.rasterUnit();
		try {
			Vec<String> messages = new Vec<String>();

			String[] extText = (request.getParameter("ext").split(" "));
			Logger.info("ext "+Arrays.toString(extText));

			double geoxmin = Double.parseDouble(extText[0]);
			double geoymin = Double.parseDouble(extText[1]);
			double geoxmax = Double.parseDouble(extText[2]);
			double geoymax = Double.parseDouble(extText[3]);

			Logger.info("geoxmin "+geoxmin);
			Logger.info("geoymin "+geoymin);
			Logger.info("geoxmax "+geoxmax);
			Logger.info("geoymax "+geoymax);
			Logger.info("geoxrange " + (geoxmax - geoxmin));
			Logger.info("geoyrange " + (geoymax - geoymin));

			int width = Integer.parseInt(request.getParameter("width"));
			int height = Integer.parseInt(request.getParameter("height"));
			Logger.info("width " + width);
			Logger.info("heigth " + height);

			double pixel_size_x = (geoxmax - geoxmin) / width;
			double pixel_size_y = (geoymax - geoymin) / height;
			Logger.info("pixel_size_x " + pixel_size_x);
			Logger.info("pixel_size_y " + pixel_size_y);

			if(rasterdb.ref().has_pixel_size()) {
				double rasterdb_pixel_size_x = rasterdb.ref().pixel_size_x;
				double rasterdb_pixel_size_y = rasterdb.ref().pixel_size_y;
				if(        Math.abs(pixel_size_x) < Math.abs(rasterdb_pixel_size_x) * 0.99d 
						|| Math.abs(rasterdb_pixel_size_x) * 1.01d < Math.abs(pixel_size_x)
						|| Math.abs(pixel_size_y) < Math.abs(rasterdb_pixel_size_y) * 0.99d 
						|| Math.abs(rasterdb_pixel_size_y) * 1.01d < Math.abs(pixel_size_y)
						) {
					messages.add("WARNING: RasterDB layer and inserted raster resolutions do not match, results may not be what you expect. RasterDB layer " + rasterdb_pixel_size_x + " x " + rasterdb_pixel_size_y + "  raster data " + pixel_size_x + " x " + pixel_size_y);
				}
			} else {			
				double rasterdb_geo_offset_x = geoxmin;
				double rasterdb_geo_offset_y = geoymin;
				rasterdb.setPixelSize(pixel_size_x, pixel_size_y, rasterdb_geo_offset_x, rasterdb_geo_offset_y);
			}

			String proj4 = request.getParameter("proj4");

			boolean flush = Web.getBoolean(request, "flush", true);

			boolean update_pyramid = Web.getBoolean(request, "update_pyramid", true);

			if(!proj4.isEmpty()) {
				if(rasterdb.ref().has_proj4()) {
					if(!proj4.trim().equals(rasterdb.ref().proj4.trim())) {
						messages.add("WARNING: RasterDB layer and inserted raster CRS do not match: "  + rasterdb.ref().proj4 + "   vs   " + proj4); 	
					}
				} else {
					rasterdb.setProj4(proj4+' '); // space at end of proj4
				}
			}

			ServletInputStream in = request.getInputStream();
			long request_size = request.getContentLengthLong();
			Logger.info("request_size "+request_size);

			String dataTypeText = request.getParameter("data_type");
			if(dataTypeText == null) {
				dataTypeText = "int16"; // compatibility to old R-package versions
			}
			int dataTypeSize = getDataTypeSize(dataTypeText);

			int raw_size = width * height * dataTypeSize;
			Logger.info("raw_size " + raw_size);
			byte[] raw = new byte[raw_size];
			int pos = 0;
			while(pos < raw_size) {
				Logger.info("read at " + pos + " of " + raw_size);
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
				LocalDateTime[] timerange = null;
				try{
					timerange = TimeUtil.getDateTimeRange(timestampText);
				}catch(Exception e) {
					Logger.warn("could not parse timestamp: " + timestampText);
				}
				if(timerange != null) {
					t = TimeUtil.toTimestamp(timerange[0]);
				} else {
					try{
						t = Integer.parseInt(timestampText);
					}catch(Exception e) {
						throw new RuntimeException("could not parse timestamp: "+timestampText);
					}
				}
			}

			boolean useTimeSlice = false;
			String time_sliceText = request.getParameter("time_slice");
			if(time_sliceText != null) {
				if(timestampText != null) {
					throw new RuntimeException("only one parameter can be specified: time_slice or timestamp");
				}
				useTimeSlice = true;
			}

			String flip_y = request.getParameter("flip_y") == null ? "FALSE" : request.getParameter("flip_y");
			boolean flipY = flip_y.trim().toUpperCase().equals("TRUE");
			int pixelXmin = rasterdb.ref().geoXToPixel(geoxmin);
			int pixelYmin = rasterdb.ref().geoYToPixel(geoymin);

			if(useTimeSlice) {
				TimeSlice timeSlice = rasterdb.getOrCreateTimeSliceByName(time_sliceText);
				t = timeSlice.id;
			}

			int bandIndex = Integer.parseInt(request.getParameter("band"));
			Band band = rasterdb.bandMapReadonly.get(bandIndex);
			String band_title = request.getParameter("band_title"); // nullable

			switch(dataTypeText) {
			case "int16": {
				short[][] pixels = flipY ? Serialisation.byteToShortArrayArrayFlipY(raw, width, height) : Serialisation.byteToShortArrayArray(raw, width, height);
				if(band == null) {
					band = Band.of(TilePixel.TYPE_SHORT, bandIndex, band_title, null);
					rasterdb.setBand(band, true);
				}
				switch(band.type) {
				case TilePixel.TYPE_SHORT: {
					ProcessingShort.writeMerge(rasterUnit, t, band, pixels, pixelYmin, pixelXmin);
					break;
				}
				case TilePixel.TYPE_FLOAT: {
					short sourceNA = 0;
					float[][] converted_pixels = TileFloat.shortToFloat(pixels, null, sourceNA);
					ProcessingFloat.writeMerge(rasterUnit, t, band, converted_pixels, pixelYmin, pixelXmin);
					Logger.info("converted int16 data to float32 band data");
					messages.add("converted int16 data to float32 band data");
					break;
				}
				default:
					throw new RuntimeException("unknown band data type: " + band.type);
				}
				break;
			}
			case "float32": {
				float[][] pixels = flipY ? Serialisation.byteToFloatArrayArrayFlipY(raw, width, height) : Serialisation.byteToFloatArrayArray(raw, width, height);
				if(band == null) {
					band = Band.of(TilePixel.TYPE_FLOAT, bandIndex, band_title, null);
					rasterdb.setBand(band, true);
				}
				switch(band.type) {
				case TilePixel.TYPE_SHORT: {
					short targetNA = band.getInt16NA();
					short[][] converted_pixels = TileFloat.floatToShort(pixels, null, targetNA);
					ProcessingShort.writeMerge(rasterUnit, t, band, converted_pixels, pixelYmin, pixelXmin);
					Logger.warn("converted float32 data to int16 band data");
					messages.add("converted float32 data to int16 band data");
					break;
				}
				case TilePixel.TYPE_FLOAT: {
					ProcessingFloat.writeMerge(rasterUnit, t, band, pixels, pixelYmin, pixelXmin);
					break;
				}					
				default:
					throw new RuntimeException("unknown band data type: " + band.type);
				}
				break;				
			}
			default:
				throw new RuntimeException("unknown data_type: " + dataTypeText);
			}			

			if(update_pyramid) {
				rasterdb.rebuildPyramid(true);
			}
			if(flush && !update_pyramid) {
				rasterdb.flush();
			}			
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("result");
			json.value("inserted data");
			if(!messages.isEmpty()) {
				json.key("messages");
				json.array();
				messages.forEach(m -> json.value(m));
				json.endArray();
			}
			json.endObject();
		} catch(Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}		
	}

}