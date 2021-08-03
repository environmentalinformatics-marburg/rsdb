package pointdb.base;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;

import org.mapdb.Serializer;

public class Point implements Comparable<Point>{

	public static final Point[] EMPTY_POINTS = new Point[0];

	public final int x;
	public final int y;
	public final int z;
	public final char intensity;
	public final byte returnNumber;
	public final byte returns;
	public final byte scanAngleRank;
	/**
	 * occurring classifications:
	 * <br>1: Unclassified
	 * <br>2: Ground
	 * <br>4: Medium Vegetation
	 * <br>8: Model Key-point (mass point) 
	 */
	public final byte classification;
	public final byte classificationFlags;

	public Point(int x, int y, int z, char intensity, byte returnNumber, byte returns, byte scanAngleRank, byte classification, byte classificationFlags) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.intensity = intensity;
		this.returnNumber = returnNumber;
		this.returns = returns;
		this.scanAngleRank = scanAngleRank;
		this.classification = classification;
		this.classificationFlags = classificationFlags;
	}

	public static Point of(int x, int y) {
		return new Point(x,y,0,(char)0,(byte)0,(byte)0,(byte)0,(byte)0,(byte)0);
	}

	public static Point of(int x, int y, Point p) {
		return new Point(x, y, p.z, p.intensity, p.returnNumber, p.returns, p.scanAngleRank, p.classification, p.classificationFlags);
	}
	
	public static Point of(int x, int y, int z, Point p) {
		return new Point(x, y, z, p.intensity, p.returnNumber, p.returns, p.scanAngleRank, p.classification, p.classificationFlags);
	}

	public static final Serializer<Point> SERIALIZER = new Serializer<Point>() {
		@Override
		public void serialize(DataOutput out, Point p) throws IOException {
			out.writeInt(p.x);
			out.writeInt(p.y);
			out.writeInt(p.z);
			out.writeChar(p.intensity);
			out.writeByte(p.returnNumber);
			out.writeByte(p.returns);
			out.writeByte(p.scanAngleRank);
			out.writeByte(p.classification);
			out.writeByte(p.classificationFlags);
		}
		@Override
		public Point deserialize(DataInput in, int available) throws IOException {
			int x = in.readInt();
			int y = in.readInt();
			int z = in.readInt();
			char intensity = in.readChar();
			byte returnNumber = in.readByte();
			byte returns = in.readByte();
			byte scanAngleRank = in.readByte();
			byte classification = in.readByte();
			byte classificationFlags = in.readByte();
			return new Point(x,y,z,intensity,returnNumber,returns, scanAngleRank, classification, classificationFlags);
		}		
	};

	public static final Serializer<Point[]> ARRAY_SERIALIZER = new Serializer<Point[]>() {
		@Override
		public void serialize(DataOutput out, Point[] points) throws IOException {
			out.writeInt(points.length);
			for(Point p:points) {
				out.writeInt(p.x);
				out.writeInt(p.y);
				out.writeInt(p.z);
				out.writeChar(p.intensity);
				out.writeByte(p.returnNumber);
				out.writeByte(p.returns);
				out.writeByte(p.scanAngleRank);
				out.writeByte(p.classification);
				out.writeByte(p.classificationFlags);
			}
		}

		@Override
		public Point[] deserialize(DataInput in, int available) throws IOException {
			int len = in.readInt();
			Point[] points = new Point[len];
			for(int i=0;i<points.length;i++) {
				int x = in.readInt();
				int y = in.readInt();
				int z = in.readInt();
				char intensity = in.readChar();
				byte returnNumber = in.readByte();
				byte returns = in.readByte();
				byte scanAngleRank = in.readByte();
				byte classification = in.readByte();
				byte classificationFlags = in.readByte();
				Point point = new Point(x, y, z, intensity, returnNumber, returns, scanAngleRank, classification, classificationFlags);
				points[i] = point;
			}
			return points;
		}

	};

	public static final Serializer<Point[]> ARRAY_DELTA_SERIALIZER = new Serializer<Point[]>() {
		@Override
		public void serialize(DataOutput out, Point[] points) throws IOException {
			out.writeInt(points.length);
			int prevX = 0;
			int prevY = 0;
			int prevZ = 0;
			char prevIntensity = 0;
			byte prevScanAngleRank = 0;
			for(Point p:points) {
				out.writeInt(p.x-prevX);
				out.writeInt(p.y-prevY);
				out.writeInt(p.z-prevZ);
				out.writeChar(p.intensity-prevIntensity);
				out.writeByte(p.returnNumber);
				out.writeByte(p.returns);
				out.writeByte(p.scanAngleRank-prevScanAngleRank);
				out.writeByte(p.classification);
				out.writeByte(p.classificationFlags);
				prevX = p.x;
				prevY = p.y;
				prevZ = p.z;
				prevIntensity = p.intensity;
				prevScanAngleRank = p.scanAngleRank;
			}
		}

		@Override
		public Point[] deserialize(DataInput in, int available) throws IOException {
			int len = in.readInt();
			Point[] points = new Point[len];
			int currX = 0;
			int currY = 0;
			int currZ = 0;
			char currIntensity = 0;
			byte currScanAngleRank = 0;
			for(int i=0;i<points.length;i++) {
				currX += in.readInt();
				currY += in.readInt();
				currZ += in.readInt();
				currIntensity += in.readChar();
				byte returnNumber = in.readByte();
				byte returns = in.readByte();
				currScanAngleRank += in.readByte();
				byte classification = in.readByte();
				byte classificationFlags = in.readByte();				
				Point point = new Point(currX, currY, currZ, currIntensity, returnNumber, returns, currScanAngleRank, classification, classificationFlags);
				points[i] = point;
			}
			return points;
		}

	};

	public static final Serializer<Point[]> ARRAY_SEPARATED_DELTA_SERIALIZER = new Serializer<Point[]>() {
		@Override
		public void serialize(DataOutput out, Point[] points) throws IOException {
			out.writeInt(points.length);
			int prevX = 0;
			for(Point p:points) {
				out.writeInt(p.x-prevX);
				prevX = p.x;
			}
			int prevY = 0;
			for(Point p:points) {
				out.writeInt(p.y-prevY);
				prevY = p.y;
			}
			int prevZ = 0;
			for(Point p:points) {
				out.writeInt(p.z-prevZ);
				prevZ = p.z;
			}
			char prevIntensity = 0;
			for(Point p:points) {
				out.writeChar(p.intensity-prevIntensity);
				prevIntensity = p.intensity;
			}			
			for(Point p:points) {
				out.writeByte(p.returnNumber);
			}			
			for(Point p:points) {
				out.writeByte(p.returns);
			}
			byte prevScanAngleRank = 0;
			for(Point p:points) {
				out.writeByte(p.scanAngleRank-prevScanAngleRank);
				prevScanAngleRank = p.scanAngleRank;
			}
			for(Point p:points) {
				out.writeByte(p.classification);
			}
			for(Point p:points) {
				out.writeByte(p.classificationFlags);
			}			
		}

		@Override
		public Point[] deserialize(DataInput in, int available) throws IOException {
			final int len = in.readInt();

			int[] x = new int[len];
			int currX = 0;
			for(int i=0;i<len;i++) {
				currX += in.readInt();				
				x[i] = currX;
			}

			int[] y = new int[len];
			int currY = 0;
			for(int i=0;i<len;i++) {
				currY += in.readInt();				
				y[i] = currY;
			}

			int[] z = new int[len];
			int currZ = 0;
			for(int i=0;i<len;i++) {
				currZ += in.readInt();				
				z[i] = currZ;
			}

			char[] intensity = new char[len];
			char currIntensity = 0;
			for(int i=0;i<len;i++) {
				currIntensity += in.readChar();
				intensity[i] = currIntensity;
			}

			byte[] returnNumber = new byte[len];
			for(int i=0;i<len;i++) {
				returnNumber[i] = in.readByte();
			}

			byte[] returns = new byte[len];
			for(int i=0;i<len;i++) {
				returns[i] = in.readByte();
			}

			byte[] scanAngleRank = new byte[len];
			byte currScanAngleRank = 0;
			for(int i=0;i<len;i++) {
				currScanAngleRank += in.readByte();
				scanAngleRank[i] = currScanAngleRank;
			}

			byte[] classifications = new byte[len];
			for(int i=0;i<len;i++) {
				classifications[i] = in.readByte();
			}

			byte[] classificationFlags = new byte[len];
			for(int i=0;i<len;i++) {
				classificationFlags[i] = in.readByte();
			}			

			Point[] points = new Point[len];
			for(int i=0;i<points.length;i++) {
				Point point = new Point(x[i], y[i], z[i], intensity[i], returnNumber[i], returns[i], scanAngleRank[i], classifications[i], classificationFlags[i]);
				points[i] = point;
			}

			return points;
		}

	};
	
	@Override
	public int compareTo(Point o) {
		int c = Integer.compare(x, o.x);
		return(c==0)?Integer.compare(y, o.y):c;		
	}

	public static Comparator<Point> XY_COMPARATOR = new Comparator<Point>() {
		@Override
		public int compare(Point a, Point b) {
			int c = Integer.compare(a.x, b.x);
			return(c==0)?Integer.compare(a.y, b.y):c;				
		}

	};
	
	@Override
	public String toString() {
		return "Point [x=" + x + ", y=" + y + ", z=" + z + ", intensity=" + (int)intensity + ", returnNumber=" + returnNumber
				+ ", returns=" + returns + "]";
	}
	
	public boolean isVegetaion() {
		return classification == 3 // low vegetation
				|| classification == 4  // medium vegetation
				|| classification == 5  // high vegetation 
				|| classification == 13  // wire - guard
				//|| classification == 1 // unassigned
				|| classification == 20; // (non standard) vegetation
	}
	
	public boolean isGround() {
		return classification == 2 // ground
				|| classification == 8; // model key/reserved
	}
	
	/**
	 * Point is classified as some valid entity.
	 * @return
	 */
	public boolean isEntity() {
		return isGround() 
				|| isVegetaion() 
				|| classification == 6 // building
				|| classification == 9; // water
	}	
	
	public boolean isLastReturn() {
		return returnNumber == returns;
	}
}