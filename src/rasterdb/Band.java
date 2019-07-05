package rasterdb;

import java.util.LinkedHashMap;
import java.util.Map;

import rasterdb.tile.TilePixel;
import util.yaml.YamlMap;

public class Band {

	public final int type;
	public final int index;
	public final double wavelength; // nanometer   optional
	public final double fwhm; // nanometer    optional
	public final String title; // optional
	public final String visualisation; // optional
	public final double vis_min; // optional
	public final double vis_max; // optional
	
	public static class Builder {
		public int type;
		public int index;
		public double wavelength; // nanometer   optional
		public double fwhm; // nanometer    optional
		public String title; // optional
		public String visualisation; // optional
		public double vis_min; // optional
		public double vis_max; // optional
		
		public Builder(int type, int index, double wavelength, double fwhm, String title, String visualisation, double vis_min, double vis_max) {
			this.type = type;
			this.index = index;
			this.wavelength = wavelength;
			this.fwhm = fwhm;
			this.title = title;
			this.visualisation = visualisation;
			this.vis_min = vis_min;
			this.vis_max = vis_max;
		}
		
		public Builder(Band band) {
			this.type = band.type;
			this.index = band.index;
			this.wavelength = band.wavelength;
			this.fwhm = band.fwhm;
			this.title = band.title;
			this.visualisation = band.visualisation;
			this.vis_min = band.vis_min;
			this.vis_max = band.vis_max;
		}
		
		public Band build() {
			return new Band(type, index, wavelength, fwhm, title, visualisation, vis_min, vis_max);
		}
	}

	private Band(int type, int index, double wavelength, double fwhm, String title, String visualisation, double vis_min, double vis_max) {
		this.type = type;
		this.index = index;
		this.wavelength = wavelength;
		this.fwhm = fwhm;
		this.title = title;
		this.visualisation = visualisation;
		this.vis_min = vis_min;
		this.vis_max = vis_max;
	}

	public static Band ofSpectralBand(int type, int index, double wavelength, double fwhm, String title, String visualisation) {
		return new Band(type, index, wavelength, fwhm, title, visualisation, Double.NaN, Double.NaN);
	}

	public static Band of(int type, int index, String title, String visualisation) {
		return new Band(type, index, Double.NaN, Double.NaN, title, visualisation, Double.NaN, Double.NaN);
	}

	public Map<String, Object> toYamlWithoutIndex() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("type", type);
		if(has_wavelength()) {
			map.put("wavelength", wavelength);
		}
		if(has_fwhm()) {
			map.put("fwhm", fwhm);
		}
		if(has_title()) {
			map.put("title", title);
		}
		if(has_visualisation()) {
			map.put("visualisation", visualisation);
		}
		if(has_vis_min()) {
			map.put("vis_min", vis_min);
		}
		if(has_vis_max()) {
			map.put("vis_max", vis_max);
		}
		return map;
	}

	public static Band ofYaml(YamlMap yamlmap, int index) {
		int type = yamlmap.optInt("type", 0);
		double wavelength = yamlmap.optDouble("wavelength", Double.NaN);
		double fwhm = yamlmap.optDouble("fwhm", Double.NaN);
		String title = yamlmap.optString("title", null);
		String visualisation = yamlmap.optString("visualisation", null);
		double vis_min = yamlmap.optDouble("vis_min", Double.NaN);
		double vis_max = yamlmap.optDouble("vis_max", Double.NaN);
		return new Band(type, index, wavelength, fwhm, title, visualisation, vis_min, vis_max);		
	}

	public double getAbsDiff(double wavelength) {
		return Math.abs(wavelength - this.wavelength);
	}

	public double getRelevanceDiff(double wavelength) {
		double half = has_fwhm() ? fwhm : 1;
		double diffA = Math.abs(wavelength - this.wavelength + half);
		double diffB = Math.abs(wavelength - this.wavelength - half);
		return (diffA + diffB) / 2;	
	}

	public boolean has_wavelength() {
		return Double.isFinite(wavelength);
	}

	public boolean has_fwhm() {
		return Double.isFinite(fwhm);
	}
	
	public boolean has_vis_min() {
		return Double.isFinite(vis_min);
	}
	
	public boolean has_vis_max() {
		return Double.isFinite(vis_max);
	}

	public boolean has_title() {
		return title != null;
	}
	
	public boolean has_visualisation() {
		return visualisation != null;
	}

	@Override
	public String toString() {
		return "Band [type=" + type + ", index=" + index + ", wavelength=" + wavelength + ", fwhm=" + fwhm + ", title="
				+ title + "]";
	}

	public boolean isInFwhm(double checkWavelength) {
		double half = has_fwhm() ? fwhm : 1;
		double min = wavelength - half;
		double max = wavelength + half;
		return min <= checkWavelength && checkWavelength <= max; 
	}
	
	public boolean isTypeShortOrExactConvertible() {
		return isTypeShort();
	}
	
	public boolean isTypeFloatOrExactConvertible() {
		return isTypeFloat() || isTypeShort();
	}
	
	public boolean isTypeShort() {
		return type == TilePixel.TYPE_SHORT;
	}
	
	public boolean isTypeFloat() {
		return type == TilePixel.TYPE_FLOAT;
	}
	
	public short getShortNA() {
		return 0;
	}
	
	public String getDatatypeName() {
		switch (type) {
		case TilePixel.TYPE_SHORT:
			return "int16";
		case TilePixel.TYPE_FLOAT:
			return "float32";			
		default:
			return "unknown";
		}
	}

}
