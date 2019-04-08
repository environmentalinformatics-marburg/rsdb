package broker.group;

import broker.Informal;
import broker.acl.ACL;

public class RoiGroup {	
	
	public final String name;
	public final Informal informal;
	public final ACL acl;
	public final Roi[] rois;
	
	public RoiGroup(String name, Informal informal, ACL acl, Roi... rois) {
		this.name = name;
		this.informal =  informal;
		this.acl = acl;
		this.rois = rois;
	}
}
