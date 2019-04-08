package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.opencsv.CSVReader;

/**
 * Helper class to read csv files and get data as a table
 * @author woellauer
 *
 */
public class Table {
	private static final Logger log = LogManager.getLogger();

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final String UTF8_BOM = "\uFEFF";

	public static class ColumnReader {
		public static final int MISSING_COLUMN = Integer.MAX_VALUE;
		public final int rowIndex;
		public ColumnReader(int rowIndex) {
			this.rowIndex = rowIndex;
		}
	}

	public static class ColumnReaderString extends ColumnReader {
		public ColumnReaderString(int rowIndex) {
			super(rowIndex);
		}
		public String get(String[] row) {
			return row[rowIndex];
		}
		public ColumnReaderString then(UnaryOperator<String> func) {
			ColumnReaderString outher = this;
			return new ColumnReaderString(rowIndex) {				
				@Override
				public String get(String[] row) {
					return func.apply(outher.get(row));
				}				
			};
		}
	}
	
	public static class ColumnReaderStringMissing extends ColumnReaderString {
		private String missing;
		public ColumnReaderStringMissing(String missing) {
			super(MISSING_COLUMN);
			this.missing = missing;
		}
		@Override
		public String get(String[] row) {
			return missing;
		}		
	}

	public static class ColumnReaderFloat extends ColumnReader {
		public ColumnReaderFloat(int rowIndex) {
			super(rowIndex);
		}
		public float get(String[] row, boolean warnIfEmpty) {			
			try {
				String textValue = row[rowIndex];
				if(textValue.isEmpty()) {
					if(warnIfEmpty) {
						log.warn("empty");
					}
					return Float.NaN;
				}
				return Float.parseFloat(row[rowIndex]);
			} catch(NumberFormatException e) {
				if(row[rowIndex].toLowerCase().equals("na")||row[rowIndex].toLowerCase().equals("null")||row[rowIndex].toLowerCase().equals("nan")) {
					return Float.NaN;
				} else {
					log.warn(row[rowIndex]+" not parsed");
					e.printStackTrace();
					return Float.NaN;
				}
			}
		}
		public ColumnReaderFloat then(UnaryOperator<Float> func) {
			ColumnReaderFloat outher = this;
			return new ColumnReaderFloat(rowIndex) {				
				@Override
				public float get(String[] row, boolean warnIfEmpty) {	
					return func.apply(outher.get(row, warnIfEmpty));
				}				
			};
		}
	}

	public static class ColumnReaderFloatMissing extends ColumnReaderFloat {
		private float missing;
		public ColumnReaderFloatMissing(float missing) {
			super(MISSING_COLUMN);
			this.missing = missing;
		}
		@Override
		public float get(String[] row, boolean warnIfEmpty) {
			return missing;
		}		
	}

	public static class ColumnReaderDouble extends ColumnReader {
		public ColumnReaderDouble(int rowIndex) {
			super(rowIndex);
		}
		public double get(String[] row, boolean warnIfEmpty) {			
			try {
				String textValue = row[rowIndex];
				if(!warnIfEmpty&&textValue.isEmpty()) {
					return Double.NaN;
				}
				return Double.parseDouble(row[rowIndex]);
			} catch(NumberFormatException e) {
				if(row[rowIndex].toLowerCase().equals("na")||row[rowIndex].toLowerCase().equals("null")) {
					return Double.NaN;
				} else {
					log.warn(row[rowIndex]+" not parsed");
					e.printStackTrace();
					return Double.NaN;
				}
			}
		}
	}

	public static class ColumnReaderInt extends ColumnReader {
		public ColumnReaderInt(int rowIndex) {
			super(rowIndex);
		}
		public int get(String[] row) {
			return Integer.parseInt(row[rowIndex]);
		}
	}

	public static class ColumnReaderIntFunc extends ColumnReader {
		private final IntegerParser parser;
		public ColumnReaderIntFunc(int rowIndex, IntegerParser parser) {
			super(rowIndex);
			this.parser = parser;
		}
		public int get(String[] row) {
			return parser.parse(row[rowIndex]);
		}
		public interface IntegerParser {
			int parse(String text);
		}
	}
	
	
	public static abstract class ColumnReaderBoolean extends ColumnReader {
		public ColumnReaderBoolean(int rowIndex) {
			super(rowIndex);
		}
		public abstract boolean get(String[] row);
	}
	
	public static class ColumnReaderBooleanMissing extends ColumnReaderBoolean {
		private final boolean missing;
		public ColumnReaderBooleanMissing(boolean missing) {
			super(MISSING_COLUMN);
			this.missing = missing;
		}
		@Override
		public boolean get(String[] row) {
			return missing;
		}		
	}
	
	public static class ColumnReaderBooleanYN extends ColumnReaderBoolean {
		private final boolean missing;
		public ColumnReaderBooleanYN(int rowIndex, boolean missing) {
			super(rowIndex);
			this.missing = missing;
		}
		@Override
		public boolean get(String[] row) {
			String text = row[rowIndex];
			if(text.length()!=1) {
				text = text.trim();
				if(text.length()!=1) {
					log.warn("boolean not parsed "+text);
					return missing;
				}
			}
			char c = text.toUpperCase().charAt(0);
			if(c=='Y') {
				return true;
			}
			if(c=='N') {
				return false;
			}
			log.warn("boolean not parsed "+text);
			return missing;
		}		
	}

	public static interface ColumnReaderTimestamp {
		public long get(String[] row);
	}

	/**
	 * header names in csv file
	 */
	public String[] names;

	/**
	 * header name -> column position
	 */
	public Map<String, Integer> nameMap;

	/**
	 * table rows of csv file
	 */
	public String[][] rows;

	protected Table() {}

