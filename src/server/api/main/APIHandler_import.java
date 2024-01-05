package server.api.main;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONObject;
import org.json.JSONWriter;

import broker.Broker;
import broker.acl.EmptyACL;
import rasterdb.RasterDB;
import remotetask.Context;
import remotetask.RemoteProxyTask;
import remotetask.RemoteTaskExecutor;
import remotetask.rasterdb.ImportBySpec;
import remotetask.rasterdb.ImportProcessor;
import remotetask.rasterdb.ImportSpec;
import server.api.APIHandler;
import server.api.main.ChunkedUploader.ChunkedUpload;
import util.Web;

public class APIHandler_import extends APIHandler {
	

	private final ChunkedUploader chunkedUploader;

	public APIHandler_import(Broker broker, ChunkedUploader chunkedUploader) {
		super(broker, "import");
		this.chunkedUploader = chunkedUploader;
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {

		JSONObject json = new JSONObject(Web.requestContentToString(request));
		JSONObject specification = json.getJSONObject("specification");
		Logger.info(specification);
		String filename = specification.getString("filename");
		ChunkedUpload chunkedUpload = chunkedUploader.uploadMap.get(filename);
		Path path;
		if(chunkedUpload == null) {
			//throw new RuntimeException("file not found");
			Logger.warn("old session ? ");
			path = Paths.get("temp/raster", filename);
		} else {
			path = chunkedUpload.path;
		}
		
		String id = specification.getString("id");
		UserIdentity userIdentity = Web.getUserIdentity(request);
		if(broker.hasRasterdb(id)) {
			Logger.info("not admin");
			RasterDB rasterdb = broker.getRasterdb(id);
			rasterdb.checkMod(userIdentity);
			Logger.info("allowed");
		}

		ImportSpec spec = new ImportSpec();
		spec.parse(specification);
		ImportProcessor importProcessor = ImportBySpec.importPerpare(broker, path, id, spec, userIdentity);
		
		Context ctx = new Context(broker, null, userIdentity);
		RemoteProxyTask remoteProxyTask = RemoteTaskExecutor.insertToExecute(importProcessor, ctx);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
		JSONWriter res = new JSONWriter(response.getWriter());
		res.object();
		res.key("remote_task");
		res.object();	
		res.key("id");
		res.value(remoteProxyTask.id);
		res.endObject();
		res.endObject();		
	}
	

}
