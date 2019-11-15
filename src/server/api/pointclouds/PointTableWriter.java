package server.api.pointclouds;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;

import pointcloud.AttributeSelector;
import pointcloud.LasWriter;
import pointcloud.PointTable;
import pointcloud.RdatWriter;
import pointcloud.LasWriter.LAS_HEADER;
import pointcloud.LasWriter.POINT_DATA_RECORD;
import util.ByteArrayOut;
import util.Receiver;

public class PointTableWriter {
	private static final Logger log = LogManager.getLogger();
	
	//derived from https://stackoverflow.com/a/10554128
	private static final int POW10[] = {1, 10, 100, 1000, 10000, 100000, 1000000};
	private static String format(double val, int precision) {
		StringBuilder sb = new StringBuilder();
		if (val < 0) {
			sb.append('-');
			val = -val;
		}
		int exp = POW10[precision];
		long lval = (long)(val * exp + 0.5);
		sb.append(lval / exp).append('.');
		long fval = lval % exp;
		for (int p = precision - 1; p > 0 && fval < POW10[p]; p--) {
			sb.append('0');
		}
		sb.append(fval);
		return sb.toString();
	}

	public static void writeXYZ(Stream<PointTable> pointTables, Receiver receiver) throws IOException {
		receiver.setContentType("application/octet-stream");
		PrintWriter writer = receiver.getWriter();
		pointTables.sequential().forEach(p -> {
			int len = p.rows;
			double[] xs = p.x == null ? new double[len] : p.x;
			double[] ys = p.y == null ? new double[len] : p.y;
			double[] zs = p.z == null ? new double[len] : p.z;
			for (int i = 0; i < len; i++) {
				writer.print(format(xs[i],2));
				writer.print(' ');
				writer.print(format(ys[i],2));
				writer.print(' ');
				writer.print(format(zs[i],2));
				writer.println();
			}
		});
	}

	static void writeLAS(PointTable[] pointTables, Receiver receiver) throws IOException {
		LasWriter.writePoints(pointTables, receiver, LAS_HEADER.V_1_2, POINT_DATA_RECORD.FORMAT_0);
	}

	private static char[] fillChar(int len) {
		char[] a = new char[len];
		Arrays.fill(a, Character.MAX_VALUE);
		return a;
	}

	private static void writeAllPoints(PointTable[] pointTables, Receiver receiver, String format, int pointCount) throws IOException {
		receiver.setContentType("application/octet-stream");
		switch(format) {
		case "xzy": {
			@SuppressWarnings("resource")
			ByteArrayOut byteOut = new ByteArrayOut((int) (4 + pointCount*(3*4)));
			byteOut.putIntRaw(pointCount);
			for(PointTable p:pointTables) {
				int len = p.rows;
				double[] x = p.x;
				double[] y = p.y;
				double[] z = p.z;
				for (int i = 0; i < len; i++) {
					byteOut.putFloatRaw((float) x[i]);
					byteOut.putFloatRaw((float) z[i]);
					byteOut.putFloatRaw((float) y[i]);
				}
			}
			byteOut.flip(receiver.getOutputStream());
			break;
		}
		case "xzy_classification": {
			@SuppressWarnings("resource")
			ByteArrayOut byteOut = new ByteArrayOut((int) (4 + pointCount*(3*4 + 1)));
			byteOut.putIntRaw(pointCount);
			for(PointTable p:pointTables) {
				int len = p.rows;
				double[] x = p.x;
				double[] y = p.y;
				double[] z = p.z;
				for (int i = 0; i < len; i++) {
					byteOut.putFloatRaw((float) x[i]);
					byteOut.putFloatRaw((float) z[i]);
					byteOut.putFloatRaw((float) y[i]);
				}
			}
			for(PointTable p:pointTables) {
				int len = p.rows;
				byte[] classification = p.classification;
				if(classification != null) {
					byteOut.putBytesRaw(classification, 0, len);
				} else  {
					byteOut.putBytesRaw(new byte[len]);
				}
			}
			byteOut.flip(receiver.getOutputStream());
			break;
		}
		case "xzy_rgb": {
			@SuppressWarnings("resource")
			ByteArrayOut byteOut = new ByteArrayOut((int) (4 + pointCount*(3*4 + 3*2)));
			byteOut.putIntRaw(pointCount);
			for(PointTable p:pointTables) {
				int len = p.rows;
				double[] x = p.x;
				double[] y = p.y;
				double[] z = p.z;
				for (int i = 0; i < len; i++) {
					byteOut.putFloatRaw((float) x[i]);
					byteOut.putFloatRaw((float) z[i]);
					byteOut.putFloatRaw((float) y[i]);
				}
			}
			for(PointTable p:pointTables) {
				int len = p.rows;
				char[] red = p.red == null ? fillChar(len) : p.red;
				char[] green = p.green == null ? fillChar(len) : p.green;
				char[] blue = p.blue == null ? fillChar(len) : p.blue;
				for (int i = 0; i < len; i++) {
					byteOut.putCharRaw(red[i]);
					byteOut.putCharRaw(green[i]);
					byteOut.putCharRaw(blue[i]);
				}
			}
			byteOut.flip(receiver.getOutputStream());
			break;
		}
		default:
			throw new RuntimeException("unknown js format: " + format);
		}		
	}

