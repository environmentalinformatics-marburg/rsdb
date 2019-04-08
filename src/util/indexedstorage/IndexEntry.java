package util.indexedstorage;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;

import me.lemire.integercompression.IntCompressor;
import util.Serialisation;

public class IndexEntry {
	public final long pos;
	public final int len;

	public IndexEntry(long pos, int len) {
		this.pos = pos;
		this.len = len;
	}
	
	public long getBorder() {
		return pos+len;
	}

	public static void serialize(DataOutput out, Collection<IndexEntry> ies) throws IOException {				
		final int SIZE = ies.size();
		IntCompressor ic = Serialisation.THREAD_LOCAL_IC.get();

		long prevPos = 0;
		int[] deltas = new int[SIZE];
		int i=0;
		for(IndexEntry ie:ies) {
			long currPos = ie.pos;
			long deltaPos = currPos-prevPos;
			if(deltaPos<0 || deltaPos>Integer.MAX_VALUE) {
				throw new RuntimeException("delta below zero or larger than integer "+currPos);
			}
			deltas[i++] = (int) deltaPos;
			prevPos = currPos;
		}				
		Serialisation.writeCompressIntArray(out, ic.compress(deltas));				

		int prevLen = 0;
		i=0;
		for(IndexEntry ie:ies) {
			int currLen = ie.len;
			deltas[i++] = Serialisation.encodeZigZag(currLen - prevLen);
			prevLen = currLen;
		}
		Serialisation.writeCompressIntArray(out, ic.compress(deltas));				
	}

	public static IndexEntry[] deserialize(DataInput in) throws IOException {
		IntCompressor ic = Serialisation.THREAD_LOCAL_IC.get();

		int[] posDeltas = ic.uncompress(Serialisation.readUncompressIntArray(in));
		int[] lens = ic.uncompress(Serialisation.readUncompressIntArray(in));
		Serialisation.decodeDeltaZigZag(lens);

		final int SIZE = posDeltas.length;
		if(lens.length!=SIZE) {
			throw new RuntimeException("read error "+SIZE+" "+lens.length);
		}

		IndexEntry[] ies = new IndexEntry[SIZE];
		long pos = 0;
		for (int i = 0; i < SIZE; i++) {
			pos += posDeltas[i];
			ies[i] = new IndexEntry(pos, lens[i]);
		}

		return ies;
	}
}