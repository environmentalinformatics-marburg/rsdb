package server.api.pointdb;

import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.jetty.server.Response;

import pointdb.PointDB;
import pointdb.base.GeoPoint;
import util.collections.vec.Vec;

public class PointXyzWriter {
	
	public static void writePoints(PointDB pointdb, Response response, Vec<GeoPoint> points, String[] columns) throws IOException {
		response.setContentType("application/octet-stream");
		
		PrintWriter writer = response.getWriter();
		
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
