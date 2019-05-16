package server.api.main;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import broker.Broker;
import broker.Informal;
import broker.Informal.Builder;
import broker.acl.ACL;
import broker.acl.EmptyACL;
import rasterdb.Band;
import rasterdb.RasterDB;
import remotetask.RemoteTaskExecutor;
import remotetask.RemoteTasks;
import remotetask.rasterdb.ImportRemoteTask;
import server.api.APIHandler;
import server.api.main.APIHandler_inspect.Strategy;
import server.api.main.ChunkedUploader.ChunkedUpload;
import util.JsonUtil;
import util.TimeUtil;
import util.Web;
import util.collections.vec.Vec;
import util.raster.GdalReader;

public class APIHandler_import extends APIHandler {
	private static final Logger log = LogManager.getLogger();

	private final ChunkedUploader chunkedUploader;

	public APIHandler_import(Broker broker, ChunkedUploader chunkedUploader) {
		super(broker, "import");
		this.chunkedUploader = chunkedUploader;
	}

	public static class BandSpec {
		public String band_name = null;
		public int file_band_index = -1;
		public int rasterdb_band_index = -1;
		public int rastedb_band_data_type = -1;
		public int gdal_raster_data_type = -1;
		public double wavelength = Double.NaN;
		public double fwhm = Double.NaN;
		public String visualisation = null;
		public Double no_data_value = null;
		public boolean import_band = true;
		public int timestamp = -1;
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {

		JSONObject json = new JSONObject(Web.requestContentToString(request));
		JSONObject specification = json.getJSONObject("specification");
		log.info(specification);


		String filename = null;
		String id = null;
		double pixel_size_x = Double.NaN;
		double pixel_size_y = Double.NaN;
		double rasterdb_geo_offset_x = Double.NaN;
		double rasterdb_geo_offset_y = Double.NaN;
		String geo_code = null;
		String proj4 = null;
		Vec<BandSpec> addBands = null;
		Vec<BandSpec> bandSpecs = null;
		boolean update_pyramid = true;
		boolean update_catalog = true;
		Builder inf = new Informal.Builder();
		ACL acl = EmptyACL.ADMIN;
		ACL acl_mod = EmptyACL.ADMIN;

		Strategy strategy = Strategy.of(specification.getString("strategy"));

		int generalTimestamp = 0;		

		Iterator<String> specIt = specification.keys();
		while(specIt.hasNext()) {
			String specKey = specIt.next();
			switch(specKey) {
			case "filename": {
				filename = specification.getString("filename");
				break;
			}
			case "id": {
				id = specification.getString("id");
				break;
			}
			case "pixel_size_x": {
				pixel_size_x = specification.getDouble("pixel_size_x");
				break;
			}
			case "pixel_size_y": {
				pixel_size_y = specification.getDouble("pixel_size_y");
				break;
			}
			case "rasterdb_geo_offset_x": {
				rasterdb_geo_offset_x = specification.getDouble("rasterdb_geo_offset_x");
				break;
			}
			case "rasterdb_geo_offset_y": {
				rasterdb_geo_offset_y = specification.getDouble("rasterdb_geo_offset_y");
				break;
			}
			case "geo_code": {
				geo_code = specification.getString("geo_code");
				break;
			}
			case "proj4": {
				proj4 = specification.getString("proj4");
				break;
			}
			case "addBands": {
				JSONArray bands = specification.getJSONArray("addBands");
				int bandLen = bands.length();
				addBands = new Vec<BandSpec>(bandLen);
				for (int i = 0; i < bandLen; i++) {
					BandSpec bandSpec = new BandSpec();
					JSONObject band = bands.getJSONObject(i);
					Iterator<String> bandIt = band.keys();
					while(bandIt.hasNext()) {
						String bandKey = bandIt.next();
						switch(bandKey) {
						case "rastedb_band_data_type": {
							String rastedb_band_data_type_name = band.getString("rastedb_band_data_type");
							switch(rastedb_band_data_type_name) {
							case "short":
								bandSpec.rastedb_band_data_type = 1;
								break;
							case "float":
								bandSpec.rastedb_band_data_type = 2;
								break;
							default:
								log.warn("unknown band type: " + rastedb_band_data_type_name);
							}
							break;
						}
						case "band_name": {
							bandSpec.band_name  = band.getString("band_name");
							break;
						}
						case "rasterdb_band_index": {
							bandSpec.rasterdb_band_index  = band.getInt("rasterdb_band_index");
							break;
						}
						case "wavelength": {							
							double v = band.get("wavelength").toString().trim().isEmpty() ? Double.NaN : band.getDouble("wavelength");
							bandSpec.wavelength = Double.isFinite(v) ? v : Double.NaN;
							break;
						}
						case "fwhm": {
							double v = band.get("fwhm").toString().trim().isEmpty() ? Double.NaN : band.getDouble("fwhm");
							bandSpec.fwhm = Double.isFinite(v) ? v : Double.NaN;
							break;
						}
						case "visualisation": {
							String v = band.getString("visualisation");
							bandSpec.visualisation  = v.isEmpty() || v.equals("(not specified)") ? null : v;
							break;
						}
						default: {					
							log.warn("unknown band key: "+bandKey);
							//throw new RuntimeException("unknown key: "+key);
						}
						}
					}
					if(bandSpec.rastedb_band_data_type < 0) {
						throw new RuntimeException("no rastedb_band_data_type");
					}
					if(bandSpec.band_name == null) {
						throw new RuntimeException("no band_name");
					}
					if(bandSpec.rasterdb_band_index < 0) {
						throw new RuntimeException("no rasterdb_band_index");
					}
					addBands.add(bandSpec);
				}
				break;
			}
			case "bands": {
				JSONArray bands = specification.getJSONArray("bands");
				int bandLen = bands.length();
				bandSpecs = new Vec<BandSpec>(bandLen);
				for (int i = 0; i < bandLen; i++) {
					BandSpec bandSpec = new BandSpec();
					JSONObject band = bands.getJSONObject(i);
					Iterator<String> bandIt = band.keys();
					while(bandIt.hasNext()) {
						String bandKey = bandIt.next();
						switch(bandKey) {
						case "gdal_raster_data_type": {
							String gdal_raster_data_type_name = band.getString("gdal_raster_data_type");
							bandSpec.gdal_raster_data_type = GdalReader.gdalBandDataTypeToNumber(gdal_raster_data_type_name); 
							break;
						}
						case "rastedb_band_data_type": {
							String rastedb_band_data_type_name = band.getString("rastedb_band_data_type");
							switch(rastedb_band_data_type_name) {
							case "short":
								bandSpec.rastedb_band_data_type = 1;
								break;
							case "float":
								bandSpec.rastedb_band_data_type = 2;
								break;
							default:
								log.warn("unknown band type: " + rastedb_band_data_type_name);
							}
							break;
						}
						case "band_name": {
							bandSpec.band_name  = band.getString("band_name");
							break;
						}
						case "file_band_index": {
							bandSpec.file_band_index   = band.getInt("file_band_index");
							break;
						}
						case "rasterdb_band_index": {
							if(band.get("rasterdb_band_index").toString().isEmpty()) {
								bandSpec.import_band = false;
								bandSpec.rasterdb_band_index  = -1;
							} else {
								bandSpec.rasterdb_band_index  = band.getInt("rasterdb_band_index");
							}
							break;
						}
						case "wavelength": {							
							double v = band.get("wavelength").toString().trim().isEmpty() ? Double.NaN : band.getDouble("wavelength");
							bandSpec.wavelength = Double.isFinite(v) ? v : Double.NaN;
							break;
						}
						case "fwhm": {
							double v = band.get("fwhm").toString().trim().isEmpty() ? Double.NaN : band.getDouble("fwhm");
							bandSpec.fwhm = Double.isFinite(v) ? v : Double.NaN;
							break;
						}
						case "visualisation": {
							String v = band.getString("visualisation");
							bandSpec.visualisation  = v.isEmpty() || v.equals("(not specified)") ? null : v;
							break;
						}
						case "no_data_value": {
							Double v = band.get("no_data_value").toString().trim().isEmpty() ? null : band.getDouble("no_data_value");
							bandSpec.no_data_value = v;
							break;
						}
						case "timestamp": {
							String timestampText = band.get("timestamp").toString().trim();
							if(!timestampText.isEmpty()) {					
								LocalDateTime[] timerange = TimeUtil.getDateTimeRange(timestampText);
								bandSpec.timestamp = TimeUtil.toTimestamp(timerange[0]);
							}
							break;
						}						
						default: {					
							log.warn("unknown band key: "+bandKey);
							//throw new RuntimeException("unknown key: "+key);
						}
						}
					}
					if(bandSpec.import_band) {
						if(bandSpec.gdal_raster_data_type < 0) {
							throw new RuntimeException("no gdal_raster_data_type");
						}
						if(bandSpec.rastedb_band_data_type < 0) {
							throw new RuntimeException("no rastedb_band_data_type");
						}
						if(bandSpec.band_name == null) {
							throw new RuntimeException("no band_name");
						}
						if(bandSpec.file_band_index < 0) {
							throw new RuntimeException("no file_band_index");
						}
						if(bandSpec.rasterdb_band_index < 0) {
							throw new RuntimeException("no rasterdb_band_index");
						}
						bandSpecs.add(bandSpec);
					}
				}
				break;
			}
			case "update_pyramid": {
				update_pyramid = specification.getBoolean("update_pyramid");
				break;
			}
			case "update_catalog": {
				update_catalog = specification.getBoolean("update_catalog");
				break;
			}
			case "title": {
				inf.title = specification.getString("title");
				break;
			}
			case "description": {
				inf.description = specification.getString("description");
				break;
			}
			case "acquisition_date": {
				inf.acquisition_date = specification.getString("acquisition_date");
				break;
			}
			case "corresponding_contact": {
				inf.corresponding_contact = specification.getString("corresponding_contact");
				break;
			}
			case "tags": {
				inf.setTags(JsonUtil.optStringArray(specification, "tags"));
				break;
			}
			case "acl": {
				acl = ACL.of(JsonUtil.optStringArray(specification, "acl"));
				break;
			}
			case "acl_mod": {
				acl_mod = ACL.of(JsonUtil.optStringArray(specification, "acl_mod"));
				break;
			}
			case "timestamp": {
				String timestampText = specification.getString("timestamp").trim();
				if(!timestampText.isEmpty()) {					
					LocalDateTime[] timerange = TimeUtil.getDateTimeRange(timestampText);
					generalTimestamp = TimeUtil.toTimestamp(timerange[0]);
				}
				break;
			}
			default: {
				log.warn("unknown spec key: "+specKey);
				//throw new RuntimeException("unknown key: "+key);
			}
			}
		}

		if(filename == null) {
			throw new RuntimeException("no filename");
		}
		if(id == null) {
			throw new RuntimeException("no id");
		}
		if(!Double.isFinite(pixel_size_x)) {
			throw new RuntimeException("no pixel_size_x");
		}
		if(!Double.isFinite(pixel_size_y)) {
			throw new RuntimeException("no pixel_size_y");
		}
		if(!Double.isFinite(rasterdb_geo_offset_x)) {
			throw new RuntimeException("no rasterdb_geo_offset_x");
		}
		if(!Double.isFinite(rasterdb_geo_offset_y)) {
			throw new RuntimeException("no rasterdb_geo_offset_y");
		}
		if(geo_code == null) {
			throw new RuntimeException("no geo_code");
		}
		if(proj4 == null) {
			throw new RuntimeException("no proj4");
		}
		if(bandSpecs == null) {
			throw new RuntimeException("no bandSpecs");
		}

		ChunkedUpload chunkedUpload = chunkedUploader.map.get(filename);
		Path path;
		if(chunkedUpload == null) {
			//throw new RuntimeException("file not found");
			log.warn("old session ? ");
			path = Paths.get("temp", filename);
		} else {
			path = chunkedUpload.path;
		}

		log.info(path);
		GdalReader gdalreader = new GdalReader(path.toString());

		UserIdentity userIdentity = Web.getUserIdentity(request);
		if(!EmptyACL.ADMIN.isAllowed(userIdentity) && broker.hasRasterdb(id)) {
			log.info("not admin");
			RasterDB rasterdb = broker.getRasterdb(id);
			rasterdb.checkMod(userIdentity);
			log.info("allowed");
		}

		RasterDB rasterdb;
		if(strategy.isCreate()) {
			rasterdb = broker.createRasterdb(id);
			rasterdb.setPixelSize(pixel_size_x, pixel_size_y, rasterdb_geo_offset_x, rasterdb_geo_offset_y);
			rasterdb.setCode(geo_code);
			rasterdb.setProj4(proj4);

			rasterdb.setInformal(inf.build());

			rasterdb.setACL(acl);
			rasterdb.setACL_mod(acl_mod);
		} else {
			rasterdb = broker.getRasterdb(id);
		}

		if(addBands != null) {
			for(BandSpec bandSpec:addBands) {
				if(!rasterdb.bandMap.containsKey(bandSpec.rasterdb_band_index)) {
					Band band = Band.ofSpectralBand(bandSpec.rastedb_band_data_type, bandSpec.rasterdb_band_index, bandSpec.wavelength, bandSpec.fwhm, bandSpec.band_name, bandSpec.visualisation);
					rasterdb.setBand(band);
				}
			}
		}

		if(bandSpecs != null) {
			for(BandSpec bandSpec:bandSpecs) {
				if(bandSpec.import_band) {
					if(!rasterdb.bandMap.containsKey(bandSpec.rasterdb_band_index)) {
						Band band = Band.ofSpectralBand(bandSpec.rastedb_band_data_type, bandSpec.rasterdb_band_index, bandSpec.wavelength, bandSpec.fwhm, bandSpec.band_name, bandSpec.visualisation);
						rasterdb.setBand(band);
					}
					Band refBand = rasterdb.bandMap.get(bandSpec.rasterdb_band_index); // correct potential user spec errors
					bandSpec.band_name = refBand.title;
					bandSpec.rastedb_band_data_type = refBand.type;
					bandSpec.wavelength = refBand.wavelength;
					bandSpec.fwhm = refBand.fwhm;
					bandSpec.visualisation = refBand.visualisation;
				}
			}
		}

		ImportRemoteTask importRemoteTask = new ImportRemoteTask(broker, rasterdb, gdalreader, bandSpecs, pixel_size_y, update_pyramid, update_catalog, generalTimestamp);
		RemoteTaskExecutor.insertToExecute(importRemoteTask);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter res = new JSONWriter(response.getWriter());
		res.object();
		res.key("remote_task");
		res.object();	
		res.key("id");
		res.value(importRemoteTask.id);
		res.endObject();
		res.endObject();		
	}
}
