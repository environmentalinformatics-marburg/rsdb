package util.rdat;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

import javax.servlet.http.HttpServletResponse;

import org.mapdb.DataIO.DataOutputByteArray;

import util.collections.vec.Vec;

public class RdatDataFrame<T> {

	public static abstract class Column<T> {
		protected final String name;
		public Column(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
		public abstract void write(DataOutput out, Iterable<T> coll) throws IOException;
	}

	public static class IntColumn<T> extends Column<T> {		

		private final ToIntFunction<T> mapper;

		public IntColumn(String name, ToIntFunction<T> mapper) {
			super(name);
			this.mapper = mapper;
		}

		@Override
		public void write(DataOutput out, Iterable<T> coll) throws IOException {			
			Rdat.writeSizedString(out, name);
			out.writeByte(Rdat.TYPE_INT32);
			out.writeByte(Rdat.TYPE_INT32_SIZE);
			for (T e:coll) {
				int v = mapper.applyAsInt(e);
				out.writeInt(v);
			}		
		}

	}

	public static class UInt16Column<T> extends Column<T> {
		@FunctionalInterface
		public static interface ToCharFunction<T> {
			char applyAsChar(T value);
		}

		private final ToCharFunction<T> mapper;

		public UInt16Column(String name, ToCharFunction<T> mapper) {
			super(name);
			this.mapper = mapper;
		}

		@Override
		public void write(DataOutput out, Iterable<T> coll) throws IOException {			
			Rdat.writeSizedString(out, name);
			out.writeByte(Rdat.TYPE_UINT16);
			out.writeByte(Rdat.TYPE_UINT16_SIZE);
			for (T e:coll) {
				char v = mapper.applyAsChar(e);
				out.writeChar(v);
			}		
		}

	}

	public static class UInt8Column<T> extends Column<T> {
		@FunctionalInterface
		public static interface ToByteFunction<T> {
			byte applyAsByte(T value);
		}

		private final ToByteFunction<T> mapper;

		public UInt8Column(String name, ToByteFunction<T> mapper) {
			super(name);
			this.mapper = mapper;
		}

		@Override
		public void write(DataOutput out, Iterable<T> coll) throws IOException {			
			Rdat.writeSizedString(out, name);
			out.writeByte(Rdat.TYPE_UINT8);
			out.writeByte(Rdat.TYPE_UINT8_SIZE);
			for (T e:coll) {
				byte v = mapper.applyAsByte(e);
				out.writeByte(v);
			}		
		}

	}

	public static class DoubleColumn<T> extends Column<T> {

		private final ToDoubleFunction<T> mapper;

		public DoubleColumn(String name, ToDoubleFunction<T> mapper) {
			super(name);
			this.mapper = mapper;
		}

		@Override
		public void write(DataOutput out, Iterable<T> coll) throws IOException {			
			Rdat.writeSizedString(out, name);
			out.writeByte(Rdat.TYPE_FLOAT64);
			out.writeByte(Rdat.TYPE_FLOAT64_SIZE);
			for (T e:coll) {
				double v = mapper.applyAsDouble(e);
				out.writeDouble(v);
			}		
		}		
	}

	public static class StringColumn<T> extends Column<T> {

		private final Function<T, String> mapper;

		public StringColumn(String name, Function<T, String> mapper) {
			super(name);
			this.mapper = mapper;
		}

		@Override
		public void write(DataOutput out, Iterable<T> coll) throws IOException {
			Rdat.writeSizedString(out, name);
			out.writeByte(Rdat.TYPE_STRING);
			out.writeByte(Rdat.TYPE_STRING_SIZE);
			for (T e:coll) {
				String s = mapper.apply(e);
				Rdat.writeSizedString(out, s);
			}	
		}

	}

	public final Map<String, Object> meta = new TreeMap<String, Object>();
	private Vec<Column<T>> list = new Vec<>();
	private final Function<Collection<T>, Integer> rowCalculator;

	public RdatDataFrame(Function<Collection<T>, Integer> rowCalculator) {
		this.rowCalculator = rowCalculator;
	}

	public void add(Column<T> col) {
		list.add(col);
	}

	public void addAll(Iterable<Column<T>> coll) {
		for(Column<T> col:coll) {
			list.add(col);
		}		
	}

	public void addInt(String name, ToIntFunction<T> mapper) {
		add(new IntColumn<T>(name, mapper));
	}

	public void addDouble(String name, ToDoubleFunction<T> mapper) {
		add(new DoubleColumn<T>(name, mapper));		
	}

	public void addString(String name, Function<T, String> mapper) {
		add(new StringColumn<T>(name, mapper));		
	}

	public void write(HttpServletResponse response, T... array) throws IOException {
		write(response, Arrays.asList(array));
	}

	public void write(HttpServletResponse response, Collection<T> coll) throws IOException {
		DataOutputByteArray out = new DataOutputByteArray();
		write(out, coll);	
		response.setContentType("application/octet-stream");
		response.getOutputStream().write(out.buf,0,out.pos);
	}

	public void write(DataOutput out, Collection<T> coll) throws IOException {
		out.write(Rdat.SIGNATURE_RDAT);
		out.write(Rdat.RDAT_TYPE_POINT_DATA_FRAME);

		RdatList metaList = new RdatList();
		metaList.addAll(meta);
		metaList.write(out);

		out.write(Rdat.SIGNATURE_DTFM);
		out.writeInt(rowCalculator.apply(coll));
		out.writeInt(list.size());	
		for (Column<T> col:list) {
			col.write(out, coll);
		}	
	}

}
