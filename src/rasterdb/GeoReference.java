package rasterdb;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


import org.tinylog.Logger;

import util.Range2d;
import util.yaml.YamlMap;

public class GeoReference {
	

	public static final double NO_PIXEL_SIZE = Double.NaN;
	public static final double NO_OFFSET = 0;
	public static final String NO_CODE = "";
	public static final boolean NOT_TRANSPOSED = false;
	public static final String NO_PROJ4 = "";

	public static final GeoReference EMPTY_DEFAULT = new GeoReference(NO_PIXEL_SIZE, NO_PIXEL_SIZE, NO_OFFSET,
			NO_OFFSET, NO_CODE, NOT_TRANSPOSED, NO_PROJ4);

	public static final String PROPERTY_PIXEL_SIZE = "pixel_size";
	public static final String PROPERTY_OFFSET = "offset";
	public static final String PROPERTY_CODE = "code";
	public static final String PROPERTY_WMS_TRANSPOSED = "wms_transposed";
	public static final String PROPERTY_PROJ4 = "proj4";

	public final double pixel_size_x;
	public final double pixel_size_y;
	public final double offset_x;
	public final double offset_y;
	public final String code;
	public final String proj4;
	public final boolean wms_transposed;

	public static final Set<String> code_wms_transposed = Collections
			.unmodifiableSet(new HashSet<String>(Arrays.asList(
					"EPSG:4326",
					"EPSG:3044"
					)));

	protected GeoReference(double pixel_size_x, double pixel_size_y, double offset_x, double offset_y, String code,
			boolean wms_transposed, String proj4) {
		this.pixel_size_x = pixel_size_x;
		this.pixel_size_y = pixel_size_y;
		this.offset_x = offset_x;
		this.offset_y = offset_y;
		this.code = code;
		this.wms_transposed = wms_transposed;
		this.proj4 = proj4;
	}

	public static GeoReference of(double pixel_size_x, double pixel_size_y, double offset_x, double offset_y,
			String code, boolean wms_transposed, String proj4) {
		if (pixel_size_x == NO_PIXEL_SIZE && pixel_size_y == NO_PIXEL_SIZE && offset_x == NO_OFFSET
				&& offset_y == NO_OFFSET && code.equals(NO_CODE) && wms_transposed == NOT_TRANSPOSED
				&& proj4.equals(NO_PROJ4)) {
			return EMPTY_DEFAULT;
		}
		return new GeoReference(pixel_size_x, pixel_size_y, offset_x, offset_y, code, wms_transposed, proj4);
	}

	/**
	 * set wms_transposed based on epsg
	 * 
	 * @param pixel_size
	 * @param epsg
	 * @return
	 */
	/*
	 * public static GeoReference of(double pixel_size, int epsg) { boolean
	 * wms_transposed = code_wms_transposed.contains(epsg); if(pixel_size ==
	 * NO_PIXEL_SIZE && epsg == NO_EPSG && wms_transposed == false) { return
	 * EMPTY_DEFAULT; } return new GeoReference(pixel_size, epsg, wms_transposed,
	 * NO_PROJ4); }
	 */

	public GeoReference withPixelSize(double pixel_size_x, double pixel_size_y, double offset_x, double offset_y) {
		if (this.pixel_size_x == pixel_size_x && this.pixel_size_y == pixel_size_y && this.offset_x == offset_x
				&& this.offset_y == offset_y) {
			return this;
		}
		return GeoReference.of(pixel_size_x, pixel_size_y, offset_x, offset_y, this.code, this.wms_transposed,
				this.proj4);
	}

	public GeoReference withCode(String code, boolean wms_transposed) {
		if (this.code == code && this.wms_transposed == wms_transposed) {
			return this;
		}
		return GeoReference.of(this.pixel_size_x, this.pixel_size_y, this.offset_x, this.offset_y, code, wms_transposed,
				this.proj4);
	}

	public GeoReference withProj4(String proj4) {
		if (this.proj4 == proj4) {
			return this;
		}
		return GeoReference.of(this.pixel_size_x, this.pixel_size_y, this.offset_x, this.offset_y, this.code,
				this.wms_transposed, proj4);
	}

	public int geoXToPixel(double geoX) {
		return (int) ((geoX - offset_x) / pixel_size_x);
	}

	public int geoYToPixel(double geoY) {
		return (int) ((geoY - offset_y) / pixel_size_y);
	}

	public double pixelXToGeo(int pixelX) {
		return (pixelX * pixel_size_x) + offset_x;
	}
	
	public double pixelYToGeo(int pixelY) {
		return (pixelY * pixel_size_y) + offset_y;
	}
	
	public double pixelXToGeoBorder(int pixelX) {
		return pixelXToGeo(pixelX + 1);
	}

	public double pixelYToGeoBorder(int pixelY) {
		return pixelYToGeo(pixelY + 1);
	}
	
	public double pixelXToGeoUpper(int pixelX) {
		return Math.nextDown(pixelXToGeoBorder(pixelX));
	}
	
	public double pixelYToGeoUpper(int pixelY) {
		return Math.nextDown(pixelYToGeoBorder(pixelY));
	}

	public double pixelXToGeo(double pixelX) {
		return (pixelX * pixel_size_x) + offset_x;
	}

	public double pixelYToGeo(double pixelY) {
		return (pixelY * pixel_size_y) + offset_y;
	}

