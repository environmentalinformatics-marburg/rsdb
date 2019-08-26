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
import remotetask.Context;
import remotetask.RemoteTask;
import remotetask.RemoteTaskExecutor;
import server.api.APIHandler;
import util.Web;

public class APIHandler_remote_tasks extends APIHandler {
	private static final Logger log = LogManager.getLogger();

	public APIHandler_remote_tasks(Broker broker) {
		super(broker, "remote_tasks");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		if(target.isEmpty()) {
			handleRoot(request, response);
		} else {
			log.info("target [" + target + "]");
			String idText = target;
			String subTarget = "";
			int idEndIndex = target.indexOf('/');
			if(idEndIndex>=0) {
				idText = target.substring(0, idEndIndex);
				subTarget = target.substring(idEndIndex + 1);
			}			
			long id = Long.parseLong(idText);
			handleId(id, subTarget, request, response);
		}		
	}

	protected void handleRoot(Request request, Response response) throws IOException {
		switch(request.getMethod()) {
		case "GET":
			handleRootGET(request, response);
			break;
		case "POST":
			handleRootPOST(request, response);
			break;
		default:
			throw new RuntimeException("invalid HTTP method: " + request.getMethod());
		}		
	}

	protected void handleRootGET(Request request, Response response) throws IOException {
		response.setContentType(MIME_JSON);		
		JSONWriter res = new JSONWriter(response.getWriter());
		res.object();
		res.key("remote_tasks");
		res.array();
		for(RemoteTask remoteTask:RemoteTaskExecutor.getRemoteTasks()) {
			res.object();
			res.key("id");
			res.value(remoteTask.id);
			res.key("name");
			res.value(remoteTask.getName());
			res.key("status");
			res.value(remoteTask.getStatus());
			res.key("runtime");
			res.value(remoteTask.getRuntimeMillis());
			res.key("message");
			res.value(remoteTask.getMessage());
			res.key("cancelable");
			res.value(remoteTask.isCancelable());
			res.key("canceled");
			res.value(remoteTask.isCanceled());
			res.endObject();
		}
		res.endArray();
		res.endObject();
	}

	protected void handleRootPOST(Request request, Response response) throws IOException {
		JSONObject json = new JSONObject(Web.requestContentToString(request));
		JSONObject args = json.getJSONObject("remote_task");
		log.info(args);
		UserIdentity userIdentity = Web.getUserIdentity(request);
		Context ctx = new Context(broker, args, userIdentity);
		RemoteTask remoteTask = RemoteTaskExecutor.createTask(ctx);
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

	private void handleId(long id, String target, Request request, Response response) throws IOException {
		log.info(id + "   "+ target);
		if(target.isEmpty()) {
			handleIdRoot(id, request, response);
		} else {
			switch(request.getMethod()) {
			case "POST":
				handleIdCancel(id, request, response);
				break;
			default:
				throw new RuntimeException("invalid HTTP method: " + request.getMethod());
			}
		}
	}

	private void handleIdRoot(long id, Request request, Response response) throws IOException {
		switch(request.getMethod()) {
		case "GET":
			handleIdGET(id, request, response);
			break;
		default:
			throw new RuntimeException("invalid HTTP method: " + request.getMethod());
		}
	}
	
	private void handleIdCancel(long id, Request request, Response response) throws IOException {
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
		} else {
			remoteTask.cancel();
			response.setContentType(MIME_JSON);
			JSONWriter res = new JSONWriter(response.getWriter());
			res.object();
			res.key("message");
			res.object();	
			res.key("remote_task cancel requested");
			res.value(id);
			res.endObject();
			res.endObject();
		}
	}

	private void handleIdGET(long id, Request request, Response response) throws IOException {
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
		res.key("cancelable");
		res.value(remoteTask.isCancelable());
		res.key("canceled");
		res.value(remoteTask.isCanceled());
		res.endObject();
		res.endObject();		
	}
}
