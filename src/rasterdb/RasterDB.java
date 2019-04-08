package rasterdb;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.UserIdentity;
import org.yaml.snakeyaml.Yaml;

import broker.Associated;
import broker.Informal;
import broker.acl.ACL;
import broker.acl.EmptyACL;
import rasterunit.RasterUnit;
import util.Range2d;
import util.yaml.YamlMap;

public class RasterDB implements AutoCloseable {
	private static final Logger log = LogManager.getLogger();

	private static final String TYPE = "RasterDB";

	public NavigableMap<Integer, Band> bandMap;

	private RasterUnit rasterUnit = null;
	private RasterUnit rasterPyr1Unit = null;
	private RasterUnit rasterPyr2Unit = null;
	private RasterUnit rasterPyr3Unit = null;
	private RasterUnit rasterPyr4Unit = null;

	private GeoReference ref = GeoReference.EMPTY_DEFAULT;
	public Associated associated = new Associated();
	private Informal informal = Informal.EMPTY;
	public int version = 1;
	private ACL acl = EmptyACL.ADMIN;
	private ACL acl_mod = EmptyACL.ADMIN;

	private boolean calcExactLocalRange = true;
	private Range2d local_extent = null;

	private final Path metaPath;
	public final RasterdbConfig config;

	private final Path path;

	public RasterDB(RasterdbConfig config) {
		this.config = config;
		path = config.getPath();
		path.toFile().mkdirs();
		bandMap = new TreeMap<Integer, Band>();

		this.metaPath = path.resolve("meta.yaml");
		readMeta();

	}

	public synchronized void commit() {
		if (rasterUnit != null) {
			rasterUnit.commit();
		}
		if (rasterPyr1Unit != null) {
			rasterPyr1Unit.commit();
		}
		if (rasterPyr2Unit != null) {
			rasterPyr2Unit.commit();
		}
		if (rasterPyr3Unit != null) {
			rasterPyr3Unit.commit();
		}
		if (rasterPyr4Unit != null) {
			rasterPyr4Unit.commit();
		}
		writeMeta();
	}

	@Override
	public synchronized void close() {
		log.info("close rasterdb " + config.getName());
		if (rasterUnit != null) {
			log.info("close rasterUnit");
			rasterUnit.close();
			rasterUnit = null;
		}
		if (rasterPyr1Unit != null) {
			rasterPyr1Unit.close();
			rasterPyr1Unit = null;
		}
		if (rasterPyr2Unit != null) {
			rasterPyr2Unit.close();
			rasterPyr2Unit = null;
		}
		if (rasterPyr3Unit != null) {
			rasterPyr3Unit.close();
			rasterPyr3Unit = null;
		}
		if (rasterPyr4Unit != null) {
			rasterPyr4Unit.close();
			rasterPyr4Unit = null;
		}
		writeMeta();
	}

	public void rebuildPyramid() {
		log.info("build map 1:4");
		long c1 = Processing.writeRasterUnitDiv4(this, rasterUnit(), rasterPyr1Unit());
		log.info("tiles written " + c1);
		log.info("build map 1:16");
		long c2 = Processing.writeRasterUnitDiv4(this, rasterPyr1Unit(), rasterPyr2Unit());
		log.info("tiles written " + c2);
		log.info("build map 1:64");
		long c3 = Processing.writeRasterUnitDiv4(this, rasterPyr2Unit(), rasterPyr3Unit());
		log.info("tiles written " + c3);
		log.info("build map 1:256");
		long c4 = Processing.writeRasterUnitDiv4(this, rasterPyr3Unit(), rasterPyr4Unit());
		log.info("tiles written " + c4);
	}

	public synchronized void writeMeta() {
		try {
			LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
			map.put("type", TYPE);
			map.put("version", version);
			if (ref != null) {
				map.put("ref", ref.toYaml());
			}
			if (!bandMap.isEmpty()) {
				LinkedHashMap<Integer, Object> m = new LinkedHashMap<Integer, Object>();
				for (Band band : bandMap.values()) {
					m.put(band.index, band.toYamlWithoutIndex());
				}
				map.put("bands", m);
			}
			map.put("associated", associated.toYaml());
			map.put("acl", acl.toYaml());
			map.put("acl_mod", acl_mod.toYaml());
			informal.writeYaml(map);
			if(local_extent != null) {
				map.put("local_extent", local_extent.toYaml());
			}
			Yaml yaml = new Yaml();
			Path writepath = Paths.get(metaPath.toString()+"_temp");
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(writepath.toFile())));
			yaml.dump(map, out);
			out.close();
			Files.move(writepath, metaPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (Exception e) {
			log.warn(e);
		}
	}

