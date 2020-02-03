package util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import util.collections.array.ReadonlyArray;

public class Util {
	private static final Logger log = LogManager.getLogger();

	public static String msToText(long start, long end) {
		long diff = end-start;
		long h = diff%1000/100;
		long z = diff%100/10;
		long e = diff%10;
		return diff/1000+"."+h+z+e+" s";
	}

	/**
	 * http://stackoverflow.com/questions/80476/how-to-concatenate-two-arrays-in-java
	 * @param a
	 * @param b
	 * @return
	 */
	public static <T> T[] concatenate(T[] a, T[] b) {
		int aLen = a.length;
		int bLen = b.length;

		@SuppressWarnings("unchecked")
		T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen+bLen);
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);

		return c;
	}

	public static boolean createPathOfFile(File filename) {
		try {
			File parentFile = filename.getParentFile();
			if(parentFile==null) { //no parent path
				return false;
			}
			parentFile.mkdirs();
			return true;
		} catch(Exception e) {
			log.error("path not created: "+filename+"    "+e);
			return false;
		}		
	}

	public static boolean createPathOfFile(String filename) {
		return createPathOfFile(new File(filename));

	}

	/*@FunctionalInterface
	public static interface Invalidation extends InvalidationListener {
		public void invalidated();
		@Override
		public default void invalidated(Observable observable) {
			invalidated();
		}
	}

	public static InvalidationListener invalidation(Invalidation invalidation) {
		return invalidation;
	}*/

	/**
	 * Checks if file is locked (by operating system).
	 * If file does not exist no error is thrown. 
	 * @param file
	 */
	public static void checkFileNotLocked(File file) {
		if(!file.exists()) {
			//log.info("file does not exist "+file);
			return;
		}
		try {
			FileChannel filechannel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE);
			FileLock lock = filechannel.tryLock();
			if(lock==null) {
				throw new RuntimeException("file locked "+file);
			}
			lock.release();
			filechannel.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}

	public static void checkPortNotListening(int port) {
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException("could not listen on port "+port+"   "+e);
		}		
	}

	/**
	 * reverses rows (y-direction)
	 * @param data
	 * @return
	 */
	public static short[][] flipRows(short[][] data) {
		final int len = data.length;
		final int max = len-1;
		short[][] temp = new short[len][];		
		for (int i = 0; i < len; i++) {
			temp[i] = data[max-i];
		}
		return temp;
	}

	/**
	 * reverses rows (y-direction)
	 * @param data
	 * @return
	 */
	public static double[][] flipRows(double[][] data) {
		final int len = data.length;
		final int max = len-1;
		double[][] temp = new double[len][];		
		for (int i = 0; i < len; i++) {
			temp[i] = data[max-i];
		}
		return temp;
	}

	/**
	 * reverses rows (y-direction)
	 * @param data
	 * @return
	 */
	public static float[][] flipRows(float[][] data) {
		final int len = data.length;
		final int max = len-1;
		float[][] temp = new float[len][];		
		for (int i = 0; i < len; i++) {
			temp[i] = data[max-i];
		}
		return temp;
	}

	public static Path[] getPaths(Path root) throws IOException {
		DirectoryStream<Path> dirStream = Files.newDirectoryStream(root);
		Path[] paths = StreamSupport.stream(dirStream.spliterator(), false)
				.sorted()
				.toArray(Path[]::new);
		dirStream.close();
		return paths;
	}

	public static short[][] arrayToArrayArray(short[] a, int line, short r[][]) {
		int count = a.length;
		int lines = count/line;
		if(r==null || r.length!=lines) {
			return arrayToArrayArray(a, line);
		}
		for(int i=0;i<lines;i++) {
			short[] row = r[i];
			if(row==null || row.length!=line) {
				return arrayToArrayArray(a, line);
			}
			System.arraycopy(a, i*line, row, 0, line);
		}
		return r;
	}

	public static short[][] arrayToArrayArrayOfByte(byte[] a, int line, short r[][]) {
		int count = a.length;
		int lines = count/line;
		if(r==null || r.length!=lines) {
			return arrayToArrayArrayOfByte(a, line);
		}
		int apos = 0;
		for(int i=0;i<lines;i++) {
			short[] row = r[i];
			if(row==null || row.length!=line) {
				return arrayToArrayArrayOfByte(a, line);
			}			
			for(int x = 0; x < line; x++) {
				row[x] = (short) (a[apos++] & 0xff);
			}			
		}
		return r;
	}

	public static float[][] arrayToArrayArray(float[] a, int line, float r[][]) {
		int count = a.length;
		int lines = count/line;
		if(r==null || r.length!=lines) {
			return arrayToArrayArray(a, line);
		}
		for(int i=0;i<lines;i++) {
			float[] row = r[i];
			if(row==null || row.length!=line) {
				return arrayToArrayArray(a, line);
			}
			System.arraycopy(a, i*line, row, 0, line);
		}
		return r;
	}

	public static short[][] arrayToArrayArray(short[] a, int line) {
		int count = a.length;
		int lines = count/line;
		short[][] r = new short[lines][line];
		for(int i=0;i<lines;i++) {
			System.arraycopy(a, i*line, r[i], 0, line);
		}
		return r;
	}

	public static short[][] arrayToArrayArrayOfByte(byte[] a, int line) {
		int count = a.length;
		int lines = count/line;
		short[][] r = new short[lines][line];
		int apos = 0;
		for(int i=0;i<lines;i++) {
			short[] row = r[i];
			for(int x = 0; x < line; x++) {
				row[x] = (short) (a[apos++] & 0xff);
				/*if(row[x] != 255) {
					System.out.println(row[x]);
				}*/
			}
		}
		return r;
	}

	public static float[][] arrayToArrayArray(float[] a, int line) {
		int count = a.length;
		int lines = count/line;
		float[][] r = new float[lines][line];
		for(int i=0;i<lines;i++) {
			System.arraycopy(a, i*line, r[i], 0, line);
		}
		return r;
	}

	public static String[] columnTextToColumns(String columnText) {
		return Arrays.stream(columnText.split(",")).map(s->s.trim()).toArray(String[]::new);
	}

	public static RuntimeException rethrow(Throwable e) {
		if(e instanceof RuntimeException) {
			throw (RuntimeException)e;
		}
		throw new RuntimeException(e);
	}

	public static final String VALID_ID_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";

	public static boolean isValidIdentifier(String name) {
		int len = name.length();
		if(len<1) {
			return false;
		}
		for (int i = 0; i < len; i++) {
			char c = name.charAt(i);
			if( !( ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || ('0' <= c && c <= '9') || c == '_' ) ) {
				return false;
			}
		}
		return true;		
	}

	public static int[] getRange(int[] data) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		int len = data.length;
		for (int i = 0; i < len; i++) {
			int x = data[i];
			if(x < min) {
				min = x;
			}
			if(max < x) {
				max = x;
			}
		}
		return new int[] {min, max};
	}

	public static String hex(long n) {
		String h = Long.toHexString(n);
		return h.length() % 2 == 0 ? "0x" + h : "0x0" + h;
	}
	
	public static String hex(short n) {
		String h = Long.toHexString(n & 0xffff);
		return h.length() % 2 == 0 ? "0x" + h : "0x0" + h;
	}

	public static int[] getRational(double v) {
		if(!Double.isFinite(v)) {
			throw new RuntimeException("not finite");
		}
		if(v < 0) {
			throw new RuntimeException("negtaive");
		}
		if(v>1_000_000_000) {
			throw new RuntimeException("too large");
		} else if(v>1_000_000) {
			return new int[] {(int) (v*1_000),1_000};
		} else if(v>1_000) {
			return new int[] {(int) (v*1_000_000),1_000_000};
		} else {
			return new int[] {(int) (v*1_000_000_000),1_000_000_000};
		}
	}
	
	public static void checkSafePath(Path path) {
		Iterator<Path> it = path.iterator();
		while(it.hasNext()) {
			Path sub = it.next();
			if(sub.toString().startsWith(".")) {
				throw new RuntimeException("unsafe path: " + path.toString());
			}
		}
		if(Files.isSymbolicLink(path)) {
			throw new RuntimeException("is link");
		}
	}
	
	public static void checkID(String id) {
		if(id.isEmpty() || id.contains("/") || id.contains("\\") || id.contains("..")) {
			throw new RuntimeException("unsafe ID");
		}
	}
	
	public static void checkStrictID(String id) {
		if(!id.chars().allMatch(c-> ('0'<=c && c<='9') || ('a'<=c && c<='z') || ('A'<=c && c<='Z')  || c=='_')) {
			throw new RuntimeException("ID with not allowed chars. Allowed chars: 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_");
		}
	}
	
	public static void safeDeleteIfExists(Path root, Path path) throws IOException {
		checkIsParent(root, path);
		Files.deleteIfExists(path);
	}

	public static void checkIsParent(Path root, Path path) {
		checkSafePath(root);
		checkSafePath(path);
		Path rel = root.normalize().relativize(path.normalize());
		checkSafePath(rel);
		if(rel.getNameCount() == 0 || (rel.getNameCount() == 1 && rel.endsWith(""))) {
			throw new RuntimeException("is no sub");
		}
		//log.info(root + "   " + path + "   " + rel);
		//log.info(root.normalize() + "   " + path.normalize() + "   " + rel.normalize());
		//log.info(rel.getNameCount() + "|"+rel.toString()+"|");
		//log.info(rel.getNameCount() == 1 && rel.endsWith(""));
		
	}
	
	public static Set<String> of(String... values) {
		return new TreeSet<>(Arrays.asList(values));
	}
	
	public static Set<String> of(Set<String> src, String... values) {
		TreeSet<String> set = new TreeSet<String>();
		set.addAll(src);
		set.addAll(new ReadonlyArray<String>(values));
		return set;
	}
	
	public static void checkProps(Set<String> propsMandatory, Set<String> props, JSONObject json) {
		Set<String> set = json.keySet();
		if(!props.containsAll(set)) {
			throw new RuntimeException("some unknown keys");
		}
		if(!set.containsAll(propsMandatory)) {
			throw new RuntimeException("some missing keys");
		}
		
	}

}
