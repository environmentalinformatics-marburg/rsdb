package pointcloud;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import pointdb.las.Las;
import pointdb.las.Laz;
import rasterunit.Tile;
import util.Timer;
import util.Util;

public class Importer {
	private static final Logger log = LogManager.getLogger();

	private final PointCloud pointcloud;

	private static final int READ_MAX_BYTES = 1_000_000_000;

	public Importer(PointCloud pointcloud) {
		this.pointcloud = pointcloud;
	}

	/**
	 * 
	 * @param root directory or file
	 * @throws IOException
	 */
	public void importDirectory(Path root) throws IOException {
		Path[] paths = null;
		if(root.toFile().isFile()) {
			paths = new Path[]{root};
		} else {
			paths = Util.getPaths(root);
			for(Path path:paths) {
				if(path.toFile().isDirectory()) {
					importDirectory(path);
				}
			}
		}
		for(Path path:paths) {
			if(path.toFile().isFile()) {				
				try {
					String filename = path.getFileName().toString().toLowerCase();
					String ext = filename.substring(filename.lastIndexOf('.')+1);
					if(ext.trim().toLowerCase().equals("las") || ext.trim().toLowerCase().equals("laz")) {
						log.info("import file "+path);					
						importFile(path);
					} else {
						//log.info("skip file "+path);	
					}
				} catch(Exception e) {
					e.printStackTrace();
					log.error(e);
				}

			}
		}
	}

	public static int dayCorrection(DayOfWeek dayOfWeek) {
		switch(dayOfWeek) {
		case SUNDAY: return 0;
		case MONDAY: return 1;
		case THURSDAY: return 2;
		case WEDNESDAY: return 3;
		case TUESDAY: return 4;
		case FRIDAY: return 5;
		case SATURDAY: return 6;
		default: throw new RuntimeException();
		}
	}

