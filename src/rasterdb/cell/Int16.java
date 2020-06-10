package rasterdb.cell;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.Range2d;

public class Int16 {
	private static final Logger log = LogManager.getLogger();
	
	


	
	public static byte[] encInt16_split(short[] data) {
		int size = data.length;
		int len = size << 1;
		byte[] result = new byte[len];
		for (int i = 0; i < size; i++) {
			short v = data[i];
			result[i] = (byte) v;
			result[i + size] = (byte) (v>>8);
		}
		return result;
	}

	public static short[] decInt16_split(byte[] data) {	
		int len = data.length;
		int size = len >> 1;		
		short[] result = new short[size];
		for(int i = 0; i < size; i++) {
			result[i] = (short) ((data[i] & 0xFF) | ((data[i + size] & 0xFF)<<8));
		}
		return result;
	}
	
	public static int countNotNa_raw(short[] raw, short na) {
		int len = raw.length;
		int cnt = 0;
		for(int i = 0; i < len; i++) {
			if(raw[i] != na) {
				cnt++;
			}
		}
		return cnt;
	}

	public static int countNotNa_raw(int[] raw, int na) {
		int len = raw.length;
		int cnt = 0;
		for(int i = 0; i < len; i++) {
			if(raw[i] != na) {
				cnt++;
			}
		}
		return cnt;
	}
}
