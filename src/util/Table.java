package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.tinylog.Logger;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import util.collections.vec.Vec;

/**
 * Helper class to read csv files and get data as a table
 * @author woellauer
 *
 */
public class Table {

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
	
	public static class ColumnReaderStringOptional extends ColumnReaderString {
		private final String missing;
		public ColumnReaderStringOptional(int rowIndex, String missing) {
			super(rowIndex);
			this.missing = missing;
		}
		public String get(String[] row) {
			if(row.length <= rowIndex) {
				return missing;
			}
			return row[rowIndex];
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
						Logger.warn("empty");
					}
					return Float.NaN;
				}
				return Float.parseFloat(row[rowIndex]);
			} catch(NumberFormatException e) {
				if(row[rowIndex].toLowerCase().equals("na")||row[rowIndex].toLowerCase().equals("null")||row[rowIndex].toLowerCase().equals("nan")) {
					return Float.NaN;
				} else {
					Logger.warn(row[rowIndex]+" not parsed");
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
					Logger.warn(row[rowIndex]+" not parsed");
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
					Logger.warn("boolean not parsed "+text);
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
			Logger.warn("boolean not parsed "+text);
			return missing;
		}		
	}

	public static class ColumnReaderBooleanGeneral extends ColumnReaderBoolean {
		private final boolean missing;
		public ColumnReaderBooleanGeneral(int rowIndex, boolean missing) {
			super(rowIndex);
			this.missing = missing;
		}
		@Override
		public boolean get(String[] row) {
			if(row.length <= rowIndex) {
				return missing;
			}
			String text = row[rowIndex].strip().toLowerCase();
			if(text.isEmpty()) {
				return missing;
			}
			if(text.length() == 1) {
				char c = text.charAt(0);
				switch(c) {
				case 'y':
					return true;
				case 'n':
					return false;
				case 't':
					return true;
				case 'f':
					return false;
				case '1':
					return true;
				case '0':
					return false;
				case 'x':
					return true;
				case 'o':
					return false;
				default:
					Logger.warn("boolean not parsed "+text);
					return missing;
				}
			}
			switch(text) {
			case "yes":
				return true;
			case "no":
				return false;
			case "true":
				return true;
			case "false":
				return false;
			default:
				Logger.warn("boolean not parsed "+text);
				return missing;
			}
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
		try(FileInputStream in = new FileInputStream(file)) {			
			return readCSV(in, separator);			
		} catch(Exception e) {
			Logger.error(e);
			return null;
		}
	}

	public static Table readCSV(InputStream in, char separator) {
		try(InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			return readCSV(reader, separator);			
		} catch(Exception e) {
			Logger.error(e);
			return null;
		}
	}

	public static Table readCSV(Reader reader, char separator) {
		try {
			Table table = new Table();
			try(CSVReader csvReader = buildCSVReader(reader, separator)) {
				//List<String[]> list = reader.readAll(); // very slow because of linkedlist for indexed access
				if(Table.readHeader(table, csvReader)) {
					table.readRows(csvReader);
				}
			}
			return table;
		} catch(Exception e) {
			Logger.error(e);
			return null;
		}
	}

	public static Table readCSVThrow(Reader reader, char separator) throws Exception {
		Table table = new Table();
		try(CSVReader csvReader = buildCSVReader(reader, separator)) {
			//List<String[]> list = reader.readAll(); // very slow because of linkedlist for indexed access
			if(Table.readHeader(table, csvReader)) {
				table.readRows(csvReader);
			}
		}
		return table;	 	
	}

	public static Table readCSVFirstDataRow(String filename, char separator) {
		try {
			return readCSVFirstDataRow(new FileReader(filename), separator);
		} catch(Exception e) {
			Logger.error(e);
			return null;
		}
	}

	public static Table readCSVFirstDataRow(Reader reader, char separator) {
		try {
			Table table = new Table();
			try(CSVReader csvReader = buildCSVReader(reader, separator)) {
				if(Table.readHeader(table, csvReader)) {
					String[] dataRow = csvReader.readNext();
					if(dataRow != null) {
						table.rows = new String[][] {dataRow};
					} else {
						return null;
					}	
				} else {
					return null;
				}
			}
			return table;
		} catch(Exception e) {
			Logger.error(e);
			return null;
		}
	}

	static CSVReader buildCSVReader(Reader reader, char separator) {
		CSVParser csvParser = new CSVParserBuilder().withSeparator(separator).build();
		return new CSVReaderBuilder(reader).withCSVParser(csvParser).build();
	}

	static boolean readHeader(Table table, CSVReader csvReader) throws IOException {
		String[] curRow = csvReader.readNextSilently();
		if(curRow != null) {
			String[] columnsNames = curRow;
			if(columnsNames.length>0) { // filter UTF8 BOM
				if(columnsNames[0].startsWith(UTF8_BOM)) {
					columnsNames[0] = columnsNames[0].substring(1, columnsNames[0].length());
				}
			}			
			table.updateNames(columnsNames);
			//Logger.info("names: "+Arrays.toString(table.names)+"   in "+filename);
			return true;
		} else {
			return false;
		}
	}

	private void readRows(CSVReader reader) throws IOException {
		Vec<String[]> dataRowList = readRowList(reader);				
		String[][] tabeRows = dataRowList.toArray(new String[0][]);
		this.rows = tabeRows;
	}

	public static Vec<String[]> readRowList(CSVReader reader) throws IOException {
		Vec<String[]> dataRowList = new Vec<String[]>();
		String[] curRow = reader.readNextSilently();
		while(curRow != null){
			dataRowList.add(curRow);
			curRow = reader.readNextSilently();
		}				
		return dataRowList;
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
				Logger.warn("dublicate name: "+columnNames[i]+ " replaced with "+name2);
				columnNames[i] = name2;
				map.put(columnNames[i], i);
			} else {
				map.put(columnNames[i], i);
			}
		}

		this.names = columnNames;
		this.nameMap = map;
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
				Logger.error("name not found in table: "+name);
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
		return new ColumnReaderStringOptional(columnIndex, missing);
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

	public ColumnReaderBoolean createColumnReaderBooleanGeneral(String name, boolean missing) {
		int columnIndex = getColumnIndex(name, false);
		if(columnIndex<0) {
			return new ColumnReaderBooleanMissing(missing);
		}
		return new ColumnReaderBooleanGeneral(columnIndex, missing);
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
