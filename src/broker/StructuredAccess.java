package broker;

import java.util.Objects;

import org.json.JSONObject;
import org.json.JSONWriter;

public class StructuredAccess {	
	public final boolean poi;
	public final boolean roi;

	public StructuredAccess(boolean poi, boolean roi) {
		this.poi = poi;
		this.roi = roi;
	}

	public static StructuredAccess parseJSON(JSONObject json) {
		boolean poi = json.has("poi") ? json.getBoolean("poi") : false;
		boolean roi = json.has("roi") ? json.getBoolean("roi") : false;
		return new StructuredAccess(poi, roi);
	}

	public void writeJSON(JSONWriter json) {
		json.object();
		json.key("poi");
		json.value(poi);
		json.key("roi");
		json.value(roi);
		json.endObject();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StructuredAccess other = (StructuredAccess) obj;
		return poi == other.poi && roi == other.roi;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(poi, roi);
	}
}
