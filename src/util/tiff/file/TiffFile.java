package util.tiff.file;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;


import org.tinylog.Logger;

import rasterdb.GeoReference;
import util.CharArrayWriterUnsync;
import util.IndentedXMLStreamWriter;
import util.Range2d;
import util.Util;
import util.collections.array.iterator.ReadonlyArrayIterator;
import util.collections.vec.Vec;
import util.tiff.GeoKeyDirectory;
import util.tiff.IFD;

public class TiffFile {
	

	public static final int TIFFsignatureBE = 0x4d_4d_00_2a; // TIFF header signature big endian
	public static final int TiffIFDOffsetBE = 0x00_00_00_08; // TIFF Offset to first IFD
	public static final int TIFFsignatureLEinBE = 0x49_49_2a_00; // TIFF header signature little endian
	public static final long BigTIFFsignature = 0x4d_4d_00_2b__00_08_00_00l;  // BigTIFF header signature big endian	
	public static final long BigTiffIFDOffset = 0x00_00_00_00__00_00_00_10l;  // BigTIFF Offset to first IFD

	public static int UINT16_MAX_VALUE = (int) Math.pow(2, 16);

	public final GeoReference ref;

	public final Range2d range;
	public final int width;
	public final int height;
	public final int tileWidth;
	public final int tileHeight;
	public final int bandCount;

	private short bitsPerSamplePerBand = 16;
	//private short compressionType = 1; // no compression
	private TiffCompression tiffCompression = TiffCompression.ZSTD;
	private short photometricInterpretationType = 1; //BlackIsZero
	private short sampleFormat = 2; // signed integer
	private boolean deltaCoding = false;

	public final TiffTile[][] tiles;
	public final Vec<TiffTile[][]> overviewtilesVec;
	public final int xtileLen;
	public final int ytileLen;
	public final int tileLen;

	public static enum TiffCompression {
		NO(1),
		DEFLATE(32946),
		ZSTD(50000);

		public final short value;

		private TiffCompression(int value) {
			this.value = (short) value;
		}

		public static TiffCompression parse(String compressionText, TiffCompression defaultCompression) {
			if(compressionText == null) {
				return defaultCompression;
			}
			compressionText = compressionText.trim().toUpperCase();
			if(compressionText.isEmpty()) {
				return defaultCompression;
			}
			switch(compressionText) {
			case "NO":
				return NO;
			case "DEFLATE":
				return DEFLATE;
			case "ZSTD":
				return ZSTD;
			default:
				throw new RuntimeException("unknown compression type");
			}
		}
	}

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

	public static class TiffImageEntry {
		public final TiffTile[][] tiles;
		public final int scale;

		public TiffImageEntry(TiffTile[][] tiles, int scale) {
			this.tiles = tiles;
			this.scale = scale;
		}
	}

