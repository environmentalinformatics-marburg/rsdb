package server.api.vectordbs;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.gdal.ogr.DataSource;
import org.gdal.ogr.Feature;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.Layer;
import org.locationtech.jts.io.WKBReader;
import org.tinylog.Logger;

import pointcloud.Rect2d;
import postgis.style.StyleJtsGeometryRasterizer;
import util.GeoUtil.Transformer;
import util.image.ImageBufferARGB;
import vectordb.style.Style;

public class ConverterRenderer {

	public static ImageBufferARGB render(DataSource datasource, Object sync, Rect2d renderRect, int width, int height, String labelField, Style style, Transformer layerRenderTransformer) {
		ImageBufferARGB image = new ImageBufferARGB(width, height);
		Graphics2D gc = image.bufferedImage.createGraphics();
		gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gc.setColor(Color.DARK_GRAY);

		double xlen = renderRect.width();
		double ylen = renderRect.height();
		double xscale = (width - 4) / xlen;
		double yscale = (height - 4) / ylen;
		double xoff = - renderRect.xmin + 2 * (1 / xscale);
		double yoff = renderRect.ymax + 2 * (1 / yscale);

		StyleJtsGeometryRasterizer styleJtsGeometryRasterizer = new StyleJtsGeometryRasterizer(xoff, yoff, xscale, yscale, gc);

		WKBReader wkbReader = new WKBReader();
		int layerCount = datasource.GetLayerCount();
		for(int layerIndex=0; layerIndex<layerCount; layerIndex++) {
			Layer layer = datasource.GetLayerByIndex(layerIndex);
			synchronized (sync) {
				layer.SetSpatialFilter(null); // clear filter
				layer.ResetReading(); // reset iterator
				Feature feature = layer.GetNextFeature();
				while(feature != null) {
					try {
						Geometry geometry = feature.GetGeometryRef();			
						if(geometry != null) {
							if(layerRenderTransformer != null) {
								geometry.Transform(layerRenderTransformer.coordinateTransformation);
							}
							byte[] wkb = geometry.ExportToWkb();						
							if(wkb != null) {								
								org.locationtech.jts.geom.Geometry jtsGeometry = wkbReader.read(wkb);	
								String label = null;
								if(labelField != null) {
									label = feature.GetFieldAsString(labelField);
									if(label != null && label.isBlank()) {
										label = null;
									}
								}								
								//Logger.info(jtsGeometry.getClass() + "   " + label);	
								styleJtsGeometryRasterizer.acceptGeometry(style, jtsGeometry, label);								
							}
						} else {
							Logger.warn("missing geometry");
						}
					} catch(Exception e) {
						Logger.warn(e);
					}
					feature = layer.GetNextFeature();
				}
			}
		}
		styleJtsGeometryRasterizer.drawLabels(style);

		gc.dispose();
		return image;
	}

}
