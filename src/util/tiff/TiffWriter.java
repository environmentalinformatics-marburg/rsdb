package util.tiff;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.CharArrayWriterUnsync;
import util.IndentedXMLStreamWriter;
import util.Util;
import util.collections.vec.Vec;

public class TiffWriter {
	private static final Logger log = LogManager.getLogger();

	private static final short compressionType = 1; //no compression
	private short photometricInterpretationType = 1; //BlackIsZero
	private short[] extraSamples = null;

	private final int width;
	private final int height;
	private Vec<TiffBand> tiffBands = new Vec<TiffBand>();
	private TiffComposite tiffComposite = null;
	private Vec<TiffTiledBand> tiffTiledBands = new Vec<TiffTiledBand>();

	private final double geoXmin;
	private final double geoYmin;
	private final double xScale;
	private final double yScale;
	private final short epsgCode;

	private Short noDataValue = null;


	public TiffWriter(int width, int height, double geoXmin, double geoYmin, double xScale, double yScale, short epsgCode) {
		this.width = width;
		this.height = height;
		this.geoXmin = geoXmin;
		this.geoYmin = geoYmin;
		this.xScale = xScale;
		this.yScale = yScale;
		this.epsgCode = epsgCode;
	}

	public void set_photometricInterpretationType_WhiteIsZero() {
		photometricInterpretationType = 0;
	}

	public void set_photometricInterpretationType_BlackIsZero() {
		photometricInterpretationType = 1;
	}

	public void set_photometricInterpretationType_RGB() {
		photometricInterpretationType = 2;
	}

	public void set_photometricInterpretationType_PaletteColor() {
		photometricInterpretationType = 3;
	}

	public void set_extraSamples_One_Alpha() {
		extraSamples = new short[] {2};
	}

	public void addTiffBand(TiffBand tiffBand) {
		if(!tiffTiledBands.isEmpty()) {
			throw new RuntimeException("there is a tiled band");
		}
		if(tiffComposite != null) {
			throw new RuntimeException("there is a composite");
		}
		Objects.requireNonNull(tiffBand);
		if(tiffBand.width != width) {
			throw new RuntimeException("tiffBand.width != width");
		}
		if(tiffBand.height != height) {
			throw new RuntimeException("tiffBand.height != height");
		}
		tiffBands.add(tiffBand);
	}

	public void addTiffTiledBand(TiffTiledBand tiffTiledBand) {
		if(!tiffBands.isEmpty()) {
			throw new RuntimeException("there is a non tiled band");
		}
		if(tiffComposite != null) {
			throw new RuntimeException("there is a composite");
		}
		Objects.requireNonNull(tiffTiledBand);
		if(tiffTiledBand.width != width) {
			throw new RuntimeException("tiffBand.width != width");
		}
		if(tiffTiledBand.height != height) {
			throw new RuntimeException("tiffBand.height != height");
		}
		tiffTiledBands.add(tiffTiledBand);
	}

	public void setNoDataValue(Short noDataValue) {
		this.noDataValue = noDataValue;
	}

