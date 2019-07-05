package remotetask.pointdb;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONObject;

import broker.Broker;
import broker.acl.EmptyACL;
import pointdb.PointDB;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import run.RunImport;

@task_pointdb("import")
@Description("import directory with LAS/LAZ files (recursive)")
@Param(name="pointdb", desc="ID of PointDB layer")
@Param(name="source", desc="source directory of files")
public class Task_import extends RemoteTask {
	//private static final Logger log = LogManager.getLogger();

	private final Broker broker;
	private final JSONObject task;

	public Task_import(Context ctx) {
		this.broker = ctx.broker;
		this.task = ctx.task;
		EmptyACL.ADMIN.check(ctx.userIdentity);
	}

	@Override
	public void process() {
		String name = task.getString("pointdb");
		String source = task.getString("source");
		Path path = Paths.get(source);
		PointDB pointdb = broker.getPointdb(name, true);
		RunImport runImport = new RunImport(pointdb);
		runImport.loadDirectory(path);
	}
}
