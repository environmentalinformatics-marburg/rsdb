package pointdb.base;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.mapdb.Serializer;

public class Tile {
	
	public static final Tile SENTINEL = new Tile(null,null);
	
	public final TileMeta meta;
	public final Point[] points;
	
	public Tile(TileMeta meta, Point[] points) {
		this.meta = meta;
		this.points = points;
	}
	
	public static Tile of(TileKey tileKey, Point[] points) {
		return new Tile(TileMeta.of(tileKey, points), points);
	}
	
	public static Tile of(int x, int y, Point[] points) {
		return of(TileKey.of(x, y), points);
	}
	
	@Override
	public String toString() {
		return meta.toString();
	}
	
	public static final Serializer<Tile> SERIALIZER = new Serializer<Tile>() {
		@Override
		public void serialize(DataOutput out, Tile tile) throws IOException {
			TileMeta.SERIALIZER.serialize(out, tile.meta);
			PointsSerializerLocal.DEFAULT.serialize(out, tile.points);
		}

		@Override
		public Tile deserialize(DataInput in, int available) throws IOException {
			TileMeta meta = TileMeta.SERIALIZER.deserialize(in, -1);
			Point[] points = PointsSerializerLocal.DEFAULT.deserialize(in, -1);
			return new Tile(meta,points);
		}		
	};
	
	public static final Serializer<Tile> SERIALIZER_PLAIN = new Serializer<Tile>() {
		@Override
		public void serialize(DataOutput out, Tile tile) throws IOException {
			TileMeta.SERIALIZER.serialize(out, tile.meta);
			PointsSerializerPlain.DEFAULT.serialize(out, tile.points);
		}

		@Override
		public Tile deserialize(DataInput in, int available) throws IOException {
			TileMeta meta = TileMeta.SERIALIZER.deserialize(in, -1);
			Point[] points = PointsSerializerPlain.DEFAULT.deserialize(in, -1);
			return new Tile(meta,points);
		}		
	};
	
	public int size() {
		return points.length;
	}
}
