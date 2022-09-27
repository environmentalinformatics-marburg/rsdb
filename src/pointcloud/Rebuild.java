package pointcloud;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Spliterator;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Phaser;
import java.util.function.Consumer;

import org.eclipse.jetty.server.UserIdentity;
import org.tinylog.Logger;

import broker.acl.ACL;
import griddb.Attribute;
import griddb.Cell;
import rasterunit.Tile;
import rasterunit.TileKey;
import remotetask.CancelableRemoteProxy;
import util.collections.ReadonlyNavigableSetView;

public class Rebuild extends CancelableRemoteProxy {
	

	private final PointCloud src;
	private final Path rootPath;
	private final String storage_type;
	private final boolean recompress;
	private final int compression_level;
	private final UserIdentity userIdentity;

	public Rebuild(PointCloud src, Path rootPath, String storage_type, boolean recompress, int compression_level, UserIdentity userIdentity) {
		this.src = src;
		this.rootPath = rootPath;
		this.storage_type = storage_type;
		this.recompress = recompress;
		this.compression_level = compression_level;
		this.userIdentity = userIdentity;
	};

	private static class TileProcessor implements Consumer<Tile> {

		private final PointCloud dst;
		private final boolean recompress;
		private final int compression_level;

		private int PIPELINE_SIZE = 256;
		private Tile[] pipeline_src = new Tile[PIPELINE_SIZE];
		private Tile[] pipeline_dst = new Tile[PIPELINE_SIZE];
		private int pipeline_src_pos = 0;

		public TileProcessor(PointCloud dst, boolean recompress, int compression_level) {
			this.dst = dst;
			this.recompress = recompress;
			this.compression_level = compression_level;
		}

		@Override
		public void accept(Tile tile_src) {
			pipeline_src[pipeline_src_pos++] = tile_src;
			if(pipeline_src_pos == PIPELINE_SIZE) {
				flush();
			}
		}

		public void flush() {
			try {
				if(recompress) {
					Phaser phaser = new Phaser();
					phaser.register();
					ForkJoinPool exe = ForkJoinPool.commonPool();
					for (int i = 0; i < pipeline_src_pos; i++) {
						phaser.register();
						final Tile tile_src = pipeline_src[i];
						final int pos = i;
						exe.execute(()->{
							// byte[] data_dst = Cell.recompressData(tile_src.data, 6);
							byte[] data_dst = Cell.recompressData(tile_src.data, compression_level);
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
				} else {
					for (int i = 0; i < pipeline_src_pos; i++) {
						Tile tile_src = pipeline_src[i];
						dst.writeTile(tile_src);						
					}					
					dst.commit();
					pipeline_src_pos = 0;
				}
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}
	};

	@Override
	public void process() throws Exception {
		File metaFile_src = src.getMetaFile();
		String name_src = src.getName();
		String name_dst = name_src + "_rebuild";
		Path path_dst = rootPath.resolve(name_dst);
		Path metaFile_dst = path_dst.resolve(metaFile_src.getName());
		Logger.info(metaFile_src);
		Logger.info(metaFile_dst);

		File pathfile_dst = path_dst.toFile();
		if(pathfile_dst.exists()) {
			throw new RuntimeException("target already exists: "+path_dst);
		}
		pathfile_dst.mkdirs();

		//Files.copy(metaFile_src.toPath(), metaFile_dst); copy meta from src to dst

		PointCloudConfig config = new PointCloudConfig(name_dst, path_dst, storage_type, false);
		try(PointCloud dst = new PointCloud(config)) {			
			for(Attribute attribute : src.getGriddb().getAttributes()) {
				Attribute dstAttribute = dst.getGriddb().getOrAddAttribute(attribute.name, attribute.encoding);
				if(!attribute.equals(dstAttribute)) {
					throw new RuntimeException("attribute create error");
				}
			}
			dst.trySetCellscale(src.getCellscale());
			dst.trySetCellsize(src.getCellsize());
			dst.getOrSetCelloffset(src.getCelloffset().x, src.getCelloffset().y);
			dst.setCode(src.getCode());
			dst.setProj4(src.getProj4());
			dst.setACL(src.getACL());
			dst.setACL_mod(src.getACL_mod());
			if(userIdentity != null) {
				String username = userIdentity.getUserPrincipal().getName();
				dst.setACL_owner(ACL.ofRole(username));
			}
			dst.setAssociatedRasterDB(src.getAssociated().getRasterDB());
			dst.setInformal(src.informal());
			dst.commitMeta();

			ReadonlyNavigableSetView<TileKey> tileKeys = src.getTileKeys();
			int tileSize = tileKeys.size();
			TileProcessor tileProcessor = new TileProcessor(dst, recompress, compression_level);
			int tileCounter = 0;
			for(TileKey tileKey : tileKeys) {
				if(isCanceled()) {
					throw new RuntimeException("canceled");
				}
				Tile tile = src.getGriddb().storage().readTile(tileKey);
				tileProcessor.accept(tile);
				tileCounter++;
				setMessage(tileCounter + " of " + tileSize + " tiles processed  " + (( ((long)tileCounter) * 100) / tileSize) + "%");
			}
			tileProcessor.flush();
			
			/*Collection<Tile> tiles = src.getGriddb().getTiles(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
			int tileSize = tiles.size();
			TileProcessor tileProcessor = new TileProcessor(dst, recompress, compression_level);
			//tiles.stream().sequential().forEach(tileProcessor);
			Spliterator<Tile> spliterator = tiles.stream().sequential().spliterator();
			int tileCounter = 0;
			while(!isCanceled() && spliterator.tryAdvance(tileProcessor)) {
				tileCounter++;
				setMessage(tileCounter + " of " + tileSize + " tiles processed  " + (( ((long)tileCounter) * 100) / tileSize) + "%");
			}			
			tileProcessor.flush();
			if(isCanceled()) {
				throw new RuntimeException("canceled");
			}*/
		}		
	}
}
