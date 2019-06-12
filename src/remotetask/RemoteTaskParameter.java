package remotetask;

public class RemoteTaskParameter {
	public final String name;
	public final String type;
	public final String desc;
	public final boolean required;

	public RemoteTaskParameter(String name, String type, String desc, boolean required) {
		this.name = name;
		this.type = type;
		this.desc = desc;
		this.required = required;
	}
	
	public static RemoteTaskParameter of(Param p) {
		String name = p.name();
		String type = p.type();
		String desc = p.desc();
		boolean required = p.required();
		return new RemoteTaskParameter(name, type, desc, required);
	}
}