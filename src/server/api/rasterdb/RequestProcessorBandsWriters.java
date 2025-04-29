package server.api.rasterdb;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Supplier;

import org.tinylog.Logger;

import jakarta.servlet.http.HttpServletResponse;
import rasterdb.GeoReference;
import rasterdb.TimeBand;
import rasterdb.TimeBandProcessor;
import rasterdb.cell.CellType;
import rasterdb.tile.NullFillTileYReverseIterator;
import rasterdb.tile.TileFloatToByteIterator;
import rasterdb.tile.TileFloatToDoubleIterator;
import rasterdb.tile.TileFloatToFloatIterator;
import rasterdb.tile.TileFloatToIntIterator;
import rasterdb.tile.TileFloatToShortIterator;
import rasterdb.tile.TilePixel;
import rasterdb.tile.TileShortToByteIterator;
import rasterdb.tile.TileShortToDoubleIterator;
import rasterdb.tile.TileShortToFloatIterator;
import rasterdb.tile.TileShortToIntIterator;
import rasterdb.tile.TileShortToShortIterator;
import rasterunit.Tile;
import server.api.rasterdb.RequestProcessor.RdatDataType;
import server.api.rasterdb.RequestProcessor.TiffDataType;
import util.Range2d;
import util.Receiver;
import util.TimeUtil;
import util.Web;
import util.rdat.RdatBand;
import util.rdat.RdatList;
import util.rdat.RdatWriter;
import util.tiff.TiffBand;
import util.tiff.TiffTiledBand;
import util.tiff.TiffTiledBandFloat32;
import util.tiff.TiffTiledBandFloat64;
import util.tiff.TiffTiledBandInt16;
import util.tiff.TiffTiledBandInt32;
import util.tiff.TiffTiledBandUint8;
import util.tiff.TiffWriter;

public class RequestProcessorBandsWriters {

	private static RdatDataType getRdatType(TimeBand timeBand) {
		switch(timeBand.band.type) {
		case TilePixel.TYPE_SHORT:
		case CellType.INT16:
			return RdatDataType.INT16;
		case CellType.UINT16:
			return RdatDataType.UINT16;
		case TilePixel.TYPE_FLOAT:
			return RdatDataType.FLOAT32;
		default:
			return RdatDataType.FLOAT64;
		}
	}
	
	private static RdatDataType getSamllestRdatType(RdatDataType rdatDataType, TimeBand timeBand) {
		switch (timeBand.band.type) {
		case TilePixel.TYPE_SHORT:
		case CellType.INT16:
			switch(rdatDataType) {
			case INT16:
				return RdatDataType.INT16;			
			case UINT16:
			case FLOAT32:	
				return RdatDataType.FLOAT32;			
			case FLOAT64:
			default:
				return RdatDataType.FLOAT64;
			}
		case CellType.UINT16:			
			switch(rdatDataType) {
			case INT16:
			case FLOAT32:
				return RdatDataType.FLOAT32;
			case UINT16:
				return RdatDataType.UINT16;
			case FLOAT64:
			default:
				return RdatDataType.FLOAT64;
			}
		case TilePixel.TYPE_FLOAT:
			switch(rdatDataType) {
			case INT16:
			case UINT16:
			case FLOAT32:	
				return RdatDataType.FLOAT32;
			case FLOAT64:
			default:
				return RdatDataType.FLOAT64;
			}
		default:
			return RdatDataType.FLOAT64;
		}
	}
	
	private static RdatDataType getRdatType(Collection<TimeBand> processingBands) {
		Iterator<TimeBand> it = processingBands.iterator();
		if(it.hasNext()) {
			RdatDataType rdatDataType = getRdatType(it.next());
			while(it.hasNext()) {
				rdatDataType = getSamllestRdatType(rdatDataType, it.next());
			}
			return rdatDataType;
		} else {
			return RdatDataType.INT16;
		}
	}

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
		
