package util.raster;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


import org.tinylog.Logger;

public class EnviHdr {
	
	
	private static final Pattern COMMA_WHITESPACE_PATTERN = Pattern.compile(",\\s");

	public final String filename;

	public int samples=-1;
	public int lines=-1;
	public int bands=-1;
	public long header_offset=-1;
	public int data_type=-1;
	public int byte_order=-1;
	public String interleave=null;

	public String mapinfo_projectionName;//		Projection name
	public double mapinfo_referenceX;//		Reference (tie point) pixel x location (in file coordinates)
	public double mapinfo_referenceY;//		Reference (tie point) pixel y location (in file coordinates)
	public double mapinfo_pixelEasting;//		Pixel easting
	public double mapinfo_pixelNorthing;//		Pixel northing
	public double mapinfo_pixelSizeX;//		x pixel size
	public double mapinfo_pixelSizeY;//		y pixel size
	public String mapinfo_projectionZone;//		Projection zone (UTM only)
	public String mapinfo_northOrSouth;//		North or South (UTM only)
	public String mapinfo_datum;//		Datum
	public String mapinfo_units;//		Units
	
	public int[] wavelength_picometre;
	public int[] fwhm_picometre;

	//private Map<String,String> map = new TreeMap<String,String>();
	private Map<String,String> map = new LinkedHashMap<String,String>(); // preserve order of keys

	private double wavelength_picometre_factor = 1000_000d; // defaults to micrometre in header




	public EnviHdr(String filename) throws IOException {
		this.filename = filename;
		readFile();
		parseMap();
		
		if(bands<1) {
			bands = 1;
		}
		
		if(wavelength_picometre==null) {
			wavelength_picometre = new int[bands];
			for (int i = 0; i < bands; i++) {
				wavelength_picometre[i] = 0;
			}
		}
		
		if(fwhm_picometre==null) {
			fwhm_picometre = new int[bands];
			int avg = 1;
			
			int sum=0;
			if(bands>1) {
				for (int i = 1; i < bands; i++) {
					sum += Math.abs(wavelength_picometre[i] - wavelength_picometre[i-1]);
				}
				avg = sum/bands;
			}
			
			for (int i = 0; i < bands; i++) {
				fwhm_picometre[i] = avg;
			}
		}
		
	}

	private void readFile() throws IOException {
		List<String> list = Files.readAllLines(Paths.get(filename));
		if(!list.get(0).equals("ENVI")) {
			throw new RuntimeException("no ENVI");
		}

		boolean braceleft = false;

		String multilineKey = null;
		ArrayList<String> multilineCollector = new ArrayList<String>();

		for(String line:list) {
			if(braceleft) {//skip

				boolean braceRigth = line.indexOf('}')>=0;
				if(braceRigth) {
					braceleft = false;
					String value = line.substring(0, line.indexOf('}')).trim();
					if(!value.isEmpty()) {
						multilineCollector.add(value);
					}
					if(!multilineCollector.isEmpty()) {
						//Logger.info("multiline "+multilineKey+"  "+multilineCollector);
						String s = multilineCollector.get(0);
						for (int i = 1; i < multilineCollector.size(); i++) {
							s += '\n'+multilineCollector.get(i);							
						}
						if(map.containsKey(multilineKey)) {
							Logger.warn("overwriting duplicate key "+multilineKey);
						}
						map.put(multilineKey, s);						
					} else {
						Logger.warn("empty multiline key "+multilineKey);
					}
					multilineKey = null;
					multilineCollector.clear();
				} else {
					String value = line.trim();
					if(!value.isEmpty()) {
						multilineCollector.add(value);
					}
				}
				continue;
			}
			int sepIndex = line.indexOf('=');
			if(sepIndex<0) {//skip
				continue;
			}
			String key = line.substring(0, sepIndex).trim();
			String value = line.substring(sepIndex+1).trim();

			braceleft = value.indexOf('{')>=0;
			if(braceleft) {//skip
				boolean braceRigth = value.indexOf('}')>=0;
				if(braceRigth) {
					braceleft = false;
					value = value.substring(value.indexOf('{')+1, value.indexOf('}')).trim();
				} else { // multiline values
					//Logger.warn("skip multiline value for key: "+key);
					multilineCollector.clear();
					multilineKey = key;
					value = value.substring(value.indexOf('{')+1).trim();
					if(!value.isEmpty()) {
						multilineCollector.add(value);
					}
					continue;
				}
			}

			//System.out.println(key+"\t\t"+value);
			//System.out.println("public int "+key+"=-1;");

			if(map.containsKey(key)) {
				Logger.warn("overwriting duplicate key "+key);
			}
			if(!value.isEmpty()) {
				map.put(key, value);	
			} else {
				Logger.warn("empty key "+key);
			}
		}
	}

