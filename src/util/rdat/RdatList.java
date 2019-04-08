package util.rdat;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

import util.collections.vec.Vec;

public class RdatList {
	//private static final Logger log = LogManager.getLogger();	
	
	Vec<RdatList.Entry> entries = new Vec<RdatList.Entry>();
	
	private static abstract class Entry {
		public final String name;
		public Entry(String name) {
			this.name = name;
		}

		abstract void write(DataOutput out) throws IOException;			
	}
	
	public void addInteger(String name, int v) {
		entries.add(new Entry(name) {				
			@Override
			public void write(DataOutput out) throws IOException {
				out.writeByte(Rdat.TYPE_INT32);
				out.writeByte(Rdat.TYPE_INT32_SIZE);
				out.writeInt(v);
			}
			@Override
			public String toString() {
				return name+"="+v;
			}		
		});
	}
	
	public void addString(String name, String v) {
		entries.add(new Entry(name) {				
			@Override
			public void write(DataOutput out) throws IOException {
				out.writeByte(Rdat.TYPE_STRING);
				Rdat.writeSizedString(out, v.length() > 255 ? v.substring(0, 255) : v);
			}
			@Override
			public String toString() {
				return name+"="+v;
			}
		});
	}
	
	public void addDouble(String name, double v) {
		entries.add(new Entry(name) {				
			@Override
			public void write(DataOutput out) throws IOException {
				out.writeByte(Rdat.TYPE_FLOAT64);
				out.writeByte(Rdat.TYPE_FLOAT64_SIZE);
				out.writeDouble(v);
			}
			@Override
			public String toString() {
				return name+"="+v;
			}
		});
	}
	
	public void addUint16(String name, int v) {
		entries.add(new Entry(name) {				
			@Override
			public void write(DataOutput out) throws IOException {
				out.writeByte(Rdat.TYPE_UINT16);
				out.writeByte(Rdat.TYPE_UINT16_SIZE);
				out.writeShort(v);
			}
			@Override
			public String toString() {
				return name+"="+v;
			}
		});
	}
	
	public void addList(String name, RdatList subList) {
		entries.add(new Entry(name) {				
			@Override
			public void write(DataOutput out) throws IOException {
				out.writeByte(Rdat.TYPE_BASIC_OBJECT);
				out.writeByte(Rdat.TYPE_BASIC_OBJECT_SIZE);				
				subList.write(out);
			}
			@Override
			public String toString() {
				return name+"="+subList;
			}
		});
	}
	
	public void write(DataOutput out) throws IOException {
		out.write(Rdat.SIGNATURE_LIST);
		int SIZE = entries.size();
		if(SIZE>255) {
			throw new RuntimeException("list too big");
		}
		out.writeByte(entries.size());
		for(RdatList.Entry e:entries) {
			Rdat.writeSizedString(out, e.name);
			e.write(out);
		}
	}

	public void addAll(Map<String, Object> map) {
		for(String name:map.keySet()) {
			Object value = map.get(name);
			if(value instanceof Integer) {
				addInteger(name, (Integer) value);
			} else if(value instanceof Double) {
				addDouble(name, (Double) value);
			} else {
				addString(name, value.toString());
			}
		}
		
	}
	
	@Override
	public String toString() {
		String s = "[";
		for(Entry entry:entries) {
			s += entry + "  ";
		}
		return s + "]";
	}
}