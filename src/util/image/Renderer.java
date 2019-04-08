package util.image;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.Timer;
import util.frame.DoubleFrame;
import util.frame.FloatFrame;
import util.frame.ShortFrame;

public class Renderer {
	static final Logger log = LogManager.getLogger();
	
	public static ImageBufferARGB renderRgbShort(ShortFrame frameR, ShortFrame frameG, ShortFrame frameB, short naR, short naG, short naB, int width, int height, double gamma, double[] range, boolean syncBands) {
		Timer.start("prep");
		short[] rlut;
		short[] glut;
		short[] blut;
		if(syncBands) {
			int[] syncRange = RangerShort.getRangeSync(new short[][][] {frameR.data, frameG.data, frameB.data}, new short[] {naR, naG, naB}, range);
			double syncGamma = RangerDouble.getGamma(syncRange, gamma);
			short[] syncLut = Lut.getGammaLUT256s(syncRange[0], syncRange[1], syncGamma);
			rlut = syncLut;
			glut = syncLut;
			blut = syncLut;
		} else {
			int[] rrange = RangerShort.getRange(frameR.data, naR, range);
			int[] grange = RangerShort.getRange(frameG.data, naG, range);
			int[] brange = RangerShort.getRange(frameB.data, naB, range);
			double rgamma = RangerDouble.getGamma(rrange, gamma);
			double ggamma = RangerDouble.getGamma(grange, gamma);
			double bgamma = RangerDouble.getGamma(brange, gamma);
			log.info("brange: "+Arrays.toString(brange));
			log.info("gamma "+rgamma+"  "+ggamma+" "+bgamma);
			rlut = Lut.getGammaLUT256s(rrange[0], rrange[1], rgamma);
			glut = Lut.getGammaLUT256s(grange[0], grange[1], ggamma);
			blut = Lut.getGammaLUT256s(brange[0], brange[1], bgamma);
		}
		log.info(Timer.stop("prep"));
		Timer.start("tonemapping3");
		ImageBufferARGB imageBufferARGB = new ImageBufferARGB(frameR.width, frameR.height);
		DrawerShort.drawRGB3Parallel(imageBufferARGB, frameR.data, frameG.data, frameB.data, naR, naG, naB, rlut, glut, blut);
		log.info(Timer.stop("tonemapping3"));
		ImageBufferARGB scaled = imageBufferARGB.width == width && imageBufferARGB.height == height ? imageBufferARGB : imageBufferARGB.scaled(width, height);
		return scaled;
	}

	public static ImageBufferARGB renderRbShort(ShortFrame frameR, ShortFrame frameB, short naR, short naB, int width, int height, double gamma, double[] range) {
		int[] rrange = RangerShort.getRange(frameR.data, naR, range);
		int[] brange = RangerShort.getRange(frameB.data, naB, range);
		ImageBufferARGB imageBufferARGB = new ImageBufferARGB(frameR.width, frameR.height);
		Timer.start("tonemapping3");
		short[] rlut = Lut.getGammaLUT256s(rrange[0], rrange[1], RangerDouble.getGamma(rrange, gamma));
		short[] blut = Lut.getGammaLUT256s(brange[0], brange[1], RangerDouble.getGamma(brange, gamma));
		DrawerShort.drawRB3Parallel(imageBufferARGB, frameR.data, frameB.data, naR, naB, rlut, blut);
		log.info(Timer.stop("tonemapping3"));
		ImageBufferARGB scaled = imageBufferARGB.width == width && imageBufferARGB.height == height ? imageBufferARGB : imageBufferARGB.scaled(width, height);
		return scaled;
	}

	public static ImageBufferARGB renderGreyShort(ShortFrame frame, short na, int width, int height, double gamma, double[] range) {
		int[] irange = RangerShort.getRange(frame.data, na, range);
		ImageBufferARGB imageBufferARGB = new ImageBufferARGB(frame.width, frame.height);
		Timer.start("tonemapping3");
		short[] lut = Lut.getGammaLUT256s(irange[0], irange[1], RangerDouble.getGamma(irange, gamma));
		DrawerShort.drawGrey3Parallel(imageBufferARGB, frame.data, na, lut);
		log.info(Timer.stop("tonemapping3"));
		//convert(imageBufferARGB);
		ImageBufferARGB scaled = imageBufferARGB.width == width && imageBufferARGB.height == height ? imageBufferARGB : imageBufferARGB.scaled(width, height);
		return scaled;		
	}
	
