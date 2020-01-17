package util;

public class Hex {
	
	private static final char[] chars = "0123456789abcdef".toCharArray();
	private static final int charsLen = chars.length;
	
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
	
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = chars[v >>> 4];
	        hexChars[j * 2 + 1] = chars[v & 0x0F];
	    }
	    return new String(hexChars);
	}

	public static byte[] hexToBytes(String s) { // derived from: https://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
}
