package postgis;

import java.nio.file.Path;

import org.eclipse.jetty.server.UserIdentity;

public abstract class PostgisLayerBase {

	public final String name;
	public final Path path;

	public PostgisLayerBase(String name, Path path) {
		this.name = name;
		this.path = path;
	}

	
	public abstract boolean isAllowed(UserIdentity userIdentity);
}
