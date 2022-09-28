package pointdb.las;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;


import org.tinylog.Logger;

import com.github.mreutegg.laszip4j.LASHeader;
import com.github.mreutegg.laszip4j.LASPoint;
import com.github.mreutegg.laszip4j.LASReader;
import com.github.mreutegg.laszip4j.laslib.LASheader;
import com.github.mreutegg.laszip4j.laslib.LASreadOpener;
import com.github.mreutegg.laszip4j.laslib.LASreader;
import com.github.mreutegg.laszip4j.laslib.LASvlr_key_entry;
import com.github.mreutegg.laszip4j.laszip.LASpoint;

import pointcloud.CellTable;
import pointdb.base.Point;

public class Laz {
	

	public final Path filename;
	public final LASReader reader;
	public final LASHeader header;

	public final double[] scale_factor;
	public final double[] offset;
	public final double[] min;
	public final double[] max;
	public final long number_of_point_records;
	public final int point_Data_Record_Length;

	private Iterator<LASPoint> lasPointIterator;
	private long lasPointIteratorPos;

	private LASreader internalReader;
	private long internalReaderPos;

	public Laz(Path filename) {
		this.filename = filename;		
		this.reader = new LASReader(filename.toFile());
		this.header = reader.getHeader();

		scale_factor = new double[] {header.getXScaleFactor(), header.getYScaleFactor(), header.getZScaleFactor()};
		offset = new double[] {header.getXOffset(), header.getYOffset(), header.getZOffset()};
		min = new double[] {header.getMinX(), header.getMinY(), header.getMinZ()};
		max = new double[] {header.getMaxX(), header.getMaxY(), header.getMaxZ()};
		number_of_point_records = getSafeNumberOfPointRecords();
		point_Data_Record_Length = header.getPointDataRecordLength();
	}

	private long getSafeNumberOfPointRecords() {
		long recordCount = header.getNumberOfPointRecords();
		if(recordCount == 0) {
			recordCount = header.getLegacyNumberOfPointRecords();
		}
		if(recordCount <= 0) {
			throw new RuntimeException("error in LAZ: no record count");
		}
		return recordCount;
	}

	@Override
	public String toString() {
		return "laz";
	}

	public Point[] read(long record_start, int record_count, int[] intDiffs, int[] intFactors) {
		Logger.info("laz offset "+ Arrays.toString(offset));
		Logger.info("laz offset "+ Arrays.toString(scale_factor));
		Logger.info("laz intDiffs "+ Arrays.toString(intDiffs));
		Logger.info("laz intFactors "+ Arrays.toString(intFactors));
		if(internalReader == null) {			
			internalReader = new LASreadOpener().open(filename.toFile().getAbsolutePath());
			internalReaderPos = 0;
		}
		if(record_start != internalReaderPos) {
			throw new RuntimeException("can not read not in sequence " + internalReaderPos + "  " + record_start);
		}

		int xd = intDiffs==null?0:intDiffs[0];
		int yd = intDiffs==null?0:intDiffs[1];
		int zd = intDiffs==null?0:intDiffs[2];
		int xf = intFactors==null?1:intFactors[0];
		int yf = intFactors==null?1:intFactors[1];
		int zf = intFactors==null?1:intFactors[2];

		Point[] points = new Point[record_count];
		int cnt = 0;
		LASpoint mutablePoint = internalReader.point;
		while(cnt < record_count) {
			//Logger.info("read " + cnt);
			if(!internalReader.read_point()) {
				throw new RuntimeException("not all points read "+ cnt + "  " + record_count + "    "  + number_of_point_records + " file points");
			}
			points[cnt] = mutablePointToPoint(mutablePoint, xd, yd, zd, xf, yf, zf);
			cnt++;
		}
		internalReaderPos += cnt;
		if(number_of_point_records == internalReaderPos || cnt < record_count) {
			Logger.info("close internal reader");
			internalReader.close();
			internalReader = null;
		}
		return points;
	}



	public Point[] readwithInternalIterator(long record_start, int record_count, int[] intDiffs, int[] intFactors) {
		Logger.info("laz offset "+ Arrays.toString(offset));
		Logger.info("laz offset "+ Arrays.toString(scale_factor));
		Logger.info("laz intDiffs "+ Arrays.toString(intDiffs));
		Logger.info("laz intFactors "+ Arrays.toString(intFactors));
		if(lasPointIterator == null) {
			lasPointIterator = reader.getPoints().iterator();
			lasPointIteratorPos = 0;
		}
		if(record_start != lasPointIteratorPos) {
			throw new RuntimeException("can not read not in sequence " + lasPointIteratorPos + "  " + record_start);
		}

		int xd = intDiffs==null?0:intDiffs[0];
		int yd = intDiffs==null?0:intDiffs[1];
		int zd = intDiffs==null?0:intDiffs[2];
		int xf = intFactors==null?1:intFactors[0];
		int yf = intFactors==null?1:intFactors[1];
		int zf = intFactors==null?1:intFactors[2];

		Point[] points = new Point[record_count];
		int cnt = 0;
		Iterator<LASPoint> it = lasPointIterator;
		while(it.hasNext() && cnt < record_count) {
			LASPoint lasPoint = it.next();
			points[cnt] = Laz.lasPointToPoint(lasPoint, xd, yd, zd, xf, yf, zf);
			cnt++;
		}
		if(cnt < record_count) {
			Logger.warn("not all points read "+ cnt + "  " + record_count);
		}
		lasPointIteratorPos += cnt;
		return points;
	}

