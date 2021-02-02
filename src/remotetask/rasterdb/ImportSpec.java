package remotetask.rasterdb;

import java.time.LocalDateTime;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import broker.Informal;
import broker.Informal.Builder;
import broker.acl.ACL;
import broker.acl.EmptyACL;
import server.api.main.APIHandler_inspect.Strategy;
import util.JsonUtil;
import util.TimeUtil;
import util.collections.vec.Vec;
import util.raster.GdalReader;

public class ImportSpec {
	private static final Logger log = LogManager.getLogger();

	public double pixel_size_x = Double.NaN;
	public double pixel_size_y = Double.NaN;
	public double rasterdb_geo_offset_x = Double.NaN;
	public double rasterdb_geo_offset_y = Double.NaN;
	public String geo_code = null;
	public String proj4 = null;
	public Vec<BandSpec> addBands = null;
	public Vec<BandSpec> bandSpecs = null;
	public Strategy strategy = null;
	public Builder inf = new Informal.Builder();
	public ACL acl = EmptyACL.ADMIN;
	public ACL acl_mod = EmptyACL.ADMIN;
	public boolean update_pyramid = true;
	public boolean update_catalog = true;
	public int generalTimestamp = 0;
	public String generalTimeSlice = null;
	public String storage_type = "TileStorage";

	public ImportSpec() {		
	}
	
	public static int parseBandDataType(String name) {
		switch(name) {
		case "short":
			return 1;
		case "float":
			return 2;
		case "int16":
			return 3;		
		default:
			log.warn("unknown band type: " + name);
			return -1;
		}	
	}

	public void parse(JSONObject specification) {
		log.info("parse spec " + specification);
		Iterator<String> specIt = specification.keys();
		while(specIt.hasNext()) {
			String specKey = specIt.next();
			//log.info("specKey " + specKey);
			switch(specKey) {
			case "strategy": {
				strategy = Strategy.of(specification.getString("strategy"));	
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
							int bandDataType = parseBandDataType(rastedb_band_data_type_name);
							if(bandDataType > 0) {
								bandSpec.rastedb_band_data_type = bandDataType;
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
							case "int16":
								bandSpec.rastedb_band_data_type = 3;
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
							String no_data_value = band.get("no_data_value").toString().trim();
							try {
								if(!no_data_value.isEmpty()) {
									String lowercase = no_data_value.toLowerCase();
									if((!lowercase.equals("nan")) && (!lowercase.equals("na"))) {
										bandSpec.no_data_value = band.getDouble("no_data_value");
									}
								}
							} catch(Exception e) {
								throw new RuntimeException("specified 'no data value' is not a number: " + no_data_value + "    " + e.getMessage());
							}
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
						case "time_slice": {
							bandSpec.timeSlice  = band.getString("time_slice");
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
			case "time_slice": {
				generalTimeSlice = specification.getString("time_slice").trim();
				break;
			}
			default: {
				log.warn("unknown spec key: "+specKey);
				//throw new RuntimeException("unknown key: "+key);
			}
			}
		}		
	}
	
	@Override
	public String toString() {
		return "ImportSpec [pixel_size_x=" + pixel_size_x + ", pixel_size_y=" + pixel_size_y
				+ ", rasterdb_geo_offset_x=" + rasterdb_geo_offset_x + ", rasterdb_geo_offset_y="
				+ rasterdb_geo_offset_y + ", geo_code=" + geo_code + ", proj4=" + proj4 + ", addBands=" + addBands
				+ ", bandSpecs=" + bandSpecs + ", strategy=" + strategy + ", inf=" + inf + ", acl=" + acl + ", acl_mod="
				+ acl_mod + ", update_pyramid=" + update_pyramid + ", update_catalog=" + update_catalog
				+ ", generalTimestamp=" + generalTimestamp + ", storage_type=" + storage_type + "]";
	}
}