	public static ImageBufferARGB renderPaletteShort(ShortFrame frame, short na, int width, int height, double gamma, double[] range, int[] palette) {
		int[] irange = RangerShort.getRange(frame.data, na, range);
		ImageBufferARGB imageBufferARGB = new ImageBufferARGB(frame.width, frame.height);
		Timer.start("tonemapping3");
		short[] lut = Lut.getGammaLUT256s(irange[0], irange[1], RangerDouble.getGamma(irange, gamma));
		DrawerShort.drawPalette3Parallel(imageBufferARGB, frame.data, na, lut, palette);
		log.info(Timer.stop("tonemapping3"));
		ImageBufferARGB scaled = imageBufferARGB.width == width && imageBufferARGB.height == height ? imageBufferARGB : imageBufferARGB.scaled(width, height);
		return scaled;		
	}

	public static ImageBufferARGB renderRgbDouble(DoubleFrame frameR, DoubleFrame frameG, DoubleFrame frameB, int width, int height, double gamma, double[] range, boolean syncBands) {
		double[] rlut;
		double[] glut;
		double[] blut;
		if(syncBands) {
			double[] syncRange = RangerDouble.getRangeSync(new double[][][] {frameR.data, frameG.data, frameB.data}, range);
			double syncGamma = RangerDouble.getGamma(syncRange, gamma);
			double[] syncLut = Lut.getGammaLUT256d(syncRange[0], syncRange[1], syncGamma);
			rlut = syncLut;
			glut = syncLut;
			blut = syncLut;
		} else {
		double[] rrange = RangerDouble.getRange(frameR.data, range);
		double[] grange = RangerDouble.getRange(frameG.data, range);
		double[] brange = RangerDouble.getRange(frameB.data, range);
		rlut = Lut.getGammaLUT256d(rrange[0], rrange[1], RangerDouble.getGamma(rrange, gamma));
		glut = Lut.getGammaLUT256d(grange[0], grange[1], RangerDouble.getGamma(grange, gamma));
		blut = Lut.getGammaLUT256d(brange[0], brange[1], RangerDouble.getGamma(brange, gamma));
		}
		Timer.start("tonemapping3 double");
		ImageBufferARGB imageBufferARGB = new ImageBufferARGB(frameR.width, frameR.height);
		DrawerDouble.drawRGB3Parallel(imageBufferARGB, frameR.data, frameG.data, frameB.data, rlut, glut, blut);
		log.info(Timer.stop("tonemapping3 double"));
		ImageBufferARGB scaled = imageBufferARGB.width == width && imageBufferARGB.height == height ? imageBufferARGB : imageBufferARGB.scaled(width, height);
		return scaled;
	}

	public static ImageBufferARGB renderRbDouble(DoubleFrame frameR, DoubleFrame frameB, int width, int height, double gamma, double[] range) {
		double[] rrange = RangerDouble.getRange(frameR.data, range);
		double[] brange = RangerDouble.getRange(frameB.data, range);
		ImageBufferARGB imageBufferARGB = new ImageBufferARGB(frameR.width, frameR.height);
		Timer.start("tonemapping3 double");
		double[] rlut = Lut.getGammaLUT256d(rrange[0], rrange[1], RangerDouble.getGamma(rrange, gamma));
		double[] blut = Lut.getGammaLUT256d(brange[0], brange[1], RangerDouble.getGamma(brange, gamma));
		DrawerDouble.drawRB3Parallel(imageBufferARGB, frameR.data, frameB.data, rlut, blut);
		log.info(Timer.stop("tonemapping3 double"));
		ImageBufferARGB scaled = imageBufferARGB.width == width && imageBufferARGB.height == height ? imageBufferARGB : imageBufferARGB.scaled(width, height);
		return scaled;
	}	

	public static ImageBufferARGB renderGreyDouble(DoubleFrame frame, int width, int height, double gamma, double[] range) {
		double[] irange = RangerDouble.getRange(frame.data, range);
		ImageBufferARGB imageBufferARGB = new ImageBufferARGB(frame.width, frame.height);
		Timer.start("tonemapping3 double");
		double[] lut = Lut.getGammaLUT256d(irange[0], irange[1], RangerDouble.getGamma(irange, gamma));
		DrawerDouble.drawGrey3Parallel(imageBufferARGB, frame.data, lut);
		log.info(Timer.stop("tonemapping3 double"));
		ImageBufferARGB scaled = imageBufferARGB.width == width && imageBufferARGB.height == height ? imageBufferARGB : imageBufferARGB.scaled(width, height);
		return scaled;
	}
	
	public static ImageBufferARGB renderPaletteDouble(DoubleFrame frame, int width, int height, double gamma, double[] range, int[] palette) {
		double[] irange = RangerDouble.getRange(frame.data, range);
		ImageBufferARGB imageBufferARGB = new ImageBufferARGB(frame.width, frame.height);
		Timer.start("tonemapping3 double");
		double[] lut = Lut.getGammaLUT256d(irange[0], irange[1], RangerDouble.getGamma(irange, gamma));
		DrawerDouble.drawPalette3Parallel(imageBufferARGB, frame.data, lut, palette);
		log.info(Timer.stop("tonemapping3 double"));
		ImageBufferARGB scaled = imageBufferARGB.width == width && imageBufferARGB.height == height ? imageBufferARGB : imageBufferARGB.scaled(width, height);
		return scaled;
	}

