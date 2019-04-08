package run;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.Util;

public class ModisPreprocess {
	private static final Logger log = LogManager.getLogger();

	public static void call(String... args) {
		try {
			List<String> argsList = Arrays.asList(args);
			log.info(argsList);
			ProcessBuilder pb = new ProcessBuilder(argsList);
			pb.redirectOutput(Redirect.INHERIT);
			pb.redirectError(Redirect.INHERIT);
			pb.redirectInput(Redirect.INHERIT);
			Process process = pb.start();
			process.waitFor();
		} catch(Exception e) {
			log.error(e);
		}
	}

	public static ArrayList<String> callCollect(String... args) {
		ArrayList<String> lines = new ArrayList<String>();
		try {
			List<String> argsList = Arrays.asList(args);
			log.info(argsList);
			ProcessBuilder pb = new ProcessBuilder(argsList);
			//pb.redirectOutput(Redirect.INHERIT);
			pb.redirectError(Redirect.INHERIT);
			pb.redirectInput(Redirect.INHERIT);
			Process process = pb.start();
			process.getInputStream();
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String s = null;
			while ((s = stdInput.readLine()) != null)
			{
				//System.out.println(s);
				lines.add(s);
			}
			process.waitFor();
		} catch(Exception e) {
			log.error(e);
		}
		return lines;
	}

	public static ArrayList<String> gdal_info(String inFile) {
		return callCollect("gdalinfo", inFile);
	}

	public static ArrayList<String> getModisDatasets(String filename) {
		ArrayList<String> datasets = new ArrayList<String>();
		ArrayList<String> lines = gdal_info(filename);
		for(String line:lines) {
			line = line.trim();
			if(line.startsWith("SUBDATASET_")) {
				String cName = "_NAME=";
				int i = line.indexOf(cName);
				if(i>=0) {
					String dataset = line.substring(i + cName.length());
					datasets.add(dataset);
				}				
			}
		}
		return datasets;
	}

	public static void gdal_translate(String inFile, String outFile) {
		call("gdal_translate", inFile, outFile);
	}

	public static void gdal_translate_modis(String inFile, String dataset, String outFile) {		
		String source = "HDF4_EOS:EOS_GRID:" + inFile + ':' + dataset;		
		call("gdal_translate", source, outFile);
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		Path sourceDir = Paths.get("temp/modis");
		Path target = Paths.get("temp/bale_modis");
		convertDirectory(sourceDir, target);
		log.info("-------------------------");
	}

	public static void convertDirectory(Path root, Path target){
		try {
			for(Path path:Util.getPaths(root)) {
				if(path.toFile().isFile()) {
					if(path.getFileName().toString().endsWith("hdf")) {
						log.info("import hdf "+path);
						convertFile(path, target);
					}
				} else if(path.toFile().isDirectory()) {
					convertDirectory(path, target);
				}
			}
		} catch(Exception e) {
			log.error(e);
		}
	}
	
	public static final DateTimeFormatter DATE_TIME_FORMATER2 = DateTimeFormatter.ofPattern("yyyy_MM_dd__HH_mm");

	public static void convertFile(Path source, Path target) {
		String filename = source.getFileName().toString();
		String name = filename.substring(0, filename.lastIndexOf('.'));

		String dateTextPre = name.substring(name.indexOf(".A")+2);
		String dateText = dateTextPre.substring(0, dateTextPre.indexOf('.'));

		DateTimeFormatter DATE_TIME_FORMATER = DateTimeFormatter.ofPattern("yyyyDDD");
		LocalDate date = LocalDate.parse(dateText, DATE_TIME_FORMATER);
		LocalDateTime datetime = LocalDateTime.of(date, LocalTime.MIDNIGHT);

		

		log.info(datetime);

		Path datsetsTarget = target.resolve(datetime.format(DATE_TIME_FORMATER2)+"__"+name);
		datsetsTarget.toFile().mkdirs();
		log.info(datsetsTarget);

		ArrayList<String> datasets = getModisDatasets(source.toString());
		for(String dataset:datasets) {
			String shortdatasetName = dataset.substring(dataset.lastIndexOf(':')+1);			
			log.info(dataset);
			Path datasetTarget = datsetsTarget.resolve(shortdatasetName+".tif");
			log.info(datasetTarget);
			gdal_translate(dataset.toString(), datasetTarget.toString());
		}
	}

}
