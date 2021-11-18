package util.tiff;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.collections.vec.Vec;
import util.tiff.file.TiffFile;

public class TiffReader {
	private static final Logger log = LogManager.getLogger();

	public interface Seek {
		void seek(long pos) throws IOException;
	}

	public final String filename;

	public static void main(String[] args) throws FileNotFoundException, IOException {		
		//TiffReader tiffReader = new TiffReader("temp/testingTiff.tif");
		TiffReader tiffReader = new TiffReader("temp/out_testingTiff.tif");
		//TiffReader tiffReader = new TiffReader("temp/outzstd_testingTiff.tif");
		//TiffReader tiffReader = new TiffReader("temp/be_alb_rapideye_atm_rebuild__2015_03_19T00_00.tiff");
		tiffReader.read();
	}

	public TiffReader(String filename) {
		this.filename = filename;
	}

	public static class IfdEntry extends IFD_Entry {
		public final int type;
		public final int count;
		public final int offset;

		public IfdEntry(short tag, int type, int count, int offset) {
			super(tag);
			this.type = type;
			this.count = count;
			this.offset = offset;
		}

		public static IFD_Entry of(short tag, int type, int count, int offset, DataInput dataInput, Seek seek, boolean littleEndian) throws IOException {
			switch(type) {
			case 2: {
				if(count == 1) {
					byte data = (byte) (offset >> 24);
					return ofAscii(tag, new byte[] {data}); 
				} else if(count == 2) {
					byte data1 = (byte) (offset >> 24);
					byte data2 = (byte) (0xff & (offset >> 16));
					return ofAscii(tag, new byte[] {data1, data2}); 
				} else if(count == 3) {
					byte data1 = (byte) (offset >> 24);
					byte data2 = (byte) (0xff & (offset >> 16));
					byte data3 = (byte) (0xff & (offset >> 8));
					return ofAscii(tag, new byte[] {data1, data2, data3}); 
				} else if(count == 4) {
					byte data1 = (byte) (offset >> 24);
					byte data2 = (byte) (0xff & (offset >> 16));
					byte data3 = (byte) (0xff & (offset >> 8));
					byte data4 = (byte) (0xff & (offset));
					return ofAscii(tag, new byte[] {data1, data2, data3, data4}); 
				} else {
					seek.seek(offset);
					byte[] values = new byte[count];
					dataInput.readFully(values);
					return ofAscii(tag, values); 
				}
			}
			case 3:
				if(count == 1) {
					short data = littleEndian ? (short) (offset) : (short) (offset >> 16);
					return ofShort(tag, data); 
				} else if(count == 2) {
					short data1 = (short) (offset >> 16);
					short data2 = (short) (0x0000ffff & offset); // TODO check
					return ofShorts(tag, new short[] {data1, data2}); 
				} else {
					seek.seek(offset);
					short[] values = new short[count];
					for (int i = 0; i < count; i++) {
						values[i] = dataInput.readShort();
					}
					return ofShorts(tag, values); 
				}
			case 4:
				if(count == 1) {
					int data = offset;
					return ofInt(tag, data); 
				} else {
					seek.seek(offset);
					int[] values = new int[count];
					for (int i = 0; i < count; i++) {
						values[i] = dataInput.readInt();
					}
					return ofInts(tag, values); 
				}
			case 5:
				if(count == 1) {
					seek.seek(offset);
					int data1a = dataInput.readInt();
					int data1b = dataInput.readInt();					
					return ofRational(tag, data1a, data1b); 
				} else {
					seek.seek(offset);
					int[] values = new int[count];
					for (int i = 0; i < count; i++) {
						values[i] = dataInput.readInt();
					}
					return ofInts(tag, values); 
				}
			case 12: {				
				seek.seek(offset);
				double[] values = new double[count];
				for (int i = 0; i < count; i++) {
					values[i] = dataInput.readDouble();
				}
				return ofDoubles(tag, values); 
			}
			default:
				return new IfdEntry(tag, type, count, offset);
			}
		}

		private static IFD_short ofShort(short tag, short value) {
			switch(tag) {
			default:
				return new IFD_short(tag, value);
			}
		}

		private static IFD_shorts ofShorts(short tag, short[] values) {
			switch(tag) {
			default:
				return new IFD_shorts(tag, values);
			}
		}

		private static IFD_int ofInt(short tag, int value) {
			switch(tag) {
			default:
				return new IFD_int(tag, value);
			}
		}

		private static IFD_rational ofRational(short tag, int value1a, int value1b) {
			switch(tag) {
			default:
				return new IFD_rational(tag, value1a, value1b);
			}
		}

		private static IFD_ints ofInts(short tag, int[] values) {
			switch(tag) {
			default:
				return new IFD_ints(tag, values);
			}
		}

		private static IFD_ASCII ofAscii(short tag, byte[] values) {
			String text = new String(values, StandardCharsets.UTF_8);
			switch(tag) {
			default:
				return new IFD_ASCII(tag, text);
			}
		}

		private static IFD_doubles ofDoubles(short tag, double[] values) {
			switch(tag) {
			default:
				return new IFD_doubles(tag, values);
			}
		}

