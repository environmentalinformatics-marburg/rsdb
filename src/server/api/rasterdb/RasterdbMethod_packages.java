package server.api.rasterdb;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.gdal.osr.SpatialReference;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.yaml.snakeyaml.Yaml;

import broker.Broker;
import broker.Informal;
import broker.InformalProperties.Builder;
import broker.TimeSlice;
import rasterdb.Band;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.TimeBand;
import rasterdb.TimeBandProcessor;
import server.api.rasterdb.RequestProcessor.OutputProcessingType;
import util.CharArrayWriterUnsync;
import util.IndentedXMLStreamWriter;
import util.JsonUtil;
import util.Range2d;
import util.StreamReceiver;
import util.TimeUtil;
import util.Web;
import util.collections.array.iterator.ReadonlyArrayIterator;
import util.collections.vec.Vec;

public class RasterdbMethod_packages extends RasterdbMethod {
	private static final Logger log = LogManager.getLogger();

	private final Map<Long, Spec> specMap = new ConcurrentHashMap<>();

	private static class Spec {

		private static AtomicLong idCounter = new AtomicLong(0);

		public final long id = idCounter.incrementAndGet();
		public final RasterDB rasterdb;
		public double[] ext = null;
		public Collection<Band> bands = null;
		public Collection<Integer> time_slice_ids = null;
		public int compression = 0;
		public int div = 1;
		public String arrangement = "multiband";

		public Spec(RasterDB rasterdb) {
			this.rasterdb = rasterdb;
		}

		public String getZipFileUrl() {
			return id + ".zip";
		}

		public String getZipFileName() {
			return rasterdb.config.getName() + ".zip";
		}
	}

	public RasterdbMethod_packages(Broker broker) {
		super(broker, "packages");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		log.info(request);
		request.setHandled(true);
		String reqMethod = request.getMethod();
		switch(reqMethod) {
		case "POST":
			post(rasterdb, target, request, response, userIdentity);
			break;
		case "GET":
			get(rasterdb, target, request, response, userIdentity);
			break;
		default:
			throw new RuntimeException("unknown request method: "  + reqMethod);
		}
	}

