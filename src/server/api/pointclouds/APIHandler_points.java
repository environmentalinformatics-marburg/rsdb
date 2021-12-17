package server.api.pointclouds;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.yaml.snakeyaml.Yaml;

import broker.Broker;
import broker.Informal;
import broker.TimeSlice;
import broker.InformalProperties.Builder;
import pointcloud.AttributeSelector;
import pointcloud.CellTable;
import pointcloud.CellTable.ChainedFilterFunc;
import pointcloud.PointCloud;
import pointdb.base.Rect;
import pointdb.subsetdsl.Region;
import server.api.pointclouds.PointProcessor.PointTableTransformFunc;
import util.Receiver;
import util.ResponseReceiver;
import util.StreamReceiver;
import util.Web;

public class APIHandler_points {
	

	protected static final String MIME_JSON = "application/json";	
	protected static final String MIME_CSV = "text/csv";

	//private final Broker broker;

	public APIHandler_points(Broker broker) {
		//this.broker = broker;
	}

	public void handle(PointCloud pointcloud, String format, Request request, HttpServletResponse response) throws IOException {
		String extText = request.getParameter("ext");
		String polygonText = request.getParameter("polygon");
		double xmin = Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;
		double xmax = -Double.MAX_VALUE;
		double ymax = -Double.MAX_VALUE;

		Rect boundingRect = null;
		Region requestRegion = null;

		if(polygonText != null) {
			if(extText != null) {
				throw new RuntimeException("If 'polygon' parameter is present, parameter 'ext' can not be specified.");
			}
			String[] cTexts = polygonText.split(" ");
			int len = cTexts.length;
			if(len % 2 != 0) {
				throw new RuntimeException();
			}
			if(len < 6) {
				throw new RuntimeException();
			}
			int vlen = len / 2;
			double[] vx = new double[vlen];
			double[] vy = new double[vlen];
			for (int i = 0; i < vlen; i++) {
				vx[i] = Double.parseDouble(cTexts[i]);
				if(vx[i]<xmin) xmin = vx[i];
				if(vx[i]>xmax) xmax = vx[i];
			}
			for (int i = 0; i < vlen; i++) {
				vy[i] = Double.parseDouble(cTexts[i + vlen]);
				if(vy[i]<ymin) ymin = vy[i];
				if(vy[i]>ymax) ymax = vy[i];
			}
			requestRegion = Region.ofPolygon(vx, vy);
			boundingRect = requestRegion.bbox;
		} else if(extText != null) {
			String[] ext = extText.split(" ");
			if(ext.length != 4) {
				throw new RuntimeException("parameter error in 'ext': "+extText);
			}
			xmin = Double.parseDouble(ext[0]);
			ymin = Double.parseDouble(ext[1]);
			xmax = Double.parseDouble(ext[2]);
			ymax = Double.parseDouble(ext[3]);
			boundingRect = Rect.of_UTM(xmin, ymin, xmax, ymax);
			requestRegion = Region.ofRect(boundingRect);
		} else {
			throw new RuntimeException("missing parameter 'ext' (or 'polygon')");
		}

		TimeSlice timeSlice = null;
		if(Web.has(request, "time_slice_id")) {
			int time_slice_id = Web.getInt(request, "time_slice_id");
			timeSlice = pointcloud.timeMapReadonly.get(time_slice_id);
			if(timeSlice == null) {
				if(time_slice_id == 0) {
					timeSlice = new TimeSlice(0, "default");
				} else {
					throw new RuntimeException("uknown time_slice_id: " + time_slice_id);
				}
			}
			if(Web.has(request, "time_slice_name") && !Web.getString(request, "time_slice_name").equals(timeSlice.name)) {
				throw new RuntimeException("time_slice_name does not match to time slice of time_slice_id: '" + Web.getString(request, "time_slice_name") + "'  '" + timeSlice.name + "'");
			}
		} else if(Web.has(request, "time_slice_name")) {
			String time_slice_name = Web.getString(request, "time_slice_name");
			timeSlice = pointcloud.getTimeSliceByName(time_slice_name);
			if(timeSlice == null) {
				throw new RuntimeException("unknown time_slice_name: " + time_slice_name);
			}
		} else if(!pointcloud.timeMapReadonly.isEmpty()) {
			timeSlice = pointcloud.timeMapReadonly.lastEntry().getValue();
		}
		int req_t = timeSlice == null ? 0 : timeSlice.id;



		PointTableTransformFunc transformFunc = null;
		/*boolean normalise_ground = Web.getBoolean(request, "normalise_ground", false);
		if(normalise_ground) {
			transformFunc = Normalise::normalise_ground;
		}*/
		PointTableTransformFunc tf = transformFunc;


		String columnsText = request.getParameter("columns");
		String[] columns = columnsText == null ? null : columnsText.split("(\\s|,)+"); // split by spaces and/or comma
		AttributeSelector selector = columns == null ? null : AttributeSelector.of(columns);
		ChainedFilterFunc filterFunc = CellTable.parseFilter(request.getParameter("filter"));

		if(format.equals("zip")) {
			String tileFormat = "las";
			response.setContentType("application/zip");
			ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
			zipOutputStream.setLevel(Deflater.NO_COMPRESSION);
			write_dublin_core(pointcloud, boundingRect, zipOutputStream);
			Receiver receiver = new StreamReceiver(zipOutputStream);
			Region rr = requestRegion;
			boundingRect.tiles_utmm(1000_000, 1000_000, (xtile, ytile, tileRect) -> {
				Logger.info(tileRect);
				String tileFilename = "tile_" + xtile + "_" + ytile + ".las";
				try {
					Region tileRegion = Region.ofFilteredBbox(tileRect, rr.polygonPoints);
					zipOutputStream.putNextEntry(new ZipEntry(tileFilename));
					double txmin = tileRect.getUTMd_min_x();
					double tymin = tileRect.getUTMd_min_y();
					double txmax = tileRect.getUTMd_max_x_inclusive();
					double tymax = tileRect.getUTMd_max_y_inclusive();
					PointProcessor.process(pointcloud, req_t, txmin, tymin, txmax, tymax, tf, filterFunc, tileRegion, tileFormat, receiver, request, selector, columns);
					zipOutputStream.closeEntry();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}				
			});			
			zipOutputStream.finish();
			zipOutputStream.flush();
		} else {
			ResponseReceiver receiver = new ResponseReceiver(response);		
			PointProcessor.process(pointcloud, req_t, xmin, ymin, xmax, ymax, transformFunc, filterFunc, requestRegion, format, receiver, request, selector, columns);
		}
	}

