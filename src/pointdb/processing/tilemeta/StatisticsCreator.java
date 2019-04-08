package pointdb.processing.tilemeta;

import pointdb.base.PdbConst;
import pointdb.base.TileMeta;

public class StatisticsCreator {
	private final TileMetaProducer tileMetaProducer;
	
	public StatisticsCreator(TileMetaProducer tileMetaProducer) {
		this.tileMetaProducer = tileMetaProducer;
	}
	
	public static class Statistics {
		protected Statistics(){}
		
		public int local_x_min = Integer.MAX_VALUE;
		public int local_x_max = Integer.MIN_VALUE;
		public int local_y_min = Integer.MAX_VALUE;
		public int local_y_max = Integer.MIN_VALUE;
		public int local_z_min = Integer.MAX_VALUE;
		public int local_z_max = Integer.MIN_VALUE;
		public long local_z_sum = 0;
		public int local_z_avg = 0;
		public long utmm_x_min = Long.MAX_VALUE;
		public long utmm_x_max = Long.MIN_VALUE;
		public long utmm_y_min = Long.MAX_VALUE;
		public long utmm_y_max = Long.MIN_VALUE;
		public int intensity_min = Integer.MAX_VALUE;
		public int intensity_max = Integer.MIN_VALUE;
		public long intensity_sum = 0;
		public int intensity_avg = 0;
		public int returnNumber_min = Integer.MAX_VALUE;
		public int returnNumber_max = Integer.MIN_VALUE;
		public int returns_min = Integer.MAX_VALUE;
		public int returns_max = Integer.MIN_VALUE;
		public int scanAngleRank_min = Integer.MAX_VALUE;
		public int scanAngleRank_max = Integer.MIN_VALUE;

		public long point_count_sum = 0;
		public int point_count_min = Integer.MAX_VALUE;
		public int point_count_max = Integer.MIN_VALUE;
		public int point_count_avg = 0;

		public long tile_sum = 0;

		public int tile_x_min = Integer.MAX_VALUE;
		public int tile_x_max = Integer.MIN_VALUE;
		public int tile_y_min = Integer.MAX_VALUE;
		public int tile_y_max = Integer.MIN_VALUE;

		public int tile_z_avg_min = Integer.MAX_VALUE;
		public int tile_z_avg_max = Integer.MIN_VALUE;
		public int tile_intensity_avg_min = Integer.MAX_VALUE;
		public int tile_intensity_avg_max = Integer.MIN_VALUE;

		public int tile_significant_z_avg_min = Integer.MAX_VALUE;
		public int tile_significant_z_avg_max = Integer.MIN_VALUE;
		public int tile_significant_intensity_avg_min = Integer.MAX_VALUE;
		public int tile_significant_intensity_avg_max = Integer.MIN_VALUE;
		public long local_significant_z_sum = 0;
		public int local_significant_z_avg = 0;
		public long intensity_significant_sum = 0;
		public int intensity_significant_avg = 0;
		public long point_count_significant_sum = 0;
		public long tile_significant_sum = 0;
		public int point_count_significant_avg = 0;
		public int point_count_significant_min = Integer.MAX_VALUE;
		public int point_count_significant_max = Integer.MIN_VALUE;
		
		public int itensity_range() {
			return intensity_max-intensity_min;
		}

		public int x_range() {
			return local_x_max-local_x_min;
		}

		public int y_range() {
			return local_y_max-local_y_min;
		}

		public int z_range() {
			return local_z_max-local_z_min;
		}

		public int returnNumber_range() {
			return returnNumber_max-returnNumber_min;
		}

		public int returns_range() {
			return returns_max-returns_min;
		}

		public int scanAngleRank_range() {
			return scanAngleRank_max-scanAngleRank_min;
		}
	}
	
