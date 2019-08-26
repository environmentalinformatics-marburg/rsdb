package rasterunit;

import java.io.File;
import java.nio.file.Path;

public class TileStorageConfig {

	public final Path path;
	public final String prefix;
	
	public final Path storagePath;
	public final Path indexPath;
	public final File dirtyFile;
	public final boolean create;

	public TileStorageConfig(Path root, String prefix, TileStorageOption ...options) {
		boolean create = false;
		for(TileStorageOption option:options) {
			switch (option) {
			case CREATE:
				create = true;
				break;
			default:
				throw new RuntimeException("unknown option: " + option);
			}
		}		
		this.path = root;
		this.prefix = prefix;
		this.storagePath = path.resolve(prefix + ".tst");
		this.indexPath = path.resolve(prefix + ".idx");
		this.dirtyFile = path.resolve(prefix + ".DIRTY").toFile();
		this.create = create;
	}
}
