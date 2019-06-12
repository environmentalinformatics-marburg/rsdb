package remotetask;

import java.lang.reflect.Constructor;

import util.collections.ReadonlyList;
import util.collections.vec.Vec;

public class RemoteTaskInfo {
	public final String name;
	public final Constructor<? extends RemoteTask> constructor;
	public final String description;
	public final ReadonlyList<RemoteTaskParameter> params;

	public RemoteTaskInfo(String name, Constructor<? extends RemoteTask> constructor, String description, Vec<RemoteTaskParameter> params) {
		this.name = name;
		this.constructor = constructor;
		this.description = description;
		this.params = params.readonlyWeakView();
	}
}