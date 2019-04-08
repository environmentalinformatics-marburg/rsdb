package util.raster;

import java.io.DataOutput;
import java.io.IOException;

public class RasterWriterBE {	
	
	public static void toBuffer(short[] row, byte[] buffer) {
		int len = row.length;
		for (int i = 0; i<len; i++) {
			short v = row[i];
			int b = i<<1;			
			buffer[b] = (byte) (v>>8);
			buffer[b+1] = (byte) v;
		}
	}
	
	public static void toBuffer(short[] row, int min, int max, byte[] buffer) {
		for (int i = min; i<=max; i++) {
			short v = row[i];
			int b = (i-min)<<1;			
			buffer[b] = (byte) (v>>8);
			buffer[b+1] = (byte) v;
		}
	}
	
	
	public static void write(short[][] data, DataOutput out) throws IOException {
		int len = data.length;
		byte[] buffer = new byte[data[0].length<<1];
		for (int i=0; i<len; i++) {
			toBuffer(data[i], buffer);
			out.write(buffer);
		}
	}
	
	public static void write(short[][] data, int xmin, int ymin, int xmax, int ymax, DataOutput out) throws IOException {
		byte[] buffer = new byte[(xmax-xmin+1)<<1];
		for (int i=ymin; i<=ymax; i++) {
			toBuffer(data[i], xmin, xmax, buffer);
			out.write(buffer);
		}
	}
	
	public static void writeFlipRows(short[][] data, DataOutput out) throws IOException {
		int len = data.length;
		byte[] buffer = new byte[data[0].length<<1];
		for (int i=len-1; 0<=i; i--) {
			toBuffer(data[i], buffer);
			out.write(buffer);
		}
	}
	
	public static void writeFlipRows(short[][] data, int xmin, int ymin, int xmax, int ymax, DataOutput out) throws IOException {
		byte[] buffer = new byte[(xmax-xmin+1)<<1];
		for (int i=ymax; ymin<=i; i--) {
			toBuffer(data[i], xmin, xmax, buffer);
			out.write(buffer);
		}
	}
	
	public static void toBuffer(double[] row, byte[] buffer) {
		int len = row.length;
		for (int i = 0; i<len; i++) {
			long v = Double.doubleToRawLongBits(row[i]);
			int b = i<<3;			
			buffer[b+0] = (byte)(v >>> 56);
			buffer[b+1] = (byte)(v >>> 48);
			buffer[b+2] = (byte)(v >>> 40);
			buffer[b+3] = (byte)(v >>> 32);
			buffer[b+4] = (byte)(v >>> 24);
			buffer[b+5] = (byte)(v >>> 16);
			buffer[b+6] = (byte)(v >>>  8);
			buffer[b+7] = (byte) v;			
		}
	}
	
	public static void toBuffer(double[] row, int min, int max, byte[] buffer) {
		for (int i = min; i<=max; i++) {
			long v = Double.doubleToRawLongBits(row[i]);
			int b = (i-min)<<3;			
			buffer[b+0] = (byte)(v >>> 56);
			buffer[b+1] = (byte)(v >>> 48);
			buffer[b+2] = (byte)(v >>> 40);
			buffer[b+3] = (byte)(v >>> 32);
			buffer[b+4] = (byte)(v >>> 24);
			buffer[b+5] = (byte)(v >>> 16);
			buffer[b+6] = (byte)(v >>>  8);
			buffer[b+7] = (byte) v;
		}
	}
	
	public static void write(double[][] data, DataOutput out) throws IOException {
		int len = data.length;
		byte[] buffer = new byte[data[0].length<<3];
		for (int i=0; i<len; i++) {
			toBuffer(data[i], buffer);
			out.write(buffer);
		}
	}
	
	public static void write(double[][] data, int xmin, int ymin, int xmax, int ymax, DataOutput out) throws IOException {
		byte[] buffer = new byte[(xmax-xmin+1)<<3];
		for (int i=ymin; i<=ymax; i++) {
			toBuffer(data[i], xmin, xmax, buffer);
			out.write(buffer);
		}
	}
	
	public static void writeFlipRows(double[][] data, DataOutput out) throws IOException {
		int len = data.length;
		byte[] buffer = new byte[data[0].length<<3];
		for (int i=len-1; 0<=i; i--) {
			toBuffer(data[i], buffer);
			out.write(buffer);
		}
	}
	
	public static void writeFlipRows(double[][] data, int xmin, int ymin, int xmax, int ymax, DataOutput out) throws IOException {
		byte[] buffer = new byte[(xmax-xmin+1)<<3];
		for (int i=ymax; ymin<=i; i--) {
			toBuffer(data[i], xmin, xmax, buffer);
			out.write(buffer);
		}
	}	
}
