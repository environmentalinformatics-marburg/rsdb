package util.tiff;

import java.io.DataOutput;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;


import org.tinylog.Logger;

public class IFD {
	

	/**
	 * 64 bit unsigned integer
	 */
	public static final short DATA_TYPE_UINT32 = 4; 
	public static final short DATA_TYPE_UINT64 = 16; 

	private static final int IFD_TIFF_ENTRY_SIZE = 12;
	private static final int IFD_BIGTIFF_ENTRY_SIZE = 20;

	private ArrayList<IFD_Entry> list = new ArrayList<IFD_Entry>(20);

	public static int writeTIFF(int ifdBytePos, DataOutput out, RandomAccessFile raf, IFD... ifds) throws IOException {
		if(ifdBytePos % 2 == 1) {
			throw new RuntimeException("position error not aligned: " + ifdBytePos);
		}
		int dataBytePos = ifdBytePos;
		Logger.info("dataBytePos " + dataBytePos);
		int imageDataBytePos = ifdBytePos;

		for (int i = 0; i < ifds.length; i++) {
			IFD ifd = ifds[i];
			ifd.list.sort(null);
			short ifdTableLen = (short) ifd.list.size();
			int ifdTableByteSize = 2 + ifdTableLen * IFD_TIFF_ENTRY_SIZE + 4;
			imageDataBytePos += ifdTableByteSize;
			for(IFD_Entry e: ifd.list) {
				imageDataBytePos += e.data_sizeTIFF();
				if(imageDataBytePos % 2 == 1) {
					imageDataBytePos++;
				}
			}
		}		

		for (int i = 0; i < ifds.length; i++) {
			boolean hasNextIfd = i < ifds.length - 1;
			IFD ifd = ifds[i];			
			short ifdTableLen = (short) ifd.list.size();
			Logger.info("ifdTableLen " + ifdTableLen);
			out.writeShort(ifdTableLen);
			int ifdTableByteSize = 2 + ifdTableLen * IFD_TIFF_ENTRY_SIZE + 4;
			dataBytePos += ifdTableByteSize;
			for(IFD_Entry e : ifd.list) {
				Logger.info("dataBytePos " + dataBytePos);
				e.writeIFD_entryTIFF(out, dataBytePos, imageDataBytePos);
				dataBytePos += e.data_sizeTIFF();
				if(dataBytePos % 2 == 1) {
					dataBytePos++;
				}
			}
			if(hasNextIfd) {
				out.writeInt(dataBytePos); // start of next IFD
			} else {
				out.writeInt(0x00_00_00_00); // end of IFDs marker
			}
			ifdBytePos += ifdTableByteSize;
			for(IFD_Entry e : ifd.list) {
				Logger.info("filepos " + raf.getFilePointer() + "  " + ifdBytePos);
				e.write_dataTIFF(out);
				ifdBytePos += e.data_sizeTIFF();
				if(ifdBytePos % 2 == 1) {
					out.writeByte(0);
					ifdBytePos++;
				}
			}			
		}

		if(dataBytePos != imageDataBytePos) {
			throw new RuntimeException("error");
		}
		return imageDataBytePos;			
	}
	