	private void importFile(Path filename) throws IOException {
		log.info("import " + filename);
		Timer.start("import");
		
		Las las = null;
		Laz laz = null;
		boolean isLas = true;
		int fileEPSG = 0;
		if(filename.toString().toLowerCase().endsWith("las")) {
			las = new Las(filename);
			log.info(las);			
			fileEPSG = las.readEPSG();			
		} else if(filename.toString().toLowerCase().endsWith("laz")) {
			laz = new Laz(filename);
			isLas = false;
			log.info(laz);
			fileEPSG = laz.readEPSG();
		} else {
			throw new RuntimeException("unknown extension");
		}
		
		if(fileEPSG != 0) {
			log.info("fileEpsg " + fileEPSG);
			if(pointcloud.hasCode()) {
				//TODO
			} else {
				pointcloud.setCodeEPSG(fileEPSG);
				if(!pointcloud.hasProj4()) {
					String url = "https://epsg.io/"+fileEPSG+".proj4";
					log.warn("request proj4 of epsg: " + fileEPSG + "     " + url);
					OkHttpClient client = new OkHttpClient.Builder()
					        .connectTimeout(15, TimeUnit.SECONDS)
					        .writeTimeout(15, TimeUnit.SECONDS)
					        .readTimeout(15, TimeUnit.SECONDS)
					        .build();
					Request request = new Request.Builder().url(url).build();
					try(Response response = client.newCall(request).execute()) {
						if(response.isSuccessful()) {
							String proj4 = response.body().string();
							pointcloud.setProj4(proj4);
							log.info("received proj4: "+proj4);							
						} else {
							log.warn("could not request proj4 of epsg");
						}
					} catch(Exception e) {
						log.warn("could not request proj4 of epsg: " + e);
					}
				}
			}
		}
		
		double[] las_scale_factor = isLas ? las.scale_factor : laz.scale_factor;
		double[] las_offset = isLas ? las.offset : laz.offset;
		long las_number_of_point_records = isLas ? las.number_of_point_records : laz.number_of_point_records;
		int record_count_max = READ_MAX_BYTES / (isLas ? las.point_Data_Record_Length : laz.point_Data_Record_Length);

		double xcellsize = pointcloud.getCellsize();
		double ycellsize = pointcloud.getCellsize();

		double xlasmin = isLas ? las.min[0] : laz.min[0];
		double ylasmin = isLas ? las.min[1] : laz.min[1];
		double xlasmax = isLas ? las.max[0] : laz.max[0];
		double ylasmax = isLas ? las.max[1] : laz.max[1];
		


		DoublePoint celloffset = pointcloud.getOrSetCelloffset(Math.floor(xlasmin / xcellsize), Math.floor(ylasmin / ycellsize));
		double xcelloffset = celloffset.x;
		double ycelloffset = celloffset.y;

		double xcellscale = pointcloud.getCellscale();
		double ycellscale = pointcloud.getCellscale();
		double zcellscale = pointcloud.getCellscale();

		int xlascellmin = (int) (Math.floor(xlasmin / xcellsize) - xcelloffset);
		int xlascellmax = (int) (Math.floor(xlasmax / xcellsize) - xcelloffset);
		int ylascellmin = (int) (Math.floor(ylasmin / ycellsize) - ycelloffset);
		int ylascellmax = (int) (Math.floor(ylasmax / ycellsize) - ycelloffset);

		long pos = 0;
		while(pos < las_number_of_point_records) {
			long current_long_len = las_number_of_point_records - pos;
			int len = current_long_len >= record_count_max ? record_count_max : (int) current_long_len;
			Timer.resume("get records");
			CellTable recordTable = isLas ? las.getRecords(pos, len) : laz.getRecords(pos, len);
			log.info(Timer.stop("get records"));
			pos += len;

			Timer.resume("convert records");

			int[] xs = recordTable.x;
			int[] ys = recordTable.y;
			int[] zs = recordTable.z;
			char[] intensity = recordTable.intensity;
			byte[] returnNumber = recordTable.returnNumber;
			byte[] returns = recordTable.returns;
			BitSet scanDirectionFlag = recordTable.scanDirectionFlag;
			BitSet edgeOfFlightLine = recordTable.edgeOfFlightLine;
			byte[] classification = recordTable.classification;
			byte[] scanAngleRank = recordTable.scanAngleRank;
			long[] gpsTime = recordTable.gpsTime;
			char[] red = recordTable.red;
			char[] green = recordTable.green;
			char[] blue = recordTable.blue;

			int[] xrange = Util.getRange(xs);
			int[] yrange = Util.getRange(ys);

			log.info("xyrange "+Arrays.toString(xrange) + " " + Arrays.toString(yrange));

			double xscale = las_scale_factor[0];
			double yscale = las_scale_factor[1];
			double zscale = las_scale_factor[2];
			double xoff = las_offset[0];
			double yoff = las_offset[1];
			double zoff = las_offset[2];

			double xmin = (xrange[0] * xscale) + xoff;
			double xmax = (xrange[1] * xscale) + xoff;
			double ymin = (yrange[0] * yscale) + yoff;
			double ymax = (yrange[1] * yscale) + yoff;

			log.info(xmin+" "+ymin+" "+xmax+" "+ymax+"     "+(xmax-xmin)+" "+(ymax-ymin));


			int xcellmin = (int) (Math.floor(xmin / xcellsize) - xcelloffset);
			int xcellmax = (int) (Math.floor(xmax / xcellsize) - xcelloffset);
			int ycellmin = (int) (Math.floor(ymin / ycellsize) - ycelloffset);
			int ycellmax = (int) (Math.floor(ymax / ycellsize) - ycelloffset);

			if(xcellmin < xlascellmin) {
				xcellmin = xlascellmin;
			}
			if(xcellmin > xlascellmax) {
				xcellmin = xlascellmax;
			}
			if(ycellmin < ylascellmin) {
				ycellmin = ylascellmin;
			}
			if(ycellmin > ylascellmax) {
				ycellmin = ylascellmax;
			}
			if(xcellmax > xlascellmax) {
				xcellmax = xlascellmax;
			}
			if(xcellmax < xlascellmin) {
				xcellmax = xlascellmin;
			}
			if(ycellmax > ylascellmax) {
				ycellmax = ylascellmax;
			}
			if(ycellmax < ylascellmin) {
				ycellmax = ylascellmin;
			}


			int xcellrange = xcellmax - xcellmin + 1;
			int ycellrange = ycellmax - ycellmin + 1;

			log.info("cell "+xcellmin+" "+ycellmin+" "+xcellmax+" "+ycellmax+"     "+xcellrange+" "+ycellrange);

			Timer.start("cell count");
			int[][] cellcnt = new int[ycellrange][xcellrange]; // upper bound of point count of cell
			for (int i = 0; i < len; i++) {
				double x = (xs[i] * xscale) + xoff;
				double y = (ys[i] * yscale) + yoff;
				int xcell = (int)(Math.floor(x / xcellsize) - xcelloffset) - xcellmin;
				int ycell = (int)(Math.floor(y / ycellsize) - ycelloffset) - ycellmin;
				if(0 <= xcell && xcell < xcellrange && 0 <= ycell && ycell < ycellrange) {
					cellcnt[ycell][xcell]++;
				}
			}
			log.info(Timer.stop("cell count"));

			CellTable[][] cells = new CellTable[ycellrange][xcellrange];
			boolean useIntensity = intensity != null;
			boolean useReturnNumber = returnNumber != null;
			boolean useReturns = returns != null;
			boolean useScanDirectionFlag = scanDirectionFlag != null;
			boolean useEdgeOfFlightLine = edgeOfFlightLine != null;
			boolean useClassification = classification != null;
			boolean useScanAngleRank = scanAngleRank != null;
			boolean useGpsTime = gpsTime != null;
			boolean useRed = red != null;
			boolean useGreen = green != null;
			boolean useBlue = blue != null;

			Timer.start("cell init");
			int cellTotalCount = 0;
			for (int y = 0; y < ycellrange; y++) {
				for (int x = 0; x < xcellrange; x++) {
					int cnt = cellcnt[y][x];
					if(cnt > 0) {
						//log.info(x+" "+y+"  cnt "+cnt);
						//cellcnt[y][x] = 0;
						CellTable cell = new CellTable(x, y, 0, new int[cnt], new int[cnt], new int[cnt]);
						if(useIntensity) {
							cell.intensity = new char[cnt];
						}
						if(useReturnNumber) {
							cell.returnNumber = new byte[cnt];
						}
						if(useReturns) {
							cell.returns = new byte[cnt];
						}
						if(useScanDirectionFlag) {
							cell.scanDirectionFlag = new BitSet(cnt);
						}
						if(useEdgeOfFlightLine) {
							cell.edgeOfFlightLine = new BitSet(cnt);
						}
						if(useClassification) {
							cell.classification = new byte[cnt];
						}
						if(useScanAngleRank) {
							cell.scanAngleRank = new byte[cnt];
						}
						if(useGpsTime) {
							cell.gpsTime = new long[cnt];
						}
						if(useRed) {
							cell.red = new char[cnt];
						}
						if(useGreen) {
							cell.green = new char[cnt];
						}
						if(useBlue) {
							cell.blue = new char[cnt];
						}
						cells[y][x] = cell;
						cellTotalCount++;
					}
				}
			}
			log.info(Timer.stop("cell init")+"  "+cellTotalCount+" cells of "+(ycellrange * xcellrange));



			for (int i = 0; i < len; i++) {
				double x = (xs[i] * xscale) + xoff;
				double y = (ys[i] * yscale) + yoff;
				if(xlasmin <= x && x <= xlasmax && ylasmin <= y && y <= ylasmax) { // extent check of las file
					int xcell = (int)(Math.floor(x / xcellsize) - xcelloffset) - xcellmin;
					int ycell = (int)(Math.floor(y / ycellsize) - ycelloffset) - ycellmin;
					//if(0 <= xcell && xcell < xcellrange && 0 <= ycell && ycell < ycellrange) { // extent check of cells
					//int xloc = (int) Math.round((x % xcellsize) * xcellscale); // not correct for x values below zero
					//int yloc = (int) Math.round((y % ycellsize) * ycellscale); // not correct for y values below zero
					//int xloc = (int) Math.round(floorMod(x, xcellsize) * xcellscale); // floor for x values below zero, not correct, may round over cell bound
					//int yloc = (int) Math.round(floorMod(y, ycellsize) * ycellscale); // floor for y values below zero, not correct, may round over cell bound
					int xloc = (int) (floorMod(x, xcellsize) * xcellscale); // floor for x values below zero, then positive local floor
					int yloc = (int) (floorMod(y, ycellsize) * ycellscale); // floor for y values below zero, then positive local floor
					//log.info(xs[i]+" "+x+" "+xcell+"    "+xloc);
					//log.info(ys[i]+" "+y+" "+ycell+"    "+yloc);		
					//int cnt = cellcnt[ycell][xcell]++;
					CellTable cellTable = cells[ycell][xcell];
					int cnt = cellTable.rows++;
					cellTable.x[cnt] = xloc;
					cellTable.y[cnt] = yloc;
					double z = (zs[i] * zscale) + zoff;
					int zloc = (int) Math.round(z * zcellscale);
					cellTable.z[cnt] = zloc;
					if(useIntensity) {
						cellTable.intensity[cnt] = intensity[i];
					}
					if(useReturnNumber) {
						cellTable.returnNumber[cnt] = returnNumber[i];
					}
					if(useReturns) {
						cellTable.returns[cnt] = returns[i];
					}
					if(useScanDirectionFlag && scanDirectionFlag.get(i)) {
						cellTable.scanDirectionFlag.set(cnt);
					}
					if(useEdgeOfFlightLine && edgeOfFlightLine.get(i)) {
						cellTable.edgeOfFlightLine.set(cnt);
					}
					if(useClassification) {
						cellTable.classification[cnt] = classification[i];
					}
					if(useScanAngleRank) {
						cellTable.scanAngleRank[cnt] = scanAngleRank[i];
					}
					if(useGpsTime) {
						cellTable.gpsTime[cnt] = gpsTime[i];
						/*long gps = gpsTime[i];
						if(i<10) {
							log.info(gps+"   "+refDateTime.plusNanos(gps));
						}*/
					}
					if(useRed) {
						cellTable.red[cnt] = red[i];
					}
					if(useGreen) {
						cellTable.green[cnt] = green[i];
					}
					if(useBlue) {
						cellTable.blue[cnt] = blue[i];
					}
					//}
				}
			}

			log.info(Timer.stop("convert records"));

			for (int y = 0; y < ycellrange; y++) {
				for (int x = 0; x < xcellrange; x++) {
					CellTable cellTable = cells[y][x];
					if(cellTable != null) {
						cellTable.cleanup();
						int tx = x + xcellmin;
						int ty = y + ycellmin;
						//Timer.resume("get old CellTable");
						CellTable oldCellTable = pointcloud.getCellTable(tx, ty);
						//log.info(Timer.stop("get old CellTable"));
						if(oldCellTable != null) {
							//Timer.resume("merge CellTable");
							cellTable = CellTable.merge(oldCellTable, cellTable);
							//log.info(Timer.stop("merge CellTable"));
						}
						//Timer.resume("create tile");
						Tile tile = pointcloud.createTile(cellTable, tx, ty);
						//log.info(Timer.stop("create tile"));
						//log.info(tile);
						//Timer.resume("write tile");
						pointcloud.writeTile(tile);
						//log.info(Timer.stop("write tile"));
					}
				}
			}
			pointcloud.commit();

		}
		log.info("done");
		log.info(Timer.stop("import"));	
		log.info(Timer.toStringAll());
	}
	
	public static double floorMod(double x, double y) {
        return x - Math.floor(x / y) * y;
    }

}
