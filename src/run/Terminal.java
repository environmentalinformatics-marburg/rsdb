package run;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gdal.gdal.gdal;
import org.json.JSONArray;
import org.json.JSONObject;

import broker.Broker;
import pointcloud.AttributeSelector;
import pointcloud.CellTable;
import pointcloud.Importer;
import pointcloud.PointCloud;
import pointcloud.Rebuild;
import pointdb.IndexRasterizer;
import pointdb.PointDB;
import pointdb.Rasterizer;
import pointdb.base.Rect;
import pointdb.process.Fun_BE;
import pointdb.process.ProcessingFun;
import pointdb.processing.tilekey.StatisticsCollector;
import rasterdb.GeoReference;
import rasterdb.RasterDB;
import rasterdb.importer.Import_banded;
import rasterdb.importer.Import_landsat8;
import rasterdb.importer.Import_modis;
import rasterdb.importer.Import_rapideye;
import rasterdb.importer.Import_soda;
import rasterdb.importer.RasterDBimporter;
import remotetask.Context;
import remotetask.RemoteTask;
import remotetask.RemoteTaskExecutor;
import remotetask.RemoteTaskInfo;
import remotetask.RemoteTasks;
import server.RunServer;
import util.TimeUtil;
import util.Timer;

public class Terminal {
	private static final Logger log = LogManager.getLogger();

	@FunctionalInterface
	interface CommandFunc {
		public void run(String[] args) throws Exception;
	}

	static class Command<T> {
		public final String name;
		public final T call;
		public Command(String name, T call) {
			this.name = name;
			this.call = call;
		}
	}

	static Map<String,Command<CommandFunc>> commandMap;

	private static void addCommand(String name, CommandFunc call) {
		commandMap.put(name, new Command<CommandFunc>(name,call));
	}

	static {
		commandMap = new TreeMap<String,Command<CommandFunc>>();
		addCommand("commands", Terminal::commands);
		addCommand("server", Terminal::server);
		addCommand("import", Terminal::command_import);
		addCommand("import_server", Terminal::command_import_server);
		addCommand("gdal", Terminal::command_gdal);
		addCommand("refresh_indexed_storage", Terminal::command_refresh_indexed_storage);
		addCommand("terminal", args->interactiveTerminal());
		addCommand("exec", Terminal::command_exec);
		addCommand("rasterdb", Terminal::command_rasterdb);
		addCommand("import_landsat8", Terminal::command_import_landsat8);
		addCommand("import_rapideye", Terminal::command_import_rapideye);
		addCommand("import_modis", Terminal::command_import_modis);
		addCommand("import_soda", Terminal::command_import_soda);
		addCommand("modis_preprocess", Terminal::command_modis_preprocess);
		addCommand("catalog_refresh", Terminal::command_catalog_refresh);
		addCommand("rasterize_pointdb", Terminal::command_rasterize_pointdb);
		addCommand("index_rasterize_pointdb", Terminal::command_index_rasterize_pointdb);
		addCommand("import_pointcloud", Terminal::command_import_pointcloud);
		addCommand("read_pointcloud", Terminal::command_read_pointcloud);
		addCommand("rasterize_pointcloud", Terminal::command_rasterize_pointcloud);
		addCommand("recompress_pointcloud", Terminal::command_recompress_pointcloud);
		addCommand("task", Terminal::command_task);
		addCommand("tasks", Terminal::command_tasks);
		addCommand("echo", Terminal::command_echo);
	}

	@FunctionalInterface
	interface CommandRasterdbFunc {
		public void run(String rastedbName, String[] args) throws Exception;
	}

	static Map<String,Command<CommandRasterdbFunc>> commandRasterdbMap;

	private static void addCommandRasterdb(String name, CommandRasterdbFunc call) {
		commandRasterdbMap.put(name, new Command<CommandRasterdbFunc>(name,call));
	}