	private IFD createIFD() {

		int samplesPerPixel;
		short[] bitsPerSample;
		long[] stripByteCounts = null;
		short[] sampleFormats;
		boolean containsFloatFormat = false;
		boolean isTiled = false;
		short tileWidth = 0;
		short tileHeight = 0;
		long[] tileByteCounts = null;


		if(!tiffBands.isEmpty()) {

			samplesPerPixel = tiffBands.size();		
			bitsPerSample = new short[samplesPerPixel];
			stripByteCounts = new long[samplesPerPixel];
			sampleFormats = new short[samplesPerPixel];

			for (int i = 0; i < samplesPerPixel; i++) {
				TiffBand tiffBand = tiffBands.get(i);
				short bps = tiffBand.getBitsPerSample();
				if(bps % 8 != 0) {
					throw new RuntimeException();
				}
				bitsPerSample[i] = bps;
				stripByteCounts[i] = ((long)width) * ((long)height) * ((long)(bps / 8));
				short sampleFormat = tiffBand.getSampleFormat();
				sampleFormats[i] = sampleFormat;
				if(sampleFormat == 3) {
					containsFloatFormat = true;
				}
			}
		} else if (!tiffTiledBands.isEmpty()) {
			isTiled = true;

			samplesPerPixel = tiffTiledBands.size();		
			bitsPerSample = new short[samplesPerPixel];
			sampleFormats = new short[samplesPerPixel];

			int tileCount = 0;
			for (int i = 0; i < samplesPerPixel; i++) {
				TiffTiledBand tiffTiledBand = tiffTiledBands.get(i);

				short bps = tiffTiledBand.getBitsPerSample();
				if(bps % 8 != 0) {
					throw new RuntimeException();
				}
				bitsPerSample[i] = bps;
				short sampleFormat = tiffTiledBand.getSampleFormat();
				sampleFormats[i] = sampleFormat;
				if(sampleFormat == 3) {
					containsFloatFormat = true;
				}
				tileWidth = (short) tiffTiledBand.tileWidth;
				tileHeight = (short) tiffTiledBand.tileHeight;				

				tileCount += tiffTiledBand.getTileCount();
			}			

			tileByteCounts = new long[tileCount];

			int tileByteCountIndex = 0;
			for (int i = 0; i < samplesPerPixel; i++) {
				TiffTiledBand tiffTiledBand = tiffTiledBands.get(i);
				long[] tileSizes = tiffTiledBand.getTileSizes();
				for (int j = 0; j < tileSizes.length; j++) {
					tileByteCounts[tileByteCountIndex++] = tileSizes[j];
				}				
			}
			if(tileByteCountIndex != tileCount) {
				throw new RuntimeException();
			}
		} else if (tiffComposite != null) {			
			bitsPerSample = tiffComposite.getBitsPerSample();
			samplesPerPixel = bitsPerSample.length;			
			sampleFormats = tiffComposite.getSampleFormats();
			for (int i = 0; i < samplesPerPixel; i++) {
				if(sampleFormats[i] == 3) {
					containsFloatFormat = true;
				}
			}
			long bytesPerPixel = tiffComposite.getBytesPerPixel();
			long dataSize = ((long)width) * ((long)height) * bytesPerPixel;
			stripByteCounts = new long[] {dataSize};			
		} else {
			throw new RuntimeException("no data in tiff");
		}

		IFD ifd = new IFD();
		ifd.add_ImageWidth((short) width);
		ifd.add_ImageLength((short) height);
		ifd.add_BitsPerSample(bitsPerSample);
		ifd.add_Compression(compressionType);
		ifd.add_PhotometricInterpretation(photometricInterpretationType);
		if(!isTiled) {
			ifd.add_StripOffsets(stripByteCounts);
		}
		ifd.add_SamplesPerPixel((short) samplesPerPixel);
		if(extraSamples != null) {
			ifd.add_ExtraSamples(extraSamples);
		}
		if(!isTiled) {
			ifd.add_RowsPerStrip((short) height);
			ifd.add_StripByteCounts(stripByteCounts);
		}
		ifd.add_SampleFormat(sampleFormats);
		if(tiffComposite == null) {
			ifd.add_PlanarConfiguration_Planar();
		} else {
			ifd.add_PlanarConfiguration_Chunky();
		}

		if(isTiled) {
			ifd.add_TileWidth(tileWidth);
			ifd.add_TileLength(tileHeight);
			ifd.add_TileOffsets(tileByteCounts);
			ifd.add_TileByteCounts(tileByteCounts);
		}

		ifd.add_ImageDescription("created by Remote Sensing Database (RSDB)");
		ifd.add_Software("Remote Sensing Database (RSDB)");
		ifd.add_DateTime_now();

		ifd.add_geotiff_ModelTiepointTag(0, height, geoXmin, geoYmin);
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

		GeoKeyDirectory geoKeyDirectory = new GeoKeyDirectory();
		geoKeyDirectory.add_ProjectedCSType(epsgCode);
		ifd.add_GeoKeyDirectory(geoKeyDirectory);

		if(noDataValue != null) {
			ifd.add_GDAL_NODATA(noDataValue);
		} else if(containsFloatFormat) {
			ifd.add_GDAL_NODATA_float_NaN();
		}

		if(true) {
			try {
				CharArrayWriterUnsync writer = new CharArrayWriterUnsync();
				XMLOutputFactory factory = XMLOutputFactory.newInstance();
				factory.setProperty("escapeCharacters", false);
				XMLStreamWriter xmlWriterInner = factory.createXMLStreamWriter(writer);
				final XMLStreamWriter xmlWriter = new IndentedXMLStreamWriter(xmlWriterInner);
				//xmlWriter.writeStartDocument(); // xml meta tag
				xmlWriter.writeStartElement("GDALMetadata");
				if(!tiffBands.isEmpty()) {
					tiffBands.forEachIndexedThrowable((TiffBand band, int i) -> {
						if(band.description != null && !band.description.isEmpty()) {
							xmlWriter.writeStartElement("Item");
							xmlWriter.writeAttribute("name", "DESCRIPTION");
							xmlWriter.writeAttribute("sample", Integer.toString(i));
							xmlWriter.writeAttribute("role", "description");
							xmlWriter.writeCharacters(band.description);
							xmlWriter.writeEndElement(); // Item
						}
					});
				} else if (!tiffTiledBands.isEmpty()) {
					tiffTiledBands.forEachIndexedThrowable((TiffBand band, int i) -> {
						if(band.description != null && !band.description.isEmpty()) {
							xmlWriter.writeStartElement("Item");
							xmlWriter.writeAttribute("name", "DESCRIPTION");
							xmlWriter.writeAttribute("sample", Integer.toString(i));
							xmlWriter.writeAttribute("role", "description");
							xmlWriter.writeCharacters(band.description);
							xmlWriter.writeEndElement(); // Item
						}
					});
				} else if (tiffComposite != null) {
					tiffComposite.bandDescriptions.forEachIndexedThrowable((String description, int i) -> {
						if(description != null && !description.isEmpty()) {
							xmlWriter.writeStartElement("Item");
							xmlWriter.writeAttribute("name", "DESCRIPTION");
							xmlWriter.writeAttribute("sample", Integer.toString(i));
							xmlWriter.writeAttribute("role", "description");
							xmlWriter.writeCharacters(description);
							xmlWriter.writeEndElement(); // Item
						}
					});
				} else {
					log.warn("IFD: no data in tiff");
				}
				xmlWriter.writeEndElement(); // GDALMetadata
				xmlWriter.writeEndDocument();
				xmlWriter.close();				
				String text = writer.toString();				
				ifd.add_GDAL_METADATA(text);
			} catch (Exception e) {
				log.warn(e);
			}
		}

		ifd.add_Orientation_top_left();

		return ifd;
	}

