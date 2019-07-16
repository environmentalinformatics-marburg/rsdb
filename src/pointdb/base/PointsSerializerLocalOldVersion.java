package pointdb.base;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.mapdb.Serializer;

import me.lemire.integercompression.FastPFOR128;
import me.lemire.integercompression.IntCompressor;
import me.lemire.integercompression.VariableByte;
import util.Serialisation;

public final class PointsSerializerLocalOldVersion extends Serializer<Point[]>{
	
	private PointsSerializerLocalOldVersion(){}
	
	public static final Serializer<Point[]> DEFAULT = new PointsSerializerLocalOldVersion();
	
	private static ThreadLocal<IntCompressor> threadLocal_ic = new ThreadLocal<IntCompressor>() {
		@Override
		protected IntCompressor initialValue() {
			SkippableCompositionOldVersion_0_1_11 codec = new SkippableCompositionOldVersion_0_1_11(new FastPFOR128(), new VariableByte());
			IntCompressor ic = new IntCompressor(codec);
			return ic;
		}		
	};	

	@Override
	public void serialize(DataOutput out, Point[] value) throws IOException {
		int len = value.length;
		out.writeInt(len);
		
		IntCompressor ic = threadLocal_ic.get();

		int[] data = new int[len];

		for(int i=0;i<len;i++) {
			data[i] = value[i].x;
		}
		Serialisation.encodeDeltaZigZag(data);
		Serialisation.INT_ARRAY_compatible_Serializer.serialize(out, ic.compress(data));
		
		for(int i=0;i<len;i++) {
			data[i] = value[i].y;
		}
		Serialisation.encodeDeltaZigZag(data);
		Serialisation.INT_ARRAY_compatible_Serializer.serialize(out, ic.compress(data));
		
		for(int i=0;i<len;i++) {
			data[i] = value[i].z;
		}
		Serialisation.encodeDeltaZigZag(data);
		Serialisation.INT_ARRAY_compatible_Serializer.serialize(out, ic.compress(data));
		
		for(int i=0;i<len;i++) {
			data[i] = value[i].intensity;
		}
		Serialisation.encodeDeltaZigZag(data);
		Serialisation.INT_ARRAY_compatible_Serializer.serialize(out, ic.compress(data));
		
		for(int i=0;i<len;i++) {
			data[i] = value[i].returnNumber;
		}
		Serialisation.encodeDeltaZigZag(data);
		Serialisation.INT_ARRAY_compatible_Serializer.serialize(out, ic.compress(data));
		
		for(int i=0;i<len;i++) {
			data[i] = value[i].returns;
		}
		Serialisation.encodeDeltaZigZag(data);
		Serialisation.INT_ARRAY_compatible_Serializer.serialize(out, ic.compress(data));
		
		for(int i=0;i<len;i++) {
			data[i] = value[i].scanAngleRank;
		}
		Serialisation.encodeDeltaZigZag(data);
		Serialisation.INT_ARRAY_compatible_Serializer.serialize(out, ic.compress(data));
		
		for(int i=0;i<len;i++) {
			data[i] = value[i].classification;
		}
		Serialisation.encodeDeltaZigZag(data);
		Serialisation.INT_ARRAY_compatible_Serializer.serialize(out, ic.compress(data));
		
		for(int i=0;i<len;i++) {
			data[i] = value[i].classificationFlags;
		}
		Serialisation.encodeDeltaZigZag(data);
		Serialisation.INT_ARRAY_compatible_Serializer.serialize(out, ic.compress(data));
	}

	@Override
	public Point[] deserialize(DataInput in, int available) throws IOException {
		int len = in.readInt();
		
		IntCompressor ic = threadLocal_ic.get();
		
		int[] x = ic.uncompress(Serialisation.INT_ARRAY_compatible_Serializer.deserialize(in, -1));
		Serialisation.decodeDeltaZigZag(x);
		
		int[] y = ic.uncompress(Serialisation.INT_ARRAY_compatible_Serializer.deserialize(in, -1));
		Serialisation.decodeDeltaZigZag(y);
		
		int[] z = ic.uncompress(Serialisation.INT_ARRAY_compatible_Serializer.deserialize(in, -1));
		Serialisation.decodeDeltaZigZag(z);
		
		int[] intensity = ic.uncompress(Serialisation.INT_ARRAY_compatible_Serializer.deserialize(in, -1));
		Serialisation.decodeDeltaZigZag(intensity);
		
		int[] returnNumber = ic.uncompress(Serialisation.INT_ARRAY_compatible_Serializer.deserialize(in, -1));
		Serialisation.decodeDeltaZigZag(returnNumber);
		
		int[] returns = ic.uncompress(Serialisation.INT_ARRAY_compatible_Serializer.deserialize(in, -1));
		Serialisation.decodeDeltaZigZag(returns);
		
		int[] scanAngleRank = ic.uncompress(Serialisation.INT_ARRAY_compatible_Serializer.deserialize(in, -1));
		Serialisation.decodeDeltaZigZag(scanAngleRank);
		
		int[] classification = ic.uncompress(Serialisation.INT_ARRAY_compatible_Serializer.deserialize(in, -1));
		Serialisation.decodeDeltaZigZag(classification);
		
		int[] classificationFlags = ic.uncompress(Serialisation.INT_ARRAY_compatible_Serializer.deserialize(in, -1));
		Serialisation.decodeDeltaZigZag(classificationFlags);

		Point[] points = new Point[len];
		for(int i=0;i<len;i++) {			
			points[i] = new Point(x[i], y[i], z[i], (char)intensity[i], (byte)returnNumber[i], (byte)returns[i], (byte)scanAngleRank[i], (byte)classification[i], (byte)classificationFlags[i]);
		}

		return points;
	}
}
