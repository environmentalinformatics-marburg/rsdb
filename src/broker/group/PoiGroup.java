package broker.group;

import broker.Informal;
import broker.acl.ACL;
import util.collections.array.ReadonlyArray;
import util.collections.vec.Vec;

public class PoiGroup {	
	
	public final String name;
	public final Informal informal;
	public final ACL acl;
	public final String epsg; // "" == missing
	public final String proj4; // "" == missing
	public final ReadonlyArray<String> messages; // nullable
	public final Poi[] pois;
	
	public PoiGroup(String name, Informal informal, ACL acl, Poi... pois) {
		this(name, informal,acl, "", "", null, pois);
	}
	
	public PoiGroup(String name, Informal informal, ACL acl, String epsg, String proj4, Vec<String> messages, Poi... pois) {
		this.messages = messages == null || messages.isEmpty() ? null : messages.copyReadonly();
		this.name = name;
		this.informal =  informal;
		this.acl = acl;
		this.epsg = epsg;
		this.proj4 = proj4;
		this.pois = pois;
	}
	
	public boolean hasMessages() {
		return messages != null;
	}
}
