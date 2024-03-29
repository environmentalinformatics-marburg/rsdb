package server.api.vectordbs;

import java.io.IOException;
import java.nio.file.Path;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
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
import vectordb.style.Style;

public class VectordbHandler_raster_png extends VectordbHandler {	

	public VectordbHandler_raster_png(Broker broker) {
		super(broker, "raster.png");
	}

	@Override
	public void handleGET(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {		
		try {
			if(!vectordb.hasDataFilename()) {
				response.setStatus(HttpServletResponse.SC_NO_CONTENT);
				response.setContentType(Web.MIME_TEXT);
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
			
			//ImageBufferARGB image = Renderer.renderProportionalFullMaxSize(datasource, this, maxWidth, maxHeight, null, vectordb.getStyle());
			
			Style style = vectordb.getStyle();
			if(style == null) {
				style = Renderer.STYLE_DEFAULT;
			}
			ImageBufferARGB image = ConverterRenderer.renderProportionalFullMaxSize(datasource, vectordb, maxWidth, maxHeight, null, style, null, false);

			/*double[] extent = VectorDB.getExtent(VectorDB.getPoints(datasource));
			Logger.info(Arrays.toString(extent));			
			

			double xoff = extent[0];
			double yoff = extent[1];
			Logger.info("xoff " + xoff +"  yoff " + yoff);
			double xlen = extent[2] - extent[0];
			double ylen = extent[3] - extent[1];
			Logger.info("xlen " + xlen +"  ylen " + ylen);
			double xres = xlen / width;
			double dheight = ylen / xres;
			int height = (int) Math.ceil(dheight);
			double yres = xres;
			Logger.info("xres " + xres +"  yres " + yres);
			Logger.info("width " + width +"  height " + height);

			Driver driver = gdal.GetDriverByName("MEM");			
			Dataset dataset = driver.Create("", width, height, 1, gdalconst.GDT_Byte);
			double[] geo = new double[]{xoff, xres, 0.0, yoff, 0.0, yres};
			dataset.SetGeoTransform(geo);
			Logger.info(Arrays.toString(dataset.GetGeoTransform()));
			
			boolean hasProjSet = false;

			int layerCount = datasource.GetLayerCount();
			Logger.info("datasource.GetLayerCount() " + layerCount);
			for(int layerIndex=0; layerIndex<layerCount; layerIndex++) {
				Layer layer = datasource.GetLayerByIndex(layerIndex);
				if(!hasProjSet) {
					SpatialReference spatialRef = layer.GetSpatialRef();
					String wkt = spatialRef.ExportToWkt();
					Logger.info(wkt);
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
			response.setContentType(Web.MIME_PNG);
			image.writePngCompressed(response.getOutputStream());			
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}		
	}
}