	private void parseMap() {
		for(String key:map.keySet()) {
			String value = map.get(key);
			switch(key) {
			case "samples":
				samples = Integer.parseInt(value);
				break;
			case "lines":
				lines = Integer.parseInt(value);
				break;
			case "bands":
				bands = Integer.parseInt(value);
				break;
			case "header offset":
				header_offset = Long.parseLong(value);
				break;
			case "data type":
				data_type = Integer.parseInt(value);
				break;
			case "byte order":
				byte_order = Integer.parseInt(value);
				break;
			case "interleave":
				interleave = value;
				break;
			case "map info":
				parse_map_info(COMMA_WHITESPACE_PATTERN.split(value));
				break;
			case "band names":
				break;
			case "wavelength":
				wavelength_picometre = Arrays.stream(COMMA_WHITESPACE_PATTERN.split(value)).mapToDouble(Double::parseDouble).mapToInt(v->(int)(v*wavelength_picometre_factor)).toArray();
				break;
			case "file type":
				break;
			case "x start":
				break;
			case "y start":
				break;
			case "z plot titles":
				break;
			case "default bands":
				break;
			case "description":
				break;
			case "fwhm":
				fwhm_picometre = Arrays.stream(COMMA_WHITESPACE_PATTERN.split(value)).mapToDouble(Double::parseDouble).mapToInt(v->(int)(v*wavelength_picometre_factor)).toArray();
				break;
			case "coordinate system string":
				break;
			case "sensor type":
				break;
			case "wavelength units":
				if(value.equalsIgnoreCase("Nanometers")) {
					wavelength_picometre_factor = 1000d;
				} else if(value.equalsIgnoreCase("Micrometers")) {
					wavelength_picometre_factor = 1000_000d;
				} else {
					Logger.info("unknown unit "+value+"   --> default to micrometers");
				}
				break;
			default:
				Logger.info("unknown key: "+key+"   "+value);
			}
		}
	}




	private void parse_map_info(String[] entries) {
		if(entries.length!=11) {
			throw new RuntimeException("invalid map_info");
		}
		mapinfo_projectionName = entries[0];//		Projection name
		mapinfo_referenceX = Double.parseDouble(entries[1]);//		Reference (tie point) pixel x location (in file coordinates)
		mapinfo_referenceY = Double.parseDouble(entries[2]);//		Reference (tie point) pixel y location (in file coordinates)
		mapinfo_pixelEasting = Double.parseDouble(entries[3]);//		Pixel easting
		mapinfo_pixelNorthing = Double.parseDouble(entries[4]);//		Pixel northing
		mapinfo_pixelSizeX = Double.parseDouble(entries[5]);//		x pixel size
		mapinfo_pixelSizeY = Double.parseDouble(entries[6]);//		y pixel size
		mapinfo_projectionZone = entries[7];//		Projection zone (UTM only)
		mapinfo_northOrSouth = entries[8];//		North or South (UTM only)
		mapinfo_datum = entries[9];//		Datum
		mapinfo_units = entries[10];//		Units

		//Logger.info(Arrays.toString(entries));
	}

	public static void main(String[] args) throws IOException {
		EnviHdr hdr = new EnviHdr("c:/hslvl3/20150327/ta2015_20150327_01_VNIR_1600_SN0003_FOVx2_raw_rad_geo_atm.hdr");
		
		/*for(Entry<String, String> e:hdr.map.entrySet()) {
			System.out.println(e+"\n");
		}*/
		
		System.out.println(Arrays.toString(hdr.wavelength_picometre));
		
	}

}