	static void writeJS(PointTable[] pointTables, Request request, Receiver receiver) throws IOException {
		int maxPoints = 5_500_000;
		int maxSamplePoints = 5_000_000;
		String format = request.getParameter("format");
		if(format == null) {
			format = "xzy_classification";
		}
		int pointCount = 0;
		for(PointTable p:pointTables) {
			pointCount += p.rows;
		}
		boolean useAllPoints = pointCount <= maxPoints;
		if(useAllPoints) {
			writeAllPoints(pointTables, receiver, format, pointCount);
		} else {
			double samplingFactor = ((double)pointCount) / maxSamplePoints;
			log.info("sample " + maxSamplePoints + " of " + pointCount+ " sampling factor " + samplingFactor);
			writeSamplePoints(pointTables, receiver, format, samplingFactor);
		}		
	}

	private static void writeSamplePoints(PointTable[] pointTables, Receiver receiver, String format, double samplingFactor) throws IOException {
		int samplePointCount = 0;
		{
			double samplingPos = 0;
			for(PointTable p:pointTables) {
				int len = p.rows;
				int ipos = (int) samplingPos;				
				while(ipos < len) {
					samplePointCount++;
					samplingPos += samplingFactor;
					ipos = (int) samplingPos;
				}
				samplingPos -= len;
			}
		}
		log.info("samples "+samplePointCount);
		int pointCount = samplePointCount;
		receiver.setContentType("application/octet-stream");
		switch(format) {
		case "xzy": {
			@SuppressWarnings("resource")
			ByteArrayOut byteOut = new ByteArrayOut((int) (4 + pointCount*(3*4)));
			byteOut.putIntRaw(pointCount);
			{
				double samplingPos = 0;
				for(PointTable p:pointTables) {
					int len = p.rows;
					double[] x = p.x;
					double[] y = p.y;
					double[] z = p.z;
					int i = (int) samplingPos;				
					while(i < len) {
						byteOut.putFloatRaw((float) x[i]);
						byteOut.putFloatRaw((float) z[i]);
						byteOut.putFloatRaw((float) y[i]);
						samplingPos += samplingFactor;
						i = (int) samplingPos;
					}
					samplingPos -= len;
				}
			}
			byteOut.flip(receiver.getOutputStream());
			break;
		}
		case "xzy_classification": {
			@SuppressWarnings("resource")
			ByteArrayOut byteOut = new ByteArrayOut((int) (4 + pointCount*(3*4 + 1)));
			byteOut.putIntRaw(pointCount);
			{
				double samplingPos = 0;
				for(PointTable p:pointTables) {
					int len = p.rows;
					double[] x = p.x;
					double[] y = p.y;
					double[] z = p.z;
					int i = (int) samplingPos;				
					while(i < len) {
						byteOut.putFloatRaw((float) x[i]);
						byteOut.putFloatRaw((float) z[i]);
						byteOut.putFloatRaw((float) y[i]);
						samplingPos += samplingFactor;
						i = (int) samplingPos;
					}
					samplingPos -= len;
				}
			}
			{
				double samplingPos = 0;
				for(PointTable p:pointTables) {
					int len = p.rows;
					byte[] classification = p.classification;
					int i = (int) samplingPos;				
					while(i < len) {
						if(classification != null) {
							byteOut.putByteRaw(classification[i]);
						} else  {
							byteOut.putByteRaw((byte)0);
						}
						samplingPos += samplingFactor;
						i = (int) samplingPos;
					}
					samplingPos -= len;
				}
			}
			byteOut.flip(receiver.getOutputStream());
			break;
		}
		case "xzy_rgb": {
			@SuppressWarnings("resource")
			ByteArrayOut byteOut = new ByteArrayOut((int) (4 + pointCount*(3*4 + 3*2)));
			byteOut.putIntRaw(pointCount);
			{
				double samplingPos = 0;
				for(PointTable p:pointTables) {
					int len = p.rows;
					double[] x = p.x;
					double[] y = p.y;
					double[] z = p.z;
					int i = (int) samplingPos;				
					while(i < len) {
						byteOut.putFloatRaw((float) x[i]);
						byteOut.putFloatRaw((float) z[i]);
						byteOut.putFloatRaw((float) y[i]);
						samplingPos += samplingFactor;
						i = (int) samplingPos;
					}
					samplingPos -= len;
				}
			}
			{
				double samplingPos = 0;
				for(PointTable p:pointTables) {
					int len = p.rows;
					char[] red = p.red == null ? fillChar(len) : p.red;
					char[] green = p.green == null ? fillChar(len) : p.green;
					char[] blue = p.blue == null ? fillChar(len) : p.blue;
					int i = (int) samplingPos;				
					while(i < len) {
						byteOut.putCharRaw(red[i]);
						byteOut.putCharRaw(green[i]);
						byteOut.putCharRaw(blue[i]);
						samplingPos += samplingFactor;
						i = (int) samplingPos;
					}
					samplingPos -= len;
				}
			}
			byteOut.flip(receiver.getOutputStream());
			break;
		}
		default:
			throw new RuntimeException("unknown js format: " + format);
		}
	}

	static void writeRdat(PointTable[] pointTables, Receiver receiver, AttributeSelector selector) throws IOException {
		AttributeSelector rdatSelector = selector == null ? PointTable.getSelector(pointTables) : selector;
		RdatWriter.writePoints(pointTables, rdatSelector, receiver);
	}
}
