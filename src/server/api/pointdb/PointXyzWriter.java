package server.api.pointdb;

import java.io.IOException;
import java.io.PrintWriter;

import pointdb.base.GeoPoint;
import util.Receiver;
import util.collections.vec.Vec;

public class PointXyzWriter {
	
	public static void writePoints(Receiver receiver, Vec<GeoPoint> points, String[] columns) throws IOException {
		receiver.setContentType("application/octet-stream");
		
		PrintWriter writer = receiver.getWriter();
		
		for(GeoPoint p:points) {
			writer.print(p.x);
			writer.print(' ');
			writer.print(p.y);
			writer.print(' ');
			writer.print(p.z);
			writer.println();
		}
	}

}
