package server.api.pointclouds;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import org.tinylog.Logger;

import pointcloud.AttributeSelector;
import pointcloud.LasWriter;
import pointcloud.LasWriter.LAS_HEADER;
import pointcloud.LasWriter.POINT_DATA_RECORD;
import pointcloud.PointTable;
import pointcloud.RdatWriter;
import util.ByteArrayOut;
import util.Receiver;
import util.Web;

public class PointTableWriter {

	//derived from https://stackoverflow.com/a/10554128
	private static final int POW10[] = {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000};
	private static String format(double val, int precision) {
		StringBuilder sb = new StringBuilder();
		if (val < 0) {
			sb.append('-');
			val = -val;
		}
		int exp = POW10[precision];
		long lval = (long)(val * exp + 0.5);
		sb.append(lval / exp);
		if(precision > 0) {
			sb.append('.');
			long fval = lval % exp;
			for (int p = precision - 1; p > 0 && fval < POW10[p]; p--) {
				sb.append('0');
			}
			sb.append(fval);
		}
		return sb.toString();
	}

	public static void writeXYZ(Stream<PointTable> pointTables, Receiver receiver) throws IOException {
		receiver.setContentType(Web.MIME_BINARY);
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

	private static class LimitReachedException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	public static void writeXYZ(Stream<PointTable> pointTables, Receiver receiver, long limit) throws IOException {
		receiver.setContentType(Web.MIME_BINARY);
		PrintWriter writer = receiver.getWriter();
		try {
			long[] count = new long[] {0};		
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
					count[0]++;
					if(limit <= count[0]) {
						throw new LimitReachedException();
					}
				}
			});
		} catch(LimitReachedException e) {
			// nothing
		}
	}

	private static abstract class PointWriter {

		public final String name;

		public PointWriter(String name) {
			this.name = name;
		}

		public abstract void set(PointTable pointTable);
		public abstract String write(int i);
	}

	private static class PointWriterFloat64 extends PointWriter {

		private double[] vs;
		private final int precision;
		private final Function<PointTable, double[]> mapper;		

		public PointWriterFloat64(String name, int precision, Function<PointTable, double[]> mapper) {
			super(name);
			this.precision = precision;
			this.mapper = mapper;
		}

		@Override
		public void set(PointTable pointTable) {
			double[] vs = mapper.apply(pointTable);
			this.vs = vs == null ? new double[pointTable.rows] : vs;
		}

		@Override
		public String write(int i) {
			return format(vs[i], precision);			
		}
	}

	private static class PointWriterUint16 extends PointWriter {

		private char[] vs;
		private final Function<PointTable, char[]> mapper;		

		public PointWriterUint16(String name, Function<PointTable, char[]> mapper) {
			super(name);
			this.mapper = mapper;
		}

		@Override
		public void set(PointTable pointTable) {
			char[] vs = mapper.apply(pointTable);
			this.vs = vs == null ? new char[pointTable.rows] : vs;
		}

		@Override
		public String write(int i) {
			return Integer.toString(vs[i]);	 
		}
	}

	private static class PointWriterUint8 extends PointWriter {

		private byte[] vs;
		private final Function<PointTable, byte[]> mapper;		

		public PointWriterUint8(String name, Function<PointTable, byte[]> mapper) {
			super(name);
			this.mapper = mapper;
		}

		@Override
		public void set(PointTable pointTable) {
			byte[] vs = mapper.apply(pointTable);
			this.vs = vs == null ? new byte[pointTable.rows] : vs;
		}

		@Override
		public String write(int i) {
			return Integer.toString(Byte.toUnsignedInt(vs[i]));		
		}
	}

	private static class PointWriterInt8 extends PointWriter {

		private byte[] vs;
		private final Function<PointTable, byte[]> mapper;		

		public PointWriterInt8(String name, Function<PointTable, byte[]> mapper) {
			super(name);
			this.mapper = mapper;
		}

		@Override
		public void set(PointTable pointTable) {
			byte[] vs = mapper.apply(pointTable);
			this.vs = vs == null ? new byte[pointTable.rows] : vs;
		}

		@Override
		public String write(int i) {
			return Integer.toString(vs[i]);		
		}
	}

	private static class PointWriterUint1 extends PointWriter {

		private java.util.BitSet vs;
		private final Function<PointTable, java.util.BitSet> mapper;		

		public PointWriterUint1(String name, Function<PointTable, java.util.BitSet> mapper) {
			super(name);
			this.mapper = mapper;
		}

		@Override
		public void set(PointTable pointTable) {
			java.util.BitSet vs = mapper.apply(pointTable);
			this.vs = vs == null ? new java.util.BitSet() : vs;
		}

		@Override
		public String write(int i) {
			return vs.get(i) ? "1" : "0";	
		}
	}

	private static class PointWriterUint64 extends PointWriter {

		private long[] vs;
		private final Function<PointTable, long[]> mapper;		

		public PointWriterUint64(String name, Function<PointTable, long[]> mapper) {
			super(name);
			this.mapper = mapper;
		}

		@Override
		public void set(PointTable pointTable) {
			long[] vs = mapper.apply(pointTable);
			this.vs = vs == null ? new long[pointTable.rows] : vs;
		}

		@Override
		public String write(int i) {
			return Long.toUnsignedString(vs[i]);	
		}
	}

	private static PointWriter createColumn(String name, int precision) {
		switch(name) {
		case "x":
			return new PointWriterFloat64("x", precision, PointTable::get_x);
		case "y":
			return new PointWriterFloat64("y", precision, PointTable::get_y);
		case "z":
			return new PointWriterFloat64("z", precision, PointTable::get_z);
		case "intensity":
			return new PointWriterUint16("intensity", PointTable::get_intensity);
		case "returnNumber":
			return new PointWriterUint8("returnNumber", PointTable::get_returnNumber);
		case "returns":
			return new PointWriterUint8("returns", PointTable::get_returns);
		case "scanDirectionFlag":
			return new PointWriterUint1("scanDirectionFlag", PointTable::get_scanDirectionFlag);
		case "edgeOfFlightLine":
			return new PointWriterUint1("edgeOfFlightLine", PointTable::get_edgeOfFlightLine);
		case "classification":
			return new PointWriterUint8("classification", PointTable::get_classification);
		case "scanAngleRank":
			return new PointWriterInt8("scanAngleRank", PointTable::get_scanAngleRank);
		case "gpsTime":
			return new PointWriterUint64("gpsTime", PointTable::get_gpsTime);
		case "red":
			return new PointWriterUint16("red", PointTable::get_red);
		case "green":
			return new PointWriterUint16("green", PointTable::get_green);
		case "blue":
			return new PointWriterUint16("blue", PointTable::get_blue);
		default:
			throw new RuntimeException("unknown column name: " + name);
		}
	}

	public static void writeCSV(Stream<PointTable> pointTables, Receiver receiver, String[] columnNames, String separator,  int precision, long limit) throws IOException {
		receiver.setContentType(Web.MIME_BINARY);
		PrintWriter writer = receiver.getWriter();
		try {
			long[] count = new long[] {0};

			PointWriter[] columns = new PointWriter[columnNames.length];
			for (int i = 0; i < columnNames.length; i++) {
				columns[i] = createColumn(columnNames[i], precision);
			}

			writer.print(columns[0].name);
			for (int c = 1; c < columns.length; c++) {
				writer.print(separator);
				writer.print(columns[c].name);
			}
			writer.println();

			pointTables.sequential().forEach(pointTable -> {				
				int len = pointTable.rows;
				for(PointWriter column : columns) {
					column.set(pointTable);
				}
				for (int i = 0; i < len; i++) {					
					writer.print(columns[0].write(i));
					for (int c = 1; c < columns.length; c++) {
						writer.print(separator);
						writer.print(columns[c].write(i));
					}
					writer.println();
					count[0]++;
					if(limit <= count[0]) {
						throw new LimitReachedException();
					}
				}
			});
		} catch(LimitReachedException e) {
			// nothing
		}
	}

	static void writeLAS(PointTable[] pointTables, Receiver receiver) throws IOException {
		LasWriter.writePoints(pointTables, receiver, LAS_HEADER.V_1_2, POINT_DATA_RECORD.FORMAT_0);
	}

	private static char[] fillChar(int len) {
		char[] a = new char[len];
		Arrays.fill(a, Character.MAX_VALUE);
		return a;
	}

	private static void writeJsAllPoints(PointTable[] pointTables, Receiver receiver, String format, int pointCount) throws IOException {
		receiver.setContentType(Web.MIME_BINARY);
		switch(format) {
		case "xzy": {
			Logger.info("write in points in format xzy");
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
			Logger.info("write in points in format xzy_classification");
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
			Logger.info("write in points in format xzy_rgb");
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

	static void writeJs(PointTable[] pointTables, String data_format, Receiver receiver) throws IOException {
		Logger.info("writeJs");
		int maxPoints = 5_500_000;
		int maxSamplePoints = 5_000_000;		
		if(data_format == null) {
			data_format = "xzy_classification";
		}
		int pointCount = 0;
		for(PointTable p:pointTables) {
			pointCount += p.rows;
		}
		boolean useAllPoints = pointCount <= maxPoints;
		if(useAllPoints) {
			writeJsAllPoints(pointTables, receiver, data_format, pointCount);
		} else {
			double samplingFactor = ((double)pointCount) / maxSamplePoints;
			Logger.info("sample " + maxSamplePoints + " of " + pointCount+ " sampling factor " + samplingFactor);
			writeJsSamplePoints(pointTables, receiver, data_format, samplingFactor);
		}		
	}

	private static void writeJsSamplePoints(PointTable[] pointTables, Receiver receiver, String format, double samplingFactor) throws IOException {
		Logger.info("writeJsSamplePoints");
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
		Logger.info("samples "+samplePointCount);
		int pointCount = samplePointCount;
		receiver.setContentType(Web.MIME_BINARY);
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
