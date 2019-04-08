package rasterdb;

import java.nio.file.Path;
import java.nio.file.Paths;

import util.yaml.YamlMap;

public class RasterdbConfig {

	private Path path;
	private String name;
	private boolean fast_unsafe_import;

	private RasterdbConfig() {
		fast_unsafe_import = false;
	}

	public static RasterdbConfig ofYAML(YamlMap map) {
		RasterdbConfig config = new RasterdbConfig();
		map.optFunStringConv("path",  Paths::get, config::setPath);		
		return config;
	}
	
	public static RasterdbConfig ofPath(Path path) {
		RasterdbConfig config = new RasterdbConfig();
		config.setPath(path);		
		return config;
	}
	
	private void setPath(Path path) {
		this.path = path;
		String filename = path.getFileName().toString().trim();
		if(filename.isEmpty()) {
			name = path.toString().trim();
		} else {
			name = filename;
		}		
	}

	public Path getPath() {
		return path;
	}
	
	public String getName() {
		return name;
	}
	
	public void set_fast_unsafe_import(boolean fast_unsafe_import) {
		this.fast_unsafe_import = fast_unsafe_import;
	}
	
	public boolean is_fast_unsafe_import() {
		return fast_unsafe_import;
	}

}
