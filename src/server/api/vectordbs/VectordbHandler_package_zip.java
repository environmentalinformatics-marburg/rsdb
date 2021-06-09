package server.api.vectordbs;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.yaml.snakeyaml.Yaml;

import broker.Broker;
import broker.Informal;
import broker.InformalProperties.Builder;
import util.Extent2d;
import vectordb.VectorDB;

public class VectordbHandler_package_zip extends VectordbHandler {
	private static final Logger log = LogManager.getLogger();

	private static final String META_FILE_NAME = "metadata.yaml";

	public VectordbHandler_package_zip(Broker broker) {
		super(broker, "package.zip");
	}

	@Override
	public void handleGET(VectorDB vectordb, String target, Request request, Response response, UserIdentity userIdentity) throws IOException {		
		try {
			List<Path> filenames = vectordb.getDataFilenames();
			Path root = vectordb.getDataPath();
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/zip");
			ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
			zipOutputStream.setLevel(Deflater.NO_COMPRESSION);

			for(Path filename : filenames) {
				String entryName = filename.toString();
				if(entryName.equals(META_FILE_NAME)) {
					log.warn("overwrite existing metadata.yaml with generated vector layer metadata in package    " + vectordb.getName());
					continue;
				}
				zipOutputStream.putNextEntry(new ZipEntry(entryName ));
				Path filePath = root.resolve(filename);
				Files.copy(filePath, zipOutputStream);
				zipOutputStream.closeEntry();
			}
			write_dublin_core(vectordb, zipOutputStream);

			zipOutputStream.finish();
			zipOutputStream.flush();
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println(e);
		}		
	}

	private void write_dublin_core(VectorDB vectordb, ZipOutputStream zipOutputStream) throws IOException {
		zipOutputStream.putNextEntry(new ZipEntry("metadata.yaml"));
		try {
			write_dublin_core_metadata(vectordb, zipOutputStream);
		} finally {
			zipOutputStream.closeEntry();	
		}		
	}

	private void write_dublin_core_metadata(VectorDB vectordb, OutputStream out) {
		Informal informal = vectordb.informal();

		Builder properties = informal.toBuilder().properties;

		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("identifier", vectordb.getName());
		if(informal.hasTitle()) {
			properties.prepend("title", informal.title);
		}
		if(!informal.tags.isEmpty()) {
			properties.prepend("subject", informal.tags);
		}
		if(informal.hasDescription()) {
			properties.prepend("description.abstract", informal.description);
		}
		if(informal.hasAcquisition_date()) {
			properties.prepend("date.created", informal.acquisition_date);
		}
		if(informal.has_corresponding_contact()) {
			properties.prepend("publisher", informal.corresponding_contact);
		}
		properties.prepend("type", "vector");		
		properties.prepend("format", "vector-file");

		Extent2d ext = null;
		try {
			ext = vectordb.getExtent();
		} catch(Exception e) {
			log.warn(e);
		}
		if(ext != null) {
			String coverage = "extent (" + ext.xmin + ", " + ext.ymin + " to " + ext.xmax + ", " + ext.ymax + ")";
			VectordbDetails details = vectordb.getDetails();
			if(!details.epsg.isEmpty() || !details.proj4.isEmpty()) {
				coverage += "   in ";
				if(!details.epsg.isEmpty()) {
					coverage += "EPSG:" + details.epsg;
					if(!details.proj4.isEmpty()) {
						coverage += "    ";
					}
				}
				if(!details.proj4.isEmpty()) {
					coverage += "PROJ4: " + details.proj4;
				}
			}
			properties.prepend("coverage", coverage);
		}

		Map<String, Object> outMap = properties.build().toSortedYaml();

		Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
		new Yaml().dump(outMap, writer);
	}
}
