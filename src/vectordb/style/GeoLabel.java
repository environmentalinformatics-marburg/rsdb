package vectordb.style;

public class GeoLabel {
	public final String text;
	public final double x;
	public final double y;

	public GeoLabel(String text, double x, double y) {
		this.text = text;
		this.x = x;
		this.y = y;
	}
}