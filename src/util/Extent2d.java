package util;

import org.json.JSONWriter;

public class Extent2d {	
	public final double xmin;
	public final double ymin;
	public final double xmax;
	public final double ymax;
	
	public static Extent2d parse(String xmin, String ymin, String xmax, String ymax) {
		return new Extent2d(Double.parseDouble(xmin), Double.parseDouble(ymin), Double.parseDouble(xmax), Double.parseDouble(ymax));
	}
	
	public Extent2d(double xmin, double ymin, double xmax, double ymax) {
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
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
		json.endObject();	
	}
	
	@Override
	public String toString() {
		return "Extent2d [xmin=" + xmin + ", ymin=" + ymin + ", xmax=" + xmax + ", ymax=" + ymax + "] w: " + getWidth() + " h: " + getHeight();
	}
	
	public double getWidth() {
		return xmax - xmin;
	}

	public double getHeight() {
		return ymax - ymin;
	}
}
