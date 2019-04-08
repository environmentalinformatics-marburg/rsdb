package vectordb;

import java.io.File;
import java.nio.file.Path;

public class VectordbConfig {
	public final String name;
	public final Path path;
	public final Path metaPath;
	public final File metaFile;
	public final Path metaTempPath;
	public final File metaTempFile;
	public final Path dataPath;
	public final File dataFile;

	public VectordbConfig(String name, Path path) {
		this.name = name;
		this.path = path;
		this.metaPath = path.resolve("meta.yml");
		this.metaFile = this.metaPath.toFile();
		this.metaTempPath = path.resolve("meta.temp.yml");
		this.metaTempFile = this.metaTempPath.toFile();
		this.dataPath = path.resolve("data");
		this.dataFile = this.dataPath.toFile();
	}

	public static VectordbConfig ofPath(Path path) {
		String name = pathToName(path);
		return new VectordbConfig(name, path);
	}

	private static String pathToName(Path path) {
		String filename = path.getFileName().toString().trim();
		if(filename.isEmpty()) {
			return path.toString().trim();
		} else {
			return filename;
		}		
	}
}