	static {
		commandRasterdbMap = new TreeMap<String,Command<CommandRasterdbFunc>>();
		addCommandRasterdb("set", Terminal::command_rasterdb_set);
		addCommandRasterdb("import", Terminal::command_rasterdb_import);
		addCommandRasterdb("import_banded", Terminal::command_rasterdb_import_banded);
		addCommandRasterdb("rebuild_pyramid", Terminal::command_rasterdb_rebuild_pyramid);
	}



	public static void server(String[] args) {
		RunServer.main(args);
	}

	public static void commands(String[] args) {
		for(Command<?> command:commandMap.values()) {
			System.out.println(command.name);
		}
		for(Command<?> command:commandRasterdbMap.values()) {
			System.out.println("rasterdb "+command.name);
		}
	}

	public static void command_import(String[] args) {
		if(args.length!=1+2) {
			throw new RuntimeException("unknown parameter count "+Arrays.toString(args));
		}
		//String name = args[2];
		switch(args[1]) {
		case "pointdb": {
			Timer.start("import");
			Broker broker = new Broker();
			PointDB pointdb = broker.getPointdb(args[2], true);
			log.info(pointdb.config);
			RunImport runImport = new RunImport(pointdb);
			runImport.loadAll();
			broker.close();
			log.info(Timer.stop("import"));
			break;
		}
		default:
			log.error("import type unknown: "+args[1]);
		}

	}

	public static void command_refresh_indexed_storage(String[] args) throws IOException {
		if(args.length!=1+2) {
			throw new RuntimeException("unknown parameter count "+Arrays.toString(args));
		}
		String name = args[2];
		switch(args[1]) {
		case "pointdb": {
			try(Broker broker = new Broker()) {
				PointDB db = broker.getPointdb(name);
				db.refreshIndexedStorage();
			}
			break;
		}
		default:
			log.error("type unknown: "+args[1]);
		}

	}

	public static void command_gdal(String[] args) {
		log.info("GDAL version "+gdal.VersionInfo());
	}

	public static void command_import_server(String[] args) {
		if(args.length!=1+2) {
			throw new RuntimeException("unknown parameter count "+Arrays.toString(args));
		}
		Broker broker = new Broker();
		try {
			new Thread(()->RunServer.run(broker)).start();
		} catch (Exception e) {
			log.error(e);
		}
		//String name = args[2];
		switch(args[1]) {
		case "pointdb": {
			Timer.start("import");
			PointDB pointdb = broker.getPointdb(args[2], true);
			log.info(pointdb.config);
			RunImport runImport = new RunImport(pointdb);
			runImport.loadAll();
			log.info(Timer.stop("import"));
			break;
		}
		default:
			log.error("import type unknown: "+args[1]);
		}
		broker.close();
	}

	public static void test(String[] args) {
		if(args.length!=1+1) {
			throw new RuntimeException("unknown parameter count "+Arrays.toString(args));
		}
		System.out.println(args[1]);
	}

	public static void main(String[] args) {
		run(args);		
	}

	public static void run(String... args) {
		try {
			if(args.length==0) {
				//interactiveTerminal();
				System.out.println("no command.\n\nAvailable commands:\n");
				runCommand(new String[]{"commands"});
			} else {
				runCommand(args);
			}
		} catch(Exception e) {
			log.error(e);
			e.printStackTrace();
		}
		System.exit(0);
	}


	private interface FlexConsole {
		public static FlexConsole create() {
			Console console = System.console();
			if(console==null) {
				log.warn("no full terminal");
				return new SimpleConsole();
			}
			return new FullConsole(console);
		}

		public void printf(String format, Object ... args);
		public void flush();
		public String readLine();

	}

	private static class FullConsole implements FlexConsole {
		private final Console console;

		public FullConsole(Console console) {
			this.console = console;
		}

		@Override
		public void printf(String format, Object ... args) {
			console.printf(format, args);			
		}

		@Override
		public void flush() {
			console.flush();			
		}

		@Override
		public String readLine() {
			return console.readLine();
		}

	}

	private static class SimpleConsole implements FlexConsole {

		private BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		public SimpleConsole() {}

		@Override
		public void printf(String format, Object... args) {
			System.out.printf(format, args);

		}

		@Override
		public void flush() {
			System.out.flush();

		}