	public synchronized void readMeta() {
		try {
			ref = GeoReference.EMPTY_DEFAULT;
			associated = new Associated();
			bandMap.clear();
			if (metaPath.toFile().exists()) {
				InputStream in = new FileInputStream(metaPath.toFile());
				YamlMap yamlMap = YamlMap.ofObject(new Yaml().load(in));
				in.close();
				String type = yamlMap.getString("type");
				if (!type.equals(TYPE)) {
					throw new RuntimeException("wrong type: " + type);
				}
				version = yamlMap.optInt("version", version);
				if (yamlMap.contains("ref")) {
					ref = GeoReference.ofYaml(yamlMap.getMap("ref"));
				}
				if (yamlMap.contains("bands")) {
					Map<Number, Object> m = (Map<Number, Object>) yamlMap.getObject("bands");
					for (Entry<Number, Object> entry : m.entrySet()) {
						int index = entry.getKey().intValue();
						YamlMap bm = YamlMap.ofObject(entry.getValue());
						Band band = Band.ofYaml(bm, index);

						bandMap.put(index, band);
					}
				}
				if (yamlMap.contains("associated")) {
					associated = Associated.ofYaml(yamlMap.getMap("associated"));
				}
				acl = ACL.of(yamlMap.optList("acl").asStrings());
				acl_mod = ACL.of(yamlMap.optList("acl_mod").asStrings());
				informal = Informal.ofYaml(yamlMap);
				if(yamlMap.contains("local_extent")) {
					local_extent = Range2d.ofYaml(yamlMap.getMap("local_extent"));
				} else {
					local_extent = null;
				}
						
			}
			log.info("*** ref *** " + ref + "    of   " + metaPath);
		} catch (Exception e) {
			e.printStackTrace();
			log.warn(e);
		}
	}

	public Band createSpectralBand(int type, double wavelength, double fwhm, String title, String visualisation) {
		int index = 0;
		for (Band band : bandMap.values()) {
			index = band.index;
		}
		index++;
		Band band = Band.ofSpectralBand(type, index, wavelength, fwhm, title, visualisation);
		log.info("create spectral band " + band.index + "  " + band.wavelength + "  " + band.fwhm);
		bandMap.put(index, band);
		writeMeta();
		return band;
	}

	public Band createBand(int type, String title, String visualisation) {
		int index = 0;
		for (Band band : bandMap.values()) {
			index = band.index;
		}
		index++;
		Band band = Band.of(type, index, title, visualisation);
		log.info("create band " + band.index);
		setBand(band);
		return band;
	}

	public void setBand(Band band) {
		if (bandMap.containsKey(band.index)) {
			log.warn("replace band" + band);
		}
		bandMap.put(band.index, band);
		writeMeta();
	}

	public synchronized Range2d getLocalRange(boolean update) {
		if(local_extent == null || update) {
			synchronized(this) {
				if(local_extent == null || update) {
					if(calcExactLocalRange) {
						Range2d localRangeUpdated = LocalExtentCalculator.calc(this);
						if(!localRangeUpdated.equals(local_extent)) {
							local_extent = localRangeUpdated;
							writeMeta();
						}
					} else {
						Range2d localRangeUpdated = rasterUnit().getTileRange().mul(TilePixel.PIXELS_PER_ROW).add(0, 0, TilePixel.PIXELS_PER_ROW_1, TilePixel.PIXELS_PER_ROW_1);
						if(!localRangeUpdated.equals(local_extent)) {
							local_extent = localRangeUpdated;
							writeMeta();
						}
					}
				}
			}
		}
		return local_extent;
	}