	public static long writeBigTIFF(long ifdBytePos, DataOutput out, RandomAccessFile raf, IFD... ifds) throws IOException {
		if(ifdBytePos % 2 == 1) {
			throw new RuntimeException("position error not aligned: " + ifdBytePos);
		}
		long dataBytePos = ifdBytePos;
		Logger.info("dataBytePos " + dataBytePos);
		long imageDataBytePos = ifdBytePos;

		for (int i = 0; i < ifds.length; i++) {
			IFD ifd = ifds[i];
			ifd.list.sort(null);
			long ifdTableLen = ifd.list.size();
			long ifdTableByteSize = 8 + ifdTableLen * IFD_BIGTIFF_ENTRY_SIZE + 8;
			imageDataBytePos += ifdTableByteSize;
			for(IFD_Entry e: ifd.list) {
				imageDataBytePos += e.data_sizeBigTIFF();
				if(imageDataBytePos % 2 == 1) {
					imageDataBytePos++;
				}
			}
		}		

		for (int i = 0; i < ifds.length; i++) {
			boolean hasNextIfd = i < ifds.length - 1;
			IFD ifd = ifds[i];			
			long ifdTableLen = ifd.list.size();
			Logger.info("ifdTableLen " + ifdTableLen);
			out.writeLong(ifdTableLen);
			long ifdTableByteSize = 8 + ifdTableLen * IFD_BIGTIFF_ENTRY_SIZE + 8;
			dataBytePos += ifdTableByteSize;
			for(IFD_Entry e : ifd.list) {
				Logger.info("dataBytePos " + dataBytePos);
				e.writeIFD_entryBigTIFF(out, dataBytePos, imageDataBytePos);
				dataBytePos += e.data_sizeBigTIFF();
				if(dataBytePos % 2 == 1) {
					dataBytePos++;
				}
			}
			if(hasNextIfd) {
				out.writeLong(dataBytePos); // start of next IFD
			} else {
				out.writeLong(0x00_00_00_00__00_00_00_00); // end of IFDs marker
			}
			ifdBytePos += ifdTableByteSize;
			for(IFD_Entry e : ifd.list) {
				Logger.info("filepos " + raf.getFilePointer() + "  " + ifdBytePos);
				e.write_dataBigTIFF(out);
				ifdBytePos += e.data_sizeBigTIFF();
				if(ifdBytePos % 2 == 1) {
					out.writeByte(0);
					ifdBytePos++;
				}
			}			
		}

		if(dataBytePos != imageDataBytePos) {
			throw new RuntimeException("error  dataBytePos != imageDataBytePos     " + dataBytePos + "    " + imageDataBytePos);
		}
		return imageDataBytePos;			
	}

	/**
	 * Write IFD Tags
	 * @param out
	 * @param overviewIterator 
	 * @return 
	 * @return start position of image data
	 * @throws IOException
	 */
	public int writeTIFF(DataOutput out) throws IOException {
		list.sort(null);
		short NumDirEntries = (short) list.size();
		out.writeShort(NumDirEntries);
		int IFD_ENTRY_SIZE = 12;
		int IFD_SIZE = NumDirEntries * IFD_ENTRY_SIZE + 2 + 4;
		int pos = 10 + IFD_SIZE;
		int data_pos = pos;
		int image_data_pos = data_pos;
		for(IFD_Entry e:list) {
			image_data_pos += e.data_sizeTIFF();
			if(image_data_pos % 2 == 1) {
				image_data_pos++;
			}
		}
		for(IFD_Entry e:list) {
			e.writeIFD_entryTIFF(out, data_pos, image_data_pos);
			data_pos += e.data_sizeTIFF();
			if(data_pos % 2 == 1) {
				data_pos++;
			}
		}
		out.writeShort(0x0000); // end of IFD tag
		out.writeInt(0x00_00_00_00); // end of IFDs marker
		for(IFD_Entry e:list) {
			e.write_dataTIFF(out);
			pos += e.data_sizeTIFF();
			if(pos % 2 == 1) {
				out.writeByte(0);
				pos++;
			}
		}
		if(image_data_pos != pos) {
			throw new RuntimeException("error");
		}
		return image_data_pos;
	}

	/**
	 * Write IFD Tags
	 * @param out
	 * @return start position of image data
	 * @throws IOException
	 */
	public long writeBigTIFF(DataOutput out) throws IOException {
		list.sort(null);
		long NumDirEntries = list.size();
		out.writeLong(NumDirEntries);
		long IFD_ENTRY_SIZE = 20;
		long IFD_SIZE = NumDirEntries * IFD_ENTRY_SIZE + 2 + 4;
		long pos = 28 + IFD_SIZE;
		long data_pos = pos;
		long image_data_pos = data_pos;
		for(IFD_Entry e:list) {
			image_data_pos += e.data_sizeBigTIFF();
			if(image_data_pos % 2 == 1) {
				image_data_pos++;
			}
		}
		for(IFD_Entry e:list) {
			e.writeIFD_entryBigTIFF(out, data_pos, image_data_pos);
			//Logger.info("bigTIFF write tag " + Util.hex(e.id) + "  " + e.getClass().getSimpleName());
			data_pos += e.data_sizeBigTIFF();
			if(data_pos % 2 == 1) {
				data_pos++;
			}
		}
		out.writeShort(0x0000); // end of IFD tag
		out.writeLong(0x00_00_00_00__00_00_00_00l); // end of IFDs marker
		for(IFD_Entry e:list) {
			e.write_dataBigTIFF(out);
			//Logger.info("bigTIFF write tag data " + Util.hex(e.id) + "  " + e.getClass().getSimpleName());
			pos += e.data_sizeBigTIFF();
			if(pos % 2 == 1) {
				out.writeByte(0);
				pos++;
			}
		}
		if(image_data_pos != pos) {
			throw new RuntimeException("error");
		}
		return image_data_pos;
	}

