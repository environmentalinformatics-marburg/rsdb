package remotetask.pointdb;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONObject;

import broker.Broker;
import broker.acl.AclUtil;
import pointdb.PointDB;
import remotetask.Context;
import remotetask.Description;
import remotetask.Param;
import remotetask.RemoteTask;
import run.RunImport;

@task_pointdb("import")
@Description("Import directory with LAS/LAZ files. (recursive)")
@Param(name="pointdb", type="layer_id", desc="ID of existing (possibly empty) PointDB layer.", example="pointdb1")
@Param(name="source", desc="Source directory of files. (located on server)", format="path", example="las/folder1")
public class Task_import extends RemoteTask {
	//

	private final Broker broker;
	private final JSONObject task;

	public Task_import(Context ctx) {
		super(ctx);
		this.broker = ctx.broker;
		this.task = ctx.task;
		AclUtil.check(ctx.userIdentity, "task pointdb import");
	}

	@Override
	public void process() {
		String name = task.getString("pointdb");
		String source = task.getString("source");
		Path path = Paths.get(source);
		PointDB pointdb = broker.getPointdb(name, true);
		RunImport runImport = new RunImport(pointdb);
		setMessage("import");
		runImport.loadDirectory(path);
	}
}