	private void post(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		try {
			JSONObject json = new JSONObject(Web.requestContentToString(request));
			JSONObject meta = json.getJSONObject("package");
			log.info(meta);
			meta.getJSONArray("ext");
			Spec spec = new Spec(rasterdb);
			double[] ext = JsonUtil.getDoubleArray(meta, "ext");
			if(ext.length != 4) {
				throw new RuntimeException();
			}
			spec.ext = ext;
			if(meta.has("bands")) {
				int[] bands = JsonUtil.getIntArray(meta, "bands");				
				Collection<Band> processingBands = Arrays.stream(bands).mapToObj(i -> {
					Band band = rasterdb.getBandByNumber(i);
					if (band == null) {
						throw new RuntimeException("band not found: " + i);
					}
					return band;
				}).collect(Collectors.toList());
				spec.bands = processingBands;
			} else {
				spec.bands = rasterdb.bandMapReadonly.values();
			}

			if(meta.has("time_slice_ids")) {
				spec.time_slice_ids = JsonUtil.getIntegerVec(meta, "time_slice_ids");
			} else {
				spec.time_slice_ids = rasterdb.rasterUnit().timeKeysReadonly();
			}

			if(meta.has("compression")) {
				spec.compression = JsonUtil.getInt(meta, "compression");
			}

			if(meta.has("arrangement")) {
				spec.arrangement  = JsonUtil.getString(meta, "arrangement");
			}

			if(meta.has("div")) {
				spec.div = JsonUtil.getInt(meta, "div");
			}

			specMap.put(spec.id, spec);

			response.setContentType(MIME_JSON);
			JSONWriter jsonResponse = new JSONWriter(response.getWriter());
			jsonResponse.object();
			jsonResponse.key("package");
			jsonResponse.object();
			jsonResponse.key("zip_file_url");
			jsonResponse.value(spec.getZipFileUrl());
			jsonResponse.key("zip_file_name");
			jsonResponse.value(spec.getZipFileName());
			jsonResponse.endObject();
			jsonResponse.endObject();
		} catch(Exception e) {
			e.printStackTrace();
			log.error(e);			
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

			response.setContentType(MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("response");
			json.object();
			json.endObject();
			json.endObject();
		} 			
	}

	private void get(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {	
		String name = rasterdb.config.getName();
		long id = Long.parseLong(target.replace(".zip", ""));
		Spec spec = specMap.get(id);
		Range2d range2d = rasterdb.ref().bboxToRange2d(spec.ext);
		ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
		/*if(spec.compression == 0) {
			zipOutputStream.setMethod(ZipOutputStream.STORED); //not valid: size and crc32 of entry needs to be known
		} else {
			zipOutputStream.setMethod(ZipOutputStream.DEFLATED);
		}*/
		zipOutputStream.setLevel(spec.compression);
		write_dublin_core(spec, zipOutputStream);
		boolean tiled = true;

		Vec<VrtEntry> vrtCollector = new Vec<VrtEntry>();
		
		String dataPath = "data/";
		if(tiled) {
			int tileSize = 4096 * spec.div;
			range2d.tiled(tileSize, tileSize, (int xtile, int ytile, Range2d tile_range2d) -> {
				String tile_name = name + "__" + xtile + "_" + ytile;
				TimeBandProcessor processor = new TimeBandProcessor(rasterdb, tile_range2d, spec.div);
				try {
					process(tile_name, spec, processor, zipOutputStream, vrtCollector, dataPath);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		} else {
			TimeBandProcessor processor = new TimeBandProcessor(rasterdb, range2d, spec.div);
			process(name, spec, processor, zipOutputStream, vrtCollector, dataPath);
		}
		
		if(!vrtCollector.isEmpty()) {
			writeVRT(vrtCollector, zipOutputStream, rasterdb.ref());
		}
		zipOutputStream.putNextEntry(new ZipEntry("readme.txt"));
		Writer zipWriter = new OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8);
		zipWriter.write("This ZIP-archive contains raster data exported from Remote Sensing Database (RSDB). https://environmentalinformatics-marburg.github.io/rsdb \n");
		zipWriter.write(" \n");
		zipWriter.write("Raster data is located at \"data\"-folder as GeoTIFF-files. (\"*.tiff\") https://www.ogc.org/standards/geotiff \n");
		zipWriter.write("Metadata is stored at \"metadata.yaml\" in YAML-format ( https://yaml.org ) with tags according to Dublin Core Metadata Initiative. https://www.dublincore.org \n");
		zipWriter.write("If raster data is tiled over multiple GeoTIFF-files, the whole tilesets can be accessed by included \"GDAL Virtual Format\" (VRT-files). (\"*.vrt\") https://gdal.org/drivers/raster/vrt.html \n");
		zipWriter.write(" \n");
		zipWriter.write("Reading the data with \"Geospatial Data Abstraction Library\" (GDAL) based applications ensures highest file format compatibility: \n");
		zipWriter.write("GDAL, https://gdal.org (library including commandline tools and bindings to programming languages) \n");
		zipWriter.write("QGIS, https://www.qgis.org (geographic information system (GIS)) \n");
		zipWriter.write("rgdal, https://cran.r-project.org/package=rgdal (binding to R programming language) \n");
		zipWriter.flush();
		zipOutputStream.closeEntry();
		zipOutputStream.finish();
		zipOutputStream.flush();
	}

	private void writeVRT(Vec<VrtEntry> vrtCollector, ZipOutputStream zipOutputStream, GeoReference ref) throws IOException {
		try {
			LinkedHashSet<Integer> timestamps = new LinkedHashSet<Integer>();
			LinkedHashMap<Integer, Band> bandMap = new LinkedHashMap<Integer, Band>();
			int xmin = Integer.MAX_VALUE;
			int ymin = Integer.MAX_VALUE;
			int xmax = Integer.MIN_VALUE;
			int ymax = Integer.MIN_VALUE;
			for(VrtEntry vrtEntry:vrtCollector) {
				for(TimeBand timeBand:vrtEntry.timeBands) {
					timestamps.add(timeBand.timestamp);
					bandMap.put(timeBand.band.index, timeBand.band);
				}
				if(vrtEntry.range.xmin < xmin) {
					xmin = vrtEntry.range.xmin;
				}
				if(vrtEntry.range.ymin < ymin) {
					ymin = vrtEntry.range.ymin;
				}
				if(vrtEntry.range.xmax > xmax) {
					xmax = vrtEntry.range.xmax;
				}
				if(vrtEntry.range.ymax > ymax) {
					ymax = vrtEntry.range.ymax;
				}
			}


			CharArrayWriterUnsync memWriter = new CharArrayWriterUnsync();

			for(int timestamp : timestamps) {
				memWriter.reset();
				XMLOutputFactory factory = XMLOutputFactory.newInstance();
				factory.setProperty("escapeCharacters", false);
				XMLStreamWriter xmlWriter = factory.createXMLStreamWriter(memWriter);
				xmlWriter = new IndentedXMLStreamWriter(xmlWriter);
				//xmlWriter.writeStartDocument(); // xml meta tag
				xmlWriter.writeStartElement("VRTDataset");
				xmlWriter.writeAttribute("rasterXSize", Integer.toString(xmax - xmin + 1));
				xmlWriter.writeAttribute("rasterYSize", Integer.toString(ymax - ymin + 1));

				if(ref.has_proj4() || ref.has_code()) {
					if(ref.has_proj4()) {
						xmlWriter.writeStartElement("SRS");
						xmlWriter.writeCharacters(ref.proj4);
						xmlWriter.writeEndElement(); // SRS
					} else if(ref.has_code()){
						xmlWriter.writeStartElement("SRS");
						xmlWriter.writeCharacters(ref.code);
						xmlWriter.writeEndElement(); // SRS
					}
					/*try {
						SpatialReference spRef = new SpatialReference("");
						if(ref.has_proj4()) {
							spRef.ImportFromProj4(ref.proj4);
						} else {
							spRef.ImportFromEPSG(ref.getEPSG(0));
						}
						String wkt = spRef.ExportToPrettyWkt();
						if(wkt != null && !wkt.isEmpty()) {
							xmlWriter.writeStartElement("SRS");
							xmlWriter.writeCharacters(wkt);
							xmlWriter.writeEndElement(); // SRS
						}
					} catch(Exception e) {
						e.printStackTrace();
						log.warn(e);
					}*/

				}

				xmlWriter.writeStartElement("GeoTransform");

				double x_upper_left_corner = ref.pixelXToGeo(xmin);
				double x_west_east_pixel_resolution = ref.pixel_size_x;
				double row_rotation = 0.0; // no rotation
				double y_upper_left_corner = ref.pixelYToGeo(ymax + 1);
				double column_rotation = 0.0; // no rotation
				double y_north_south_pixel_resolution = -ref.pixel_size_y;

				String geoTransform = "" + x_upper_left_corner + ", " + x_west_east_pixel_resolution + ", " + row_rotation + ", " + y_upper_left_corner + ", " + column_rotation + ", " + y_north_south_pixel_resolution;
				xmlWriter.writeCharacters(geoTransform);
				xmlWriter.writeEndElement(); // GeoTransform			
				int vrtBandIndex = 0;
				for(Entry<Integer, Band> e : bandMap.entrySet()) {
					int srcBandIndex = e.getKey();
					Band srcBand = e.getValue();
					vrtBandIndex++;
					xmlWriter.writeStartElement("VRTRasterBand");
					String gdalTypeName = srcBand.getPixelGdalTypeName();
					xmlWriter.writeAttribute("dataType", gdalTypeName);
					xmlWriter.writeAttribute("band", Integer.toString(vrtBandIndex));
					xmlWriter.writeAttribute("blockXSize", Integer.toString(4096));
					xmlWriter.writeAttribute("blockYSize", Integer.toString(4096));
					if(srcBand.has_visualisation()) {
						switch(srcBand.visualisation) {
						case "red":
							xmlWriter.writeStartElement("ColorInterp");
							xmlWriter.writeCharacters("Red");
							xmlWriter.writeEndElement(); // ColorInterp
							break;
						case "green":
							xmlWriter.writeStartElement("ColorInterp");
							xmlWriter.writeCharacters("Green");
							xmlWriter.writeEndElement(); // ColorInterp
							break;
						case "blue":
							xmlWriter.writeStartElement("ColorInterp");
							xmlWriter.writeCharacters("Blue");
							xmlWriter.writeEndElement(); // ColorInterp
							break;
						default:
							log.warn("unknown visualisation: " + srcBand.visualisation);
						}
					}
					switch(gdalTypeName) {
					case "Int16":
						xmlWriter.writeStartElement("NoDataValue");
						xmlWriter.writeCharacters(Short.toString(srcBand.getInt16NA()));
						xmlWriter.writeEndElement(); // NoDataValue
						break;
					case "Float32":
						// nothing: NoDataValue is NaN
						break;
					default:
						log.warn("unknown type: " + gdalTypeName);
					}
					if(srcBand.has_title()) {
						xmlWriter.writeStartElement("Description");
						xmlWriter.writeCharacters(srcBand.title);
						xmlWriter.writeEndElement(); // Description
					}

					for(VrtEntry vrtEntry:vrtCollector) {
						int sourceBandIndex = 0;
						for(TimeBand timeBand:vrtEntry.timeBands) {
							sourceBandIndex++;
							if(timeBand.timestamp == timestamp && timeBand.band.index == srcBandIndex) {
								xmlWriter.writeStartElement("SimpleSource");
								xmlWriter.writeStartElement("SourceFilename");
								xmlWriter.writeAttribute("relativeToVRT", "1");
								xmlWriter.writeCharacters(vrtEntry.filename);
								xmlWriter.writeEndElement(); // SourceFilename
								xmlWriter.writeStartElement("SourceBand");
								xmlWriter.writeCharacters(Integer.toString(sourceBandIndex));
								xmlWriter.writeEndElement(); // SourceBand
								xmlWriter.writeEmptyElement("SourceProperties");
								xmlWriter.writeAttribute("RasterXSize", Integer.toString(vrtEntry.range.getWidth()));
								xmlWriter.writeAttribute("RasterYSize", Integer.toString(vrtEntry.range.getHeight()));
								xmlWriter.writeAttribute("DataType", gdalTypeName);
								xmlWriter.writeAttribute("BlockXSize", Integer.toString(vrtEntry.range.getWidth()));
								xmlWriter.writeAttribute("BlockYSize", Integer.toString(vrtEntry.range.getHeight()));
								//xmlWriter.writeEndElement(); // SourceProperties
								xmlWriter.writeEmptyElement("SrcRect");
								xmlWriter.writeAttribute("xOff", "0");
								xmlWriter.writeAttribute("yOff", "0");
								xmlWriter.writeAttribute("xSize", Integer.toString(vrtEntry.range.getWidth()));
								xmlWriter.writeAttribute("ySize", Integer.toString(vrtEntry.range.getHeight()));
								//xmlWriter.writeEndElement(); // SrcRect
								xmlWriter.writeEmptyElement("DstRect");
								xmlWriter.writeAttribute("xOff", Integer.toString(vrtEntry.range.xmin - xmin));
								//xmlWriter.writeAttribute("yOff", Integer.toString(vrtEntry.range.ymin - ymin));
								//xmlWriter.writeAttribute("yOff", Integer.toString(0));
								xmlWriter.writeAttribute("yOff", Integer.toString(ymax - vrtEntry.range.ymax));
								xmlWriter.writeAttribute("xSize", Integer.toString(vrtEntry.range.getWidth()));
								xmlWriter.writeAttribute("ySize", Integer.toString(vrtEntry.range.getHeight()));
								//xmlWriter.writeEndElement(); // DstRect
								xmlWriter.writeEndElement(); // SimpleSource
							}
						}
					}
					xmlWriter.writeEndElement(); // VRTRasterBand
				}
				xmlWriter.writeEndElement(); // VRTDataset
				xmlWriter.writeEndDocument();
				xmlWriter.close();

				zipOutputStream.putNextEntry(new ZipEntry("raster_" + timestamp + "_experimental" + ".vrt"));
				Writer zipWriter = new OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8);
				memWriter.writeTo(zipWriter);
				zipWriter.flush();
				zipOutputStream.closeEntry();
			}
		} catch (Exception e) {
			log.warn(e);
		}
	}

	private static class VrtEntry {
		public String filename = null; // needs to be set
		public Range2d range = null; // needs to be set
		public Collection<TimeBand> timeBands;
	}

	private static String timeSliceIdToText(RasterDB rasterdb, int id) {
		TimeSlice timeSlice = rasterdb.timeMapReadonly.get(id);
		if(timeSlice != null) {
			return timeSlice.name;
		} else {
			return TimeUtil.toFileText(id);
		}

	}

	private void process(String name, Spec spec, TimeBandProcessor processor, ZipOutputStream zipOutputStream, Vec<VrtEntry> vrtCollector, String dataPath) throws IOException {		
		OutputProcessingType outputProcessingType = OutputProcessingType.IDENTITY;
		String format = "tiff";

		switch (spec.arrangement) {
		case "multiband": {
			for(int timestamp:spec.time_slice_ids) {
				String filename = dataPath + name + "__" + timeSliceIdToText(spec.rasterdb, timestamp) + ".tiff";				
				zipOutputStream.putNextEntry(new ZipEntry(filename));
				List<TimeBand> timebands = TimeBand.of(timestamp, spec.bands);
				RequestProcessorBands.processBands(processor, timebands, outputProcessingType, format, new StreamReceiver(zipOutputStream));			
				zipOutputStream.closeEntry();			
				VrtEntry vrtEntry = new VrtEntry();
				vrtEntry.filename = filename;
				vrtEntry.range = processor.getDstRange();
				vrtEntry.timeBands = timebands;
				vrtCollector.add(vrtEntry);
			}
			break;
		}
		case "timeseries": {
			for(Band band: spec.bands) {
				String filename = dataPath + name + "__band_" + band.index + ".tiff";
				zipOutputStream.putNextEntry(new ZipEntry(filename));
				List<TimeBand> timebands = spec.time_slice_ids.stream().map(timestamp -> new TimeBand(timestamp, band)).collect(Collectors.toList());
				RequestProcessorBands.processBands(processor, timebands, outputProcessingType, format, new StreamReceiver(zipOutputStream));			
				zipOutputStream.closeEntry();
				VrtEntry vrtEntry = new VrtEntry();
				vrtEntry.filename = filename;
				vrtEntry.range = processor.getDstRange();
				vrtEntry.timeBands = timebands;
				vrtCollector.add(vrtEntry);
			}
			break;
		}
		case "separate_timestamp_band": {
			for(int timestamp:spec.time_slice_ids) {
				String tfile = name + "__" + timeSliceIdToText(spec.rasterdb, timestamp);
				for(Band band: spec.bands) {
					String filename = dataPath + tfile + "__band_" + band.index + ".tiff";
					zipOutputStream.putNextEntry(new ZipEntry(filename));
					Set<TimeBand> timebands = java.util.Collections.singleton(new TimeBand(timestamp, band));
					RequestProcessorBands.processBands(processor, timebands, outputProcessingType, format, new StreamReceiver(zipOutputStream));			
					zipOutputStream.closeEntry();
					VrtEntry vrtEntry = new VrtEntry();
					vrtEntry.filename = filename;
					vrtEntry.range = processor.getDstRange();
					vrtEntry.timeBands = timebands;
					vrtCollector.add(vrtEntry);
				}
			}
			break;		
		}
		case "separate_band_timestamp": {
			for(Band band: spec.bands) {
				String bfile = name + "__band_" + band.index;
				for(int timestamp:spec.time_slice_ids) {
					String filename = dataPath + bfile + "__" + timeSliceIdToText(spec.rasterdb, timestamp) + ".tiff";
					zipOutputStream.putNextEntry(new ZipEntry(filename));
					Set<TimeBand> timebands = java.util.Collections.singleton(new TimeBand(timestamp, band));
					RequestProcessorBands.processBands(processor, timebands, outputProcessingType, format, new StreamReceiver(zipOutputStream));			
					zipOutputStream.closeEntry();
					VrtEntry vrtEntry = new VrtEntry();
					vrtEntry.filename = filename;
					vrtEntry.range = processor.getDstRange();
					vrtEntry.timeBands = timebands;
					vrtCollector.add(vrtEntry);
				}
			}
			break;		
		}
		default:
			throw new RuntimeException("unknown arrangement: "+spec.arrangement);
		}		
	}

	private void write_dublin_core(Spec spec, ZipOutputStream zipOutputStream) throws IOException {
		zipOutputStream.putNextEntry(new ZipEntry("metadata.yaml"));
		try {
			write_dublin_core_metadata(spec, zipOutputStream);
		} finally {
			zipOutputStream.closeEntry();	
		}		
	}

	private void write_dublin_core_metadata(Spec spec, OutputStream out) {
		RasterDB rasterdb = spec.rasterdb;
		Informal informal = rasterdb.informal();

		Builder properties = informal.toBuilder().properties;

		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("identifier", rasterdb.config.getName());
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
		properties.prepend("type", "raster");		
		properties.prepend("format", "image/tiff");

		if(spec.ext != null) {
			String coverage = "extent (" + spec.ext[0] + ", " + spec.ext[1] + " to " + spec.ext[2] + ", " + spec.ext[3] + ")";
			GeoReference ref = rasterdb.ref();
			if(ref.has_code() || ref.has_proj4()) {
				coverage += "   in ";
				if(ref.has_code()) {
					coverage += ref.code;
					if(ref.has_proj4()) {
						coverage += "    ";
					}
				}
				if(ref.has_proj4()) {
					coverage += "PROJ4: " + ref.proj4;
				}
			}

			if(!spec.time_slice_ids.isEmpty()) {
				Vec<String> slices = new Vec<String>();
				for(int slice : spec.time_slice_ids) {
					if(slice == 0) {
						slices.add("unspecified time");
					} else {
						TimeSlice timeSlice = rasterdb.timeMapReadonly.get(slice);
						if(timeSlice != null) {
							slices.add(timeSlice.name);
						} else {
							slices.add(TimeUtil.toText(slice));
						}
					}
				}
				if(!slices.isEmpty()) {
					coverage += "   at ";
					ReadonlyArrayIterator<String> it = slices.iterator();
					while(true) {
						coverage += it.next();
						if(!it.hasNext()) {
							break;
						}
						coverage += ", ";
					}
				}
			}

			properties.prepend("coverage", coverage);
		}

		Map<String, Object> outMap = properties.build().toSortedYaml();

		Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
		new Yaml().dump(outMap, writer);
	}
}