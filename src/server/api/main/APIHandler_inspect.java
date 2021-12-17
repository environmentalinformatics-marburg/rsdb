package server.api.main;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.gdal.gdal.Driver;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import broker.Broker;
import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.cell.CellType;
import rasterdb.tile.TilePixel;
import remotetask.rasterdb.ImportSpec;
import server.api.APIHandler;
import server.api.main.ChunkedUploader.ChunkedUpload;
import util.CharArrayReaderUnsync;
import util.CharArrayWriterUnsync;
import util.TimeUtil;
import util.Web;
import util.raster.GdalReader;

public class APIHandler_inspect extends APIHandler {
	

	private final ChunkedUploader chunkedUploader;

	public APIHandler_inspect(Broker broker, ChunkedUploader chunkedUploader) {
		super(broker, "inspect");
		this.chunkedUploader = chunkedUploader;
	}

	public static enum Strategy {
		CREATE,
		EXISTING_ADD,
		EXISTING_MERGE;

		public static Strategy of(String strategyText) {
			if(strategyText == null) {
				throw new RuntimeException("strategyText null");
			}
			switch(strategyText) {
			case "create":
				return Strategy.CREATE;
			case "existing_add":
				return Strategy.EXISTING_ADD;
			case "existing_merge":
				return Strategy.EXISTING_MERGE;
			default:
				throw new RuntimeException("unknown strategy: " + strategyText);
			}
		}

		public boolean isExisting() {
			return this == EXISTING_ADD || this == Strategy.EXISTING_MERGE;
		}

		public boolean isCreate() {
			return this == CREATE;
		}

		public boolean isExistingAdd() {
			return this == EXISTING_ADD;
		}

		public boolean isExistingMerge() {
			return this == EXISTING_MERGE;
		}

		@Override
		public String toString() {
			switch(this) {
			case CREATE:
				return "create";
			case EXISTING_ADD:
				return "existing_add";
			case EXISTING_MERGE:
				return "existing_merge";
			default:
				return "unknown";
			}

		}
	}

	public static ImportSpec createSpec(Path fullPath, Strategy strategy, String fileID, String rasterdbID, RasterDB rasterdb, boolean guessTimestamp, int[] layerBandIndices, String[] timesliceNames) {
		Logger.info(fullPath);
		GdalReader gdalreader = new GdalReader(fullPath.toString());
		return createSpec(gdalreader, strategy, fileID, rasterdbID, rasterdb, guessTimestamp, layerBandIndices, timesliceNames);
	}

	public static void createJSONspec(Path fullPath, Strategy strategy, String fileID, String rasterdbID, RasterDB rasterdb, boolean guessTimestamp, JSONWriter json, int[] layerBandIndices, String[] timesliceNames) {
		Logger.info(fullPath);
		GdalReader gdalreader = new GdalReader(fullPath.toString());
		createJSONspec(gdalreader, strategy, fileID, rasterdbID, rasterdb, guessTimestamp, json, layerBandIndices, timesliceNames);
	}

	public static ImportSpec createSpec(GdalReader gdalreader, Strategy strategy, String fileID, String rasterdbID, RasterDB rasterdb, boolean guessTimestamp, int[] layerBandIndices, String[] timesliceNames) {
		CharArrayWriterUnsync writer = new CharArrayWriterUnsync();
		JSONWriter json = new JSONWriter(writer);
		createJSONspec(gdalreader, strategy, fileID, rasterdbID, rasterdb, guessTimestamp, json, layerBandIndices, timesliceNames);
		ImportSpec spec = new ImportSpec();
		CharArrayReaderUnsync reader = writer.toCharArrayReaderUnsync();
		spec.parse(new JSONObject(new JSONTokener(reader)));
		return spec;
	}