		@Override
		public String readLine() {
			try {
				return in.readLine();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void interactiveTerminal() throws IOException {
		FlexConsole console = FlexConsole.create();
		if(console==null) {
			System.err.println("error: no terminal");
			return;
		}
		System.out.println("");
		System.out.println("------ PointDB Terminal ------");
		System.out.println("Type 'commands' to get a listing of available commands.");
		System.out.println("Type 'exit' to return.");
		System.out.println("");
		for(;;){
			console.printf("\n%s", "PointDB$ ");
			console.flush();
			String input = console.readLine();
			if(input!=null&&input.equals("exit")) {
				return;
			}
			if(input!=null&&!input.isEmpty()) {
				runInteractiveCommand(input);
			}			
		}
	}

	public static void runInteractiveCommand(String arg) throws IOException {
		if(arg.trim().isEmpty()) {
			return;
		}
		String[] args = arg.split(" ");
		runCommand(args);
	}


	public static String[] mergeQuote(String[] args) {
		//log.info("merge " + Arrays.toString(args));
		ArrayList<String> result = new ArrayList<String>();
		boolean quoteOne = false;
		boolean quoteTwo = false;
		boolean quoteParenthesis = false;
		String argCollector = "";
		for(int i=0;i<args.length;i++) {
			String arg = args[i];
			if(!(quoteOne || quoteTwo || quoteParenthesis)) {
				if(arg.startsWith("'")) {
					quoteOne = true;
					arg = arg.substring(1);
				} else if(arg.startsWith("\"")) {
					quoteTwo = true;
					arg = arg.substring(1);
				} else if (arg.startsWith("{")) {
					quoteParenthesis = true;
				} else {
					result.add(arg);
				}
				argCollector = "";
			}
			if(quoteOne || quoteTwo || quoteParenthesis) {
				if(quoteOne && arg.endsWith("'")) {
					quoteOne = false;
					if(!argCollector.isEmpty()) {
						argCollector += ' ';
					}
					argCollector += arg.substring(0, argCollector.length() - 1);
					result.add(argCollector);
					argCollector = "";
				} else if(quoteTwo && arg.endsWith("\"")) {
					quoteTwo = false;
					if(!argCollector.isEmpty()) {
						argCollector += ' ';
					}
					argCollector += arg.substring(0, argCollector.length() - 1);
					result.add(argCollector);
					argCollector = "";
				} else if(quoteParenthesis && arg.endsWith("}")) {
					quoteParenthesis = false;
					if(!argCollector.isEmpty()) {
						argCollector += ' ';
					}
					argCollector += arg;
					result.add(argCollector);
					argCollector = "";
				} else {
					if(!argCollector.isEmpty()) {
						argCollector += ' ';
					}
					argCollector += arg;
				}
			}
		}
		if(quoteOne) {
			throw new RuntimeException("error in commandline quotes (missing closing quote ' )");
		}
		if(quoteTwo) {
			throw new RuntimeException("error in commandline quotes (missing closing quote \" )");
		}
		if(quoteParenthesis) {
			throw new RuntimeException("error in commandline quotes (missing closing quote } )");
		}
		return result.toArray(new String[0]);
	}


	public static String[] mergeQuote_OLD(String[] args) {
		ArrayList<String> res = new ArrayList<String>();
		boolean quote = false;
		String quoteString = "";
		for(int i=0;i<args.length;i++) {
			String arg = args[i];
			boolean containsQuote = arg.indexOf('\'')>=0 || arg.indexOf('"')>=0;
			if(quote) {
				if(containsQuote) {
					quoteString += ' ' + arg;
					quoteString = quoteString.replaceAll("'", "");
					quoteString = quoteString.replaceAll("\"", "");
					res.add(quoteString);
					quote = false;
				} else {
					quoteString += ' ' + arg;
				}
			} else {
				if(containsQuote) {
					quoteString = arg;
					quote = true;
				} else {
					res.add(arg);
				}
			}
		}
		if(quote) {
			quoteString = quoteString.replaceAll("'", "");
			quoteString = quoteString.replaceAll("\"", "");
			res.add(quoteString);
		}
		return res.toArray(new String[0]);
	}

	public static void runCommand(String[] args) {
		args = mergeQuote(args);
		Command<CommandFunc> command = commandMap.get(args[0]);
		if(command!=null) {
			try {
				command.call.run(args);
			} catch(Exception e) {
				e.printStackTrace();
				log.error(e);
			}
		} else {
			log.error("error unknown command: "+args[0]);
		}
	}

	private static final String[] NO_ARGS = new String[]{};

	public static void command_exec(String[] args) {
		try {
			Class<?> clazz = Class.forName(args[1]);
			Method method = clazz.getMethod("main", String[].class);
			String[] execArgs = NO_ARGS;
			int len = args.length - 2;
			if(len > 0) {
				execArgs = new String[len];
				for (int i = 0; i < len; i++) {
					execArgs[i] = args[i + 2];
				}
			}
			method.invoke(null, new Object[]{execArgs});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void command_rasterdb(String[] args) {
		if(3<=args.length) {
			String rasterdbName = args[1];
			String subCommand = args[2];
			String[] execArgs = NO_ARGS;
			int len = args.length - 3;
			if(len > 0) {
				execArgs = new String[len];
				for (int i = 0; i < len; i++) {
					execArgs[i] = args[i + 3];
				}
			}
			System.out.println("command "+subCommand+" : "+Arrays.toString(execArgs));

			Command<CommandRasterdbFunc> command = commandRasterdbMap.get(subCommand);
			if(command!=null) {
				try {
					command.call.run(rasterdbName, execArgs);
				} catch(Exception e) {
					e.printStackTrace();
					log.error(e);
				}
			} else {
				log.error("error unknown command: "+subCommand);
			}

		} else {
			System.out.println("no command specified");
		}
	}

	public static void command_rasterdb_set(String rasterdbName, String[] args) {
		System.out.println("command_rasterdb_set "+Arrays.toString(args));
		if(2<=args.length) {
			String key = args[0];
			String value = args[1];

			try(Broker broker = new Broker()) {
				RasterDB rasterdb = broker.getRasterdb(rasterdbName);
				switch(key) {
				case GeoReference.PROPERTY_CODE:
					rasterdb.setCode(value);
					break;
				case GeoReference.PROPERTY_PROJ4:
					rasterdb.setProj4(value);
					break;					
				default:
					System.out.println("unknown key: "+key);
				}
			}
		} else {
			System.out.println("missing name key value");
		}
	}

	public static void command_rasterdb_import(String rasterdbName, String[] args) {
		System.out.println("command_rasterdb_import "+Arrays.toString(args));
		if(args.length == 1 || args.length == 2) {
			String path = args[0];
			int timestamp = 0;
			if(args.length==2) {
				String timestampText = args[1].trim();
				log.info(timestampText);
				timestamp = TimeUtil.parseIsoTimestamp(timestampText);
				log.info(timestamp);
			}
			try(Broker broker = new Broker()) {
				RasterDB rasterdb = broker.createOrGetRasterdb(rasterdbName);
				RasterDBimporter importer = new RasterDBimporter(rasterdb);
				try {
					importer.importDirectoryRecursive(Paths.get(path), timestamp);
				} catch (Exception e) {
					log.error(e);
				}
				rasterdb.rebuildPyramid();
			} catch(Exception e){
				e.printStackTrace();
				log.error(e);
			}
		} else {
			System.out.println("need one parameter as import path and one optional timestamp: "+Arrays.toString(args));
		}
	}

	public static void command_rasterdb_import_banded(String rasterdbName, String[] args) {
		System.out.println("command_rasterdb_import_banded "+Arrays.toString(args));
		if(args.length == 1) {
			String path = args[0];
			try(Broker broker = new Broker()) {
				RasterDB rasterdb = broker.createOrGetRasterdb(rasterdbName);
				Import_banded importer = new Import_banded(rasterdb);

				/*try {
					new Thread(()->RunServer.run(broker)).start();
				} catch (Exception e) {
					log.error(e);
				}*/

				try {
					importer.importDirectoryRecursive(Paths.get(path));
				} catch (Exception e) {
					log.error(e);
				}
				rasterdb.rebuildPyramid();
			} catch(Exception e){
				log.error(e);
			}
		} else {
			System.out.println("need one parameter as import path: "+Arrays.toString(args));
		}
	}

	public static void command_rasterdb_rebuild_pyramid(String rasterdbName, String[] args) {
		System.out.println("command_rasterdb_rebuild_pyramid "+Arrays.toString(args));
		if(args.length == 0) {
			try(Broker broker = new Broker()) {
				RasterDB rasterdb = broker.createOrGetRasterdb(rasterdbName);
				rasterdb.rebuildPyramid();
			} catch(Exception e){
				log.error(e);
			}
		} else {
			System.out.println("no parameters needed: "+Arrays.toString(args));
		}
	}

	public static void command_import_landsat8(String[] args) {
		if(args.length == 3) {
			String name = args[1];
			String source = args[2];			
			log.info("import_landsat8 "+name+" from "+source);
			try(Broker broker = new Broker()) {
				Import_landsat8 importer = new Import_landsat8(broker, name);
				importer.importDirectory(Paths.get(source));
			} catch (Exception e) {
				log.error(e);
			}
		} else {
			System.out.println("command import_landsat8 needs 2 parameters");
		}
	}

	public static void command_import_rapideye(String[] args) {
		if(args.length == 3) {
			String name = args[1];
			String source = args[2];			
			log.info("import_rapideye "+name+" from "+source);
			try(Broker broker = new Broker()) {
				Import_rapideye importer = new Import_rapideye(broker, name);
				importer.importDirectory(Paths.get(source));
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e);
			}
		} else {
			System.out.println("command import_rapideye needs 2 parameters");
		}
	}

	public static void command_import_modis(String[] args) {
		if(args.length == 3) {
			String name = args[1];
			String source = args[2];			
			log.info("import_modis "+name+" from "+source);
			try(Broker broker = new Broker()) {
				Import_modis importer = new Import_modis(broker, name);
				importer.importDirectory(Paths.get(source));
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e);
			}
		} else {
			System.out.println("command import_modis needs 2 parameters");
		}
	}

	public static void command_import_soda(String[] args) {
		if(args.length == 3) {
			String name = args[1];
			String source = args[2];			
			log.info("import_soda "+name+" from "+source);
			try(Broker broker = new Broker()) {
				Import_soda importer = new Import_soda(broker, name);
				importer.importDirectory(Paths.get(source));
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e);
			}
		} else {
			System.out.println("command import_soda needs 2 parameters");
		}
	}

	public static void command_modis_preprocess(String[] args) {
		if(args.length == 3) {
			String source = args[1];
			String target = args[2];			
			ModisPreprocess.convertDirectory(Paths.get(source), Paths.get(target));
		} else {
			System.out.println("command import_modis needs 2 parameters");
		}
	}

	public static void command_catalog_refresh(String[] args) {
		if(args.length == 1) {
			try(Broker broker = new Broker()) {
				broker.catalog.rebuildCatalog();
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e);
			}
		} else {
			System.out.println("catalog_refresh does not have arguments");
		}
	}

