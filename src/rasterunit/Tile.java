package rasterunit;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.mapdb.Serializer;

public class Tile {
	//private static final Logger log = LogManager.getLogger();

	public final int t;
	public final int b;
	public final int y;
	public final int x;
	public final int type;

	public final byte[] data;

	public Tile(int t, int b, int y, int x, int type, byte[] data) {
		this.t = t;
		this.b = b;
		this.y = y;
		this.x = x;
		this.type = type;
		this.data = data;
	}

	public Tile(TileKey tileKey, int type, byte[] data) {
		this(tileKey.t, tileKey.b, tileKey.y, tileKey.x, type, data);
	}

	public static final Serializer<Tile> SERIALIZER = new Serializer<Tile>() {
		@Override
		public void serialize(DataOutput out, Tile tile) throws IOException {
			out.writeInt(tile.t);
			out.writeInt(tile.b);
			out.writeInt(tile.y);
			out.writeInt(tile.x);
			out.writeInt(tile.type);
			out.writeInt(tile.data.length);
			out.write(tile.data);
		}
		@Override
		public Tile deserialize(DataInput in, int available) throws IOException {
			int t = in.readInt();
			int b = in.readInt();
			int y = in.readInt();
			int x = in.readInt();
			int type = in.readInt();
			int len = in.readInt();
			byte[] data = new byte[len];
			in.readFully(data);
			return new Tile(t, b, y, x, type, data);
		}
		@Override
		public boolean isTrusted() {
			return true;
		}		
	};
	
	@Override
	public String toString() {
		return t+" "+b+" "+y+" "+x+" "+type+" "+data.length;
	}
}
