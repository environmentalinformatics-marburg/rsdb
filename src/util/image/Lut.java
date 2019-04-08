package util.image;

public class Lut {
	
	public static double[] getGammaLUT256d(double min, double max, double gamma) {		
		double range = max - min;
		double[] lut = new double[256];
		for (int i = 0; i < 256; i++) {
			double v = i / 256d;
			double w = Math.pow(v, gamma) * range + min;
			lut[i] = w;
		}
		return lut;
	}
	
	public static int match256d(double[] lut, double w) {
		int i = 128;
		int len = 64;
		while(len>0) {
			if(w < lut[i]) {
				i -= len;
			} else {
				i += len;				
			}
			len >>>= 1;
		}
		return w < lut[i] ? i - 1 : i;
	}	
	
	public static float[] getGammaLUT256f(double min, double max, double gamma) {		
		double range = max - min;
		float[] lut = new float[256];
		for (int i = 0; i < 256; i++) {
			double v = i / 256d;
			double w = Math.pow(v, gamma) * range + min;
			lut[i] = (float) w;
		}
		return lut;
	}
	
	public static int match256f(float[] lut, float w) {
		int i = 128;
		int len = 64;
		while(len>0) {
			if(w < lut[i]) {
				i -= len;
			} else {
				i += len;				
			}
			len >>>= 1;
		}
		return w < lut[i] ? i - 1 : i;
	}
	
	public static short[] getGammaLUT256s(double min, double max, double gamma) {		
		double range = max - min + 1;
		short[] lut = new short[256];
		for (int i = 0; i < 256; i++) {
			double v = i / 256d;
			double w = Math.pow(v, gamma) * range + min;
			lut[i] = (short) w;
		}
		return lut;
	}
	
	public static int match256s(short[] lut, short w) {
		int i = 128;
		int len = 64;
		while(len>0) {
			if(w < lut[i]) {
				i -= len;
			} else {
				i += len;				
			}
			len >>>= 1;
		}
		return w < lut[i] ? i - 1 : i;
	}
	
	public static double[] getLogLUT256d(double min, double max) {		
		double[] lut = new double[256];
		double f = Math.log(max - min + 1);
		for (int i = 0; i < 256; i++) {
			double v = i / 256d;
			double w = Math.expm1(v * f) + min;
			lut[i] = w;
		}
		return lut;
	}
	
	public static short[] getLogLUT256s(double min, double max) {		
		short[] lut = new short[256];
		double f = Math.log(max - min + 1);
		for (int i = 0; i < 256; i++) {
			double v = i / 256d;
			double w = Math.expm1(v * f) + min;
			lut[i] = (short) w;
		}
		return lut;
	}
	
}
