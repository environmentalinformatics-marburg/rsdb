package util.tiff.file;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Iterator;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rasterdb.GeoReference;
import util.CharArrayWriterUnsync;
import util.IndentedXMLStreamWriter;
import util.Util;
import util.tiff.GeoKeyDirectory;
import util.tiff.IFD;
import util.tiff.TiffBand;

public class TiffFile {
	private static final Logger log = LogManager.getLogger();

	public static final int TIFFsignature = 0x4d_4d_00_2a; //TIFF header signature big endian
	public static final int TiffIFDOffset = 0x00_00_00_08; // TIFF Offset to first IFD
	public static final long BigTIFFsignature = 0x4d_4d_00_2b__00_08_00_00l;  // BigTIFF header signature big endian	
	public static final long BigTiffIFDOffset = 0x00_00_00_00__00_00_00_10l;  // BigTIFF Offset to first IFD

	public final GeoReference ref;

	public final int xmin; 
	public final int ymin; 
	public final int xmax; 
	public final int ymax;
	public final int width;
	public final int height;
	public final int tileWidth;
	public final int tileHeight;
	public final int bandCount;

	private short[] bitsPerSample;
	private short compressionType = 1; //no compression
	private short photometricInterpretationType = 1; //BlackIsZero
	private short sampleFormat = 2; // Int16

	public final TiffTile[][] tiles;
	public final int xtileLen;
	public final int ytileLen;
	public final int tileLen;

	public static class TiffTile {
		public final int tileXmin;
		public final int tileYmin;
		public final int tileXmax;
		public final int tileYmax;
		public long pos = 0;
		public long len = 0;

		public TiffTile(int tileXmin, int tileYmin, int tileXmax, int tileYmax) {
			this.tileXmin = tileXmin;
			this.tileYmin = tileYmin;
			this.tileXmax = tileXmax;
			this.tileYmax = tileYmax;
		}

		@Override
		public String toString() {
			return "TiffTile [tileXmin=" + tileXmin + ", tileYmin=" + tileYmin + ", tileXmax=" + tileXmax
					+ ", tileYmax=" + tileYmax + ", pos=" + pos + ", len=" + len + "]";
		}
	}	

	public TiffFile(GeoReference ref, int xmin, int ymin, int xmax, int ymax, int tileWidth, int tileHeight, int bandCount) {
		this.ref = ref;
		this.xmin = xmin; 
		this.ymin = ymin; 
		this.xmax = xmax; 
		this.ymax = ymax;
		int w = xmax - xmin + 1;
		int h = ymax - ymin + 1;
		if(w < 1 || w > Short.MAX_VALUE || h < 1 || h > Short.MAX_VALUE) {
			throw new RuntimeException("raster too large");
		}
		if(tileWidth % 16 != 0 || tileHeight % 16 != 0) {
			throw new RuntimeException("tileWidth and tileHeight need to be multiples of 16");
		}
		this.width = w;
		this.height = h;
		this.xtileLen = (width + tileWidth - 1) / tileWidth;
		this.ytileLen = (height + tileHeight - 1) / tileHeight;
		this.tileLen = xtileLen * ytileLen;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.bandCount = bandCount;

		this.bitsPerSample = new short[bandCount];

		long maxTileOffset = Long.MAX_VALUE;				
		long maxTileByteCount = Long.MAX_VALUE;

		log.info("xtileLen " + xtileLen);
		log.info("ytileLen " + ytileLen);

		this.tiles = new TiffTile[bandCount][tileLen];
		for (int b = 0; b < bandCount; b++) {
			TiffTile[] btiles = tiles[b];
			bitsPerSample[b] = 16; // int16 TODO
			int i = 0;
			for(int y = ymax; y > ymin; y -= tileHeight) {
				for(int x = xmin; x < xmax; x += tileWidth) {
					TiffTile tile = new TiffTile(x, y - tileHeight + 1, x + tileWidth - 1, y);
					tile.pos = maxTileOffset;
					tile.len = maxTileByteCount;
					btiles[i++] = tile;					
				}
			}
		}
	}

