package server.api.rasterdb;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import rasterdb.GeoReference;
import rasterdb.TilePixel;
import rasterdb.TimeBand;
import rasterdb.TimeBandProcessor;
import server.api.rasterdb.RequestProcessor.RdatDataType;
import server.api.rasterdb.RequestProcessor.TiffDataType;
import server.api.rasterdb.RequestProcessorBands.Receiver;
import util.Range2d;
import util.TimeUtil;
import util.rdat.RdatBand;
import util.rdat.RdatList;
import util.rdat.RdatWriter;
import util.tiff.TiffBand;
import util.tiff.TiffWriter;

public class RequestProcessorBandsWriters {
	//private static final Logger log = LogManager.getLogger();

	public static void writeRdat(TimeBandProcessor processor, Collection<TimeBand> processingBands, Receiver resceiver) throws IOException {
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
		meta.addString("source", "rasterdb " + processor.rasterdb.config.getName());
		int t = -1;
		for(TimeBand timeband : processingBands) {
			if(t == -1) {
				t = timeband.timestamp;
			} else if(t >= 0 && t != timeband.timestamp) {
				t = -2;
			}
		}
		if(t >= 0) {
			meta.addString("timestamp", TimeUtil.toPrettyText(t));
		}
		RdatWriter rdatWriter = new RdatWriter(dstWidth, dstHeight, dstGeoXmin, dstGeoYmin, dstGeoXmax, dstGeoYmax, meta);
		if(ref.has_proj4()) {
			rdatWriter.setProj4(ref.proj4);
		}
		Short noDataValue = null;

		RdatDataType dataType = RdatDataType.INT16;
		for(TimeBand timeband : processingBands) {
			switch (timeband.band.type) {
			case TilePixel.TYPE_SHORT:
				// nothing
				break;
			case TilePixel.TYPE_FLOAT:
				if(dataType == RdatDataType.INT16) {
					dataType = RdatDataType.FLOAT32;
				}
				break;
			default:
				dataType = RdatDataType.FLOAT64;
				break;
			}
		}
		for(TimeBand timeband : processingBands) {
			RdatList bandMeta = new RdatList();
			bandMeta.addInteger("index", timeband.band.index);
			bandMeta.addString("name", timeband.band.has_title() ? timeband.band.title : "band" + timeband.band.index);
			if (timeband.band.has_wavelength()) {
				bandMeta.addDouble("wavelength", timeband.band.wavelength);
				if (timeband.band.has_fwhm()) {
					bandMeta.addDouble("fwhm", timeband.band.fwhm);
				}
			}
			switch(dataType) {
			case INT16:
				rdatWriter.addRdatBand(RdatBand.ofInt16(dstWidth, dstHeight, bandMeta, ()->processor.getShortFrame(timeband).data));
				if(noDataValue == null) {
					noDataValue = timeband.band.getShortNA();
				}
				break;
			case FLOAT32:
				rdatWriter.addRdatBand(RdatBand.ofFloat32(dstWidth, dstHeight, bandMeta, ()->processor.getFloatFrame(timeband).data));
				break;
			case FLOAT64:
				rdatWriter.addRdatBand(RdatBand.ofFloat64(dstWidth, dstHeight, bandMeta, ()->processor.getDoubleFrame(timeband).data));
				break;
			default:
				throw new RuntimeException("unknown data type");
			}
		}
		rdatWriter.setNoDataValue(noDataValue);
		resceiver.setStatus(HttpServletResponse.SC_OK);
		resceiver.setContentType("application/octet-stream");
		rdatWriter.write(new DataOutputStream(resceiver.getOutputStream()));		
	}

	public static void writeTiff(TimeBandProcessor processor, Collection<TimeBand> processingBands, Receiver resceiver) throws IOException {
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
		Short noDataValue = null;
		TiffDataType tiffdataType = TiffDataType.INT16;
		for(TimeBand timeband : processingBands) {
			switch (timeband.band.type) {
			case TilePixel.TYPE_SHORT:
				// nothing
				break;
			case TilePixel.TYPE_FLOAT:
				if(tiffdataType == TiffDataType.INT16) {
					tiffdataType = TiffDataType.FLOAT32;
				}
				break;
			default:
				tiffdataType = TiffDataType.FLOAT64;
				break;
			}
		}

		for(TimeBand timeband : processingBands) {				
			switch(tiffdataType) { // all bands need same data type for tiff reader compatibility (e.g. GDAL)
			case INT16:
				tiffWriter.addTiffBand(TiffBand.ofInt16(dstWidth, dstHeight, ()->processor.getShortFrame(timeband).data));
				if(noDataValue == null) {
					noDataValue = timeband.band.getShortNA();
				}
				break;
			case FLOAT32:
				tiffWriter.addTiffBand(TiffBand.ofFloat32(dstWidth, dstHeight, ()->processor.getFloatFrame(timeband).data));
				break;
			case FLOAT64:
				tiffWriter.addTiffBand(TiffBand.ofFloat64(dstWidth, dstHeight, ()->processor.getDoubleFrame(timeband).data));
				break;
			default:
				throw new RuntimeException("unknown tiff data type");
			}
		}
		tiffWriter.setNoDataValue(noDataValue);
		resceiver.setStatus(HttpServletResponse.SC_OK);
		resceiver.setContentType("image/tiff");
		tiffWriter.writeAuto(new DataOutputStream(resceiver.getOutputStream()));		
	}

}
