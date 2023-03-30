package server.api.main;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONWriter;

import broker.Broker;
import remotetask.RemoteTaskInfo;
import remotetask.RemoteTaskParameter;
import remotetask.RemoteTasks;
import server.api.APIHandler;
import util.Web;

public class APIHandler_remote_task_entries extends APIHandler {
	
	public APIHandler_remote_task_entries(Broker broker) {
		super(broker, "remote_task_entries");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		if(target.isEmpty()) {
			handleRoot(request, response);
		} else {
			throw new RuntimeException("unknown");
		}		
	}

	protected void handleRoot(Request request, Response response) throws IOException {
		switch(request.getMethod()) {
		case "GET":
			handleRootGET(request, response);
			break;
		default:
			throw new RuntimeException("invalid HTTP method: " + request.getMethod());
		}		
	}

	protected void handleRootGET(Request request, Response response) throws IOException {
		response.setContentType(Web.MIME_JSON);		
		JSONWriter res = new JSONWriter(response.getWriter());
		res.object();
		res.key("remote_task_categories");
		res.array();		
		Map<String, TreeMap<String, RemoteTaskInfo>> map = RemoteTasks.list();
		for(Entry<String, TreeMap<String, RemoteTaskInfo>> eCat:map.entrySet()) {
			String task_category = eCat.getKey();
			res.object();
			res.key("category");
			res.value(task_category);
			res.key("remote_task_entries");
			res.array();
			for(RemoteTaskInfo rti:eCat.getValue().values()) {
				res.object();
				res.key("name");
				res.value(rti.name);
				if(!rti.description.isEmpty()) {
					res.key("description");
					res.value(rti.description);
				}
				res.key("params");
				res.array();
				for(RemoteTaskParameter param:rti.params) {
					res.object();
					res.key("name");
					res.value(param.name);
					res.key("type");
					res.value(param.type);
					res.key("desc");
					res.value(param.desc);
					res.key("format");
					res.value(param.format);
					res.key("example");
					res.value(param.example);
					res.key("required");
					res.value(param.required);
					res.endObject();
				}
				res.endArray();
				res.endObject();				
			}
			res.endArray();
			res.endObject();
		}
		res.endArray();
		res.endObject();
	}	
}
