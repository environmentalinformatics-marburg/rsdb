package util;

import java.util.concurrent.ThreadLocalRandom;

public class Nonce {
	
	private static final char[] chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
	private static final int charsLen = chars.length;
	
	public static String get(int len) {
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		char[] nonce = new char[len];
		for (int i = 0; i < len; i++) {
			nonce[i] = chars[rnd.nextInt(charsLen)];
		}
		return String.valueOf(nonce);
	}

	public static boolean isValid(String nonce, int len) {
		if(nonce == null || nonce.length() != len) {
			return false;
		}
		for (int i = 0; i < len; i++) {
			char c = nonce.charAt(i);
			boolean valid = false;
			for (int j = 0; j < charsLen; j++) {
				if(c == chars[j]) {
					valid = true;
				}
			}
			if(!valid) {
				return false;
			}
		}
		return true;
	}
}
