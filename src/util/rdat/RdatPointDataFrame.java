package util.rdat;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jetty.server.Response;
import org.mapdb.DataIO.DataOutputByteArray;

import pointdb.PointDB;
import pointdb.base.GeoPoint;
import util.Receiver;

public class RdatPointDataFrame {

	private interface ColumnWriter {
		void write(DataOutput dataOutput, Collection<GeoPoint> points) throws IOException;
	}

	@SuppressWarnings("serial")
	private static Map<String, ColumnWriter> columnsMap = new LinkedHashMap<String,ColumnWriter>(){{
		put("x", RdatPointDataFrame::writeX);
		put("y", RdatPointDataFrame::writeY);
		put("z", RdatPointDataFrame::writeZ);
		put("intensity", RdatPointDataFrame::writeIntensity);
		put("returnNumber", RdatPointDataFrame::writeReturnNumber);
		put("returns", RdatPointDataFrame::writeReturns);
		put("scanAngleRank", RdatPointDataFrame::writeScanAngleRank);
		put("classification)", RdatPointDataFrame::writeClassification);
		put("classificationFlags)", RdatPointDataFrame::writeClassificationFlags);
	}};	
	
	
	public static void writePointList(Receiver receiver, Collection<GeoPoint> points, String[] columns, String proj4) throws IOException {
		RdatDataFrame_points.write(receiver, points, columns, proj4);
	}

	@Deprecated
	public static void writePointList2(PointDB pointdb, Response response, Collection<GeoPoint> points, String[] columns) throws IOException {
		
		ArrayList<ColumnWriter> columnWriterList = new ArrayList<ColumnWriter>();
		
		if(columns==null) {
			columnWriterList.addAll(columnsMap.values());
		} else {
			for(String c:columns) {
				ColumnWriter writer = columnsMap.get(c);
				if(writer==null) {
					for(Entry<String, ColumnWriter> e:columnsMap.entrySet()) {
						if(String.CASE_INSENSITIVE_ORDER.compare(e.getKey(), c)==0) {
							writer = e.getValue();
							break;
						}
					}
				}
				
				if(writer!=null) {
					columnWriterList.add(writer);
				} else {
					throw new RuntimeException("column unknown "+c);
				}
			}
		}
		
		if(columnWriterList.isEmpty()) {
			throw new RuntimeException("no columns");
		}

		DataOutputByteArray out = new DataOutputByteArray();
		write_RDAT_POINT_DATA_FRAME(out, points, columnWriterList, pointdb.config.getProj4());		
		response.setContentType("application/octet-stream");
		response.getOutputStream().write(out.buf,0,out.pos);		
	}

	private static void write_RDAT_POINT_DATA_FRAME(DataOutput out, Collection<GeoPoint> points, ArrayList<ColumnWriter> columnWriterList, String proj4) throws IOException {
		out.write(Rdat.SIGNATURE_RDAT);
		out.write(Rdat.RDAT_TYPE_POINT_DATA_FRAME);
		RdatList list = new RdatList();
		list.addString("proj4", proj4);
		list.write(out);
		out.write(Rdat.SIGNATURE_DTFM);
		out.writeInt(points.size());
		out.writeInt(columnWriterList.size());		
		for(ColumnWriter columnWriter:columnWriterList) {
			columnWriter.write(out, points);
		}		
	}

	private static void writeX(DataOutput dataOutput, Collection<GeoPoint> points) throws IOException {
		Rdat.writeSizedString(dataOutput, "x");
		dataOutput.writeByte(Rdat.TYPE_FLOAT64);
		dataOutput.writeByte(Rdat.TYPE_FLOAT64_SIZE);
		for(GeoPoint p:points) {					
			dataOutput.writeDouble(p.x);
		}
	}

	private static void writeY(DataOutput dataOutput, Collection<GeoPoint> points) throws IOException {
		Rdat.writeSizedString(dataOutput, "y");
		dataOutput.writeByte(Rdat.TYPE_FLOAT64);
		dataOutput.writeByte(Rdat.TYPE_FLOAT64_SIZE);
		for(GeoPoint p:points) {					
			dataOutput.writeDouble(p.y);
		}
	}

	private static void writeZ(DataOutput dataOutput, Collection<GeoPoint> points) throws IOException {
		Rdat.writeSizedString(dataOutput, "z");
		dataOutput.writeByte(Rdat.TYPE_FLOAT64);
		dataOutput.writeByte(Rdat.TYPE_FLOAT64_SIZE);
		for(GeoPoint p:points) {					
			dataOutput.writeDouble(p.z);
		}
	}

	private static void writeIntensity(DataOutput dataOutput, Collection<GeoPoint> points) throws IOException {
		Rdat.writeSizedString(dataOutput, "intensity");
		dataOutput.writeByte(Rdat.TYPE_UINT16);
		dataOutput.writeByte(Rdat.TYPE_UINT16_SIZE);
		for(GeoPoint p:points) {					
			dataOutput.writeChar(p.intensity);
		}
	}

	private static void writeReturnNumber(DataOutput dataOutput, Collection<GeoPoint> points) throws IOException {
		Rdat.writeSizedString(dataOutput, "returnNumber");
		dataOutput.writeByte(Rdat.TYPE_UINT8);
		dataOutput.writeByte(Rdat.TYPE_UINT8_SIZE);
		for(GeoPoint p:points) {					
			dataOutput.writeByte(p.returnNumber);
		}
	}

	private static void writeReturns(DataOutput dataOutput, Collection<GeoPoint> points) throws IOException {
		Rdat.writeSizedString(dataOutput, "returns");
		dataOutput.writeByte(Rdat.TYPE_UINT8);
		dataOutput.writeByte(Rdat.TYPE_UINT8_SIZE);
		for(GeoPoint p:points) {					
			dataOutput.writeByte(p.returns);
		}
	}

	private static void writeScanAngleRank(DataOutput dataOutput, Collection<GeoPoint> points) throws IOException {
		Rdat.writeSizedString(dataOutput, "scanAngleRank");
		dataOutput.writeByte(Rdat.TYPE_INT8);
		dataOutput.writeByte(Rdat.TYPE_INT8_SIZE);
		for(GeoPoint p:points) {					
			dataOutput.writeByte(p.scanAngleRank);
		}
	}
	
	private static void writeClassification(DataOutput dataOutput, Collection<GeoPoint> points) throws IOException {
		Rdat.writeSizedString(dataOutput, "classification");
		dataOutput.writeByte(Rdat.TYPE_UINT8);
		dataOutput.writeByte(Rdat.TYPE_UINT8_SIZE);
		for(GeoPoint p:points) {					
			dataOutput.writeByte(p.classification);
		}
	}
	
	private static void writeClassificationFlags(DataOutput dataOutput, Collection<GeoPoint> points) throws IOException {
		Rdat.writeSizedString(dataOutput, "classificationFlags");
		dataOutput.writeByte(Rdat.TYPE_UINT8);
		dataOutput.writeByte(Rdat.TYPE_UINT8_SIZE);
		for(GeoPoint p:points) {					
			dataOutput.writeByte(p.classificationFlags);
		}
	}
}