	public TiffFile(GeoReference ref, Range2d range, int tileWidth, int tileHeight, int bandCount) {
		this.ref = ref;
		this.range = range; 
		int w = range.getWidth();
		int h = range.getHeight();
		/*if(w < 1 || w > UINT16_MAX_VALUE || h < 1 || h > UINT16_MAX_VALUE) {
			throw new RuntimeException("raster too large: " + w + " x " + h);
		}*/
		if(w < 1 || h < 1) {
			throw new RuntimeException("raster size not valid: " + w + " x " + h);
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



		long maxTileOffset = Long.MAX_VALUE;				
		long maxTileByteCount = Long.MAX_VALUE;

		Logger.info("tileLen " + xtileLen + " x " + ytileLen + " = "+ tileLen);

		this.tiles = new TiffTile[bandCount][tileLen];
		for (int b = 0; b < bandCount; b++) {
			TiffTile[] btiles = tiles[b];
			int i = 0;
			for(int y = range.ymax; y > range.ymin; y -= tileHeight) {
				for(int x = range.xmin; x < range.xmax; x += tileWidth) {
					TiffTile tile = new TiffTile(x, y - tileHeight + 1, x + tileWidth - 1, y);
					tile.pos = maxTileOffset;
					tile.len = maxTileByteCount;
					btiles[i++] = tile;					
				}
			}
		}

		this.overviewtilesVec = new Vec<TiffTile[][]>();
		if(xtileLen > 3 || ytileLen > 3) {
			int scale = 1;
			int prevTileLen = tileLen;
			while(true) {
				scale *= 2;
				int overviewTileWidth = tileWidth * scale;
				int overviewTileHeight = tileHeight * scale;
				Logger.info("overviewTileWidth " + overviewTileWidth);
				//Range2d overviewRange = range.allignMaxToTiles(overviewTileWidth, overviewTileHeight);
				Range2d overviewRange = range; // TODO check
				int overviewWidth = overviewRange.getWidth();
				int overviewHeight = overviewRange.getHeight();
				int overviewXtileLen = (overviewWidth + overviewTileWidth - 1) / overviewTileWidth;
				int overviewYtileLen = (overviewHeight + overviewTileHeight - 1) / overviewTileHeight;
				int overviewTileLen = overviewXtileLen * overviewYtileLen;
				if(overviewTileLen == prevTileLen) {
					break;
				}
				Logger.info("overviewTileLen " + overviewXtileLen + " x " + overviewYtileLen + " = "+ overviewTileLen);

				TiffTile[][] overviewTiles = new TiffTile[bandCount][overviewTileLen];
				for (int b = 0; b < bandCount; b++) {
					TiffTile[] btiles = overviewTiles[b];
					int i = 0;
					for(int y = overviewRange.ymax; y > overviewRange.ymin; y -= overviewTileHeight) {
						for(int x = overviewRange.xmin; x < overviewRange.xmax; x += overviewTileWidth) {
							TiffTile tile = new TiffTile(x, y - overviewTileHeight + scale, x + overviewTileWidth - scale, y);
							tile.pos = maxTileOffset;
							tile.len = maxTileByteCount;
							btiles[i++] = tile;					
						}
					}
					Logger.info("counter " + i + " of " + overviewTileLen);
				}
				this.overviewtilesVec.add(overviewTiles);

				if(overviewXtileLen <= 3 && overviewYtileLen <= 3) {
					break;
				}
				if(scale >= 256) {
					break;
				}
				prevTileLen = overviewTileLen;
			}
			Logger.info("overviews: " + this.overviewtilesVec.size());
		}
	}

	public long writeHeader(RandomAccessFile raf, boolean bigTiff) throws IOException {

		if(!bigTiff) {
			raf.seek(0);
			raf.writeInt(TIFFsignatureBE); 
			raf.writeInt(((int)raf.getFilePointer()) + 4);
		} else {		
			raf.seek(0);
			raf.writeLong(BigTIFFsignature); 
			raf.writeLong((raf.getFilePointer()) + 8);
		}

		IFD ifd = new IFD();

		ifd.add_ImageWidth(width);
		ifd.add_ImageLength(height);		

		short[] bitsPerSample = new short[bandCount];
		Arrays.fill(bitsPerSample, bitsPerSamplePerBand);
		ifd.add_BitsPerSample(bitsPerSample);

		ifd.add_Compression(tiffCompression.value);

		if(isApplicableDeltaCoding()) {
			ifd.add_Predictor((short) 2); // Horizontal differencing
		} else {
			ifd.add_Predictor((short) 1); // No prediction
		}

		ifd.add_PhotometricInterpretation(photometricInterpretationType);	

		short[] sampleFormats = new short[bandCount];
		Arrays.fill(sampleFormats, sampleFormat);
		long[] tileOffsets = Arrays.stream(tiles).flatMapToLong(bandTiles -> Arrays.stream(bandTiles).mapToLong(tile -> tile.pos)).toArray();
		long[] tileByteCounts = Arrays.stream(tiles).flatMapToLong(bandTiles -> Arrays.stream(bandTiles).mapToLong(tile -> tile.len)).toArray();

		Logger.info("tileOffsets " + Arrays.toString(tileOffsets));
		Logger.info("tileByteCounts " + Arrays.toString(tileByteCounts));

		ifd.add_SamplesPerPixel((short) bandCount);
		ifd.add_SampleFormat(sampleFormats);
		ifd.add_PlanarConfiguration_Planar();
		ifd.add_TileWidth((short) tileWidth);
		ifd.add_TileLength((short) tileHeight);
		ifd.add_TileOffsets_direct(tileOffsets);
		ifd.add_TileByteCounts(tileByteCounts);

		ifd.add_ImageDescription("GeoTIFF created by Remote Sensing Database (RSDB) raster layer export.");
		ifd.add_Software("Remote Sensing Database (RSDB)");
		ifd.add_DateTime_now();

		double geoXmin = ref.pixelXToGeo(range.xmin);
		double geoYmin = ref.pixelYToGeo(range.ymin);
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
			Logger.error(e);
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
			//factory.setProperty("escapeCharacters", false);  // not needed?  no direct GML geometry output?
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
			Logger.warn(e);
		}

		ifd.add_Orientation_top_left();

		ReadonlyArrayIterator<TiffTile[][]> it = overviewtilesVec.iterator();
		Vec<IFD> overviewIfds = new Vec<IFD>();
		int scale = 1;
		while(it.hasNext()) {
			scale *= 2;
			int i = it.nextIndex();
			Logger.info("overview i " + i + "  scale " + scale);
			TiffTile[][] tiles = it.next();
			IFD overviewIfd = new IFD();
			overviewIfd.add_NewSubfileType_reduced_resolution();
			overviewIfd.add_ImageWidth((short) (width / scale)); // TODO
			overviewIfd.add_ImageLength((short) (height / scale));// TODO
			overviewIfd.add_BitsPerSample(bitsPerSample);
			overviewIfd.add_Compression(tiffCompression.value);
			if(isApplicableDeltaCoding()) {
				overviewIfd.add_Predictor((short) 2); // Horizontal differencing
			} else {
				overviewIfd.add_Predictor((short) 1); // No prediction
			}
			overviewIfd.add_PhotometricInterpretation(photometricInterpretationType);
			overviewIfd.add_SamplesPerPixel((short) bandCount);
			overviewIfd.add_SampleFormat(sampleFormats);
			overviewIfd.add_PlanarConfiguration_Planar();
			overviewIfd.add_TileWidth((short) tileWidth);
			overviewIfd.add_TileLength((short) tileHeight);
			long[] overviewTileOffsets = Arrays.stream(tiles).flatMapToLong(bandTiles -> Arrays.stream(bandTiles).mapToLong(tile -> tile.pos)).toArray();
			long[] overviewTileByteCounts = Arrays.stream(tiles).flatMapToLong(bandTiles -> Arrays.stream(bandTiles).mapToLong(tile -> tile.len)).toArray();
			overviewIfd.add_TileOffsets_direct(overviewTileOffsets);
			overviewIfd.add_TileByteCounts(overviewTileByteCounts);
			overviewIfds.add(overviewIfd);			
		}

		IFD[] ifds = new IFD[overviewtilesVec.size() + 1];
		ifds[0] = ifd;
		overviewIfds.forEachIndexed((e, i) -> ifds[i + 1] = e);

		long imageDataPos = bigTiff ? IFD.writeBigTIFF(raf.getFilePointer(), raf, raf, ifds) : IFD.writeTIFF((int) raf.getFilePointer(), raf, raf, ifds);
		Logger.info("raf.getFilePointer " + raf.getFilePointer() + "   " + imageDataPos);
		return imageDataPos;
	}

