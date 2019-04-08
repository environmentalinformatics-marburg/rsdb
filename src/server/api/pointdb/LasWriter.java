package server.api.pointdb;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;

import org.eclipse.jetty.server.Response;

import pointdb.PointDB;
import pointdb.base.GeoPoint;
import util.LittleEndianDataOutputStream;
import util.collections.vec.Vec;

public class LasWriter {
	//private static final Logger log = LogManager.getLogger();

	private final static int LASF_SIGNATUR = 1179861324;

	private static final byte[] SYSTEM_IDENTIFIER = Arrays.copyOf("data export".getBytes(StandardCharsets.US_ASCII), 32);
	private static final byte[] GENERATING_SOFTWARE = Arrays.copyOf("Remote Sensing Database".getBytes(StandardCharsets.US_ASCII), 32);

	public static enum LAS_HEADER {
		V_1_2(1, 2, 227),
		V_1_4(1, 4, 375);

		public final int version_major;
		public final int version_minor;
		public final int size;

		private LAS_HEADER(int version_major, int version_minor, int size) {
			this.version_major = version_major;
			this.version_minor = version_minor;
			this.size = size;
		}
	}

	public static enum POINT_DATA_RECORD {
		FORMAT_0(0, 20),
		FORMAT_6(6, 30);

		public final int id;
		public final int size;

		private POINT_DATA_RECORD(int id, int size) {
			this.id = id;
			this.size = size;
		}
	}
	
	private static final double SCALE_FACTOR = 0.001;

	@SuppressWarnings("resource")
	public static void writePoints(PointDB pointdb, Response response, Vec<GeoPoint> points, String[] columns, LAS_HEADER header, POINT_DATA_RECORD pointDataRecord) throws IOException {
		LocalDate date = LocalDate.now();

		double xmin = Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;
		double zmin = Double.MAX_VALUE;
		double xmax = -Double.MAX_VALUE;
		double ymax = -Double.MAX_VALUE;
		double zmax = -Double.MAX_VALUE;
		int[] numberOfPointsByReturn = new int[15];
		for(GeoPoint p : points) {
			if(p.x < xmin) {
				xmin = p.x;
			}
			if(p.y < ymin) {
				ymin = p.y;
			}
			if(p.z < zmin) {
				zmin = p.z;
			}
			if(xmax < p.x) {
				xmax = p.x;
			}
			if(ymax < p.y) {
				ymax = p.y;
			}	
			if(zmax < p.z) {
				zmax = p.z;
			}
			if(p.returnNumber >= 0 && p.returnNumber <= 15) {
				numberOfPointsByReturn[p.returnNumber - 1]++;
			}
		}
		double xoff = ((int)xmin);
		double yoff = ((int)ymin);
		double zoff = ((int)zmin);		

		response.setContentType("application/octet-stream");		
		LittleEndianDataOutputStream out = new LittleEndianDataOutputStream(response.getHttpOutput());

		

		out.writeInt(LASF_SIGNATUR); //File Signature (“LASF”) char[4] 4 bytes
		out.writeShort(0); //File Source ID unsigned short 2 bytes
		out.writeShort(0); //Global Encoding unsigned short 2 bytes
		out.writeInt(0);//Project ID - GUID data 1 unsigned long 4 bytes
		out.writeShort(0);//Project ID - GUID data 2 unsigned short 2 byte
		out.writeShort(0);//Project ID - GUID data 3 unsigned short 2 byte
		out.writeLong(0);//Project ID - GUID data 4 unsigned char[8] 8 bytes
		out.writeByte(header.version_major);//Version Major unsigned char 1 byte
		out.writeByte(header.version_minor);//Version Minor unsigned char 1 byte
		out.write(SYSTEM_IDENTIFIER);//System Identifier char[32] 32 bytes
		out.write(GENERATING_SOFTWARE);//Generating Software char[32] 32 bytes
		out.writeShort(date.getDayOfYear());//File Creation Day of Year unsigned short 2 bytes
		out.writeShort(date.getYear());//File Creation Year unsigned short 2 bytes
		out.writeShort(header.size);//Header Size unsigned short 2 bytes
		out.writeInt(header.size);//Offset to point data unsigned long 4 bytes 
		out.writeInt(0);//Number of Variable Length Records unsigned long 4 bytes
		out.writeByte(pointDataRecord.id);//Point Data Record Format unsigned char 1 byte
		out.writeShort(pointDataRecord.size);//Point Data Record Length unsigned short 2 bytes
		out.writeInt(points.size());//Legacy Number of point records unsigned long 4 bytes
		out.writeUint32(numberOfPointsByReturn, 5);//Legacy Number of points by return unsigned long [5] 20 bytes
		out.writeDouble(SCALE_FACTOR);//X scale factor double 8 bytes
		out.writeDouble(SCALE_FACTOR);//Y scale factor double 8 bytes
		out.writeDouble(SCALE_FACTOR);//Z scale factor double 8 bytes
		out.writeDouble(xoff);//X offset double 8 bytes
		out.writeDouble(yoff);//Y offset double 8 bytes
		out.writeDouble(zoff);//Z offset double 8 bytes
		out.writeDouble(xmax);//Max X double 8 bytes
		out.writeDouble(xmin);//Min X double 8 byte
		out.writeDouble(ymax);//Max Y double 8 bytes
		out.writeDouble(ymin);//Min Y double 8 bytes
		out.writeDouble(zmax);//Max Z double 8 bytes
		out.writeDouble(zmin);//Min Z double 8 bytes
		if((header.version_major == 1 && header.version_minor >= 3) || header.version_major >= 2) {
			out.writeLong(0);//Start of Waveform Data Packet Record Unsigned long long 8 bytes
			if((header.version_major == 1 && header.version_minor >= 4) || header.version_major >= 2) {
				out.writeLong(0);//Start of first Extended Variable Length Record unsigned long long 8 bytes
				out.writeInt(0);//Number of Extended Variable Length Records unsigned long 4 bytes
				out.writeLong(points.size());//Number of point records unsigned long long 8 bytes
				out.writeUint64(numberOfPointsByReturn);//Number of points by return unsigned long long [15] 120 bytes
			}
		}

		switch(pointDataRecord) {
		case FORMAT_0:
			writePointDataRecordFormat0(out, points, xoff, yoff, zoff);
			break;
		case FORMAT_6:
			writePointDataRecordFormat6(out, points, xoff, yoff, zoff);
			break;
		default:
			throw new RuntimeException();
		}


	}

