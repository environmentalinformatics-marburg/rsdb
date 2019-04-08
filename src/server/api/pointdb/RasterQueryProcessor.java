package server.api.pointdb;

import java.io.IOException;
import java.util.Arrays;

import org.json.JSONWriter;

import pointdb.PointDB;
import pointdb.base.GeoPoint;
import pointdb.base.Point;
import pointdb.base.Rect;
import pointdb.processing.geopoint.PointGrid;
import pointdb.processing.geopoint.RasterGrid;
import pointdb.processing.geopoint.RasterSubGrid;
import pointdb.processing.tilepoint.TilePointProducer;
import util.collections.vec.Vec;

public class RasterQueryProcessor {
	//private static final Logger log = LogManager.getLogger();
	
	public static class TypeEntry {
		public final String name;
		public final String title;
		public final String data_type;
		public TypeEntry(String name, String title) {
			this.name = name;
			this.title = title;
			this.data_type = "basic_raster";
		}
		public TypeEntry(String name, String title, String data_type) {
			this.name = name;
			this.title = title;
			this.data_type = data_type;
		}
		public void writeJSON(JSONWriter json) {
			json.object();
			json.key("name");
			json.value(name);
			json.key("title");
			json.value(title);
			json.key("data_type");
			json.value(data_type);
			json.endObject();
		}
	}
	
	public static final TypeEntry[] TYPES = new TypeEntry[] {
			new TypeEntry("DTM", "Digital Terrain Model"),
			new TypeEntry("DTM_slope", "Slope of DTM"),
			new TypeEntry("DTM_aspect", "Aspect of DTM (experimental)"),
			new TypeEntry("DTM_roughness", "Roughness of DTM"),
			new TypeEntry("DTM_TRI", "TRI of DTM (Terrain Ruggedness Index)"),
			new TypeEntry("DTM_TPI", "TPI of DTM (Topographic Position Index)"),
			new TypeEntry("DTM_surface_area", "surface area of DTM"),
			new TypeEntry("DSM", "Digital Surface Model"),
			new TypeEntry("DSM_slope", "Slope of DSM"),
			new TypeEntry("DSM_aspect", "Aspect of DSM (experimental)"),
			new TypeEntry("DSM_roughness", "Roughness of DSM"),
			new TypeEntry("DSM_TRI", "TRI of DSM (cf. Terrain Ruggedness Index)"),
			new TypeEntry("DSM_TPI", "TPI of DSM (cf. Topographic Position Index)"),
			new TypeEntry("DSM_surface_area", "surface area of DSM"),
			new TypeEntry("CHM", "Canopy Height Model"),
			new TypeEntry("CHM_slope", "Slope of CHM"),
			new TypeEntry("CHM_aspect", "Aspect of CHM (experimental)"),
			new TypeEntry("CHM_roughness", "Roughness of CHM"),
			new TypeEntry("CHM_TRI", "TRI of CHM (cf. Terrain Ruggedness Index)"),
			new TypeEntry("CHM_TPI", "TPI of CHM (cf. Topographic Position Index)"),
			new TypeEntry("CHM_surface_area", "surface area of CHM"),
			new TypeEntry("watershed", "Segmentation", "index_raster"),
			new TypeEntry("voxel", "Voxel Map", "multi_raster"),			
	};
	
	private static final int INITIAL_CELL_CAPACITY = 12;
	
	private final PointDB db;
	
	public static void writeProcessingTypesJSON(JSONWriter json) {
		json.array();
		for(TypeEntry type:TYPES) {
			type.writeJSON(json);
		}
		json.endArray();
	}

	public RasterQueryProcessor(PointDB db) {
		this.db = db;
	}
	
