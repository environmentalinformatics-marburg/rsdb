package broker.catalog;

import java.util.Arrays;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import broker.Associated;
import broker.acl.ACL;
import util.JsonUtil;

public class CatalogEntry {

	public final String name;
	public final String type;
	public final String description; // may be empty
	public final double[][] points; // nullable
	public final Associated associated; // nullable
	public final ACL acl; // not null
	public final ACL acl_mod; // not null
	public final String[] tags;  // not null
	public final String title; // may be empty

	public CatalogEntry(String name, String type, String description, double[][] points, Associated associated,
			ACL acl, ACL acl_mod, String[] tags, String title) {
		this.name = name;
		this.type = type;
		this.description = description;
		this.points = points;
		this.associated = associated;
		this.acl = Objects.requireNonNull(acl);
		this.acl_mod = Objects.requireNonNull(acl_mod);
		this.tags = Objects.requireNonNull(tags);
		this.title = Objects.requireNonNull(title);
	}

	public static CatalogEntry of(CatalogEntry catalogEntry, double[][] points) {
		return new CatalogEntry(catalogEntry.name, catalogEntry.type, catalogEntry.description, points, catalogEntry.associated, catalogEntry.acl, catalogEntry.acl_mod, catalogEntry.tags, catalogEntry.title);
	}

	public static CatalogEntry parseJSON(JSONObject json) {
		String name = json.getString("name");
		String type = json.getString("type");
		String description = json.optString("description", "");
		double[][] points = null;
		if (json.has("polygon")) {
			JSONArray polygon = json.getJSONArray("polygon");
			int len = polygon.length();
			points = new double[len][2];
			for (int i = 0; i < len; i++) {
				JSONArray cord = polygon.getJSONArray(i);
				if (cord.length() != 2) {
					throw new RuntimeException("parse error: " + cord);
				}
				points[i][0] = cord.getDouble(0);
				points[i][1] = cord.getDouble(1);
			}
		}
		Associated associated = json.has("associated") ? Associated.parseJSON(json.getJSONObject("associated")) : null;
		ACL acl = ACL.of(JsonUtil.optStringList(json, "acl"));
		ACL acl_mod = ACL.of(JsonUtil.optStringList(json, "acl_mod"));
		String[] tags = JsonUtil.optStringArray(json, "tags");
		String title = json.optString("title", "");
		return new CatalogEntry(name, type, description, points, associated, acl, acl_mod, tags, title);
	}

	public void writeJSON(JSONWriter json, boolean withACL) {
		json.object();
		json.key("name");
		json.value(name);
		json.key("type");
		json.value(type);
		if (!description.isEmpty()) {
			json.key("description");
			json.value(description);
		}
		if (!title.isEmpty()) {
			json.key("title");
			json.value(title);
		}
		if (points != null) {
			json.key("polygon");
			json.array();
			for (double[] p : points) {
				json.array();
				json.value(p[0]);
				json.value(p[1]);
				json.endArray();
			}
			json.endArray();
		}
		if (associated != null) {
			json.key("associated");
			associated.writeJSON(json);
		}
		JsonUtil.writeOptArray(json, "tags", tags);
		if(withACL) {
			json.key("acl");
			acl.writeJSON(json);
			json.key("acl_mod");
			acl_mod.writeJSON(json);
		}
		json.endObject();
	}

	public CatalogKey toKey() {
		return new CatalogKey(name, type);
	}

	@Override
	public String toString() {
		return "CatalogEntry [name=" + name + ", type=" + type + ", description=" + description + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CatalogEntry other = (CatalogEntry) obj;
		if (acl == null) {
			if (other.acl != null)
				return false;
		} else if (!acl.equals(other.acl))
			return false;
		if (acl_mod == null) {
			if (other.acl_mod != null)
				return false;
		} else if (!acl_mod.equals(other.acl_mod))
			return false;
		if (associated == null) {
			if (other.associated != null)
				return false;
		} else if (!associated.equals(other.associated))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (!Arrays.deepEquals(points, other.points))
			return false;
		if (!Arrays.equals(tags, other.tags))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
}
