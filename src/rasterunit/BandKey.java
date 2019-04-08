package rasterunit;

import java.util.Comparator;

public class BandKey {
	
	public final int t;
	public final int b;
	
	public BandKey(int t, int b) {
		this.t = t;
		this.b = b;
	}
	
	public TileKey toTileKey(int y, int x) {
		return new TileKey(t, b, y, x);
	}
	
	public TileKey toTileKeyMin() {
		return toTileKey(Integer.MIN_VALUE, Integer.MIN_VALUE);
	}
	
	public TileKey toTileKeyMax() {
		return toTileKey(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}
	
	public static BandKey toBandKeyMin(int t) {
		return new BandKey(t, Integer.MIN_VALUE);
	}
	
	public static BandKey toBandKeyMax(int t) {
		return new BandKey(t, Integer.MAX_VALUE);
	}
	
	public static final Comparator<BandKey> COMPARATOR = new Comparator<BandKey>() {
		@Override
		public int compare(BandKey k1, BandKey k2) {
			int tcmp = Integer.compare(k1.t, k2.t);
			if(tcmp!=0) {
				return tcmp;
			}			
			return Integer.compare(k1.b, k2.b);
		}		
	};

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + b;
		result = prime * result + t;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BandKey other = (BandKey) obj;
		if (b != other.b)
			return false;
		if (t != other.t)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BandKey [t=" + t + ", b=" + b + "]";
	}
}