	public RasterSubGrid[] process(Rect requestRect, String processingType) throws IOException {
		switch(processingType) {
		case "dtm": {
			RasterSubGrid dtm = produce_dtm(requestRect);
			dtm.meta.put("name", "dtm");
			return new RasterSubGrid[]{dtm};	
		}
		case "dtm_slope": {
			RasterSubGrid dtm_slope = produce_dtm(requestRect).toSlope();
			dtm_slope.meta.put("name", "dtm_slope");
			return new RasterSubGrid[]{dtm_slope};	
		}
		case "dtm_aspect": {
			RasterSubGrid r = produce_dtm(requestRect).toAspect();
			r.meta.put("name", processingType);
			return new RasterSubGrid[]{r};	
		}
		case "dtm_roughness": {
			RasterSubGrid dtm_roughness = produce_dtm(requestRect).toRoughness();
			dtm_roughness.meta.put("name", "dtm_roughness");
			return new RasterSubGrid[]{dtm_roughness};	
		}
		case "dtm_tri": {
			RasterSubGrid r = produce_dtm(requestRect).toTerrainRuggednessIndex();
			r.meta.put("name", processingType);
			return new RasterSubGrid[]{r};	
		}
		case "dtm_tpi": {
			RasterSubGrid r = produce_dtm(requestRect).toTopographicPositionIndex();
			r.meta.put("name", processingType);
			return new RasterSubGrid[]{r};	
		}
		case "dsm": {
			RasterSubGrid dsm = produce_dsm(requestRect);
			dsm.meta.put("name", "dsm");
			return new RasterSubGrid[]{dsm}; 
		}
		case "dsm_slope": {
			RasterSubGrid dsm_slope = produce_dsm(requestRect).toSlope();
			dsm_slope.meta.put("name", "dsm_slope");
			return new RasterSubGrid[]{dsm_slope};	
		}
		case "dsm_aspect": {
			RasterSubGrid r = produce_dsm(requestRect).toAspect();
			r.meta.put("name", processingType);
			return new RasterSubGrid[]{r};	
		}
		case "dsm_roughness": {
			RasterSubGrid dsm_roughness = produce_dsm(requestRect).toRoughness();
			dsm_roughness.meta.put("name", "dsm_roughness");
			return new RasterSubGrid[]{dsm_roughness};	
		}
		case "dsm_tri": {
			RasterSubGrid r = produce_dsm(requestRect).toTerrainRuggednessIndex();
			r.meta.put("name", processingType);
			return new RasterSubGrid[]{r};	
		}
		case "dsm_tpi": {
			RasterSubGrid r = produce_dsm(requestRect).toTopographicPositionIndex();
			r.meta.put("name", processingType);
			return new RasterSubGrid[]{r};	
		}
		case "chm": {
			RasterSubGrid chm = produce_chm(requestRect);
			chm.meta.put("name", "chm");
			return new RasterSubGrid[]{chm};
		}
		case "chm_slope": {
			RasterSubGrid chm_slope = produce_chm(requestRect).toSlope();
			chm_slope.meta.put("name", "chm_slope");
			return new RasterSubGrid[]{chm_slope};
		}
		case "chm_aspect": {
			RasterSubGrid r = produce_chm(requestRect).toAspect();
			r.meta.put("name", processingType);
			return new RasterSubGrid[]{r};	
		}
		case "chm_roughness": {
			RasterSubGrid chm_roughness = produce_chm(requestRect).toRoughness();
			chm_roughness.meta.put("name", "chm_roughness");
			return new RasterSubGrid[]{chm_roughness};	
		}
		case "chm_tri": {
			RasterSubGrid r = produce_chm(requestRect).toTerrainRuggednessIndex();
			r.meta.put("name", processingType);
			return new RasterSubGrid[]{r};	
		}
		case "chm_tpi": {
			RasterSubGrid r = produce_chm(requestRect).toTopographicPositionIndex();
			r.meta.put("name", processingType);
			return new RasterSubGrid[]{r};	
		}
		case "watershed": {
			RasterSubGrid ws = produce_chm(requestRect);
			new Watershed(ws).run();
			ws.meta.put("name", "watershed");
			return new RasterSubGrid[]{ws}; 
		}
		case "voxel": {
			Rect qRect = requestRect.withBorderUTM(16).outerMeterRect();
			Vec<GeoPoint> pointlist = db.tilePointProducer(qRect).toGeoPointProducer().toList();
			VoxelGenerator voxel_gen = new VoxelGenerator(qRect, pointlist);			
			RasterSubGrid[] rasters = Arrays.stream(voxel_gen.get()).map(r->{
				RasterSubGrid sg = r.subGrid(requestRect);
				sg.meta.put("name", "voxel");
				return sg;
			}).toArray(RasterSubGrid[]::new);
			return rasters;
		}
		case "dsm_robust": {
			RasterSubGrid dsm_robust = produce_dsm_robust(requestRect);
			dsm_robust.meta.put("name", "dsm_robust");
			return new RasterSubGrid[]{dsm_robust}; 
		}
		case "dtm_surface_area": {
			RasterSubGrid r = produce_dtm(requestRect).toSurfaceArea();
			r.meta.put("name", processingType);
			return new RasterSubGrid[]{r};	
		}
		case "dsm_surface_area": {
			RasterSubGrid r = produce_dsm(requestRect).toSurfaceArea();
			r.meta.put("name", processingType);
			return new RasterSubGrid[]{r};	
		}
		case "chm_surface_area": {
			RasterSubGrid r = produce_chm(requestRect).toSurfaceArea();
			r.meta.put("name", processingType);
			return new RasterSubGrid[]{r};	
		}
		default:
			throw new RuntimeException("unknown type: "+processingType);
		}
	}
	
