package pointdb.process;

import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pointcloud.GeoPointTransformer;
import pointcloud.PointCloud;
import pointcloud.PointTable;
import pointdb.PointDB;
import pointdb.base.GeoPoint;
import pointdb.base.Point2d;
import pointdb.base.PolygonUtil;
import pointdb.base.Rect;
import pointdb.processing.geopoint.PointGrid;
import pointdb.processing.geopoint.RasterGrid;
import pointdb.processing.geopoint.RasterSubGrid;
import pointdb.subsetdsl.Region;
import server.api.pointdb.DSM_generator;
import server.api.pointdb.DTM2_generator;
import server.api.pointdb.RasterGenerator;
import util.collections.vec.DoubleVec;
import util.collections.vec.Vec;

public class DataProvider2 {
	static final Logger log = LogManager.getLogger();

	private static final int BORDER_SIZE = 16;

	public final PointCloud pointcloud;
	public final PointDB db;
	public final boolean classified_ground; // some ground points of are classified as ground
	public final boolean classified_vegetation; // ground_classified + all vegetation points are classified as vegetation
	public final Region region; // region of query
	public final Rect bbox_rect; // bbox of region
	public final Rect bordered_bbox_rect; // bordered bbox of region

	private Vec<GeoPoint> bordered_bboxPoints = null; // all points in bbox + some border
	private Vec<GeoPoint> bboxPoints = null; // all points in bbox
	private Vec<GeoPoint> regionPoints = null; // all points in region

	private RasterSubGrid dtm;  //processing (optional using classification)
	private RasterSubGrid dsm;
	private RasterSubGrid chm;

	private Vec<GeoPoint> sortedRegionHeightPoints = null; // all points in region with z as height above ground (DTM)

	private double[] sortedCanopyHeights = null;

	public DataProvider old;

	private int pulseCount = Integer.MIN_VALUE;

	public DataProvider2(PointCloud pointcloud, Region region) {
		this(pointcloud, null, region);
	}

	public DataProvider2(PointDB db, Region region) {
		this(null, db, region);
	}

	public DataProvider2(PointCloud pointcloud, PointDB db, Region region) {
		this.pointcloud = pointcloud;
		this.db = db;
		this.classified_ground = pointcloud == null ? db.config.isClassified_ground() : pointcloud.isClassified_ground();
		this.classified_vegetation = pointcloud == null ? db.config.isClassified_vegetation() : pointcloud.isClassified_vegetation();
		this.region = region;
		this.bbox_rect = region.bbox;
		this.bordered_bbox_rect = bbox_rect.withBorderUTM(BORDER_SIZE).outerMeterRect();
		this.old = new DataProvider(db, region, this);
	}

	public synchronized Vec<GeoPoint> get_bordered_bboxPoints() {
		if(bordered_bboxPoints == null) {
			bordered_bboxPoints = load_rect(bordered_bbox_rect);
		}
		return bordered_bboxPoints;
	}

	private synchronized Vec<GeoPoint> load_rect(Rect rect) {
		if(pointcloud == null) {
			return db.tilePointProducer(rect).toGeoPointProducer().toList();
		} else {
			double xmin = rect.getUTMd_min_x();
			double ymin = rect.getUTMd_min_y();
			double xmax = rect.getUTMd_max_x();
			double ymax = rect.getUTMd_max_y();
			Stream<PointTable> pointTables = pointcloud.getPointTables(xmin, ymin, xmax, ymax, GeoPointTransformer.FULL_GEOPOINT_SELECTOR);
			return GeoPointTransformer.transform(pointTables);
		}
	}

	public synchronized Vec<GeoPoint> get_bboxPoints() {
		return get_bboxPoints(true);
	}

	public synchronized Vec<GeoPoint> get_bboxPoints(boolean load_bordered_bbox) {
		if(bboxPoints == null) {
			if(load_bordered_bbox) {
				Vec<GeoPoint> pointlist = new Vec<>();
				double xmin = bbox_rect.getUTMd_min_x();
				double xmax_exclusive = bbox_rect.getUTMd_max_x_exclusive();
				double ymin = bbox_rect.getUTMd_min_y();
				double ymax_exclusive = bbox_rect.getUTMd_max_y_exclusive();
				for(GeoPoint p:get_bordered_bboxPoints()) {
					double x = p.x;
					double y = p.y;
					if(xmin <= x && x < xmax_exclusive && ymin <= y && y < ymax_exclusive) {
						pointlist.add(p);
					}
				}
				bboxPoints = pointlist;
			} else {
				bboxPoints = load_rect(bbox_rect);
			}
		}
		return bboxPoints;
	}

	public synchronized Vec<GeoPoint> get_regionPoints() {
		return get_regionPoints(true);
	}

