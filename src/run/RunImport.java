package run;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


import org.tinylog.Logger;

import pointdb.Loader;
import pointdb.PointDB;
import pointdb.base.PointdbConfig;
import util.Timer;

public class RunImport {
	

	private final PointDB pointdb;
	private final Loader loader;

	public RunImport(PointDB pointdb) {
		this.pointdb = pointdb;
		this.loader = new Loader(this.pointdb);
	}

	public void loadAll() {
		PointdbConfig config = pointdb.config;
		Logger.info("load "+config.getImportDirectories());
		for(Path dir:config.getImportDirectories()) {
			loadDirectory(dir);
		}
		Logger.info("load "+config.getImportFiles());
		for(Path file:config.getImportFiles()) {
			loadFile(file);
		}
	}

	/**
	 * load directory of files.
	 * <br>
	 * currently not recursive
	 * @param dir
	 */
	public void loadDirectory(Path dir) {
		try {
			DirectoryStream<Path> filesStream = Files.newDirectoryStream(dir);
			List<Path> files = new ArrayList<Path>();
			for(Path path:filesStream) {
				if(path.toFile().isFile()) {
					if(path.toString().toLowerCase().endsWith(".las") || path.toString().toLowerCase().endsWith(".laz")) {
						files.add(path);
					}
				} else {
					Logger.warn("subdirectory not read "+path);
				}
			}

			Logger.info("load directory "+dir+"   "+files.size()+" files");
			files.sort((Path o1, Path o2)->o1.toString().compareTo(o2.toString()));

			int total_files = files.size();
			int count = 0;
			for(Path path:files) {
				count++;
				Timer.start("file");
				long r = loadFile(path);
				Timer.stop("file");
				if(r>=0) {
					Logger.info("loaded file "+path+"     ("+count+"/"+total_files+")   "+r+" points  "+Timer.get("file"));
				} else {
					Logger.info("loaded file "+path+"     ("+count+"/"+total_files+")   "+r+" error  "+Timer.get("file"));
				}
			}
		} catch (IOException e) {
			Logger.error(e);
		}

	}

	private long loadFile(Path filename) {
		try {
			//return loader.load2(filename);
			return loader.load3(filename);
		} catch (Exception e) {
			//e.getStackTrace()[0].
			//Logger.error(e);
			Logger.error("read ",e);
			return -1;
		}
	}

}
