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
import util.frame.FloatFrame;
import util.frame.ShortFrame;
import util.tiff.TiffBandFloat32;
import util.tiff.TiffBandInt16;
import util.tiff.file.TiffFile.TiffCompression;
import util.tiff.file.TiffFile.TiffImageEntry;
import util.tiff.file.TiffFile.TiffTile;

public class TiledWriter extends CancelableRemoteProxy {
	private static final Logger log = LogManager.getLogger();

	private final RasterDB rasterdb;
	private final BandType bandType;
	private final TiffCompression tiffCompression;
	private Range2d range;
	private final int timestamp;
	private final String filename;

	public enum BandType {
		INT16(2, 16),
		FLOAT32(3, 32);

		public final short tiff_sampleFormat;
		public final short tiff_bitsPerSamplePerBand;

		private BandType(int tiff_sampleFormat, int tiff_bitsPerSamplePerBand) {
			this.tiff_sampleFormat = (short) tiff_sampleFormat;
			this.tiff_bitsPerSamplePerBand = (short) tiff_bitsPerSamplePerBand;
		}
	}

	public static void main(String[] args) throws IOException {
		try(Broker broker = new Broker()) {
			RasterDB rasterdb = broker.getRasterdb("be_alb_rapideye_atm_rebuild");
			GeoReference ref = rasterdb.ref();
			Range2d range = ref.bboxToRange2d(new double[] {512920, 5353805, 544770, 5376220});
			String filename = "temp/testingTiff.tif";
			int timestamp = 60598080;
			try(TiledWriter tiledWriter = new TiledWriter(rasterdb, BandType.INT16, TiffCompression.NO, range, timestamp, filename)) {
				tiledWriter.process();
			}		
		}
	}

	public TiledWriter(RasterDB rasterdb, BandType bandType, TiffCompression tiffCompression, Range2d range, int timestamp, String filename) {
		this.rasterdb = rasterdb;
		this.bandType = bandType;
		this.tiffCompression = tiffCompression;
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
		tiffFile.set_sampleFormat(bandType.tiff_sampleFormat);
		tiffFile.set_bitsPerSamplePerBand(bandType.tiff_bitsPerSamplePerBand);
		tiffFile.setTiffCompression(tiffCompression);

		switch(bandType) {
		case INT16:
			tiffFile.setDeltaCoding(true);
			break;	
		case FLOAT32:
		default:
			tiffFile.setDeltaCoding(false);
		}

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

			long totalTileSum = 0;
			for(TiffImageEntry tiffImageEntry : imageList) {
				for(int b = 0; b < bandCount; b++) {
					totalTileSum += tiffImageEntry.tiles[b].length;
				}
			}

			long totalTileCount = 0;

			for(TiffImageEntry tiffImageEntry : imageList.asReverseIterable()) {
				log.info("process scale " + tiffImageEntry.scale);
				for(int b = 0; b < bandCount; b++) {
					TiffTile[] btiles = tiffImageEntry.tiles[b];
					Band band = bands[b];
					for (int i = 0; i < btiles.length; i++) {
						totalTileCount++;
						//this.setMessage("tile " + (i+1) +"/" + btiles.length + "   of band " + (b+1) + "/" + bandCount + "   of image scale " + tiffImageEntry.scale);
						this.setMessage("image scale " + tiffImageEntry.scale + ", band " + (b+1) + "/" + bandCount + ", tile " + (i+1) +"/" + btiles.length + "  total tiles " + totalTileCount + "/" + totalTileSum + "  progress " + (totalTileCount*100)/totalTileSum + "%");
						TiffTile tile = btiles[i];
						Range2d trange = new Range2d(tile.tileXmin, tile.tileYmin, tile.tileXmax, tile.tileYmax);
						BandProcessor bandProcessor = new BandProcessor(rasterdb, trange, timestamp, tiffImageEntry.scale);
						log.info(trange + "  " + band);
						tile.pos = raf.getFilePointer();
						log.info("file tile pos " + tile.pos);
						switch(bandType) {
						case INT16: {
							ShortFrame shortFrame = bandProcessor.getShortFrame(band);	
							switch(tiffFile.getTiffCompression()) {
							case NO:
								TiffBandInt16.writeData(raf, shortFrame.data, tileWidth, tileHeight, tiffFile.getDeltaCoding());
								break;
							case DEFLATE:
								TiffBandInt16.writeDataDeflate(raf, shortFrame.data, tileWidth, tileHeight, tiffFile.getDeltaCoding());
								break;
							case ZSTD:
								TiffBandInt16.writeDataZSTD(raf, shortFrame.data, tileWidth, tileHeight, tiffFile.getDeltaCoding());
								break;
							default:
								throw new RuntimeException("unknown compression type");
							}
							break;
						}
						case FLOAT32: {
							FloatFrame floatFrame = bandProcessor.getFloatFrame(band);							
							switch(tiffFile.getTiffCompression()) {
							case NO:
								TiffBandFloat32.writeData(raf, floatFrame.data, tileWidth, tileHeight, tiffFile.getDeltaCoding());	
								break;
							case DEFLATE:
								TiffBandFloat32.writeDataDeflate(raf, floatFrame.data, tileWidth, tileHeight, tiffFile.getDeltaCoding());	
								break;
							case ZSTD:
								TiffBandFloat32.writeDataZSTD(raf, floatFrame.data, tileWidth, tileHeight, tiffFile.getDeltaCoding());	
								break;
							default:
								throw new RuntimeException("unknown compression type");
							}							
							break;
						}
						default:
							throw new RuntimeException("unknown band type");
						}

						tile.len = raf.getFilePointer() - tile.pos;
						log.info(tile);
					}
				}
			}				

			tiffFile.writeHeader(raf, bigTiff);
		}
	}

}
