package util.tiff;

import java.io.DataOutput;
import java.io.IOException;

public class DataOutputNull implements DataOutput {
	
	public static final DataOutputNull DEFAULT = new DataOutputNull();

	@Override
	public void write(int b) throws IOException {}

	@Override
	public void write(byte[] b) throws IOException {}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {}

	@Override
	public void writeBoolean(boolean v) throws IOException {}

	@Override
	public void writeByte(int v) throws IOException {}

	@Override
	public void writeShort(int v) throws IOException {}

	@Override
	public void writeChar(int v) throws IOException {}

	@Override
	public void writeInt(int v) throws IOException {}

	@Override
	public void writeLong(long v) throws IOException {}

	@Override
	public void writeFloat(float v) throws IOException {}

	@Override
	public void writeDouble(double v) throws IOException {}

	@Override
	public void writeBytes(String s) throws IOException {}

	@Override
	public void writeChars(String s) throws IOException {}

	@Override
	public void writeUTF(String s) throws IOException {}

}
