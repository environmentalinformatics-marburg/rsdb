package server.api.pointclouds;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;

import broker.Broker;
import pointcloud.AttributeSelector;
import pointcloud.CellTable;
import pointcloud.CellTable.ChainedFilterFunc;
import pointcloud.LasWriter;
import pointcloud.LasWriter.LAS_HEADER;
import pointcloud.LasWriter.POINT_DATA_RECORD;
import pointcloud.PointCloud;
import pointcloud.PointTable;
import pointcloud.PointTable.FilterByPolygonFunc;
import pointcloud.RdatWriter;
import util.ByteArrayOut;
import util.Receiver;
import util.ResponseReceiver;

public class APIHandler_points {
	private static final Logger log = LogManager.getLogger();

	protected static final String MIME_JSON = "application/json";	
	protected static final String MIME_CSV = "text/csv";

	//private final Broker broker;

	public APIHandler_points(Broker broker) {
		//this.broker = broker;
	}

	public void handle(PointCloud pointcloud, String format, Request request, HttpServletResponse response) throws IOException {
		String extText = request.getParameter("ext");
		String polygonText = request.getParameter("polygon");
		double xmin = Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;
		double xmax = -Double.MAX_VALUE;
		double ymax = -Double.MAX_VALUE;
		double[] vx = null;
		double[] vy = null;
		if(polygonText != null) {
			if(extText != null) {
				throw new RuntimeException("If 'polygon' parameter is present, parameter 'ext' can not be specified.");
			}
			String[] cTexts = polygonText.split(" ");
			int len = cTexts.length;
			if(len % 2 != 0) {
				throw new RuntimeException();
			}
			if(len < 6) {
				throw new RuntimeException();
			}
			int vlen = len / 2;
			vx = new double[vlen];
			vy = new double[vlen];
			for (int i = 0; i < vlen; i++) {
				vx[i] = Double.parseDouble(cTexts[i]);
				if(vx[i]<xmin) xmin = vx[i];
				if(vx[i]>xmax) xmax = vx[i];
			}
			for (int i = 0; i < vlen; i++) {
				vy[i] = Double.parseDouble(cTexts[i + vlen]);
				if(vy[i]<ymin) ymin = vy[i];
				if(vy[i]>ymax) ymax = vy[i];
			}
		} else if(extText != null) {
			String[] ext = extText.split(" ");
			if(ext.length != 4) {
				throw new RuntimeException("parameter error in 'ext': "+extText);
			}
			xmin = Double.parseDouble(ext[0]);
			ymin = Double.parseDouble(ext[1]);
			xmax = Double.parseDouble(ext[2]);
			ymax = Double.parseDouble(ext[3]);
		} else {
			throw new RuntimeException("missing parameter 'ext'");
		}

		String columnsText = request.getParameter("columns");
		AttributeSelector selector = columnsText == null ? null : AttributeSelector.of(columnsText.split("\\s+"));
		ChainedFilterFunc filterFunc = CellTable.parseFilter(request.getParameter("filter"));
		Stream<PointTable> pointTables = pointcloud.getPointTables(xmin, ymin, xmax, ymax, selector == null ? new AttributeSelector(true) : selector, filterFunc);

		if(vx != null && vy != null) {
			FilterByPolygonFunc filterByPolygonFunc = PointTable.getFilterByPolygonFunc(vx, vy);
			pointTables = pointTables.map(pointTable -> PointTable.applyMask(pointTable, filterByPolygonFunc.apply(pointTable)));
		}

		ResponseReceiver receiver = new ResponseReceiver(response);
		switch(format) {
		case "xyz": {
			writeXYZ(pointTables, receiver);
			break;
		}
		case "las": {
			writeLAS(pointTables.toArray(PointTable[]::new), receiver);
			break;
		}
		case "js": {
			writeJS(pointTables.toArray(PointTable[]::new), request, receiver);
			break;
		}
		case "rdat": {
			writeRdat(pointTables.toArray(PointTable[]::new), receiver, selector);
			break;
		}
		default:
			throw new RuntimeException("unknown format: "+format);
		}
	}

	private void writeXYZ(Stream<PointTable> pointTables, ResponseReceiver receiver) throws IOException {
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

	private void writeLAS(PointTable[] pointTables, ResponseReceiver receiver) throws IOException {
		LasWriter.writePoints(pointTables, receiver, LAS_HEADER.V_1_2, POINT_DATA_RECORD.FORMAT_0);
	}

	private void writeJS(PointTable[] pointTables, Request request, Receiver receiver) throws IOException {
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

	private void writeSamplePoints(PointTable[] pointTables, Receiver receiver, String format, double samplingFactor) throws IOException {
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

	private void writeAllPoints(PointTable[] pointTables, Receiver receiver, String format, int pointCount) throws IOException {
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

	private static char[] fillChar(int len) {
		char[] a = new char[len];
		Arrays.fill(a, Character.MAX_VALUE);
		return a;
	}

	private void writeRdat(PointTable[] pointTables, ResponseReceiver receiver, AttributeSelector selector) throws IOException {
		AttributeSelector rdatSelector = selector == null ? PointTable.getSelector(pointTables) : selector;
		RdatWriter.writePoints(pointTables, rdatSelector, receiver);
	}

}
