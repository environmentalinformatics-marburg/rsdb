package remotetask;

public class RemoteTaskParameter {
	public final String name;
	public final String type;
	public final String desc;
	public final String format;
	public final String example;
	public final boolean required;

	public RemoteTaskParameter(String name, String type, String desc, String format, String example, boolean required) {
		this.name = name;
		this.type = type;
		this.desc = desc;
		this.example = example;
		this.format = format;
		this.required = required;
	}

	public static RemoteTaskParameter of(Param p) {
		String name = p.name();
		String type = p.type();
		String desc = p.desc();
		String format = p.format();
		if(format.isEmpty()) {
			switch(type) {
			case "rasterdb":
				format = "RasterDB ID";
				break;
			case "pointdb":
				format = "PointDB ID";
				break;
			case "pointcloud":
				format = "PointCloud ID";
				break;
			case "vectordb":
				format = "VectorDB ID";
				break;
			case "string":
				format = "-";
				break;
			case "string_array":
				format = "list of entries, comma separated";
				break;
			case "integer":
				format = "integer";
				break;
			case "integer_array":
				format = "list of integer numbers, comma separated";
				break;				
			case "number":
				format = "number";
				break;
			case "number_array":
				format = "list of numbers, comma separated";
				break;	
			case "number_rect":
				format = "xmin, ymin, xmax, ymax";
				break;	
			case "layer_id":
				format = "layer ID";
				break;		
			case "boolean":
				format = "true or false";
				break;	
			}
		}
		String example = p.example();
		boolean required = p.required();
		return new RemoteTaskParameter(name, type, desc, format, example, required);
	}
}