	public synchronized Vec<GeoPoint> get_regionPoints(boolean load_bordered_bbox) {
		if(regionPoints == null) {
			if(region.isBbox()) {
				regionPoints = get_bboxPoints(load_bordered_bbox);
			} else {
				Vec<GeoPoint> pointlist = new Vec<>();
				Point2d[] polygon = region.polygonPoints;
				for(GeoPoint p:get_bboxPoints(load_bordered_bbox)) {					
					if(PolygonUtil.wn_PnPoly(p, polygon) != 0) {
						pointlist.add(p);
					}
				}
				regionPoints = pointlist;
			}
		}
		return regionPoints;
	}

	public synchronized RasterSubGrid getDTM() {
		if(dtm == null) {
			if(classified_ground) {
				Vec<GeoPoint> pointlist = new Vec<>();
				for(GeoPoint p:get_bordered_bboxPoints()) {
					if(p.isGround()) {
						pointlist.add(p);
					}
				}
				RasterGenerator dtmGenerator = new RasterGenerator(bordered_bbox_rect, pointlist);
				dtmGenerator.run();
				dtm = dtmGenerator.rasterGrid.subGrid(bbox_rect);
			} else {				
				Vec<GeoPoint> pointlist = new Vec<>();
				for(GeoPoint p:get_bordered_bboxPoints()) {
					if(p.returns == p.returnNumber) {
						pointlist.add(p);
					}
				}
				DTM2_generator dtm2_gen = new DTM2_generator(bordered_bbox_rect, pointlist);
				dtm = dtm2_gen.get().subGrid(bbox_rect);
			}
		}
		return dtm;
	}

	public synchronized RasterSubGrid getDSM() {
		if(dsm == null) {
			Vec<GeoPoint> points = get_bordered_bboxPoints();
			if(classified_ground && classified_vegetation) {
				points = points.filter(GeoPoint::isEntity);
			}
			PointGrid pointGrid = PointGrid.of(bordered_bbox_rect, points);
			RasterGrid rasterGrid = DSM_generator.generate(pointGrid);
			dsm = rasterGrid.subGrid(bbox_rect);			
		}
		return dsm;
	}

	public synchronized RasterSubGrid getCHM() {
		if(chm == null) {
			RasterSubGrid r = getDSM().copy();
			r.minus_zero(getDTM());
			chm = r;
		}
		return chm;
	}

	public synchronized Vec<GeoPoint> get_sortedRegionHeightPoints() {
		if(sortedRegionHeightPoints==null) {
			RasterSubGrid raster = getDTM();
			int lx = raster.local_min_x - raster.start_x;
			int ly = raster.local_min_y - raster.start_y;
			double[][] data = raster.data;
			Vec<GeoPoint> zs = new Vec<GeoPoint>();
			for(GeoPoint p:get_regionPoints()) {
				int x = ((int)p.x) - lx;
				int y = ((int)p.y) - ly;
				double z = p.z - data[y][x];
				GeoPoint pz = GeoPoint.of(p.x, p.y, z, p);
				zs.add(pz);
			}
			//Timer.start("get_sortedRegionHeightPoints sort");
			//log.info("sort "+zs.size());
			zs.sort(GeoPoint.Z_COMPARATOR_SAFE);
			//log.info(Timer.stop("get_sortedRegionHeightPoints sort"));
			sortedRegionHeightPoints = zs;
			//log.info("sorted");
			if(zs.size() > 0) {
				log.info("get_sortedRegionHeightPoints " + zs.size() + "  min " + zs.get(0) + "  max " + zs.get(zs.size()-1));
			}
		}
		return sortedRegionHeightPoints;
	}

	/**
	 * all points, including ground
	 * @return
	 */
	public double[] get_sortedRegionHeights() {
		return get_sortedRegionHeightPoints().mapDoubleArray(GeoPoint::getZ);
	}

	public static final double GROUND_HEIGHT = 0.3d; // ground height tolerance for not classified points

	/**
	 * just vegetation points, no ground points, no > 98% points
	 * @return
	 */
	public synchronized double[] get_sortedCanopyHeights() {
		if(sortedCanopyHeights == null) {
			DoubleVec zs = new DoubleVec();
			Vec<GeoPoint> points = get_sortedRegionHeightPoints();
			int border = points.size();
			if(border>=100) {
				border = (border*98)/100;
			}
			if(classified_vegetation) {
				for (int i = 0; i < border; i++) {
					GeoPoint p = points.get(i);
					if(p.isVegetaion()) {
						zs.add(p.z);
					}
				}
			} else {
				for (int i = 0; i < border; i++) {
					GeoPoint p = points.get(i);
					if(p.z>=GROUND_HEIGHT) {
						zs.add(p.z);
					}
				}				
			}
			sortedCanopyHeights = zs.toArray();
		}
		return sortedCanopyHeights;
	}

	public synchronized int getPulseCount() {
		if(pulseCount == Integer.MIN_VALUE) {
			pulseCount = get_regionPoints().count(GeoPoint::isFirstReturn);
		}
		return pulseCount;
	}

	public void close() {
		old.close();		
		bordered_bboxPoints = null;
		bboxPoints = null;
		regionPoints = null;
		dtm = null;
		dsm = null;
		chm = null;
		sortedRegionHeightPoints = null;
		sortedCanopyHeights = null;
		old = null;
	}
}