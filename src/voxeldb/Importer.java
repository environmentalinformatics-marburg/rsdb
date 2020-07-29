package voxeldb;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import pointcloud.CellTable;
import pointcloud.DoubleRect;
import pointdb.las.Las;
import pointdb.las.Laz;
import remotetask.MessageReceiver;
import util.Timer;
import util.Util;

public class Importer {
	private static final Logger log = LogManager.getLogger();

	private static final CRSFactory CRS_FACTORY = new CRSFactory();

	private static final int READ_MAX_BYTES = 1_000_000_000;

	private final VoxelDB voxeldb;
	private final DoubleRect filterRect;
	private final boolean trySetOriginToFileOrigin;
	private final TimeSlice timeSlice;
	private final MessageReceiver messageReceiver;

	public Importer(VoxelDB voxeldb, DoubleRect filterRect, boolean trySetOriginToFileOrigin, TimeSlice timeSlice, MessageReceiver messageReceiver) {
		this.voxeldb = voxeldb;
		this.filterRect = filterRect;
		this.trySetOriginToFileOrigin = trySetOriginToFileOrigin;
		this.timeSlice = timeSlice;
		this.messageReceiver = messageReceiver;
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
						//log.info("import file "+path);					
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
			if(voxeldb.geoRef().hasEpsg()) {
				//TODO
			} else {
				voxeldb.setEpsg(fileEPSG);
				if(!voxeldb.geoRef().hasProj4()) {
					try {
						String fileCode = "EPSG:"+fileEPSG;
						CoordinateReferenceSystem crs = CRS_FACTORY.createFromName(fileCode);
						if(crs != null) {
							String fileProj4 = crs.getParameterString();
							if(fileProj4 != null && !fileProj4.isEmpty()) {
								log.info("set proj4: " + fileProj4);
								voxeldb.setProj4(fileProj4);
							} else {
								log.warn("could not get proj4 of epsg");
							}
						} else {
							log.warn("could not get proj4 of epsg");
						}
					} catch(Exception e) {
						log.warn("could not get proj4 of epsg: " + e);
					}
				}
			}
		}

		double[] las_scale_factor = isLas ? las.scale_factor : laz.scale_factor;
		double[] las_offset = isLas ? las.offset : laz.offset;
		long las_number_of_point_records = isLas ? las.number_of_point_records : laz.number_of_point_records;
		int record_count_max = READ_MAX_BYTES / (isLas ? las.point_Data_Record_Length : laz.point_Data_Record_Length);

		int xVoxelCellsize = voxeldb.getCellsize();
		int yVoxelCellsize = voxeldb.getCellsize();
		int zVoxelCellsize = voxeldb.getCellsize();

		double voxelSizeX = voxeldb.geoRef().voxelSizeX;
		double voxelSizeY = voxeldb.geoRef().voxelSizeY;
		double voxelSizeZ = voxeldb.geoRef().voxelSizeZ;

		double xProjectedCellsize = xVoxelCellsize * voxelSizeX;
		double yProjectedCellsize = yVoxelCellsize * voxelSizeY;
		double zProjectedCellsize = zVoxelCellsize * voxelSizeZ;

		double xlasmin = isLas ? las.min[0] : laz.min[0];
		double ylasmin = isLas ? las.min[1] : laz.min[1];
		double zlasmin = isLas ? las.min[2] : laz.min[2];
		double xlasmax = isLas ? las.max[0] : laz.max[0];
		double ylasmax = isLas ? las.max[1] : laz.max[1];
		double zlasmax = isLas ? las.max[2] : laz.max[2];

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

		if(trySetOriginToFileOrigin) {
			voxeldb.trySetOrigin(xlasmin, ylasmin, zlasmin);
		}

		double xProjectedOrigin = voxeldb.geoRef().originX;
		double yProjectedOrigin = voxeldb.geoRef().originY;
		double zProjectedOrigin = voxeldb.geoRef().originZ;

		int xlascellmin = (int) Math.floor((xlasmin - xProjectedOrigin) / xProjectedCellsize);
		int xlascellmax = (int) Math.floor((xlasmax - xProjectedOrigin) / xProjectedCellsize);		
		int ylascellmin = (int) Math.floor((ylasmin - yProjectedOrigin) / yProjectedCellsize);
		int ylascellmax = (int) Math.floor((ylasmax - yProjectedOrigin) / yProjectedCellsize);		
		int zlascellmin = (int) Math.floor((zlasmin - zProjectedOrigin) / zProjectedCellsize);
		int zlascellmax = (int) Math.floor((zlasmax - zProjectedOrigin) / zProjectedCellsize);

		final int t = timeSlice.id;

