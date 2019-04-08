package util;

import java.io.DataInput;

public class LittleEndianDataInput implements DataInput {
	 public byte[] buf;
	 public int pos;

     public LittleEndianDataInput(byte[] b) {
         this(b, 0);
     }

     public LittleEndianDataInput(byte[] b, int pos) {
         buf = b;
         this.pos = pos;
     }

	@Override
	public void readFully(byte[] b) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void readFully(byte[] b, int off, int len) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public int skipBytes(int n) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public boolean readBoolean() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public byte readByte() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public int readUnsignedByte() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public short readShort() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public int readUnsignedShort() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public char readChar() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public int readInt() {
		byte[] b = buf;
		int p = pos;		
		int result = (b[p] & 0xFF) | ((b[p+1] & 0xFF)<<8) | ((b[p+2] & 0xFF)<<16) | (b[p+3]<<24);
		pos = p+4;
		return result;
	}

	@Override
	public long readLong() {
		byte[] b = buf;
		/*int p = pos;		
		long result = ((long)b[p++]&0xFF)
				|(((long)b[p++]&0xFF)<<8)
				|(((long)b[p++]&0xFF)<<16)
				|(((long)b[p++]&0xFF)<<24)
				|(((long)b[p++]&0xFF)<<32)
				|(((long)b[p++]&0xFF)<<40)
				|(((long)b[p++]&0xFF)<<48)
				|(((long)b[p++])<<56);
		pos = p;*/
		int p=pos+7;
		long result =				
				(((long)b[p--]&0xFF)<<56)
				|(((long)b[p--]&0xFF)<<48)
				|(((long)b[p--]&0xFF)<<40)
				|(((long)b[p--]&0xFF)<<32)
				|((b[p--]&0xFF)<<24)
				|((b[p--]&0xFF)<<16)
				|((b[p--]&0xFF)<<8)
				|(b[p]&0xFF);
		pos = p+8;
		return result;
	}

	@Override
	public float readFloat() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public double readDouble() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public String readLine() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public String readUTF() {
		throw new RuntimeException("not implemented");
	}

}
