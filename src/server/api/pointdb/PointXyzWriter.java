package server.api.pointdb;

import java.io.IOException;
import java.io.PrintWriter;

import pointdb.base.GeoPoint;
import util.Receiver;
import util.Web;
import util.collections.vec.Vec;

public class PointXyzWriter {

	private static abstract class PointWriter {
		abstract void write(GeoPoint p);
	}

	private static class PointWriter_x extends PointWriter {
		private final PrintWriter writer;

		public PointWriter_x(PrintWriter writer) {
			this.writer = writer;
		}

		@Override
		void write(GeoPoint p) {
			writer.print(p.x);			
		}		
	}

	private static class PointWriter_y extends PointWriter {
		private final PrintWriter writer;

		public PointWriter_y(PrintWriter writer) {
			this.writer = writer;
		}

		@Override
		void write(GeoPoint p) {
			writer.print(p.y);			
		}		
	}

	private static class PointWriter_z extends PointWriter {
		private final PrintWriter writer;

		public PointWriter_z(PrintWriter writer) {
			this.writer = writer;
		}

		@Override
		void write(GeoPoint p) {
			writer.print(p.z);			
		}		
	}
	
	private static class PointWriter_intensity extends PointWriter {
		private final PrintWriter writer;

		public PointWriter_intensity(PrintWriter writer) {
			this.writer = writer;
		}

		@Override
		void write(GeoPoint p) {
			writer.print(p.intensity);			
		}		
	}
	
	private static class PointWriter_returnNumber extends PointWriter {
		private final PrintWriter writer;

		public PointWriter_returnNumber(PrintWriter writer) {
			this.writer = writer;
		}

		@Override
		void write(GeoPoint p) {
			writer.print(p.returnNumber);			
		}		
	}
	
	private static class PointWriter_returns extends PointWriter {
		private final PrintWriter writer;

		public PointWriter_returns(PrintWriter writer) {
			this.writer = writer;
		}

		@Override
		void write(GeoPoint p) {
			writer.print(p.returns);			
		}		
	}
	
	private static class PointWriter_scanAngleRank extends PointWriter {
		private final PrintWriter writer;

		public PointWriter_scanAngleRank(PrintWriter writer) {
			this.writer = writer;
		}

		@Override
		void write(GeoPoint p) {
			writer.print(p.scanAngleRank);			
		}		
	}
	
	private static class PointWriter_classification extends PointWriter {
		private final PrintWriter writer;

		public PointWriter_classification(PrintWriter writer) {
			this.writer = writer;
		}

		@Override
		void write(GeoPoint p) {
			writer.print(p.classification);			
		}		
	}
	
	private static class PointWriter_classificationFlags extends PointWriter {
		private final PrintWriter writer;

		public PointWriter_classificationFlags(PrintWriter writer) {
			this.writer = writer;
		}

		@Override
		void write(GeoPoint p) {
			writer.print(p.classificationFlags);			
		}		
	}
	
	

	public static void writePoints(Receiver receiver, Vec<GeoPoint> points, String[] columns) throws IOException {
		receiver.setContentType(Web.MIME_BINARY);

		PrintWriter writer = receiver.getWriter();

		if(columns == null || (columns.length == 3 && columns[0].equals("x") && columns[1].equals("y") && columns[2].equals("z"))) {
			for(GeoPoint p:points) {
				writer.print(p.x);
				writer.print(' ');
				writer.print(p.y);
				writer.print(' ');
				writer.print(p.z);
				writer.println();
			}
		} if(columns.length == 0) {
			// nothing
		} else {
			PointWriter[] pointWriters = new PointWriter[columns.length];
			for (int i = 0; i < columns.length; i++) {
				switch(columns[i]) {
				case "x":
					pointWriters[i] = new PointWriter_x(writer);
					break;
				case "y":
					pointWriters[i] = new PointWriter_y(writer);
					break;
				case "z":
					pointWriters[i] = new PointWriter_z(writer);
					break;
				case "intensity":
					pointWriters[i] = new PointWriter_intensity(writer);
					break;
				case "returnNumber":
					pointWriters[i] = new PointWriter_returnNumber(writer);
					break;
				case "returns":
					pointWriters[i] = new PointWriter_returns(writer);
					break;
				case "scanAngleRank":
					pointWriters[i] = new PointWriter_scanAngleRank(writer);
					break;
				case "classification":
					pointWriters[i] = new PointWriter_classification(writer);
					break;
				case "classificationFlags":
					pointWriters[i] = new PointWriter_classificationFlags(writer);
					break;
				default:
					throw new RuntimeException("unknown column: " + columns[i]);
				}
			}

			for(GeoPoint p:points) {
				pointWriters[0].write(p);
				for (int i = 1; i < pointWriters.length; i++) {
					writer.print(' ');
					pointWriters[i].write(p);
				}
				writer.println();
			}
		}
	}

}
