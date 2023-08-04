package postgis;

import java.io.File;
import java.nio.file.Path;

import org.eclipse.jetty.server.UserIdentity;

public abstract class PostgisLayerBase {

	public final String name;
	public final Path metaPath;
	public final File metaFile;

	public PostgisLayerBase(String name, Path path) {
		this.name = name;
		this.metaPath = path;
		this.metaFile = path.toFile();
	}
	
	public abstract boolean isAllowed(UserIdentity userIdentity);
}
