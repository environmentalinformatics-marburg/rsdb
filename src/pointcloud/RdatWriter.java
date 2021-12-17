package pointcloud;

import java.io.DataOutput;
import java.io.IOException;
import java.util.BitSet;
import java.util.Collection;
import java.util.function.Function;


import org.tinylog.Logger;

import util.Receiver;
import util.rdat.Rdat;
import util.rdat.RdatDataFrame;
import util.rdat.RdatDataFrame.Column;

public class RdatWriter {
	

	public static class DoublesColumn extends Column<PointTable> {
		private final Function<PointTable, double[]> mapper;

		public DoublesColumn(String name, Function<PointTable, double[]> mapper) {
			super(name);
			this.mapper = mapper;
		}

		@Override
		public void write(DataOutput out, Iterable<PointTable> coll) throws IOException {			
			Rdat.writeSizedString(out, name);
			out.writeByte(Rdat.TYPE_FLOAT64);
			out.writeByte(Rdat.TYPE_FLOAT64_SIZE);
			for (PointTable p:coll) {
				double[] v = mapper.apply(p);
				int len = p.rows;
				if(v != null) {
					for (int i = 0; i < len; i++) {
						out.writeDouble(v[i]);					
					}
				} else {
					for (int i = 0; i < len; i++) {
						out.writeDouble(0d);					
					}
				}
			}		
		}
	}
	
	public static class Uint16Column extends Column<PointTable> {
		private final Function<PointTable, char[]> mapper;

		public Uint16Column(String name, Function<PointTable, char[]> mapper) {
			super(name);
			this.mapper = mapper;
		}

		@Override
		public void write(DataOutput out, Iterable<PointTable> coll) throws IOException {			
			Rdat.writeSizedString(out, name);
			out.writeByte(Rdat.TYPE_UINT16);
			out.writeByte(Rdat.TYPE_UINT16_SIZE);
			for (PointTable p:coll) {
				char[] v = mapper.apply(p);
				int len = p.rows;
				if(v != null) {
					for (int i = 0; i < len; i++) {
						out.writeChar(v[i]);					
					}
				} else {
					for (int i = 0; i < len; i++) {
						out.writeChar(0);					
					}
				}
			}		
		}
	}
	
	public static class Uint8Column extends Column<PointTable> {
		private final Function<PointTable, byte[]> mapper;

		public Uint8Column(String name, Function<PointTable, byte[]> mapper) {
			super(name);
			this.mapper = mapper;
		}

		@Override
		public void write(DataOutput out, Iterable<PointTable> coll) throws IOException {			
			Rdat.writeSizedString(out, name);
			out.writeByte(Rdat.TYPE_UINT8);
			out.writeByte(Rdat.TYPE_UINT8_SIZE);
			for (PointTable p:coll) {
				byte[] v = mapper.apply(p);
				int len = p.rows;
				if(v != null) {
					for (int i = 0; i < len; i++) {
						out.writeByte(v[i]);					
					}
				} else {
					for (int i = 0; i < len; i++) {
						out.writeByte(0);					
					}
				}
			}		
		}
	}
	
	public static class Int64Column extends Column<PointTable> {
		private final Function<PointTable, long[]> mapper;

		public Int64Column(String name, Function<PointTable, long[]> mapper) {
			super(name);
			this.mapper = mapper;
		}

		@Override
		public void write(DataOutput out, Iterable<PointTable> coll) throws IOException {			
			Rdat.writeSizedString(out, name);
			out.writeByte(Rdat.TYPE_INT64);
			out.writeByte(Rdat.TYPE_INT64_SIZE);
			for (PointTable p:coll) {
				long[] v = mapper.apply(p);
				int len = p.rows;
				if(v != null) {
					for (int i = 0; i < len; i++) {
						out.writeLong(v[i]);
						Logger.info("gps " + v[i]);
					}
				} else {
					for (int i = 0; i < len; i++) {
						out.writeLong(0l);					
					}
				}
			}		
		}
	}
	
