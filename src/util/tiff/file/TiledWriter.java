package util.tiff.file;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broker.Broker;
import rasterdb.Band;
import rasterdb.BandProcessor;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import util.Range2d;
import util.frame.ShortFrame;
import util.tiff.TiffBandInt16;
import util.tiff.file.TiffFile.TiffTile;

public class TiledWriter {
	private static final Logger log = LogManager.getLogger();


	public static void write() {
	}

	public static void main(String[] args) throws IOException {

		try(Broker broker = new Broker()) {
			RasterDB rasterdb = broker.getRasterdb("be_alb_rapideye_atm_rebuild");
			int timestamp = 60598080;
			int scale = 1;
			GeoReference ref = rasterdb.ref();
			Range2d range = ref.bboxToRange2d(new double[] {512920, 5353805, 544770, 5376220});
			int tileWidth = 1024;
			int tileHeight = 1024;
			log.info(range);
			//range = range.allignMaxToTiles(tileWidth, tileHeight);
			range = range.allignToTiles(tileWidth, tileHeight);
			log.info(range);
			int xmin = range.xmin;
			int ymin = range.ymin;
			int xmax = range.xmax;
			int ymax = range.ymax;
			int bandCount = rasterdb.bandMapReadonly.size();
			Band[] bands = rasterdb.bandMapReadonly.values().toArray(Band[]::new);
			TiffFile tiffFile = new TiffFile(ref, xmin, ymin, xmax, ymax, tileWidth, tileHeight, bandCount);

			try(RandomAccessFile raf = new RandomAccessFile("temp/testingTiff.tiff", "rw")) {
				raf.setLength(0);
				long imageDataPos = tiffFile.writeHeader(raf);
				raf.seek(imageDataPos);

				TiffTile[][] tiles = tiffFile.tiles;
				for(int b = 0; b < bandCount; b++) {
					TiffTile[] btiles = tiles[b];
					Band band = bands[b];
					for (int i = 0; i < btiles.length; i++) {
						TiffTile tile = btiles[i];
						Range2d trange = new Range2d(tile.tileXmin, tile.tileYmin, tile.tileXmax, tile.tileYmax);
						BandProcessor bandProcessor = new BandProcessor(rasterdb, trange, timestamp, scale);
						ShortFrame shortFrame = bandProcessor.getShortFrame(band);
						/*for(int y = 0; y < tileHeight; y++) {
							for(int x = 0; x < tileWidth; x++) {
								///shortFrame.data[y][x] = (short) (b == 0 ? (i*256) : 0);
								shortFrame.data[y][x] = (short) (b == 0 ? (i) : 0);
							}
						}*/
						//log.info(shortFrame.data[256][256]);
						log.info(trange + "  " + band);
						log.info(Arrays.toString(shortFrame.getMinMax0()));

						tile.pos = raf.getFilePointer();
						TiffBandInt16.writeData(raf, shortFrame.data, tileWidth, tileHeight);
						tile.len = raf.getFilePointer() - tile.pos;
						log.info(tile);
					}
				}

				tiffFile.writeHeader(raf);
			}
		}
	}

}
