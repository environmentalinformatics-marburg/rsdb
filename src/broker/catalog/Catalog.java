package broker.catalog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Stream;


import org.tinylog.Logger;
import org.eclipse.jetty.server.UserIdentity;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import broker.Associated;
import broker.Broker;
import broker.Informal;
import broker.StructuredAccess;
import broker.acl.ACL;
import broker.acl.AclUtil;
import broker.acl.EmptyACL;
import pointcloud.P2d;
import pointcloud.PointCloud;
import pointdb.PointDB;
import pointdb.base.PdbConst;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.tile.TilePixel;
import rasterunit.TileKey;
import util.Range2d;
import util.Timer;
import vectordb.VectorDB;
import voxeldb.VoxelDB;

public class Catalog {
	

	private static final Path CATALOG_FILENAME = Paths.get("catalog/catalog.json");
	private static final String CATALOG_TEMP_PREFIX = "catalog/catalog_temp";
	private static final SpatialReference CATALOG_SPATIAL_REFERENCE = new SpatialReference("");

	static {
		CATALOG_SPATIAL_REFERENCE.ImportFromProj4("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
	}

	private final Broker broker;
	private Map<CatalogKey, CatalogEntry> map = new LinkedHashMap<>();

	public Catalog(Broker broker) {
		this.broker = broker;
		if(CATALOG_FILENAME.toFile().exists()) {
			loadFromFile();
			updateCatalog();
		} else {
			rebuildCatalog();
		}
	}

	private CatalogEntry generateCatalogEntryRasterDB(RasterDB rasterdb, boolean generateHull) {
		if(rasterdb == null) {
			return null;
		}
		try {
			double[][] points = null;
			if(generateHull && rasterdb.hasRasterUnit() && !rasterdb.rasterUnit().isEmpty()) {
				GeoReference ref = rasterdb.ref();
				SpatialReference src = new SpatialReference("");
				if(ref.has_proj4()) {
					src.ImportFromProj4(ref.proj4);
				} else {
					ACL acl_ownermod = AclUtil.union(rasterdb.getACL_owner(), rasterdb.getACL_mod());
					Logger.warn("no poj4: "+rasterdb.config.getName());
					return generateCatalogEntry(rasterdb.config.getName(), CatalogKey.TYPE_RASTERDB, null, rasterdb.associated, rasterdb.getACL(), acl_ownermod, rasterdb.informal(), null);
				}

				CoordinateTransformation ct = CoordinateTransformation.CreateCoordinateTransformation(src, CATALOG_SPATIAL_REFERENCE);

				final int polygon = 0;
				final int fastPoly = 1;
				final int rect = 2;

				final int hullType = polygon; 
				switch(hullType) {
				case polygon: {
					Range2d localRange = rasterdb.getLocalRange(false);
					if(localRange != null) {
						double pxmin = ref.pixelXToGeo(localRange.xmin);
						double pymin = ref.pixelYToGeo(localRange.ymin);
						double pxmax = ref.pixelXToGeo(localRange.xmax + 1);
						double pymax = ref.pixelYToGeo(localRange.ymax + 1);
						Set<TileKey> keys = rasterdb.rasterUnit().tileKeysReadonly();
						Coordinate[] coordinates = new Coordinate[keys.size() * 4];
						int i = 0;
						for(TileKey key:keys) {
							double xmin = ref.pixelXToGeo(TilePixel.tileToPixel(key.x));
							double ymin = ref.pixelYToGeo(TilePixel.tileToPixel(key.y));
							double xmax = ref.pixelXToGeo(TilePixel.tileToPixelMax(key.x));
							double ymax = ref.pixelYToGeo(TilePixel.tileToPixelMax(key.y));
							if(xmin < pxmin) {
								xmin = pxmin;
							}
							if(ymin < pymin) {
								ymin = pymin;
							}
							if(xmax > pxmax) {
								xmax = pxmax;
							}
							if(ymax > pymax) {
								ymax = pymax;
							}

							coordinates[i++] = new Coordinate(xmin, ymin);
							coordinates[i++] = new Coordinate(xmax, ymin);
							coordinates[i++] = new Coordinate(xmin, ymax);
							coordinates[i++] = new Coordinate(xmax, ymax);
						}
						points = generateConvexHullPoints(coordinates);
					}
					break;
				}
				case fastPoly: {
					Range2d localRange = rasterdb.getLocalRange(false);
					if(localRange != null) {
						double xmin = ref.pixelXToGeo(localRange.xmin);
						double ymin = ref.pixelYToGeo(localRange.ymin);					
						double xmax = ref.pixelXToGeo(localRange.xmax);
						double ymax = ref.pixelYToGeo(localRange.ymax);
						double xmid = (xmin + xmax) / 2;
						double ymid = (ymin + ymax) / 2;					
						double xq1 = (xmin + xmid) / 2;
						double xq3 = (xmid + xmax) / 2;
						double yq1 = (ymin + ymid) / 2;
						double yq3 = (ymid + ymax) / 2;
						points = new double[17][2];
						points[0][0] = xmin; points[0][1] = ymin;
						points[1][0] = xq1; points[1][1] = ymin;
						points[2][0] = xmid; points[2][1] = ymin;
						points[3][0] = xq3; points[3][1] = ymin;
						points[4][0] = xmax; points[4][1] = ymin;

						points[5][0] = xmax; points[5][1] = yq1;
						points[6][0] = xmax; points[6][1] = ymid;
						points[7][0] = xmax; points[7][1] = yq3;

						points[8][0] = xmax; points[8][1] = ymax;
						points[9][0] = xq3; points[9][1] = ymax;
						points[10][0] = xmid; points[10][1] = ymax;
						points[11][0] = xq1; points[11][1] = ymax;
						points[12][0] = xmin; points[12][1] = ymax;

						points[13][0] = xmin; points[13][1] = yq3;
						points[14][0] = xmin; points[14][1] = ymid;
						points[15][0] = xmin; points[15][1] = yq1;

						points[16][0] = xmin; points[16][1] = ymin;
					}
					break;
				}
				case rect: {
					Range2d localRange = rasterdb.getLocalRange(false);
					if(localRange != null) {
						double xmin = ref.pixelXToGeo(localRange.xmin);
						double ymin = ref.pixelYToGeo(localRange.ymin);					
						double xmax = ref.pixelXToGeo(localRange.xmax);
						double ymax = ref.pixelYToGeo(localRange.ymax);
						points = new double[5][2];
						points[0][0] = xmin; points[0][1] = ymin;
						points[1][0] = xmax; points[1][1] = ymin;
						points[2][0] = xmax; points[2][1] = ymax;
						points[3][0] = xmin; points[3][1] = ymax;					
						points[4][0] = xmin; points[4][1] = ymin;
					}
					break;
				}
				default:
					Logger.warn("unknown hullType");
					return null;
				}
				ct.TransformPoints(points);
			}
			return generateCatalogEntry(rasterdb.config.getName(), CatalogKey.TYPE_RASTERDB, points, rasterdb.associated, rasterdb.getACL(), rasterdb.getACL_mod(), rasterdb.informal(), null);			
		} catch(Exception e) {
			e.printStackTrace();
			Logger.warn(e);
			return generateCatalogEntry(rasterdb.config.getName(), CatalogKey.TYPE_RASTERDB, null, rasterdb.associated, rasterdb.getACL(), rasterdb.getACL_mod(), rasterdb.informal(), null);
		}
	}

	private CatalogEntry generateCatalogEntryPointDB(PointDB pointdb, boolean generateHull) {
		if(pointdb == null) {
			return null;
		}
		Associated associated = new Associated();
		associated.setPoi_groups(pointdb.config.getPoiGroupNames());
		associated.setRoi_groups(pointdb.config.getRoiGroupNames());
		associated.setRasterDB(pointdb.config.getRasterDB());
		try {
			if(pointdb.tileMetaMap.isEmpty()) {
				return null;
			}			
			double[][] points = null;
			if(generateHull) {
				SpatialReference src = new SpatialReference("");
				String proj4 = pointdb.config.getProj4();
				if(proj4.isEmpty()) {
					Logger.warn("no poj4: "+pointdb.config.name);
					return generateCatalogEntry(pointdb.config.name, CatalogKey.TYPE_POINTDB, null, associated, pointdb.config.getAcl(), EmptyACL.ADMIN, pointdb.informal(), null);
				} else {
					src.ImportFromProj4(proj4);				
				}
				CoordinateTransformation ct = CoordinateTransformation.CreateCoordinateTransformation(src, CATALOG_SPATIAL_REFERENCE);

				Logger.info("generateCatalogEntryPointDB ...");
				Timer.start("generateCatalogEntryPointDB");
				Logger.info("generateCatalogEntryPointDB pointdb.tileMetaMap.keySet() ...");
				Timer.start("pointdb.tileMetaMap.keySet()");
				NavigableSet<pointdb.base.TileKey> keys = pointdb.tileMetaMap.keySet();
				Logger.info(Timer.stop("pointdb.tileMetaMap.keySet()"));
				Logger.info("generateCatalogEntryPointDB keys.size() ...");
				Timer.start("keys.size()");
				int keyCount = keys.size();
				Coordinate[] coordinates = new Coordinate[keyCount * 4];
				Logger.info(Timer.stop("keys.size()")+"  "+keyCount+" keys");
				Logger.info("generateCatalogEntryPointDB loop ...");
				Timer.start("loop");
				int i = 0;				
				for(pointdb.base.TileKey key: keys) {
					double xmin = key.x;
					double ymin = key.y;
					double xmax = xmin + PdbConst.UTM_TILE_SIZE;
					double ymax = ymin + PdbConst.UTM_TILE_SIZE;
					coordinates[i++] = new Coordinate(xmin, ymin);
					coordinates[i++] = new Coordinate(xmax, ymin);
					coordinates[i++] = new Coordinate(xmin, ymax);
					coordinates[i++] = new Coordinate(xmax, ymax);
				}
				Logger.info(Timer.stop("loop"));
				Logger.info("generateCatalogEntryPointDB generateConvexHullPoints ...");
				Timer.start("generateConvexHullPoints");
				points = generateConvexHullPoints(coordinates);
				Logger.info(Timer.stop("generateConvexHullPoints"));
				Logger.info("generateCatalogEntryPointDB ct.TransformPoints ...");
				Timer.start("ct.TransformPoints");
				ct.TransformPoints(points);
				Logger.info(Timer.stop("ct.TransformPoints"));
				Logger.info(Timer.stop("generateCatalogEntryPointDB"));
			}
			return generateCatalogEntry(pointdb.config.name, CatalogKey.TYPE_POINTDB, points, associated, pointdb.config.getAcl(), EmptyACL.ADMIN, pointdb.informal(), null);			
		} catch(Exception e) {
			e.printStackTrace();
			Logger.warn(e);
			return generateCatalogEntry(pointdb.config.name, CatalogKey.TYPE_POINTDB, null, associated, pointdb.config.getAcl(), EmptyACL.ADMIN, pointdb.informal(), null);	
		}
	}

	private CatalogEntry generateCatalogEntryPointcloud(PointCloud pointcloud, boolean generateHull) {
		if(pointcloud == null) {
			return null;
		}
		try {
			if(pointcloud.isEmpty()) {
				return null;
			}
			double[][] points = null;
			if(generateHull) {
				SpatialReference src = new SpatialReference("");
				String proj4 = pointcloud.getProj4();
				if(proj4.isEmpty()) {
					Logger.warn("no poj4: " + pointcloud.getName());
					ACL acl_ownermod = AclUtil.union(pointcloud.getACL_owner(), pointcloud.getACL_mod());
					return generateCatalogEntry(pointcloud.getName(), CatalogKey.TYPE_POINTCLOUD, null, pointcloud.getAssociated(), pointcloud.getACL(), acl_ownermod, pointcloud.informal(), null);
				} else {
					src.ImportFromProj4(proj4);				
				}
				CoordinateTransformation ct = CoordinateTransformation.CreateCoordinateTransformation(src, CATALOG_SPATIAL_REFERENCE);

				Set<TileKey> keys = pointcloud.getTileKeys();
				Coordinate[] coordinates = new Coordinate[keys.size() * 4];
				int i = 0;
				double cellsize = pointcloud.getCellsize();
				P2d celloffset = pointcloud.getCelloffset();
				double cellscale = pointcloud.getCellscale();
				for(TileKey key:keys) {				
					double xmin = (celloffset.x + key.x) * cellsize;
					double ymin = (celloffset.y + key.y) * cellsize;
					double xmax = (celloffset.x + key.x + 1) * cellsize - (1 / cellscale);
					double ymax = (celloffset.y + key.y + 1) * cellsize - (1 / cellscale);
					coordinates[i++] = new Coordinate(xmin, ymin);
					coordinates[i++] = new Coordinate(xmax, ymin);
					coordinates[i++] = new Coordinate(xmin, ymax);
					coordinates[i++] = new Coordinate(xmax, ymax);
				}
				points = generateConvexHullPoints(coordinates);
				ct.TransformPoints(points);
			}
			return generateCatalogEntry(pointcloud.getName(), CatalogKey.TYPE_POINTCLOUD, points, pointcloud.getAssociated(), pointcloud.getACL(), pointcloud.getACL_mod(), pointcloud.informal(), null);
		} catch(Exception e) {
			e.printStackTrace();
			Logger.warn(e);
			return generateCatalogEntry(pointcloud.getName(), CatalogKey.TYPE_POINTCLOUD, null, pointcloud.getAssociated(), pointcloud.getACL(), pointcloud.getACL_mod(), pointcloud.informal(), null);	
		}
	}

	private CatalogEntry generateCatalogEntryVectordb(VectorDB vectordb, boolean generateHull) {
		if(vectordb == null) {
			return null;
		}
		ACL acl_ownermod = AclUtil.union(vectordb.getACL_owner(), vectordb.getACL_mod());
		try {
			double[][] points = null;
			if(generateHull) {
				points = vectordb.getPoints();
				SpatialReference src = vectordb.getSpatialReference();
				if(points == null || points.length == 0 || src == null) {
					return generateCatalogEntry(vectordb.getName(), CatalogKey.TYPE_VECTORDB, null, null, vectordb.getACL(), acl_ownermod, vectordb.informal(), vectordb.getStructuredAccess());
				}
				int len = points.length;
				Coordinate[] coordinates = new Coordinate[len];
				for (int j = 0; j < len; j++) {
					coordinates[j] = new Coordinate(points[j][0], points[j][1]);
				}
				points = generateConvexHullPoints(coordinates);
				CoordinateTransformation ct = CoordinateTransformation.CreateCoordinateTransformation(src, CATALOG_SPATIAL_REFERENCE);
				ct.TransformPoints(points);
			}
			return generateCatalogEntry(vectordb.getName(), CatalogKey.TYPE_VECTORDB, points, null, vectordb.getACL(), acl_ownermod, vectordb.informal(), vectordb.getStructuredAccess());
		} catch(Exception e) {
			e.printStackTrace();
			Logger.warn(e);
			return generateCatalogEntry(vectordb.getName(), CatalogKey.TYPE_VECTORDB, null, null, vectordb.getACL(), acl_ownermod, vectordb.informal(), vectordb.getStructuredAccess());
		}
	}


	public CatalogEntry generateCatalogEntry(String name, String type, double[][] points, Associated associated, ACL acl, ACL acl_mod, Informal informal, StructuredAccess structuredAccess) {

		boolean usePoints = true;

		if(points == null) {
			usePoints = false;
		} else {

			for(double[] p:points) {
				for(double c:p) {
					if(!Double.isFinite(c)) {
						Logger.warn("not finite pos in " + name);
						usePoints = false;
					}
				}
			}

			for(double[] p:points) {
				if(p[0] < -180)  {
					p[0] = -180;
				}
				if(p[1] < -90)  {
					p[1] = -90;
				}
				if(180 < p[0])  {
					p[0] = 180;
				}
				if(90 < p[1])  {
					p[1] = 90;
				}
			}

		}

		return usePoints ? new CatalogEntry(name, type, informal.description, points, associated, acl, acl_mod, informal.tags.toArray(String[]::new), informal.title, structuredAccess) : new CatalogEntry(name, type, informal.description, null, associated, acl, acl_mod, informal.tags.toArray(String[]::new), informal.title, structuredAccess);
	}

	public double[][] generateConvexHullPoints(Coordinate[] coordinates) {
		GeometryFactory geometryFactory = new GeometryFactory();
		ConvexHull convexHull = new ConvexHull(coordinates, geometryFactory);
		Geometry geometry = convexHull.getConvexHull();
		Polygon polygon = (Polygon) geometry;
		Coordinate[] polyCoord = polygon.getCoordinates();
		double[][] points = new double[polyCoord.length][2];
		for (int j = 0; j < polyCoord.length; j++) {
			Coordinate coord = polyCoord[j];
			points[j] = new double[] {coord.x, coord.y};
		}
		return points;
	}


	public synchronized void rebuildCatalog() {
		Map<CatalogKey, CatalogEntry> map = new LinkedHashMap<>();
		for(String name:broker.getRasterdbNames()) {				
			try {
				RasterDB rasterdb = broker.getRasterdb(name);
				CatalogEntry catalogEntry = generateCatalogEntryRasterDB(rasterdb, true);
				if(catalogEntry != null) {
					map.put(catalogEntry.toKey(), catalogEntry);
				}
			} catch(Exception e) {
				Logger.error(e);
			}
		}

		for(String name:broker.brokerConfig.pointdbMap().keySet()) {
			try {
				PointDB pointdb = broker.getPointdb(name);
				CatalogEntry catalogEntry = generateCatalogEntryPointDB(pointdb, true);
				if(catalogEntry != null) {
					map.put(catalogEntry.toKey(), catalogEntry);
				}
			} catch(Exception e) {				
				Logger.error(e);
			}
		}

		for(String name:broker.getPointCloudNames()) {
			try {
				PointCloud pointcloud = broker.getPointCloud(name);
				CatalogEntry catalogEntry = generateCatalogEntryPointcloud(pointcloud, true);
				if(catalogEntry != null) {
					map.put(catalogEntry.toKey(), catalogEntry);
				}
			} catch(Exception e) {				
				Logger.error(e);
			}
		}

		for(String name:broker.getVectordbNames()) {
			try {
				VectorDB vectordb = broker.getVectorDB(name);
				CatalogEntry catalogEntry = generateCatalogEntryVectordb(vectordb, true);
				if(catalogEntry != null) {
					map.put(catalogEntry.toKey(), catalogEntry);
				}
			} catch(Exception e) {				
				Logger.error(e);
			}
		}

		this.map = map;
		writeToFile();
	}

	/**
	 * Check if layers have been added or removed.
	 */
	public synchronized void updateCatalog() {
		Map<CatalogKey, CatalogEntry> map = new LinkedHashMap<>(this.map);
		//Logger.info("map "+map.keySet());
		boolean changed = false;
		Iterator<CatalogEntry> it = map.values().iterator();
		while(it.hasNext()) {
			CatalogEntry catalogEntry = it.next();
			switch(catalogEntry.type) {
			case CatalogKey.TYPE_RASTERDB: {
				if(!broker.getRasterdbNames().contains(catalogEntry.name)) {
					Logger.warn("remove missing RasterDB: "+catalogEntry);
					it.remove();
					changed = true;
				}
				break;
			}
			case CatalogKey.TYPE_POINTDB: {
				if(!broker.brokerConfig.pointdbMap().containsKey(catalogEntry.name)) {
					Logger.warn("remove missing PointDB: "+catalogEntry);
					it.remove();
					changed = true;
				}
				break;
			}
			case CatalogKey.TYPE_POINTCLOUD: {
				if(!broker.getPointCloudNames().contains(catalogEntry.name)) {
					Logger.warn("remove missing pointcloud: "+catalogEntry);
					it.remove();
					changed = true;
				}
				break;
			}
			case CatalogKey.TYPE_VECTORDB: {
				if(!broker.getVectordbNames().contains(catalogEntry.name)) {
					Logger.warn("remove missing VectorDB: "+catalogEntry);
					it.remove();
					changed = true;
				}
				break;
			}
			default: {
				Logger.warn("remove unknown type: "+catalogEntry);
				it.remove();
				changed = true;
			}
			}
		}

		for(String name:broker.getRasterdbNames()) {
			if(!map.containsKey(new CatalogKey(name, CatalogKey.TYPE_RASTERDB))) {				
				try {
					RasterDB rasterdb = broker.getRasterdb(name);
					CatalogEntry catalogEntry = generateCatalogEntryRasterDB(rasterdb, true);
					if(catalogEntry != null) {
						map.put(catalogEntry.toKey(), catalogEntry);
						changed = true;
					}
				} catch(Exception e) {
					Logger.error(e);
				}
			}			
		}

		for(String name:broker.brokerConfig.pointdbMap().keySet()) {
			if(!map.containsKey(new CatalogKey(name, CatalogKey.TYPE_POINTDB))) {
				try {
					PointDB pointdb = broker.getPointdb(name);
					CatalogEntry catalogEntry = generateCatalogEntryPointDB(pointdb, true);
					if(catalogEntry != null) {
						map.put(catalogEntry.toKey(), catalogEntry);
						changed = true;
					}
				} catch(Exception e) {
					Logger.error(e);
				}
			}
		}

		for(String name:broker.getPointCloudNames()) {
			if(!map.containsKey(new CatalogKey(name, CatalogKey.TYPE_POINTCLOUD))) {
				try {
					PointCloud pointcloud = broker.getPointCloud(name);
					CatalogEntry catalogEntry = generateCatalogEntryPointcloud(pointcloud, true);
					if(catalogEntry != null) {
						map.put(catalogEntry.toKey(), catalogEntry);
						changed = true;
					}
				} catch(Exception e) {
					Logger.error(e);
				}
			}
		}

		for(String name:broker.getVectordbNames()) {
			if(!map.containsKey(new CatalogKey(name, CatalogKey.TYPE_VECTORDB))) {
				try {
					VectorDB vectordb = broker.getVectorDB(name);
					CatalogEntry catalogEntry = generateCatalogEntryVectordb(vectordb, true);
					if(catalogEntry != null) {
						map.put(catalogEntry.toKey(), catalogEntry);
						changed = true;
					}
				} catch(Exception e) {
					Logger.error(e);
				}
			}
		}

		if(changed) {
			this.map = map;
			writeToFile();
			Logger.info("catalog updated");
		}
	}

	public void writeJSON(Writer writer, UserIdentity userIdentity, boolean withACL) {
		writeJSON(new JSONWriter(writer), userIdentity, withACL);
	}

	public void writeJSON(JSONWriter json, UserIdentity userIdentity, boolean withACL) {
		json.array();
		for(CatalogEntry entry:map.values()) {
			if(AclUtil.isAllowed(entry.acl, entry.acl_mod, userIdentity))
				entry.writeJSON(json, withACL);			
		}
		json.endArray();
	}

	public synchronized void writeToFile() {
		try {
			int tempIndex = 0;
			Path pathWrite;
			File fileWrite; 
			do {
				tempIndex++;
				pathWrite = Paths.get(CATALOG_TEMP_PREFIX + tempIndex + ".json");
				fileWrite = pathWrite.toFile();
			} while(fileWrite.exists());
			fileWrite.getParentFile().mkdirs();
			try (FileWriter writer = new FileWriter(fileWrite);
					BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
				writeJSON(bufferedWriter, null, true);
			}
			Files.move(fileWrite.toPath(), CATALOG_FILENAME, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch(Exception e) {
			e.printStackTrace();
			Logger.warn(e);
		}
	}

	public void loadFromFile() {
		try {
			String source = new String(Files.readAllBytes(CATALOG_FILENAME), StandardCharsets.UTF_8);
			JSONArray jsonArray = new JSONArray(source);
			int len = jsonArray.length();
			Map<CatalogKey, CatalogEntry> map = new LinkedHashMap<>();
			for (int i = 0; i < len; i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				CatalogEntry catalogEntry = CatalogEntry.parseJSON(jsonObject);
				map.put(catalogEntry.toKey(), catalogEntry);
			}
			this.map = map;
		} catch(Exception e) {
			e.printStackTrace();
			Logger.warn(e);
		}
	}
	
	public void collectRoles(Set<String> roles) {
		for(CatalogEntry e:map.values()) {
			e.acl.collectRoles(roles);
			e.acl_mod.collectRoles(roles);
		}
	}

	public String[] getRoles() {
		HashSet<String> collector = new HashSet<String>();
		collectRoles(collector);
		String[] result = collector.toArray(new String[0]);
		Arrays.sort(result, String.CASE_INSENSITIVE_ORDER);
		return result;
	}

	public String[] getTags(UserIdentity userIdentity) {
		HashSet<String> collector = new HashSet<String>();
		for(CatalogEntry e:map.values()) {
			if(AclUtil.isAllowed(e.acl, e.acl_mod, userIdentity)) {
				for(String tag:e.tags) {
					collector.add(tag);
				}
			}
		}
		String[] result = collector.toArray(new String[0]);
		Arrays.sort(result, String.CASE_INSENSITIVE_ORDER);
		return result;
	}

	public void update(PointCloud pointcloud, boolean updateHull) {
		CatalogKey catalogKey = new CatalogKey(pointcloud.getName(), CatalogKey.TYPE_POINTCLOUD);
		CatalogEntry oldCatalogEntry = map.get(catalogKey);
		CatalogEntry genCatalogEntry;
		if(oldCatalogEntry == null || updateHull) {
			genCatalogEntry = generateCatalogEntryPointcloud(pointcloud, true);
		} else {
			genCatalogEntry = generateCatalogEntryPointcloud(pointcloud, false);
			genCatalogEntry = CatalogEntry.of(genCatalogEntry, oldCatalogEntry.points);
		}
		if(!genCatalogEntry.equals(oldCatalogEntry)) {
			update(genCatalogEntry);
		}
	}

	public void update(RasterDB rasterdb, boolean updateHull) {
		CatalogKey catalogKey = new CatalogKey(rasterdb.config.getName(), CatalogKey.TYPE_RASTERDB);
		CatalogEntry oldCatalogEntry = map.get(catalogKey);
		CatalogEntry genCatalogEntry;
		if(oldCatalogEntry == null || updateHull) {
			genCatalogEntry = generateCatalogEntryRasterDB(rasterdb, true);
		} else {
			genCatalogEntry = generateCatalogEntryRasterDB(rasterdb, false);
			genCatalogEntry = CatalogEntry.of(genCatalogEntry, oldCatalogEntry.points);
		}
		if(!genCatalogEntry.equals(oldCatalogEntry)) {
			update(genCatalogEntry);
		}
	}

	public void update(PointDB pointdb, boolean updateHull) {
		CatalogKey catalogKey = new CatalogKey(pointdb.config.name, CatalogKey.TYPE_POINTDB);
		CatalogEntry oldCatalogEntry = map.get(catalogKey);
		CatalogEntry genCatalogEntry;
		if(oldCatalogEntry == null || updateHull) {
			genCatalogEntry = generateCatalogEntryPointDB(pointdb, true);
		} else {
			genCatalogEntry = generateCatalogEntryPointDB(pointdb, false);
			genCatalogEntry = CatalogEntry.of(genCatalogEntry, oldCatalogEntry.points);
		}
		if(!genCatalogEntry.equals(oldCatalogEntry)) {
			update(genCatalogEntry);
		}
	}

	public void update(VectorDB vectordb, boolean updateHull) {
		Logger.info("update catalog entry vecotrdb " + vectordb.informal());
		CatalogKey catalogKey = new CatalogKey(vectordb.getName(), CatalogKey.TYPE_VECTORDB);
		CatalogEntry oldCatalogEntry = map.get(catalogKey);
		CatalogEntry genCatalogEntry;
		if(oldCatalogEntry == null || updateHull) {
			genCatalogEntry = generateCatalogEntryVectordb(vectordb, true);
		} else {
			genCatalogEntry = generateCatalogEntryVectordb(vectordb, false);
			genCatalogEntry = CatalogEntry.of(genCatalogEntry, oldCatalogEntry.points);
		}
		if(!genCatalogEntry.equals(oldCatalogEntry)) {
			Logger.info("update");
			update(genCatalogEntry);
		}
	}

	public synchronized void update(CatalogEntry catalogEntry) {
		Map<CatalogKey, CatalogEntry> map = new LinkedHashMap<>(this.map);
		map.put(catalogEntry.toKey(), catalogEntry);
		this.map = map;
		writeToFile();
		Logger.info("catalog updated");
	}

	private static Comparator<CatalogEntry> CATALOG_ENTRY_COMPARATOR = new Comparator<CatalogEntry>() {
		@Override
		public int compare(CatalogEntry o1, CatalogEntry o2) {
			return String.CASE_INSENSITIVE_ORDER.compare(o1.name, o2.name);
		}		
	};

	public Stream<CatalogEntry> getSorted(String type, UserIdentity userIdentity) {
		return map.keySet().stream()
				.filter(key -> key.type.equals(type))
				.map(key -> map.get(key))
				.filter(entry -> AclUtil.isAllowed(entry.acl, entry.acl_mod, userIdentity))
				.sorted(CATALOG_ENTRY_COMPARATOR);
	}

	public void update(VoxelDB voxeldb, boolean updateCatalogPoints) {
		// TODO Auto-generated method stub
		
	}
}
