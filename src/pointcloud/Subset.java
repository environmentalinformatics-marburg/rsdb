package pointcloud;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Consumer;

import org.eclipse.jetty.server.UserIdentity;
import org.tinylog.Logger;

import broker.TimeSlice;
import broker.acl.ACL;
import griddb.Attribute;
import rasterunit.Tile;
import rasterunit.TileCollection;
import rasterunit.TileKey;
import remotetask.CancelableRemoteProxy;

public class Subset extends CancelableRemoteProxy {	

	private final PointCloud src;
	private final Path rootPath;
	private final UserIdentity userIdentity;
	private final double xmin;
	private final double ymin;
	private final double xmax;
	private final double ymax;

	public Subset(PointCloud src, Path rootPath, UserIdentity userIdentity, int t, double xmin, double ymin, double xmax, double ymax) {
		this.src = src;
		this.rootPath = rootPath;
		this.userIdentity = userIdentity;
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
	};

	private static class TileProcessor implements Consumer<Tile> {

		private final PointCloud dst;

		private int PIPELINE_SIZE = 256;
		private Tile[] pipeline_src = new Tile[PIPELINE_SIZE];
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
			try {
				for (int i = 0; i < pipeline_src_pos; i++) {
					Tile tile_src = pipeline_src[i];
					dst.writeTile(tile_src);						
				}					
				dst.commit();
				pipeline_src_pos = 0;
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}
	};

	@Override
	public void process() throws Exception {
		File metaFile_src = src.getMetaFile();
		String name_src = src.getName();
		String name_dst = name_src + "_subset";
		Path path_dst = rootPath.resolve(name_dst);
		Path metaFile_dst = path_dst.resolve(metaFile_src.getName());
		Logger.info(metaFile_src);
		Logger.info(metaFile_dst);

		File pathfile_dst = path_dst.toFile();
		if(pathfile_dst.exists()) {
			throw new RuntimeException("target already exists: "+path_dst);
		}
		pathfile_dst.mkdirs();

		PointCloudConfig config = new PointCloudConfig(name_dst, path_dst, "TileStorage", false);
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
			dst.setInformal(src.informal());
			dst.commitMeta();

			if(src.timeMapReadonly.isEmpty()) {
				processTimeSlice(TimeSlice.ZERO_UNTITLED, dst);
			} else {
				for(TimeSlice timeSlice : src.timeMapReadonly.values()) {
					processTimeSlice(timeSlice, dst);
				}
			}
		}		
	}

	private void processTimeSlice(TimeSlice timeSlice, PointCloud dst) throws IOException {
		TileCollection tiles = src.getTiles(timeSlice.id, xmin, ymin, xmax, ymax);
		int tileSize = tiles.size();
		setMessage(tileSize + " tiles of " + xmin + ", " + ymin +" to "+ xmax + ", " + ymax + "  at " + timeSlice);
		if(tileSize > 0) {
			if(timeSlice != TimeSlice.ZERO_UNTITLED) {
				dst.setTimeSlice(timeSlice);
			}
			TileProcessor tileProcessor = new TileProcessor(dst);
			int tileCounter = 0;
			Iterator<TileKey> tileKeyIt = tiles.keyIterator();
			while(tileKeyIt.hasNext()) {
				TileKey tileKey = tileKeyIt.next();
				if(isCanceled()) {
					throw new RuntimeException("canceled");
				}
				Tile tile = src.getGriddb().storage().readTile(tileKey);
				tileProcessor.accept(tile);
				tileCounter++;
				setMessage(tileCounter + " of " + tileSize + " tiles processed  " + (( ((long)tileCounter) * 100) / tileSize) + "%   at " + timeSlice);
			}
			tileProcessor.flush();
		}
	}
}
