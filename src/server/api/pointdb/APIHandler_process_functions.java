package server.api.pointdb;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONWriter;

import broker.Broker;
import pointdb.process.Functions;
import pointdb.process.ProcessingFun;
import util.JsonUtil;
import util.Web;
import util.rdat.RdatDataFrame;

public class APIHandler_process_functions extends PointdbAPIHandler {
	//private static final Logger log = LogManager.getLogger();	

	public APIHandler_process_functions(Broker broker) {
		super(broker, "process_functions");		
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		request.setHandled(true);

		String format = Web.getString(request, "format", "json");

		switch(format) {
		case "json": {
			response.setContentType(Web.MIME_JSON);
			JSONWriter json = new JSONWriter(response.getWriter());
			json.object();
			json.key("functions");
			json.array();
			for(ProcessingFun f:Functions.getFunctions()) {
				json.object();
				json.key("name");
				json.value(f.name);
				json.key("description");
				json.value(f.description);
				JsonUtil.writeOptArray(json, "tags", f.tags);
				json.endObject();
			}
			json.endArray();
			json.key("tags");
			json.array();
			for(String tag:Functions.getTags()) {
				json.value(tag);				
			}
			json.endArray();
			json.endObject();
			break;
		}
		case "rdat": {
			RdatDataFrame<ProcessingFun> df = new RdatDataFrame<ProcessingFun>(Collection::size);
			df.addString("name", ProcessingFun::getName);
			df.addString("description", ProcessingFun::getDescription);
			df.write(response, Functions.getFunctions());
			break;
		}
		default:
			throw new RuntimeException("unknown format: "+format);
		}

	}
}