	public static Table readCSV(Path filename, char separator) {
		return readCSV(filename.toFile(),separator);
	}
	
	public static Table readCSV(String filename, char separator) {
		return readCSV(new File(filename), separator);
	}

	/**
	 * create a Table Object from CSV-File
	 * @param filename
	 * @return
	 */
	public static Table readCSV(File file, char separator) {
		try {
			Table table = new Table();

			//CSVReader reader = new CSVReader(new FileReader(filename),separator);
			InputStreamReader in = new InputStreamReader(new FileInputStream(file),UTF8);
			CSVReader reader = new CSVReader(in,separator);

			List<String[]> list = reader.readAll();
			
			reader.close();
			
			String[] columnsNames = list.get(0);
			if(columnsNames.length>0) { // filter UTF8 BOM
				if(columnsNames[0].startsWith(UTF8_BOM)) {
					columnsNames[0] = columnsNames[0].substring(1, columnsNames[0].length());
				}
			}			
			table.updateNames(columnsNames);			

			//log.info("names: "+Arrays.toString(table.names)+"   in "+filename);

			table.rows = new String[list.size()-1][];

			for(int i=1;i<list.size();i++) {
				table.rows[i-1] = list.get(i);
			}

			return table;
		} catch(Exception e) {
			log.error(e);
			return null;
		}
	}
	
	public void updateNames(String[] columnNames) {
		HashMap<String, Integer> map = new HashMap<String, Integer>();

		for(int i=0;i<columnNames.length;i++) {
			if(map.containsKey(columnNames[i])) {
				int nameNumber = 2;
				String name2 = columnNames[i]+nameNumber;
				while(map.containsKey(name2)) {
					nameNumber++;
					name2 = columnNames[i]+nameNumber;
				}
				log.warn("dublicate name: "+columnNames[i]+ " replaced with "+name2);
				columnNames[i] = name2;
				map.put(columnNames[i], i);
			} else {
				map.put(columnNames[i], i);
			}
		}
		
		this.names = columnNames;
		this.nameMap = map;
	}

	public static Table readCSVFirstDataRow(String filename, char separator) {
		try {
			Table table = new Table();

			CSVReader reader = new CSVReader(new FileReader(filename),separator);

			String[] headerRow = reader.readNext();
			String[] dataRow = reader.readNext();

			reader.close();
			
			table.updateNames(headerRow);

			table.rows = new String[1][];
			table.rows[0] = dataRow;			

			return table;
		} catch(Exception e) {
			log.error(e);
			return null;
		}
	}

	/**
	 * get column position of one header name
	 * @param name
	 * @return if name not found -1
	 */
	public int getColumnIndex(String name) {
		return getColumnIndex(name, true);
	}

	/**
	 * get column position of one header name
	 * @param name
	 * @return if name not found -1
	 */
	public int getColumnIndex(String name, boolean warn) {
		Integer index = nameMap.get(name);
		if(index==null) {			
			if(warn) {
				log.error("name not found in table: "+name);
			}
			return -1;
		}
		return index;
	}

	public boolean containsColumn(String name) {
		return nameMap.containsKey(name);
	}

	public ColumnReaderString createColumnReader(String name) {
		int columnIndex = getColumnIndex(name);
		if(columnIndex<0) {
			return null;
		}
		return new ColumnReaderString(columnIndex);
	}
	
	public static interface ReaderConstructor<T> {
		T create(int a);
	}
	
	public <T> T getColumnReader(String name, ReaderConstructor<T> readerConstructor) {
		int columnIndex = getColumnIndex(name);
		if(columnIndex<0) {
			return null;
		}
		return readerConstructor.create(columnIndex);
	}
	
	
	public ColumnReaderString createColumnReader(String name, String missing) {
		int columnIndex = getColumnIndex(name, false);
		if(columnIndex<0) {
			return new ColumnReaderStringMissing(missing);
		}
		return new ColumnReaderString(columnIndex);
	}

	public ColumnReaderFloat createColumnReaderFloat(String name) {
		int columnIndex = getColumnIndex(name);
		if(columnIndex<0) {
			return null;
		}
		return new ColumnReaderFloat(columnIndex);
	}

	/**
	 * Creates reader of column or producer of value "missing" if columns does not exist.
	 * @param name
	 * @param missing
	 * @return
	 */
	public ColumnReaderFloat createColumnReaderFloat(String name, float missing) {
		int columnIndex = getColumnIndex(name, false);
		if(columnIndex<0) {
			return new ColumnReaderFloatMissing(missing);
		}
		return new ColumnReaderFloat(columnIndex);
	}

	public ColumnReaderDouble createColumnReaderDouble(String name) {
		int columnIndex = getColumnIndex(name);
		if(columnIndex<0) {
			return null;
		}
		return new ColumnReaderDouble(columnIndex);
	}

	public ColumnReaderInt createColumnReaderInt(String name) {
		int columnIndex = getColumnIndex(name);
		if(columnIndex<0) {
			return null;
		}
		return new ColumnReaderInt(columnIndex);
	}

	public ColumnReaderBoolean createColumnReaderBooleanYN(String name, boolean missing) {
		int columnIndex = getColumnIndex(name, false);
		if(columnIndex<0) {
			return new ColumnReaderBooleanMissing(missing);
		}
		return new ColumnReaderBooleanYN(columnIndex, missing);
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for(String name:names) {
			s.append(name);
			s.append(' ');
		}
		s.append('\n');
		for(String[] row:rows) {
			for(String cell:row) {
				s.append(cell);
				s.append(' ');
			}
			s.append('\n');
		}
		return s.toString();
	}
	
	public String getName(ColumnReader cr) {
		return names[cr.rowIndex];
	}
}