	public void add(IFD_Entry e) {
		list.add(e);
	}
	
	public void add_NewSubfileType(int flags) {
		add(new IFD_int((short)0x0fe, flags));
	}
	
	public void add_NewSubfileType_reduced_resolution() {
		add_NewSubfileType(1);
	}
	
	public void add_ImageWidth(int width) {
		add(IFD_int.ofAuto((short) 0x0100, width));
	}

	public void add_ImageWidth_uint16(short width) {
		add(new IFD_short((short) 0x0100, width));
	}
	
	public void add_ImageWidth_uint32(int width) {
		add(new IFD_int((short) 0x0100, width));
	}
	
	public void add_ImageLength(int height) {
		add(IFD_int.ofAuto((short) 0x0101, height));
	}

	public void add_ImageLength_uint16(short height) {
		add(new IFD_short((short) 0x0101, height));
	}
	
	public void add_ImageLength_uint32(int height) {
		add(new IFD_int((short) 0x0101, height));
	}

	public void add_BitsPerSample(short samples, short bits) {
		short[] bps = new short[samples];
		for (int i = 0; i < bps.length; i++) {
			bps[i] = bits;
		}
		add_BitsPerSample(bps);
	}

	public void add_BitsPerSample(short[] bits) {
		add(new IFD_shorts((short) 0x0102, bits));
	}

	public void add_Compression(short compressionType) {
		add(new IFD_short((short) 0x0103, compressionType));
	}

	public void add_PhotometricInterpretation(short photometricInterpretationType) {
		add(new IFD_short((short) 0x0106, photometricInterpretationType));
	}

	public void add_StripOffsets(int... stripByteCounts) {
		add(new IFD_StripOffsets(stripByteCounts));
	}

	public void add_StripOffsets(long... stripByteCounts) {
		add(new IFD_StripOffsets(stripByteCounts));
	}

	public void add_SamplesPerPixel(short samplesPerPixel) {
		add(new IFD_short((short) 0x0115, samplesPerPixel));
	}

	public void add_RowsPerStrip(int rowsPerStrip) {
		add(IFD_int.ofAuto((short) 0x0116, rowsPerStrip));
	}

	public void add_StripByteCounts(int... stripByteCounts) {
		add(new IFD_ints((short) 0x0117, stripByteCounts));
	}

	public void add_StripByteCounts(long... stripByteCounts) {
		add(new IFD_longs((short) 0x0117, stripByteCounts));
	}

	public void add_XResolution(int a, int b) {
		add(new IFD_rational((short) 0x011a, a, b));
	}

	public void add_YResolution(int a, int b) {
		add(new IFD_rational((short) 0x011b, a, b));
	}

	public void add_ResolutionUnit(short resolutionUnit) {
		add(new IFD_short((short) 0x0128, resolutionUnit));
	}

	public void add_ResolutionUnit_noAbsoluteUnit() {
		add_ResolutionUnit((short) 1);
	}

	public void add_SampleFormat(short samplesPerPixel, short sampleFormat) {
		short[] sampleFormats = new short[samplesPerPixel];
		for (int i = 0; i < sampleFormats.length; i++) {
			sampleFormats[i] = sampleFormat;
		}
		add_SampleFormat(sampleFormats);
	}

	public void add_ExtraSamples(short[] extraSamples) {
		Objects.requireNonNull(extraSamples);
		add(new IFD_shorts((short) 0x0152, extraSamples));
	}