	public void writeTIFF(DataOutput out) throws IOException {		
		writeMetaTIFF(out);
		writeData(out);		
	}

	public void writeBigTIFF(DataOutput out) throws IOException {
		writeMetaBigTIFF(out);
		writeData(out);
	}

	/**
	 * Write meta
	 * @param out
	 * @return 
	 * @return start position of image data
	 * @throws IOException
	 */
	private int writeMetaTIFF(DataOutput out) throws IOException {
		out.writeInt(0x4d_4d_00_2a); //magic tiff header  big endian
		int IFDOffset = 0x00_00_00_08;
		out.writeInt(IFDOffset);		
		return createIFD().writeTIFF(out); // return exact written byte count including magic tiff header
	}


	/**
	 * Write meta
	 * @param out
	 * @return 
	 * @return start position of image data
	 * @throws IOException
	 */
	private long writeMetaBigTIFF(DataOutput out) throws IOException {
		out.writeLong(0x4d_4d_00_2b__00_08_00_00l);		
		//out.writeLong(0x00_08_00_00__4d_4d_00_2bl); // magic BigTIFF header, big endian, byte size of offsets
		long IFDOffset = 0x00_00_00_00__00_00_00_10l;  // Offset to first IFD
		out.writeLong(IFDOffset);		
		return createIFD().writeBigTIFF(out); // return exact written byte count including magic tiff header		
	}

	private void writeData(DataOutput out) throws IOException {
		if(!tiffBands.isEmpty()) {
			int samplesPerPixel = tiffBands.size();	
			for (int i = 0; i < samplesPerPixel; i++) {
				TiffBand band = tiffBands.get(i);
				band.writeData(out);
			}
		} else if(!tiffTiledBands.isEmpty()) {
			int samplesPerPixel = tiffTiledBands.size();	
			for (int i = 0; i < samplesPerPixel; i++) {
				TiffTiledBand band = tiffTiledBands.get(i);
				band.writeData(out);
			}
		} else if(tiffComposite != null){
			tiffComposite.writeData(out);
		} else {
			throw new RuntimeException("no data");
		}
	}

	public long estimateSize() {
		try {
			long h1 = writeMetaTIFF(DataOutputNull.DEFAULT);
			long h2 = writeMetaBigTIFF(DataOutputNull.DEFAULT);
			long pos = h1 < h2 ? h2 : h1;
			if(!tiffBands.isEmpty()) {
				for(TiffBand tiffBand:tiffBands) {
					pos += tiffBand.getDataSize();
				}
			} else if (!tiffTiledBands.isEmpty()) {
				for(TiffBand tiffBand:tiffTiledBands) {
					pos += tiffBand.getDataSize();
				}
			} else if (tiffComposite != null) {	
				pos += tiffComposite.getDataSize();
			} else {
				throw new RuntimeException("no data in tiff");
			}
			return pos;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public long exactSizeOfWriteAuto() {
		try {
			long pos = isAutoBigTiff() ? writeMetaBigTIFF(DataOutputNull.DEFAULT) : writeMetaTIFF(DataOutputNull.DEFAULT);
			if(!tiffBands.isEmpty()) {
				for(TiffBand tiffBand:tiffBands) {
					pos += tiffBand.getDataSize();
				}
			} else if (!tiffTiledBands.isEmpty()) {
				for(TiffBand tiffBand:tiffTiledBands) {
					pos += tiffBand.getDataSize();
				}
			} else if (tiffComposite != null) {	
				pos += tiffComposite.getDataSize();
			} else {
				throw new RuntimeException("no data in tiff");
			}
			return pos;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void writeAuto(DataOutput out) throws IOException {	
		if(isAutoBigTiff()) {
			writeBigTIFF(out);
		} else {
			writeTIFF(out);
		}
	}

	public boolean isAutoBigTiff() {
		long len = estimateSize();
		return len >= 2_000_000_000;
	}

	public void setTiffComposite(TiffComposite tiffComposite) {
		Objects.requireNonNull(tiffComposite);
		if(!tiffBands.isEmpty()) {
			throw new RuntimeException("there are bands");
		}
		if(tiffComposite != null) {
			log.warn("overwrite composite");
		}
		this.tiffComposite = tiffComposite;
	}

}
