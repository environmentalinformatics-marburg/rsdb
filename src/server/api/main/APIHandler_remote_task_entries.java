package server.api.main;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONWriter;

import broker.Broker;
import remotetask.RemoteTask;
import remotetask.RemoteTasks;
import server.api.APIHandler;

public class APIHandler_remote_task_entries extends APIHandler {
	private static final Logger log = LogManager.getLogger();

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
		response.setContentType(MIME_JSON);		
		JSONWriter res = new JSONWriter(response.getWriter());
		res.object();
		res.key("remote_task_categories");
		res.array();		
		Map<String, TreeMap<String, Constructor<? extends RemoteTask>>> map = RemoteTasks.list();
		for(Entry<String, TreeMap<String, Constructor<? extends RemoteTask>>> eCat:map.entrySet()) {
			String task_category = eCat.getKey();
			res.object();
			res.key("category");
			res.value(task_category);
			res.key("remote_task_entries");
			res.array();
			for(Entry<String, Constructor<? extends RemoteTask>> e:eCat.getValue().entrySet()) {
				String name = e.getKey();
				res.object();
				res.key("name");
				res.value(name);
				res.endObject();				
			}
			res.endArray();
			res.endObject();
		}
		res.endArray();
		res.endObject();
	}	
}