		@Override
		public String toString() {
			switch(type) {
			case 1:
				return tagToText(id) + " " + (count==1 ? "" : count + "x ") + "uint8  offset:" + offset + "";
			case 2:
				return tagToText(id) + " " + (count==1 ? "" : count + "x ") + "ascii  offset:" + offset + "";
			case 3:
				return tagToText(id) + " " + (count==1 ? "" : count + "x ") + "uint16 offset:" + offset + "";
			case 4:
				return tagToText(id) + " " + (count==1 ? "" : count + "x ") + "uint32  offset:" + offset + "";
			case 5:
				return tagToText(id) + " " + (count==1 ? "" : count + "x ") + "rational64  offset:" + offset + "";
			case 12:
				return tagToText(id) + " " + (count==1 ? "" : count + "x ") + "float64  offset:" + offset + "";
			default:
				return tagToText(id) + " " + (count==1 ? "" : count + "x ") + "type:" + type + " offset:" + offset + "";
			}
		}

		public static String tagToText(short id) {
			switch(id) {
			case 254:
				return "NewSubfileType";			
			case 256:
				return "ImageWidth";
			case 257:
				return "ImageLength";
			case 258:
				return "BitsPerSample";
			case 259:
				return "Compression";
			case 262:
				return "PhotometricInterpretation";
			case 270:
				return "ImageDescription";
			case 274:
				return "Orientation";
			case 277:
				return "SamplesPerPixel";
			case 282:
				return "XResolution";
			case 283:
				return "YResolution";
			case 284:
				return "PlanarConfiguration";
			case 296:
				return "ResolutionUnit";
			case 305:
				return "Software";
			case 306:
				return "DateTime";
			case 317:
				return "Predictor";
			case 322:
				return "TileWidth";
			case 323:
				return "TileLength";
			case 324:
				return "TileOffsets";
			case 325:
				return "TileByteCounts";
			case 338:
				return "ExtraSamples";
			case 339:
				return "SampleFormat";
			case (short) 33550: // GeoTIFF
				return "ModelPixelScaleTag";
			case (short) 33922: // GeoTIFF
				return "ModelTiepointTag";
			case (short) 34735: // GeoTIFF
				return "GeoKeyDirectoryTag";
			case (short) 34736: // GeoTIFF
				return "GeoDoubleParamsTag";
			case (short) 34737: // GeoTIFF
				return "GeoAsciiParamsTag";
			case (short) 42112: // GDAL
				return "GDAL_METADATA";
			case (short) 42113: // GDAL
				return "GDAL_NODATA";
			default:
				return "tag:"+ Short.toUnsignedInt(id);
			}
		}

		@Override
		public void writeIFD_entryTIFF(DataOutput out, int data_pos, int image_data_pos) throws IOException {
			throw new RuntimeException("not implemented");			
		}

		@Override
		public void writeIFD_entryBigTIFF(DataOutput out, long data_pos, long image_data_pos) throws IOException {
			throw new RuntimeException("not implemented");			
		}
	}

	public int readIFDs(Vec<IFD_Entry> collector, RandomAccessFile raf, DataInput dataInput, boolean littleEndian) throws IOException {
		long idfPos = raf.getFilePointer();
		//log.info("idfPos " + idfPos);
		short ifdLen = dataInput.readShort();
		log.info("IFD entries " + ifdLen);
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(ifdLen * 12);
		if(littleEndian) {
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
		raf.getChannel().read(byteBuffer);
		byteBuffer.flip();
		for (int i = 0; i < ifdLen; i++) {
			short tag = byteBuffer.getShort();
			short type = byteBuffer.getShort();
			int count = byteBuffer.getInt();
			int offset = byteBuffer.getInt();
			//log.info("tag" + tag);
			IFD_Entry ifdEntry = IfdEntry.of(tag, type, count, offset, dataInput, raf::seek, littleEndian);
			collector.add(ifdEntry);
		}
		raf.seek(idfPos + 2 + ifdLen * 12);
		//log.info("pos " + raf.getFilePointer());
		int subtiffIFOOffset = dataInput.readInt();
		return subtiffIFOOffset;
	}

	public void read() throws FileNotFoundException, IOException {
		try(RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
			int tiffSignature = raf.readInt();
			if(tiffSignature == TiffFile.TIFFsignatureBE) {
				int tiffIFOOffset = raf.readInt();
				log.info("IFD offset " + tiffIFOOffset);				
				Vec<IFD_Entry> ifdEntries = new Vec<IFD_Entry>();
				int nextIfdPos = tiffIFOOffset;
				int IFDcount = 0;
				while(nextIfdPos != 0) {
					raf.seek(nextIfdPos);
					nextIfdPos = readIFDs(ifdEntries, raf, raf, false);
					IFDcount++;
				}
				for(IFD_Entry ifdEntry : ifdEntries) {
					log.info(ifdEntry);
				}
				log.info("IFDcount " + IFDcount);
			} else if(tiffSignature == TiffFile.TIFFsignatureLEinBE) {
				LittleEndianRafDataInput dataInput = new LittleEndianRafDataInput(raf);
				int tiffIFOOffset = dataInput.readInt();
				//log.info("IFD offset " + tiffIFOOffset);
				Vec<IFD_Entry> ifdEntries = new Vec<IFD_Entry>();
				int nextIfdPos = tiffIFOOffset;
				int IFDcount = 0;
				while(nextIfdPos != 0) {
					raf.seek(nextIfdPos);
					nextIfdPos = readIFDs(ifdEntries, raf, dataInput, true);
					IFDcount++;
				}
				for(IFD_Entry ifdEntry : ifdEntries) {
					log.info(ifdEntry);
				}
				log.info("IFDcount " + IFDcount);
			} else {
				throw new RuntimeException("invalid TIFF signature");
			}
		}
	}

}
