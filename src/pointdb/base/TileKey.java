package pointdb.base;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;

import org.mapdb.Serializer;

public class TileKey implements Comparable<TileKey> {

	public final int x;
	public final int y;

	public TileKey(int x,int y) {
		this.x = x;
		this.y = y;
	}

	public static TileKey of(int x,int y) {
		return new TileKey(x,y);
	}

	@Override
	public String toString() {
		return "("+x+","+y+")";
	}

	@Override
	public int hashCode() {
		return x ^ Integer.reverse(y);
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
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}

	/*public static final Comparator<TileKey> COMPARATOR = new Comparator<TileKey>() {// old
		@Override
		public int compare(TileKey k1, TileKey k2) {
			int cmp = Integer.compare(k1.x, k2.x);
			return cmp==0?Integer.compare(k1.y, k2.y):cmp;
		}		
	};*/

	public static final Comparator<TileKey> COMPARATOR = new Comparator<TileKey>() {
		@Override
		public int compare(TileKey k1, TileKey k2) {
			int cmp = Integer.compare(k1.y, k2.y);
			return cmp==0?Integer.compare(k1.x, k2.x):cmp;
		}		
	};

	public static final Serializer<TileKey> SERIALIZER = new Serializer<TileKey>() {
		@Override
		public void serialize(DataOutput out, TileKey tileKey) throws IOException {
			out.writeInt(tileKey.x);
			out.writeInt(tileKey.y);
		}
		@Override
		public TileKey deserialize(DataInput in, int available) throws IOException {
			int x = in.readInt();
			int y = in.readInt();
			return new TileKey(x, y);
		}		
	};

	@Override
	public int compareTo(TileKey o) {
		int cmp = Integer.compare(this.y, o.y);
		return cmp==0?Integer.compare(this.x, o.x):cmp;
	}
}
