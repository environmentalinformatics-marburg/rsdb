package pointdb.base;

import java.util.Comparator;

import pointcloud.Rect2d;
import pointcloud.Rect3d;

public class GeoPoint {
	
	public static final GeoPoint MAX_Z = GeoPoint.of(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
	
	public final double x;
	public final double y;
	public final double z;
	public final char intensity;
	public final byte returnNumber;
	public final byte returns;
	public final byte scanAngleRank;
	/**
	 * classifications:
	 * <br> 0: not classified
	 * <br> 1: Unclassified
	 * <br> 2: Ground
	 * <br> 3: Low Vegetation
	 * <br> 4: Medium Vegetation
	 * <br> 5: High Vegetation
	 * <br> 6: Building
	 * <br> 7: Low Point (noise)
	 * <br> 8: Model Key-point (mass point)
	 * <br> 9: Water
	 * <br>13: unofficial classification in layer "hessen" Vegetation 
	 * <br>Reserved for ASPRS Definition (13-31) 
	 */
	public final byte classification;
	public final byte classificationFlags;
	
	public GeoPoint(double x2, double y2, double z2, char intensity, byte returnNumber, byte returns, byte scanAngleRank, byte classification, byte classificationFlags) {
		this.x = x2;
		this.y = y2;
		this.z = z2;
		this.intensity = intensity;
		this.returnNumber = returnNumber;
		this.returns = returns;
		this.scanAngleRank = scanAngleRank;
		this.classification = classification;
		this.classificationFlags = classificationFlags;
	}
	
	public static GeoPoint of(double x, double y, double z, Point p) {
		return new GeoPoint(x, y, z, p.intensity, p.returnNumber, p.returns, p.scanAngleRank, p.classification, p.classificationFlags);
	}
	
	public static GeoPoint of(double x, double y, double z, GeoPoint p) {
		return new GeoPoint(x, y, z, p.intensity, p.returnNumber, p.returns, p.scanAngleRank, p.classification, p.classificationFlags);
	}
	
	public static GeoPoint of(double x, double y, GeoPoint p) {
		return new GeoPoint(x, y, p.z, p.intensity, p.returnNumber, p.returns, p.scanAngleRank, p.classification, p.classificationFlags);
	}
	
	public static GeoPoint of(double x, double y, double z) {
		return new GeoPoint(x, y, z, (char)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0);
	}

	@Override
	public String toString() {
		return String.format(
				"GeoPoint [x=%s, y=%s, z=%s, intensity=%s, returnNumber=%s, returns=%s, scanAngleRank=%s, classification=%s, classificationFlags=%s]",
				x, y, z, (int)intensity, returnNumber, returns, scanAngleRank, classification, classificationFlags);
	}
	
	public static Rect2d getRect2d(Iterable<GeoPoint> points) {
		double xmin = Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;
		double xmax = -Double.MAX_VALUE;
		double ymax = -Double.MAX_VALUE;
		for(GeoPoint p:points) {
			if(p.x < xmin) xmin = p.x;
			if(p.y < ymin) ymin = p.y;
			if(p.x > xmax) xmax = p.x;
			if(p.y > ymax) ymax = p.y;
		}
		return new Rect2d(xmin, ymin, xmax, ymax);
	}
	
	public static Rect2d getRect2d(GeoPoint[] points) {
		double xmin = Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;
		double xmax = -Double.MAX_VALUE;
		double ymax = -Double.MAX_VALUE;
		for(GeoPoint p:points) {
			if(p.x < xmin) xmin = p.x;
			if(p.y < ymin) ymin = p.y;
			if(p.x > xmax) xmax = p.x;
			if(p.y > ymax) ymax = p.y;
		}
		return new Rect2d(xmin, ymin, xmax, ymax);
	}
	
	public static Rect3d getRect3d(Iterable<GeoPoint> points) {
		double xmin = Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;
		double zmin = Double.MAX_VALUE;
		double xmax = -Double.MAX_VALUE;
		double ymax = -Double.MAX_VALUE;
		double zmax = -Double.MAX_VALUE;
		for(GeoPoint p:points) {
			if(p.x < xmin) xmin = p.x;
			if(p.y < ymin) ymin = p.y;
			if(p.z < zmin) zmin = p.z;
			if(p.x > xmax) xmax = p.x;
			if(p.y > ymax) ymax = p.y;
			if(p.z > zmax) zmax = p.z;
		}
		return new Rect3d(xmin, ymin, zmin, xmax, ymax, zmax);
	}
	
	public static Rect3d getRect3d(GeoPoint[] points) {
		double xmin = Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;
		double zmin = Double.MAX_VALUE;
		double xmax = -Double.MAX_VALUE;
		double ymax = -Double.MAX_VALUE;
		double zmax = -Double.MAX_VALUE;
		for(GeoPoint p:points) {
			if(p.x < xmin) xmin = p.x;
			if(p.y < ymin) ymin = p.y;
			if(p.z < zmin) zmin = p.z;
			if(p.x > xmax) xmax = p.x;
			if(p.y > ymax) ymax = p.y;
			if(p.z > zmax) zmax = p.z;
		}
		return new Rect3d(xmin, ymin, zmin, xmax, ymax, zmax);
	}
	
	/**
	 * Get distance to p at xy-plane.
	 * @param p
	 * @return
	 */
	public double distanceXY(GeoPoint p) {
		double dx = this.x - p.x;
		double dy = this.y - p.y;
		return Math.sqrt(dx*dx + dy*dy);
	}
	
	/**
	 * Get slope to p at xy-plane in z-direction.
	 * @param p
	 * @return negative: downhill, zero: no slope, positive: uphill
	 */
	public double slope(GeoPoint p) {
		return (p.z - this.z) / distanceXY(p);
	}
	
	/**
	 * no NaN checks, may cause deadlock in parallel sort !!!
	 */
	public static final Comparator<GeoPoint> Z_COMPARATOR_DEADLOCK = new Comparator<GeoPoint>() {
		@Override
		public int compare(GeoPoint p1, GeoPoint p2) {
			return (p1.z < p2.z) ? -1 : (p1.z > p2.z) ? 1 : 0;
		}
		
	};
	
	public static final Comparator<GeoPoint> Z_COMPARATOR_SAFE = new Comparator<GeoPoint>() {
		@Override
		public int compare(GeoPoint p1, GeoPoint p2) {
			return Double.compare(p1.z, p2.z);
		}
		
	};

	/**
	 * no NaN checks, may cause deadlock in parallel sort !!!
	 */
	public static final Comparator<GeoPoint> Z_REVERSE_COMPARATOR_DEADLOCK = new Comparator<GeoPoint>() {
		@Override
		public int compare(GeoPoint p1, GeoPoint p2) {
			return (p1.z < p2.z) ? 1 : (p1.z > p2.z) ? -1 : 0;
		}
		
	};
	
	public static final Comparator<GeoPoint> Z_REVERSE_COMPARATOR_SAFE = new Comparator<GeoPoint>() {
		@Override
		public int compare(GeoPoint p1, GeoPoint p2) {
			return Double.compare(p2.z, p1.z);
		}
		
	};

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}

	public char getIntensity() {
		return intensity;
	}

	public byte getReturnNumber() {
		return returnNumber;
	}

	public byte getReturns() {
		return returns;
	}

	public byte getScanAngleRank() {
		return scanAngleRank;
	}

	public byte getClassification() {
		return classification;
	}

	public byte getClassificationFlags() {
		return classificationFlags;
	}
	
	public boolean isVegetaion() {
		return classification == 3 // low vegetation
				|| classification == 4  // medium vegetation
				|| classification == 5  // high vegetation 
				|| classification == 13  // wire - guard
				|| classification == 0 // not classified
				|| classification == 1 // unassigned
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
	
	public boolean isFirstReturn() {
		return returnNumber == 1;
	}
}