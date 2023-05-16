package pointcloud;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.BitSet;


import org.tinylog.Logger;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import pointdb.las.Las;
import pointdb.las.Laz;
import rasterunit.Tile;
import remotetask.CancelableRemoteProxy;
import remotetask.PrintLineStreamAdapter;
import util.Timer;
import util.Util;

public class Importer extends CancelableRemoteProxy {
	

	private static final CRSFactory CRS_FACTORY = new CRSFactory();

	private final PointCloud pointcloud;
	
	private final Rect2d filterRect; // nullable
	private final AttributeSelector selector;
	private final int compression_level; // default if == Integer.MIN_VALUE

	private static final int READ_MAX_BYTES = 1_000_000_000;
	
	public int file_counter = 0;
	public int file_error_counter = 0;

	
	public Importer(PointCloud pointcloud, Rect2d filterRect, AttributeSelector selector, int compression_level) {
		this.pointcloud = pointcloud;
		this.filterRect = filterRect;
		this.selector = selector;
		this.compression_level = compression_level;
	}

	/**
	 * 
	 * @param root directory or file
	 * @throws IOException
	 */
	public void importDirectory(Path root) throws IOException {
		Path[] paths = null;
		if(isCanceled()) {
			throw new RuntimeException("canceled");
		}
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
		if(isCanceled()) {
			throw new RuntimeException("canceled");
		}
		for(Path path:paths) {
			if(isCanceled()) {
				throw new RuntimeException("canceled");
			}
			if(path.toFile().isFile()) {				
				try {
					String filename = path.getFileName().toString().toLowerCase();
					String ext = filename.substring(filename.lastIndexOf('.')+1);
					if(ext.trim().toLowerCase().equals("las") || ext.trim().toLowerCase().equals("laz")) {
						//Logger.info("import file "+path);
						setMessage("import file "+ path + " :: imported files " + file_counter + ",   erroneous files " + file_error_counter);
						if(isCanceled()) {
							throw new RuntimeException("canceled");
						}
						importFile(path);
						file_counter++;
					} else {
						//Logger.info("skip file "+path);	
					}
				} catch(Exception e) {
					file_error_counter++;
					e.printStackTrace();
					Logger.error(e);
					e.printStackTrace(new PrintLineStreamAdapter(s -> log(s)));
				}

			}
		}	}

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
		Logger.info("import " + filename);
		Timer.start("import");

		Las las = null;
		Laz laz = null;
		boolean isLas = true;
		int fileEPSG = 0;
		if(filename.toString().toLowerCase().endsWith("las")) {
			las = new Las(filename);
			Logger.info(las);			
			fileEPSG = las.readEPSG();			
		} else if(filename.toString().toLowerCase().endsWith("laz")) {
			laz = new Laz(filename);
			isLas = false;
			Logger.info(laz);
			fileEPSG = laz.readEPSG();
		} else {
			throw new RuntimeException("unknown extension");
		}

