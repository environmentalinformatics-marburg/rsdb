package pointdb.base;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;

import org.mapdb.Serializer;

import me.lemire.integercompression.IntCompressor;
import util.Serialisation;
import util.indexedstorage.BulkSerializer;

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
	
	public static final BulkSerializer<TileKey> BULK_SERIALIZER = new BulkSerializer<TileKey>() {

		@Override
		public void serializeBulk(DataOutput out, TileKey[] keys) throws IOException {
			final int SIZE = keys.length;
			IntCompressor ic = Serialisation.THREAD_LOCAL_IC.get();			
			int[] deltas = new int[SIZE];

			int prevY = 0;
			for (int i = 0; i < SIZE; i++) {
				int currY = keys[i].y;
				deltas[i] = currY-prevY;
				prevY = currY;
			}
			Serialisation.writeCompressIntArray(out, ic.compress(deltas));
			
			int prevX = 0;
			for (int i = 0; i < SIZE; i++) {
				int currX = keys[i].x;
				deltas[i] = currX-prevX;
				prevX = currX;
			}
			Serialisation.writeCompressIntArray(out, ic.compress(deltas));
		}

		@Override
		public TileKey[] deserializeBulk(DataInput in) throws IOException {
			IntCompressor ic = Serialisation.THREAD_LOCAL_IC.get();
			int[] yDeltas = ic.uncompress(Serialisation.readUncompressIntArray(in));
			int[] xDeltas = ic.uncompress(Serialisation.readUncompressIntArray(in));
			final int SIZE = yDeltas.length;
			if(xDeltas.length!=SIZE) {
				throw new RuntimeException("read error "+SIZE+" "+yDeltas.length+" "+xDeltas.length);
			}
			TileKey[] keys = new TileKey[SIZE];
			int y=0;
			int x=0;
			for (int i = 0; i < SIZE; i++) {
				y += yDeltas[i];
				x += xDeltas[i];
				keys[i] = new TileKey(x, y);
			}
			return keys;
		}

		@Override
		public Class<TileKey> getEntryClass() {
			return TileKey.class;
		}		
	};
}
