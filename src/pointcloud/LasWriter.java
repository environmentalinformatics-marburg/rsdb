package pointcloud;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;

import util.BufferedDataOuputStreamLE;
import util.Receiver;
import util.Web;

public class LasWriter {

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
	public static void writePoints(PointTable[] pointTables, Receiver receiver, LAS_HEADER header, POINT_DATA_RECORD pointDataRecord) throws IOException {
		LocalDate date = LocalDate.now();

		double xmin = Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;
		double zmin = Double.MAX_VALUE;
		double xmax = -Double.MAX_VALUE;
		double ymax = -Double.MAX_VALUE;
		double zmax = -Double.MAX_VALUE;
		long[] numberOfPointsByReturn = new long[15];
		long pointCount = 0;
		for(PointTable p:pointTables) {
			int len = p.rows;
			for (int i = 0; i < len; i++) {
				if(p.x[i] < xmin) {
					xmin = p.x[i];
				}
				if(p.y[i] < ymin) {
					ymin = p.y[i];
				}
				if(p.z[i] < zmin) {
					zmin = p.z[i];
				}
				if(xmax < p.x[i]) {
					xmax = p.x[i];
				}
				if(ymax < p.y[i]) {
					ymax = p.y[i];
				}	
				if(zmax < p.z[i]) {
					zmax = p.z[i];
				}
				if(p.returnNumber[i] > 0 && p.returnNumber[i] <= 15) {
					numberOfPointsByReturn[p.returnNumber[i] - 1]++;
				}
			}
			pointCount += len;
		}
		double xoff = ((int)xmin);
		double yoff = ((int)ymin);
		double zoff = ((int)zmin);		

		receiver.setContentType(Web.MIME_BINARY);	
		//LittleEndianDataOutputStream out = new LittleEndianDataOutputStream(receiver.getOutputStream());
		try(BufferedDataOuputStreamLE out = new BufferedDataOuputStreamLE(receiver.getOutputStream())) {
			out.putInt(LASF_SIGNATUR); //File Signature (“LASF”) char[4] 4 bytes
			out.putShort((short) 0); //File Source ID unsigned short 2 bytes
			out.putShort((short) 0); //Global Encoding unsigned short 2 bytes
			out.putInt(0);//Project ID - GUID data 1 unsigned long 4 bytes
			out.putShort((short) 0);//Project ID - GUID data 2 unsigned short 2 byte
			out.putShort((short) 0);//Project ID - GUID data 3 unsigned short 2 byte
			out.putLong(0);//Project ID - GUID data 4 unsigned char[8] 8 bytes
			out.putByte((byte) header.version_major);//Version Major unsigned char 1 byte
			out.putByte((byte) header.version_minor);//Version Minor unsigned char 1 byte
			out.putBytes(SYSTEM_IDENTIFIER);//System Identifier char[32] 32 bytes
			out.putBytes(GENERATING_SOFTWARE);//Generating Software char[32] 32 bytes
			out.putShort((short) date.getDayOfYear());//File Creation Day of Year unsigned short 2 bytes
			out.putShort((short) date.getYear());//File Creation Year unsigned short 2 bytes
			out.putShort((short) header.size);//Header Size unsigned short 2 bytes
			out.putInt(header.size);//Offset to point data unsigned long 4 bytes 
			out.putInt(0);//Number of Variable Length Records unsigned long 4 bytes
			out.putByte((byte) pointDataRecord.id);//Point Data Record Format unsigned char 1 byte
			out.putShort((short) pointDataRecord.size);//Point Data Record Length unsigned short 2 bytes
			out.putInt(pointCount > Integer.MAX_VALUE ? 0 : (int) pointCount);//Legacy Number of point records unsigned long 4 bytes
			out.putIntsOverflowZero(numberOfPointsByReturn, 5);//Legacy Number of points by return unsigned long [5] 20 bytes
			out.putDouble(SCALE_FACTOR);//X scale factor double 8 bytes
			out.putDouble(SCALE_FACTOR);//Y scale factor double 8 bytes
			out.putDouble(SCALE_FACTOR);//Z scale factor double 8 bytes
			out.putDouble(xoff);//X offset double 8 bytes
			out.putDouble(yoff);//Y offset double 8 bytes
			out.putDouble(zoff);//Z offset double 8 bytes
			out.putDouble(xmax);//Max X double 8 bytes
			out.putDouble(xmin);//Min X double 8 byte
			out.putDouble(ymax);//Max Y double 8 bytes
			out.putDouble(ymin);//Min Y double 8 bytes
			out.putDouble(zmax);//Max Z double 8 bytes
			out.putDouble(zmin);//Min Z double 8 bytes
			if((header.version_major == 1 && header.version_minor >= 3) || header.version_major >= 2) {
				out.putLong(0);//Start of Waveform Data Packet Record Unsigned long long 8 bytes
				if((header.version_major == 1 && header.version_minor >= 4) || header.version_major >= 2) {
					out.putLong(0);//Start of first Extended Variable Length Record unsigned long long 8 bytes
					out.putInt(0);//Number of Extended Variable Length Records unsigned long 4 bytes
					out.putLong(pointCount);//Number of point records unsigned long long 8 bytes
					out.putLongs(numberOfPointsByReturn);//Number of points by return unsigned long long [15] 120 bytes
				}
			}

			switch(pointDataRecord) {
			case FORMAT_0:
				writePointDataRecordFormat0(out, pointTables, xoff, yoff, zoff);
				break;
			case FORMAT_6:
				writePointDataRecordFormat6(out, pointTables, xoff, yoff, zoff);
				break;
			default:
				throw new RuntimeException();
			}
		}
	}

