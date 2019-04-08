package pointcloud;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Phaser;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import griddb.Cell;
import rasterunit.Tile;

public class Recompress {
	private static final Logger log = LogManager.getLogger();


	private final PointCloud src;
	private final Path rootPath;

	public Recompress(PointCloud src, Path rootPath) {
		this.src = src;
		this.rootPath = rootPath;
	};

	private static class TileProcessor implements Consumer<Tile> {

		private final PointCloud dst;

		private int PIPELINE_SIZE = 256;
		private Tile[] pipeline_src = new Tile[PIPELINE_SIZE];
		private Tile[] pipeline_dst = new Tile[PIPELINE_SIZE];
		private int pipeline_src_pos = 0;

		public TileProcessor(PointCloud dst) {
			this.dst = dst;
		}

		@Override
		public void accept(Tile tile_src) {
			pipeline_src[pipeline_src_pos++] = tile_src;
			if(pipeline_src_pos == PIPELINE_SIZE) {
				flush();
			}
		}

		public void flush() {
			Phaser phaser = new Phaser();
			phaser.register();
			ForkJoinPool exe = ForkJoinPool.commonPool();
			for (int i = 0; i < pipeline_src_pos; i++) {
				phaser.register();
				final Tile tile_src = pipeline_src[i];
				final int pos = i;
				exe.execute(()->{
					// byte[] data_dst = Cell.recompressData(tile_src.data, 6);
					byte[] data_dst = Cell.recompressData(tile_src.data, 100);
					Tile tile_dst = new Tile(tile_src.t, tile_src.b, tile_src.y, tile_src.x, tile_src.type, data_dst);			
					pipeline_dst[pos] = tile_dst;
					phaser.arrive();
				});
			}
			phaser.arriveAndAwaitAdvance();
			for (int i = 0; i < pipeline_src_pos; i++) {
				Tile tile_dst = pipeline_dst[i];			
				dst.writeTile(tile_dst);
			}
			dst.commit();
			pipeline_src_pos = 0;
		}

	};

	public void run() throws IOException {
		File metaFile_src = src.getMetaFile();
		String name_src = src.getName();
		String name_dst = name_src + "_recompressed";
		Path path_dst = rootPath.resolve(name_dst);
		Path metaFile_dst = path_dst.resolve(metaFile_src.getName());
		log.info(metaFile_src);
		log.info(metaFile_dst);

		File pathfile_dst = path_dst.toFile();
		if(pathfile_dst.exists()) {
			throw new RuntimeException("target already exists: "+path_dst);
		}
		pathfile_dst.mkdirs();

		Files.copy(metaFile_src.toPath(), metaFile_dst);

		PointCloudConfig config = new PointCloudConfig(name_dst, path_dst, false);
		try(PointCloud dst = new PointCloud(config)) {			
			Collection<Tile> tiles = src.getGriddb().getTiles(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
			TileProcessor tileProcessor = new TileProcessor(dst);
			tiles.stream().sequential().forEach(tileProcessor);
			tileProcessor.flush();
		}


	}

}