	public static Point lasPointToPoint(LASPoint lasPoint) {
		int x = lasPoint.getX();
		int y = lasPoint.getY();
		int z = lasPoint.getZ();
		char intensity = lasPoint.getIntensity();
		byte returnNumber = lasPoint.getReturnNumber();
		byte returns = lasPoint.getNumberOfReturns();
		byte scanAngleRank = lasPoint.getScanAngleRank();
		byte classification = (byte) lasPoint.getClassification(); // convert to unsigned byte, range of classification is 0 to 31 and 0 to 255 for newer LAS versions
		byte classificationFlags = 0; // TODO
		return new Point(x, y, z, intensity, returnNumber, returns, scanAngleRank, classification, classificationFlags);
	}

	public static Point lasPointToPoint(LASPoint lasPoint, int xd, int yd, int zd, int xf, int yf, int zf) {
		int x = (lasPoint.getX() - xd) * xf;
		int y = (lasPoint.getY() - yd) * yf;
		int z = (lasPoint.getZ() - zd) * zf;
		char intensity = lasPoint.getIntensity();
		byte returnNumber = lasPoint.getReturnNumber();
		byte returns = lasPoint.getNumberOfReturns();
		byte scanAngleRank = lasPoint.getScanAngleRank();
		byte classification = (byte) lasPoint.getClassification(); // convert to unsigned byte, range of classification is 0 to 31 and 0 to 255 for newer LAS versions
		byte classificationFlags = 0; // TODO
		return new Point(x, y, z, intensity, returnNumber, returns, scanAngleRank, classification, classificationFlags);
	}

	private static Point mutablePointToPoint(LASpoint lasPoint, int xd, int yd, int zd, int xf, int yf, int zf) {
		int x = (lasPoint.getX() - xd) * xf;
		int y = (lasPoint.getY() - yd) * yf;
		int z = (lasPoint.getZ() - zd) * zf;
		char intensity = lasPoint.getIntensity();
		byte returnNumber = lasPoint.getReturn_number();
		byte returns = lasPoint.getNumber_of_returns();
		byte scanAngleRank = lasPoint.getScan_angle_rank();
		byte classification = (byte) lasPoint.getClassification(); // convert to unsigned byte, range of classification is 0 to 31 and 0 to 255 for newer LAS versions
		byte classificationFlags = 0; // TODO
		return new Point(x, y, z, intensity, returnNumber, returns, scanAngleRank, classification, classificationFlags);
	}

	public CellTable getRecords(long record_start, int record_count) {
		if(internalReader == null) {			
			internalReader = new LASreadOpener().open(filename.toFile().getAbsolutePath());
			internalReaderPos = 0;
		}
		if(record_start != internalReaderPos) {
			throw new RuntimeException("can not read not in sequence " + internalReaderPos + "  " + record_start);
		}

		int[] xs = new int[record_count];
		int[] ys = new int[record_count];
		int[] zs = new int[record_count];
		char[] intensity = new char[record_count];
		byte[] returnNumber = new byte[record_count];
		byte[] returns = new byte[record_count];
		BitSet scanDirectionFlag = new BitSet(record_count);
		BitSet edgeOfFlightLine = new BitSet(record_count);
		byte[] classification = new byte[record_count];
		byte[] scanAngleRank = new byte[record_count];
		char[] red = new char[record_count];
		char[] green = new char[record_count];
		char[] blue = new char[record_count];		

		int cnt = 0;
		LASpoint mutablePoint = internalReader.point;
		while(cnt < record_count) {
			//Logger.info("read point " + cnt);
			if(!internalReader.read_point()) {
				throw new RuntimeException("not all points read "+ cnt + "  " + record_count + "    "  + number_of_point_records + " file points");
			}
			xs[cnt] = mutablePoint.getX();
			ys[cnt] = mutablePoint.getY();
			zs[cnt] = mutablePoint.getZ();
			intensity[cnt] = mutablePoint.getIntensity();
			returnNumber[cnt] = mutablePoint.getReturn_number();
			returns[cnt] = mutablePoint.getNumber_of_returns();
			if(mutablePoint.getScan_direction_flag() != 0) {
				scanDirectionFlag.set(cnt);
			}
			if(mutablePoint.getEdge_of_flight_line() != 0) {
				edgeOfFlightLine.set(cnt);
			}
			classification[cnt] = (byte) mutablePoint.getClassification(); // convert to unsigned byte, range of classification is 0 to 31 and 0 to 255 for newer LAS versions
			scanAngleRank[cnt] = mutablePoint.getScan_angle_rank();
			red[cnt] = mutablePoint.get_R();
			green[cnt] = mutablePoint.get_G();
			blue[cnt] = mutablePoint.get_B();

			cnt++;
		}
		internalReaderPos += cnt;
		if(number_of_point_records == internalReaderPos || cnt < record_count) {
			Logger.info("close internal reader");
			internalReader.close();
			internalReader = null;
		}

		CellTable recordTable = new CellTable(0, 0, 0, record_count, xs, ys, zs);
		recordTable.intensity = intensity;
		recordTable.returnNumber = returnNumber;
		recordTable.returns = returns;
		recordTable.scanDirectionFlag = scanDirectionFlag;
		recordTable.edgeOfFlightLine = edgeOfFlightLine;
		recordTable.classification = classification;
		recordTable.scanAngleRank = scanAngleRank;
		recordTable.red = red;
		recordTable.green = green;
		recordTable.blue = blue;
		

		recordTable.cleanup();

		return recordTable;
	}

