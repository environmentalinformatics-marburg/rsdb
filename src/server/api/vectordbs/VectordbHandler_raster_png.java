package server.api.vectordbs;

import java.io.IOException;
import java.nio.file.Path;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.ogr;

import broker.Broker;
import util.Util;
import util.Web;
import util.image.ImageBufferARGB;
import vectordb.Renderer;
import vectordb.VectorDB;

public class VectordbHandler_raster_png extends VectordbHandler {
	private static final Logger log = LogManager.getLogger();

	public VectordbHandler_raster_png(Broker broker) {
		super(broker, "raster.png");
	}

	@Override
	public void handleGET(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {		
		try {
			if(!vectordb.hasDataFilename()) {
				response.setStatus(HttpServletResponse.SC_NO_CONTENT);
				response.setContentType("text/plain;charset=utf-8");
				response.getWriter().print("no data");
				return;
			}
			
			int reqWidth = Web.getInt(request, "width", -1);
			int reqHeight = Web.getInt(request, "height", -1);

			ogr.RegisterAll();
			Path path = vectordb.getDataPath().resolve(vectordb.getDataFilename());
			Util.checkIsParent(vectordb.getDataPath(), path);
			String filename = path.toString();
			DataSource datasource = ogr.Open(filename);
			
			int maxWidth = reqWidth < 1 ? (reqHeight < 1 ? 100 : reqHeight * 10) : reqWidth;			
			int maxHeight = reqHeight < 1 ? (reqWidth < 1 ? 100 : reqWidth * 10) : reqHeight;			
			
			ImageBufferARGB image = Renderer.renderProportionalFullMaxSize(datasource, maxWidth, maxHeight, null, vectordb.getVectorStyle());

			/*double[] extent = VectorDB.getExtent(VectorDB.getPoints(datasource));
			log.info(Arrays.toString(extent));			
			

			double xoff = extent[0];
			double yoff = extent[1];
			log.info("xoff " + xoff +"  yoff " + yoff);
			double xlen = extent[2] - extent[0];
			double ylen = extent[3] - extent[1];
			log.info("xlen " + xlen +"  ylen " + ylen);
			double xres = xlen / width;
			double dheight = ylen / xres;
			int height = (int) Math.ceil(dheight);
			double yres = xres;
			log.info("xres " + xres +"  yres " + yres);
			log.info("width " + width +"  height " + height);

			Driver driver = gdal.GetDriverByName("MEM");			
			Dataset dataset = driver.Create("", width, height, 1, gdalconst.GDT_Byte);
			double[] geo = new double[]{xoff, xres, 0.0, yoff, 0.0, yres};
			dataset.SetGeoTransform(geo);
			log.info(Arrays.toString(dataset.GetGeoTransform()));
			
			boolean hasProjSet = false;

			int layerCount = datasource.GetLayerCount();
			log.info("datasource.GetLayerCount() " + layerCount);
			for(int layerIndex=0; layerIndex<layerCount; layerIndex++) {
				Layer layer = datasource.GetLayerByIndex(layerIndex);
				if(!hasProjSet) {
					SpatialReference spatialRef = layer.GetSpatialRef();
					String wkt = spatialRef.ExportToWkt();
					log.info(wkt);
					dataset.SetProjection(wkt);
					hasProjSet = true;
				}
				Vector<String> option = new Vector<String>();
				option.add("ALL_TOUCHED=TRUE");
				gdal.RasterizeLayer(dataset, new int[] {1}, layer, new double[]{255}, option);
			}			

			GdalReader gdalreader = new GdalReader(dataset);
			short[][] data = gdalreader.getDataShortOfByte(1, null);
			ShortFrame frame = new ShortFrame(data, 0, 0, 0, 0);
			ImageBufferARGB image = Renderer.renderGreyShort(frame, (short)0, frame.width, frame.height, 1, null);*/
			
			
			//image.writePngCompressed("out.png");
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("image/png");
			image.writePngCompressed(response.getOutputStream());			
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}		
	}
}
