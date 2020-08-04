package rasterdb;

import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.util.NumberUtil;

public class BandProcessing {
	private static final Logger log = LogManager.getLogger();

	/**
	 * Get band of wavelength and fwhm or null if no exact match is found
	 * Used to get correct band for data import
	 * @param wavelength
	 * @param fwhm
	 * @return null if not found
	 */
	public static Band matchSpectralBand(RasterDB rasterdb, double wavelength, double fwhm, double tolerance) {
		for(Band band:rasterdb.bandMapReadonly.values()) {
			if(band.has_wavelength()) {
				if(NumberUtil.equalsWithTolerance(band.wavelength, wavelength, tolerance)) { 
					if(NumberUtil.equalsWithTolerance(band.fwhm, fwhm, tolerance)) {
						return band;
					}
				}
			}
		}
		return null;
	}

	/**
	 * 
	 * @param rasterdb
	 * @param wavelength
	 * @return null if not found
	 */
	public static Band getClosestSpectralBand(RasterDB rasterdb, double wavelength) {
		double minDiff = Double.POSITIVE_INFINITY;
		//double minFwhm = Double.POSITIVE_INFINITY;
		Band minBand = null;
		for(Band band:rasterdb.bandMapReadonly.values()) {
			if(band.has_wavelength()) {
				//double diff = band.getAbsDiff(wavelength);
				double diff = band.getRelevanceDiff(wavelength);
				//log.info(diff + "   "+wavelength+ "   "  +band);
				if(diff < minDiff) {
					minBand = band;
					minDiff = diff;
				}
			}
		}
		//log.info("GET "+minBand);
		return minBand;
	}
	
	public static Band getBestSpectralBandWithinFwhm(RasterDB rasterdb, double wavelength) {
		Band bestBand = null;
		double minFwhm = Double.MAX_VALUE;
		for(Band band:rasterdb.bandMapReadonly.values()) {
			if(band.isInFwhm(wavelength)) {
				if(band.fwhm < minFwhm) {
					bestBand = band;
					minFwhm = band.fwhm;
				} else if (band.fwhm == minFwhm) {
					if(band.getAbsDiff(wavelength) < bestBand.getAbsDiff(wavelength)) {
						bestBand = band;
						minFwhm = band.fwhm;
					}
				}
			}
		}
		return bestBand;
	}

	/**
	 * 
	 * @param rasterdb
	 * @param m
	 * @param targetWavelength 
	 * @return same band if not found
	 */
	public static Band getNextLessSpectralBand(RasterDB rasterdb, Band m, double targetWavelength) {
		Band r = null;
		for(Band band:rasterdb.bandMapReadonly.values()) {
			if(band.has_wavelength()) {
				double wv = band.wavelength;
				if(wv < m.wavelength) {
					if(r == null || r.getRelevanceDiff(targetWavelength) > band.getRelevanceDiff(targetWavelength)) {
						r = band;
					}
				}
			}
		}
		return r==null?m:r;
	}

	/**
	 * 
	 * @param rasterdb
	 * @param m
	 * @return same band if not found
	 */
	public static Band getNextGreaterSpectralBand(RasterDB rasterdb, Band m, double targetWavelength) {
		Band r = null;
		for(Band band:rasterdb.bandMapReadonly.values()) {
			if(band.has_wavelength()) {
				double wv = band.wavelength;
				if(wv > m.wavelength) {
					if(r == null || r.getRelevanceDiff(targetWavelength) > band.getRelevanceDiff(targetWavelength)) {
						r = band;
					}
				}
			}
		}
		return r==null?m:r;
	}

	/**
	 * Get three best fitting bands for RGB color data.
	 * Based on available bands two or all three bands may be the same.
	 * @return
	 */
	public static Band[] getBestColorBands(RasterDB rasterdb) {

		/*//https://de.wikipedia.org/wiki/Zapfen_%28Auge%29#/media/File:Cone-response-de.svg
		double WV_R = 564.000;
		double WV_G = 534.000;
		double WV_B = 420.000;*/

		/*//hyperspectral
		double WV_R = 604.300;
		double WV_G = 553.500;
		double WV_B = 448.400;*/

		//color spreading
		double WV_R = 604.300;
		double WV_G = 534.000;
		double WV_B = 420.000;

		Band r = getClosestSpectralBand(rasterdb, WV_R);
		Band g = getClosestSpectralBand(rasterdb, WV_G);
		Band b = getClosestSpectralBand(rasterdb, WV_B);

		if(r != null && g != null && b != null) {
			if(r.index == g.index) {
				r = getNextGreaterSpectralBand(rasterdb, r, WV_R); 
			}
			if(g.index == b.index) {
				b = getNextLessSpectralBand(rasterdb, b, WV_B); 
			}
			if(r.index == g.index && g.index == b.index) {
				Iterator<Band> it = rasterdb.bandMapReadonly.values().iterator();
				if(it.hasNext()) {
					r = g = b = it.next();
				}
				if(it.hasNext()) {
					g = b = it.next();
				}
				if(it.hasNext()) {
					b = it.next();
				}
			}		
		} else { // no spectral bands
			Iterator<Band> it = rasterdb.bandMapReadonly.values().iterator();
			if(it.hasNext()) {
				r = g = b = it.next();
			}
			if(it.hasNext()) {
				g = b = it.next();
			}
			if(it.hasNext()) {
				b = it.next();
			}			
		}
		boolean rSet = false;
		boolean gSet = false;
		boolean bSet = false;
		for(Band band:rasterdb.bandMapReadonly.values()) {
			if(band.visualisation != null) {
				switch(band.visualisation) {
				case "red":
					r = band;					
					rSet = true;
					break;
				case "green":
					g = band;
					gSet = true;
					break;
				case "blue":
					b = band;
					bSet = true;
					break;
				}
			}
		}
		if(!(rSet && bSet && gSet)) {
			for(Band band:rasterdb.bandMapReadonly.values()) {
				if(!rSet && band.has_title() && band.title.toLowerCase().equals("red")) {
					r = band;					
					rSet = true;
				}
				if(!gSet && band.has_title() && band.title.toLowerCase().equals("green")) {
					g = band;					
					gSet = true;
				}
				if(!bSet && band.has_title() && band.title.toLowerCase().equals("blue")) {
					b = band;					
					bSet = true;
				}
			}
		}
		if(rSet && bSet && !gSet) {
			log.info("set green");
			g = r;
		}
		return new Band[]{r, g, b};
	}
	
	public static Band getBandRed(RasterDB rasterdb) {
		for(Band band:rasterdb.bandMapReadonly.values()) {
			if(band.has_title() && band.title.toLowerCase().equals("red")) {
				return band;
			}
		}
		return getClosestSpectralBand(rasterdb, 564);
	}
	
	public static Band getBandGreen(RasterDB rasterdb) {
		for(Band band:rasterdb.bandMapReadonly.values()) {
			if(band.has_title() && band.title.toLowerCase().equals("green")) {
				return band;
			}
		}
		return getClosestSpectralBand(rasterdb, 534);
	}
	
	public static Band getBandBlue(RasterDB rasterdb) {
		for(Band band:rasterdb.bandMapReadonly.values()) {
			if(band.has_title() && band.title.toLowerCase().equals("blue")) {
				return band;
			}
		}
		return getClosestSpectralBand(rasterdb, 420);
	}

}
