package pointdb.las;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.BitSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pointcloud.CellTable;
import pointdb.base.Point;
import util.Timer;

public class Las {
	private static final Logger log = LogManager.getLogger();

	private final static int LASF_SIGNATUR = 1179861324;

	private final static byte LAS_RECORD_TYPE_0 = 0;
	private final static byte LAS_RECORD_TYPE_1 = 1;
	private final static byte LAS_RECORD_TYPE_2 = 2;
	private final static byte LAS_RECORD_TYPE_3 = 3;
	private final static byte LAS_RECORD_TYPE_4 = 4;
	private final static byte LAS_RECORD_TYPE_5 = 5;
	private final static byte LAS_RECORD_TYPE_6 = 6;
	private final static byte LAS_RECORD_TYPE_7 = 7;
	private final static byte LAS_RECORD_TYPE_8 = 8;
	private final static byte LAS_RECORD_TYPE_9 = 9;
	private final static byte LAS_RECORD_TYPE_10 = 10;

	public final Path filename;
	private final FileChannel filechannel;
	private final long filesize;

	private short global_Encoding;

	private byte version_Major;
	private byte version_Minor;

	private short creation_Day_of_Year;
	private short creation_Year;

	private short header_Size;
	private int number_of_Variable_Length_Records;

	long offset_to_point_data;
	public int point_Data_Record_Length;
	public long number_of_point_records;

	int point_data_record_format;

	public double[] offset;
	public double[] scale_factor;

	public double[] min; //min x,y,z in projection coordinates
	public double[] max; //max x,y,z in projection coordinates

	public Las(Path filename) throws IOException {
		this.filename = filename;
		this.filechannel = FileChannel.open(filename, StandardOpenOption.READ);
		this.filesize = filechannel.size();
		readHeader();
	}

	/*public LocalDate getCreationDate() {
		return LocalDate.ofYearDay(creation_Year, creation_Day_of_Year);
	}*/

	@SuppressWarnings("unused")
	private void readHeader() throws IOException {
		final int HEADER_SIZE = 1000;
		final int headerbuffersize = (int) (HEADER_SIZE<filesize?HEADER_SIZE:filesize);
		ByteBuffer mappedByteBuffer = java.nio.ByteBuffer.allocate((int) headerbuffersize);
		mappedByteBuffer.rewind();
		int ret = filechannel.read(mappedByteBuffer, 0);
		mappedByteBuffer.rewind();
		if(ret!=headerbuffersize) {
			throw new RuntimeException("file header read error");
		}		
		//MappedByteBuffer mappedByteBuffer = filechannel.map(MapMode.READ_ONLY, 0, headerbuffersize); //removed memmory mapped
		mappedByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		int signature = mappedByteBuffer.getInt();
		if(signature!=LASF_SIGNATUR) {
			throw new RuntimeException("wrong signatur "+signature);
		}

		short source_ID = mappedByteBuffer.getShort();
		//System.out.println("source_ID "+source_ID);
		global_Encoding = mappedByteBuffer.getShort();
		//System.out.println("global_Encoding "+global_Encoding);

		int project_ID_1 = mappedByteBuffer.getInt();
		short project_ID_2 = mappedByteBuffer.getShort();
		short project_ID_3 = mappedByteBuffer.getShort();
		long project_ID_4 = mappedByteBuffer.getLong();

		version_Major = mappedByteBuffer.get();
		version_Minor = mappedByteBuffer.get();
		//System.out.println("version "+version_Major+"."+version_Minor);

		byte[] system_Identifier_bytes = new byte[32];
		mappedByteBuffer.get(system_Identifier_bytes);
		//String system_Identifier = new String(system_Identifier_bytes, StandardCharsets.US_ASCII));
		//System.out.println("system_Identifier "+system_Identifier);

		byte[] generating_Software_bytes = new byte[32];
		mappedByteBuffer.get(generating_Software_bytes);
		//String generating_Software = new String(generating_Software_bytes, StandardCharsets.US_ASCII));
		//System.out.println("generating_Software "+generating_Software);

		creation_Day_of_Year = mappedByteBuffer.getShort();
		creation_Year = mappedByteBuffer.getShort();
		//System.out.println("file creation "+creation_Year+" "+creation_Day_of_Year);

		header_Size = mappedByteBuffer.getShort();
		//System.out.println("header_Size "+header_Size);

		offset_to_point_data = Integer.toUnsignedLong(mappedByteBuffer.getInt());
		//System.out.println("offset_to_point_data "+offset_to_point_data);

		number_of_Variable_Length_Records = mappedByteBuffer.getInt();
		//log.info("number_of_Variable_Length_Records "+number_of_Variable_Length_Records);

		point_data_record_format = Byte.toUnsignedInt(mappedByteBuffer.get());
		//System.out.println("point_data_record_format "+point_data_record_format);

		point_Data_Record_Length = mappedByteBuffer.getShort();
		//System.out.println("point_Data_Record_Length "+point_Data_Record_Length);

		number_of_point_records = Integer.toUnsignedLong(mappedByteBuffer.getInt()); //legacy
		//System.out.println("number_of_point_records "+number_of_point_records);

		long[] number_of_points_by_return = new long[5]; //legacy
		for(int i=0;i<number_of_points_by_return.length;i++) { //legacy
			number_of_points_by_return[i] = Integer.toUnsignedLong(mappedByteBuffer.getInt()); //legacy   
		}
		//System.out.println("number_of_points_by_return  "+Arrays.toString(number_of_points_by_return));

		scale_factor = new double[3];
		for(int i=0;i<scale_factor.length;i++) {
			scale_factor[i] = mappedByteBuffer.getDouble();
		}
		//System.out.println("scale_factor "+Arrays.toString(scale_factor));

		offset = new double[3];
		for(int i=0;i<offset.length;i++) {
			offset[i] = mappedByteBuffer.getDouble();
		}
		//System.out.println("offset "+Arrays.toString(offset));

		min = new double[3];
		max = new double[3];
		for(int i=0;i<min.length;i++) {
			max[i] = mappedByteBuffer.getDouble();
			min[i] = mappedByteBuffer.getDouble();
		}
		//System.out.println("min "+Arrays.toString(min));
		//System.out.println("max "+Arrays.toString(max));

		if(version_Major != 1) {
			throw new RuntimeException("error unknown version "+version_Major);
		}

		if(version_Minor>2) {
			long start_of_Waveform_Data_Packet_Record = mappedByteBuffer.getLong();
			//log.info("start_of_Waveform_Data_Packet_Record "+start_of_Waveform_Data_Packet_Record);
		}

		if(version_Minor>3) {
			long start_of_first_Extended_Variable_Length_Record = mappedByteBuffer.getLong();
			long Number_of_Extended_Variable_Length_Records = mappedByteBuffer.getInt();
			number_of_point_records = mappedByteBuffer.getLong(); //overwrite legacy
			number_of_points_by_return = new long[15]; //overwrite legacy
			for(int i=0;i<number_of_points_by_return.length;i++) { //overwrite legacy
				number_of_points_by_return[i] = mappedByteBuffer.getLong(); //overwrite legacy   
			}
		}

		if(mappedByteBuffer.position()!=header_Size) {
			log.warn("header longer than read bytes: " + mappedByteBuffer.position() +" of " + header_Size);
		}

		readEPSG();
	}

