package rasterunit;

import java.util.Comparator;

public class RowKey {
	
	public final int t;
	public final int b;
	public final int y;
	
	public RowKey(int t, int b, int y) {
		this.t = t;
		this.b = b;
		this.y = y;
	}
	
	public TileKey toTileKey(int x) {
		return new TileKey(t, b, y, x);
	}
	
	public TileKey toTileKeyMin() {
		return toTileKey(Integer.MIN_VALUE);
	}
	
	public TileKey toTileKeyMax() {
		return toTileKey(Integer.MAX_VALUE);
	}
	
	public static final Comparator<RowKey> COMPARATOR = new Comparator<RowKey>() {
		@Override
		public int compare(RowKey k1, RowKey k2) {
			int tcmp = Integer.compare(k1.t, k2.t);
			if(tcmp!=0) {
				return tcmp;
			}			
			int bcmp = Integer.compare(k1.b, k2.b);
			if(bcmp!=0) {
				return bcmp;
			}			
			return Integer.compare(k1.y, k2.y);
		}		
	};

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + b;
		result = prime * result + t;
		result = prime * result + y;
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
		RowKey other = (RowKey) obj;
		if (b != other.b)
			return false;
		if (t != other.t)
			return false;
		if (y != other.y)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RowKey [t=" + t + ", b=" + b + ", y=" + y + "]";
	}

}
