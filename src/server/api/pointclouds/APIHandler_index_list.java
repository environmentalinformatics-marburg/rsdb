package server.api.pointclouds;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.json.JSONWriter;

import broker.Broker;
import pointcloud.PointCloud;
import pointdb.process.AbstractProcessingFun;
import pointdb.process.Functions;
import pointdb.process.ProcessingFun;
import util.Web;

public class APIHandler_index_list {

	//private final Broker broker;

	public APIHandler_index_list(Broker broker) {
		//this.broker = broker;
	}

	public void handle(PointCloud pointcloud, String format, Request request, HttpServletResponse response) throws IOException {
		response.setContentType(Web.MIME_JSON);
		JSONWriter json = new JSONWriter(response.getWriter());
		json.object();
		json.key("index_list");
		json.array();
		for(AbstractProcessingFun f : Functions.getFunctions()) {
			json.object();
			json.key("name");
			json.value(f.name);
			json.key("description");
			json.value(f.description);
			json.endObject();
		}
		json.endArray();
		json.endObject();
	}

}
