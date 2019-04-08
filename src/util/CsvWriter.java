package util;

import java.io.PrintWriter;
import java.util.ArrayList;

public class CsvWriter<T> {

	private final PrintWriter out;

	private ArrayList<String> names = new ArrayList<String>();
	private ArrayList<IntGetter<T>> getters = new ArrayList<IntGetter<T>>();

	public static interface IntGetter<T> {
		int get(T e);
	}

	public CsvWriter(PrintWriter out) {
		this.out = out;
	}

	public void addInt(String name, IntGetter<T> getter) {
		names.add(name);
		getters.add(getter);
	}

	public void writeHeaderRow() {
		StringBuilder s = new StringBuilder();
		boolean more=false;
		for(String name:names) {
			if(more) {
				s.append(',');				
			} else {
				more = true;
			}
			s.append(name);
		}
		s.append('\n');
		out.append(s);
	}

	public void writeRow(T e) {
		StringBuilder s = new StringBuilder();
		boolean more=false;
		for(IntGetter<T> getter:getters) {
			if(more) {
				s.append(',');				
			} else {
				more = true;
			}
			s.append(getter.get(e));
		}
		s.append('\n');
		out.append(s);
	}

}
