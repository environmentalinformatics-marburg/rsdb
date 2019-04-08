package pointdb.processing.tilemeta;

import pointdb.base.PdbConst;
import pointdb.base.TileMeta;
import pointdb.processing.tilemeta.StatisticsCreator.Statistics;
import util.image.ImageRGBA;

public class MetaImageCreator {

	private final TileMetaProducer tileMetaProducer;
	private final Statistics stat;
	private final int x_start;
	private final int y_start;
	private final int width;
	private final int height;
	private final int view_tile_size;

	public MetaImageCreator(TileMetaProducer tileMetaProducer, Statistics stat, int view_tile_size, int x_start, int y_start, int width, int height) {
		this.tileMetaProducer = tileMetaProducer;
		this.stat = stat;
		this.view_tile_size = view_tile_size;
		this.x_start = x_start;
		this.y_start = y_start;
		this.width = width;
		this.height = height;
	}

	private static class MetaImageLinear extends ImageRGBA implements TileMetaConsumer {

		protected final int view_tile_size;
		protected final int x_start;
		//protected final int y_start;
		protected final int y_end;
		protected final int min_z;
		protected final int range_z;
		protected final int min_intensity;
		protected final int range_intensity;

		public MetaImageLinear(Statistics stat, int view_tile_size, int x_start, int y_start, int width, int height) {
			super(width, height);
			this.view_tile_size = view_tile_size;
			this.x_start = x_start;
			//this.y_start = y_start;			
			this.y_end = y_start+height-1;
			this.min_z = Math.max(stat.tile_significant_z_avg_min,PdbConst.gMin_z);
			int max_z = Math.min(stat.tile_significant_z_avg_max,PdbConst.gMax_z);		
			this.range_z = max_z-min_z;
			this.min_intensity = Math.max(stat.tile_significant_intensity_avg_min,PdbConst.gMin_intensity);
			int max_intensity = Math.min(stat.tile_significant_intensity_avg_max,PdbConst.gMax_intensity);		
			this.range_intensity = max_intensity-min_intensity;
		}

		@Override
		public void nextTileMeta(TileMeta meta) {
			int x = meta.x/view_tile_size - x_start;
			//int y = (meta.y/view_tile_size-y_start);
			int y = y_end - meta.y/view_tile_size;  // flip y		

			if(x>=0&&x<width&&y>=0&&y<height) {
				int base = y*width+x;

				int z = (meta.avg.z-min_z)*255/range_z;
				if(z<0) {
					z=0;
				}
				if(z>255) {
					z=255;
				}
				int intensity = (meta.avg.intensity-min_intensity)*255/range_intensity;
				if(intensity<0) {
					intensity=0;
				}
				if(intensity>255) {
					intensity=255;
				}
				int r,g; 
				r = ((255-z)*intensity)/255;
				int b = (z*intensity)/255;
				g = (r+b)/2;
				imageBuffer[base] = 0xff000000 | (r<<16) | (g<<8) | b;			
			}
		}

	}

	private static class MetaImageGamma extends MetaImageLinear {

		private final float ginv;

		public MetaImageGamma(Statistics stat, int view_tile_size, int x_start, int y_start, int width, int height, double gamma) {
			super(stat, view_tile_size, x_start, y_start, width, height);
			this.ginv = (float) (1f / gamma);
		}

		@Override
		public void nextTileMeta(TileMeta meta) {
			int x = meta.x/view_tile_size - x_start;
			//int y = (meta.y/view_tile_size-y_start);
			int y = y_end - meta.y/view_tile_size;  // flip y		

			if(x>=0&&x<width&&y>=0&&y<height) {
				int base = y*width+x;

				int z = (meta.avg.z-min_z)*255/range_z;
				if(z<0) {
					z=0;
				}
				if(z>255) {
					z=255;
				}
				int intensity = (int) Math.round(255d*Math.pow((float)(meta.avg.intensity-min_intensity) / range_intensity, ginv));
				if(intensity<0) {
					intensity=0;
				}
				if(intensity>255) {
					intensity=255;
				}
				int r,g; 
				r = ((255-z)*intensity)/255;
				int b = (z*intensity)/255;
				g = (r+b)/2;
				imageBuffer[base] = 0xff000000 | (r<<16) | (g<<8) | b;			
			}
		}

	}

	public ImageRGBA create(double gamma) {
		if(gamma==1) {
			MetaImageLinear metaImage = new MetaImageLinear(stat, view_tile_size, x_start, y_start, width, height);
			tileMetaProducer.produce(metaImage);
			return metaImage;
		} else {
			MetaImageGamma metaImage = new MetaImageGamma(stat, view_tile_size, x_start, y_start, width, height, gamma);
			tileMetaProducer.produce(metaImage);
			return metaImage;
		}
	}
}
