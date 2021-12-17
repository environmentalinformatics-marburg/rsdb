package server.api.pointdb;

import java.io.IOException;
import java.io.OutputStream;


import org.tinylog.Logger;

import pointdb.base.GeoPoint;
import pointdb.processing.geopoint.RasterSubGrid;
import util.ByteArrayOut;
import util.Receiver;
import util.collections.vec.Vec;

public class JsWriter {
	

	public static void writePoints(Receiver receiver, Vec<GeoPoint> result, String[] columns) throws IOException {

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

		receiver.setContentType("application/octet-stream");
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
		receiver.setContentType("application/octet-stream");
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
		receiver.setContentType("application/octet-stream");
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
		receiver.setContentType("application/octet-stream");
		OutputStream stream = receiver.getOutputStream();
		out.flip(stream);
	}

}