	private static void writePointDataRecordFormat0(LittleEndianDataOutputStream out, Vec<GeoPoint> points, double xoff, double yoff, double zoff) throws IOException {
		for(GeoPoint p : points) {
			out.writeInt((int) ((p.x - xoff) * 1000d)); //X long 4 bytes
			out.writeInt((int) ((p.y - yoff) * 1000d)); //Y long 4 bytes
			out.writeInt((int) ((p.z - zoff)  * 1000d)); //Z long 4 bytes
			out.writeChar(p.intensity);//Intensity unsigned short 2 bytes
			//Return Number 3 bits (bits 0 – 2) 3 bits
			//Number of Returns (given pulse) 3 bits (bits 3 – 5) 3 bits
			//Scan Direction Flag 1 bit (bit 6) 1 bit
			//Edge of Flight Line 1 bit (bit 7) 1 bit
			out.writeByte(getFormat0Flag(p.returnNumber, p.returns));
			out.writeByte(p.classification);//Classification unsigned char 1 byte
			out.writeByte(p.scanAngleRank);//Scan Angle Rank (-90 to +90) – Left side char 1 byte
			out.writeByte(0);//User Data unsigned char 1 byte
			out.writeShort(0);//Point Source ID unsigned short 2 bytes
		}
	}

	private static int getFormat0Flag(int returnNumber, int returns) {		
		int rn = 7 < returnNumber ? 7 : returnNumber;
		int rs = 7 < returns ? 7 : returns;		
		return rn & (rs << 3);		
	}

	private static void writePointDataRecordFormat6(LittleEndianDataOutputStream out, Vec<GeoPoint> points, double xoff, double yoff, double zoff) throws IOException {
		for(GeoPoint p : points) {
			out.writeInt((int) ((p.x - xoff) * 1000d)); //X long 4 bytes
			out.writeInt((int) ((p.y - yoff) * 1000d)); //Y long 4 bytes
			out.writeInt((int) ((p.z - zoff)  * 1000d)); //Z long 4 bytes
			out.writeChar(p.intensity);//Intensity unsigned short 2 bytes
			//Return Number 4 bits (bits 0 - 3) 4 bits
			//Number of Returns (given pulse) 4 bits (bits 4 - 7) 4 bits
			out.writeByte(getFormat6ReturnFlag(p.returnNumber, p.returns));
			//Classification Flags 4 bits (bits 0 - 3) 4 bits
			//Scanner Channel 2 bits (bits 4 - 5) 2 bits
			//Scan Direction Flag 1 bit (bit 6) 1 bit
			//Edge of Flight Line 1 bit (bit 7) 1 bit
			out.writeByte(0);
			out.writeByte(p.classification);//Classification unsigned char 1 byte
			out.writeByte(0);//User Data unsigned char 1 byte
			out.writeShort((int) (p.scanAngleRank / 0.006d));//Scan Angle short 2 bytes
			out.writeShort(0);//Point Source ID unsigned short 2 bytes
			out.writeDouble(0);//GPS Time double 8 bytes			
		}
	}

	private static int getFormat6ReturnFlag(int returnNumber, int returns) {		
		int rn = 15 < returnNumber ? 15 : returnNumber;
		int rs = 15 < returns ? 15 : returns;		
		return rn & (rs << 4);		
	}
}