		long pos = 0;
		int subdividedLen = 0;  // 0 == no subdivision
		int round = 0;
		while(pos < las_number_of_point_records) {
			round++;
			long current_long_len = subdividedLen > 0 ? subdividedLen : las_number_of_point_records - pos;
			int len = current_long_len >= record_count_max ? record_count_max : (int) current_long_len;
			messageReceiver.setMessage("read records at " + pos + " len " + len + "  of " + las_number_of_point_records + "   round " + round);
			Timer.resume("get records");
			CellTable recordTable = isLas ? las.getRecords(pos, len) : laz.getRecords(pos, len);
			//log.info(Timer.stop("get records"));
			messageReceiver.setMessage("process records at " + pos + " len " + len + "  of " + las_number_of_point_records + "   round " + round);

			Timer.resume("convert records");

			int[] xs = recordTable.x;
			int[] ys = recordTable.y;
			int[] zs = recordTable.z;

			int[] xrange = Util.getRange(xs);
			int[] yrange = Util.getRange(ys);
			int[] zrange = Util.getRange(zs);

			log.info("block range "+Arrays.toString(xrange) + " " + Arrays.toString(yrange) + " " + Arrays.toString(zrange));

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
			double zmin = (zrange[0] * zscale) + zoff;
			double zmax = (zrange[1] * zscale) + zoff;

			log.info("projected range "+xmin+" "+ymin+" "+zmin+"   "+xmax+" "+ymax+" "+zmax+"      "+(xmax-xmin)+" "+(ymax-ymin)+" "+(zmax-zmin));

			int xcellmin = (int) Math.floor((xmin - xProjectedOrigin) / xProjectedCellsize);
			int xcellmax = (int) Math.floor((xmax - xProjectedOrigin) / xProjectedCellsize);		
			int ycellmin = (int) Math.floor((ymin - yProjectedOrigin) / yProjectedCellsize);
			int ycellmax = (int) Math.floor((ymax - yProjectedOrigin) / yProjectedCellsize);			
			int zcellmin = (int) Math.floor((zmin - zProjectedOrigin) / zProjectedCellsize);
			int zcellmax = (int) Math.floor((zmax - zProjectedOrigin) / zProjectedCellsize);

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
			if(zcellmin < zlascellmin) {
				zcellmin = zlascellmin;
			}
			if(zcellmin > zlascellmax) {
				zcellmin = zlascellmax;
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
			if(zcellmax > zlascellmax) {
				zcellmax = zlascellmax;
			}
			if(zcellmax < zlascellmin) {
				zcellmax = zlascellmin;
			}

			int xcellrange = xcellmax - xcellmin + 1;
			int ycellrange = ycellmax - ycellmin + 1;
			int zcellrange = zcellmax - zcellmin + 1;
			long totalCellCount = ((long) xcellrange) * ((long) ycellrange) * ((long) zcellrange);
			log.info("cell "+xcellmin+" "+ycellmin+" "+zcellmin+"   "+xcellmax+" "+ycellmax+" "+zcellmax+"     "+xcellrange+" "+ycellrange+" "+zcellrange + "   " + totalCellCount);
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

			VoxelCell[][][] cells = new VoxelCell[ycellrange][xcellrange][zcellrange];

			for (int i = 0; i < len; i++) {
				double x = (xs[i] * xscale) + xoff;
				double y = (ys[i] * yscale) + yoff;
				double z = (zs[i] * zscale) + zoff;
				if(xlasmin <= x && x <= xlasmax && ylasmin <= y && y <= ylasmax) { // extent check of las file and filter rect					
					int xcell = (int) Math.floor((x - xProjectedOrigin) / xProjectedCellsize) - xcellmin;
					int ycell = (int) Math.floor((y - yProjectedOrigin) / yProjectedCellsize) - ycellmin;
					int zcell = (int) Math.floor((z - zProjectedOrigin) / zProjectedCellsize) - zcellmin;

					//log.info("point cell " + xcell + " " + ycell + " " + zcell);

					VoxelCell voxelCell = cells[ycell][xcell][zcell];
					if(voxelCell == null) {
						voxelCell = voxeldb.getVoxelCell(xcellmin + xcell, ycellmin + ycell, zcellmin + zcell, t);
						if(voxelCell == null) {
							int[][][] cnt = new int[zVoxelCellsize][yVoxelCellsize][xVoxelCellsize];
							voxelCell = new VoxelCell(xcellmin + xcell, ycellmin + ycell, zcellmin + zcell, cnt);
						}
						cells[ycell][xcell][zcell] = voxelCell;
					}

					int xloc = (int) (floorMod(x - xProjectedOrigin, xProjectedCellsize) / voxelSizeX);
					int yloc = (int) (floorMod(y - yProjectedOrigin, yProjectedCellsize) / voxelSizeY);
					int zloc = (int) (floorMod(z - zProjectedOrigin, zProjectedCellsize) / voxelSizeZ);

					voxelCell.cnt[zloc][yloc][xloc]++;
				}
			}
			
			messageReceiver.setMessage("write records at " + pos + " len " + len + "  of " + las_number_of_point_records + "   round " + round);

			for (int y = 0; y < ycellrange; y++) {
				VoxelCell[][] cellsY = cells[y];
				for (int x = 0; x < xcellrange; x++) {
					VoxelCell[] cellsYX = cellsY[x];				
					for (int z = 0; z < zcellrange; z++) {
						VoxelCell voxelCell = cellsYX[z];
						if(voxelCell != null) {
							voxeldb.writeVoxelCell(voxelCell, t);
						}
					}
				}
			}
			voxeldb.commit();
		}
		log.info(Timer.stop("import"));	
	}

	public static double floorMod(double x, double y) {
		return x - Math.floor(x / y) * y;
	}
}