	private RasterSubGrid produce_dtm(Rect requestRect) {
		if(db.config.isClassified_ground()) {
			Rect qRect = requestRect.withBorderUTM(16).outerMeterRect();
			Vec<GeoPoint> points = db.tilePointProducer(qRect).filter(Point::isGround).toGeoPointProducer().toList();
			RasterGenerator rasterGenerator = new RasterGenerator(qRect, points);
			rasterGenerator.run();
			RasterGrid rasterGrid = rasterGenerator.rasterGrid;
			return rasterGrid.subGrid(requestRect);
		} else {
			Rect qRect = requestRect.withBorderUTM(16).outerMeterRect();
			Vec<GeoPoint> pointlist = db.tilePointProducer(qRect).filter(Point::isLastReturn).toGeoPointProducer().toList();
			DTM2_generator dtm2_gen = new DTM2_generator(qRect, pointlist);
			RasterGrid rasterGrid = dtm2_gen.get();
			return rasterGrid.subGrid(requestRect);
		}		
	}
	
	private RasterSubGrid produce_dsm(Rect requestRect) {
		Rect rawQueryRect = requestRect.withBorderUTM(PointGrid.window_size).outerMeterRect();
		TilePointProducer producer = db.tilePointProducer(rawQueryRect);
		if(db.config.isClassified_ground() && db.config.isClassified_vegetation()) {
			producer = producer.filter(Point::isEntity);
		}			
		PointGrid pointGrid = db.tilePointProducer(rawQueryRect).toGeoPointProducer().toPointGrid(rawQueryRect, INITIAL_CELL_CAPACITY);
		RasterGrid rasterGrid = DSM_generator.generate(pointGrid);
		RasterSubGrid rasterSubGrid = rasterGrid.subGrid(requestRect);
		return rasterSubGrid; 		
	}
	
	private RasterSubGrid produce_dsm_robust(Rect requestRect) {
		Rect rawQueryRect = requestRect.withBorderUTM(PointGrid.window_size).outerMeterRect();
		TilePointProducer producer = db.tilePointProducer(rawQueryRect);
		if(db.config.isClassified_vegetation()) {
			producer = producer.filter(Point::isEntity);
		}
		Vec<GeoPoint> pointList = producer.toGeoPointProducer().toList();
		DSM2_generator dsm2_gen = new DSM2_generator(rawQueryRect, pointList);
		RasterGrid rasterGrid = dsm2_gen.get();
		RasterSubGrid rasterSubGrid = rasterGrid.subGrid(requestRect);
		return rasterSubGrid; 		
	}
	
	private RasterSubGrid produce_chm(Rect requestRect) {
		RasterSubGrid dtm = produce_dtm(requestRect);
		RasterSubGrid chm = produce_dsm(requestRect);
		chm.minus_zero(dtm);
		return chm; 		
	}

}
