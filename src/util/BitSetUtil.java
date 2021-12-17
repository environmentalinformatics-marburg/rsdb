package util;

import java.lang.reflect.Field;
import java.util.function.IntConsumer;

import com.googlecode.javaewah.datastructure.BitSet;

public class BitSetUtil {
	//

	private static final Field EWAH_DATA;

	static {
		Field fieldItems = null;
		try {
			fieldItems = com.googlecode.javaewah.datastructure.BitSet.class.getDeclaredField("data");
			fieldItems.setAccessible(true);
		} catch(Exception e) {}
		EWAH_DATA = fieldItems;
	}

	public static long[] getData(BitSet ewah) {
		try {
			return (long[]) EWAH_DATA.get(ewah);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void forEach(BitSet ewah, IntConsumer ic) {
		long[] data = getData(ewah);
		int len = data.length;
		for (int x = 0; x < len; ++x) {
			long w = data[x];
			int m = x << 6;
			while (w != 0) {
				long t = w & -w; //Bit Hack #7. Isolate the rightmost 1-bit. //http://www.catonmat.net/blog/low-level-bit-hacks-you-absolutely-must-know/
				int i = m + Long.bitCount(t - 1);
				ic.accept(i);
				w ^= t; // remove rightmost 1-bit
			}
		}
	}	
}
