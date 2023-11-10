package server.api.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import jakarta.servlet.http.HttpServletResponse;


import org.tinylog.Logger;
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


	public APIHandler_remote_tasks(Broker broker) {
		super(broker, "remote_tasks");
	}

	@Override
	protected void handle(String target, Request request, Response response) throws IOException {
		if(target.isEmpty()) {
			handleRoot(request, response);
		} else {
			//Logger.info("target [" + target + "]");
			String idText = target;
			String subTarget = "";
			int idEndIndex = target.indexOf('/');
			if(idEndIndex>=0) {
				idText = target.substring(0, idEndIndex);
				long id = Long.parseLong(idText);
				subTarget = target.substring(idEndIndex + 1);
				switch(subTarget) {
				case "log": {
					handleLog(id, request, response);
					return;
				}
				case "cancel": {
					handleIdCancel(id, request, response);
					return;
				}
				default:
					throw new RuntimeException("unknown target: " + subTarget);
				}
			}
			long id = Long.parseLong(idText);
			handleId(id, subTarget, request, response);
		}		
	}

	private void handleLog(long id, Request request, Response response) throws IOException {
		RemoteTask remoteTask = RemoteTaskExecutor.getTaskByID(id);
		if(remoteTask == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType(Web.MIME_JSON);
			JSONWriter res = new JSONWriter(response.getWriter());
			res.object();
			res.key("error");
			res.object();	
			res.key("unknown remote_task");
			res.value(id);
			res.endObject();
			res.endObject();
			return;
		}

		response.setContentType(Web.MIME_TEXT);	
		PrintWriter out = response.getWriter();
		for(String line:remoteTask.getLog()) {
			out.println(line);
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
		boolean identity = Web.getFlag(request, "identity");

		response.setContentType(Web.MIME_JSON);		
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
			if(identity && remoteTask.ctx != null && remoteTask.ctx.userIdentity != null) {
				res.key("identity");
				res.value(remoteTask.ctx.userIdentity.getUserPrincipal().getName());
			}
			res.endObject();
		}
		res.endArray();
		res.endObject();
	}

	protected void handleRootPOST(Request request, Response response) throws IOException {
		JSONObject json = new JSONObject(Web.requestContentToString(request));
		JSONObject args = json.getJSONObject("remote_task");
		Logger.info(args);
		UserIdentity userIdentity = Web.getUserIdentity(request);
		Context ctx = new Context(broker, args, userIdentity);
		RemoteTask remoteTask = RemoteTaskExecutor.createTask(ctx);
		RemoteTaskExecutor.insertToExecute(remoteTask);


		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
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
		//Logger.info(id + "   "+ target);
		if(target.isEmpty()) {
			handleIdRoot(id, request, response);
		} else {
			switch(request.getMethod()) {
			case "POST":
				handleIdCancel(id, request, response); //TODO check if remove
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
			response.setContentType(Web.MIME_JSON);
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
			response.setContentType(Web.MIME_JSON);
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
	
	private static final ZonedDateTime TIME_ORIGIN = ZonedDateTime.of(LocalDateTime.of(1970, 1, 1, 0, 0, 0), Clock.systemUTC().getZone()).withZoneSameInstant(Clock.systemDefaultZone().getZone());
	private static final DateTimeFormatter ZONED_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy'T'HH:mm:ssX");

	private void handleIdGET(long id, Request request, Response response) throws IOException {
		RemoteTask remoteTask = RemoteTaskExecutor.getTaskByID(id);
		if(remoteTask == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType(Web.MIME_JSON);
			JSONWriter res = new JSONWriter(response.getWriter());
			res.object();
			res.key("error");
			res.object();	
			res.key("unknown remote_task");
			res.value(id);
			res.endObject();
			res.endObject();
			return;
		}

		boolean task = Web.getFlag(request, "task");
		boolean identity = Web.getFlag(request, "identity");

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(Web.MIME_JSON);
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
		long tstart = remoteTask.getStartMillis();
		if(tstart > 0) {
			ZonedDateTime zonedDateTime = TIME_ORIGIN.plus(tstart, ChronoUnit.MILLIS);
			res.key("start");
			res.value(zonedDateTime.format(ZONED_FORMATTER));
		}
		long tend = remoteTask.getEndMillis();
		if(tend > 0) {			
			ZonedDateTime zonedDateTime = TIME_ORIGIN.plus(tend, ChronoUnit.MILLIS);
			res.key("end");
			res.value(zonedDateTime.format(ZONED_FORMATTER));
		}
		res.key("message");
		res.value(remoteTask.getMessage());
		res.key("cancelable");
		res.value(remoteTask.isCancelable());
		res.key("canceled");
		res.value(remoteTask.isCanceled());
		res.key("log");
		res.value(remoteTask.getLog());		
		if(task && remoteTask.ctx != null && remoteTask.ctx.task != null) {
			res.key("task");
			res.value(remoteTask.ctx.task);
		}
		if(identity && remoteTask.ctx != null && remoteTask.ctx.userIdentity != null) {
			res.key("identity");
			res.value(remoteTask.ctx.userIdentity.getUserPrincipal().getName());
		}
		res.endObject();
		res.endObject();		
	}
}