	public long writeHeader(RandomAccessFile raf) throws IOException {

		raf.seek(0);
		raf.writeInt(TIFFsignature); 
		raf.writeInt(TiffIFDOffset);
		/*
		raf.seek(0);
		raf.writeLong(BigTIFFsignature); 
		raf.writeLong(BigTiffIFDOffset);*/

		IFD ifd = new IFD();

		ifd.add_ImageWidth((short) width);
		ifd.add_ImageLength((short) height);
		ifd.add_BitsPerSample(bitsPerSample);
		ifd.add_Compression(compressionType);
		ifd.add_PhotometricInterpretation(photometricInterpretationType);	

		short[] sampleFormats = new short[bandCount];
		Arrays.fill(sampleFormats, sampleFormat);
		long[] tileOffsets = Arrays.stream(tiles).flatMapToLong(bandTiles -> Arrays.stream(bandTiles).mapToLong(tile -> tile.pos)).toArray();
		long[] tileByteCounts = Arrays.stream(tiles).flatMapToLong(bandTiles -> Arrays.stream(bandTiles).mapToLong(tile -> tile.len)).toArray();

		log.info("tileOffsets " + Arrays.toString(tileOffsets));
		log.info("tileByteCounts " + Arrays.toString(tileByteCounts));

		ifd.add_SamplesPerPixel((short) bandCount);
		ifd.add_SampleFormat(sampleFormats);
		ifd.add_PlanarConfiguration_Planar();
		ifd.add_TileWidth((short) tileWidth);
		ifd.add_TileLength((short) tileHeight);
		ifd.add_TileOffsets_direct(tileOffsets);
		ifd.add_TileByteCounts(tileByteCounts);

		ifd.add_ImageDescription("created by Remote Sensing Database (RSDB)");
		ifd.add_Software("Remote Sensing Database (RSDB)");
		ifd.add_DateTime_now();

		double geoXmin = ref.pixelXToGeo(xmin);
		double geoYmin = ref.pixelYToGeo(ymin);
		ifd.add_geotiff_ModelTiepointTag(0, height, geoXmin, geoYmin);
		double xScale = ref.pixel_size_x;
		double yScale = ref.pixel_size_y;
		ifd.add_geotiff_ModelPixelScaleTag(xScale, yScale);
		try {
			int[] xRat = Util.getRational(xScale);
			int[] yRat = Util.getRational(yScale);
			ifd.add_ResolutionUnit_noAbsoluteUnit();
			ifd.add_XResolution(xRat[0], xRat[1]);
			ifd.add_YResolution(yRat[0], yRat[1]);
		} catch(Exception e) {
			log.error(e);
		}

		short epsgCode = (short) ref.getEPSG(0);
		GeoKeyDirectory geoKeyDirectory = new GeoKeyDirectory();
		if(epsgCode > 0) {
			geoKeyDirectory.add_ProjectedCSType(epsgCode);
		}
		ifd.add_GeoKeyDirectory(geoKeyDirectory);
		
		short noDataValue = 0;
		ifd.add_GDAL_NODATA(noDataValue);

		try {
			CharArrayWriterUnsync writer = new CharArrayWriterUnsync();
			XMLOutputFactory factory = XMLOutputFactory.newInstance();
			factory.setProperty("escapeCharacters", false);
			XMLStreamWriter xmlWriterInner = factory.createXMLStreamWriter(writer);
			final XMLStreamWriter xmlWriter = new IndentedXMLStreamWriter(xmlWriterInner);
			//xmlWriter.writeStartDocument(); // xml meta tag
			xmlWriter.writeStartElement("GDALMetadata");
			for (int j = 0; j < bandCount; j++) {
				xmlWriter.writeStartElement("Item");
				xmlWriter.writeAttribute("name", "DESCRIPTION");
				xmlWriter.writeAttribute("sample", ""+(j+1));
				xmlWriter.writeAttribute("role", "description");
				xmlWriter.writeCharacters("band" + (j+1));
				xmlWriter.writeEndElement(); // Item
			}			
			xmlWriter.writeEndElement(); // GDALMetadata
			xmlWriter.writeEndDocument();
			xmlWriter.close();				
			String text = writer.toString();				
			ifd.add_GDAL_METADATA(text);
		} catch (Exception e) {
			log.warn(e);
		}
		
		ifd.add_Orientation_top_left();

		//ifd.writeBigTIFF(raf);
		long imageDataPos = ifd.writeTIFF(raf);
		log.info("raf.getFilePointer " + raf.getFilePointer() + "   " + imageDataPos);
		return imageDataPos;
	}
}