		RdatDataType dataType = getRdatType(processingBands);
				
		for(TimeBand timeband : processingBands) {
			RdatList bandMeta = new RdatList();
			bandMeta.addInt32("index", timeband.band.index);
			bandMeta.addString("name", timeband.band.has_title() ? timeband.band.title : "band" + timeband.band.index);
			if (timeband.band.has_wavelength()) {
				bandMeta.addFloat64("wavelength", timeband.band.wavelength);
				if (timeband.band.has_fwhm()) {
					bandMeta.addFloat64("fwhm", timeband.band.fwhm);
				}
			}
			switch(dataType) {
			case INT16:
				Logger.info("RDAT INT16");
				rdatWriter.addRdatBand(RdatBand.ofInt16(dstWidth, dstHeight, bandMeta, ()->processor.getShortFrame(timeband).data));
				if(noDataValue == null) {
					noDataValue = timeband.band.getInt16NA();
				}
				break;
			case UINT16:
				Logger.info("RDAT UINT16");
				rdatWriter.addRdatBand(RdatBand.ofUint16(dstWidth, dstHeight, bandMeta, ()->processor.getCharFrame(timeband).data));
				if(noDataValue == null) {
					noDataValue = (short) timeband.band.getUint16NA(); // !!! TODO change to char
				}
				break;
			case FLOAT32:
				Logger.info("RDAT FLOAT32");
				rdatWriter.addRdatBand(RdatBand.ofFloat32(dstWidth, dstHeight, bandMeta, ()->processor.getFloatFrame(timeband).data));
				break;
			case FLOAT64:
				Logger.info("RDAT FLOAT64");
				rdatWriter.addRdatBand(RdatBand.ofFloat64(dstWidth, dstHeight, bandMeta, ()->processor.getDoubleFrame(timeband).data));
				break;
			default:
				throw new RuntimeException("unknown data type");
			}
		}
		rdatWriter.setNoDataValue(noDataValue);
		resceiver.setStatus(HttpServletResponse.SC_OK);
		resceiver.setContentType(Web.MIME_BINARY);
		rdatWriter.write(new DataOutputStream(resceiver.getOutputStream()));		
	}

	public static TiffDataType getTiffDataType(TimeBand processingBand) {
		switch (processingBand.band.type) {
		case TilePixel.TYPE_SHORT:
		case CellType.INT16:
			return TiffDataType.INT16;
		case CellType.UINT16:
			return TiffDataType.UINT16;
		case TilePixel.TYPE_FLOAT:
			return TiffDataType.FLOAT32;
		default:
			return TiffDataType.FLOAT64;
		}
	}

	public static TiffDataType getSamllestTiffDataType(TiffDataType tiffdataType, TimeBand processingBand) {
		switch (processingBand.band.type) {
		case TilePixel.TYPE_SHORT:
		case CellType.INT16:
			switch(tiffdataType) {
			case UINT8:
			case INT16:
				return TiffDataType.INT16;
			case UINT16:
			case INT32:
				return TiffDataType.INT32;				
			case FLOAT32:	
				return TiffDataType.FLOAT32;			
			case FLOAT64:
			default:
				return TiffDataType.FLOAT64;
			}
		case CellType.UINT16:			
			switch(tiffdataType) {
			case UINT8:
			case UINT16:
				return TiffDataType.UINT16;
			case INT16:
				return TiffDataType.INT32;
			case FLOAT32:	
				return TiffDataType.FLOAT32;
			case INT32:
			case FLOAT64:
			default:
				return TiffDataType.FLOAT64;
			}
		case TilePixel.TYPE_FLOAT:
			switch(tiffdataType) {
			case UINT8:
			case INT16:
			case UINT16:
			case FLOAT32:	
				return TiffDataType.FLOAT32;
			case INT32:
			case FLOAT64:
			default:
				return TiffDataType.FLOAT64;
			}
		default:
			return TiffDataType.FLOAT64;
		}
	}

	public static TiffDataType getTiffDataType(Collection<TimeBand> processingBands) {
		Iterator<TimeBand> it = processingBands.iterator();
		if(it.hasNext()) {
			TiffDataType tiffdataType = getTiffDataType(it.next());
			while(it.hasNext()) {
				tiffdataType = getSamllestTiffDataType(tiffdataType, it.next());
			}
			return tiffdataType;
		} else {
			return TiffDataType.INT16;
		}
	}

	/**
	 * 
	 * @param processor
	 * @param processingBands
	 * @param receiver
	 * @param reqTiffdataType nullable
	 * @throws IOException
	 */
	public static void writeTiff(TimeBandProcessor processor, Collection<TimeBand> processingBands, Receiver receiver, TiffDataType reqTiffdataType) throws IOException {
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
		TiffDataType tiffdataType = reqTiffdataType == null ? getTiffDataType(processingBands) : reqTiffdataType;

		for(TimeBand timeband : processingBands) {				
			switch(tiffdataType) { // all bands need same data type for tiff reader compatibility (e.g. GDAL)
			case UINT8:
				tiffWriter.addTiffBand(TiffBand.ofUint8(dstWidth, dstHeight, ()->processor.getByteFrame(timeband).data, timeband.toDescription()));
				if(noDataValue == null) {
					noDataValue = timeband.band.getInt16NA();
				}
				break;
			case INT16:
				tiffWriter.addTiffBand(TiffBand.ofInt16(dstWidth, dstHeight, ()->processor.getShortFrame(timeband).data, timeband.toDescription()));
				if(noDataValue == null) {
					noDataValue = timeband.band.getInt16NA();
				}
				break;
			case UINT16:
				tiffWriter.addTiffBand(TiffBand.ofUint16(dstWidth, dstHeight, ()->processor.getCharFrame(timeband).data, timeband.toDescription()));
				if(noDataValue == null) {
					noDataValue = timeband.band.getInt16NA();
				}
				break;
			case INT32:
				tiffWriter.addTiffBand(TiffBand.ofInt32(dstWidth, dstHeight, ()->processor.getIntFrame(timeband).data, timeband.toDescription()));
				if(noDataValue == null) {
					noDataValue = timeband.band.getInt16NA();
				}
				break;
			case FLOAT32:
				tiffWriter.addTiffBand(TiffBand.ofFloat32(dstWidth, dstHeight, ()->processor.getFloatFrame(timeband).data, timeband.toDescription()));
				break;
			case FLOAT64:
				tiffWriter.addTiffBand(TiffBand.ofFloat64(dstWidth, dstHeight, ()->processor.getDoubleFrame(timeband).data, timeband.toDescription()));
				break;
			default:
				throw new RuntimeException("unknown tiff data type");
			}
		}
		tiffWriter.setNoDataValue(noDataValue);
		receiver.setStatus(HttpServletResponse.SC_OK);
		receiver.setContentType(Web.MIME_TIFF);
		receiver.setContentLength(tiffWriter.exactSizeOfWriteAuto());
		tiffWriter.writeAuto(new DataOutputStream(receiver.getOutputStream()));		
	}

	/**
	 * 
	 * @param queryProcessor
	 * @param processingBands
	 * @param receiver
	 * @param reqTiffdataType nullable
	 * @throws IOException
	 */
	public static void writeTiffTiled(TimeBandProcessor queryProcessor, Collection<TimeBand> processingBands, Receiver receiver, TiffDataType reqTiffdataType) throws IOException {
		GeoReference ref = queryProcessor.rasterdb.ref();
		Range2d queryRange2d = queryProcessor.range2d;		
		int tymin = TilePixel.pixelToTile(queryRange2d.ymin);
		int tymax = TilePixel.pixelToTile(queryRange2d.ymax);
		int txmin = TilePixel.pixelToTile(queryRange2d.xmin);
		int txmax = TilePixel.pixelToTile(queryRange2d.xmax);
		int ymin = TilePixel.tileToPixel(tymin);
		int ymax = TilePixel.tileToPixelMax(tymax);
		int xmin = TilePixel.tileToPixel(txmin);
		int xmax = TilePixel.tileToPixelMax(txmax);
		Range2d range2d = new Range2d(xmin, ymin, xmax, ymax);			
		TimeBandProcessor processor = new TimeBandProcessor(queryProcessor.rasterdb, range2d, 1);
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
		TiffDataType tiffdataType = reqTiffdataType == null ? getTiffDataType(processingBands) : reqTiffdataType;

		for(TimeBand timeband : processingBands) {
			Supplier<Iterator<Tile>> tileIterator = () -> {
				Collection<Tile> tilesYReverse = processor.rasterdb.rasterUnit().getTilesYReverse(timeband.timestamp, timeband.band.index, tymin, tymax, txmin, txmax);
				Iterator<Tile> tileYReverseIt = tilesYReverse.iterator();
				return new NullFillTileYReverseIterator(tileYReverseIt, tymin, tymax, txmin, txmax);
			};
			switch(tiffdataType) { // all bands need same data type for tiff reader compatibility (e.g. GDAL)
			case UINT8:
				switch (timeband.band.type) {
				case TilePixel.TYPE_SHORT: {
					TiffTiledBandUint8 tiffTiledBand = TiffTiledBand.ofUint8Iterator(dstWidth, dstHeight, 256, 256, () -> {
						Logger.warn("downcast short to byte");
						short na_src = timeband.band.getInt16NA();
						byte na_dst = 0;
						byte[][] empty = new byte[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
						return new TileShortToByteIterator(tileIterator.get(), empty, na_src, na_dst);
					}, timeband.toDescription());
					tiffWriter.addTiffTiledBand(tiffTiledBand);			
					break;
				}
				case TilePixel.TYPE_FLOAT: {
					TiffTiledBandUint8 tiffTiledBand = TiffTiledBand.ofUint8Iterator(dstWidth, dstHeight, 256, 256, () -> {						
						Logger.warn("downcast float to byte");
						byte na_dst = 0;
						byte[][] empty = new byte[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
						return new TileFloatToByteIterator(tileIterator.get(), empty, na_dst);
					}, timeband.toDescription());
					tiffWriter.addTiffTiledBand(tiffTiledBand);					
					break;
				}
				default:
					throw new RuntimeException("unknown band type");
				}				
				if(noDataValue == null) {
					noDataValue = timeband.band.getInt16NA();
				}
				break;
			case INT16:
				switch (timeband.band.type) {
				case TilePixel.TYPE_SHORT: {
					TiffTiledBandInt16 tiffTiledBand = TiffTiledBand.ofInt16Iterator(dstWidth, dstHeight, 256, 256, () -> {						
						short[][] empty = new short[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
						return new TileShortToShortIterator(tileIterator.get(), empty);
					}, timeband.toDescription());
					tiffWriter.addTiffTiledBand(tiffTiledBand);					
					break;
				}
				case TilePixel.TYPE_FLOAT: {
					TiffTiledBandInt16 tiffTiledBand = TiffTiledBand.ofInt16Iterator(dstWidth, dstHeight, 256, 256, () -> {						
						Logger.warn("downcast float to short");
						short na_target = 0;
						short[][] empty = new short[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
						return new TileFloatToShortIterator(tileIterator.get(), empty, na_target);
					}, timeband.toDescription());
					tiffWriter.addTiffTiledBand(tiffTiledBand);					
					break;
				}
				default:
					throw new RuntimeException("unknown band type");
				}				
				if(noDataValue == null) {
					noDataValue = timeband.band.getInt16NA();
				}
				break;
			case INT32:
				switch (timeband.band.type) {
				case TilePixel.TYPE_SHORT: {
					TiffTiledBandInt32 tiffTiledBand = TiffTiledBand.ofInt32Iterator(dstWidth, dstHeight, 256, 256, () -> {						
						int[][] empty = new int[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
						short na_src = timeband.band.getInt16NA();
						int na_dst = 0;
						return new TileShortToIntIterator(tileIterator.get(), empty, na_src, na_dst);
					}, timeband.toDescription());
					tiffWriter.addTiffTiledBand(tiffTiledBand);					
					break;
				}
				case TilePixel.TYPE_FLOAT: {
					TiffTiledBandInt32 tiffTiledBand = TiffTiledBand.ofInt32Iterator(dstWidth, dstHeight, 256, 256, () -> {						
						Logger.warn("downcast float to short");
						int na_target = 0;
						int[][] empty = new int[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
						return new TileFloatToIntIterator(tileIterator.get(), empty, na_target);
					}, timeband.toDescription());
					tiffWriter.addTiffTiledBand(tiffTiledBand);					
					break;
				}
				default:
					throw new RuntimeException("unknown band type");
				}				
				if(noDataValue == null) {
					noDataValue = timeband.band.getInt16NA();
				}
				break;
			case FLOAT32:
				switch (timeband.band.type) {
				case TilePixel.TYPE_SHORT: {
					TiffTiledBandFloat32 tiffTiledBand = TiffTiledBand.ofFloat32Iterator(dstWidth, dstHeight, 256, 256, () -> {						
						float[][] empty = new float[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
						return new TileShortToFloatIterator(tileIterator.get(), empty, timeband.band.getInt16NA());
					}, timeband.toDescription());
					tiffWriter.addTiffTiledBand(tiffTiledBand);
					break;
				}
				case TilePixel.TYPE_FLOAT: {
					TiffTiledBandFloat32 tiffTiledBand = TiffTiledBand.ofFloat32Iterator(dstWidth, dstHeight, 256, 256, () -> {						
						float[][] empty = new float[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
						return new TileFloatToFloatIterator(tileIterator.get(), empty);
					}, timeband.toDescription());
					tiffWriter.addTiffTiledBand(tiffTiledBand);
					break;
				}
				default:
					throw new RuntimeException("unknown band type");
				}				
				break;
			case FLOAT64:
				switch (timeband.band.type) {
				case TilePixel.TYPE_SHORT: {
					TiffTiledBandFloat64 tiffTiledBand = TiffTiledBand.ofFloat64Iterator(dstWidth, dstHeight, 256, 256, () -> {						
						double[][] empty = new double[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
						return new TileShortToDoubleIterator(tileIterator.get(), empty, timeband.band.getInt16NA());
					}, timeband.toDescription());
					tiffWriter.addTiffTiledBand(tiffTiledBand);
					break;
				}
				case TilePixel.TYPE_FLOAT: {
					TiffTiledBandFloat64 tiffTiledBand = TiffTiledBand.ofFloat64Iterator(dstWidth, dstHeight, 256, 256, () -> {						
						double[][] empty = new double[TilePixel.PIXELS_PER_ROW][TilePixel.PIXELS_PER_ROW];
						return new TileFloatToDoubleIterator(tileIterator.get(), empty);
					}, timeband.toDescription());
					tiffWriter.addTiffTiledBand(tiffTiledBand);
					break;
				}
				default:
					throw new RuntimeException("unknown band type");
				}				
				break;
			default:
				throw new RuntimeException("unknown tiff data type");
			}
		}
		tiffWriter.setNoDataValue(noDataValue);
		receiver.setStatus(HttpServletResponse.SC_OK);
		receiver.setContentType(Web.MIME_TIFF);
		receiver.setContentLength(tiffWriter.exactSizeOfWriteAuto());
		tiffWriter.writeAuto(new DataOutputStream(receiver.getOutputStream()));		
	}
}
