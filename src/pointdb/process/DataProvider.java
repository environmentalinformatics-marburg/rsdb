package pointdb.process;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pointdb.PointDB;
import pointdb.base.GeoPoint;
import pointdb.base.Rect;
import pointdb.processing.geopoint.Normalise;
import pointdb.processing.geopoint.RasterGrid;
import pointdb.processing.geopoint.RasterSubGrid;
import pointdb.subsetdsl.Region;
import server.api.pointdb.DSM2_generator;
import server.api.pointdb.DTM2_generator;
import server.api.pointdb.RasterGenerator;
import util.collections.vec.Vec;

public class DataProvider {
	static final Logger log = LogManager.getLogger();

	public DataProvider2 dp;

	public final  Rect transformed_rect; // transformed bbox of region

	private Vec<GeoPoint> transformedPoints = null; // polygon aware
	private Vec<GeoPoint> extremesNormalisedPoints = null;  // polygon aware
	private Vec<GeoPoint> groundNormalisedPoints = null; // polygon aware
	private RasterSubGrid dtm = null; // bbox of region
	private RasterSubGrid dsm = null; // bbox of region
	private RasterSubGrid chm = null; // bbox of region
	private RasterSubGrid dtm_slope = null; // bbox of region
	private RasterSubGrid dtm_aspect = null; // bbox of region

	public DataProvider(PointDB db, Region region, DataProvider2 dp) {
		this.dp = dp;
		this.transformed_rect = dp.bbox_rect.transform(-dp.bbox_rect.getUTMd_min_x(), -dp.bbox_rect.getUTMd_min_y());
	}

	public synchronized Vec<GeoPoint> getTransformedPoints() {
		if(transformedPoints == null) {
			double dx = dp.bbox_rect.getUTMd_min_x();
			double dy = dp.bbox_rect.getUTMd_min_y();
			transformedPoints = dp.get_regionPoints().map(p->GeoPoint.of(p.x - dx, p.y - dy, p));
		}			
		return transformedPoints;			
	}

	public synchronized Vec<GeoPoint> getExtremesNormalisedPoints() {
		if(extremesNormalisedPoints == null) {
			Normalise normalise = new Normalise();
			normalise.normalise_origin = true; //not applied	
			normalise.normalise_extremes = true;
			normalise.normalise_ground = false;
			extremesNormalisedPoints = normalise.optional_normalise(getTransformedPoints());
		}
		return extremesNormalisedPoints;
	}

	public synchronized Vec<GeoPoint> getGroundNormalisedPoints() {
		if(groundNormalisedPoints == null) {
			Normalise normalise = new Normalise();
			normalise.normalise_origin = true; //not applied	
			normalise.normalise_extremes = true;
			normalise.normalise_ground = true;
			groundNormalisedPoints = normalise.optional_normalise(getTransformedPoints());
		}
		return groundNormalisedPoints;
	}

	public synchronized RasterSubGrid getDTM() {
		if(dtm == null) {
			if(dp.classified_ground) {
				Vec<GeoPoint> pointlist = dp.get_bordered_bboxPoints().filter(GeoPoint::isGround);				
				RasterGenerator dtmGenerator = new RasterGenerator(dp.bordered_bbox_rect, pointlist);
				dtmGenerator.run();
				dtm = dtmGenerator.rasterGrid.subGrid(dp.bbox_rect);
			} else {
				Vec<GeoPoint> pointlist = dp.get_bordered_bboxPoints().filter(GeoPoint::isLastReturn);	
				DTM2_generator dtm2_gen = new DTM2_generator(dp.bordered_bbox_rect, pointlist);
				dtm = dtm2_gen.get().subGrid(dp.bbox_rect);
			}
		}
		return dtm;
	}

	public synchronized RasterSubGrid getDSM() {
		if(dsm == null) {			
			Vec<GeoPoint> p = getExtremesNormalisedPoints();			
			DSM2_generator dsm2_gen = new DSM2_generator(transformed_rect, p);
			dsm = dsm2_gen.get();
		}
		return dsm;
	}

	public synchronized RasterSubGrid getCHM() {
		if(chm == null) {
			Vec<GeoPoint> pointlist = dp.get_bordered_bboxPoints();

			Normalise normalise = new Normalise();
			normalise.normalise_origin = false; //not applied	
			normalise.normalise_extremes = true;
			normalise.normalise_ground = false;
			Vec<GeoPoint> np = normalise.optional_normalise(pointlist);			

			DSM2_generator dsm2_gen = new DSM2_generator(dp.bordered_bbox_rect, np);
			RasterGrid rasterGrid_DSM = dsm2_gen.get();
			DTM2_generator dtm2_gen = new DTM2_generator(dp.bordered_bbox_rect, pointlist);
			RasterGrid rasterGrid_DTM = dtm2_gen.get();
			RasterGrid rasterGrid_CHM = rasterGrid_DSM;		
			rasterGrid_CHM.minus_zero(rasterGrid_DTM);
			chm = rasterGrid_CHM.subGrid(dp.bbox_rect);
		}
		return chm;
	}

	public synchronized RasterSubGrid getDTM_slope() {
		if(dtm_slope == null) {
			dtm = getDTM();			
			dtm_slope = dtm.toSlope();
		}
		return dtm_slope;
	}

	public synchronized RasterSubGrid getDTM_aspect() {
		if(dtm_aspect == null) {
			dtm = getDTM();			
			dtm_aspect = dtm.toAspect();
		}
		return dtm_aspect;
	}

	public void close() {
		dp = null;
		transformedPoints = null;
		extremesNormalisedPoints = null;
		groundNormalisedPoints = null;
		dtm = null;
		dsm = null;
		chm = null;
		dtm_slope = null;
		dtm_aspect = null;		
	}
}