		if(fileEPSG != 0) {
			Logger.info("fileEpsg " + fileEPSG);
			if(pointcloud.hasCode()) {
				//TODO
			} else {
				pointcloud.setCodeEPSG(fileEPSG);
				if(!pointcloud.hasProj4()) {
					try {
						String fileCode = "EPSG:"+fileEPSG;
						CoordinateReferenceSystem crs = CRS_FACTORY.createFromName(fileCode);
						if(crs != null) {
							String fileProj4 = crs.getParameterString();
							if(fileProj4 != null && !fileProj4.isEmpty()) {
								Logger.info("set proj4: " + fileProj4);
								pointcloud.setProj4(fileProj4);
							} else {
								Logger.warn("could not get proj4 of epsg");
							}
						} else {
							Logger.warn("could not get proj4 of epsg");
						}
					} catch(Exception e) {
						Logger.warn("could not get proj4 of epsg: " + e);
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
		
		if(filterRect != null) {
			if(xlasmin < filterRect.xmin) {
				xlasmin = filterRect.xmin;
			}
			if(ylasmin < filterRect.ymin) {
				ylasmin = filterRect.ymin;
			}
			if(xlasmax > filterRect.xmax) {
				xlasmax = filterRect.xmax;
			}
			if(ylasmax > filterRect.ymax) {
				ylasmax = filterRect.ymax;
			}
		}

		P2d celloffset = pointcloud.getOrSetCelloffset(Math.floor(xlasmin / xcellsize), Math.floor(ylasmin / ycellsize));
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
		int subdividedLen = 0;  // 0 == no subdivision
		while(pos < las_number_of_point_records) {
			long current_long_len = subdividedLen > 0 ? subdividedLen : las_number_of_point_records - pos;
			int len = current_long_len >= record_count_max ? record_count_max : (int) current_long_len;
			Timer.resume("get records");
			CellTable recordTable = isLas ? las.getRecords(pos, len) : laz.getRecords(pos, len);
			//Logger.info(Timer.stop("get records"));

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

			Logger.info("xyrange "+Arrays.toString(xrange) + " " + Arrays.toString(yrange));

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

			Logger.info(xmin+" "+ymin+" "+xmax+" "+ymax+"     "+(xmax-xmin)+" "+(ymax-ymin));


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
			long totalCellCount = ((long) xcellrange) * ((long) ycellrange);
			Logger.info("cell "+xcellmin+" "+ycellmin+" "+xcellmax+" "+ycellmax+"     "+xcellrange+" "+ycellrange + "   " + totalCellCount);
			int maxCellCountPerBatch = 1024*1024;
			if(totalCellCount > maxCellCountPerBatch) {
				subdividedLen = len / 2;
				if(subdividedLen < 1) {
					subdividedLen = 1;
				}
				continue; // process smaller set of point records	
			}
			subdividedLen = 0; // no subdivision
			pos += len; // process len point records

			//Timer.start("cell count");
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
			//Logger.info(Timer.stop("cell count"));

			CellTable[][] cells = new CellTable[ycellrange][xcellrange];
			boolean useIntensity = intensity != null && selector.intensity;
			boolean useReturnNumber = returnNumber != null && selector.returnNumber;
			boolean useReturns = returns != null && selector.returns;
			boolean useScanDirectionFlag = scanDirectionFlag != null && selector.scanDirectionFlag;
			boolean useEdgeOfFlightLine = edgeOfFlightLine != null && selector.edgeOfFlightLine;
			boolean useClassification = classification != null && selector.classification;
			boolean useScanAngleRank = scanAngleRank != null && selector.scanAngleRank;
			boolean useGpsTime = gpsTime != null && selector.gpsTime;
			boolean useRed = red != null && selector.red;
			boolean useGreen = green != null && selector.green;
			boolean useBlue = blue != null && selector.blue;

			Timer.start("cell init");
			int cellTotalCount = 0;
			int cz = 0;
			for (int cy = 0; cy < ycellrange; cy++) {
				for (int cx = 0; cx < xcellrange; cx++) {
					int cnt = cellcnt[cy][cx];
					if(cnt > 0) {
						//Logger.info(x+" "+y+"  cnt "+cnt);
						//cellcnt[y][x] = 0;
						CellTable cell = new CellTable(cx, cy, cz, 0, new int[cnt], new int[cnt], new int[cnt]);
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
						cells[cy][cx] = cell;
						cellTotalCount++;
					}
				}
			}
			//Logger.info(Timer.stop("cell init")+"  "+cellTotalCount+" cells of "+(ycellrange * xcellrange));



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
					//Logger.info(xs[i]+" "+x+" "+xcell+"    "+xloc);
					//Logger.info(ys[i]+" "+y+" "+ycell+"    "+yloc);		
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
						//Logger.info("set returnNumber " + returnNumber[i]);
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
							Logger.info(gps+"   "+refDateTime.plusNanos(gps));
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

			//Logger.info(Timer.stop("convert records"));

			for (int y = 0; y < ycellrange; y++) {
				for (int x = 0; x < xcellrange; x++) {
					CellTable cellTable = cells[y][x];
					if(cellTable != null) {
						cellTable.cleanup();
						int tx = x + xcellmin;
						int ty = y + ycellmin;
						int tz = 0;
						//Timer.resume("get old CellTable");
						CellTable oldCellTable = pointcloud.getCellTable(tx, ty, tz);
						//Logger.info(Timer.stop("get old CellTable"));
						if(oldCellTable != null) {
							//Timer.resume("merge CellTable");
							cellTable = CellTable.merge(oldCellTable, cellTable);
							/*if(cellTable.returnNumber != null && cellTable.returnNumber.length > 0) {
								Logger.info("cellTable.returnNumber " + cellTable.returnNumber[0]);								
							}*/
							//Logger.info(Timer.stop("merge CellTable"));
						}
						//Timer.resume("create tile");
						int t = 0;
						Tile tile = pointcloud.createTile(cellTable, tx, ty, tz, t, compression_level);

						/*pointcloud.getGriddb();
						CellTable newCellTable = pointcloud.getCellTable(GridDB.tileToCell(tile), new AttributeSelector(true));
						if(newCellTable.returnNumber != null && newCellTable.returnNumber.length > 0) {
							Logger.info("newCellTable.returnNumber " + newCellTable.returnNumber[0]);								
						}
						PointTable newPointTable = pointcloud.cellTableToPointTable(newCellTable);
						if(newPointTable.returnNumber != null && newPointTable.returnNumber.length > 0) {
							Logger.info("newPointTable.returnNumber " + newPointTable.returnNumber[0]);								
						}*/

						//Logger.info(Timer.stop("create tile"));
						//Logger.info(tile);
						//Timer.resume("write tile");
						pointcloud.writeTile(tile);
						//Logger.info(Timer.stop("write tile"));


						/*double px = (pointcloud.getCelloffset().x + x) * pointcloud.getCellsize();
						double py = (pointcloud.getCelloffset().y + y) * pointcloud.getCellsize();
						Logger.info("tps" + px + "   " + py);
						Stream<PointTable> tps = pointcloud.getPointTables(px, py, px + 100, py + 100, new AttributeSelector(true));
						tps.forEach(pt -> {
							Logger.info("pt");
							if(pt.returnNumber != null && pt.returnNumber.length > 0) {
								Logger.info("pt.returnNumber " + pt.returnNumber[0]);								
							}
						});*/
					}
				}
			}
			pointcloud.commit();

		}
		//Logger.info("done");
		Logger.info(Timer.stop("import"));	
		//Logger.info(Timer.toStringAll());
	}

	public static double floorMod(double x, double y) {
		return x - Math.floor(x / y) * y;
	}

	@Override
	public void process() throws Exception {
		throw new RuntimeException("'process' should not be called for this class.");		
	}

}
