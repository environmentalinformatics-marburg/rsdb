package rasterdb.importer;

import java.nio.file.Path;
import java.util.TreeMap;


import org.tinylog.Logger;

import broker.Broker;
import rasterdb.Band;
import rasterdb.RasterDB;
import rasterdb.tile.TilePixel;
import remotetask.MessageSink;
import util.Timer;
import util.Util;

public class Import_landsat8 {
	

	//private final Broker broker;
	private final RasterDB rasterdb_30m;
	private final RasterDBimporter rasterdbimporter_30m;
	private final RasterDB rasterdb_15m;
	private final RasterDBimporter rasterdbimporter_15m;

	public Import_landsat8(Broker broker, String name) {
		//this.broker = broker;
		this.rasterdb_30m = broker.getRasterdb(name+"_30m");
		this.rasterdbimporter_30m = new RasterDBimporter(rasterdb_30m);
		this.rasterdb_15m = broker.getRasterdb(name+"_15m");
		this.rasterdbimporter_15m = new RasterDBimporter(rasterdb_15m);
	}

	private static final String[] bandIDs = new String[]{
			"b1",
			"b2",
			"b3",
			"b4",
			"b5",
			"b6",
			"b7",
			"b8",
			"b9",
			"b10",
			"b11",
	};

	private static final int[] bandRes = new int[]{
			30,
			30,
			30,
			30,
			30,
			30,
			30,
			15,
			30,
			30,
			30,
	};

	private static final int[] bandIndices = new int[]{
			1,
			2,
			3,
			4,
			5,
			6,
			7,
			8,
			9,
			10,
			11
	};

	private static final String[] bandTitles = new String[]{
			"Ultra Blue (coastal/aerosol)",
			"Blue",
			"Green",
			"Red",
			"Near Infrared (NIR)",
			"Shortwave Infrared (SWIR) 1",
			"Shortwave Infrared (SWIR) 2",
			"Panchromatic",
			"Cirrus",
			"Thermal Infrared (TIRS) 1",
			"Thermal Infrared (TIRS) 2",
	};

	private static final String[] bandVisualisations = new String[]{
			null,
			"blue",
			"green",
			"red",
			null,
			null,
			null,
			null,
			null,
			null,
			null,
	};

	private static final double[] bandWvmin = new double[]{
			0.435,
			0.452,
			0.533,
			0.636,
			0.851,
			1.566,
			2.107,
			0.503,
			1.363,
			10.6,
			11.5,
	};

	private static final double[] bandWvmax = new double[]{	
			0.451,
			0.512,
			0.59,
			0.673,
			0.879,
			1.651,
			2.294,
			0.676,
			1.384,
			11.19,
			12.51,
			0.451,
			0.512,
			0.59,
	};



	public void importDirectory(Path root) throws Exception {
		Timer.start("import_landsat8"+root);
		TreeMap<String, Path> fileMap = new TreeMap<String, Path>();
		for(Path path:Util.getPaths(root)) {
			if(path.toFile().isFile()) {
				try {
					String filename = path.getFileName().toString().toLowerCase();
					String ext = filename.substring(filename.lastIndexOf('.')+1);
					if(ext.equals("tif")) {
						//Logger.info("import file "+path);
						String title = filename.substring(0, filename.lastIndexOf('.'));
						Logger.info("title "+title);
						String bandTitle = title.substring(title.lastIndexOf('_')+1);
						Logger.info("bandTitle "+bandTitle);
						if(fileMap.containsKey(bandTitle)) {
							Logger.warn("band already inserted. overwrite");
						}
						fileMap.put(bandTitle, path);

						//rasterdbimporter.importFile_GDAL(filename);


						//importFile(path.toString());
					} else {
						//Logger.info("skip file "+path);	
					}
				} catch(Exception e) {
					e.printStackTrace();
					Logger.error(e);
				}
			}
		}

		for(int i=0;i<bandIndices.length;i++) {
			try {
				String bandTitle = bandIDs[i];
				int bandIndex = bandIndices[i];
				Path filename = fileMap.get(bandTitle);
				double wvmin = bandWvmin[i] * 1000d;
				double wvmax = bandWvmax[i] * 1000d;

				Band band = Band.ofSpectralBand(TilePixel.TYPE_SHORT, bandIndex, (wvmin + wvmax) / 2, (wvmax - wvmin) / 2, bandTitles[i], bandVisualisations[i]);
				switch(bandRes[i]) {
				case 30:
					rasterdb_30m.setBand(band, true);
					rasterdbimporter_30m.importFile_GDAL(filename, band, false, 0);
					break;
				case 15:
					rasterdb_15m.setBand(band, true);
					rasterdbimporter_15m.importFile_GDAL(filename, band, false, 0);
					break;
				default:
					throw new RuntimeException();
				}
			} catch(Exception e) {
				Logger.warn(e);
			}
		}

		Logger.info(Timer.stop("import_landsat8"+root));
		rasterdb_30m.rebuildPyramid(true, MessageSink.MESSAGE_SINK_LOG);
	}

}
