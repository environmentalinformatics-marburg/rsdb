package server.api.rasterdb;

import java.io.DataOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import rasterdb.BandProcessor;
import rasterdb.GeoReference;
import rasterdb.TimeBandProcessor;
import server.api.rasterdb.RequestProcessorBands.Receiver;
import util.Range2d;
import util.TimeUtil;
import util.frame.DoubleFrame;
import util.image.ImageBufferARGB;
import util.rdat.RdatBand;
import util.rdat.RdatList;
import util.rdat.RdatWriter;
import util.tiff.TiffBand;
import util.tiff.TiffComposite;
import util.tiff.TiffWriter;

public class RequestProcessorProductWriters {
	//private static final Logger log = LogManager.getLogger();	

	public static void writeRdat(DoubleFrame[] doubleFrames, BandProcessor processor, String productText, Receiver resceiver) throws IOException {
		GeoReference ref = processor.rasterdb.ref();
		Range2d dstRange = processor.getDstRange();
		int dstWidth = dstRange.getWidth();
		int dstHeight = dstRange.getHeight();
		int dstDiv = processor.getScale();
		double dstGeoXmin = ref.pixelXdivToGeo(dstDiv, dstRange.xmin);
		double dstGeoYmin = ref.pixelYdivToGeo(dstDiv, dstRange.ymin);
		double dstGeoXmax = ref.pixelXdivToGeo(dstDiv, dstRange.xmax + 1); // extent of pixels
		double dstGeoYmax = ref.pixelYdivToGeo(dstDiv, dstRange.ymax + 1); // extent of pixels	
		
		RdatList meta = new RdatList();
		meta.addString("source", processor.rasterdb.config.getName());
		meta.addString("timestamp", TimeUtil.toPrettyText(processor.timestamp));
		meta.addString("product", productText);	
		RdatWriter rdatWriter = new RdatWriter(dstWidth, dstHeight, dstGeoXmin, dstGeoYmin, dstGeoXmax, dstGeoYmax, meta);
		if(ref.has_proj4()) {
			rdatWriter.setProj4(ref.proj4);
		}
		//Short noDataValue = null;
		for(DoubleFrame doubleFrame : doubleFrames) {
			RdatList bandMeta = new RdatList();
			bandMeta.addAll(doubleFrame.meta);
			rdatWriter.addRdatBand(RdatBand.ofFloat64(dstWidth, dstHeight, bandMeta, doubleFrame.data));
		}
		//rdatWriter.setNoDataValue(noDataValue);
		resceiver.setStatus(HttpServletResponse.SC_OK);
		resceiver.setContentType("application/octet-stream");
		rdatWriter.write(new DataOutputStream(resceiver.getOutputStream()));
	}

	public static void writeTiff(DoubleFrame[] doubleFrames, BandProcessor processor, String productText, Receiver resceiver) throws IOException {
		GeoReference ref = processor.rasterdb.ref();
		Range2d dstRange = processor.getDstRange();
		int dstWidth = dstRange.getWidth();
		int dstHeight = dstRange.getHeight();
		int dstDiv = processor.getScale();
		double dstGeoXmin = ref.pixelXdivToGeo(dstDiv, dstRange.xmin);
		double dstGeoYmin = ref.pixelYdivToGeo(dstDiv, dstRange.ymin);
		double dstPixelSizeX = ref.getPixelSizeXdiv(dstDiv);
		double dstPixelSizeY = ref.getPixelSizeYdiv(dstDiv);
		TiffWriter tiffWriter = new TiffWriter(dstWidth, dstHeight, dstGeoXmin, dstGeoYmin, dstPixelSizeX, dstPixelSizeY, (short)ref.getEPSG(0));

		//Short noDataValue = null;
		for(DoubleFrame doubleFrame : doubleFrames) {
			tiffWriter.addTiffBand(TiffBand.ofFloat64(dstWidth, dstHeight, doubleFrame.data));
		}
		//tiffWriter.setNoDataValue(noDataValue);
		resceiver.setStatus(HttpServletResponse.SC_OK);
		resceiver.setContentType("image/tiff");
		tiffWriter.writeTIFF(new DataOutputStream(resceiver.getOutputStream()));
	}

	public static void writeImagePng(ImageBufferARGB image, TimeBandProcessor processor, String productText, Receiver resceiver) throws IOException {
		resceiver.setStatus(HttpServletResponse.SC_OK);
		resceiver.setContentType("image/png");
		image.writePngCompressed(resceiver.getOutputStream());
	}
	
	public static void writeImageJpg(ImageBufferARGB image, TimeBandProcessor processor, String productText, Receiver resceiver) throws IOException {
		resceiver.setStatus(HttpServletResponse.SC_OK);
		resceiver.setContentType("image/jpeg");
		image.writeJpg(resceiver.getOutputStream(), 0.7f);
	}

	public static void writeImageTiff(ImageBufferARGB image, TimeBandProcessor processor, String productText, Receiver resceiver) throws IOException {
		GeoReference ref = processor.rasterdb.ref();
		Range2d dstRange = processor.getDstRange();
		int dstWidth = dstRange.getWidth();
		int dstHeight = dstRange.getHeight();
		int dstDiv = processor.getScale();
		double dstGeoXmin = ref.pixelXdivToGeo(dstDiv, dstRange.xmin);
		double dstGeoYmin = ref.pixelYdivToGeo(dstDiv, dstRange.ymin);
		double dstPixelSizeX = ref.getPixelSizeXdiv(dstDiv);
		double dstPixelSizeY = ref.getPixelSizeYdiv(dstDiv);
		TiffWriter tiffWriter = new TiffWriter(dstWidth, dstHeight, dstGeoXmin, dstGeoYmin, dstPixelSizeX, dstPixelSizeY, (short)ref.getEPSG(0));
		tiffWriter.set_photometricInterpretationType_RGB();
		tiffWriter.set_extraSamples_One_Alpha();
		TiffComposite composite = TiffComposite.ofImageBufferARGB(image);
		tiffWriter.setTiffComposite(composite);
		resceiver.setStatus(HttpServletResponse.SC_OK);
		resceiver.setContentType("image/tiff");
		tiffWriter.writeAuto(new DataOutputStream(resceiver.getOutputStream()));		
	}

	public static void writeImage(ImageBufferARGB image, String outputType, TimeBandProcessor processor, String productText, Receiver resceiver) throws IOException {
		switch(outputType) {
		case "png":
			RequestProcessorProductWriters.writeImagePng(image, processor, productText, resceiver);	
			break;
		case "tiff":
			RequestProcessorProductWriters.writeImageTiff(image, processor, productText, resceiver);
			break;
		case "jpg":
			RequestProcessorProductWriters.writeImageJpg(image, processor, productText, resceiver);	
			break;
		default:
			throw new RuntimeException("unknown output: " + outputType);
		}		
	}

}