	public static void createJSONspec(GdalReader gdalreader, Strategy strategy, String fileID, String rasterdbID, RasterDB rasterdb, boolean guessTimestamp, JSONWriter json, int[] layerBandIndices, String[] timesliceNames) {
		json.object();

		json.key("strategy");
		json.value(strategy.toString());

		if(fileID != null) {
			json.key("filename");
			json.value(fileID);
		}

		if(strategy.isCreate() && rasterdbID == null && fileID != null) {
			rasterdbID = fileID;
			int li = fileID.lastIndexOf('.');
			if(li > 0) {
				rasterdbID = rasterdbID.substring(0, li);
			}		
			rasterdbID = rasterdbID.replace(' ', '_');
			rasterdbID = rasterdbID.replace('\t', '_');
			rasterdbID = rasterdbID.replace('\n', '_');
			rasterdbID = rasterdbID.replace('/', '_');
			rasterdbID = rasterdbID.replace('\\', '_');
			rasterdbID = rasterdbID.replace('.', '_');
		}
		json.key("id");
		json.value(rasterdbID);

		double file_pixel_size_x = gdalreader.getPixelSize_x();
		double file_pixel_size_y = gdalreader.getPixelSize_y();

		json.key("x_range");
		json.value(gdalreader.x_range);

		json.key("y_range");
		json.value(gdalreader.y_range);
		try {
			Driver driver = gdalreader.dataset.GetDriver();
			if(driver != null) {
				String longname = driver.getLongName();
				if(longname != null) {
					json.key("gdal_driver");
					json.value(longname);
				}
			}

		} catch(Exception e) {
			Logger.warn(e);
		}

		json.key("file_pixel_size_x");
		json.value(file_pixel_size_x);
		json.key("file_pixel_size_y");
		json.value(file_pixel_size_y);

		double[] gdal_ref = gdalreader.getGeoRef();
		double easting = gdal_ref[0];
		double northing = gdal_ref[1];

		if(strategy.isCreate() || !rasterdb.ref().has_pixel_size()) {
			json.key("pixel_size_x");
			json.value(file_pixel_size_x);
			json.key("pixel_size_y");
			json.value(file_pixel_size_y);
			double corrected_northing = northing - (gdalreader.y_range * file_pixel_size_y);
			double rasterdb_geo_offset_x = easting;
			double rasterdb_geo_offset_y = corrected_northing;
			json.key("rasterdb_geo_offset_x");
			json.value(rasterdb_geo_offset_x);
			json.key("rasterdb_geo_offset_y");
			json.value(rasterdb_geo_offset_y);
		} else  {
			json.key("pixel_size_x");
			json.value(rasterdb.ref().pixel_size_x);
			json.key("pixel_size_y");
			json.value(rasterdb.ref().pixel_size_y);
			json.key("rasterdb_geo_offset_x");
			json.value(rasterdb.ref().offset_x);
			json.key("rasterdb_geo_offset_y");
			json.value(rasterdb.ref().offset_y);
		}	


		json.key("easting");
		json.value(easting);
		json.key("northing");
		json.value(northing);

		String file_geo_code = null;
		String wkt = gdalreader.getWKT();
		if(GdalReader.wktMap.containsKey(wkt)) {
			file_geo_code = GdalReader.wktMap.get(wkt);
		}
		if(file_geo_code == null) {
			file_geo_code = gdalreader.getCRS_code();
		}
		String file_proj4 = null;
		if(file_geo_code != null && GdalReader.codeMap.containsKey(file_geo_code)) {
			file_proj4 = GdalReader.codeMap.get(file_geo_code);
		}
		if(file_proj4 == null) {
			file_proj4 = gdalreader.getProj4();
		}

		json.key("file_geo_code");
		json.value(file_geo_code == null ? "" : file_geo_code);
		json.key("file_proj4");
		json.value(file_proj4 == null ? "" : file_proj4);

		if(strategy.isCreate() || !rasterdb.ref().has_proj4()) {
			json.key("proj4");
			json.value(file_proj4 == null ? "" : file_proj4);
		} else {
			json.key("proj4");
			json.value(rasterdb.ref().proj4);	
		}

		if(strategy.isCreate() || !rasterdb.ref().has_code()) {
			json.key("geo_code");
			json.value(file_geo_code == null ? "" : file_geo_code);
		} else {
			json.key("geo_code");
			json.value(rasterdb.ref().code);
		}

		json.key("bands");
		json.array();

		int rastedbIndex = 1;
		if(strategy.isExistingAdd() && !rasterdb.bandMapReadonly.isEmpty()) {
			rastedbIndex = rasterdb.bandMapReadonly.lastKey() + 1;			
		}
		int cnt = gdalreader.getRasterCount();
		if(layerBandIndices != null && layerBandIndices.length < cnt) {
			cnt = layerBandIndices.length;
		}
		
		if(timesliceNames != null) {
			if(layerBandIndices == null) {
				throw new RuntimeException("IF timesliceNames != null ==> layerBandIndices != null");				
			}
			if(timesliceNames.length != layerBandIndices.length) {
				throw new RuntimeException("timesliceNames need to be of same length as layerBandIndices");
			}
		}

		for (int i = 0; i < cnt; i++) {
			int fileIndex = i + 1;

			if(layerBandIndices != null) {
				if(layerBandIndices[i] <= 0) {
					continue;
				}
				rastedbIndex = layerBandIndices[i];
			} else if(strategy.isExistingAdd()) {
				while(rasterdb.bandMapReadonly.get(rastedbIndex) != null) {
					rastedbIndex++;
				}
			}

			json.object();

			json.key("file_band_index");
			json.value(fileIndex);

			json.key("rasterdb_band_index");
			json.value(rastedbIndex);

			org.gdal.gdal.Band gdalRasterBand = gdalreader.dataset.GetRasterBand(fileIndex);
			int gdalRasterDataType = gdalRasterBand.GetRasterDataType();

			json.key("gdal_raster_data_type");
			switch(gdalRasterDataType) {
			case GdalReader.GDAL_BYTE: {
				json.value("BYTE");	
				break;
			}
			case GdalReader.GDAL_UINT16: {
				json.value("UINT16");	
				break;
			}
			case GdalReader.GDAL_INT16: {
				json.value("INT16");	
				break;
			}
			case GdalReader.GDAL_UINT32: {
				json.value("UINT32");	
				break;
			}
			case GdalReader.GDAL_INT32: {
				json.value("INT32");	
				break;
			}
			case GdalReader.GDAL_FLOAT32: {
				json.value("FLOAT32");	
				break;
			}
			case GdalReader.GDAL_FLOAT64: {
				json.value("FLOAT64");	
				break;
			}
			default:
				json.value("unknown");				
			}

			boolean hasBandValueScale = false;
			double bandValueScale = 1d;
			try {
				Double[] bandValueScaleHolder = new Double[1];
				gdalRasterBand.GetScale(bandValueScaleHolder);
				if(bandValueScaleHolder[0] != null 
						&& bandValueScaleHolder[0].doubleValue() != 1d
						&& Double.isFinite(bandValueScaleHolder[0].doubleValue())) {
					hasBandValueScale = true;
					bandValueScale = bandValueScaleHolder[0].doubleValue();
				}			
			} catch (Exception e) {
				Logger.warn(e);
			}
			if(hasBandValueScale) {
				json.key("value_scale");
				json.value(bandValueScale);
			}

			boolean hasBandValueOffset = false;
			double bandValueOffset = 0d;
			try {
				Double[] bandValueOffsetHolder = new Double[1];
				gdalRasterBand.GetOffset(bandValueOffsetHolder);
				if(bandValueOffsetHolder[0] != null 
						&& bandValueOffsetHolder[0].doubleValue() != 0d
						&& Double.isFinite(bandValueOffsetHolder[0].doubleValue())) {
					hasBandValueOffset = true;
					bandValueOffset = bandValueOffsetHolder[0].doubleValue();
				}			
			} catch (Exception e) {
				Logger.warn(e);
			}
			if(hasBandValueOffset) {
				json.key("value_offset");
				json.value(bandValueOffset);
			}
			
			boolean hasValueDerivation = hasBandValueScale || hasBandValueOffset;

			json.key("rastedb_band_data_type");
			switch(gdalRasterDataType) {
			case GdalReader.GDAL_BYTE: {
				if(hasValueDerivation) {
					json.value("float");	
				} else {
					json.value("short");
				}
				break;
			}
			case GdalReader.GDAL_UINT16: {
				if(hasValueDerivation) {
					json.value("float");	
				} else {
					json.value("short");
				}
				break;
			}
			case GdalReader.GDAL_INT16: {
				if(hasValueDerivation) {
					json.value("float");	
				} else {
					json.value("short");
				}
				break;
			}
			case GdalReader.GDAL_UINT32: {
				json.value("float");	
				break;
			}
			case GdalReader.GDAL_INT32: {
				json.value("float");	
				break;
			}
			case GdalReader.GDAL_FLOAT32: {
				json.value("float");	
				break;
			}
			case GdalReader.GDAL_FLOAT64: {
				json.value("float");	
				break;
			}
			default:
				json.value("float");				
			}

			json.key("band_name");			
			String bandName = "band" + rastedbIndex;
			json.value(bandName);

			json.key("wavelength");			
			json.value("");

			json.key("fwhm");			
			json.value("");

			json.key("visualisation");			
			json.value("(not specified)");

			json.key("no_data_value");
			Double[] noDataValueHolder = new Double[1];
			gdalRasterBand.GetNoDataValue(noDataValueHolder);
			json.value(noDataValueHolder[0] != null ? noDataValueHolder[0].toString() : "");
			

			json.key("timestamp");
			json.value("");
			
			if(timesliceNames != null) {
				String timesliceName = timesliceNames[i];
				json.key("time_slice");
				json.value(timesliceName);
			}

			json.endObject();
			rastedbIndex++;
		}
		json.endArray();

		json.key("update_pyramid");
		json.value(true);

		json.key("update_catalog");
		json.value(true);

		json.key("title");
		if(strategy.isExisting()) {	
			json.value(rasterdb.informal().title);	
		} else {
			json.value("");	
		}

		json.key("description");
		if(strategy.isExisting()) {	
			json.value(rasterdb.informal().description);	
		} else {
			json.value("");	
		}

		json.key("acquisition_date");
		if(strategy.isExisting()) {	
			json.value(rasterdb.informal().acquisition_date);	
		} else {
			json.value("");	
		}

		json.key("corresponding_contact");
		if(strategy.isExisting()) {	
			json.value(rasterdb.informal().corresponding_contact);	
		} else {
			json.value("");	
		}

		json.key("tags");
		if(strategy.isExisting()) {			
			json.array();
			for(String tag:rasterdb.informal().tags) {
				json.value(tag);
			}
			json.endArray();
		} else {
			json.array(); json.endArray();
		}

		json.key("acl");
		if(strategy.isExisting()) {
			rasterdb.getACL().writeJSON(json);
		} else {
			json.array(); json.endArray();
		}

		json.key("acl_mod");
		if(strategy.isExisting()) {
			rasterdb.getACL_mod().writeJSON(json);
		} else {
			json.array(); json.endArray();
		}

		if(strategy.isExisting()) {
			json.key("existing_bands");
			json.array();
			for(Band band:rasterdb.bandMapReadonly.values()) {
				json.object();
				json.key("index");
				json.value(band.index);
				json.key("type");
				switch(band.type) {
				case TilePixel.TYPE_SHORT:
					json.value("short");
					break;
				case TilePixel.TYPE_FLOAT:
					json.value("float");
					break;
				case CellType.INT16:
					json.value("int16");
					break;
				default:
					throw new RuntimeException("unknown band type: "+ band.type);
				}
				json.key("name");
				json.value(band.title);
				json.key("wavelength");
				json.value(Double.isFinite(band.wavelength) ? band.wavelength : "");
				json.key("fwhm");
				json.value(Double.isFinite(band.fwhm) ? band.fwhm : "");
				json.key("visualisation");
				json.value(band.visualisation == null ? "(not specified)" : band.visualisation);
				json.endObject();
			}
			json.endArray();
		}


		String timestampText = "";
		if(guessTimestamp) {
			try {
				Pattern pattern = Pattern.compile("_(\\d{4}_\\d{2}_\\d{2})_");
				Matcher matcher = pattern.matcher(fileID);
				if(matcher.find()) {
					String tText = matcher.group(1);
					Logger.info("tText " + tText);
					LocalDate date = LocalDate.parse(tText, TimeUtil.DATE_UNDERSCORE);
					timestampText = date.toString();
				}
			} catch (Exception e) {
				Logger.warn(e);
			}
		}

		json.key("timestamp");
		json.value(timestampText);

		json.endObject();			
	}



	@Override
	protected void handle(String target, Request request, Response response) throws IOException {		

		String fileID = request.getParameter("filename");
		if(fileID == null) {
			throw new RuntimeException("no filename");
		}

		Strategy strategy = Strategy.of(request.getParameter("strategy"));

		boolean guessTimestamp = Web.getBoolean(request, "guess_timestamp", false);

		String id = request.getParameter("rasterdb");
		if(strategy.isExisting()) {
			if(id == null) {
				throw new RuntimeException("missing rasterdb parameter");
			}
		} else {
			if(id != null) {
				throw new RuntimeException("rasterdb parameter not applicable for create strategy");
			}
		}
		RasterDB rasterdb = null;
		if(strategy.isExisting()) {
			rasterdb = broker.getRasterdb(id);
		}

		ChunkedUpload chunkedUpload = chunkedUploader.map.get(fileID);
		Path path;
		if(chunkedUpload == null) {
			//throw new RuntimeException("file not found");
			Logger.warn("old session ? ");
			path = Paths.get("temp", fileID);
		} else {
			path = chunkedUpload.path;
		}

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("specification");
		createJSONspec(path, strategy, fileID, id, rasterdb, guessTimestamp, json, null, null);	
		json.endObject();
	}
}


