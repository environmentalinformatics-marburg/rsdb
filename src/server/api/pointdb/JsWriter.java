package server.api.pointdb;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.tinylog.Logger;

import pointdb.base.GeoPoint;
import pointdb.processing.geopoint.RasterSubGrid;
import util.ByteArrayOut;
import util.Receiver;
import util.Web;
import util.collections.vec.Vec;

public class JsWriter {

	public static void writePoints(Receiver receiver, Vec<GeoPoint> result, String[] columns) throws IOException {
		Logger.info("JsWriter writePoints " + Arrays.toString(columns));

		if(columns != null && columns.length == 3 && "x".equals(columns[0]) && "y".equals(columns[1]) && "z".equals(columns[2])) {
			writePoints__xzy(receiver, result);		
		} else if(columns != null && columns.length == 4 && "x".equals(columns[0]) && "y".equals(columns[1]) && "z".equals(columns[2]) && "classification".equals(columns[3])) {
			writePoints__xzy_classification(receiver, result);		
		} else {
			throw new RuntimeException("unknown point format: " + Arrays.toString(columns));
		}
	}	

	public static void writePoints__xzy(Receiver receiver, Vec<GeoPoint> result) throws IOException {
		int n = result.size();
		ByteArrayOut out = ByteArrayOut.of(4+n*3*4);

		out.putIntRaw(n);
		for(GeoPoint p:result) {
			out.putFloatRaw(p.x);
			out.putFloatRaw(p.z);
			out.putFloatRaw(p.y);
		}

		receiver.setContentType(Web.MIME_BINARY);
		OutputStream stream = receiver.getOutputStream();
		out.flip(stream);
	}

	public static void writePoints__xzy_classification(Receiver receiver, Vec<GeoPoint> result) throws IOException {
		int n = result.size();
		ByteArrayOut out = ByteArrayOut.of(4+n*3*4+n);

		out.putIntRaw(n);
		for(GeoPoint p:result) {
			out.putFloatRaw(p.x);
			out.putFloatRaw(p.z);
			out.putFloatRaw(p.y);
		}

		for(GeoPoint p:result) {
			out.putByteRaw(p.classification);
		}

		receiver.setContentType(Web.MIME_BINARY);
		OutputStream stream = receiver.getOutputStream();
		out.flip(stream);
	}

	public static void writeRaster(Receiver receiver, RasterSubGrid rasterGrid) throws IOException {
		double[][] data = rasterGrid.data;
		int xStart = rasterGrid.start_x;
		int yStart = rasterGrid.start_y;
		int xBorder = rasterGrid.border_x;
		int yBorder = rasterGrid.border_y;
		int xLen = xBorder - xStart;
		int yLen = yBorder - yStart;
		ByteArrayOut out = ByteArrayOut.of(xLen*yLen*4+2*4);
		out.putIntRaw(xLen);
		out.putIntRaw(yLen);
		out.putFloats2dBorderedRaw(data, yStart, yBorder, xStart, xBorder);
		receiver.setContentType(Web.MIME_BINARY);
		OutputStream stream = receiver.getOutputStream();
		out.flip(stream);
	}

	public static void writeFloat2d(int[][] data, Receiver receiver) throws IOException {
		int xLen = data[0].length;
		int yLen = data.length;
		ByteArrayOut out = ByteArrayOut.of(xLen*yLen*4+2*4);
		Logger.info("writeFloat2d" + xLen +"  "  + yLen  +"  " + (xLen*yLen*4+2*4) + "  " + out.buf.length);
		out.putIntRaw(xLen);
		out.putIntRaw(yLen);
		out.putFloats2d(data);
		receiver.setContentType(Web.MIME_BINARY);
		OutputStream stream = receiver.getOutputStream();
		out.flip(stream);
	}

	public static void writeFloat2d(double[][] data, Receiver receiver) throws IOException {
		int xLen = data[0].length;
		int yLen = data.length;
		ByteArrayOut out = ByteArrayOut.of(xLen*yLen*4+2*4);
		Logger.info("writeFloat2d" + xLen +"  "  + yLen  +"  " + (xLen*yLen*4+2*4) + "  " + out.buf.length);
		out.putIntRaw(xLen);
		out.putIntRaw(yLen);
		out.putFloats2d(data);
		receiver.setContentType(Web.MIME_BINARY);
		OutputStream stream = receiver.getOutputStream();
		out.flip(stream);
	}

}