	public static ImageBufferARGB renderRgbFloat(FloatFrame frameR, FloatFrame frameG, FloatFrame frameB, int width, int height, double gamma, double[] range, boolean syncBands) {
		float[] rlut;
		float[] glut;
		float[] blut;
		if(syncBands) {
			double[] syncRange = RangerFloat.getRangeSync(new float[][][] {frameR.data, frameG.data, frameB.data}, range);
			double syncGamma = RangerDouble.getGamma(syncRange, gamma);
			float[] syncLut = Lut.getGammaLUT256f(syncRange[0], syncRange[1], syncGamma);
			rlut = syncLut;
			glut = syncLut;
			blut = syncLut;			 
		} else {
			double[] rrange = RangerFloat.getRange(frameR.data, range);
			double[] grange = RangerFloat.getRange(frameG.data, range);
			double[] brange = RangerFloat.getRange(frameB.data, range);
			rlut = Lut.getGammaLUT256f(rrange[0], rrange[1], RangerDouble.getGamma(rrange, gamma));
			glut = Lut.getGammaLUT256f(grange[0], grange[1], RangerDouble.getGamma(grange, gamma));
			blut = Lut.getGammaLUT256f(brange[0], brange[1], RangerDouble.getGamma(brange, gamma));
		}
		Timer.start("tonemapping3 float");
		ImageBufferARGB imageBufferARGB = new ImageBufferARGB(frameR.width, frameR.height);
		DrawerFloat.drawRGB3Parallel(imageBufferARGB, frameR.data, frameG.data, frameB.data, rlut, glut, blut);
		log.info(Timer.stop("tonemapping3 float"));
		ImageBufferARGB scaled = imageBufferARGB.width == width && imageBufferARGB.height == height ? imageBufferARGB : imageBufferARGB.scaled(width, height);
		return scaled;
	}

	public static ImageBufferARGB renderRbFloat(FloatFrame frameR, FloatFrame frameB, int width, int height, double gamma, double[] range) {
		double[] rrange = RangerFloat.getRange(frameR.data, range);
		double[] brange = RangerFloat.getRange(frameB.data, range);
		ImageBufferARGB imageBufferARGB = new ImageBufferARGB(frameR.width, frameR.height);
		Timer.start("tonemapping3 float");
		float[] rlut = Lut.getGammaLUT256f(rrange[0], rrange[1], RangerDouble.getGamma(rrange, gamma));
		float[] blut = Lut.getGammaLUT256f(brange[0], brange[1], RangerDouble.getGamma(brange, gamma));
		DrawerFloat.drawRB3Parallel(imageBufferARGB, frameR.data, frameB.data, rlut, blut);
		log.info(Timer.stop("tonemapping3 float"));
		ImageBufferARGB scaled = imageBufferARGB.width == width && imageBufferARGB.height == height ? imageBufferARGB : imageBufferARGB.scaled(width, height);
		return scaled;
	}

	public static ImageBufferARGB renderGreyFloat(FloatFrame frame, int width, int height, double gamma, double[] range) {
		double[] irange = RangerFloat.getRange(frame.data, range);
		ImageBufferARGB imageBufferARGB = new ImageBufferARGB(frame.width, frame.height);
		Timer.start("tonemapping3 float");
		float[] lut = Lut.getGammaLUT256f(irange[0], irange[1], RangerDouble.getGamma(irange, gamma));
		DrawerFloat.drawGrey3Parallel(imageBufferARGB, frame.data, lut);
		log.info(Timer.stop("tonemapping3 float"));
		ImageBufferARGB scaled = imageBufferARGB.width == width && imageBufferARGB.height == height ? imageBufferARGB : imageBufferARGB.scaled(width, height);
		return scaled;
	}
	
	public static ImageBufferARGB renderPaletteFloat(FloatFrame frame, int width, int height, double gamma, double[] range, int[] palette) {
		double[] irange = RangerFloat.getRange(frame.data, range);
		ImageBufferARGB imageBufferARGB = new ImageBufferARGB(frame.width, frame.height);
		Timer.start("tonemapping3 float");
		float[] lut = Lut.getGammaLUT256f(irange[0], irange[1], RangerDouble.getGamma(irange, gamma));
		DrawerFloat.drawPalette3Parallel(imageBufferARGB, frame.data, lut, palette);
		log.info(Timer.stop("tonemapping3 float"));
		ImageBufferARGB scaled = imageBufferARGB.width == width && imageBufferARGB.height == height ? imageBufferARGB : imageBufferARGB.scaled(width, height);
		return scaled;
	}
}
