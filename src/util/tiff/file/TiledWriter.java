package util.tiff.file;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broker.Broker;
import rasterdb.Band;
import rasterdb.BandProcessor;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import remotetask.CancelableRemoteProxy;
import util.Range2d;
import util.collections.vec.Vec;
import util.frame.ShortFrame;
import util.tiff.TiffBandInt16;
import util.tiff.file.TiffFile.TiffImageEntry;
import util.tiff.file.TiffFile.TiffTile;

public class TiledWriter extends CancelableRemoteProxy {
	private static final Logger log = LogManager.getLogger();

	private final RasterDB rasterdb;
	private Range2d range;
	private final int timestamp;
	private final String filename;

	public static void main(String[] args) throws IOException {
		try(Broker broker = new Broker()) {
			RasterDB rasterdb = broker.getRasterdb("be_alb_rapideye_atm_rebuild");
			GeoReference ref = rasterdb.ref();
			Range2d range = ref.bboxToRange2d(new double[] {512920, 5353805, 544770, 5376220});
			String filename = "temp/testingTiff.tif";
			int timestamp = 60598080;
			try(TiledWriter tiledWriter = new TiledWriter(rasterdb, range, timestamp, filename)) {
				tiledWriter.process();
			}		
		}
	}

	public TiledWriter(RasterDB rasterdb, Range2d range, int timestamp, String filename) {
		this.rasterdb = rasterdb;
		this.range = range;
		this.timestamp = timestamp;
		this.filename = filename;
	}

	public void process() throws IOException {
		boolean bigTiff = true;
		int scale = 1;
		int tileWidth = 1024;
		int tileHeight = 1024;
		log.info(range);
		//range = range.allignMaxToTiles(tileWidth, tileHeight);
		range = range.allignToTiles(tileWidth, tileHeight);
		log.info(range);
		int bandCount = rasterdb.bandMapReadonly.size();
		Band[] bands = rasterdb.bandMapReadonly.values().toArray(Band[]::new);
		GeoReference ref = rasterdb.ref();
		TiffFile tiffFile = new TiffFile(ref, range, tileWidth, tileHeight, bandCount);

		try(RandomAccessFile raf = new RandomAccessFile(filename, "rw")) {				
			raf.setLength(0);
			long imageDataPos = tiffFile.writeHeader(raf, bigTiff);
			raf.seek(imageDataPos);
			Vec<TiffImageEntry> imageList = new Vec<TiffImageEntry>();
			imageList.add(new TiffImageEntry(tiffFile.tiles, scale));

			for(TiffTile[][] tiles : tiffFile.overviewtilesVec) {
				scale *= 2;
				imageList.add(new TiffImageEntry(tiles, scale));
			}

			for(TiffImageEntry tiffImageEntry : imageList.asReverseIterable()) {
				log.info("process scale " + tiffImageEntry.scale);
				for(int b = 0; b < bandCount; b++) {
					TiffTile[] btiles = tiffImageEntry.tiles[b];
					Band band = bands[b];
					for (int i = 0; i < btiles.length; i++) {
						TiffTile tile = btiles[i];
						Range2d trange = new Range2d(tile.tileXmin, tile.tileYmin, tile.tileXmax, tile.tileYmax);
						BandProcessor bandProcessor = new BandProcessor(rasterdb, trange, timestamp, tiffImageEntry.scale);
						ShortFrame shortFrame = bandProcessor.getShortFrame(band);
						/*for(int y = 0; y < tileHeight; y++) {
							for(int x = 0; x < tileWidth; x++) {
								///shortFrame.data[y][x] = (short) (b == 0 ? (i*256) : 0);
								shortFrame.data[y][x] = (short) (b == 0 ? (i) : 0);
							}
						}*/
						//log.info(shortFrame.data[256][256]);
						log.info(trange + "  " + band);
						//log.info(Arrays.toString(shortFrame.getMinMax0()));

						tile.pos = raf.getFilePointer();
						log.info("file tile pos " + tile.pos);
						//TiffBandInt16.writeData(raf, shortFrame.data, tileWidth, tileHeight);
						//TiffBandInt16.writeDataDeflate(raf, shortFrame.data, tileWidth, tileHeight);
						//TiffBandInt16.writeDataZSTD(raf, shortFrame.data, tileWidth, tileHeight);
						TiffBandInt16.writeDataDiffZSTD(raf, shortFrame.data, tileWidth, tileHeight);
						tile.len = raf.getFilePointer() - tile.pos;
						log.info(tile);
					}
				}
			}				

			tiffFile.writeHeader(raf, bigTiff);
		}
	}

}
