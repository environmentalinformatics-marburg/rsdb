package pointdb.base;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.mapdb.Serializer;

public class TileMeta {
	
	public final int x;
	public final int y;
	
	public final int point_count;
	
	public final Point min;
	public final Point max;
	public final Point avg;
	
	public TileMeta(int x,int y, int point_count, Point min, Point max, Point avg) {
		this.x = x;
		this.y = y;
		
		this.point_count = point_count;
		this.min = min;
		this.max = max;
		this.avg = avg;
	}
	
	public TileKey createTileKey() {
		return new TileKey(x, y);
	}
	
	@Override
	public String toString() {
		return "("+x+","+y+":"+point_count+")";
	}
	
	public static final Serializer<TileMeta> SERIALIZER = new Serializer<TileMeta>() {
		@Override
		public void serialize(DataOutput out, TileMeta tileMeta) throws IOException {
			out.writeInt(tileMeta.x);
			out.writeInt(tileMeta.y);
			out.writeInt(tileMeta.point_count);
			Point.SERIALIZER.serialize(out, tileMeta.min);
			Point.SERIALIZER.serialize(out, tileMeta.max);
			Point.SERIALIZER.serialize(out, tileMeta.avg);
		}
		@Override
		public TileMeta deserialize(DataInput in, int available) throws IOException {
			int x = in.readInt();
			int y = in.readInt();
			int point_count = in.readInt();
			Point min = Point.SERIALIZER.deserialize(in, -1);
			Point max = Point.SERIALIZER.deserialize(in, -1);
			Point avg = Point.SERIALIZER.deserialize(in, -1);
			return new TileMeta(x, y, point_count, min, max, avg);
		}
		
	};
	
	public static TileMeta of(TileKey tileKey, Point[] points) {
		int min_x = Integer.MAX_VALUE;
		int min_y= Integer.MAX_VALUE;
		int min_z= Integer.MAX_VALUE;
		char min_intensity= Character.MAX_VALUE;
		byte min_returnNumber = 127;
		byte min_returns = 127;
		byte min_scanAngleRank = Byte.MAX_VALUE;
		int max_x = Integer.MIN_VALUE; 
		int max_y = Integer.MIN_VALUE;
		int max_z = Integer.MIN_VALUE;
		char max_intensity = Character.MIN_VALUE;
		byte max_returnNumber = 0;
		byte max_returns = 0;
		byte max_scanAngleRank = Byte.MIN_VALUE;
		long sum_z = 0;
		long sum_intensity = 0;
		
		for(Point p:points) {
			if(p.x<min_x) min_x = p.x;
			if(p.y<min_y) min_y = p.y;
			if(p.z<min_z) min_z = p.z;
			if(p.intensity<min_intensity) min_intensity = p.intensity;
			if(p.returnNumber<min_returnNumber) min_returnNumber = p.returnNumber;
			if(p.returns<min_returns) min_returns = p.returns;
			if(p.scanAngleRank<min_scanAngleRank) min_scanAngleRank = p.scanAngleRank;
			
			if(p.x>max_x) max_x = p.x;
			if(p.y>max_y) max_y = p.y;
			if(p.z>max_z) max_z = p.z;
			if(p.intensity>max_intensity) max_intensity = p.intensity;
			if(p.returnNumber>max_returnNumber) max_returnNumber = p.returnNumber;
			if(p.returns>max_returns) max_returns = p.returns;
			if(p.scanAngleRank>max_scanAngleRank) max_scanAngleRank = p.scanAngleRank;
			
			sum_z += p.z;
			sum_intensity += p.intensity;
		}
		
		Point min = new Point(min_x, min_y, min_z, min_intensity, min_returnNumber, min_returns, min_scanAngleRank, (byte)0, (byte)0);
		Point max = new Point(max_x, max_y, max_z, max_intensity, max_returnNumber, max_returns, max_scanAngleRank, (byte)0, (byte)0);
		int c = points.length==0?1:points.length;
		Point avg = new Point(0, 0, (int)(sum_z/c), (char)(sum_intensity/c), (byte)0, (byte)0, (byte)0, (byte)0, (byte)0);
		
		return new TileMeta(tileKey.x, tileKey.y, points.length, min, max, avg);
	}
	
	public static TileMeta merge(TileMeta meta1, TileMeta meta2) {
		if(!meta1.createTileKey().equals(meta2.createTileKey())) {
			throw new RuntimeException(meta1.createTileKey()+"  "+meta2.createTileKey());
		}
		
		Point a = meta1.min;
		Point b = meta2.min;
		Point min = new Point(a.x<b.x?a.x:b.x, 
				              a.y<b.y?a.y:b.y, 
				              a.z<b.z?a.z:b.z, 
				              a.intensity<b.intensity?a.intensity:b.intensity,
				              a.returnNumber<b.returnNumber?a.returnNumber:b.returnNumber,
				              a.returns<b.returns?a.returns:b.returns,
				              a.scanAngleRank<b.scanAngleRank?a.scanAngleRank:b.scanAngleRank,
				              (byte) 0,
				              (byte) 0);
		
		a = meta1.max;
		b = meta2.max;
		Point max = new Point(a.x<b.x?b.x:a.x, 
				              a.y<b.y?b.y:a.y,
				              a.z<b.z?b.z:a.z, 
				              a.intensity<b.intensity?b.intensity:a.intensity,
				              a.returnNumber<b.returnNumber?b.returnNumber:a.returnNumber,
				              a.returns<b.returns?b.returns:a.returns,
				              a.scanAngleRank<b.scanAngleRank?b.scanAngleRank:a.scanAngleRank,
						      (byte) 0,
						      (byte) 0);
		
		int avg_z = (int)((((long)meta1.avg.z)*meta1.point_count+((long)meta2.avg.z)*meta2.point_count)/(meta1.point_count+meta2.point_count));
		char avg_intensity = (char)((((long)meta1.avg.intensity)*meta1.point_count+((long)meta2.avg.intensity)*meta2.point_count)/(meta1.point_count+meta2.point_count));
		Point avg = new Point(
				0, 
				0, 
				avg_z, 
				avg_intensity, 
				(byte)0, 
				(byte)0, 
				(byte)0, 
				(byte)0, 
				(byte)0);
		
		return new TileMeta(meta1.x, meta1.y, meta1.point_count+meta2.point_count, min, max, avg);
	}

	public static TileMeta of(int x, int y, Point[] points) {
		return of(TileKey.of(x, y), points);
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getPoint_count() {
		return point_count;
	}

	public Point getMin() {
		return min;
	}

	public Point getMax() {
		return max;
	}

	public Point getAvg() {
		return avg;
	}
}
