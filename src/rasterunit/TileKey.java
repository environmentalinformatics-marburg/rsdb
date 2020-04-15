package rasterunit;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;

import org.mapdb.Serializer;

public class TileKey {
	
	public final int t;
	public final int b;
	public final int y;
	public final int x;
	
	public TileKey(int t, int b, int y, int x) {
		this.t = t;
		this.b = b;
		this.y = y;
		this.x = x;
	}
	
	public static final Comparator<TileKey> COMPARATOR = new Comparator<TileKey>() {
		@Override
		public int compare(TileKey k1, TileKey k2) {
			int tcmp = Integer.compare(k1.t, k2.t);
			if(tcmp!=0) {
				return tcmp;
			}			
			int bcmp = Integer.compare(k1.b, k2.b);
			if(bcmp!=0) {
				return bcmp;
			}			
			int ycmp = Integer.compare(k1.y, k2.y);
			if(ycmp!=0) {
				return ycmp;
			}
			return Integer.compare(k1.x, k2.x);
		}		
	};
	
	public static final Serializer<TileKey> SERIALIZER = new Serializer<TileKey>() {
		@Override
		public void serialize(DataOutput out, TileKey tileKey) throws IOException {
			out.writeInt(tileKey.t);
			out.writeInt(tileKey.b);
			out.writeInt(tileKey.y);
			out.writeInt(tileKey.x);
		}
		@Override
		public TileKey deserialize(DataInput in, int available) throws IOException {
			int t = in.readInt();
			int b = in.readInt();
			int y = in.readInt();
			int x = in.readInt();
			return new TileKey(t, b, y, x);
		}
		@Override
		public int fixedSize() {
			return 16;
		}
		@Override
		public boolean isTrusted() {
			return true;
		}		
	};

	@Override
	public String toString() {
		return "(t" + t + " b" + b + " y" + y + " x" + x + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + b;
		result = prime * result + t;
		result = prime * result + x;
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
		TileKey other = (TileKey) obj;
		if (t != other.t)			
			return false;
		if (b != other.b)
			return false;
		if (y != other.y)
			return false;
		if (x != other.x)
			return false;
		return true;
	}

}