	private static void writePointDataRecordFormat0(BufferedDataOuputStreamLE out, PointTable[] pointTables, double xoff, double yoff, double zoff) throws IOException {
		for(PointTable p:pointTables) {
			int len = p.rows;
			double[] xs = p.x == null ? new double[len] : p.x;
			double[] ys = p.y == null ? new double[len] : p.y;
			double[] zs = p.z == null ? new double[len] : p.z;
			char[] is = p.intensity == null ? new char[len] : p.intensity;
			byte[] rns = p.returnNumber == null ? new byte[len] : p.returnNumber;
			byte[] rs = p.returns == null ? new byte[len] : p.returns;
			byte[] cs = p.classification == null ? new byte[len] : p.classification;
			byte[] sars = p.scanAngleRank == null ? new byte[len] : p.scanAngleRank;
			for (int i = 0; i < len; i++) {
				out.putInt((int) ((xs[i] - xoff) * 1000d)); //X long 4 bytes
				out.putInt((int) ((ys[i] - yoff) * 1000d)); //Y long 4 bytes
				out.putInt((int) ((zs[i] - zoff)  * 1000d)); //Z long 4 bytes
				out.putChar(is[i]);//Intensity unsigned short 2 bytes
				//Return Number 3 bits (bits 0 – 2) 3 bits
				//Number of Returns (given pulse) 3 bits (bits 3 – 5) 3 bits
				//Scan Direction Flag 1 bit (bit 6) 1 bit
				//Edge of Flight Line 1 bit (bit 7) 1 bit
				out.putByte((byte) getFormat0Flag(rns[i], rs[i]));
				out.putByte(cs[i]);//Classification unsigned char 1 byte
				out.putByte(sars[i]);//Scan Angle Rank (-90 to +90) – Left side char 1 byte
				out.putByte((byte) 0);//User Data unsigned char 1 byte
				out.putShort((short) 0);//Point Source ID unsigned short 2 bytes
			}
		}
	}

	private static int getFormat0Flag(int returnNumber, int returns) {		
		int rn = 7 < returnNumber ? 7 : returnNumber;
		int rs = 7 < returns ? 7 : returns;		
		return rn & (rs << 3);		
	}

	private static void writePointDataRecordFormat6(BufferedDataOuputStreamLE out, PointTable[] pointTables, double xoff, double yoff, double zoff) throws IOException {
		for(PointTable p:pointTables) {
			int len = p.rows;
			double[] xs = p.x == null ? new double[len] : p.x;
			double[] ys = p.y == null ? new double[len] : p.y;
			double[] zs = p.z == null ? new double[len] : p.z;
			char[] is = p.intensity == null ? new char[len] : p.intensity;
			byte[] rns = p.returnNumber == null ? new byte[len] : p.returnNumber;
			byte[] rs = p.returns == null ? new byte[len] : p.returns;
			byte[] cs = p.classification == null ? new byte[len] : p.classification;
			byte[] sars = p.scanAngleRank == null ? new byte[len] : p.scanAngleRank;
			for (int i = 0; i < len; i++) {
				out.putInt((int) ((xs[i] - xoff) * 1000d)); //X long 4 bytes
				out.putInt((int) ((ys[i] - yoff) * 1000d)); //Y long 4 bytes
				out.putInt((int) ((zs[i] - zoff)  * 1000d)); //Z long 4 bytes
				out.putChar(is[i]);//Intensity unsigned short 2 bytes
				//Return Number 4 bits (bits 0 - 3) 4 bits
				//Number of Returns (given pulse) 4 bits (bits 4 - 7) 4 bits
				out.putByte((byte) getFormat6ReturnFlag(rns[i], rs[i]));
				//Classification Flags 4 bits (bits 0 - 3) 4 bits
				//Scanner Channel 2 bits (bits 4 - 5) 2 bits
				//Scan Direction Flag 1 bit (bit 6) 1 bit
				//Edge of Flight Line 1 bit (bit 7) 1 bit
				out.putByte((byte) 0);
				out.putByte(cs[i]);//Classification unsigned char 1 byte
				out.putByte((byte) 0);//User Data unsigned char 1 byte
				out.putShort((short) (sars[i] / 0.006d));//Scan Angle short 2 bytes
				out.putShort((short) 0);//Point Source ID unsigned short 2 bytes
				out.putDouble(0);//GPS Time double 8 bytes			
			}
		}
	}

	private static int getFormat6ReturnFlag(int returnNumber, int returns) {		
		int rn = 15 < returnNumber ? 15 : returnNumber;
		int rs = 15 < returns ? 15 : returns;		
		return rn & (rs << 4);		
	}
}
