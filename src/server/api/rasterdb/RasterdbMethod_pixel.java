package server.api.rasterdb;

import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

import broker.Broker;
import rasterdb.Band;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.TimeBandProcessor;
import util.Range2d;
import util.Web;
import util.frame.DoubleFrame;

public class RasterdbMethod_pixel extends RasterdbMethod {
	//

	public RasterdbMethod_pixel(Broker broker) {
		super(broker, "pixel");	
	}

	@Override
	public void handle(RasterDB rasterdb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {
		double geoX = Web.getDouble(request, "x", Double.NaN);
		double geoY = Web.getDouble(request, "y", Double.NaN);
		int b = Web.getInt(request, "b", -1);
		int t = 0;		
		int scaleDiv = 1;		
		GeoReference ref = rasterdb.ref();
		int lx = ref.geoXToPixel(geoX);
		int ly = ref.geoYToPixel(geoY);
		Range2d range2d = new Range2d(lx, ly, lx, ly);
		TimeBandProcessor processor = new TimeBandProcessor(rasterdb, range2d, scaleDiv);
		Band band = rasterdb.getBandByNumber(b);
		DoubleFrame doubleFrame = processor.getDoubleFrame(t, band);
		double[][] data = doubleFrame.data;
		PrintWriter out = response.getWriter();
		out.println(doubleFrame.local_min_x + "  " + doubleFrame.local_min_y);
		out.println(ref.pixelXToGeo(doubleFrame.local_min_x) + "  " + ref.pixelYToGeo(doubleFrame.local_min_y));
		out.println(ref.pixelXToGeo(doubleFrame.local_min_x + 0.99999d) + "  " + ref.pixelYToGeo(doubleFrame.local_min_y + 0.99999d));
		out.println(data[0].length + "  " + data.length);
		out.println(data[0][0] +"  " + rasterdb.config.getName());
	}

}