	public void set_sampleFormat(short sampleFormat) {
		this.sampleFormat = sampleFormat;
	}

	public void set_sampleFormat_unsigned_integer() {
		set_sampleFormat((short) 1);
	}

	public void set_sampleFormat_signed_integer() {
		set_sampleFormat((short) 2);
	}

	public void set_sampleFormat_floating_point() {
		set_sampleFormat((short) 3);
	}

	public void set_sampleFormat_undefined_data() {
		set_sampleFormat((short) 3);
	}

	public void set_bitsPerSamplePerBand(short bitsPerSamplePerBand) {
		this.bitsPerSamplePerBand = bitsPerSamplePerBand;
	}

	public void setBandDataType_int16() {
		set_sampleFormat_signed_integer();
		set_bitsPerSamplePerBand((short) 16);
	}

	public void setDeltaCoding(boolean deltaCoding) {
		this.deltaCoding = deltaCoding;
	}

	public boolean getDeltaCoding() {
		return deltaCoding;
	}
	
	public boolean isApplicableDeltaCoding() {
		switch(tiffCompression) {
		case NO:
			return false;
		case DEFLATE:
			return deltaCoding;
		case ZSTD:
			return deltaCoding;
		default:
			throw new RuntimeException("unknown compression type");
		}
	}

	public TiffCompression getTiffCompression() {
		return tiffCompression;
	}

	public void setTiffCompression(TiffCompression tiffCompression) {
		this.tiffCompression = tiffCompression;
	}
}
