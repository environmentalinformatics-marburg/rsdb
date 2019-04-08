package server.api.pointdb.feature;

import org.json.JSONWriter;

import pointdb.PointDB;
import pointdb.base.Rect;

public abstract class Feature {	
	public final String name;

	protected Feature() {
		String className = this.getClass().getSimpleName();
		if(className.startsWith("Feature_")) {
			name = className.substring(className.indexOf('_')+1);
		} else {
			name = className;
		}
	}

	protected Feature(String name) {
		this.name = name;
	}
	
	public abstract void calc(JSONWriter json, PointDB db, Rect rect);
}