	private final static int GTModelTypeGeoKey = 1024;
	private final static int GTRasterTypeGeoKey = 1025;
	private final static int GTCitationGeoKey = 1026;	
	private final static int GeographicTypeGeoKey = 2048;
	private final static int GeogCitationGeoKey  = 2049;
	private final static int GeogGeodeticDatumGeoKey  = 2050;
	private final static int GeogPrimeMeridianGeoKey  = 2051;
	private final static int GeogLinearUnitsGeoKey   = 2052;
	private final static int GeogAngularUnitsGeoKey   = 2054;
	private final static int GeogEllipsoidGeoKey   = 2056;	
	private final static int GeogSemiMajorAxisGeoKey   = 2057;
	private final static int GeogInvFlatteningGeoKey   = 2059;
	private final static int GeogAzimuthUnitsGeoKey    = 2060;
	private final static int GeogPrimeMeridianLongGeoKey   = 2061;	
	public final static int ProjectedCSTypeGeoKey   = 3072;
	private final static int PCSCitationGeoKey   = 3073;
	private final static int ProjectionGeoKey   = 3074;
	private final static int ProjCoordTransGeoKey   = 3075;
	private final static int ProjLinearUnitsGeoKey   = 3076;
	private final static int ProjNatOriginLongGeoKey   = 3080;
	private final static int ProjNatOriginLatGeoKey   = 3081;
	private final static int ProjFalseEastingGeoKey    = 3082;
	private final static int ProjFalseNorthingGeoKey   = 3083;
	private final static int ProjScaleAtNatOriginGeoKey   = 3092;
	private final static int VerticalUnitsGeoKey   = 4099;

	private static class GeoKeyTag {
		public final int wKeyID;
		public final int wTIFFTagLocation;
		public final int wCount;
		public final int wValue_Offset;

		public GeoKeyTag(int wKeyID, int wTIFFTagLocation, int wCount, int wValue_Offset) {
			this.wKeyID = wKeyID;
			this.wTIFFTagLocation = wTIFFTagLocation;
			this.wCount = wCount;
			this.wValue_Offset = wValue_Offset;
		}

		public int getInt() {
			switch(wTIFFTagLocation) {
			case 0:
				if(wCount != 1) {
					throw new RuntimeException("wCount is not 1: " + wCount);
				}
				return wValue_Offset;
			case 34736:
				throw new RuntimeException("wTIFFTagLocation is no integer: " + "double");
			case 34737:
				throw new RuntimeException("wTIFFTagLocation is no integer: " + "acii");
			default:
				throw new RuntimeException("unknown wTIFFTagLocation: " + wTIFFTagLocation);
			}
		}

		public double getDouble(double[] doubles) {
			switch(wTIFFTagLocation) {
			case 0:
				throw new RuntimeException("wTIFFTagLocation is no double: " + "integer");
			case 34736:
				if(wCount != 1) {
					throw new RuntimeException("wCount is not 1: " + wCount);
				}
				return doubles[wValue_Offset];
			case 34737:
				throw new RuntimeException("wTIFFTagLocation is no double: " + "acii");
			default:
				throw new RuntimeException("unknown wTIFFTagLocation: " + wTIFFTagLocation);
			}
		}

		public String getString(byte[] asciiBytes) {
			switch(wTIFFTagLocation) {
			case 0:
				throw new RuntimeException("wTIFFTagLocation is no ascii: " + "integer");
			case 34736:
				throw new RuntimeException("wTIFFTagLocation is no ascii: " + "double");
			case 34737:
				return new String(asciiBytes, wValue_Offset, wCount, StandardCharsets.US_ASCII);
			default:
				throw new RuntimeException("unknown wTIFFTagLocation: " + wTIFFTagLocation);
			}
		}
	}

	public static int getEPSGfromProjectedCSTypeGeoKey(int epsgRaw) {
		if(epsgRaw == 0) {
			log.warn("no epsg: " + epsgRaw + " -> undefined");
			return 0;
		}
		if(epsgRaw == 32767) {
			log.warn("no epsg: " + epsgRaw + " -> user-defined");
			return 0;
		}
		if(epsgRaw >= 32768) {
			log.warn("no epsg: " + epsgRaw + " -> private user implementations");
			return 0;
		}
		return epsgRaw;
	}