	public void setPixelSize(double pixel_size_x, double pixel_size_y, double offset_x, double offset_y) {
		ref = ref.withPixelSize(pixel_size_x, pixel_size_y, offset_x, offset_y);
		writeMeta();
	}

	public void setCode(String code) {
		ref = ref.withCode(code, GeoReference.code_wms_transposed.contains(code));
		log.info(ref + "   " + GeoReference.code_wms_transposed.contains(code));
		writeMeta();
	}

	public void setProj4(String proj4) {
		ref = ref.withProj4(proj4);
		log.info(ref);
		writeMeta();
	}

	public GeoReference ref() {
		return ref;
	}

	public Band getBandByTitle(String title) {
		for (Band band : bandMap.values()) {
			if (band.has_title() && band.title.equals(title)) {
				return band;
			}
		}
		return null;
	}

	public Band getBandByNumber(int band_number) {
		return bandMap.get(band_number);
	}

	public Informal informal() {
		return informal;
	}

	public void setInformal(Informal informal) {
		this.informal = informal;
		writeMeta();
	}

	public ACL getACL() {
		return acl;
	}

	public ACL getACL_mod() {
		return acl_mod;
	}

	public void setACL(ACL acl) {
		this.acl = acl;
		writeMeta();	
	}

	public void setACL_mod(ACL acl_mod) {
		this.acl_mod = acl_mod;
		writeMeta();	
	}

	public boolean isAllowed(UserIdentity userIdentity) {
		return acl.isAllowed(userIdentity);
	}

	public void check(UserIdentity userIdentity) {
		acl.check(userIdentity);
	}

	public boolean isAllowedMod(UserIdentity userIdentity) {
		return acl_mod.isAllowed(userIdentity);
	}

	public void checkMod(UserIdentity userIdentity) {
		acl_mod.check(userIdentity);
	}

	public RasterUnit rasterUnit() {
		RasterUnit r = rasterUnit;
		return r == null ? loadRasterUnit() : r;
	}

	public RasterUnit rasterPyr1Unit() {
		RasterUnit r = rasterPyr1Unit;
		return r == null ? loadRasterPyr1Unit() : r;
	}

	public RasterUnit rasterPyr2Unit() {
		RasterUnit r = rasterPyr2Unit;
		return r == null ? loadRasterPyr2Unit() : r;
	}

	public RasterUnit rasterPyr3Unit() {
		RasterUnit r = rasterPyr3Unit;
		return r == null ? loadRasterPyr3Unit() : r;
	}

	public RasterUnit rasterPyr4Unit() {
		RasterUnit r = rasterPyr4Unit;
		return r == null ? loadRasterPyr4Unit() : r;
	}

	private synchronized RasterUnit loadRasterUnit() {
		if (rasterUnit == null) {
			log.info("open rasterUnit of " + config.getName());
			rasterUnit = new RasterUnit(path, "raster", config.is_fast_unsafe_import()); // 4^0 = 1
		}
		return rasterUnit;
	}

	private synchronized RasterUnit loadRasterPyr1Unit() {
		if (rasterPyr1Unit == null) {
			rasterPyr1Unit = new RasterUnit(path, "raster1", config.is_fast_unsafe_import()); // 4^1 = 4
		}
		return rasterPyr1Unit;
	}

	private synchronized RasterUnit loadRasterPyr2Unit() {
		if (rasterPyr2Unit == null) {
			rasterPyr2Unit = new RasterUnit(path, "raster2", config.is_fast_unsafe_import()); // 4^2 = 16
		}
		return rasterPyr2Unit;
	}

	private synchronized RasterUnit loadRasterPyr3Unit() {
		if (rasterPyr3Unit == null) {
			rasterPyr3Unit = new RasterUnit(path, "raster3", config.is_fast_unsafe_import()); // 4^3 = 64
		}
		return rasterPyr3Unit;
	}

	private synchronized RasterUnit loadRasterPyr4Unit() {
		if (rasterPyr4Unit == null) {
			rasterPyr4Unit = new RasterUnit(path, "raster4", config.is_fast_unsafe_import()); // 4^4 = 256
		}
		return rasterPyr4Unit;
	}

	public synchronized boolean hasRasterUnit() {
		return Files.exists(path.resolve("raster"));
	}
}