	public CellTable getRecordsInternalIterator(long record_start, int record_count) {
		if(lasPointIterator == null) {
			lasPointIterator = reader.getPoints().iterator();
			lasPointIteratorPos = 0;
		}
		if(record_start != lasPointIteratorPos) {
			throw new RuntimeException("can not read not in sequence " + lasPointIteratorPos + "  " + record_start);
		}

		int[] xs = new int[record_count];
		int[] ys = new int[record_count];
		int[] zs = new int[record_count];
		char[] intensity = new char[record_count];
		byte[] returnNumber = new byte[record_count];
		byte[] returns = new byte[record_count];
		BitSet scanDirectionFlag = new BitSet(record_count);
		BitSet edgeOfFlightLine = new BitSet(record_count);
		byte[] classification = new byte[record_count];
		byte[] scanAngleRank = new byte[record_count];

		int cnt = 0;
		Iterator<LASPoint> it = lasPointIterator;
		while(it.hasNext() && cnt < record_count) {
			LASPoint lasPoint = it.next();

			xs[cnt] = lasPoint.getX();
			ys[cnt] = lasPoint.getY();
			zs[cnt] = lasPoint.getZ();
			intensity[cnt] = lasPoint.getIntensity();
			returnNumber[cnt] = lasPoint.getReturnNumber();
			returns[cnt] = lasPoint.getNumberOfReturns();
			if(lasPoint.getScanDirectionFlag() != 0) {
				scanDirectionFlag.set(cnt);
			}
			if(lasPoint.getEdgeOfFlightLine() != 0) {
				edgeOfFlightLine.set(cnt);
			}
			classification[cnt] = (byte) lasPoint.getClassification(); // convert to unsigned byte, range of classification is 0 to 31 and 0 to 255 for newer LAS versions
			scanAngleRank[cnt] = lasPoint.getScanAngleRank();

			cnt++;
		}
		if(cnt < record_count) {
			Logger.warn("not all points read "+ cnt + "  " + record_count);
		}
		lasPointIteratorPos += cnt;

		CellTable recordTable = new CellTable(0, 0, 0, record_count, xs, ys, zs);
		recordTable.intensity = intensity;
		recordTable.returnNumber = returnNumber;
		recordTable.returns = returns;
		recordTable.scanDirectionFlag = scanDirectionFlag;
		recordTable.edgeOfFlightLine = edgeOfFlightLine;
		recordTable.classification = classification;
		recordTable.scanAngleRank = scanAngleRank;

		recordTable.cleanup();

		return recordTable;
	}

	public int readEPSG() {		
		LASreader r = new LASreadOpener().open(filename.toFile().getAbsolutePath());
		try {
			LASheader h = r.header;
			return readEPSGByHeader(h);
		} catch(Exception e) {
			Logger.warn(e);
			return 0;
		} finally {
			r.close();
		}
	}

	public int readEPSGByHeader(LASheader h) {
		int epsg = 0;
		for(LASvlr_key_entry e:h.vlr_geo_key_entries) {			
			switch(e.key_id) {
			case Las.ProjectedCSTypeGeoKey: {
				int epsgRaw = e.value_offset;
				epsg = Las.getEPSGfromProjectedCSTypeGeoKey(epsgRaw);
				break;
			}			
			}			
			//Logger.info("len re " + ((int)e.key_id) + "   " + ((int)e.count) + "   " + ((int)e.tiff_tag_location) + "  " + ((int) e.value_offset));
		}
		return epsg;
	}
}
