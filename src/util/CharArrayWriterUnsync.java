package util;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

/**
 * Writer to char array without synchronization for better performance.
 *
 */
public class CharArrayWriterUnsync extends Writer {
	//
	private static final int DEFAULT_CAPACITY = 10;

	protected char buf[];
	protected int count;

	public CharArrayWriterUnsync() {
		this(DEFAULT_CAPACITY);
	}

	public CharArrayWriterUnsync(int initialSize) {
		if (initialSize < 0) {
			throw new IllegalArgumentException("Negative initial size: " + initialSize);
		}
		buf = new char[initialSize];
	}

	public void write(int c) {
		int newcount = count + 1;
		if (newcount > buf.length) {
			buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
		}
		buf[count] = (char)c;
		count = newcount;
	}

	public void write(char c[], int off, int len) {
		if ((off < 0) || (off > c.length) || (len < 0) || ((off + len) > c.length) || ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return;
		}
		int newcount = count + len;
		if (newcount > buf.length) {
			buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
		}
		System.arraycopy(c, off, buf, count, len);
		count = newcount;
	}

	public void write(String str, int off, int len) {
		int newcount = count + len;
		if (newcount > buf.length) {
			buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
		}
		str.getChars(off, off + len, buf, count);
		count = newcount;
	}

	public void writeTo(Writer out) throws IOException {		
		//Logger.info("write " + count + "   " + new String(buf, 0, count));
		out.write(buf, 0, count);
	}

	public CharArrayWriterUnsync append(CharSequence csq) {
		String s = (csq == null ? "null" : csq.toString());
		write(s, 0, s.length());
		return this;
	}

	public CharArrayWriterUnsync append(CharSequence csq, int start, int end) {
		String s = (csq == null ? "null" : csq).subSequence(start, end).toString();
		write(s, 0, s.length());
		return this;
	}

	public CharArrayWriterUnsync append(char c) {
		write(c);
		return this;
	}

	public void reset() {
		count = 0;
	}

	public char toCharArray()[] {
		return Arrays.copyOf(buf, count);
	}

	public CharArrayReader toCharArrayReader() {
		return new CharArrayReader(buf, 0, count);
	}

	public CharArrayReaderUnsync toCharArrayReaderUnsync () {
		return new CharArrayReaderUnsync(buf, 0, count);
	}

	public int size() {
		return count;
	}

	public String toString() {
		return new String(buf, 0, count);
	}

	public void flush() { }

	public void close() { }
}