	public Map<String, Object> toYaml() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		if (has_pixel_size()) {
			LinkedHashMap<String, Object> pmap = new LinkedHashMap<String, Object>();
			pmap.put("x", pixel_size_x);
			pmap.put("y", pixel_size_y);
			map.put(PROPERTY_PIXEL_SIZE, pmap);
			LinkedHashMap<String, Object> omap = new LinkedHashMap<String, Object>();
			omap.put("x", offset_x);
			omap.put("y", offset_y);
			map.put(PROPERTY_OFFSET, omap);
		}
		if (has_code()) {
			map.put(PROPERTY_CODE, code);
		}
		if (wms_transposed) {
			map.put(PROPERTY_WMS_TRANSPOSED, wms_transposed);
		}
		if (has_proj4()) {
			map.put(PROPERTY_PROJ4, proj4);
		}
		return map;
	}

	public static GeoReference ofYaml(YamlMap yamlMap) {
		double pixel_size_x = NO_PIXEL_SIZE;
		double pixel_size_y = NO_PIXEL_SIZE;
		if (yamlMap.contains(PROPERTY_PIXEL_SIZE)) {
			Object obj = yamlMap.getObject(PROPERTY_PIXEL_SIZE);
			if (obj instanceof Number) {
				double pixel_size = yamlMap.optDouble(PROPERTY_PIXEL_SIZE, NO_PIXEL_SIZE);
				pixel_size_x = pixel_size;
				pixel_size_y = pixel_size;
			} else {
				YamlMap pmap = yamlMap.getMap(PROPERTY_PIXEL_SIZE);
				pixel_size_x = pmap.optDouble("x", NO_PIXEL_SIZE);
				pixel_size_y = pmap.optDouble("y", NO_PIXEL_SIZE);
			}
		}
		YamlMap omap = yamlMap.optMap(PROPERTY_OFFSET);
		double offset_x = omap.optDouble("x", NO_OFFSET);
		double offset_y = omap.optDouble("y", NO_OFFSET);
		String code = yamlMap.optString(PROPERTY_CODE, NO_CODE);
		boolean wms_transposed = yamlMap.optBoolean(PROPERTY_WMS_TRANSPOSED, false);
		String proj4 = yamlMap.optString(PROPERTY_PROJ4, NO_PROJ4);
		return GeoReference.of(pixel_size_x, pixel_size_y, offset_x, offset_y, code, wms_transposed, proj4);
	}

	public boolean has_pixel_size() {
		return Double.isFinite(pixel_size_x) && Double.isFinite(pixel_size_y);
	}

	public boolean has_code() {
		return !code.equals(NO_CODE);
	}

	public int getEPSG(int missing) {
		if (has_code()) {
			String epsgText = code.toLowerCase();
			if (epsgText.startsWith("epsg:")) {
				String codeText = epsgText.substring(5);
				try {
					return Integer.parseUnsignedInt(codeText.trim());
				} catch (Exception e) {
					Logger.warn(e);
					return missing;
				}
			}
			return missing;
		} else {
			return missing;
		}

	}

	public String optCode(String code_default) {
		return has_code() ? code : code_default;
	}

	public boolean has_proj4() {
		return !proj4.equals(NO_PROJ4);
	}

	public Range2d parseBboxToRange2d(String[] bbox, boolean transposed) {
		if (transposed) {
			return bboxToRange2d(Double.parseDouble(bbox[1]), Double.parseDouble(bbox[0]), Double.parseDouble(bbox[3]),
					Double.parseDouble(bbox[2]));
		} else {
			return bboxToRange2d(Double.parseDouble(bbox[0]), Double.parseDouble(bbox[1]), Double.parseDouble(bbox[2]),
					Double.parseDouble(bbox[3]));
		}
	}

	public Range2d parseExtentToRange2d(String[] ext) {
		return bboxToRange2d(Double.parseDouble(ext[0]), Double.parseDouble(ext[1]), Double.parseDouble(ext[2]),
				Double.parseDouble(ext[3]));
	}

	public Range2d bboxToRange2d(double[] bbox) {
		return bboxToRange2d(bbox[0], bbox[1], bbox[2], bbox[3]);
	}

	public Range2d bboxToRange2d(double xmin, double ymin, double xmax, double ymax) {
		return new Range2d(geoXToPixel(xmin), geoYToPixel(ymin), geoXToPixel(Math.nextDown(xmax)), geoYToPixel(Math.nextDown(ymax)));
	}

	@Override
	public String toString() {
		return "GeoReference [pixel_size_x=" + pixel_size_x + ", pixel_size_y=" + pixel_size_y + ", offset_x="
				+ offset_x + ", offset_y=" + offset_y + ", code=" + code + ", proj4=" + proj4 + ", wms_transposed="
				+ wms_transposed + "]";
	}

	/**
	 * 
	 * @return nullable
	 */
	public String getProjectionTitle() {
		if(has_proj4()) {
			try {
				if(proj4.contains("+proj=utm")) {
					String prefix = "+zone=";
					int i = proj4.indexOf(prefix);
					if(i>=0) {
						int pos = i+prefix.length();
						String zoneName = proj4.substring(pos, pos+3).trim();
						return "UTM-"+zoneName;
					}
				}
			} catch(Exception e) {
				Logger.warn(e);
			}
		}		
		return null;
	}

	public double getPixelSizeXdiv(int div) {
		return pixel_size_x * div;
	}

	public double getPixelSizeYdiv(int div) {
		return pixel_size_y * div;
	}

	public double pixelXdivToGeo(int div, int pixelX) {
		return (pixelX * (pixel_size_x * div)) + offset_x;
	}

	public double pixelYdivToGeo(int div, int pixelY) {
		return (pixelY * (pixel_size_y * div)) + offset_y;
	}
	
	public void throwNotSameXYpixelsize() {
		if(pixel_size_x != pixel_size_y) {
			throw new RuntimeException("Not same pixel size in x y: " + pixel_size_x + "  " + pixel_size_y);
		}
	}

}