	public void add_SampleFormat(short[] sampleFormats) {
		add(new IFD_shorts((short) 0x0153, sampleFormats));
	}

	public void add_PlanarConfiguration_Chunky() {
		add(new IFD_short((short) 0x011c, (short) 1));
	}

	public void add_PlanarConfiguration_Planar() {
		add(new IFD_short((short) 0x011c, (short) 2));
	}

	public void add_Orientation_top_left() {
		add(new IFD_short((short) 0x0112, (short) 1));
	}

	public void add_Predictor(short predictor) {
		add(new IFD_short((short) 0x013d, (short) predictor));
	}

	public void add_geotiff_ModelPixelScaleTag(double x, double y) {
		add(new IFD_doubles((short)0x830E, x, y, 0d));
	}

	public void add_geotiff_ModelTiepointTag(double x, double y, double geoX, double geoY) {
		double I = x; // raster x
		double J = y; // raster y
		double K = 0; // raster z
		double X = geoX; // crs easting
		double Y = geoY; // crs northing
		double Z = 0; // crs elevation
		add(new IFD_doubles((short)0x8482, I, J, K, X, Y, Z));
	}

	public void add_GeoKeyDirectory(GeoKeyDirectory geoKeyDirectory) { // GeoKeyDirectory will not be sorted
		add_geotiff_GeoKeyDirectoryTag(geoKeyDirectory.get_directory_content());
		add_geotiff_GeoDoubleParamsTag();
		add_geotiff_GeoAsciiParamsTag(geoKeyDirectory.get_ascii_content());
	}

	public void add_geotiff_GeoKeyDirectoryTag(short[] content) {
		add(new IFD_shorts((short) 0x87AF, content));
	}

	public void add_geotiff_GeoDoubleParamsTag() {
		double[] params = new double[2];
		add(new IFD_doubles((short)0x87B0, params));
	}

	public void add_geotiff_GeoAsciiParamsTag(CharSequence ascii) {
		add(new IFD_ASCII((short)0x87B1, ascii));
	}

	public void add_ImageDescription(String text) { // TIFFTAG_IMAGEDESCRIPTION
		add(new IFD_ASCII((short)0x010E, text));
	}

	public void add_Software(String text) { // TIFFTAG_SOFTWARE
		add(new IFD_ASCII((short)0x0131, text));
	}

	private static final DateTimeFormatter dataTimeFormaterTiff = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
	//private static final DateTimeFormatter dataTimeFormaterTiff = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm");

	public void add_DateTime_now() { // TIFFTAG_DATETIME
		LocalDateTime now = LocalDateTime.now();		
		add(new IFD_ASCII((short)0x0132, dataTimeFormaterTiff.format(now)));
	}

	public void add_TileWidth(short width) {
		add(new IFD_short((short) 0x0142, width));
	}

	public void add_TileLength(short height) {
		add(new IFD_short((short) 0x0143, height));
	}

	public void add_TileOffsets_of_tileByteCounts(int... tileByteCounts) {
		add(new IFD_Data_Offsets_of_image_data_lenghts((short) 0x0144, tileByteCounts));
	}

	public void add_TileOffsets_of_tileByteCounts(long... tileByteCounts) {
		add(new IFD_Data_Offsets_of_image_data_lenghts((short) 0x0144, tileByteCounts));
	}

	public void add_TileOffsets_direct(long... offsets) {
		add(new IFD_longs((short) 0x0144, offsets));
	}

	public void add_TileByteCounts(long... stripByteCounts) {
		add(new IFD_longs((short) 0x0145, stripByteCounts));
	}

	public void add_GDAL_NODATA(short noDataValue) { // TIFF Tag GDAL_NODATA
		add(new IFD_ASCII((short)0xA481, Short.toString(noDataValue)));
	}

	public void add_GDAL_NODATA_float_NaN() { // TIFF Tag GDAL_NODATA
		add(new IFD_ASCII((short)0xA481, "nan"));
	}

	public void add_GDAL_METADATA(String text) { // TIFF Tag GDAL_METADATA,  https://www.awaresystems.be/imaging/tiff/tifftags/gdal_metadata.html
		add(new IFD_ASCII((short)0xA480, text));
	}	
}