	public static class GpsTimeColumn extends Column<PointTable> {
		private final Function<PointTable, long[]> mapper;

		public GpsTimeColumn(String name, Function<PointTable, long[]> mapper) {
			super(name);
			this.mapper = mapper;
		}

		@Override
		public void write(DataOutput out, Iterable<PointTable> coll) throws IOException {			
			Rdat.writeSizedString(out, name);
			out.writeByte(Rdat.TYPE_FLOAT64);
			out.writeByte(Rdat.TYPE_FLOAT64_SIZE);
			for (PointTable p:coll) {
				long[] v = mapper.apply(p);
				int len = p.rows;
				if(v != null) {
					for (int i = 0; i < len; i++) {
						out.writeDouble(v[i] / 1_000_000_000d);
					}
				} else {
					for (int i = 0; i < len; i++) {
						out.writeDouble(0d);					
					}
				}
			}		
		}
	}
	
	public static class LogicalColumn extends Column<PointTable> {
		private final Function<PointTable, BitSet> mapper;

		public LogicalColumn(String name, Function<PointTable, BitSet> mapper) {
			super(name);
			this.mapper = mapper;
		}

		@Override
		public void write(DataOutput out, Iterable<PointTable> coll) throws IOException {			
			Rdat.writeSizedString(out, name);
			out.writeByte(Rdat.TYPE_LOGICAL);
			out.writeByte(Rdat.TYPE_LOGICAL_SIZE);
			for (PointTable p:coll) {
				BitSet v = mapper.apply(p);
				int len = p.rows;
				if(v != null) {
					for (int i = 0; i < len; i++) {
						out.writeByte(v.get(i) ? 1 : 0);					
					}
				} else {
					for (int i = 0; i < len; i++) {
						out.writeByte(0);				
					}
				}
			}		
		}
	}

	public static void writePoints(PointTable[] pointTables, AttributeSelector selector, Receiver receiver) throws IOException {		
		Function<Collection<PointTable>, Integer> rowCalculator = coll -> {
			int rows = 0;
			for(PointTable pointTable:coll) {
				rows += pointTable.rows;
			}
			return rows;
		};
		RdatDataFrame<PointTable> df = new RdatDataFrame<PointTable>(rowCalculator);
		if(selector.x) {
			df.add(new DoublesColumn("x", p->p.x));
		}
		if(selector.y) {
			df.add(new DoublesColumn("y", p->p.y));
		}
		if(selector.z) {
			df.add(new DoublesColumn("z", p->p.z));
		}
		if(selector.intensity) {
			df.add(new Uint16Column("intensity", p->p.intensity));
		}
		if(selector.returnNumber) {
			df.add(new Uint8Column("returnNumber", p->p.returnNumber));
		}
		if(selector.returns) {
			df.add(new Uint8Column("returns", p->p.returns));
		}
		if(selector.scanDirectionFlag) {
			df.add(new LogicalColumn("scanDirectionFlag", p->p.scanDirectionFlag));
		}
		if(selector.edgeOfFlightLine) {
			df.add(new LogicalColumn("edgeOfFlightLine", p->p.edgeOfFlightLine));
		}		
		if(selector.classification) {
			df.add(new Uint8Column("classification", p->p.classification));
		}
		if(selector.scanAngleRank) {
			df.add(new Uint8Column("scanAngleRank", p->p.scanAngleRank));
		}
		if(selector.gpsTime) {
			df.add(new GpsTimeColumn("gpsTime", p->p.gpsTime));
		}
		if(selector.red) {
			df.add(new Uint16Column("red", p->p.red));
		}
		if(selector.green) {
			df.add(new Uint16Column("green", p->p.green));
		}
		if(selector.blue) {
			df.add(new Uint16Column("blue", p->p.blue));
		}
		df.write(receiver, pointTables);
	}

}
