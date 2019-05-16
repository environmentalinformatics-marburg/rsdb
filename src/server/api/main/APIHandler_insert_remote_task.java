package server.api.main;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONObject;
import org.json.JSONWriter;

import broker.Broker;
import remotetask.RemoteTask;
import remotetask.RemoteTaskExecutor;
import remotetask.RemoteTasks;
import server.api.APIHandler;
import util.Web;

public class APIHandler_insert_remote_task extends APIHandler {
	private static final Logger log = LogManager.getLogger();

	public APIHandler_insert_remote_task(Broker broker) {
		super(broker, "insert_remote_task");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		
		JSONObject json = new JSONObject(Web.requestContentToString(request));
		JSONObject args = json.getJSONObject("remote_task");
		log.info(args);
		UserIdentity userIdentity = Web.getUserIdentity(request);
		RemoteTask remoteTask = RemoteTaskExecutor.createTask(broker, args, userIdentity);
		RemoteTaskExecutor.insertToExecute(remoteTask);
		
		
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MIME_JSON);
		JSONWriter res = new JSONWriter(response.getWriter());
		res.object();
		res.key("remote_task");
		res.object();	
		res.key("id");
		res.value(remoteTask.id);		
		res.endObject();
		res.endObject();		
	}
}