package broker.group;

import broker.Informal;
import broker.acl.ACL;
import util.collections.array.ReadonlyArray;
import util.collections.vec.Vec;

public class RoiGroup {	
	
	public final String name;
	public final Informal informal;
	public final ACL acl;
	public final String epsg;
	public final String proj4;
	public final ReadonlyArray<String> messages; // nullable
	public final Roi[] rois;
	
	public RoiGroup(String name, Informal informal, ACL acl, Vec<String> messages, Roi... rois) {
		this(name, informal,acl, "", "", messages, rois);
	}	

	public RoiGroup(String name, Informal informal, ACL acl, String epsg, String proj4, Vec<String> messages, Roi... rois) {
		this.messages = messages == null || messages.isEmpty() ? null : messages.copyReadonly();
		this.name = name;
		this.informal =  informal;
		this.acl = acl;
		this.epsg = epsg;
		this.proj4 = proj4;
		this.rois = rois;
	}
	
	public boolean hasMessages() {
		return messages != null;
	}
}