	private static class StatisticsInternal extends Statistics implements TileMetaConsumer {
		@Override
		public void nextTileMeta(TileMeta tileMeta) {
			if(tileMeta.min.intensity<intensity_min) intensity_min = tileMeta.min.intensity;
			if(intensity_max<tileMeta.max.intensity) intensity_max = tileMeta.max.intensity;
			intensity_sum += ((long)tileMeta.avg.intensity)*tileMeta.point_count;
			if(tileMeta.min.x<local_x_min) local_x_min = tileMeta.min.x;
			if(local_x_max<tileMeta.max.x) local_x_max = tileMeta.max.x;
			if(tileMeta.min.y<local_y_min) local_y_min = tileMeta.min.y;
			if(local_y_max<tileMeta.max.y) local_y_max = tileMeta.max.y;
			if(tileMeta.min.z<local_z_min) local_z_min = tileMeta.min.z;
			if(local_z_max<tileMeta.max.z) local_z_max = tileMeta.max.z;
			local_z_sum += ((long)tileMeta.avg.z)*tileMeta.point_count;

			long x = ((long)tileMeta.x)*PdbConst.LOCAL_SCALE_FACTOR;
			long y = ((long)tileMeta.y)*PdbConst.LOCAL_SCALE_FACTOR; 
			if(x+tileMeta.min.x<utmm_x_min) utmm_x_min = x+tileMeta.min.x;
			if(y+tileMeta.min.y<utmm_y_min) utmm_y_min = y+tileMeta.min.y;		
			if(x+tileMeta.max.x>utmm_x_max) utmm_x_max = x+tileMeta.max.x;
			if(y+tileMeta.max.y>utmm_y_max) utmm_y_max = y+tileMeta.max.y;


			if(tileMeta.min.returnNumber<returnNumber_min) returnNumber_min = tileMeta.min.returnNumber;
			if(returnNumber_max<tileMeta.max.returnNumber) returnNumber_max = tileMeta.max.returnNumber;
			if(tileMeta.min.returns<returns_min) returns_min = tileMeta.min.returns;
			if(returns_max<tileMeta.max.returns) returns_max = tileMeta.max.returns;
			if(tileMeta.min.scanAngleRank<scanAngleRank_min) scanAngleRank_min = tileMeta.min.scanAngleRank;
			if(scanAngleRank_max<tileMeta.max.scanAngleRank) scanAngleRank_max = tileMeta.max.scanAngleRank;

			if(tileMeta.point_count<point_count_min) point_count_min = tileMeta.point_count;
			if(point_count_max<tileMeta.point_count) point_count_max = tileMeta.point_count;
			point_count_sum += tileMeta.point_count;

			if(tileMeta.x<tile_x_min) tile_x_min = tileMeta.x;
			if(tile_x_max<tileMeta.x) tile_x_max = tileMeta.x; 
			if(tileMeta.y<tile_y_min) tile_y_min = tileMeta.y;
			if(tile_y_max<tileMeta.y) tile_y_max = tileMeta.y;

			if(tileMeta.avg.z<tile_z_avg_min) tile_z_avg_min = tileMeta.avg.z;
			if(tile_z_avg_max<tileMeta.avg.z) tile_z_avg_max = tileMeta.avg.z;

			if(tileMeta.avg.intensity<tile_intensity_avg_min) tile_intensity_avg_min = tileMeta.avg.intensity;
			if(tile_intensity_avg_max<tileMeta.avg.intensity) tile_intensity_avg_max = tileMeta.avg.intensity;

			if(tileMeta.point_count>=PdbConst.MIN_SIGNIFICANT_TILE_POINT_COUNT) { // tile is significant
				if(tileMeta.point_count<point_count_significant_min) point_count_significant_min = tileMeta.point_count;
				if(point_count_significant_max<tileMeta.point_count) point_count_significant_max = tileMeta.point_count;

				if(tileMeta.avg.z<tile_significant_z_avg_min) tile_significant_z_avg_min = tileMeta.avg.z;
				if(tile_significant_z_avg_max<tileMeta.avg.z) tile_significant_z_avg_max = tileMeta.avg.z;

				if(tileMeta.avg.intensity<tile_significant_intensity_avg_min) tile_significant_intensity_avg_min = tileMeta.avg.intensity;
				if(tile_significant_intensity_avg_max<tileMeta.avg.intensity) tile_significant_intensity_avg_max = tileMeta.avg.intensity;

				local_significant_z_sum += ((long)tileMeta.avg.z)*tileMeta.point_count;
				intensity_significant_sum += ((long)tileMeta.avg.intensity)*tileMeta.point_count;

				point_count_significant_sum += tileMeta.point_count;

				tile_significant_sum++;
			}

			tile_sum++;			
		}
		
		public void finishedCalc() {
			if(point_count_sum>0) {
				local_z_avg = (int) (local_z_sum/point_count_sum);
				intensity_avg = (int) (intensity_sum/point_count_sum);
				if(tile_sum>0) point_count_avg = (int) (point_count_sum/tile_sum);
			}

			if(point_count_significant_sum>0) {
				local_significant_z_avg = (int) (local_significant_z_sum/point_count_significant_sum);
				intensity_significant_avg = (int) (intensity_significant_sum/point_count_significant_sum);
				if(tile_significant_sum>0) point_count_significant_avg = (int) (point_count_significant_sum/tile_significant_sum);
			}
			
			if(point_count_sum==0) { //TODO add all variables
				tile_x_min = 0;
				tile_x_max = 0;
				tile_y_min = 0;
				tile_y_max = 0;			
			}
		}
	}
	
	public Statistics create() {
		StatisticsInternal tileMetatStatistics = new StatisticsInternal();
		tileMetaProducer.produce(tileMetatStatistics);
		tileMetatStatistics.finishedCalc();
		return tileMetatStatistics;
	}
	
	public static Statistics direct(TileMetaProducer tileMetaProducer) {
		return new StatisticsCreator(tileMetaProducer).create();
	}
}
