package util;

import org.json.JSONWriter;

public class Extent3d {	
	public final double xmin;
	public final double ymin;
	public final double zmin;	
	public final double xmax;
	public final double ymax;
	public final double zmax;
	
	public Extent3d(double xmin, double ymin, double zmin, double xmax, double ymax, double zmax) {
		this.xmin = xmin;
		this.ymin = ymin;
		this.zmin = zmin;
		this.xmax = xmax;
		this.ymax = ymax;
		this.zmax = zmax;
	}
	
	public void toJSON(JSONWriter json) {
		json.object();
		json.key("xmin");
		json.value(xmin);	
		json.key("xmax");
		json.value(xmax);	
		json.key("ymin");
		json.value(ymin);	
		json.key("ymax");
		json.value(ymax);
		json.key("zmin");
		json.value(zmin);	
		json.key("zmax");
		json.value(zmax);
		json.endObject();	
	}
}
