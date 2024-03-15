package server.api.vectordbs;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.gdal.ogr.DataSource;
import org.gdal.ogr.Feature;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.Layer;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.io.WKBReader;
import org.tinylog.Logger;

import pointcloud.Rect2d;
import postgis.style.StyleJtsGeometryRasterizer;
import util.GeoUtil.Transformer;
import util.image.ImageBufferARGB;
import vectordb.VectorDB;
import vectordb.style.Style;

public class ConverterRenderer {

	private static final CoordinateSequenceFilter SWAP_COORDINATES = new CoordinateSequenceFilter() {									
		@Override
		public boolean isGeometryChanged() {
			return true;
		}

		@Override
		public boolean isDone() {
			return false;
		}

		@Override
		public void filter(CoordinateSequence seq, int i) {
			double x = seq.getX(i);
			double y = seq.getY(i);
			seq.setOrdinate(i, 0, y);
			seq.setOrdinate(i, 1, x);
		}
	};
	
	public static ImageBufferARGB renderProportionalFullMaxSize(DataSource datasource, VectorDB sync, int maxWidth, int maxHeight, String labelField, Style style, Transformer layerRenderTransformer, boolean swapCoordinates) {
		Rect2d renderRect = VectorDB.getExtent(VectorDB.getPoints(datasource, sync));
		return renderProportionalFullMaxSize(datasource, sync, renderRect, maxWidth, maxHeight, labelField, style, layerRenderTransformer, swapCoordinates);
	}
	
	public static ImageBufferARGB renderProportionalFullMaxSize(DataSource datasource, VectorDB sync, Rect2d renderRect, int maxWidth, int maxHeight, String labelField, Style style, Transformer layerRenderTransformer, boolean swapCoordinates) {
		double xlen = renderRect.width();
		double ylen = renderRect.height();

		double xTargetScale = maxWidth / xlen;
		double yTargetScale = maxHeight / ylen;

		double targetScale = Math.min(xTargetScale, yTargetScale);

		int width = (int) Math.ceil(targetScale * xlen);
		int height = (int) Math.ceil(targetScale * ylen);

		Logger.info(maxWidth + " x " + maxHeight + " -> " + width + " x " + height);
		return render(datasource, sync, renderRect, width, height, labelField, style, layerRenderTransformer, swapCoordinates);
	}

	public static ImageBufferARGB render(DataSource datasource, VectorDB sync, Rect2d renderRect, int width, int height, String labelField, Style style, Transformer layerRenderTransformer, boolean swapCoordinates) {
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
				int missingGeometries = 0;
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
								if(swapCoordinates) {
									jtsGeometry.apply(SWAP_COORDINATES);
								}
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
							missingGeometries++;
						}
					} catch(Exception e) {
						Logger.warn(e.getMessage());
					}
					feature = layer.GetNextFeature();
				}
				if(missingGeometries > 0) {
					Logger.info(missingGeometries + " missing geometries of feature from vectordb " + sync.getName());					
				}
			}
		}
		styleJtsGeometryRasterizer.drawLabels(style);

		gc.dispose();
		return image;
	}

}
