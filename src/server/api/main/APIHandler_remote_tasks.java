package server.api.main;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.json.JSONWriter;

import broker.Broker;
import remotetask.RemoteTask;
import remotetask.RemoteTaskExecutor;
import server.api.APIHandler;

public class APIHandler_remote_tasks extends APIHandler {
	//private static final Logger log = LogManager.getLogger();

	public APIHandler_remote_tasks(Broker broker) {
		super(broker, "remote_tasks");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		//log.info("target " + target);
		String idText = target;
		int i = idText.indexOf('/');
		if(i>=0) {
			idText = idText.substring(0, i);
		}			
		long id = Long.parseLong(idText);
		
		RemoteTask remoteTask = RemoteTaskExecutor.getTaskByID(id);
		if(remoteTask == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType(MIME_JSON);
			JSONWriter res = new JSONWriter(response.getWriter());
			res.object();
			res.key("error");
			res.object();	
			res.key("unknown remote_task");
			res.value(id);
			res.endObject();
			res.endObject();
		}
		
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter res = new JSONWriter(response.getWriter());
		res.object();
		res.key("remote_task");
		res.object();	
		res.key("id");
		res.value(remoteTask.id);
		res.key("status");
		res.value(remoteTask.getStatus());
		res.key("active");
		res.value(remoteTask.isActive());
		res.key("runtime");
		res.value(remoteTask.getRuntimeMillis());
		res.key("message");
		res.value(remoteTask.getMessage());
		res.endObject();
		res.endObject();		
	}
}