	private void write_dublin_core(PointCloud pointcloud, Rect ext, ZipOutputStream zipOutputStream) throws IOException {
		zipOutputStream.putNextEntry(new ZipEntry("metadata.yaml"));
		try {
			write_dublin_core_metadata(pointcloud, ext, zipOutputStream);
		} finally {
			zipOutputStream.closeEntry();	
		}		
	}

	private void write_dublin_core_metadata(PointCloud pointcloud, Rect ext, OutputStream out) {
		Informal informal = pointcloud.informal();

		Builder properties = informal.toBuilder().properties;

		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("identifier", pointcloud.getName());
		if(informal.hasTitle()) {
			properties.prepend("title", informal.title);
		}
		if(!informal.tags.isEmpty()) {
			properties.prepend("subject", informal.tags);
		}
		if(informal.hasDescription()) {
			properties.prepend("description.abstract", informal.description);
		}
		if(informal.hasAcquisition_date()) {
			properties.prepend("date.created", informal.acquisition_date);
		}
		if(informal.has_corresponding_contact()) {
			properties.prepend("publisher", informal.corresponding_contact);
		}
		properties.prepend("type", "pointcloud");		
		properties.prepend("format", "points-file");

		if(ext != null) {
			String coverage = "extent (" + ext.getUTMd_min_x() + ", " + ext.getUTMd_min_y() + " to " + ext.getUTMd_max_x() + ", " + ext.getUTMd_max_y() + ")";

			if(pointcloud.hasCode()|| pointcloud.hasProj4()) {
				coverage += "   in ";
				if(pointcloud.hasCode()) {
					coverage += pointcloud.getCode();
					if(pointcloud.hasProj4()) {
						coverage += "    ";
					}
				}
				if(pointcloud.hasProj4()) {
					coverage += "PROJ4: " + pointcloud.getProj4();
				}
			}
			properties.prepend("coverage", coverage);
		}

		Map<String, Object> outMap = properties.build().toSortedYaml();

		Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
		new Yaml().dump(outMap, writer);
	}
}
