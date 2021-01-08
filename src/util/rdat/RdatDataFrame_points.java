package util.rdat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pointdb.base.GeoPoint;
import util.Receiver;
import util.collections.vec.Vec;
import util.rdat.RdatDataFrame.Column;
import util.rdat.RdatDataFrame.DoubleColumn;
import util.rdat.RdatDataFrame.UInt16Column;
import util.rdat.RdatDataFrame.UInt8Column;

public class RdatDataFrame_points {
	private static final Logger log = LogManager.getLogger();

	private static final Vec<Column<GeoPoint>> columnsList = new Vec<Column<GeoPoint>>(){{
		add(new DoubleColumn<GeoPoint>("x", GeoPoint::getX));
		add(new DoubleColumn<GeoPoint>("y", GeoPoint::getY));
		add(new DoubleColumn<GeoPoint>("z", GeoPoint::getZ));
		add(new UInt16Column<GeoPoint>("intensity", GeoPoint::getIntensity));
		add(new UInt8Column<GeoPoint>("returnNumber", GeoPoint::getReturnNumber));
		add(new UInt8Column<GeoPoint>("returns", GeoPoint::getReturns));
		add(new UInt8Column<GeoPoint>("scanAngleRank", GeoPoint::getScanAngleRank));
		add(new UInt8Column<GeoPoint>("classification", GeoPoint::getClassification));
		add(new UInt8Column<GeoPoint>("classificationFlags", GeoPoint::getClassificationFlags));
	}};	

	private static Map<String, Column<GeoPoint>> columnsMap = new TreeMap<String, Column<GeoPoint>>(String.CASE_INSENSITIVE_ORDER){{
		columnsList.forEach(col->put(col.name, col));
	}};

	public static String[] columnTextToColumns(String columnText) {
		return Arrays.stream(columnText.split(",")).map(s->s.trim()).toArray(String[]::new);
	}

    /**
     * 
     * @param receiver
     * @param coll
     * @param columns nullable
     * @param pointdb
     * @throws IOException
     */
	public static void write(Receiver receiver, Collection<GeoPoint> coll, String[] columns, String proj4) throws IOException {

		RdatDataFrame<GeoPoint> df = new RdatDataFrame<GeoPoint>(Collection::size);
		df.meta.put("proj4", proj4);

		if(columns==null||columns.length==0) {
			df.addAll(columnsList);
		} else {
			log.info("col "+Arrays.toString(columns));
			for(String c:columns) {
				String key = c.trim();
				if(key.isEmpty()) {
					continue;
				}
				Column<GeoPoint> col = columnsMap.get(key);
				if(col!=null) {
					df.add(col);
				} else {
					throw new RuntimeException("column unknown "+key);
				}
			}
		}

		df.write(receiver, coll);
	}

}
