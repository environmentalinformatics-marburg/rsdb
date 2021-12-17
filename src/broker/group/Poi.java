package broker.group;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


import org.tinylog.Logger;

import com.opencsv.CSVReader;

import util.Util;

public class Poi {
	

	private static final Charset UTF8 = Charset.forName("UTF-8");

	public final String name;
	public final double x;
	public final double y;

	public Poi(String name, double x, double y) {
		this.name = name;
		this.x = x;
		this.y = y;
	}

	public static Poi[] readPoiCsv(String filename) throws IOException {
		InputStreamReader in = new InputStreamReader(new FileInputStream(filename),UTF8);
		CSVReader reader = new CSVReader(in);
		try {
			List<String[]> lines = reader.readAll();
			Iterator<String[]> it = lines.iterator();
			String[] header = it.next();
			if(header.length!=3 || !header[0].equals("name") || !header[1].equals("x") || !header[2].equals("y")) {
				throw new RuntimeException("wrong header in csv file "+filename);
			}
			ArrayList<Poi> poiList = new ArrayList<Poi>(lines.size());
			while(it.hasNext()) {
				String[] row = it.next();
				String id = row[0];
				if(Util.isValidIdentifier(id)) {
					poiList.add(new Poi(id, Double.parseDouble(row[1]), Double.parseDouble(row[2])));
				} else {
					Logger.warn("POI not inserted: invalid identifier: "+id+" at "+Arrays.toString(row)+" in "+filename);
				}

			}
			poiList.sort((a,b)->String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name));
			return poiList.toArray(new Poi[0]);
		} finally {
			reader.close();
		}
	}

}
