package pointdb.process;

import java.util.stream.Stream;

import pointcloud.GeoPointTransformer;
import pointcloud.PointCloud;
import pointcloud.PointTable;
import pointcloud.PointTable.FilterByPolygonsWithHoleFunc;
import pointcloud.PointTable.FilterByPolygonsWithHolesFunc;
import pointdb.PointDB;
import pointdb.base.GeoPoint;
import pointdb.base.Point2d;
import pointdb.base.PolygonUtil;
import pointdb.base.PolygonUtil.PolygonWithHoles;
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

	private static final int BORDER_SIZE = 16;

	public final PointCloud pointcloud;
	public final PointDB db;
	public final boolean classified_ground; // some ground points of are classified as ground
	public final boolean classified_vegetation; // ground_classified + all vegetation points are classified as vegetation
	public final int t; // poincloud only
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
	private GeoPoint[] sortedCanopyHeightPoints = null;	

	public DataProvider old;

	private int regionPulseCount = Integer.MIN_VALUE;
	private int bboxPulseCount = Integer.MIN_VALUE;

	public DataProvider2(PointCloud pointcloud, int t, Region region) {
		this(pointcloud, null, t, region);
	}

	public DataProvider2(PointDB db, Region region) {
		this(null, db, 0, region);
	}

	public DataProvider2(PointCloud pointcloud, PointDB db, int t, Region region) {
		this.pointcloud = pointcloud;
		this.db = db;
		this.classified_ground = pointcloud == null ? db.config.isClassified_ground() : pointcloud.isClassified_ground();
		this.classified_vegetation = pointcloud == null ? db.config.isClassified_vegetation() : pointcloud.isClassified_vegetation();
		this.t = t;
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
			double xmax = rect.getUTMd_max_x_inclusive();
			double ymax = rect.getUTMd_max_y_inclusive();
			Stream<PointTable> pointTables = pointcloud.getPointTables(t, xmin, ymin, xmax, ymax, GeoPointTransformer.FULL_GEOPOINT_SELECTOR);
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

	private static void filterByPolygon(Vec<GeoPoint> bps, Point2d[] polygonPoints, Vec<GeoPoint> pointlist) {
		int polygonPointsLen = polygonPoints.length;
		double[] vx = new double[polygonPointsLen];
		double[] vy = new double[polygonPointsLen];
		for (int i = 0; i < polygonPointsLen; i++) {
			vx[i] = polygonPoints[i].x;
			vy[i] = polygonPoints[i].y;
		}
		for(GeoPoint p : bps) {					
			if(PolygonUtil.wn_PnPolyDirectV2(p.x, p.y, vx, vy) != 0) {
				pointlist.add(p);
			}
		}
	}

	private static void filterByPolygonWithHole(Vec<GeoPoint> bps, Point2d[] polygonPoints, Point2d[] polygonHolePoints, Vec<GeoPoint> pointlist) {
		int polygonPointsLen = polygonPoints.length;
		double[] vx = new double[polygonPointsLen];
		double[] vy = new double[polygonPointsLen];
		for (int i = 0; i < polygonPointsLen; i++) {
			vx[i] = polygonPoints[i].x;
			vy[i] = polygonPoints[i].y;
		}
		int polygonHolePointsLen = polygonHolePoints.length;
		double[] hx = new double[polygonHolePointsLen];
		double[] hy = new double[polygonHolePointsLen];
		for (int i = 0; i < polygonHolePointsLen; i++) {
			hx[i] = polygonHolePoints[i].x;
			hy[i] = polygonHolePoints[i].y;
		}
		for(GeoPoint p : bps) {					
			if(PolygonUtil.wn_PnPolyDirectV2(p.x, p.y, vx, vy) != 0 && PolygonUtil.wn_PnPolyDirectV2(p.x, p.y, hx, hy) == 0) {
				pointlist.add(p);
			}
		}
	}

	private static void filterByPolygonWithHolesDirect(Vec<GeoPoint> bps, Point2d[] polygonPoints, Point2d[][] polygonHolesPoints, Vec<GeoPoint> pointlist) {
		int polygonPointsLen = polygonPoints.length;
		double[] vx = new double[polygonPointsLen];
		double[] vy = new double[polygonPointsLen];
		for (int i = 0; i < polygonPointsLen; i++) {
			vx[i] = polygonPoints[i].x;
			vy[i] = polygonPoints[i].y;
		}

		int polygonHolesPointsLen = polygonHolesPoints.length;
		double[][] hvx = new double[polygonHolesPointsLen][];
		double[][] hvy = new double[polygonHolesPointsLen][];
		for (int polygonHoleIndex = 0; polygonHoleIndex < polygonHolesPointsLen; polygonHoleIndex++) {
			Point2d[] polygonHolePoints = polygonHolesPoints[polygonHoleIndex];
			int len = polygonHolePoints.length;
			double[] hx = new double[len];
			double[] hy = new double[len];
			for (int i = 0; i < len; i++) {
				hx[i] = polygonHolePoints[i].x;
				hy[i] = polygonHolePoints[i].y;
			}
			hvx[polygonHoleIndex] = hx;
			hvy[polygonHoleIndex] = hy;
		}

		POINT_LIST_LOOP: for(GeoPoint p : bps) {					
			if(PolygonUtil.wn_PnPolyDirectV2(p.x, p.y, vx, vy) != 0) {
				for (int polygonHoleIndex = 0; polygonHoleIndex < polygonHolesPointsLen; polygonHoleIndex++) {
					if(PolygonUtil.wn_PnPolyDirectV2(p.x, p.y, hvx[polygonHoleIndex], hvy[polygonHoleIndex]) != 0) {
						continue POINT_LIST_LOOP;
					}
				}
				pointlist.add(p);
			}
		}
	}

	private static void filterByPolygonWithHoles(Vec<GeoPoint> bps, PolygonWithHoles polygonWithHoles, Vec<GeoPoint> pointlist) {
		if(polygonWithHoles.hasNoHoles()) {
			filterByPolygon(bps, polygonWithHoles.polygon, pointlist);
		} else {
			if(polygonWithHoles.holes.length == 1) {
				filterByPolygonWithHole(bps, polygonWithHoles.polygon, polygonWithHoles.holes[0], pointlist);
			} else {
				filterByPolygonWithHolesDirect(bps, polygonWithHoles.polygon, polygonWithHoles.holes, pointlist);
			}
		}
	}

	private static void filterByPolygons(Vec<GeoPoint> bps, Point2d[][] polygonsPoints, Vec<GeoPoint> pointlist) {
		int polgygonsLen = polygonsPoints.length;
		double[][] cvx = new double[polgygonsLen][];
		double[][] cvy = new double[polgygonsLen][];
		for (int polygonIndex = 0; polygonIndex < polgygonsLen; polygonIndex++) {
			Point2d[] polygonPoints = polygonsPoints[polygonIndex];
			int len = polygonPoints.length;
			double[] vx = new double[len];
			double[] vy = new double[len];
			for (int i = 0; i < len; i++) {
				vx[i] = polygonPoints[i].x;
				vy[i] = polygonPoints[i].y;
			}
			cvx[polygonIndex] = vx;
			cvy[polygonIndex] = vy;
		}	

		POINT_LIST_LOOP: for(GeoPoint p : bps) {
			for (int polygonIndex = 0; polygonIndex < polgygonsLen; polygonIndex++) {
				if(PolygonUtil.wn_PnPolyDirectV2(p.x, p.y, cvx[polygonIndex], cvy[polygonIndex]) != 0) {
					pointlist.add(p);
					continue POINT_LIST_LOOP;
				}
			}
		}
	}

	private static void filterByPolygonsWithHole(Vec<GeoPoint> bps, Point2d[][] polygonsPoints, Point2d[][] polygonsHolePoints, Vec<GeoPoint> pointlist) {

		int polgygonsLen = polygonsPoints.length;
		double[][] cvx = new double[polgygonsLen][];
		double[][] cvy = new double[polgygonsLen][];
		for (int polygonIndex = 0; polygonIndex < polgygonsLen; polygonIndex++) {
			Point2d[] polygonPoints = polygonsPoints[polygonIndex];
			int len = polygonPoints.length;
			double[] vx = new double[len];
			double[] vy = new double[len];
			for (int i = 0; i < len; i++) {
				vx[i] = polygonPoints[i].x;
				vy[i] = polygonPoints[i].y;
			}
			cvx[polygonIndex] = vx;
			cvy[polygonIndex] = vy;
		}

		int polygonsHolePointsLen = polygonsHolePoints.length;
		double[][] hvx = new double[polygonsHolePointsLen][];
		double[][] hvy = new double[polygonsHolePointsLen][];
		for (int polygonHoleIndex = 0; polygonHoleIndex < polygonsHolePointsLen; polygonHoleIndex++) {
			Point2d[] polygonHolePoints = polygonsPoints[polygonHoleIndex];
			if(polygonHolePoints != null) {
				int len = polygonHolePoints.length;
				double[] hx = new double[len];
				double[] hy = new double[len];
				for (int i = 0; i < len; i++) {
					hx[i] = polygonHolePoints[i].x;
					hy[i] = polygonHolePoints[i].y;
				}
				hvx[polygonHoleIndex] = hx;
				hvy[polygonHoleIndex] = hy;
			}
		}		

		POINT_LIST_LOOP: for(GeoPoint p : bps) {
			for (int polygonIndex = 0; polygonIndex < polgygonsLen; polygonIndex++) {
				if(PolygonUtil.wn_PnPolyDirectV2(p.x, p.y, cvx[polygonIndex], cvy[polygonIndex]) != 0 && (hvx == null || PolygonUtil.wn_PnPolyDirectV2(p.x, p.y, hvx[polygonIndex], hvy[polygonIndex]) == 0)) {
					pointlist.add(p);
					continue POINT_LIST_LOOP;
				}
			}
		}
	}

	private static void filterByPolygonsWithHoles(Vec<GeoPoint> bps, PolygonWithHoles[] polygonsWithHoles, Vec<GeoPoint> pointlist) {

		int polygonsWithHolesLen = polygonsWithHoles.length;
		double[][] cvx = new double[polygonsWithHolesLen][];
		double[][] cvy = new double[polygonsWithHolesLen][];
		for (int polygonIndex = 0; polygonIndex < polygonsWithHolesLen; polygonIndex++) {
			Point2d[] polygonPoints = polygonsWithHoles[polygonIndex].polygon;
			int len = polygonPoints.length;
			double[] vx = new double[len];
			double[] vy = new double[len];
			for (int i = 0; i < len; i++) {
				vx[i] = polygonPoints[i].x;
				vy[i] = polygonPoints[i].y;
			}
			cvx[polygonIndex] = vx;
			cvy[polygonIndex] = vy;
		}
		
		double[][][] hvx = new double[polygonsWithHolesLen][][];
		double[][][] hvy = new double[polygonsWithHolesLen][][];
		for (int polygonIndex = 0; polygonIndex < polygonsWithHolesLen; polygonIndex++) {
			Point2d[][] holesPoints = polygonsWithHoles[polygonIndex].holes;
			if(holesPoints != null) {
				int holesPointsLen = holesPoints.length;
				double[][] phx = new double[holesPointsLen][];
				double[][] phy = new double[holesPointsLen][];
				for (int holeIndex = 0; holeIndex < holesPointsLen; holeIndex++) {
					Point2d[] ringPoints = holesPoints[holeIndex];
					int len = ringPoints.length;
					double[] hx = new double[len];
					double[] hy = new double[len];
					for (int i = 0; i < len; i++) {
						hx[i] = ringPoints[i].x;
						hy[i] = ringPoints[i].y;
					}
					phx[holeIndex] = hx;
					phy[holeIndex] = hy;
				}
				hvx[polygonIndex] = phx;
				hvy[polygonIndex] = phy;
			}
		}

		POINT_LIST_LOOP: for(GeoPoint p : bps) {
			POLYGON_LOOP: for (int polygonIndex = 0; polygonIndex < polygonsWithHolesLen; polygonIndex++) {
				if(PolygonUtil.wn_PnPolyDirectV2(p.x, p.y, cvx[polygonIndex], cvy[polygonIndex]) != 0) {
					double[][] hx = hvx[polygonIndex];
					if(hx != null) {
						int holesPointsLen = hx.length;
						double[][] hy = hvy[polygonIndex];
						for (int holeIndex = 0; holeIndex < holesPointsLen; holeIndex++) {
							if(PolygonUtil.wn_PnPolyDirectV2(p.x, p.y, hx[holeIndex], hy[holeIndex]) != 0) {
								continue POLYGON_LOOP;
							}
						}
					}
					pointlist.add(p);
					continue POINT_LIST_LOOP;
				}
			}
		}
	}

	private static void filterByRoi(Vec<GeoPoint> bps, PolygonWithHoles[] polygonsWithHoles, Vec<GeoPoint> pointlist) {
		if(polygonsWithHoles.length == 1) {
			filterByPolygonWithHoles(bps, polygonsWithHoles[0], pointlist);
		} else {
			int maxHoles = 0;
			for(PolygonWithHoles polygonWithHoles : polygonsWithHoles) {
				if(polygonWithHoles.hasHoles()) {
					if(maxHoles < polygonWithHoles.holes.length) {
						maxHoles = polygonWithHoles.holes.length;
					}
				}
			}
			if(maxHoles == 0) {
				Point2d[][] polygons = new Point2d[polygonsWithHoles.length][];
				for (int i = 0; i < polygonsWithHoles.length; i++) {
					polygons[i] = polygonsWithHoles[i].polygon;
					if(polygonsWithHoles[i].holes != null) {
						throw new RuntimeException("hole error");
					}
				}
				filterByPolygons(bps, polygons, pointlist);
			} else if(maxHoles == 1) {
				Point2d[][] polygons = new Point2d[polygonsWithHoles.length][];
				Point2d[][] polygonsHole = new Point2d[polygonsWithHoles.length][];
				for (int i = 0; i < polygonsWithHoles.length; i++) {
					polygons[i] = polygonsWithHoles[i].polygon;
					if(polygonsWithHoles[i].hasHoles()) {
						if(polygonsWithHoles[i].holes.length != 1) {
							throw new RuntimeException("hole error");
						}
						polygonsHole[i] = polygonsWithHoles[i].holes[0];
					}
				}
				filterByPolygonsWithHole(bps, polygons, polygonsHole, pointlist);				
			} else {
				filterByPolygonsWithHoles(bps, polygonsWithHoles, pointlist);
			}
		}
	}

	public synchronized Vec<GeoPoint> get_regionPoints(boolean load_bordered_bbox) {
		Vec<GeoPoint> bps = get_bboxPoints(load_bordered_bbox);
		if(regionPoints == null) {
			if(region.isBbox()) {
				regionPoints = bps;
			} else {
				Vec<GeoPoint> pointlist = new Vec<>();			
				filterByRoi(bps, region.polygons, pointlist);
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
			//Logger.info("sort "+zs.size());
			zs.sort(GeoPoint.Z_COMPARATOR_SAFE);
			//Logger.info(Timer.stop("get_sortedRegionHeightPoints sort"));
			sortedRegionHeightPoints = zs;
			//Logger.info("sorted");
			if(zs.size() > 0) {
				//Logger.info("get_sortedRegionHeightPoints " + zs.size() + "  min " + zs.get(0) + "  max " + zs.get(zs.size()-1));
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
					if(p.z >= GROUND_HEIGHT) {
						zs.add(p.z);
					}
				}				
			}
			sortedCanopyHeights = zs.toArray();
		}
		return sortedCanopyHeights;
	}

	/**
	 * just vegetation points, no ground points, no > 98% points
	 * @return
	 */
	public synchronized GeoPoint[] get_sortedCanopyHeightPoints() {
		if(sortedCanopyHeightPoints == null) {
			Vec<GeoPoint> points = get_sortedRegionHeightPoints();
			int border = points.size();
			Vec<GeoPoint> zs = new Vec<GeoPoint>(border);
			if(border>=100) {
				border = (border*98)/100;
			}
			if(classified_vegetation) {
				for (int i = 0; i < border; i++) {
					GeoPoint p = points.get(i);
					if(p.isVegetaion()) {
						zs.add(p);
					}
				}
			} else {
				for (int i = 0; i < border; i++) {
					GeoPoint p = points.get(i);
					if(p.z >= GROUND_HEIGHT) {
						zs.add(p);
					}
				}				
			}
			sortedCanopyHeightPoints = zs.toArray(GeoPoint[]::new);
		}
		return sortedCanopyHeightPoints;
	}

	public synchronized int getRegionPulseCount() {
		if(regionPulseCount == Integer.MIN_VALUE) {
			regionPulseCount = get_regionPoints().count(GeoPoint::isFirstReturn);
		}
		return regionPulseCount;
	}

	public synchronized int getBboxPulseCount() {
		if(bboxPulseCount == Integer.MIN_VALUE) {
			bboxPulseCount = get_bboxPoints().count(GeoPoint::isFirstReturn);
		}
		return bboxPulseCount;
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