	public static void command_rasterize_pointdb(String[] args) {
		log.info("command_rasterize_pointdb " + Arrays.toString(args));
		if(args.length == 3) {
			if(args[1].isEmpty()) {
				System.out.println("command_rasterize_pointdb empty pointdb parameter");
				return;
			}
			if(args[2].isEmpty()) {
				System.out.println("command_rasterize_pointdb empty rasterdb parameter");
				return;
			}
			try(Broker broker = new Broker()) {
				PointDB pointdb = broker.getPointdb(args[1]);			
				RasterDB rasterdb = broker.createOrGetRasterdb(args[2]);
				Rasterizer rasterizer = new Rasterizer(pointdb, rasterdb);
				rasterizer.run(rasterizer.bandIntensity);
				rasterizer.run(rasterizer.bandElevation);
				rasterdb.flush();
				rasterdb.rebuildPyramid();
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e);
			}
		} else {
			System.out.println("command_rasterize_pointdb does not have 2 arguments");
		}
	}

	public static void command_import_pointcloud(String[] args) {
		log.info("command_import_pointcloud " + Arrays.toString(args));
		if(args.length >= 3) {
			try(Broker broker = new Broker()) {
				String name = args[1];
				String storage_type = "RasterUnit";
				String source = args[2];	
				PointCloud pointcloud = broker.createNewPointCloud(name, storage_type, false);
				for (int i = 3; i < args.length; i++) {
					String arg = args[i];
					String argCellSize = "-cellsize=";
					if(arg.startsWith(argCellSize)) {
						String value = arg.substring(argCellSize.length());
						try {
							double cellsize = Double.parseDouble(value);
							if(pointcloud.trySetCellsize(cellsize)) {
								log.info("set cellsize " + cellsize);
							} else {
								log.warn("could not set cellsize " + cellsize);
							}
						} catch(Exception e) {
							throw new RuntimeException("error in arg: " + arg +"        " + e);
						}
					} else {
						throw new RuntimeException("unknown arg: " + arg);
					}
				}
				Path root = Paths.get(source);
				Importer importer = new Importer(pointcloud);
				Timer.start("total import");
				importer.importDirectory(root);
				pointcloud.getGriddb().getStorage().flush();
				log.info(Timer.stop("total import"));
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e);
			}
		} else {
			System.out.println("command_import_pointcloud does not have 2 arguments");
		}
	}

	public static void command_read_pointcloud(String[] args) {
		log.info("command_read_pointcloud " + Arrays.toString(args));
		if(args.length == 1) {
			try(Broker broker = new Broker()) {
				PointCloud pointcloud = broker.getPointCloud("test");
				Timer.start("full read");
				AttributeSelector selector = new AttributeSelector(true);
				Stream<CellTable> cellTables = pointcloud.getCellTables(-Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, selector);
				cellTables.sequential().forEach(cellTable -> {
					log.info(cellTable);
					if(cellTable.rows >= 100000 && cellTable.gpsTime != null) {
						for (int i = 0; i < 100000; i++) {
							if(cellTable.edgeOfFlightLine != null && cellTable.edgeOfFlightLine.get(i)) {
								log.info(i+"   "+cellTable.gpsTime[i]+"  "+cellTable.x[i]+"  "+(cellTable.scanDirectionFlag == null ? "-" : cellTable.scanDirectionFlag.get(i))+" "+ (cellTable.edgeOfFlightLine == null ? "-" : cellTable.edgeOfFlightLine.get(i)));
							}
						}
					}
				});
				log.info(Timer.stop("full read"));
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e);
			}
		} else {
			System.out.println("command_read_pointcloud does not have 1 arguments");
		}
	}

	public static void command_rasterize_pointcloud(String[] args) {
		log.info("command_rasterize_pointcloud " + Arrays.toString(args));
		if(args.length == 3) {
			if(args[1].isEmpty()) {
				System.out.println("command_rasterize_pointcloud empty pointcloud parameter");
				return;
			}
			if(args[2].isEmpty()) {
				System.out.println("command_rasterize_pointcloud empty rasterdb parameter");
				return;
			}
			try(Broker broker = new Broker()) {
				PointCloud pointdb = broker.getPointCloud(args[1]);
				RasterDB rasterdb = broker.createNewRasterdb(args[2], false);
				pointcloud.Rasterizer rasterizer = new pointcloud.Rasterizer(pointdb, rasterdb);
				rasterizer.run();
				rasterdb.flush();
				rasterdb.rebuildPyramid();
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e);
			}
		} else {
			System.out.println("command_rasterize_pointcloud does not have 1 arguments");
		}
	}

	public static void command_recompress_pointcloud(String[] args) {
		log.info("command_recompress_pointcloud " + Arrays.toString(args));
		if(args.length == 2) {
			try(Broker broker = new Broker()) {
				PointCloud pointcloud = broker.getPointCloud(args[1]);
				String stroage_type = "RasterUnit";
				boolean recompress = true;
				int comression_level = 100;
				Timer.start("recompress");
				Rebuild rebuild = new Rebuild(pointcloud, broker.getPointCloudRoot(), stroage_type, recompress, comression_level);
				rebuild.run();
				log.info(Timer.stop("recompress"));
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e);
			}
		} else {
			System.out.println("command_recompress_pointcloud does not have 2 arguments");
		}
	}

	public static void command_index_rasterize_pointdb(String[] args) {
		log.info("command_index_rasterize_pointdb " + Arrays.toString(args));
		if(args.length == 3) {
			if(args[1].isEmpty()) {
				System.out.println("command_rasterize_pointdb empty pointdb parameter");
				return;
			}
			if(args[2].isEmpty()) {
				System.out.println("command_rasterize_pointdb empty rasterdb parameter");
				return;
			}
			Timer.start("command_index_rasterize_pointdb");
			try(Broker broker = new Broker()) {
				PointDB pointdb = broker.getPointdb(args[1]);			
				RasterDB rasterdb = broker.createOrGetRasterdb(args[2]);
				Rect rect = StatisticsCollector.collect(pointdb.tileKeyProducer(null)).toRect();
				log.info("limit");
				long xmin = rect.utmm_min_x;
				long ymin = rect.utmm_min_y;
				long xmax = rect.utmm_max_x;
				long ymax = rect.utmm_max_y;
				long xcen = (xmin + xmax) / 2;
				long ycen = (ymin + ymax) / 2;
				long xrange = (xmax-xmin) / 8;
				long yrange = (ymax-ymin) / 8;
				xmin = xcen - xrange / 2;
				ymin = ycen - yrange / 2;
				xmax = xcen + xrange / 2;
				ymax = ycen + yrange / 2;
				rect = Rect.of_UTMM(xmin, ymin, xmax, ymax);
				IndexRasterizer rasterizer = new IndexRasterizer(pointdb, rasterdb, 5, rect);
				ProcessingFun processingFun = new Fun_BE.Fun_BE_ELEV_MEAN();
				rasterizer.process(processingFun);
				rasterdb.flush();
				rasterdb.rebuildPyramid();
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e);
			}
			log.info(Timer.stop("command_index_rasterize_pointdb"));
		} else {
			System.out.println("command_rasterize_pointdb does not have 2 arguments");
		}
	}

	public static void command_task(String[] args) {
		log.info("command_task " + Arrays.toString(args));
		if(args.length == 1) {
			FlexConsole console = FlexConsole.create();
			console.printf("\n%s", "task$ ");
			console.flush();
			String input = console.readLine();
			JSONObject task = new JSONObject(input);
			try(Broker broker = new Broker()) {
				Context ctx = new Context(broker, task, null);
				RemoteTask remoteTask = RemoteTaskExecutor.createTask(ctx);
				remoteTask.run();
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e);
			}
		} else if(args.length == 2) {
			log.info("JSON "+ args[1]);
			JSONObject task = new JSONObject(args[1]);
			try(Broker broker = new Broker()) {
				Context ctx = new Context(broker, task, null);
				RemoteTask remoteTask = RemoteTaskExecutor.createTask(ctx);
				remoteTask.run();
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e);
			}
		} else {
			System.out.println("command_task can only have zero or two args: " + (args.length - 1));
		}		
	}

	public static void command_tasks(String[] args) {
		log.info("command_tasks " + Arrays.toString(args));
		if(args.length == 1) {
			Map<String, TreeMap<String, RemoteTaskInfo>> map = RemoteTasks.list();
			for(Entry<String, TreeMap<String, RemoteTaskInfo>> eCat:map.entrySet()) {
				String task_category = eCat.getKey();
				for(RemoteTaskInfo rti:eCat.getValue().values()) {
					System.out.println(task_category+":\t\t" + rti.name);
				}
			}
		} else {
			System.out.println("command_tasks can only have zero args: " + (args.length - 1));
		}		
	}

	public static void command_echo(String[] args) {
		log.info("command_echo " + Arrays.toString(args));
		for (int i = 0; i < args.length; i++) {
			log.info(i+". [" + args[i] + "]");
		}
	}
}
