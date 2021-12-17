package pointdb.base;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


import org.tinylog.Logger;
import org.mapdb.Serializer;

import me.lemire.integercompression.IntCompressor;
import util.Serialisation;

public class PointsSerializerPlain extends Serializer<Point[]> {
	@SuppressWarnings("unused")
	
	
	public static final Serializer<Point[]> DEFAULT = new PointsSerializerPlain();

	private PointsSerializerPlain(){}

	@Override
	public void serialize(DataOutput out, Point[] value) throws IOException {
		int len = value.length;
		out.writeInt(len);
		IntCompressor ic = Serialisation.THREAD_LOCAL_IC.get();
		int[] data = new int[len];

		for(int i=0;i<len;i++) {
			data[i] = value[i].x;
		}
		Serialisation.encodeDeltaZigZag(data);		
		Serialisation.serializeIntArray(out, ic.compress(data));

		for(int i=0;i<len;i++) {
			data[i] = value[i].y;
		}
		Serialisation.encodeDeltaZigZag(data);		
		Serialisation.serializeIntArray(out, ic.compress(data));

		for(int i=0;i<len;i++) {
			data[i] = value[i].z;
		}
		Serialisation.encodeDeltaZigZag(data);		
		Serialisation.serializeIntArray(out, ic.compress(data));

		for(int i=0;i<len;i++) {
			data[i] = value[i].intensity;
		}
		Serialisation.encodeDeltaZigZag(data);		
		Serialisation.serializeIntArray(out, ic.compress(data));

		for(int i=0;i<len;i++) {
			data[i] = value[i].returnNumber;
		}
		Serialisation.encodeDeltaZigZag(data);		
		Serialisation.serializeIntArray(out, ic.compress(data));

		for(int i=0;i<len;i++) {
			data[i] = value[i].returns;
		}
		Serialisation.encodeDeltaZigZag(data);		
		Serialisation.serializeIntArray(out, ic.compress(data));

		for(int i=0;i<len;i++) {
			data[i] = value[i].scanAngleRank;
		}
		Serialisation.encodeDeltaZigZag(data);		
		Serialisation.serializeIntArray(out, ic.compress(data));

		for(int i=0;i<len;i++) {
			data[i] = value[i].classification;
		}
		Serialisation.encodeDeltaZigZag(data);		
		Serialisation.serializeIntArray(out, ic.compress(data));

		for(int i=0;i<len;i++) {
			data[i] = value[i].classificationFlags;
		}
		Serialisation.encodeDeltaZigZag(data);		
		Serialisation.serializeIntArray(out, ic.compress(data));
	}

	@Override
	public Point[] deserialize(DataInput in, int available) throws IOException {
		int len = in.readInt();
		IntCompressor ic = Serialisation.THREAD_LOCAL_IC.get();

		int[] x = ic.uncompress(Serialisation.deserializeIntArray(in));
		Serialisation.decodeDeltaZigZag(x);

		int[] y = ic.uncompress(Serialisation.deserializeIntArray(in));
		Serialisation.decodeDeltaZigZag(y);

		int[] z = ic.uncompress(Serialisation.deserializeIntArray(in));
		Serialisation.decodeDeltaZigZag(z);

		int[] intensity = ic.uncompress(Serialisation.deserializeIntArray(in));
		Serialisation.decodeDeltaZigZag(intensity);

		int[] returnNumber = ic.uncompress(Serialisation.deserializeIntArray(in));
		Serialisation.decodeDeltaZigZag(returnNumber);

		int[] returns = ic.uncompress(Serialisation.deserializeIntArray(in));
		Serialisation.decodeDeltaZigZag(returns);

		int[] scanAngleRank = ic.uncompress(Serialisation.deserializeIntArray(in));
		Serialisation.decodeDeltaZigZag(scanAngleRank);

		int[] classification = ic.uncompress(Serialisation.deserializeIntArray(in));
		Serialisation.decodeDeltaZigZag(classification);

		int[] classificationFlags = ic.uncompress(Serialisation.deserializeIntArray(in));
		Serialisation.decodeDeltaZigZag(classificationFlags);

		Point[] points = new Point[len];
		for(int i=0;i<len;i++) {			
			points[i] = new Point(x[i], y[i], z[i], (char)intensity[i], (byte)returnNumber[i], (byte)returns[i], (byte)scanAngleRank[i], (byte)classification[i], (byte)classificationFlags[i]);
		}

		return points;
	}

}