	public int readEPSG() throws IOException {
		int epsg = 0;
		try {
			int VLR_HEADER_LENGTH = 54;
			ByteBuffer vlrHeaderBuffer = ByteBuffer.allocateDirect(VLR_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
			filechannel.position(header_Size);

			GeoKeyTag[] geoKeyTags = new GeoKeyTag[0];
			double[] doubles = new double[0];
			byte[] asciiBytes = new byte[0];

			for (int vlrIndex = 0; vlrIndex < number_of_Variable_Length_Records; vlrIndex++) {
				vlrHeaderBuffer.rewind();
				int ret = filechannel.read(vlrHeaderBuffer);
				if(ret != VLR_HEADER_LENGTH) {
					throw new RuntimeException("file read error");
				}
				vlrHeaderBuffer.rewind();
				int reserved = (int) vlrHeaderBuffer.getChar();
				byte[] userIDBytes = new byte[16];			
				vlrHeaderBuffer.get(userIDBytes);
				String userID = new String(userIDBytes, StandardCharsets.US_ASCII);
				int recordID = (int) vlrHeaderBuffer.getChar();
				int recordLengthAfterHeader = (int) vlrHeaderBuffer.getChar();
				byte[] descriptionBytes = new byte[32];
				vlrHeaderBuffer.get(descriptionBytes);
				String description = new String(descriptionBytes, StandardCharsets.US_ASCII);
				//log.info("VLR " + vlrIndex);
				//log.info("reserved " + reserved);
				//log.info("userID " + userID);
				//log.info("recordID " + recordID);
				//log.info("recordLengthAfterHeader " + recordLengthAfterHeader);
				//log.info("description " + description);
				ByteBuffer vlrContentBuffer = ByteBuffer.allocateDirect(recordLengthAfterHeader).order(ByteOrder.LITTLE_ENDIAN);
				filechannel.read(vlrContentBuffer);
				vlrContentBuffer.rewind();

				switch(recordID) {
				case 34735: {
					//log.info("GeoKeyDirectoryTag Record");
					int wKeyDirectoryVersion = (int) vlrContentBuffer.getChar();
					if(wKeyDirectoryVersion != 1) {
						throw new RuntimeException("error in GeoKeyDirectoryTag wKeyDirectoryVersion is not 1: " + wKeyDirectoryVersion);
					}
					int wKeyRevision = (int) vlrContentBuffer.getChar();
					if(wKeyRevision != 1) {
						throw new RuntimeException("error in GeoKeyDirectoryTag wKeyRevision is not 1: " + wKeyRevision);
					}
					int wMinorRevision = (int) vlrContentBuffer.getChar();
					if(wMinorRevision != 0) {
						throw new RuntimeException("error in GeoKeyDirectoryTag wMinorRevision is not 0: " + wMinorRevision);
					}
					int wNumberOfKeys = (int) vlrContentBuffer.getChar();
					//log.info("wNumberOfKeys " + wNumberOfKeys);
					geoKeyTags = new GeoKeyTag[wNumberOfKeys];
					for (int keyIndex = 0; keyIndex < wNumberOfKeys; keyIndex++) {
						int wKeyID = (int) vlrContentBuffer.getChar();
						int wTIFFTagLocation = (int) vlrContentBuffer.getChar();
						int wCount = (int) vlrContentBuffer.getChar();
						int wValue_Offset = (int) vlrContentBuffer.getChar();
						geoKeyTags[keyIndex] = new GeoKeyTag(wKeyID, wTIFFTagLocation, wCount, wValue_Offset);
					}
					break;
				}
				case 34736: {
					//log.info("GeoDoubleParamsTag Record");
					doubles = new double[recordLengthAfterHeader / 8];
					vlrContentBuffer.asDoubleBuffer().get(doubles);
					//log.info(Arrays.toString(doubles));
					break;
				}
				case 34737: {
					//log.info("GeoAsciiParamsTag Record");
					asciiBytes = new byte[recordLengthAfterHeader];
					vlrContentBuffer.get(asciiBytes);
					//log.info(new String(asciiBytes, StandardCharsets.US_ASCII));
					break;
				}
				default:
					//log.info("unknown recordID "+recordID);
				}
			}

			for (GeoKeyTag geoKeyTag : geoKeyTags) {
				try {
					switch(geoKeyTag.wKeyID) {
					case GTModelTypeGeoKey:
						//log.info("GTModelTypeGeoKey " + geoKeyTag.getInt());
						break;
					case GTRasterTypeGeoKey:
						//log.info("GTRasterTypeGeoKey " + geoKeyTag.getInt());
						break;
					case GTCitationGeoKey:
						//log.info("GTCitationGeoKey " + geoKeyTag.getString(asciiBytes));
						break;
					case GeographicTypeGeoKey:
						//log.info("GeographicTypeGeoKey " + geoKeyTag.getInt());
						break;
					case GeogCitationGeoKey:
						//log.info("GeogCitationGeoKey " + geoKeyTag.getString(asciiBytes));
						break;
					case GeogGeodeticDatumGeoKey:
						//log.info("GeogGeodeticDatumGeoKey " + geoKeyTag.getInt());
						break;
					case GeogPrimeMeridianGeoKey:
						//log.info("GeogPrimeMeridianGeoKey " + geoKeyTag.getInt());
						break;
					case GeogLinearUnitsGeoKey:
						//log.info("GeogLinearUnitsGeoKey " + geoKeyTag.getInt());
						break;
					case GeogAngularUnitsGeoKey:
						//log.info("GeogAngularUnitsGeoKey " + geoKeyTag.getInt());
						break;
					case GeogEllipsoidGeoKey:
						//log.info("GeogEllipsoidGeoKey " + geoKeyTag.getInt());
						break;
					case GeogSemiMajorAxisGeoKey:
						//log.info("GeogSemiMajorAxisGeoKey " + geoKeyTag.getDouble(doubles));
						break;
					case GeogInvFlatteningGeoKey:
						//log.info("GeogInvFlatteningGeoKey " + geoKeyTag.getDouble(doubles));
						break;
					case GeogAzimuthUnitsGeoKey:
						//log.info("GeogAzimuthUnitsGeoKey " + geoKeyTag.getInt());
						break;
					case GeogPrimeMeridianLongGeoKey:
						//log.info("GeogPrimeMeridianLongGeoKey " + geoKeyTag.getDouble(doubles));
						break;
					case ProjectedCSTypeGeoKey: {
						int epsgRaw = geoKeyTag.getInt();
						epsg = getEPSGfromProjectedCSTypeGeoKey(epsgRaw);
						//log.info("ProjectedCSTypeGeoKey " + epsg);
						break;
					}
					case PCSCitationGeoKey:
						//log.info("PCSCitationGeoKey " + geoKeyTag.getString(asciiBytes));
						break;
					case ProjectionGeoKey:
						//log.info("ProjectionGeoKey " + geoKeyTag.getInt());
						break;
					case ProjCoordTransGeoKey:
						//log.info("GProjCoordTransGeoKey " + geoKeyTag.getInt());
						break;
					case ProjLinearUnitsGeoKey:
						//log.info("ProjLinearUnitsGeoKey " + geoKeyTag.getInt());
						break;
					case ProjNatOriginLongGeoKey:
						//log.info("ProjNatOriginLongGeoKey " + geoKeyTag.getDouble(doubles));
						break;
					case ProjNatOriginLatGeoKey:
						//log.info("ProjNatOriginLatGeoKey " + geoKeyTag.getDouble(doubles));
						break;
					case ProjFalseEastingGeoKey:
						//log.info("ProjFalseEastingGeoKey " + geoKeyTag.getDouble(doubles));
						break;
					case ProjFalseNorthingGeoKey:
						//log.info("ProjFalseNorthingGeoKey " + geoKeyTag.getDouble(doubles));
						break;
					case ProjScaleAtNatOriginGeoKey:
						//log.info("ProjScaleAtNatOriginGeoKey " + geoKeyTag.getDouble(doubles));
						break;
					case VerticalUnitsGeoKey:
						//log.info("VerticalUnitsGeoKey " + geoKeyTag.getInt());
						break;
					default:
						//log.info("wKeyID " + geoKeyTag.wKeyID);
					}
				} catch(Exception e) {
					log.warn(e);
				}
			}
		} catch(Exception e) {
			log.warn(e);
		}
		return epsg;
	}

	/**
	 * 
	 * @param record_start
	 * @param record_count
	 * @param intFactors nullable
	 * @return
	 * @throws IOException
	 */
	public Point[] read(long record_start, int record_count, int[] intDiffs, int[] intFactors) throws IOException {
		log.info("las offset "+ Arrays.toString(offset));
		log.info("las offset "+ Arrays.toString(scale_factor));
		log.info("las intDiffs "+ Arrays.toString(intDiffs));
		log.info("las intFactors "+ Arrays.toString(intFactors));
		switch(point_data_record_format) {
		case LAS_RECORD_TYPE_0:
			return read_record_format_0(record_start, record_count, intDiffs, intFactors);
		case LAS_RECORD_TYPE_1: // format 0 + GPS Time
			return read_record_format_0(record_start, record_count, intDiffs, intFactors);
		case LAS_RECORD_TYPE_2: // format 0 + Red Green Blue
			return read_record_format_0(record_start, record_count, intDiffs, intFactors);
		case LAS_RECORD_TYPE_3: // format 0 + GPS Time + Red Green Blue
			return read_record_format_0(record_start, record_count, intDiffs, intFactors);
		case LAS_RECORD_TYPE_4: // format 0 + GPS Time + Wave Packets
			return read_record_format_0(record_start, record_count, intDiffs, intFactors);
		case LAS_RECORD_TYPE_5: // format 0 + GPS Time + Red Green Blue + Wave Packets 
			return read_record_format_0(record_start, record_count, intDiffs, intFactors);
		case LAS_RECORD_TYPE_6:
			return read_record_format_6(record_start, record_count, intDiffs, intFactors);
		case LAS_RECORD_TYPE_7: // format 6 + Red Green Blue
			return read_record_format_6(record_start, record_count, intDiffs, intFactors);
		case LAS_RECORD_TYPE_8: // format 6 + Red Green Blue + NIR
			return read_record_format_6(record_start, record_count, intDiffs, intFactors);			
		case LAS_RECORD_TYPE_9: // format 6 + Wave Packets
			return read_record_format_6(record_start, record_count, intDiffs, intFactors);
		case LAS_RECORD_TYPE_10: // format 6 + Red Green Blue + Wave Packets
			return read_record_format_6(record_start, record_count, intDiffs, intFactors);			
		default:
			throw new RuntimeException("unknown point_data_record_format "+point_data_record_format);
		}
	}

	ByteBuffer helperBuffer = null;

	/**
	 * @param record_start
	 * @param record_count
	 * @param intFactors nullable
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private Point[] read_record_format_0(long record_start, int record_count, int[] intDiffs, int[] intFactors) throws IOException {
		if(record_start<0||record_start>number_of_point_records||record_count<1||(record_start+record_count)>number_of_point_records||record_start>Integer.MAX_VALUE/2||record_count>Integer.MAX_VALUE/2) {
			throw new RuntimeException();
		}
		long filePosStart = offset_to_point_data+((long)record_start)*point_Data_Record_Length;
		long fileBytes = record_count*point_Data_Record_Length;
		//log.info("read file bytes "+fileBytes);
		//MappedByteBuffer mappedByteBuffer = filechannel.map(MapMode.READ_ONLY, filePosStart, fileBytes); // buggy?

		if(fileBytes>Integer.MAX_VALUE) {
			throw new RuntimeException("integer overflow");
		}
		if(helperBuffer==null || helperBuffer.capacity()!=fileBytes) {
			helperBuffer = java.nio.ByteBuffer.allocateDirect((int) fileBytes); //faster
			//helperBuffer = java.nio.ByteBuffer.allocate((int) fileBytes); //slower
		}
		helperBuffer.rewind();
		int ret = filechannel.read(helperBuffer, filePosStart);
		helperBuffer.rewind();
		if(ret!=fileBytes) {
			throw new RuntimeException("file read error");
		}

		java.nio.ByteBuffer byteBuffer = helperBuffer;

		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		Point[] points = new Point[record_count];

		if(intDiffs==null && intFactors==null) {		
			for(int i=0;i<record_count;i++) {
				byteBuffer.position(i*point_Data_Record_Length);

				int x = byteBuffer.getInt();
				int y = byteBuffer.getInt();
				int z = byteBuffer.getInt();
				char intensity = byteBuffer.getChar();
				byte flags = byteBuffer.get();
				byte classificationWithFlags = byteBuffer.get();
				byte scanAngleRank = byteBuffer.get();

				byte return_Number = (byte) (flags & 0b00000111);
				byte number_of_Returns = (byte) ((flags>>>3) & 0b00000111);			
				byte scanDirectionFlag  = (byte) ((flags>>>6) & 0b00000001);
				byte edgeOfFlightLine = (byte) ((flags>>>7) & 0b00000001);

				byte classification = (byte) (classificationWithFlags & 0b00011111);
				byte classificationFlags = (byte) (classificationWithFlags>>>5);

				points[i] = new Point(x,y,z,intensity,return_Number,number_of_Returns, scanAngleRank, classification, classificationFlags);
			}
		} else {

			int xd = intDiffs==null?0:intDiffs[0];
			int yd = intDiffs==null?0:intDiffs[1];
			int zd = intDiffs==null?0:intDiffs[2];
			int xf = intFactors==null?1:intFactors[0];
			int yf = intFactors==null?1:intFactors[1];
			int zf = intFactors==null?1:intFactors[2];

			for(int i=0;i<record_count;i++) {
				byteBuffer.position(i*point_Data_Record_Length);

				int x = (byteBuffer.getInt()-xd)*xf;
				int y = (byteBuffer.getInt()-yd)*yf;
				int z = (byteBuffer.getInt()-zd)*zf;
				char intensity = byteBuffer.getChar();
				byte flags = byteBuffer.get();
				byte classificationWithFlags = byteBuffer.get();
				byte scanAngleRank = byteBuffer.get();

				/*
				byte userData = byteBuffer.get();
				log.info((userData&0xff)*0.1d);
				char pointSourceID = byteBuffer.getChar();
				 */

				byte return_Number = (byte) (flags & 0b00000111);
				byte number_of_Returns = (byte) ((flags>>>3) & 0b00000111);			
				byte scanDirectionFlag  = (byte) ((flags>>>6) & 0b00000001);
				byte edgeOfFlightLine = (byte) ((flags>>>7) & 0b00000001);

				byte classification = (byte) (classificationWithFlags & 0b00011111);
				byte classificationFlags = (byte) (classificationWithFlags>>>5);

				points[i] = new Point(x,y,z,intensity,return_Number,number_of_Returns, scanAngleRank, classification, classificationFlags);
			}
		}
		return points;
	}

	/**
	 * 
	 * @param record_start
	 * @param record_count
	 * @param intFactors nullable
	 * @return
	 * @throws IOException
	 */
	//TODO change file reading (to type of format 1)
	@SuppressWarnings("unused")
	private Point[] read_record_format_6(long record_start, int record_count, int[] intDiffs, int[] intFactors) throws IOException {
		if(record_start<0||record_start>number_of_point_records||record_count<1||(record_start+record_count)>number_of_point_records||record_start>Integer.MAX_VALUE/2||record_count>Integer.MAX_VALUE/2) {
			throw new RuntimeException();
		}
		long filePosStart = offset_to_point_data+((long)record_start)*point_Data_Record_Length;
		long fileBytes = record_count*point_Data_Record_Length;

		//MappedByteBuffer mappedByteBuffer = filechannel.map(MapMode.READ_ONLY, filePosStart, fileBytes); // buggy?

		if(fileBytes>Integer.MAX_VALUE) {
			throw new RuntimeException("integer overflow");
		}
		if(helperBuffer==null || helperBuffer.capacity()!=fileBytes) {
			helperBuffer = ByteBuffer.allocateDirect((int) fileBytes); //faster
			//helperBuffer = ByteBuffer.allocate((int) fileBytes); //slower
		}
		helperBuffer.rewind();
		int ret = filechannel.read(helperBuffer, filePosStart);
		helperBuffer.rewind();
		if(ret!=fileBytes) {
			throw new RuntimeException("file read error");
		}

		java.nio.ByteBuffer byteBuffer = helperBuffer;

		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		Point[] points = new Point[record_count];

		if(intDiffs==null && intFactors==null) {	
			for(int i=0;i<record_count;i++) {
				byteBuffer.position(i*point_Data_Record_Length);

				int x = byteBuffer.getInt();
				int y = byteBuffer.getInt();
				int z = byteBuffer.getInt();
				char intensity = byteBuffer.getChar();
				byte flag1 = byteBuffer.get();
				byte flag2 = byteBuffer.get();
				byte classification = byteBuffer.get();
				byte userData = byteBuffer.get();
				short scanAngle = byteBuffer.getShort();



				byte return_Number = (byte) (flag1 & 0b00001111);
				byte number_of_Returns = (byte) ((flag1>>>4) & 0b00001111);
				int sa = (((int)scanAngle)*6)/1000;
				byte scanAngleRank;
				if(sa<Byte.MIN_VALUE) {
					scanAngleRank = Byte.MIN_VALUE;
				} else if(sa>Byte.MAX_VALUE){
					scanAngleRank = Byte.MAX_VALUE;
				} else {
					scanAngleRank = (byte) sa;
				}

				byte classificationFlags = (byte) (flag2     & 0b00001111);
				byte scannerChannel = (byte) ((flag2>>>4)    & 0b00000011);
				byte scanDirectionFlag  = (byte) ((flag2>>>6)& 0b00000001);
				byte edgeOfFlightLine  = (byte) ((flag2>>>7) & 0b00000001);

				/*
				//********** additional data
				if(return_Number==3) {
					short pointSourceID = mappedByteBuffer.getShort();
					//log.info("pointSourceID "+pointSourceID);
					double GPSTime = mappedByteBuffer.getDouble();
					//log.info("GPSTime "+GPSTime);

					//********** additional data in format 9
					byte wavePacketDescriptorIndex = mappedByteBuffer.get();
					//log.info("wavePacketDescriptorIndex "+wavePacketDescriptorIndex);
					long byteOffsetToWaveformData = mappedByteBuffer.getLong();
					log.info("byteOffsetToWaveformData "+byteOffsetToWaveformData);
					int waveformPacketSizeInBytes = mappedByteBuffer.getInt();
					log.info("waveformPacketSizeInBytes "+waveformPacketSizeInBytes);
					float returnPointWaveformLocation = mappedByteBuffer.getFloat();
					//log.info("returnPointWaveformLocation "+returnPointWaveformLocation);
					float x_t = mappedByteBuffer.getFloat();
					float y_t = mappedByteBuffer.getFloat();
					float z_t = mappedByteBuffer.getFloat();
					//log.info("x_t "+x_t+"y_t "+y_t+"z_t "+z_t);
				}
				//**********
				 */

				points[i] = new Point(x, y, z, intensity, return_Number, number_of_Returns, scanAngleRank, classification, classificationFlags);
			}
		} else {

			int xd = intDiffs==null?0:intDiffs[0];
			int yd = intDiffs==null?0:intDiffs[1];
			int zd = intDiffs==null?0:intDiffs[2];
			int xf = intFactors==null?1:intFactors[0];
			int yf = intFactors==null?1:intFactors[1];
			int zf = intFactors==null?1:intFactors[2];

			for(int i=0;i<record_count;i++) {
				byteBuffer.position(i*point_Data_Record_Length);

				int x = (byteBuffer.getInt()-xd)*xf;
				int y = (byteBuffer.getInt()-yd)*yf;
				int z = (byteBuffer.getInt()-zd)*zf;
				char intensity = byteBuffer.getChar();
				byte flag1 = byteBuffer.get();
				byte flag2 = byteBuffer.get();
				byte classification = byteBuffer.get();
				byte userData = byteBuffer.get();
				short scanAngle = byteBuffer.getShort();



				byte return_Number = (byte) (flag1 & 0b00001111);
				byte number_of_Returns = (byte) ((flag1>>>4) & 0b00001111);
				int sa = (((int)scanAngle)*6)/1000;
				byte scanAngleRank;
				if(sa<Byte.MIN_VALUE) {
					scanAngleRank = Byte.MIN_VALUE;
				} else if(sa>Byte.MAX_VALUE){
					scanAngleRank = Byte.MAX_VALUE;
				} else {
					scanAngleRank = (byte) sa;
				}

				byte classificationFlags = (byte) (flag2     & 0b00001111);
				byte scannerChannel = (byte) ((flag2>>>4)    & 0b00000011);
				byte scanDirectionFlag  = (byte) ((flag2>>>6)& 0b00000001);
				byte edgeOfFlightLine  = (byte) ((flag2>>>7) & 0b00000001);

				/*
				//********** additional data
				if(return_Number==3) {
					short pointSourceID = mappedByteBuffer.getShort();
					//log.info("pointSourceID "+pointSourceID);
					double GPSTime = mappedByteBuffer.getDouble();
					//log.info("GPSTime "+GPSTime);

					//********** additional data in format 9
					byte wavePacketDescriptorIndex = mappedByteBuffer.get();
					//log.info("wavePacketDescriptorIndex "+wavePacketDescriptorIndex);
					long byteOffsetToWaveformData = mappedByteBuffer.getLong();
					log.info("byteOffsetToWaveformData "+byteOffsetToWaveformData);
					int waveformPacketSizeInBytes = mappedByteBuffer.getInt();
					log.info("waveformPacketSizeInBytes "+waveformPacketSizeInBytes);
					float returnPointWaveformLocation = mappedByteBuffer.getFloat();
					//log.info("returnPointWaveformLocation "+returnPointWaveformLocation);
					float x_t = mappedByteBuffer.getFloat();
					float y_t = mappedByteBuffer.getFloat();
					float z_t = mappedByteBuffer.getFloat();
					//log.info("x_t "+x_t+"y_t "+y_t+"z_t "+z_t);
				}
				//**********
				 */

				points[i] = new Point(x, y, z, intensity, return_Number, number_of_Returns, scanAngleRank, classification, classificationFlags);
			}
		}
		return points;
	}

	@Override
	public String toString() {
		return "LAS "+version_Major+"."+version_Minor+" "+filename.getFileName()+" " + filesize + " bytes, file_offset=" + offset_to_point_data
				+ ", record_length=" + point_Data_Record_Length + ", records="
				+ number_of_point_records + ", record_format=" + point_data_record_format + ", offset="
				+ Arrays.toString(offset) + ", scale_factor=" + Arrays.toString(scale_factor) + ", min="
				+ Arrays.toString(min) + ", max=" + Arrays.toString(max) + ", global encoding=" + Integer.toBinaryString(global_Encoding) + "]";
	}

	public static class Getter {
		public int[][] col_int;

		public Getter() {
			col_int = new int[3][];
		}
	}

	public CellTable getRecords(long record_start, int record_count) throws IOException {
		switch (point_data_record_format) {
		case 0:
			return getRecords_format_0(record_start, record_count);
		case 1:
		case 4:
			return getRecords_format_1(record_start, record_count);
		case 2:
			return getRecords_format_2(record_start, record_count);
		case 3:
		case 5:
			return getRecords_format_3(record_start, record_count);
		case 6:
		case 9:
			return getRecords_format_6(record_start, record_count);
		case 7:
		case 8:
		case 10:
			return getRecords_format_7(record_start, record_count);
		default:
			throw new RuntimeException();
		}
	}

	private CellTable getRecords_format_0(long record_start, int record_count) throws IOException {
		if(record_start<0||record_start>number_of_point_records||record_count<1||(record_start+record_count)>number_of_point_records||record_start>Integer.MAX_VALUE/2||record_count>Integer.MAX_VALUE/2) {
			throw new RuntimeException();
		}
		long filePosStart = offset_to_point_data+((long)record_start)*point_Data_Record_Length;
		long fileBytes = record_count*point_Data_Record_Length;

		if(fileBytes>Integer.MAX_VALUE) {
			throw new RuntimeException("integer overflow");
		}
		if(helperBuffer==null || helperBuffer.capacity()!=fileBytes) {
			helperBuffer = java.nio.ByteBuffer.allocateDirect((int) fileBytes);
		}
		helperBuffer.rewind();
		//Timer.resume("read records");
		int ret = filechannel.read(helperBuffer, filePosStart);
		//log.info(Timer.stop("read records"));
		helperBuffer.rewind();
		if(ret!=fileBytes) {
			throw new RuntimeException("file read error");
		}

		java.nio.ByteBuffer byteBuffer = helperBuffer;

		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		int[] xs = new int[record_count];
		int[] ys = new int[record_count];
		int[] zs = new int[record_count];
		char[] intensity = new char[record_count];
		byte[] returnNumber = new byte[record_count];
		byte[] returns = new byte[record_count];
		BitSet scanDirectionFlag = new BitSet(record_count);
		BitSet edgeOfFlightLine = new BitSet(record_count);
		byte[] classification = new byte[record_count];
		byte[] scanAngleRank = new byte[record_count];

		for(int i = 0; i < record_count; i++) {
			byteBuffer.position(i*point_Data_Record_Length);
			xs[i] = byteBuffer.getInt();
			ys[i] = byteBuffer.getInt();
			zs[i] = byteBuffer.getInt();
			intensity[i] = byteBuffer.getChar();
			byte flags = byteBuffer.get();
			returnNumber[i] = (byte) (flags & 0b00000111);
			returns[i] = (byte) ((flags>>>3) & 0b00000111);
			if((flags & 0b01_00_00_00) != 0) {
				scanDirectionFlag.set(i);
			}
			if((flags & 0b10_00_00_00) != 0) {
				edgeOfFlightLine.set(i);
			}
			byte classificationWithFlags = byteBuffer.get();
			classification[i] = (byte) (classificationWithFlags & 0b00011111);
			//classificationFlags[i] = (byte) (classificationWithFlags>>>5);
			scanAngleRank[i] = byteBuffer.get();
			byte userData = byteBuffer.get();
			char pointSourceID = byteBuffer.getChar();
		}
		CellTable recordTable = new CellTable(0, 0, 0, record_count, xs, ys, zs);
		recordTable.intensity = intensity;
		recordTable.returnNumber = returnNumber;
		recordTable.returns = returns;
		recordTable.scanDirectionFlag = scanDirectionFlag;
		recordTable.edgeOfFlightLine = edgeOfFlightLine;
		recordTable.classification = classification;
		recordTable.scanAngleRank = scanAngleRank;

		recordTable.cleanup();

		return recordTable;
	}

	private CellTable getRecords_format_1(long record_start, int record_count) throws IOException {
		if(record_start<0||record_start>number_of_point_records||record_count<1||(record_start+record_count)>number_of_point_records||record_start>Integer.MAX_VALUE/2||record_count>Integer.MAX_VALUE/2) {
			throw new RuntimeException();
		}
		long filePosStart = offset_to_point_data+((long)record_start)*point_Data_Record_Length;
		long fileBytes = record_count*point_Data_Record_Length;

		if(fileBytes>Integer.MAX_VALUE) {
			throw new RuntimeException("integer overflow");
		}
		if(helperBuffer==null || helperBuffer.capacity()!=fileBytes) {
			helperBuffer = java.nio.ByteBuffer.allocateDirect((int) fileBytes);
		}
		helperBuffer.rewind();
		Timer.resume("read records");
		int ret = filechannel.read(helperBuffer, filePosStart);
		log.info(Timer.stop("read records"));
		helperBuffer.rewind();
		if(ret!=fileBytes) {
			throw new RuntimeException("file read error");
		}

		java.nio.ByteBuffer byteBuffer = helperBuffer;

		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		int[] xs = new int[record_count];
		int[] ys = new int[record_count];
		int[] zs = new int[record_count];
		char[] intensity = new char[record_count];
		byte[] returnNumber = new byte[record_count];
		byte[] returns = new byte[record_count];
		BitSet scanDirectionFlag = new BitSet(record_count);
		BitSet edgeOfFlightLine = new BitSet(record_count);
		byte[] classification = new byte[record_count];
		byte[] scanAngleRank = new byte[record_count];
		long[] gpsTime = new long[record_count];

		for(int i = 0; i < record_count; i++) {
			byteBuffer.position(i*point_Data_Record_Length);
			xs[i] = byteBuffer.getInt();
			ys[i] = byteBuffer.getInt();
			zs[i] = byteBuffer.getInt();
			intensity[i] = byteBuffer.getChar();
			byte flags = byteBuffer.get();
			returnNumber[i] = (byte) (flags & 0b00000111);
			returns[i] = (byte) ((flags>>>3) & 0b00000111);
			if((flags & 0b01_00_00_00) != 0) {
				scanDirectionFlag.set(i);
			}
			if((flags & 0b10_00_00_00) != 0) {
				edgeOfFlightLine.set(i);
			}
			byte classificationWithFlags = byteBuffer.get();
			classification[i] = (byte) (classificationWithFlags & 0b00011111);
			//classificationFlags[i] = (byte) (classificationWithFlags>>>5);
			scanAngleRank[i] = byteBuffer.get();
			byte userData = byteBuffer.get();
			char pointSourceID = byteBuffer.getChar();
			double gpsTimeRaw = byteBuffer.getDouble();
			gpsTime[i] = (long) (gpsTimeRaw * 1_000_000_000d);
		}
		CellTable recordTable = new CellTable(0, 0, 0, record_count, xs, ys, zs);
		recordTable.intensity = intensity;
		recordTable.returnNumber = returnNumber;
		recordTable.returns = returns;
		recordTable.scanDirectionFlag = scanDirectionFlag;
		recordTable.edgeOfFlightLine = edgeOfFlightLine;
		recordTable.classification = classification;
		recordTable.scanAngleRank = scanAngleRank;
		recordTable.gpsTime = gpsTime;

		recordTable.cleanup();

		return recordTable;
	}

	private CellTable getRecords_format_2(long record_start, int record_count) throws IOException {
		if(record_start<0||record_start>number_of_point_records||record_count<1||(record_start+record_count)>number_of_point_records||record_start>Integer.MAX_VALUE/2||record_count>Integer.MAX_VALUE/2) {
			throw new RuntimeException();
		}
		long filePosStart = offset_to_point_data+((long)record_start)*point_Data_Record_Length;
		long fileBytes = record_count*point_Data_Record_Length;

		if(fileBytes>Integer.MAX_VALUE) {
			throw new RuntimeException("integer overflow");
		}
		if(helperBuffer==null || helperBuffer.capacity()!=fileBytes) {
			helperBuffer = java.nio.ByteBuffer.allocateDirect((int) fileBytes);
		}
		helperBuffer.rewind();
		Timer.resume("read records");
		int ret = filechannel.read(helperBuffer, filePosStart);
		log.info(Timer.stop("read records"));
		helperBuffer.rewind();
		if(ret!=fileBytes) {
			throw new RuntimeException("file read error");
		}

		java.nio.ByteBuffer byteBuffer = helperBuffer;

		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		int[] xs = new int[record_count];
		int[] ys = new int[record_count];
		int[] zs = new int[record_count];
		char[] intensity = new char[record_count];
		byte[] returnNumber = new byte[record_count];
		byte[] returns = new byte[record_count];
		BitSet scanDirectionFlag = new BitSet(record_count);
		BitSet edgeOfFlightLine = new BitSet(record_count);
		byte[] classification = new byte[record_count];
		byte[] scanAngleRank = new byte[record_count];
		char[] red = new char[record_count];
		char[] green = new char[record_count];
		char[] blue = new char[record_count];

		for(int i = 0; i < record_count; i++) {
			byteBuffer.position(i*point_Data_Record_Length);
			xs[i] = byteBuffer.getInt();
			ys[i] = byteBuffer.getInt();
			zs[i] = byteBuffer.getInt();
			intensity[i] = byteBuffer.getChar();
			byte flags = byteBuffer.get();
			returnNumber[i] = (byte) (flags & 0b00000111);
			returns[i] = (byte) ((flags>>>3) & 0b00000111);
			if((flags & 0b01_00_00_00) != 0) {
				scanDirectionFlag.set(i);
			}
			if((flags & 0b10_00_00_00) != 0) {
				edgeOfFlightLine.set(i);
			}
			byte classificationWithFlags = byteBuffer.get();
			classification[i] = (byte) (classificationWithFlags & 0b00011111);
			//classificationFlags[i] = (byte) (classificationWithFlags>>>5);
			scanAngleRank[i] = byteBuffer.get();
			byte userData = byteBuffer.get();
			char pointSourceID = byteBuffer.getChar();
			red[i] = byteBuffer.getChar();
			green[i] = byteBuffer.getChar();
			blue[i] = byteBuffer.getChar();

		}
		CellTable recordTable = new CellTable(0, 0, 0, record_count, xs, ys, zs);
		recordTable.intensity = intensity;
		recordTable.returnNumber = returnNumber;
		recordTable.returns = returns;
		recordTable.scanDirectionFlag = scanDirectionFlag;
		recordTable.edgeOfFlightLine = edgeOfFlightLine;
		recordTable.classification = classification;
		recordTable.scanAngleRank = scanAngleRank;
		recordTable.red = red;
		recordTable.green = green;
		recordTable.blue = blue;

		recordTable.cleanup();

		return recordTable;
	}

	private CellTable getRecords_format_3(long record_start, int record_count) throws IOException {
		if(record_start<0||record_start>number_of_point_records||record_count<1||(record_start+record_count)>number_of_point_records||record_start>Integer.MAX_VALUE/2||record_count>Integer.MAX_VALUE/2) {
			throw new RuntimeException();
		}
		long filePosStart = offset_to_point_data+((long)record_start)*point_Data_Record_Length;
		long fileBytes = record_count*point_Data_Record_Length;

		if(fileBytes>Integer.MAX_VALUE) {
			throw new RuntimeException("integer overflow");
		}
		if(helperBuffer==null || helperBuffer.capacity()!=fileBytes) {
			helperBuffer = java.nio.ByteBuffer.allocateDirect((int) fileBytes);
		}
		helperBuffer.rewind();
		Timer.resume("read records");
		int ret = filechannel.read(helperBuffer, filePosStart);
		log.info(Timer.stop("read records"));
		helperBuffer.rewind();
		if(ret!=fileBytes) {
			throw new RuntimeException("file read error");
		}

		java.nio.ByteBuffer byteBuffer = helperBuffer;

		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		int[] xs = new int[record_count];
		int[] ys = new int[record_count];
		int[] zs = new int[record_count];
		char[] intensity = new char[record_count];
		byte[] returnNumber = new byte[record_count];
		byte[] returns = new byte[record_count];
		BitSet scanDirectionFlag = new BitSet(record_count);
		BitSet edgeOfFlightLine = new BitSet(record_count);
		byte[] classification = new byte[record_count];
		byte[] scanAngleRank = new byte[record_count];
		long[] gpsTime = new long[record_count];
		char[] red = new char[record_count];
		char[] green = new char[record_count];
		char[] blue = new char[record_count];

		for(int i = 0; i < record_count; i++) {
			byteBuffer.position(i*point_Data_Record_Length);
			xs[i] = byteBuffer.getInt();
			ys[i] = byteBuffer.getInt();
			zs[i] = byteBuffer.getInt();
			intensity[i] = byteBuffer.getChar();
			byte flags = byteBuffer.get();
			returnNumber[i] = (byte) (flags & 0b00000111);
			returns[i] = (byte) ((flags>>>3) & 0b00000111);
			if((flags & 0b01_00_00_00) != 0) {
				scanDirectionFlag.set(i);
			}
			if((flags & 0b10_00_00_00) != 0) {
				edgeOfFlightLine.set(i);
			}
			byte classificationWithFlags = byteBuffer.get();
			classification[i] = (byte) (classificationWithFlags & 0b00011111);
			//classificationFlags[i] = (byte) (classificationWithFlags>>>5);
			scanAngleRank[i] = byteBuffer.get();
			byte userData = byteBuffer.get();
			char pointSourceID = byteBuffer.getChar();
			double gpsTimeRaw = byteBuffer.getDouble();
			gpsTime[i] = (long) (gpsTimeRaw * 1_000_000_000d);
			red[i] = byteBuffer.getChar();
			green[i] = byteBuffer.getChar();
			blue[i] = byteBuffer.getChar();
		}
		CellTable recordTable = new CellTable(0, 0, 0, record_count, xs, ys, zs);
		recordTable.intensity = intensity;
		recordTable.returnNumber = returnNumber;
		recordTable.returns = returns;
		recordTable.scanDirectionFlag = scanDirectionFlag;
		recordTable.edgeOfFlightLine = edgeOfFlightLine;
		recordTable.classification = classification;
		recordTable.scanAngleRank = scanAngleRank;
		recordTable.gpsTime = gpsTime;
		recordTable.red = red;
		recordTable.green = green;
		recordTable.blue = blue;

		recordTable.cleanup();

		return recordTable;
	}

	private CellTable getRecords_format_6(long record_start, int record_count) throws IOException {
		if(record_start<0||record_start>number_of_point_records||record_count<1||(record_start+record_count)>number_of_point_records||record_start>Integer.MAX_VALUE/2||record_count>Integer.MAX_VALUE/2) {
			throw new RuntimeException();
		}
		long filePosStart = offset_to_point_data+((long)record_start)*point_Data_Record_Length;
		long fileBytes = record_count*point_Data_Record_Length;

		if(fileBytes>Integer.MAX_VALUE) {
			throw new RuntimeException("integer overflow");
		}
		if(helperBuffer==null || helperBuffer.capacity()!=fileBytes) {
			helperBuffer = java.nio.ByteBuffer.allocateDirect((int) fileBytes);
		}
		helperBuffer.rewind();
		Timer.resume("read records");
		int ret = filechannel.read(helperBuffer, filePosStart);
		log.info(Timer.stop("read records"));
		helperBuffer.rewind();
		if(ret!=fileBytes) {
			throw new RuntimeException("file read error");
		}

		java.nio.ByteBuffer byteBuffer = helperBuffer;

		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		int[] xs = new int[record_count];
		int[] ys = new int[record_count];
		int[] zs = new int[record_count];
		char[] intensity = new char[record_count];
		byte[] returnNumber = new byte[record_count];
		byte[] returns = new byte[record_count];
		BitSet scanDirectionFlag = new BitSet(record_count);
		BitSet edgeOfFlightLine = new BitSet(record_count);
		byte[] classification = new byte[record_count];
		byte[] scanAngleRank = new byte[record_count];
		long[] gpsTime = new long[record_count];

		for(int i=0;i<record_count;i++) {
			byteBuffer.position(i*point_Data_Record_Length);
			xs[i] = byteBuffer.getInt();
			ys[i] = byteBuffer.getInt();
			zs[i] = byteBuffer.getInt();
			intensity[i] = byteBuffer.getChar();
			byte flag1 = byteBuffer.get();
			returnNumber[i] = (byte) (flag1 & 0b00001111);
			returns[i] =  (byte) ((flag1>>>4) & 0b00001111);
			byte flag2 = byteBuffer.get();
			if((flag2 & 0b01_00_00_00) != 0) {
				scanDirectionFlag.set(i);
			}
			if((flag2 & 0b10_00_00_00) != 0) {
				edgeOfFlightLine.set(i);
			}
			classification[i] = byteBuffer.get();
			byte userData = byteBuffer.get();
			short scanAngle = byteBuffer.getShort();
			int sa = (((int)scanAngle)*6)/1000;
			if(sa<Byte.MIN_VALUE) {
				scanAngleRank[i] = Byte.MIN_VALUE;
			} else if(sa>Byte.MAX_VALUE){
				scanAngleRank[i] = Byte.MAX_VALUE;
			} else {
				scanAngleRank[i] = (byte) sa;
			}
			char pointSourceID = byteBuffer.getChar();
			double gpsTimeRaw = byteBuffer.getDouble();
			gpsTime[i] = (long) (gpsTimeRaw * 1_000_000_000d);
		}
		CellTable recordTable = new CellTable(0, 0, 0, record_count, xs, ys, zs);
		recordTable.intensity = intensity;
		recordTable.returnNumber = returnNumber;
		recordTable.returns = returns;
		recordTable.scanDirectionFlag = scanDirectionFlag;
		recordTable.edgeOfFlightLine = edgeOfFlightLine;
		recordTable.classification = classification;
		recordTable.scanAngleRank = scanAngleRank;
		recordTable.gpsTime = gpsTime;

		recordTable.cleanup();

		return recordTable;
	}

	private CellTable getRecords_format_7(long record_start, int record_count) throws IOException {
		if(record_start<0||record_start>number_of_point_records||record_count<1||(record_start+record_count)>number_of_point_records||record_start>Integer.MAX_VALUE/2||record_count>Integer.MAX_VALUE/2) {
			throw new RuntimeException();
		}
		long filePosStart = offset_to_point_data+((long)record_start)*point_Data_Record_Length;
		long fileBytes = record_count*point_Data_Record_Length;

		if(fileBytes>Integer.MAX_VALUE) {
			throw new RuntimeException("integer overflow");
		}
		if(helperBuffer==null || helperBuffer.capacity()!=fileBytes) {
			helperBuffer = java.nio.ByteBuffer.allocateDirect((int) fileBytes);
		}
		helperBuffer.rewind();
		Timer.resume("read records");
		int ret = filechannel.read(helperBuffer, filePosStart);
		log.info(Timer.stop("read records"));
		helperBuffer.rewind();
		if(ret!=fileBytes) {
			throw new RuntimeException("file read error");
		}

		java.nio.ByteBuffer byteBuffer = helperBuffer;

		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		int[] xs = new int[record_count];
		int[] ys = new int[record_count];
		int[] zs = new int[record_count];
		char[] intensity = new char[record_count];
		byte[] returnNumber = new byte[record_count];
		byte[] returns = new byte[record_count];
		BitSet scanDirectionFlag = new BitSet(record_count);
		BitSet edgeOfFlightLine = new BitSet(record_count);
		byte[] classification = new byte[record_count];
		byte[] scanAngleRank = new byte[record_count];
		long[] gpsTime = new long[record_count];
		char[] red = new char[record_count];
		char[] green = new char[record_count];
		char[] blue = new char[record_count];

		for(int i=0;i<record_count;i++) {
			byteBuffer.position(i*point_Data_Record_Length);
			xs[i] = byteBuffer.getInt();
			ys[i] = byteBuffer.getInt();
			zs[i] = byteBuffer.getInt();
			intensity[i] = byteBuffer.getChar();
			byte flag1 = byteBuffer.get();
			returnNumber[i] = (byte) (flag1 & 0b00001111);
			returns[i] =  (byte) ((flag1>>>4) & 0b00001111);
			byte flag2 = byteBuffer.get();
			if((flag2 & 0b01_00_00_00) != 0) {
				scanDirectionFlag.set(i);
			}
			if((flag2 & 0b10_00_00_00) != 0) {
				edgeOfFlightLine.set(i);
			}
			classification[i] = byteBuffer.get();
			byte userData = byteBuffer.get();
			short scanAngle = byteBuffer.getShort();
			int sa = (((int)scanAngle)*6)/1000;
			if(sa<Byte.MIN_VALUE) {
				scanAngleRank[i] = Byte.MIN_VALUE;
			} else if(sa>Byte.MAX_VALUE){
				scanAngleRank[i] = Byte.MAX_VALUE;
			} else {
				scanAngleRank[i] = (byte) sa;
			}
			char pointSourceID = byteBuffer.getChar();
			double gpsTimeRaw = byteBuffer.getDouble();
			gpsTime[i] = (long) (gpsTimeRaw * 1_000_000_000d);
			red[i] = byteBuffer.getChar();
			green[i] = byteBuffer.getChar();
			blue[i] = byteBuffer.getChar();
		}
		CellTable recordTable = new CellTable(0, 0, 0, record_count, xs, ys, zs);
		recordTable.intensity = intensity;
		recordTable.returnNumber = returnNumber;
		recordTable.returns = returns;
		recordTable.scanDirectionFlag = scanDirectionFlag;
		recordTable.edgeOfFlightLine = edgeOfFlightLine;
		recordTable.classification = classification;
		recordTable.scanAngleRank = scanAngleRank;
		recordTable.gpsTime = gpsTime;
		recordTable.red = red;
		recordTable.green = green;
		recordTable.blue = blue;

		recordTable.cleanup();

		return recordTable;
	}
}
